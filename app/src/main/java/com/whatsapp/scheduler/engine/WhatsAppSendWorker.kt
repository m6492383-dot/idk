package com.whatsapp.scheduler.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whatsapp.scheduler.WhatsAppSchedulerApp
import com.whatsapp.scheduler.data.model.FailureReason
import com.whatsapp.scheduler.data.model.RepeatType
import com.whatsapp.scheduler.data.model.ScheduleStatus
import com.whatsapp.scheduler.data.model.ScheduledMessage
import com.whatsapp.scheduler.service.SendTask
import com.whatsapp.scheduler.service.WhatsAppAccessibilityService
import com.whatsapp.scheduler.util.NotificationHelper
import com.whatsapp.scheduler.util.PermissionUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URLEncoder
import kotlin.coroutines.resume

class WhatsAppSendWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val messageId = inputData.getLong(KEY_MESSAGE_ID, -1L)
        if (messageId == -1L) {
            Log.e(TAG, "Invalid message ID in worker input")
            return Result.failure()
        }

        val app = context.applicationContext as WhatsAppSchedulerApp
        val repository = app.repository

        val message = repository.getMessageById(messageId)
        if (message == null) {
            Log.e(TAG, "Scheduled message id=$messageId not found in database")
            return Result.failure()
        }

        if (message.status != ScheduleStatus.PENDING.name) {
            Log.w(TAG, "Message id=$messageId is not in PENDING state (status=${message.status}), skipping")
            return Result.success()
        }

        Log.d(TAG, "Executing send task for message id=$messageId, contact=${message.contactName}")

        // 1. Validate WhatsApp package installation
        if (!isWhatsAppInstalled(context)) {
            Log.e(TAG, "WhatsApp is not installed on device")
            repository.updateStatus(
                id = messageId,
                status = ScheduleStatus.FAILED,
                failureReason = FailureReason.WHATSAPP_NOT_INSTALLED
            )
            NotificationHelper.showFailureNotification(
                context, messageId, message.contactName, FailureReason.WHATSAPP_NOT_INSTALLED
            )
            return Result.failure()
        }

        // 2. Validate Accessibility Service enablement
        if (!PermissionUtils.isAccessibilityServiceEnabled(context)) {
            Log.e(TAG, "Accessibility service is not enabled")
            repository.updateStatus(
                id = messageId,
                status = ScheduleStatus.FAILED,
                failureReason = FailureReason.ACCESSIBILITY_DISABLED
            )
            NotificationHelper.showFailureNotification(
                context, messageId, message.contactName, FailureReason.ACCESSIBILITY_DISABLED
            )
            return Result.failure()
        }

        val accessibilityService = WhatsAppAccessibilityService.instance
        if (accessibilityService == null) {
            Log.e(TAG, "Accessibility service instance unavailable")
            repository.updateStatus(
                id = messageId,
                status = ScheduleStatus.FAILED,
                failureReason = FailureReason.ACCESSIBILITY_DISABLED
            )
            NotificationHelper.showFailureNotification(
                context, messageId, message.contactName, FailureReason.ACCESSIBILITY_DISABLED
            )
            return Result.failure()
        }

        // 3. Post sending notification
        NotificationHelper.showSendingNotification(context, messageId, message.contactName)

        // 4. Launch WhatsApp chat via Deep Link Intent
        val launched = launchWhatsAppChat(context, message.phoneNumber, message.message)
        if (!launched) {
            repository.updateStatus(
                id = messageId,
                status = ScheduleStatus.FAILED,
                failureReason = FailureReason.CONTACT_NOT_FOUND
            )
            NotificationHelper.showFailureNotification(
                context, messageId, message.contactName, FailureReason.CONTACT_NOT_FOUND
            )
            return Result.failure()
        }

        // 5. Suspend until Accessibility Service completes sending
        val sendResult = suspendCancellableCoroutine { continuation ->
            val task = SendTask(
                messageId = message.id,
                contactName = message.contactName,
                phoneNumber = message.phoneNumber,
                messageText = message.message,
                attachmentPath = message.attachmentPath
            ) { success, failureReason ->
                if (continuation.isActive) {
                    continuation.resume(Pair(success, failureReason))
                }
            }

            accessibilityService.executeSendTask(task)
        }

        val (success, failureReason) = sendResult

        if (success) {
            Log.d(TAG, "Message id=$messageId sent successfully!")
            repository.updateStatus(id = messageId, status = ScheduleStatus.SENT)
            NotificationHelper.showSuccessNotification(context, messageId, message.contactName)

            // Handle recurring schedule creation if repeatType is DAILY, WEEKLY, or MONTHLY
            handleRecurringSchedule(repository, message)

            return Result.success()
        } else {
            Log.e(TAG, "Failed sending message id=$messageId: $failureReason")
            repository.updateStatus(
                id = messageId,
                status = ScheduleStatus.FAILED,
                failureReason = failureReason ?: FailureReason.UNKNOWN_ERROR
            )
            NotificationHelper.showFailureNotification(
                context, messageId, message.contactName, failureReason
            )
            return Result.failure()
        }
    }

    private fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(WhatsAppAccessibilityService.WHATSAPP_PKG, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                pm.getPackageInfo(WhatsAppAccessibilityService.WHATSAPP_BUSINESS_PKG, 0)
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun launchWhatsAppChat(context: Context, phoneNumber: String, messageText: String): Boolean {
        return try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
            val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching WhatsApp intent", e)
            false
        }
    }

    private suspend fun handleRecurringSchedule(
        repository: com.whatsapp.scheduler.data.repository.SchedulerRepository,
        currentMessage: ScheduledMessage
    ) {
        val repeatTypeEnum = try {
            RepeatType.valueOf(currentMessage.repeatType)
        } catch (e: Exception) {
            RepeatType.ONCE
        }

        if (repeatTypeEnum != RepeatType.ONCE) {
            val nextExecutionTime = repository.calculateNextScheduleTime(
                currentMessage.scheduledDateTime,
                repeatTypeEnum
            )
            val nextSchedule = currentMessage.copy(
                id = 0,
                status = ScheduleStatus.PENDING.name,
                failureReason = null,
                scheduledDateTime = nextExecutionTime,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.insertSchedule(nextSchedule)
            Log.d(TAG, "Created recurring schedule id=$newId for repeatType=$repeatTypeEnum at $nextExecutionTime")
        }
    }

    companion object {
        private const val TAG = "WhatsAppSendWorker"
        const val KEY_MESSAGE_ID = "key_message_id"
    }
}
