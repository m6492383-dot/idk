package com.whatsapp.scheduler

import android.app.Application
import com.whatsapp.scheduler.data.local.AppDatabase
import com.whatsapp.scheduler.data.repository.SchedulerRepository
import com.whatsapp.scheduler.util.NotificationHelper

class WhatsAppSchedulerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: SchedulerRepository by lazy { SchedulerRepository(this, database.messageDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: WhatsAppSchedulerApp
            private set
    }
}
