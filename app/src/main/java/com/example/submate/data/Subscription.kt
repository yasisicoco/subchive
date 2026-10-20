package com.example.submate.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.submate.domain.Billing
import com.example.submate.domain.BillingCycle
import java.time.LocalDate

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String = "KRW",
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val nextBillingDay: Long,
    val anchorDay: Int,
    val serviceKey: String = "custom",
    val category: String = "기타",
    val autoRenew: Boolean = true,
    val active: Boolean = true,
    val reminders: String = "1,0",
    val cancelUrl: String = "",
    val note: String = "",
    val lastNotificationKey: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(nextBillingDay)
    fun nextDate(today: LocalDate = LocalDate.now()): LocalDate = if (autoRenew) Billing.onOrAfter(date, cycle, anchorDay, today) else date
    fun reminderOffsets(): Set<Int> = reminders.split(',').mapNotNull { it.toIntOrNull() }.filter { it in 0..30 }.toSet()
}
