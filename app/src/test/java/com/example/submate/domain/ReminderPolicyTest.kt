package com.example.submate.domain

import com.example.submate.data.Subscription
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReminderPolicyTest {
    private val today = LocalDate.of(2026, 10, 20)
    private fun subscription(date: LocalDate = today.plusDays(1)) = Subscription(name = "구독", amountMinor = 1000, nextBillingDay = date.toEpochDay(), anchorDay = date.dayOfMonth)
    @Test fun dayBeforeReminder() { assertEquals(1, ReminderPolicy.due(subscription(), today)?.days) }
    @Test fun sameDayReminder() { assertEquals(0, ReminderPolicy.due(subscription(today), today)?.days) }
    @Test fun pausedSubscriptionHasNoReminder() { assertNull(ReminderPolicy.due(subscription().copy(active = false), today)) }
    @Test fun repeatedRunDoesNotDuplicate() {
        val value = subscription()
        val due = ReminderPolicy.due(value, today)!!
        assertNull(ReminderPolicy.due(value.copy(lastNotificationKey = due.key), today))
    }
    @Test fun disabledRemindersDoNotNotify() { assertNull(ReminderPolicy.due(subscription().copy(reminders = ""), today)) }
    @Test fun overdueOneTimeDoesNotNotify() { assertNull(ReminderPolicy.due(subscription(today.minusDays(1)).copy(autoRenew = false), today)) }
    @Test fun recurringDateAdvancesAndNotifiesNextCycle() {
        val value = subscription(LocalDate.of(2026, 9, 21))
        assertEquals(LocalDate.of(2026, 10, 21), ReminderPolicy.due(value, today)?.date)
    }
}
