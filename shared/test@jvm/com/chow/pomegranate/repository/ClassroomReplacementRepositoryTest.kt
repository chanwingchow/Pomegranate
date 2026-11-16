package com.chow.pomegranate.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.chow.pomegranate.data.local.AppDatabase
import com.chow.pomegranate.service.academic.affairs.demo.MockAcademicAffairs
import com.chow.pomegranate.service.foundation.Semester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*

class ClassroomReplacementRepositoryTest {
    @Test fun userRulesMayOverrideTheSameBuiltinSourceInEachTarget() = runBlocking {
        withClassroomDatabase { db ->
            val preferences = AppPreferenceRepository(db)
            val repository = ClassroomReplacementRepository(preferences)
            repository.save(null, "乐学楼", "我的教学楼")
            repository.save(null, "高等数学", "数学自定义", NameReplacementTarget.Course)
            val reopened = ClassroomReplacementRepository(preferences)
            val rules = reopened.get()
            assertEquals("我的教学楼503", ClassroomReplacements(rules).format("乐学楼503"))
            assertEquals("数学自定义A", ClassroomReplacements(rules, NameReplacementTarget.Course).format("高等数学A"))
            assertEquals("乐学楼701", ClassroomReplacements(rules).format("乐学楼503", "乐学楼701"))
            assertEquals("高等数学自选课", ClassroomReplacements(rules, NameReplacementTarget.Course).format("高等数学A", "高等数学自选课"))
            reopened.delete(rules.filterNot { it.isBuiltin }.map { it.id }.toSet())
            assertEquals("北五503", ClassroomReplacements(reopened.get()).format("乐学楼503"))
            assertEquals("高数A", ClassroomReplacements(reopened.get(), NameReplacementTarget.Course).format("高等数学A"))
        }
    }
    @Test fun malformedPreferenceKeepsBuiltinsAvailableButDisabled() = runBlocking {
        withClassroomDatabase { db ->
            val preferences = AppPreferenceRepository(db)
            preferences.set("classroom_replacement_rules", "invalid-json")
            val repository = ClassroomReplacementRepository(preferences)
            val rules = repository.rules.first()
            assertEquals(builtinNameRules.size, rules.size)
            assertTrue(rules.all { it.isBuiltin && !it.enabled })
        }
    }
    @Test fun rulesPersistGloballyWhileServerDataAndPerAccountOverridesStayIndependent() = runBlocking {
        withClassroomDatabase { db ->
            val preferences = AppPreferenceRepository(db)
            val repository = ClassroomReplacementRepository(preferences)
            repository.save(null, "教学楼 C", "笃行楼 C")
            val rule = repository.get().single { it.source == "教学楼 C" }
            val reopened = ClassroomReplacementRepository(preferences)
            assertEquals(repository.get(), reopened.rules.first())
            val data = MockAcademicAffairs(0, 0).enrollment.getTimetable(Semester.now())
            val first = data.courses.first().lesson(data.ownerKey())
            val other = data.copy(userId = "other-user", semester = data.semester.next()).let { it.courses.first().lesson(it.ownerKey()) }
            val customizations = TimetableCustomizationRepository(preferences)
            customizations.save(first, "教学楼 C221", "第一位用户的备注")
            customizations.save(other, "教学楼 C305", "第二位用户的备注")
            val mapping = ClassroomReplacements(reopened.get())
            assertEquals("笃行楼 C221", mapping.format(customizations.get(first.owner).getValue(first.key).classroom!!))
            assertEquals("笃行楼 C305", mapping.format(customizations.get(other.owner).getValue(other.key).classroom!!))
            assertEquals("教学楼 C221", customizations.get(first.owner).getValue(first.key).classroom)
            assertEquals("实验楼 A301", data.courses.first().classroom)
            repository.enabled(rule.id, false)
            assertEquals("教学楼 C221", ClassroomReplacements(repository.get()).format("教学楼 C221"))
            repository.enabled(rule.id, true)
            repository.save(rule.id, "教学楼 C", "知行楼 C")
            assertEquals("知行楼 C221", ClassroomReplacements(reopened.get()).format("教学楼 C221"))
            repository.delete(rule.id)
            assertTrue(reopened.get().none { it.id == rule.id })
            assertEquals("教学楼 C221", ClassroomReplacements(reopened.get()).format("教学楼 C221"))
        }
    }

    @Test fun rejectsDuplicateEmptyAndStaleRulesWithoutLosingExistingSettings() = runBlocking {
        withClassroomDatabase { db ->
            val repository = ClassroomReplacementRepository(AppPreferenceRepository(db))
            repository.save(null, "教学楼", "笃行楼")
            assertFailsWith<IllegalArgumentException> { repository.save(null, " 教学楼 ", "知行楼") }
            assertFailsWith<IllegalArgumentException> { repository.save(null, "", "知行楼") }
            assertFailsWith<IllegalArgumentException> { repository.save(null, "实验楼", "实验楼") }
            assertFailsWith<IllegalArgumentException> { repository.save("deleted-id", "实验楼", "科技楼") }
            assertEquals("笃行楼", repository.get().single { it.source == "教学楼" }.replacement)
        }
    }

    @Test fun builtinsSupportIndividualAndGroupSwitchesButCannotBeEditedOrDeleted() = runBlocking {
        withClassroomDatabase { db ->
            val preferences = AppPreferenceRepository(db)
            val repository = ClassroomReplacementRepository(preferences)
            val initial = repository.get()
            assertEquals(builtinNameRules, initial)
            val rule = initial.single { it.source == "乐学楼" }
            repository.enabled(rule.id, false)
            assertEquals("乐学楼701", ClassroomReplacements(repository.get()).format("乐学楼701"))
            assertFailsWith<IllegalArgumentException> { repository.save(rule.id, rule.source, "自定义楼名") }
            val ids = initial.map { it.id }.toSet()
            repository.delete(ids)
            assertEquals(initial.size, repository.get().size)
            repository.enabled(ids, false)
            val reopened = ClassroomReplacementRepository(preferences)
            assertTrue(reopened.get().all { !it.enabled })
            reopened.enabled(ids, true)
            assertEquals(initial, reopened.get())
        }
    }

    @Test fun batchDeletionPreservesUnselectedRulesAndProtectsBuiltins() = runBlocking {
        withClassroomDatabase { db ->
            val preferences = AppPreferenceRepository(db)
            val repository = ClassroomReplacementRepository(preferences)
            for (name in listOf("A", "B", "C")) repository.save(null, name, "楼$name")
            val rules = repository.get()
            val keep = rules.single { it.source == "B" }
            repository.enabled(keep.id, false)
            repository.delete(rules.filter { it.source != "B" }.map { it.id }.toSet() + "stale-id")
            val reopened = ClassroomReplacementRepository(preferences)
            assertEquals(builtinNameRules + keep.copy(enabled = false), reopened.get())
            reopened.delete(setOf(keep.id))
            assertEquals(builtinNameRules, ClassroomReplacementRepository(preferences).get())
        }
    }

    @Test fun previouslyDeletedBuiltinsReturnSwitchedOffWithoutLosingCustomRules() = runBlocking {
        withClassroomDatabase { db ->
            val preferences = AppPreferenceRepository(db)
            val custom = ClassroomReplacementRule("custom", "A", "B")
            preferences.set("classroom_replacement_rules", kotlinx.serialization.json.Json.encodeToString(listOf(custom)))
            val rules = ClassroomReplacementRepository(preferences).get()
            assertEquals(builtinNameRules.map { it.copy(enabled = it.target == NameReplacementTarget.Course) } + custom, rules)
        }
    }
}

private suspend fun withClassroomDatabase(block: suspend (AppDatabase) -> Unit) {
    val file = Files.createTempFile("classroom-replacements-", ".db")
    val db = Room.databaseBuilder<AppDatabase>(file.toString()).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO).build()
    try { block(db) } finally { db.close(); file.deleteIfExists() }
}
