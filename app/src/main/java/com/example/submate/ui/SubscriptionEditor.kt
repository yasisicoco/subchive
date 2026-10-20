package com.example.submate.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.submate.data.Subscription
import com.example.submate.domain.Billing
import com.example.submate.domain.BillingCycle
import com.example.submate.domain.ReceiptParser
import com.example.submate.domain.ServiceCatalog
import com.example.submate.integration.ReceiptScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionEditor(original: Subscription?, model: SubMateViewModel, back: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var name by rememberSaveable { mutableStateOf(original?.name.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(original?.let { Billing.amountInput(it.amountMinor, it.currency) }.orEmpty()) }
    var currency by rememberSaveable { mutableStateOf(original?.currency ?: "KRW") }
    var cycleName by rememberSaveable { mutableStateOf((original?.cycle ?: BillingCycle.MONTHLY).name) }
    var dateDay by rememberSaveable { mutableStateOf((original?.nextDate(today) ?: today).toEpochDay()) }
    var dateChanged by rememberSaveable { mutableStateOf(false) }
    var serviceKey by rememberSaveable { mutableStateOf(original?.serviceKey ?: "custom") }
    var category by rememberSaveable { mutableStateOf(original?.category ?: "기타") }
    var autoRenew by rememberSaveable { mutableStateOf(original?.autoRenew ?: true) }
    var reminders by rememberSaveable { mutableStateOf(original?.reminders ?: "1,0") }
    var note by rememberSaveable { mutableStateOf(original?.note.orEmpty()) }
    var cancelUrl by rememberSaveable { mutableStateOf(original?.cancelUrl.orEmpty()) }
    var catalog by rememberSaveable { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var recognized by rememberSaveable { mutableStateOf<String?>(null) }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun scan(uri: Uri) {
        scope.launch {
            scanning = true
            try { recognized = ReceiptScanner.scan(context, uri) }
            catch (cancel: CancellationException) { throw cancel }
            catch (problem: Exception) { model.message(problem.message ?: "이미지를 읽지 못했습니다.") }
            finally {
                scanning = false
                if (uri.toString() == photoUri) { File(context.cacheDir, "photos").listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 60_000 }?.forEach(File::delete) }
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) scan(uri) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success) photoUri?.let { scan(Uri.parse(it)) } }
    LazyColumn(modifier = Modifier.imePadding().testTag("editorList"), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeading(if (original == null) "새로운 구독" else "구독 수정", back) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Text("결제 화면에서 빠르게 입력", fontWeight = FontWeight.Medium, fontSize = 13.sp) }
                    Text("이미지의 서비스명·금액·날짜를 읽고, 확인 후 입력에 적용해요.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { gallery.launch("image/*") }, enabled = !scanning, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoLibrary, null, Modifier.size(16.dp)); Text("  이미지") }
                        OutlinedButton(onClick = {
                            runCatching {
                                val file = File.createTempFile("receipt-", ".jpg", File(context.cacheDir, "photos").apply { mkdirs() })
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                photoUri = uri.toString()
                                camera.launch(uri)
                            }.onFailure { model.message("카메라 앱을 열 수 없습니다.") }
                        }, enabled = !scanning, modifier = Modifier.weight(1f)) { Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp)); Text("  촬영") }
                    }
                    if (scanning) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(Modifier.size(18.dp)); Text("기기에서 이미지를 읽고 있어요", fontSize = 12.sp) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clickable { catalog = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceBadge(serviceKey, name)
                Column(Modifier.weight(1f)) { Text("서비스 로고 선택", fontWeight = FontWeight.Medium); Text("서비스명과 카테고리를 자동으로 채워요", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Icon(Icons.Default.ExpandMore, null)
            }
        }
        item { OutlinedTextField(name, { if (it.length <= 100) name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("서비스명") }, placeholder = { Text("예: 넷플릭스") }, singleLine = true) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(amount, { amount = it }, modifier = Modifier.weight(1f), label = { Text("결제 금액") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            ChoiceField(currency, ServiceCatalog.currencies, { currency = it })
        } }
        item { ChoiceField(category, ServiceCatalog.categories, { category = it }, "카테고리") }
        item { SectionTitle("결제 일정"); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { BillingCycle.entries.forEach { cycle -> FilterChip(selected = cycleName == cycle.name, onClick = { cycleName = cycle.name }, label = { Text(cycle.title) }) } } }
        item { SettingCard("다음 결제일", LocalDate.ofEpochDay(dateDay).toString(), { datePicker = true }) { Icon(Icons.Default.CalendarMonth, null) } }
        item { SettingCard("자동갱신", "결제일이 지나면 다음 주기로 계산해요.", { autoRenew = !autoRenew }) { Switch(checked = autoRenew, onCheckedChange = { autoRenew = it }) } }
        item {
            SectionTitle("미리 알려드릴게요")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0, 1, 3, 7, 14).forEach { day ->
                val selected = day in reminders.split(',').mapNotNull(String::toIntOrNull)
                FilterChip(selected = selected, onClick = { val set = reminders.split(',').mapNotNull(String::toIntOrNull).toMutableSet(); if (selected) set.remove(day) else set.add(day); reminders = set.sortedDescending().joinToString(",") }, label = { Text(if (day == 0) "당일" else "${day}일 전") })
            } }
        }
        item { OutlinedTextField(cancelUrl, { if (it.length <= 2000) cancelUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("해지 페이지 링크 (선택)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true) }
        item { OutlinedTextField(note, { if (it.length <= 2000) note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("메모 (선택)") }, minLines = 3, maxLines = 6) }
        if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
        item {
            Button(onClick = {
                val minor = Billing.parseAmount(amount, currency)
                if (name.isBlank()) { error = "서비스명을 입력해 주세요."; return@Button }
                if (minor == null) { error = "금액과 소수점 자릿수를 확인해 주세요."; return@Button }
                saving = true
                val date = LocalDate.ofEpochDay(dateDay)
                val base = original ?: Subscription(name = "", amountMinor = 1, nextBillingDay = dateDay, anchorDay = date.dayOfMonth)
                val value = base.copy(name = name.trim(), amountMinor = minor, currency = currency, cycle = BillingCycle.valueOf(cycleName), nextBillingDay = dateDay, anchorDay = if (!dateChanged && original != null) original.anchorDay else date.dayOfMonth, serviceKey = serviceKey, category = category, autoRenew = autoRenew, reminders = reminders, cancelUrl = cancelUrl.trim(), note = note.trim())
                model.save(value, back).invokeOnCompletion { saving = false }
            }, enabled = !saving && !scanning, modifier = Modifier.fillMaxWidth().height(48.dp)) { if (saving) CircularProgressIndicator(Modifier.size(20.dp)) else Text("구독 저장", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (datePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateDay * 86_400_000L, yearRange = 2000..2100)
        DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dateDay = it / 86_400_000L; dateChanged = true }; datePicker = false }) { Text("선택") } }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("취소") } }) { DatePicker(state = state) }
    }
    if (catalog) ServicePicker(onDismiss = { catalog = false }) { service -> serviceKey = service.key; if (service.key != "custom") name = service.name; category = service.category; catalog = false }
    recognized?.let { raw ->
        val suggestion = ReceiptParser.parse(raw)
        AlertDialog(onDismissRequest = { recognized = null }, title = { Text("인식한 내용을 확인해 주세요") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(raw, { recognized = it }, label = { Text("인식된 텍스트") }, maxLines = 6)
                Text(listOfNotNull(suggestion.service?.name, suggestion.amount?.let { "$it ${suggestion.currency}" }, suggestion.date?.toString(), suggestion.cycle?.title).joinToString(" · ").ifBlank { "자동 추출된 항목이 없습니다. 직접 입력해 주세요." }, fontSize = 13.sp)
                Text("틀린 부분은 저장 전에 수정해 주세요. 금액과 날짜는 직접 확인해야 합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, confirmButton = { TextButton(onClick = {
            suggestion.service?.let { serviceKey = it.key; name = it.name; category = it.category }
            suggestion.amount?.let { amount = it; currency = suggestion.currency }
            suggestion.date?.let { dateDay = it.toEpochDay(); dateChanged = true }
            suggestion.cycle?.let { cycleName = it.name }
            recognized = null
        }) { Text("입력에 적용") } }, dismissButton = { TextButton(onClick = { recognized = null }) { Text("취소") } })
    }
}

@Composable
private fun ChoiceField(value: String, options: List<String>, selected: (String) -> Unit, label: String = "통화") {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }) { Text("$label: $value"); Icon(Icons.Default.ExpandMore, null) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { selected(option); expanded = false }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicePicker(onDismiss: () -> Unit, selected: (com.example.submate.domain.ServiceTemplate) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = ServiceCatalog.services.filter { it.name.contains(query, true) || it.category.contains(query, true) || it.aliases.any { alias -> alias.contains(query, true) } }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("어떤 서비스를 구독하세요?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(query, { query = it }, placeholder = { Text("서비스 또는 카테고리 검색") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), singleLine = true)
            LazyColumn(Modifier.height(350.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered) { service ->
                    Row(Modifier.fillMaxWidth().clickable { selected(service) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ServiceBadge(service.key, service.name, 32)
                        Column { Text(service.name, fontWeight = FontWeight.Medium, fontSize = 13.sp); Text(service.category, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}
