package com.example.submate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.submate.data.Subscription
import com.example.submate.domain.Billing
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(values: List<Subscription>, today: LocalDate) {
    val active = values.filter { it.active && (it.autoRenew || !it.date.isBefore(today)) }
    val currencies = active.map { it.currency }.distinct().sorted().ifEmpty { listOf("KRW") }
    var preferred by rememberSaveable { mutableStateOf("KRW") }
    val currency = preferred.takeIf { it in currencies } ?: currencies.first()
    val selected = active.filter { it.currency == currency }
    val monthly = selected.fold(BigDecimal.ZERO) { a, s -> a + Billing.monthlyMinor(s.amountMinor, s.cycle) }
    val groups = selected.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { Billing.monthlyMinor(it.amountMinor, it.cycle).toDouble() } }.toList().sortedByDescending { it.second }
    val colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF708393), Color(0xFF95A3AF), Color(0xFFB0BAC3), Color(0xFFC6CED5), Color(0xFF566D7F), Color(0xFF81929F), Color(0xFFD8DFE4))
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PageHeading("지출 분석", "구독료의 흐름을 읽어요") }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { currencies.forEach { code -> FilterChip(selected = currency == code, onClick = { preferred = code }, label = { Text(code) }) } } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("월 환산", Billing.money(monthly.toLong(), currency), Modifier.weight(1f))
                MetricCard("연 환산", Billing.money(monthly.multiply(BigDecimal(12)).toLong(), currency), Modifier.weight(1f))
            }
        }
        item { SectionTitle("카테고리별 구독료", "월 환산 기준") }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(144.dp), contentAlignment = Alignment.Center) {
                        val track = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(Modifier.size(136.dp)) {
                            val stroke = 14.dp.toPx()
                            val inset = stroke / 2
                            val arcSize = Size(size.width - stroke, size.height - stroke)
                            drawArc(track, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke))
                            val total = groups.sumOf { it.second }
                            var start = -90f
                            if (total > 0) groups.forEachIndexed { index, (_, value) ->
                                val sweep = (value / total * 360).toFloat()
                                drawArc(colors[index % colors.size], start, (sweep - 3).coerceAtLeast(0f), false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                                start += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("활성 구독", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${selected.size}개", fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    if (groups.isEmpty()) Text("구독을 등록하면 분석이 시작됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    groups.forEachIndexed { index, (category, total) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(9.dp).background(colors[index % colors.size], RoundedCornerShape(3.dp)))
                            Text(category, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Billing.money(total.toLong(), currency), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item { SectionTitle("앞으로 6개월", "예상 결제액") }
        item {
            val months = (0..5).map { YearMonth.from(today).plusMonths(it.toLong()) }
            val forecasts = months.map { month -> selected.sumOf { s -> Billing.occurrences(s.date, s.cycle, s.anchorDay, maxOf(today, month.atDay(1)), month.atEndOfMonth(), s.autoRenew).size * s.amountMinor } }
            val maximum = forecasts.maxOrNull()?.coerceAtLeast(1L) ?: 1L
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                months.forEachIndexed { index, month ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${month.monthValue}월", fontSize = 13.sp); Text(Billing.money(forecasts[index], currency), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                            if (forecasts[index] > 0) Box(Modifier.fillMaxWidth((forecasts[index].toFloat() / maximum).coerceIn(0.02f, 1f)).height(8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = if (index == 0) 1f else 0.35f), RoundedCornerShape(8.dp)))
                        }
                    }
                }
            }
        }
        item { Text("월·연 환산은 결제주기를 기준으로 한 추정치입니다. 이번 달 예상액은 오늘 이후 일정만 포함하며, 환율 변환은 하지 않습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (label == "월 환산") FontWeight.SemiBold else FontWeight.Medium, fontSize = 18.sp, color = if (label == "월 환산") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
