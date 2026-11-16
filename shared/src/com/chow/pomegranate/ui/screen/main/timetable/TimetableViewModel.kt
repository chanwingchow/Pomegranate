package com.chow.pomegranate.ui.screen.main.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.data.local.PreferenceKeys
import com.chow.pomegranate.repository.AppPreferenceRepository
import com.chow.pomegranate.repository.TimetableRepository
import com.chow.pomegranate.repository.TimetableCustomizationRepository
import com.chow.pomegranate.repository.TimetableCourseCustomization
import com.chow.pomegranate.repository.ownerKey
import com.chow.pomegranate.repository.AcademicWeekRepository
import com.chow.pomegranate.repository.currentLocalDate
import com.chow.pomegranate.service.foundation.Campus
import kotlinx.datetime.*
import kotlinx.coroutines.Job
import com.chow.pomegranate.service.academic.affairs.model.Timetable
import com.chow.pomegranate.session.SessionManager
import com.chow.pomegranate.session.isLoggedInWithWait
import com.chow.pomegranate.utils.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** 课表页 UI 状态。 */
sealed interface TimetableUiState {
    /** 加载中。 */
    data object Loading : TimetableUiState

    /** 尚未登录，不能请求课表。 */
    data object NotLoggedIn : TimetableUiState

    /** 加载成功。 */
    data class Success(
        val timetable: Timetable,
        val display: TimetableDisplay,
    ) : TimetableUiState

    /** 加载失败。 */
    data class Error(val throwable: Throwable) : TimetableUiState
}

/** 课表页 ViewModel。 */
@KoinViewModel
class TimetableViewModel(
    private val repository: TimetableRepository,
    private val preferenceRepository: AppPreferenceRepository,
    private val sessionManager: SessionManager,
    private val customizationRepository: TimetableCustomizationRepository,
    private val academicWeeks: AcademicWeekRepository,
) : ViewModel() {
    private val date = MutableStateFlow(currentLocalDate())

    /** 当前 UI 状态。 */
    val uiState: StateFlow<TimetableUiState>
        field = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)

    /** 周次，第 0 周表示开学前一周。 */
    val week: StateFlow<Int>
        field = MutableStateFlow(0)

    /** 临时浏览日期，不写入本地偏好。 */
    val selectedDay: StateFlow<Int>
        field = MutableStateFlow(today().dayOfWeek.ordinal)
    val focusedDate: StateFlow<LocalDate?>
        field = MutableStateFlow(null)

    val campus: StateFlow<Campus>
        field = MutableStateFlow(Campus.Canton)

    val currentWeek: StateFlow<Int>
        field = MutableStateFlow(0)
    val showOtherWeeks: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val customizations: StateFlow<Map<String, TimetableCourseCustomization>>
        field = MutableStateFlow(emptyMap())
    private var customizationRequest: Job? = null
    private var customizationOwner: String? = null
    private var observedCurrentWeek: Int? = null

    /** 已加载的课表：切换周次时复用它重新计算，不重新请求。 */
    private var loadedTimetable: Timetable? = null

    init {
        viewModelScope.launch {
            academicWeeks.observe(date).collect { value ->
                val changed = observedCurrentWeek != null && observedCurrentWeek != value
                observedCurrentWeek = value
                currentWeek.value = value
                if (changed) returnToToday()
            }
        }

        viewModelScope.launch {
            preferenceRepository.observe(PreferenceKeys.CAMPUS).collect { saved -> campus.value = Campus.entries.getOrNull(saved?.toIntOrNull() ?: -1) ?: Campus.Canton }
        }
        viewModelScope.launch {
            preferenceRepository.observe("timetable_show_other_weeks").collect { showOtherWeeks.value = it == "true" }
        }
        load(refresh = false)
    }

    /** 临时切换查看周次，刷新和详情返回时复用，不保存到下一次启动。 */
    fun setWeek(value: Int) {
        week.value = value.coerceIn(0, TIMETABLE_MAX_WEEK - 1)

        // 已有数据时立即重算，避免重新请求
        loadedTimetable?.let { uiState.value = successState(it) }

    }

    fun selectDate(date: LocalDate) {
        selectedDay.value = date.dayOfWeek.ordinal
        focusedDate.value = date
    }

    fun returnToToday() {
        selectDate(today())
        setWeek(currentWeek.value)
    }

    private fun today() = date.value

    fun updateDate(value: LocalDate) {
        val previous = date.value
        date.value = value
        if (previous != value && focusedDate.value == previous) selectDate(value)
    }

    fun setShowOtherWeeks(value: Boolean) {
        showOtherWeeks.value = value
        viewModelScope.launch { preferenceRepository.set("timetable_show_other_weeks", value.toString()) }
    }

    /** 从教务系统刷新课表。 */
    fun refresh() = load(refresh = true)

    private fun load(refresh: Boolean) {
        viewModelScope.launch {
            uiState.value = TimetableUiState.Loading

            // 未登录时不请求受保护接口，先等待自动登录
            if (!sessionManager.isLoggedInWithWait()) {
                uiState.value = TimetableUiState.NotLoggedIn
                return@launch
            }

            uiState.value = runSuspendCatching {
                repository.getTimetable(refresh = refresh).also { timetable ->
                    if (customizationOwner != timetable.ownerKey()) {
                        currentWeek.value = academicWeeks.get(today())
                        week.value = currentWeek.value
                        selectDate(today())
                    }
                }
            }
                .fold(
                    onSuccess = { timetable ->
                        loadedTimetable = timetable
                        if (customizationOwner != timetable.ownerKey()) {
                            customizationOwner = timetable.ownerKey()
                            customizationRequest?.cancel()
                            customizations.value = emptyMap()
                            customizationRequest = viewModelScope.launch {
                                customizationRepository.observe(timetable.ownerKey()).collect { customizations.value = it }
                            }
                        }
                        successState(timetable)
                    },
                    onFailure = TimetableUiState::Error,
                )
        }
    }

    /** 按当前周次生成成功状态。 */
    private fun successState(timetable: Timetable) = TimetableUiState.Success(
        timetable = timetable,
        display = TimetableDisplay.of(timetable.courses, week = week.value),
    )
}
