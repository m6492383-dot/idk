package com.whatsapp.scheduler.data.repository

import android.content.Context
import com.whatsapp.scheduler.data.local.MessageDao
import com.whatsapp.scheduler.data.model.RepeatType
import com.whatsapp.scheduler.data.model.ScheduleStatus
import com.whatsapp.scheduler.data.model.ScheduledMessage
import com.whatsapp.scheduler.engine.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SchedulerRepository(
    private val context: Context,
    private val messageDao: MessageDao
) {
    private val alarmScheduler = AlarmScheduler(context)

    fun getAllMessages(): Flow<List<ScheduledMessage>> = messageDao.getAllMessages()

    fun getMessagesByStatus(status: String): Flow<List<ScheduledMessage>> =
        messageDao.getMessagesByStatus(status)

    suspend fun getMessageById(id: Long): ScheduledMessage? = messageDao.getMessageById(id)

    suspend fun getAllPendingMessages(): List<ScheduledMessage> = messageDao.getAllPendingMessages()

    suspend fun insertSchedule(message: ScheduledMessage): Long {
        val insertedId = messageDao.insertMessage(message)
        val savedMessage = message.copy(id = insertedId)
        if (savedMessage.status == ScheduleStatus.PENDING.name &&
            savedMessage.scheduledDateTime > System.currentTimeMillis()
        ) {
            alarmScheduler.scheduleAlarm(savedMessage)
        }
        return insertedId
    }

    suspend fun updateSchedule(message: ScheduledMessage) {
        val updatedMessage = message.copy(updatedAt = System.currentTimeMillis())
        messageDao.updateMessage(updatedMessage)

        if (updatedMessage.status == ScheduleStatus.PENDING.name) {
            if (updatedMessage.scheduledDateTime > System.currentTimeMillis()) {
                alarmScheduler.scheduleAlarm(updatedMessage)
            } else {
                alarmScheduler.cancelAlarm(updatedMessage.id)
            }
        } else {
            alarmScheduler.cancelAlarm(updatedMessage.id)
        }
    }

    suspend fun deleteSchedule(message: ScheduledMessage) {
        alarmScheduler.cancelAlarm(message.id)
        messageDao.deleteMessage(message)
    }

    suspend fun deleteScheduleById(id: Long) {
        alarmScheduler.cancelAlarm(id)
        messageDao.deleteMessageById(id)
    }

    suspend fun updateStatus(id: Long, status: ScheduleStatus, failureReason: String? = null) {
        messageDao.updateStatus(
            id = id,
            status = status.name,
            failureReason = failureReason,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun duplicateSchedule(message: ScheduledMessage): Long {
        val newSchedule = message.copy(
            id = 0,
            status = ScheduleStatus.PENDING.name,
            failureReason = null,
            scheduledDateTime = System.currentTimeMillis() + (5 * 60 * 1000),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return insertSchedule(newSchedule)
    }

    fun scheduleAlarmForMessage(message: ScheduledMessage) {
        alarmScheduler.scheduleAlarm(message)
    }

    fun cancelAlarmForMessage(messageId: Long) {
        alarmScheduler.cancelAlarm(messageId)
    }

    fun calculateNextScheduleTime(currentTime: Long, repeatType: RepeatType): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
        }
        when (repeatType) {
            RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatType.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RepeatType.ONCE -> return currentTime
        }
        return calendar.timeInMillis
    }
}
