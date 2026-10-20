package com.example.submate.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.submate.MainActivity
import com.example.submate.R
import com.example.submate.SubMateApplication
import com.example.submate.domain.Billing
import com.example.submate.domain.ReminderPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class BillingReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SubMateApplication
        return try {
            val today = LocalDate.now()
            val allowed = app.settings.state.value.notifications && NotificationManagerCompat.from(app).areNotificationsEnabled() && (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            if (allowed) app.reminderMutex.withLock {
                app.repository.dao.all().filter { it.active }.forEach { s ->
                    val reminder = ReminderPolicy.due(s, today)
                    if (reminder != null) {
                        val days = reminder.days
                        val intent = Intent(app, MainActivity::class.java).putExtra(MainActivity.SUBSCRIPTION_ID, s.id).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        val id = s.id.hashCode()
                        val pending = PendingIntent.getActivity(app, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        val timing = if (days == 0) "오늘" else "${days}일 후"
                        val notification = NotificationCompat.Builder(app, CHANNEL)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("${s.name} 결제 $timing")
                            .setContentText("${Billing.money(s.amountMinor, s.currency)} · 결제 전에 구독을 확인해 보세요.")
                            .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
                        NotificationManagerCompat.from(app).notify(id, notification)
                        app.repository.dao.markNotified(s.id, reminder.key)
                    }
                }
            }
            app.repository.refreshCalendar()
            Result.success()
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: SecurityException) { Result.success() }
        catch (_: Exception) { Result.retry() }
    }
    companion object {
        const val CHANNEL = "billing_reminders"
        fun initialize(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "구독 결제 알림", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "구독별로 설정한 결제일 사전 알림" })
        }
        fun schedule(context: Context, hour: Int, enabled: Boolean, replace: Boolean = false) {
            val manager = WorkManager.getInstance(context)
            val calendarEnabled = (context.applicationContext as SubMateApplication).settings.state.value.calendarSync
            if (!enabled && !calendarEnabled) { manager.cancelUniqueWork("daily_billing"); return }
            val now = ZonedDateTime.now()
            var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<BillingReminderWorker>(24, TimeUnit.HOURS).setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS).build()
            manager.enqueueUniquePeriodicWork("daily_billing", if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP, request)
        }
        fun checkNow(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork("billing_check_now", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<BillingReminderWorker>().build())
    }
}

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED && intent.action != Intent.ACTION_TIMEZONE_CHANGED) return
        val settings = (context.applicationContext as SubMateApplication).settings.state.value
        BillingReminderWorker.schedule(context, settings.hour, settings.notifications, true)
    }
}
