package com.awesometodo.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    private lateinit var database: AppDatabase

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() {
        database.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(MIGRATION_DB)
    }

    @Test fun deletingTodoKeepsSessionSnapshot() = runTest {
        val dao = database.appDao()
        val todo = TodoEntity("todo", "阅读", 25, 0, false, 1, 1)
        dao.upsertTodo(todo)
        dao.insertSession(FocusSessionEntity(
            "session", todo.id, todo.title, 1500, 1500, 25, SessionOutcome.NATURAL_COMPLETION,
            true, 1, 2, "2026-07-21", "Asia/Shanghai",
        ))
        dao.deleteTodo(todo)
        assertEquals("阅读", dao.observeSessions().first().single().todoTitle)
    }

    @Test fun migrationFromOneDefaultsExistingRowsToCountdown() {
        migrationHelper.createDatabase(MIGRATION_DB, 1).apply {
            execSQL("INSERT INTO todos (id, title, plannedMinutes, themeId, isCompleted, createdAt, updatedAt, completedAt) VALUES ('todo', '阅读', 25, 0, 0, 1, 1, NULL)")
            execSQL("INSERT INTO focus_sessions (id, todoId, todoTitle, plannedSeconds, actualFocusSeconds, creditedMinutes, outcome, countsTowardStats, startedAt, endedAt, endedLocalDate, endedZoneId) VALUES ('session', 'todo', '阅读', 1500, 1500, 25, 'NATURAL_COMPLETION', 1, 1, 2, '2026-07-21', 'Asia/Shanghai')")
            close()
        }

        migrationHelper.runMigrationsAndValidate(MIGRATION_DB, 2, true, AppDatabase.MIGRATION_1_2).use { migrated ->
            migrated.query("SELECT timerMode FROM todos WHERE id = 'todo'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("COUNTDOWN", cursor.getString(0))
            }
            migrated.query("SELECT timerMode FROM focus_sessions WHERE id = 'session'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("COUNTDOWN", cursor.getString(0))
            }
        }
    }

    @Test fun untimedTodoCanBeCompletedRepeatedlyWithoutEnteringFocusStats() = runTest {
        val dao = database.appDao()
        val repository = AppRepository(database)
        val todo = TodoEntity("ordinary", "喝水", 0, 0, false, 1, 1, timerMode = TimerMode.UNTIMED)
        dao.upsertTodo(todo)

        repository.completeUntimed(todo, now = 1_000)
        repository.completeUntimed(todo.copy(isCompleted = true, completedAt = 1_000), now = 2_000)

        val sessions = dao.getSessions()
        assertEquals(2, sessions.size)
        assertTrue(sessions.all { it.outcome == SessionOutcome.UNTIMED_COMPLETION })
        assertTrue(sessions.all { it.timerMode == TimerMode.UNTIMED })
        assertTrue(sessions.all { !it.countsTowardStats })
        assertTrue(dao.getTodo(todo.id)!!.isCompleted)
    }

    @Test fun startingAnotherTodoReturnsExistingTimer() = runTest {
        val repository = AppRepository(database)
        val first = TodoEntity("one", "阅读", 25, 0, false, 1, 1)
        val second = TodoEntity("two", "写作", 45, 1, false, 2, 2)

        val active = repository.startTimer(first)
        val repeatedStart = repository.startTimer(second)

        assertEquals(active.todoId, repeatedStart.todoId)
        assertEquals("one", database.appDao().getActiveTimer()!!.todoId)
        assertFalse(database.appDao().getActiveTimer()!!.todoId == second.id)
    }

    companion object { private const val MIGRATION_DB = "migration-v1-v2" }
}
