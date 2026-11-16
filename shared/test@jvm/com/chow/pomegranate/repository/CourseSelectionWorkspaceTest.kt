package com.chow.pomegranate.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.service.academic.affairs.api.AcademicAffairs
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.academic.affairs.model.*
import com.chow.pomegranate.session.*
import com.chow.pomegranate.ui.component.*
import com.chow.pomegranate.ui.screen.feature.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*

class CourseSelectionWorkspaceTest {
    @Test fun favoritesPersistAndStayIsolatedByAccountModeAndRound() = runBlocking {
        workspaceFixture { f ->
            val choice = f.available.first()
            f.workspace.toggleFavorite(choice, CourseSelectCategory.General.name)
            f.workspace.setPreferenceMode(true)
            val restored = f.newWorkspace()
            restored.activate(f.overview)
            assertEquals(listOf(choice), restored.state.value.favorites.map { it.course })
            assertTrue(restored.state.value.preferenceMode)
            f.session.userId.value = "other"
            restored.activate(f.overview)
            assertTrue(restored.state.value.favorites.isEmpty())
            f.session.userId.value = "demo"
            restored.activate(f.overview)
            assertEquals(1, restored.state.value.favorites.size)
            f.mode.setEnabled(false)
            restored.activate(f.overview)
            assertTrue(restored.state.value.favorites.isEmpty())
            f.mode.setEnabled(true)
            restored.activate(f.overview.copy(entrance = f.overview.entrance.copy(id = "another-round")))
            assertTrue(restored.state.value.favorites.isEmpty())
        }
    }

    @Test fun mixedBatchDeduplicatesAndKeepsOnlyFailedItemsForRetry() = runBlocking {
        workspaceFixture { f ->
            val (first, second) = f.available.take(2).map { f.item(it) }
            var fail = true
            f.select = { id, _, _ -> CourseOperateResult(id != second.id || !fail, if (id == second.id && fail) "名额已满" else "成功") }
            f.workspace.togglePending(first, false); f.workspace.togglePending(second, false)
            val report = f.workspace.submit(listOf(first, first, second), false)!!
            assertEquals(1, report.successCount)
            assertEquals(2, f.selectCalls.size)
            assertEquals(listOf(second.id), f.workspace.state.value.pendingSelect.map { it.id })
            assertEquals("名额已满", report.results.last().message)
            fail = false
            f.workspace.submit(f.workspace.state.value.pendingSelect, false)
            assertTrue(f.workspace.state.value.pendingSelect.isEmpty())
            assertEquals(listOf(first.id, second.id, second.id), f.selectCalls.map { it.first })
        }
    }

    @Test fun priorityParametersReachApiWithoutPretendingToWinLottery() = runBlocking {
        workspaceFixture { f ->
            f.workspace.setPreferenceMode(true)
            val first = f.item(f.available[0]).copy(courseCode = "SAME")
            val second = f.item(f.available[1]).copy(courseCode = "SAME")
            f.workspace.togglePending(first, false); f.workspace.togglePending(second, false)
            assertEquals(listOf(1, 2), f.workspace.state.value.pendingSelect.map { it.priority })
            f.workspace.setPriority(second.id, 3)
            val before = f.workspace.state.value.selectedIds
            val report = f.workspace.submit(f.workspace.state.value.pendingSelect, false)!!
            assertTrue(report.preference)
            assertEquals(listOf(CourseSelectPriority.First, CourseSelectPriority.Third), f.selectCalls.map { it.third })
            assertEquals(before, f.workspace.state.value.selectedIds)
            assertEquals(2, f.workspace.state.value.submittedPreferences.size)
            assertFailsWith<IllegalArgumentException> {
                f.workspace.submit(listOf(first.copy(id = "another", priority = 1)), false)
            }
            assertEquals(2, f.selectCalls.size)
        }
    }

    @Test fun dropBatchKeepsFailedCourseAndConfirmedNavigationReturnsToList() = runBlocking {
        workspaceFixture { f ->
            val selected = f.overview.selectedCourses.take(2)
            val items = selected.map { it.record().detail.cartItem()!! }
            f.drop = { id -> if (id == selected.last().id) CourseOperateResult(false, "退课时段未开放") else f.demo.courseSystem.dropCourse(id) }
            items.forEach { f.workspace.togglePending(it, true) }
            val report = f.workspace.submit(items, true)!!
            assertEquals(1, report.successCount)
            assertEquals(listOf(items.last()), f.workspace.state.value.pendingDrop)
            assertFalse(items.first().id in f.workspace.state.value.selectedIds)
            assertTrue(items.last().id in f.workspace.state.value.selectedIds)
            val detail = AdaptiveStudyDetailRoute(items.first().detail)
            val operation = CourseOperationRoute(CourseOperation.Drop, items.first().id, items.first().name, items = items, ownerKey = f.workspace.state.value.key)
            assertEquals(operation, Json.decodeFromString<CourseOperationRoute>(Json.encodeToString(operation)))
            val stack = mutableListOf<androidx.navigation3.runtime.NavKey>(CourseSelectionRoute, detail, operation)
            stack.removeLast() // 取消确认保留详情。
            assertEquals(detail, stack.last())
            stack.add(operation); stack.finishCourseOperation()
            assertEquals(listOf<androidx.navigation3.runtime.NavKey>(CourseSelectionRoute), stack)
        }
    }

    @Test fun semesterIsExplicitEvenWhenRoundIdsAreReusedAndExpiredFavoritesAreNotDeletedAutomatically() = runBlocking {
        workspaceFixture { f ->
            val past = f.overview.copy(entrance = f.overview.entrance.copy(
                semester = f.overview.entrance.semester.previous(), endTime = kotlinx.datetime.LocalDateTime(2000, 1, 1, 0, 0)))
            f.workspace.activate(past)
            f.workspace.toggleFavorite(f.available[0], CourseSelectCategory.General.name)
            val oldKey = f.workspace.state.value.key
            assertTrue(f.workspace.state.value.expired)
            assertEquals(1, f.workspace.state.value.favorites.size)
            f.workspace.activate(f.overview)
            assertNotEquals(oldKey, f.workspace.state.value.key)
            assertTrue(f.workspace.state.value.favorites.isEmpty())
            assertEquals(1, f.workspace.state.value.archived.size)
            assertTrue(f.workspace.state.value.archived.single().scope.isExpired())
            assertFailsWith<IllegalStateException> { f.workspace.submit(listOf(f.item(f.available[0])), false, oldKey) }
            assertTrue(f.selectCalls.isEmpty())
            assertEquals(1, f.workspace.clearExpiredFavorites())
            assertTrue(f.workspace.state.value.archived.isEmpty())
        }
    }

    @Test fun expiryBlocksOldTeachingIdsAndCleanupOnlyTouchesCurrentAccount() = runBlocking {
        workspaceFixture { f ->
            f.session.userId.value = "other"
            f.workspace.activate(f.overview)
            f.workspace.toggleFavorite(f.available[0], CourseSelectCategory.General.name)
            f.session.userId.value = "demo"
            f.workspace.activate(f.overview)
            f.workspace.toggleFavorite(f.available[1], CourseSelectCategory.General.name)
            f.workspace.activate(f.overview.copy(entrance = f.overview.entrance.copy(endTime = kotlinx.datetime.LocalDateTime(2000, 1, 1, 0, 0))))
            assertTrue(f.workspace.state.value.expired)
            assertFailsWith<IllegalStateException> { f.workspace.submit(listOf(f.item(f.available[1])), false) }
            assertTrue(f.selectCalls.isEmpty())
            assertEquals(1, f.workspace.clearExpiredFavorites())
            f.session.userId.value = "other"
            f.workspace.activate(f.overview)
            assertEquals(1, f.workspace.state.value.favorites.size)
        }
    }

    @Test fun accountSwitchStopsRemainingRequestsAndResultsDoNotLeakToNewAccount() = runBlocking {
        workspaceFixture { f ->
            coroutineScope {
                val started = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                f.select = { _, _, _ -> started.complete(Unit); release.await(); CourseOperateResult(true, "成功") }
                val batch = async { f.workspace.submit(f.available.take(2).map { f.item(it) }, false) }
                started.await()
                assertNull(f.workspace.submit(listOf(f.item(f.available[2])), false))
                f.session.userId.value = "other"
                f.workspace.activate(f.overview)
                release.complete(Unit)
                batch.await()
                assertEquals(1, f.selectCalls.size)
                assertNull(f.workspace.state.value.report)
                assertFalse(f.workspace.state.value.running)
            }
        }
    }
}

private class WorkspaceFixture(val db: AppDatabase) {
    val preferences = AppPreferenceRepository(db)
    val mode = DemoModeRepository(preferences)
    val session = WorkspaceSession()
    val demo = MockAcademicAffairs(0, 0)
    val selectCalls = mutableListOf<Triple<String, CourseSelectCategory, CourseSelectPriority?>>()
    var select: suspend (String, CourseSelectCategory, CourseSelectPriority?) -> CourseOperateResult = { _, _, _ -> CourseOperateResult(true, "成功") }
    var drop: suspend (String) -> CourseOperateResult = { demo.courseSystem.dropCourse(it) }
    val service = object : AcademicAffairs by demo {
        override val courseSystem = object : AcademicAffairs.CourseSystem by demo.courseSystem {
            override suspend fun selectCourse(id: String, category: CourseSelectCategory, priority: CourseSelectPriority?): CourseOperateResult {
                selectCalls.add(Triple(id, category, priority)); return select(id, category, priority)
            }
            override suspend fun dropCourse(id: String) = drop(id)
        }
    }
    val repository = CourseSelectionRepository(service)
    val workspace = newWorkspace()
    lateinit var overview: CourseSelectionOverview
    lateinit var available: List<SelectableCourse>
    fun newWorkspace() = CourseSelectionWorkspace(preferences, session, mode, repository)
    fun item(course: SelectableCourse) = course.record(CourseSelectCategory.General).detail.cartItem()!!
    suspend fun initialize() {
        mode.setEnabled(true)
        overview = repository.getOverview()
        available = repository.searchPage("").items.filter { !it.isSelected }
        check(available.size >= 3)
        workspace.activate(overview)
    }
}

private suspend fun workspaceFixture(block: suspend (WorkspaceFixture) -> Unit) {
    val file = Files.createTempFile("course-workspace-", ".db")
    val db = Room.databaseBuilder<AppDatabase>(file.toString()).setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    try { val f = WorkspaceFixture(db); f.initialize(); block(f) } finally { db.close(); file.deleteIfExists() }
}

private class WorkspaceSession : SessionManager {
    override val userId = MutableStateFlow<String?>("demo")
    override val state = MutableStateFlow<SessionState>(SessionState.Active)
    override val accounts = MutableStateFlow(emptyList<Account>())
    override suspend fun awaitLogin() = false
    override suspend fun getCaptcha(): ByteArray = error("unused")
    override suspend fun signIn(request: LoginRequest) = Unit
    override suspend fun login(userId: String, password: String, secondClassroomPassword: String, captcha: String) = Unit
    override suspend fun autoLogin() = Unit
    override suspend fun switchAccount(userId: String) = Unit
    override suspend fun deleteAccount(userId: String) = Unit
    override suspend fun refresh() = Unit
    override suspend fun logout() = Unit
}
