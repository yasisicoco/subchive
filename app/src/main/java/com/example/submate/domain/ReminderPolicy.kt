package com.example.submate.domain

import com.example.submate.data.Subscription
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DueReminder(val date: LocalDate, val days: Int, val key: String)

object ReminderPolicy {
    fun due(subscription: Subscription, today: LocalDate): DueReminder? {
        if (!subscription.active) return null
        val date = subscription.nextDate(today)
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        val key = "$date:$days"
        return if (days in subscription.reminderOffsets() && subscription.lastNotificationKey != key) DueReminder(date, days, key) else null
    }
}
