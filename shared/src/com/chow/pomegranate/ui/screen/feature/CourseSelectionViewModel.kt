package com.chow.pomegranate.ui.screen.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.CourseSelectionOverview
import com.chow.pomegranate.repository.CourseSelectionRepository
import com.chow.pomegranate.repository.CourseSearchFilters
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

sealed interface CourseSelectionUiState {
    data object Loading : CourseSelectionUiState
    data object NotLoggedIn : CourseSelectionUiState
    data class Error(val throwable: Throwable) : CourseSelectionUiState
    data class Success(
        val overview: CourseSelectionOverview,
        val query: String = "", val category: CourseSelectCategory = CourseSelectCategory.General,
        val searchResults: List<SelectableCourse> = emptyList(), val hasMore: Boolean = false,
        val filters: CourseSearchFilters = CourseSearchFilters(),
        val isSearching: Boolean = false, val isOperating: Boolean = false,
        val appendError: Boolean = false,
        val notice: CourseSelectionNotice? = null,
        val logs: List<CourseDropLog>? = null, val loadingLogs: Boolean = false, val logError: Throwable? = null,
        val timetable: SelectedCourseTimetable? = null, val loadingTimetable: Boolean = false, val timetableError: Throwable? = null,
    ) : CourseSelectionUiState
}
enum class CourseSelectionNotice { SearchFailed }

@KoinViewModel
class CourseSelectionViewModel(private val repository: CourseSelectionRepository, private val session: SessionManager, val workspace: com.chow.pomegranate.repository.CourseSelectionWorkspace) : ViewModel() {
    val uiState: StateFlow<CourseSelectionUiState>
        field = MutableStateFlow<CourseSelectionUiState>(CourseSelectionUiState.Loading)
    val isRefreshing: StateFlow<Boolean>
        field = MutableStateFlow(false)
    val operationNotice = repository.operationNotice
    fun consumeOperationNotice() = repository.consumeOperationNotice()
    private var request: Job? = null
    private var logRequest: Job? = null
    private var timetableRequest: Job? = null
    private var page = 0
    init {
        refresh()
        viewModelScope.launch {
            session.userId.drop(1).collect {
                request?.cancelAndJoin()
                logRequest?.cancelAndJoin()
                timetableRequest?.cancelAndJoin()
                workspace.invalidate()
                repository.consumeOperationNotice()
                isRefreshing.value = false
                uiState.value = CourseSelectionUiState.Loading
                refresh()
            }
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        val previous = uiState.value as? CourseSelectionUiState.Success
        request?.cancel()
        logRequest?.cancel()
        timetableRequest?.cancel()
        request = viewModelScope.launch {
            uiState.value = previous?.copy(isSearching = false, loadingLogs = false) ?: CourseSelectionUiState.Loading
            try {
                if (!session.isLoggedInWithWait()) { uiState.value = CourseSelectionUiState.NotLoggedIn; return@launch }
                val overview = repository.getOverview()
                workspace.activate(overview)
                val current = previous?.copy(overview = overview, logs = null, logError = null, loadingLogs = false,
                    timetable = null, timetableError = null, loadingTimetable = false, appendError = false) ?: CourseSelectionUiState.Success(overview)
                val effective = if (workspace.state.value.preferenceMode) current.filters.copy(filterFull = false) else current.filters
                val results = repository.searchPage(current.query, current.category, filters = effective)
                workspace.refreshFavorites(results.items)
                page = 0
                uiState.value = current.copy(filters = effective, searchResults = results.items, hasMore = results.items.size < results.filteredCount, isSearching = false)
                if (previous?.logs != null || previous?.loadingLogs == true) loadLogs()
                if (previous?.timetable != null || previous?.loadingTimetable == true) loadTimetable()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { uiState.value = CourseSelectionUiState.Error(error) }
            finally { isRefreshing.value = false }
        }
    }

    fun search(query: String, category: CourseSelectCategory? = null, filters: CourseSearchFilters? = null, append: Boolean = false) {
        val previous = uiState.value as? CourseSelectionUiState.Success ?: return
        if (append && (!previous.hasMore || previous.isSearching)) return
        request?.cancel()
        val current = previous.copy(query = query, category = category ?: previous.category,
            filters = (filters ?: previous.filters).forCategory(category ?: previous.category).let { if (workspace.state.value.preferenceMode) it.copy(filterFull = false) else it },
            searchResults = if (append) previous.searchResults else emptyList(), isSearching = true, notice = null, appendError = false)
        uiState.value = current
        request = viewModelScope.launch {
            try {
                val nextPage = if (append) page + 1 else 0
                val result = repository.searchPage(query, current.category, nextPage, filters = current.filters)
                workspace.refreshFavorites(result.items)
                page = nextPage
                val items = if (append) (current.searchResults + result.items).distinctBy { it.id } else result.items
                // 日志请求可能同时完成，保留它更新的状态。
                val latest = uiState.value as? CourseSelectionUiState.Success ?: current
                uiState.value = latest.copy(searchResults = items, hasMore = (nextPage + 1) * 20 < result.filteredCount, isSearching = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                val latest = uiState.value as? CourseSelectionUiState.Success ?: current
                uiState.value = latest.copy(isSearching = false, notice = CourseSelectionNotice.SearchFailed, appendError = append)
            }
        }
    }
    fun loadLogs() {
        val current = uiState.value as? CourseSelectionUiState.Success ?: return
        if (current.loadingLogs || current.logs != null) return
        uiState.value = current.copy(loadingLogs = true, logError = null)
        logRequest = viewModelScope.launch {
            try {
                val logs = repository.getDropLogs()
                val latest = uiState.value as? CourseSelectionUiState.Success ?: return@launch
                uiState.value = latest.copy(logs = logs, loadingLogs = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val latest = uiState.value as? CourseSelectionUiState.Success ?: return@launch
                uiState.value = latest.copy(loadingLogs = false, logError = error)
            }
        }
    }
    fun consumeNotice() { (uiState.value as? CourseSelectionUiState.Success)?.let { uiState.value = it.copy(notice = null) } }

    fun loadTimetable() {
        val current = uiState.value as? CourseSelectionUiState.Success ?: return
        if (current.loadingTimetable || current.timetable != null) return
        uiState.value = current.copy(loadingTimetable = true, timetableError = null)
        timetableRequest = viewModelScope.launch {
            try {
                val timetable = repository.getTimetable()
                val latest = uiState.value as? CourseSelectionUiState.Success ?: return@launch
                uiState.value = latest.copy(timetable = timetable, loadingTimetable = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val latest = uiState.value as? CourseSelectionUiState.Success ?: return@launch
                uiState.value = latest.copy(timetableError = error, loadingTimetable = false)
            }
        }
    }
}

@KoinViewModel
class CourseOperationViewModel(private val repository: CourseSelectionRepository) : ViewModel() {
    fun operate(route: CourseOperationRoute, onResult: (Result<CourseOperateResult>) -> Unit) {
        viewModelScope.launch {
            val result = try {
                Result.success(when (route.operation) {
                    CourseOperation.Select -> repository.select(route.courseId, CourseSelectCategory.valueOf(route.category))
                    CourseOperation.Drop -> repository.drop(route.courseId)
                })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Result.failure(error) }
            if (result.getOrNull()?.isSuccess == true) repository.notifyOperation(route.operation == CourseOperation.Drop)
            onResult(result)
        }
    }
}
