package com.example.submate.integration

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.submate.data.Subscription
import com.example.submate.domain.Billing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarSync(private val context: Context) {
    private val mutex = Mutex()
    private val account = "${context.packageName}.local"
    fun permitted() = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR).all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    suspend fun sync(subscriptions: List<Subscription>, enabled: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(permitted()) { "캘린더 권한을 허용해 주세요." }
            val resolver = context.contentResolver
            val calendarId = resolver.query(CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars._ID), "${CalendarContract.Calendars.ACCOUNT_NAME}=? AND ${CalendarContract.Calendars.ACCOUNT_TYPE}=?", arrayOf(account, "LOCAL"), null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
                ?: if (enabled) createCalendar() else return@withLock
            resolver.delete(CalendarContract.Events.CONTENT_URI, "${CalendarContract.Events.CALENDAR_ID}=?", arrayOf(calendarId.toString()))
            if (!enabled) return@withLock
            val today = LocalDate.now()
            val operations = arrayListOf<ContentProviderOperation>()
            subscriptions.filter { it.active }.forEach { subscription ->
                Billing.occurrences(subscription.date, subscription.cycle, subscription.anchorDay, today, today.plusMonths(3), subscription.autoRenew).forEach { date ->
                    val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    operations += ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                        .withValue(CalendarContract.Events.TITLE, "${subscription.name} · ${Billing.money(subscription.amountMinor, subscription.currency)}")
                        .withValue(CalendarContract.Events.DESCRIPTION, "Subchive에서 등록한 결제 일정\n${subscription.note.take(500)}")
                        .withValue(CalendarContract.Events.DTSTART, start)
                        .withValue(CalendarContract.Events.DTEND, start + 86_400_000L)
                        .withValue(CalendarContract.Events.ALL_DAY, 1)
                        .withValue(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                        .build()
                }
            }
            operations.chunked(100).forEach { resolver.applyBatch(CalendarContract.AUTHORITY, ArrayList(it)) }
        }
    }

    private fun createCalendar(): Long {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "LOCAL").build()
        val values = android.content.ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, account)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, "LOCAL")
            put(CalendarContract.Calendars.NAME, "구독노트")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Subchive 결제일")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF3E5668.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, account)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, ZoneId.systemDefault().id)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        return context.contentResolver.insert(uri, values)?.let(ContentUris::parseId) ?: error("캘린더를 만들 수 없습니다.")
    }
}
