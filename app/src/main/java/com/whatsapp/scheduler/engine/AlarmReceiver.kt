package com.whatsapp.scheduler.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(AlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) {
            Log.e(TAG, "Alarm received with invalid message ID")
            return
        }

        Log.d(TAG, "Alarm triggered for message ID: $messageId. Handing off to WorkManager...")

        val inputData = Data.Builder()
            .putLong(WhatsAppSendWorker.KEY_MESSAGE_ID, messageId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WhatsAppSendWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "send_whatsapp_msg_$messageId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}
