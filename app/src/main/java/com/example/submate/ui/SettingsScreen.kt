package com.example.submate.ui

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.example.submate.BuildConfig
import com.example.submate.integration.AdsConsent
import com.example.submate.integration.PurchaseManager

@Composable
fun SettingsScreen(model: SubMateViewModel, consent: AdsConsent, purchases: PurchaseManager) {
    val settings by model.settings.collectAsStateWithLifecycle()
    val purchaseStatus by purchases.status.collectAsStateWithLifecycle()
    val privacyRequired by consent.privacyRequired.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    var osNotifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { osNotifications = NotificationManagerCompat.from(context).areNotificationsEnabled() }
    var importDialog by rememberSaveable { mutableStateOf(false) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) model.export(uri) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) model.import(uri) }
    val calendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions -> if (permissions.values.all { it } && model.app.calendar.permitted()) model.calendar(true) else model.message("캘린더 권한을 허용해야 연결할 수 있습니다.") }
    val notification = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> osNotifications = granted; if (granted) model.notifications(true) else model.message("알림 권한이 허용되지 않았습니다.") }
    fun enableNotifications() {
        if (settings.notifications && NotificationManagerCompat.from(context).areNotificationsEnabled()) model.notifications(false)
        else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notification.launch(Manifest.permission.POST_NOTIFICATIONS)
        else if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        else model.notifications(true)
    }
    fun toggleCalendar() {
        if (settings.calendarSync) model.calendar(false)
        else if (model.app.calendar.permitted()) model.calendar(true)
        else calendar.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
    }
    LazyColumn(modifier = Modifier.testTag("settingsList"), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { PageHeading("Subchive", "나에게 맞는 Subchive") }
        item { SectionTitle("알림과 일정") }
        item { SettingCard("결제일 알림", "구독마다 선택한 사전 알림을 보내요.", ::enableNotifications) { Switch(checked = settings.notifications && osNotifications, onCheckedChange = { enableNotifications() }) } }
        item { SettingCard("알림 확인 시간", "매일 ${settings.hour}시 · 절전 상태에 따라 지연될 수 있어요.", { TimePickerDialog(context, { _, hour, _ -> model.notifications(settings.notifications, hour) }, settings.hour, 0, true).show() }) }
        item { SettingCard("지금 알림 확인", "오늘 알림 대상만 확인해요. 같은 알림은 반복하지 않아요.", model::checkReminders) }
        item { SettingCard("캘린더 자동 연결", "별도 로컬 캘린더에 다음 3개월 일정을 표시해요.", ::toggleCalendar) { Switch(checked = settings.calendarSync, onCheckedChange = { toggleCalendar() }) } }
        if (settings.calendarMessage.isNotBlank()) item { Text(settings.calendarMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionTitle("데이터와 화면") }
        item { SettingCard("백업 파일 저장", "구독 정보가 포함된 JSON 파일을 저장해요.", { backup.launch("Subchive-${java.time.LocalDate.now()}.json") }) }
        item { SettingCard("백업 파일 가져오기", "기존 구독은 유지하고 백업의 구독을 추가해요.", { importDialog = true }) }
        item { SettingCard("화면 테마", when (settings.darkMode) { "light" -> "밝게"; "dark" -> "어둡게"; else -> "시스템 설정" }, { model.theme(when (settings.darkMode) { "system" -> "light"; "light" -> "dark"; else -> "system" }) }) }
        if (BuildConfig.BILLING_ENABLED) {
            item { SectionTitle("광고와 구매") }
            item { SettingCard(if (settings.purchased) "광고 제거 구매 완료" else "광고 제거", purchaseStatus, { if (!settings.purchased) activity?.let(purchases::buy) }) }
            item { SettingCard("구매 복원", "Google Play 계정의 구매를 확인해요.", { purchases.connect() }) }
        }
        if (BuildConfig.ADS_ENABLED && privacyRequired) item { SettingCard("광고 개인정보 선택", "광고 동의 설정을 다시 확인해요.", consent::privacy) }
        item { SectionTitle("Subchive ${BuildConfig.VERSION_NAME}") }
        item { Text("구독 정보는 이 기기의 앱 저장소에 보관됩니다. 앱을 삭제하면 데이터도 삭제되므로 백업을 먼저 저장해 주세요. 캘린더 앱에 직접 추가한 일정은 해당 캘린더의 동기화 설정을 따릅니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text(if (BuildConfig.ADS_ENABLED) "광고 기능이 켜진 빌드입니다. Google 광고 서비스가 네트워크와 기기 정보를 사용할 수 있습니다." else "광고 요청과 인앱 결제는 기본 빌드에서 꺼져 있습니다. 이미지 인식은 APK에 포함된 모델로 처리합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(24.dp)) }
    }
    if (importDialog) AlertDialog(onDismissRequest = { importDialog = false }, title = { Text("백업을 가져올까요?") }, text = { Text("백업에 있는 구독을 현재 목록에 추가합니다. 같은 파일을 다시 가져오면 구독이 중복될 수 있습니다.") }, confirmButton = { TextButton(onClick = { importDialog = false; restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("파일 선택") } }, dismissButton = { TextButton(onClick = { importDialog = false }) { Text("취소") } })
}
