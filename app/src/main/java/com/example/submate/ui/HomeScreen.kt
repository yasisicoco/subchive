package com.example.submate.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.submate.data.Subscription
import com.example.submate.domain.Billing
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(values: List<Subscription>, today: LocalDate, loaded: Boolean, add: () -> Unit, detail: (Long) -> Unit, model: SubMateViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("전체") }
    val active = values.filter { it.active && (it.autoRenew || !it.date.isBefore(today)) }
    val totals = active.groupBy { it.currency }.mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { total, item -> total + Billing.monthlyMinor(item.amountMinor, item.cycle) }.toLong() }
    val visible = values.filter { it.name.contains(query, true) || it.category.contains(query, true) }.filter { when (filter) { "7일 이내" -> it.active && ChronoUnit.DAYS.between(today, it.nextDate(today)) in 0..7; "일시정지" -> !it.active; else -> true } }.sortedBy { it.nextDate(today) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) model.notifications(true) else model.message("설정에서 알림 권한을 허용해 주세요.") }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        else model.message("결제 알림이 켜져 있습니다.")
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { PageHeading("Subchive", "나의 구독") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::requestNotifications) { Icon(Icons.Default.NotificationsNone, "알림 권한 설정", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Button(onClick = add, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Icon(Icons.Default.Add, "구독 추가", Modifier.size(16.dp)); Text(" 추가") }
                }
            } }
            item {
                Card(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("월 환산 구독료", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("${today.monthValue}월", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        if (totals.isEmpty()) Text("₩0", fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
                        totals.entries.sortedBy { if (it.key == "KRW") "" else it.key }.forEach { (currency, amount) -> Text(Billing.money(amount, currency), fontSize = if (totals.size == 1) 30.sp else 24.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("활성 구독 ${active.size}개", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            if (totals.size > 1) Text("· 통화별로 따로 계산", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
            }
            item {
                val upcoming = active.count { ChronoUnit.DAYS.between(today, it.nextDate(today)) in 0..7 }
                Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.NotificationsNone, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("7일 안에", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${upcoming}건 결제 예정", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (upcoming > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionTitle("구독 목록", "총 ${values.size}개") }
            item { OutlinedTextField(query, { query = it }, placeholder = { Text("서비스 또는 카테고리 검색", fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp)) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("전체", "7일 이내", "일시정지").forEach { label -> FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) }) } } }
            if (!loaded) item { CircularProgressIndicator(Modifier.padding(24.dp)) }
            else if (values.isEmpty()) item {
                EmptyPage("첫 구독을 등록하고\n다음 결제일부터 챙겨 보세요.", add, "첫 구독 등록")
                TextButton(onClick = model::sample, modifier = Modifier.fillMaxWidth()) { Text("샘플 구독 4개로 둘러보기") }
            }
            else if (visible.isEmpty()) item { Text("조건에 맞는 구독이 없습니다.", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(visible, key = { it.id }) { SubscriptionRow(it, today, { detail(it.id) }) }
        }
    }
}
