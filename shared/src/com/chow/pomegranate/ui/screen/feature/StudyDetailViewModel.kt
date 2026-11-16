package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.ui.component.StudyDetailRoute
import com.chow.pomegranate.ui.component.StudySection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chow.pomegranate.repository.StudyCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class StudyDetailState(val route: StudyDetailRoute? = null, val sections: List<StudySection>? = null, val error: Throwable? = null)

@KoinViewModel
class StudyDetailViewModel(private val repository: StudyCatalogRepository) : ViewModel() {
    val state: StateFlow<StudyDetailState>
        field = MutableStateFlow(StudyDetailState())
    private var request: Job? = null

    fun load(route: StudyDetailRoute, force: Boolean = false) {
        if (!force && state.value.route == route) return
        request?.cancel()
        if (route.teacherId == null && route.eventId == null) {
            state.value = StudyDetailState(route, route.sections)
            return
        }
        state.value = StudyDetailState(route)
        request = viewModelScope.launch {
            try {
                val sections = when {
                    route.teacherId != null -> repository.teacher(route.teacherId)
                    route.eventId != null -> repository.event(route.eventId)
                    else -> route.sections
                }
                state.value = StudyDetailState(route, sections)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { state.value = StudyDetailState(route, error = error) }
        }
    }
}
