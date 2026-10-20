package com.example.submate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.submate.data.Subscription
import com.example.submate.domain.Billing
import com.example.submate.domain.ServiceCatalog
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun PageHeading(eyebrow: String, title: String, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(title, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
        }
        action()
    }
}

@Composable
fun BackHeading(title: String, back: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun ServiceBadge(key: String, name: String, size: Int = 36) {
    val logo = ServiceLogos.resource(key)
    val shape = RoundedCornerShape((size / 4).dp)
    Box(Modifier.size(size.dp).clip(shape).background(if (logo != null) Color.White else MaterialTheme.colorScheme.surfaceVariant).border(1.dp, if (logo != null) Color(0xFFE5E7EB) else MaterialTheme.colorScheme.outlineVariant, shape), contentAlignment = Alignment.Center) {
        if (logo != null) Image(painterResource(logo), contentDescription = "$name 로고", contentScale = ContentScale.Fit, modifier = Modifier.padding((size * 0.18f).dp).size(size.dp))
        else Text(name.take(1).ifBlank { "+" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (size / 2.8).sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SubscriptionRow(value: Subscription, today: LocalDate, onClick: () -> Unit, occurrence: LocalDate? = null) {
    val date = occurrence ?: value.nextDate(today)
    val days = ChronoUnit.DAYS.between(today, date)
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ServiceBadge(value.serviceKey, value.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(value.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${value.category} · ${value.cycle.title}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Billing.money(value.amountMinor, value.currency), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(when { !value.active -> "일시정지"; days < 0 -> "결제일 지남"; days == 0L -> "오늘 결제"; days == 1L -> "내일 결제"; else -> "${date.monthValue}.${date.dayOfMonth} · D-$days" }, fontSize = 11.sp, color = if (value.active && days in 0..3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, trailing: String = "") {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (trailing.isNotEmpty()) Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
fun EmptyPage(text: String, action: () -> Unit, button: String = "돌아가기") {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = action) { Text(button) }
    }
}

@Composable
fun SettingCard(title: String, subtitle: String, onClick: () -> Unit, trailing: @Composable () -> Unit = { Icon(Icons.Default.ChevronRight, null) }) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp, end = 8.dp))
            }
            trailing()
        }
    }
}
