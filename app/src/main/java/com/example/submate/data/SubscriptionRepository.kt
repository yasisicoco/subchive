package com.example.submate.data

import androidx.room.withTransaction
import com.example.submate.domain.BillingCycle
import com.example.submate.domain.ServiceCatalog
import com.example.submate.integration.CalendarSync
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.LocalDate

class SubscriptionRepository(val database: AppDatabase, private val settings: SettingsStore, private val calendar: CalendarSync) {
    val dao = database.subscriptions()
    val subscriptions = dao.observeAll()
    private val mutation = Mutex()

    suspend fun save(value: Subscription): Long {
        validate(value)
        val id = mutation.withLock {
            if (value.id == 0L) dao.insert(value) else {
                check(dao.find(value.id) != null) { "구독을 찾을 수 없습니다." }
                dao.update(value.copy(lastNotificationKey = ""))
                value.id
            }
        }
        refreshCalendar()
        return id
    }

    suspend fun delete(value: Subscription) {
        mutation.withLock { dao.delete(value) }
        refreshCalendar()
    }

    suspend fun refreshCalendar() {
        if (!settings.state.value.calendarSync) return
        try {
            calendar.sync(dao.all(), true)
            settings.update { it.copy(calendarMessage = "다음 3개월 결제 일정 동기화 완료") }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { settings.update { it.copy(calendarMessage = error.message ?: "캘린더 동기화에 실패했습니다.") } }
    }

    suspend fun setCalendar(enabled: Boolean) {
        if (enabled) {
            check(calendar.permitted()) { "캘린더 권한을 허용해 주세요." }
            calendar.sync(dao.all(), true)
            settings.update { it.copy(calendarSync = true, calendarMessage = "다음 3개월 결제 일정 동기화 완료") }
        } else {
            if (calendar.permitted()) calendar.sync(emptyList(), false)
            settings.update { it.copy(calendarSync = false, calendarMessage = "자동 동기화를 껐습니다.") }
        }
    }

    suspend fun exportJson(): String {
        val array = JSONArray()
        dao.all().forEach { s ->
            array.put(JSONObject().apply {
                put("name", s.name); put("amountMinor", s.amountMinor); put("currency", s.currency)
                put("cycle", s.cycle.name); put("nextBillingDay", s.nextBillingDay); put("anchorDay", s.anchorDay)
                put("serviceKey", s.serviceKey); put("category", s.category); put("autoRenew", s.autoRenew)
                put("active", s.active); put("reminders", s.reminders); put("cancelUrl", s.cancelUrl); put("note", s.note)
            })
        }
        return JSONObject().put("format", "submate-backup").put("version", 1).put("subscriptions", array).toString(2)
    }

    suspend fun importJson(text: String): Int {
        require(text.length <= 2_000_000) { "백업 파일이 너무 큽니다." }
        val root = JSONObject(text)
        require(root.optString("format") == "submate-backup" && root.optInt("version") == 1) { "지원하지 않는 백업 형식입니다." }
        val array = root.getJSONArray("subscriptions")
        require(array.length() <= 1000) { "한 번에 최대 1,000개까지 가져올 수 있습니다." }
        val values = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Subscription(name = item.getString("name"), amountMinor = item.getLong("amountMinor"), currency = item.getString("currency"), cycle = BillingCycle.valueOf(item.getString("cycle")), nextBillingDay = item.getLong("nextBillingDay"), anchorDay = item.getInt("anchorDay"), serviceKey = item.optString("serviceKey", "custom"), category = item.optString("category", "기타"), autoRenew = item.getBoolean("autoRenew"), active = item.getBoolean("active"), reminders = item.optString("reminders", "1,0"), cancelUrl = item.optString("cancelUrl"), note = item.optString("note")).also(::validate)
        }
        mutation.withLock { database.withTransaction { values.forEach { dao.insert(it) } } }
        refreshCalendar()
        return values.size
    }

    private fun validate(value: Subscription) {
        require(value.name.isNotBlank() && value.name.length <= 100) { "서비스명은 1~100자로 입력해 주세요." }
        require(value.currency in ServiceCatalog.currencies && value.amountMinor in 1..100_000_000_000_000L) { "금액 또는 통화가 올바르지 않습니다." }
        require(value.date in LocalDate.of(2000, 1, 1)..LocalDate.of(2100, 12, 31) && value.anchorDay in 1..31) { "결제일을 확인해 주세요." }
        require(value.note.length <= 2000 && value.cancelUrl.length <= 2000 && value.category in ServiceCatalog.categories) { "입력 내용을 확인해 주세요." }
        require(value.reminders.length <= 30 && value.reminders.split(',').all { it.isEmpty() || it.toIntOrNull() in 0..30 }) { "알림 설정을 확인해 주세요." }
        if (value.cancelUrl.isNotBlank()) require(runCatching { URI(value.cancelUrl).let { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() } }.getOrDefault(false)) { "해지 링크는 https:// 또는 http://로 입력해 주세요." }
    }
}
