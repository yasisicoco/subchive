package com.example.submate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.submate.data.Subscription
import com.example.submate.domain.Billing
import com.example.submate.domain.ServiceCatalog
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(values: List<Subscription>, today: LocalDate, detail: (Long) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.ofEpochDay(selectedDay)
    val events = remember(values, month) { values.filter { it.active }.flatMap { s -> Billing.occurrences(s.date, s.cycle, s.anchorDay, month.atDay(1), month.atEndOfMonth(), s.autoRenew).map { it to s } }.groupBy({ it.first }, { it.second }) }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PageHeading("일정 관리", "결제일을 놓치지 않게") }
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { monthText = month.minusMonths(1).toString(); selectedDay = month.minusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Default.ChevronLeft, "이전 달") }
                        Text("${month.year}년 ${month.monthValue}월", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { monthText = YearMonth.from(today).toString(); selectedDay = today.toEpochDay() }) { Text("오늘") }
                        IconButton(onClick = { monthText = month.plusMonths(1).toString(); selectedDay = month.plusMonths(1).atDay(1).toEpochDay() }) { Icon(Icons.Default.ChevronRight, "다음 달") }
                    }
                    Row(Modifier.fillMaxWidth()) { listOf("일", "월", "화", "수", "목", "금", "토").forEach { day -> Box(Modifier.weight(1f).padding(vertical = 6.dp), contentAlignment = Alignment.Center) { Text(day, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                    val leading = month.atDay(1).dayOfWeek.value % 7
                    val cells = ((leading + month.lengthOfMonth() + 6) / 7) * 7
                    (0 until cells step 7).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            (0..6).forEach { column ->
                                val day = row + column - leading + 1
                                val valid = day in 1..month.lengthOfMonth()
                                val date = if (valid) month.atDay(day) else null
                                val chosen = date == selected
                                Box(Modifier.weight(1f).height(48.dp).then(if (valid) Modifier.clickable { selectedDay = date!!.toEpochDay() } else Modifier).padding(3.dp).background(if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                    if (date != null) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(day.toString(), fontWeight = if (date == today || chosen) FontWeight.SemiBold else FontWeight.Normal, color = if (chosen) MaterialTheme.colorScheme.onPrimary else if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { events[date].orEmpty().take(3).forEach { Box(Modifier.size(3.dp).background(if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, CircleShape)) } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("${selected.monthValue}월 ${selected.dayOfMonth}일", "${events[selected].orEmpty().size}건 예정") }
        if (events[selected].isNullOrEmpty()) item { Text("이 날은 예정된 결제가 없어요.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp)) }
        items(events[selected].orEmpty(), key = { it.id }) { SubscriptionRow(it, today, { detail(it.id) }, selected) }
        item { Spacer(Modifier.height(20.dp)); Text("현재 등록된 결제일과 주기로 계산한 예정 일정입니다. 과거 결제 내역을 수집하지 않습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
