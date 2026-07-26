package com.whatsapp.scheduler.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RepeatType {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class ScheduleStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED
}

object FailureReason {
    const val WHATSAPP_NOT_INSTALLED = "WhatsApp not installed"
    const val CONTACT_NOT_FOUND = "Contact not found"
    const val ATTACHMENT_MISSING = "Attachment missing"
    const val ACCESSIBILITY_DISABLED = "Accessibility disabled"
    const val PERMISSION_DENIED = "Permission denied"
    const val USER_CANCELLED = "User cancelled"
    const val SEND_BUTTON_NOT_DETECTED = "Send button not detected"
    const val TIMEOUT_WAITING_FOR_UI = "Timeout waiting for UI"
    const val UNKNOWN_ERROR = "Unknown error"
}

@Entity(tableName = "messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "contact_name")
    val contactName: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "attachment_path")
    val attachmentPath: String? = null,

    @ColumnInfo(name = "scheduled_datetime")
    val scheduledDateTime: Long,

    @ColumnInfo(name = "repeat_type")
    val repeatType: String = RepeatType.ONCE.name,

    @ColumnInfo(name = "status")
    val status: String = ScheduleStatus.PENDING.name,

    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
