package com.awesometodo.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() = database.close()

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
}
