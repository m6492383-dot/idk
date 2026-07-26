package com.whatsapp.scheduler.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.whatsapp.scheduler.data.model.ScheduledMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY scheduled_datetime ASC")
    fun getAllMessages(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM messages WHERE status = :status ORDER BY scheduled_datetime ASC")
    fun getMessagesByStatus(status: String): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM messages WHERE status = 'PENDING' AND scheduled_datetime <= :currentTime")
    suspend fun getDuePendingMessages(currentTime: Long): List<ScheduledMessage>

    @Query("SELECT * FROM messages WHERE status = 'PENDING'")
    suspend fun getAllPendingMessages(): List<ScheduledMessage>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): ScheduledMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ScheduledMessage): Long

    @Update
    suspend fun updateMessage(message: ScheduledMessage)

    @Query("UPDATE messages SET status = :status, failure_reason = :failureReason, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, failureReason: String?, updatedAt: Long)

    @Delete
    suspend fun deleteMessage(message: ScheduledMessage)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
}
