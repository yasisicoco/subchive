package com.example.submate.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    @Test fun koreanReceipt() {
        val result = ReceiptParser.parse("넷플릭스\n월간 요금 17,000원\n다음 결제일 2026년 11월 15일")
        assertEquals("netflix", result.service?.key)
        assertEquals("17000", result.amount)
        assertEquals("KRW", result.currency)
        assertEquals(LocalDate.of(2026, 11, 15), result.date)
        assertEquals(BillingCycle.MONTHLY, result.cycle)
    }
    @Test fun englishReceipt() {
        val result = ReceiptParser.parse("OpenAI ChatGPT\nUSD 19.99\n2026-12-01\nAnnual")
        assertEquals("chatgpt", result.service?.key)
        assertEquals("19.99", result.amount)
        assertEquals("USD", result.currency)
        assertEquals(BillingCycle.YEARLY, result.cycle)
    }
    @Test fun doesNotTreatDateAsAmount() { assertNull(ReceiptParser.parse("2026-11-15").amount) }
    @Test fun invalidDateIsNotApplied() { assertNull(ReceiptParser.parse("2026-02-31").date) }
    @Test fun unknownServiceRemainsUnselected() { assertNull(ReceiptParser.parse("아무개 서비스").service) }
    @Test fun wonPrefix() { assertEquals("12000", ReceiptParser.parse("₩12,000").amount) }
    @Test fun dollarSymbol() { val result = ReceiptParser.parse("Spotify $10.99"); assertEquals("1099", result.amount?.replace(".", "")); assertEquals("USD", result.currency) }
}
