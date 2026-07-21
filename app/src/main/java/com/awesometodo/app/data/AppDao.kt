package com.awesometodo.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM todos ORDER BY updatedAt DESC")
    fun observeTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY endedAt DESC")
    fun observeSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM active_timer WHERE singletonId = 1")
    fun observeActiveTimer(): Flow<ActiveTimerEntity?>

    @Query("SELECT * FROM todos ORDER BY updatedAt DESC")
    suspend fun getTodos(): List<TodoEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY endedAt DESC")
    suspend fun getSessions(): List<FocusSessionEntity>

    @Query("SELECT * FROM active_timer WHERE singletonId = 1")
    suspend fun getActiveTimer(): ActiveTimerEntity?

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodo(id: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveTimer(timer: ActiveTimerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM active_timer")
    suspend fun clearActiveTimer()

    @Query("DELETE FROM todos")
    suspend fun clearTodos()

    @Query("DELETE FROM focus_sessions")
    suspend fun clearSessions()
}
