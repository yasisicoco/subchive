package com.example.submate.ui

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.submate.data.Subscription
import com.example.submate.domain.Billing
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DetailScreen(value: Subscription?, today: LocalDate, loaded: Boolean, model: SubMateViewModel, back: () -> Unit, edit: () -> Unit) {
    if (value == null) { EmptyPage(if (loaded) "구독을 찾을 수 없습니다." else "불러오는 중입니다.", back); return }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    fun open(intent: Intent) { runCatching { context.startActivity(intent) }.onFailure { model.message("이 작업을 처리할 앱이 없습니다.") } }
    LazyColumn(modifier = Modifier.testTag("detailList"), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { BackHeading("구독 상세", back) { IconButton(onClick = edit) { Icon(Icons.Default.Edit, "구독 수정") }; IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.DeleteOutline, "구독 삭제") } } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServiceBadge(value.serviceKey, value.name, 52)
                    Text(value.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("${value.category} · ${value.cycle.title}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Billing.money(value.amountMinor, value.currency), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { DetailLine("다음 결제일", value.nextDate(today).toString()); DetailLine("반복 결제", if (value.autoRenew) "자동갱신" else "한 번만"); DetailLine("상태", if (value.active) "활성" else "일시정지"); DetailLine("결제 알림", value.reminderOffsets().sortedDescending().joinToString(" · ") { if (it == 0) "당일" else "${it}일 전" }.ifBlank { "알림 없음" }) }
        item { SectionTitle("나에게 필요한 메모"); Text(value.note.ifBlank { "등록된 메모가 없습니다." }, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (value.cancelUrl.isNotBlank()) item { SettingCard("서비스 해지 페이지", "서비스의 웹사이트로 이동합니다.", { open(Intent(Intent.ACTION_VIEW, Uri.parse(value.cancelUrl))) }) }
        item { SettingCard("캘린더에 한 번 추가", "휴대폰 캘린더 앱에서 확인하고 저장해요.", {
            val date = value.nextDate(today).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            open(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE, "${value.name} 결제").putExtra(CalendarContract.Events.DESCRIPTION, "${Billing.money(value.amountMinor, value.currency)}\n${value.note}").putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, value.nextDate(today).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()).putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true))
        }) }
        item { OutlinedButton(onClick = { model.toggleActive(value) }, modifier = Modifier.fillMaxWidth()) { Text(if (value.active) "구독 일시정지" else "구독 다시 활성화") }; Spacer(Modifier.height(20.dp)) }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("구독을 삭제할까요?") }, text = { Text("${value.name}의 등록 정보와 앱에서 만든 연결 일정이 삭제됩니다.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; model.delete(value, back) }) { Text("삭제", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } })
}

@Composable
private fun DetailLine(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp)) }
}
