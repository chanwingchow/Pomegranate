package com.chow.pomegranate.ui.screen.feature

import com.chow.pomegranate.repository.StudyCatalogRepository
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.extracurricular.demo.MockExtracurricularActivity
import com.chow.pomegranate.ui.component.StudyDetailRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class StudyDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }
    private fun model() = StudyDetailViewModel(StudyCatalogRepository(MockAcademicAffairs(0, 0), MockExtracurricularActivity(100)))

    @Test fun returningDuringRemoteLoadDoesNotRestartItAndLoadedDataIsReused() = runTest {
        val model = model()
        val route = StudyDetailRoute("event:1001", "活动", eventId = 1001)
        model.load(route)
        dispatcher.scheduler.runCurrent()
        dispatcher.scheduler.advanceTimeBy(50)
        model.load(route)
        dispatcher.scheduler.advanceTimeBy(51)
        dispatcher.scheduler.runCurrent()
        assertNotNull(model.state.value.sections)
        val loaded = model.state.value
        model.load(route)
        assertSame(loaded, model.state.value)
        model.load(route, force = true)
        assertNull(model.state.value.sections)
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(model.state.value.sections)
    }

    @Test fun newerRouteCannotBeOverwrittenByAnOlderRequest() = runTest {
        val model = model()
        val remote = StudyDetailRoute("event:1001", "活动", eventId = 1001)
        val local = StudyDetailRoute("course:1", "课程", sections = emptyList())
        model.load(remote)
        dispatcher.scheduler.runCurrent()
        model.load(local)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(local, model.state.value.route)
        assertEquals(emptyList(), model.state.value.sections)
    }
}
