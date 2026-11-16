package com.chow.pomegranate.ui.screen.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.*
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.SessionState
import com.chow.pomegranate.session.isLoggedInWithWait
import com.chow.pomegranate.session.isSecondClassroomLoggedInWithWait
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class StudyCatalogState(
    val catalog: StudyCatalog? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val notLoggedIn: Boolean = false,
    val error: Throwable? = null,
    val semester: Semester = Semester.now(),
    val query: String = "",
    val tab: Int = 0,
)

@KoinViewModel
class StudyCatalogViewModel(private val repository: StudyCatalogRepository, private val session: SessionManager,
    private val schoolTimetables: SchoolTimetableRepository) : ViewModel() {
    val state: StateFlow<StudyCatalogState>
        field = MutableStateFlow(StudyCatalogState())
    val tabStates: StateFlow<Map<Int, StudyCatalogState>>
        field = MutableStateFlow(emptyMap())
    val schoolTimetableFilters: StateFlow<SchoolTimetableFilters>
        field = MutableStateFlow(SchoolTimetableFilters())

    fun setSchoolTimetableFilters(filters: SchoolTimetableFilters) {
        schoolTimetableFilters.value = filters.normalized()
    }

    fun setLocalQuery(query: String) { publish(state.value.copy(query = query)) }

    private var request: Job? = null
    private var feature: AcademicFeature? = null
    private var loadedForUser: String? = null
    private var page = 1

    /** 返回列表时复用已有状态；登录成功或切换账号后重新获取对应账号的数据。 */
    fun ensureLoaded(feature: AcademicFeature, initialQuery: String? = null) {
        val loggedIn = if (feature == AcademicFeature.Extracurricular) session.secondClassroomState.value else session.state.value
        if (this.feature == feature && loadedForUser == session.userId.value &&
            !(state.value.notLoggedIn && loggedIn == SessionState.Active)) return
        load(feature, query = initialQuery ?: state.value.query)
    }

    private fun publish(value: StudyCatalogState) {
        state.value = value
        if (feature == AcademicFeature.Extracurricular) tabStates.value = tabStates.value + (value.tab to value)
    }

    fun selectTab(tab: Int) {
        val feature = feature ?: return
        if (feature != AcademicFeature.Extracurricular || state.value.tab == tab) return
        request?.cancel()
        val cached = tabStates.value[tab]
        if (cached != null && !cached.loading) publish(cached.copy(refreshing = false, loadingMore = false)) else load(feature, tab = tab)
    }

    fun refresh() {
        val feature = feature ?: return
        if (state.value.loading || state.value.refreshing) return
        load(feature, refresh = true)
    }

    fun load(feature: AcademicFeature, semester: Semester = state.value.semester, query: String = state.value.query, tab: Int = state.value.tab, refresh: Boolean = false) {
        if (this.feature != feature || loadedForUser != session.userId.value) {
            tabStates.value = emptyMap()
            schoolTimetableFilters.value = SchoolTimetableFilters()
        }
        this.feature = feature
        loadedForUser = session.userId.value
        request?.cancel()
        publish(if (refresh) state.value.copy(refreshing = true, loadingMore = false, error = null)
            else StudyCatalogState(semester = semester, query = query, tab = tab))
        request = viewModelScope.launch {
            try {
                val loggedIn = if (feature == AcademicFeature.Extracurricular) session.isSecondClassroomLoggedInWithWait() else session.isLoggedInWithWait()
                if (!loggedIn) {
                    publish(state.value.copy(loading = false, refreshing = false, notLoggedIn = true))
                    return@launch
                }
                loadedForUser = session.userId.value
                val catalog = if (feature == AcademicFeature.CourseTimetable) schoolTimetables.get(semester, refresh).toStudyCatalog()
                    else repository.load(feature, semester, query, tab = tab)
                page = 1
                publish(state.value.copy(catalog = catalog, loading = false, refreshing = false))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { publish(state.value.copy(loading = false, refreshing = false, error = error)) }
        }
    }

    fun loadMore() {
        val feature = feature ?: return
        val previous = state.value
        if (previous.loading || previous.refreshing || previous.loadingMore || previous.catalog?.hasMore != true) return
        request = viewModelScope.launch {
            state.value = previous.copy(loadingMore = true, error = null)
            try {
                val result = repository.load(feature, previous.semester, previous.query, page + 1)
                val records = (previous.catalog.groups.first().records + result.groups.first().records).distinctBy { it.id }
                page++
                state.value = previous.copy(catalog = StudyCatalog(listOf(StudyGroup("教师", records)), result.hasMore))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { state.value = previous.copy(error = error) }
        }
    }
}
