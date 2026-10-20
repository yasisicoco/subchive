package com.example.submate

import android.app.Application
import com.example.submate.data.AppDatabase
import com.example.submate.data.SettingsStore
import com.example.submate.data.SubscriptionRepository
import com.example.submate.integration.CalendarSync
import com.example.submate.worker.BillingReminderWorker

class SubMateApplication : Application() {
    val reminderMutex = kotlinx.coroutines.sync.Mutex()
    val settings by lazy { SettingsStore(this) }
    val calendar by lazy { CalendarSync(this) }
    val repository by lazy { SubscriptionRepository(AppDatabase.create(this), settings, calendar) }
    override fun onCreate() {
        super.onCreate()
        BillingReminderWorker.initialize(this)
        BillingReminderWorker.schedule(this, settings.state.value.hour, settings.state.value.notifications)
    }
}
