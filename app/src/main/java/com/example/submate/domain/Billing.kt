package com.example.submate.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

enum class BillingCycle(val title: String, val months: Long) {
    WEEKLY("매주", 0), MONTHLY("매월", 1), QUARTERLY("3개월", 3), YEARLY("매년", 12)
}

object Billing {
    fun next(date: LocalDate, cycle: BillingCycle, anchorDay: Int): LocalDate {
        if (cycle == BillingCycle.WEEKLY) return date.plusWeeks(1)
        val month = YearMonth.from(date).plusMonths(cycle.months)
        return month.atDay(anchorDay.coerceIn(1, month.lengthOfMonth()))
    }

    fun onOrAfter(date: LocalDate, cycle: BillingCycle, anchorDay: Int, today: LocalDate): LocalDate {
        var next = date
        while (next.isBefore(today)) next = Billing.next(next, cycle, anchorDay)
        return next
    }

    fun occurrences(date: LocalDate, cycle: BillingCycle, anchorDay: Int, from: LocalDate, until: LocalDate, autoRenew: Boolean): List<LocalDate> {
        if (!autoRenew) return listOf(date).filter { !it.isBefore(from) && !it.isAfter(until) }
        val result = mutableListOf<LocalDate>()
        var current = onOrAfter(date, cycle, anchorDay, from)
        while (!current.isAfter(until)) {
            result += current
            current = next(current, cycle, anchorDay)
        }
        return result
    }

    fun monthlyMinor(amountMinor: Long, cycle: BillingCycle): BigDecimal = when (cycle) {
        BillingCycle.WEEKLY -> BigDecimal.valueOf(amountMinor).multiply(BigDecimal(52)).divide(BigDecimal(12), 2, RoundingMode.HALF_UP)
        else -> BigDecimal.valueOf(amountMinor).divide(BigDecimal.valueOf(cycle.months), 2, RoundingMode.HALF_UP)
    }

    fun parseAmount(input: String, currency: String): Long? = runCatching {
        val digits = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
        val value = input.trim().replace(",", "").toBigDecimal()
        require(value > BigDecimal.ZERO)
        require(value.scale() <= digits || value.stripTrailingZeros().scale() <= digits)
        value.movePointRight(digits).longValueExact().also { require(it <= 100_000_000_000_000L) }
    }.getOrNull()

    fun amountInput(minor: Long, currency: String): String = BigDecimal.valueOf(minor, Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)).toPlainString()

    fun money(minor: Long, currency: String): String {
        val unit = Currency.getInstance(currency)
        return NumberFormat.getCurrencyInstance(Locale.KOREA).apply {
            this.currency = unit
            minimumFractionDigits = unit.defaultFractionDigits.coerceAtLeast(0)
            maximumFractionDigits = minimumFractionDigits
        }.format(BigDecimal.valueOf(minor, unit.defaultFractionDigits.coerceAtLeast(0)))
    }
}
