package com.example.submate.domain

import java.time.LocalDate

data class ReceiptSuggestion(val service: ServiceTemplate?, val amount: String?, val currency: String, val date: LocalDate?, val cycle: BillingCycle?)

object ReceiptParser {
    fun parse(text: String): ReceiptSuggestion {
        val currency = when {
            Regex("USD|US\\$|\\$|달러", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "USD"
            Regex("JPY|엔", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "JPY"
            Regex("EUR|€", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "EUR"
            else -> "KRW"
        }
        val prefix = Regex("(?:₩|KRW|USD|US\\$|\\$|JPY|EUR|€)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val suffix = Regex("([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:원|달러|엔)")
        val amount = (prefix.find(text)?.groupValues?.get(1) ?: suffix.find(text)?.groupValues?.get(1))?.replace(",", "")
        val dateMatch = Regex("(20\\d{2})\\s*[년./-]\\s*(\\d{1,2})\\s*[월./-]\\s*(\\d{1,2})").find(text)
        val date = dateMatch?.let { runCatching { LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }.getOrNull() }
        val cycle = when {
            Regex("매년|연간|year|annual", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BillingCycle.YEARLY
            Regex("분기|3개월|quarter", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BillingCycle.QUARTERLY
            Regex("매주|weekly", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BillingCycle.WEEKLY
            Regex("매월|월간|month", RegexOption.IGNORE_CASE).containsMatchIn(text) -> BillingCycle.MONTHLY
            else -> null
        }
        return ReceiptSuggestion(ServiceCatalog.match(text), amount, currency, date, cycle)
    }
}
