package com.example.submate

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.submate.data.Subscription
import com.example.submate.worker.BillingReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AndroidIntegrationTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(*(listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR) + if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()).toTypedArray())
    private val app get() = ApplicationProvider.getApplicationContext<SubMateApplication>()
    @Before fun prepare() = runBlocking {
        app.settings.update { it.copy(calendarSync = false, notifications = true) }
        app.repository.dao.all().forEach { app.repository.dao.delete(it) }
        app.getSystemService(NotificationManager::class.java).cancelAll()
    }
    @After fun cleanup() = runBlocking {
        app.repository.dao.all().forEach { app.repository.dao.delete(it) }
        app.getSystemService(NotificationManager::class.java).cancelAll()
        if (app.calendar.permitted()) app.calendar.sync(emptyList(), false)
        app.settings.update { it.copy(calendarSync = false) }
    }
    @Test fun workerPostsNotificationOnlyOncePerDueKey() = runBlocking {
        val today = LocalDate.now()
        val id = app.repository.save(Subscription(name = "알림 검증", amountMinor = 12000, nextBillingDay = today.toEpochDay(), anchorDay = today.dayOfMonth))
        val firstWorker = TestListenableWorkerBuilder<BillingReminderWorker>(app).build()
        assertEquals(ListenableWorker.Result.success(), firstWorker.doWork())
        val manager = app.getSystemService(NotificationManager::class.java)
        val first = manager.activeNotifications.first { it.id == id.hashCode() }
        assertNotNull(first.notification.contentIntent)
        val key = app.repository.dao.find(id)!!.lastNotificationKey
        assertTrue(key.endsWith(":0"))
        val secondWorker = TestListenableWorkerBuilder<BillingReminderWorker>(app).build()
        assertEquals(ListenableWorker.Result.success(), secondWorker.doWork())
        assertEquals(first.postTime, manager.activeNotifications.first { it.id == id.hashCode() }.postTime)
    }
    @Test fun notificationsOffDoesNotMarkOrPost() = runBlocking {
        val today = LocalDate.now()
        val id = app.repository.save(Subscription(name = "꺼진 알림", amountMinor = 12000, nextBillingDay = today.toEpochDay(), anchorDay = today.dayOfMonth))
        app.settings.update { it.copy(notifications = false) }
        assertEquals(ListenableWorker.Result.success(), TestListenableWorkerBuilder<BillingReminderWorker>(app).build().doWork())
        assertEquals("", app.repository.dao.find(id)?.lastNotificationKey)
        assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty())
    }
    @Test fun ownedCalendarSyncCreatesAndClearsEvents() = runBlocking {
        val day = LocalDate.now().plusDays(2)
        val value = Subscription(id = 77, name = "캘린더 검증", amountMinor = 15000, nextBillingDay = day.toEpochDay(), anchorDay = day.dayOfMonth)
        app.calendar.sync(listOf(value), true)
        val resolver = app.contentResolver
        val id = resolver.query(CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars._ID), "account_name=? AND account_type=?", arrayOf("${app.packageName}.local", "LOCAL"), null)!!.use { cursor -> assertTrue(cursor.moveToFirst()); cursor.getLong(0) }
        val before = resolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID), "calendar_id=?", arrayOf(id.toString()), null)!!.use { it.count }
        assertTrue(before >= 3)
        app.calendar.sync(emptyList(), false)
        val after = resolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID), "calendar_id=?", arrayOf(id.toString()), null)!!.use { it.count }
        assertEquals(0, after)
    }
}
