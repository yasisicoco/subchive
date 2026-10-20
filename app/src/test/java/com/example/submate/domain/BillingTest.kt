package com.example.submate.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BillingTest {
    @Test fun monthEndKeepsOriginalAnchor() {
        val february = Billing.next(LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY, 31)
        assertEquals(LocalDate.of(2026, 2, 28), february)
        assertEquals(LocalDate.of(2026, 3, 31), Billing.next(february, BillingCycle.MONTHLY, 31))
    }
    @Test fun leapYearRecoversFebruary29() {
        assertEquals(LocalDate.of(2028, 2, 29), Billing.onOrAfter(LocalDate.of(2024, 2, 29), BillingCycle.YEARLY, 29, LocalDate.of(2027, 3, 1)))
    }
    @Test fun quarterlyAnchorSurvivesShortMonth() {
        assertEquals(LocalDate.of(2026, 11, 30), Billing.next(LocalDate.of(2026, 8, 31), BillingCycle.QUARTERLY, 31))
    }
    @Test fun weeklyIsSevenDays() {
        assertEquals(LocalDate.of(2027, 1, 4), Billing.next(LocalDate.of(2026, 12, 28), BillingCycle.WEEKLY, 28))
    }
    @Test fun dueTodayIsNotSkipped() {
        val today = LocalDate.of(2026, 10, 20)
        assertEquals(today, Billing.onOrAfter(today, BillingCycle.MONTHLY, 20, today))
    }
    @Test fun overdueRecurringRollsForward() {
        assertEquals(LocalDate.of(2026, 10, 31), Billing.onOrAfter(LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY, 31, LocalDate.of(2026, 10, 20)))
    }
    @Test fun occurrencesRespectMonthBounds() {
        val dates = Billing.occurrences(LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY, 31, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31), true)
        assertEquals(listOf(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)), dates)
    }
    @Test fun nonRenewingDoesNotGenerateFutureCharges() {
        assertTrue(Billing.occurrences(LocalDate.of(2026, 1, 1), BillingCycle.MONTHLY, 1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 12, 31), false).isEmpty())
    }
    @Test fun futureSubscriptionDoesNotCreatePastHistory() {
        assertTrue(Billing.occurrences(LocalDate.of(2026, 11, 1), BillingCycle.MONTHLY, 1, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), true).isEmpty())
    }
    @Test fun parsesKoreanWholeUnits() { assertEquals(17000L, Billing.parseAmount("17,000", "KRW")) }
    @Test fun parsesDollarCentsExactly() { assertEquals(1999L, Billing.parseAmount("19.99", "USD")) }
    @Test fun rejectsFractionalWon() { assertNull(Billing.parseAmount("1.5", "KRW")) }
    @Test fun rejectsFractionalCents() { assertNull(Billing.parseAmount("1.001", "USD")) }
    @Test fun rejectsNonPositiveAndOverflow() {
        listOf("0", "-1", "not money", "99999999999999999999999").forEach { assertNull(Billing.parseAmount(it, "USD")) }
    }
    @Test fun backupDisplayAmountRoundTrips() { assertEquals(1999L, Billing.parseAmount(Billing.amountInput(1999, "USD"), "USD")) }
    @Test fun yearlyIsDividedByTwelve() { assertEquals(0, BigDecimal("1000").compareTo(Billing.monthlyMinor(12000, BillingCycle.YEARLY))) }
    @Test fun weeklyUsesFiftyTwoWeeks() { assertEquals(0, BigDecimal("5200").compareTo(Billing.monthlyMinor(1200, BillingCycle.WEEKLY))) }
}
