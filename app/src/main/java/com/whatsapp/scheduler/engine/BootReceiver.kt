package com.whatsapp.scheduler.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.whatsapp.scheduler.WhatsAppSchedulerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Device rebooted or package updated ($action). Rescheduling pending alarms...")

            val app = context.applicationContext as? WhatsAppSchedulerApp ?: return
            val repository = app.repository

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pendingMessages = repository.getAllPendingMessages()
                    Log.d(TAG, "Found ${pendingMessages.size} pending messages to reschedule")
                    for (msg in pendingMessages) {
                        if (msg.scheduledDateTime > System.currentTimeMillis()) {
                            repository.scheduleAlarmForMessage(msg)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms on boot", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
