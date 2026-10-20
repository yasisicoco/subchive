package com.example.submate.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.submate.SubMateApplication
import com.example.submate.data.Subscription
import com.example.submate.domain.BillingCycle
import com.example.submate.domain.ServiceCatalog
import com.example.submate.worker.BillingReminderWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SubMateViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as SubMateApplication
    val repository = app.repository
    private val loadedMutable = MutableStateFlow(false)
    val loaded = loadedMutable.asStateFlow()
    val subscriptions = repository.subscriptions.onEach { loadedMutable.value = true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = app.settings.state
    private val todayMutable = MutableStateFlow(LocalDate.now())
    val today = todayMutable.asStateFlow()
    private val eventsMutable = MutableSharedFlow<String>()
    val events = eventsMutable.asSharedFlow()
    fun refreshDate() { todayMutable.value = LocalDate.now() }
    fun message(text: String) { viewModelScope.launch { eventsMutable.emit(text) } }
    fun save(value: Subscription, done: () -> Unit) = viewModelScope.launch {
        try { repository.save(value); done(); message("구독을 저장했습니다.") }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { message(error.message ?: "저장하지 못했습니다.") }
    }
    fun delete(value: Subscription, done: () -> Unit) = viewModelScope.launch {
        try { repository.delete(value); done(); message("구독을 삭제했습니다.") }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { message(error.message ?: "삭제하지 못했습니다.") }
    }
    fun toggleActive(value: Subscription) = save(value.copy(active = !value.active)) {}
    fun notifications(enabled: Boolean, hour: Int = settings.value.hour) {
        app.settings.update { it.copy(notifications = enabled, hour = hour) }
        BillingReminderWorker.schedule(app, hour, enabled, true)
    }
    fun calendar(enabled: Boolean) = viewModelScope.launch {
        try {
            repository.setCalendar(enabled)
            BillingReminderWorker.schedule(app, settings.value.hour, settings.value.notifications)
        }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { message(error.message ?: "캘린더를 연결하지 못했습니다.") }
    }
    fun theme(value: String) = app.settings.update { it.copy(darkMode = value) }
    fun checkReminders() { BillingReminderWorker.checkNow(app); message("오늘 대상 구독의 알림을 확인합니다.") }
    fun export(uri: Uri) = viewModelScope.launch {
        try {
            val text = repository.exportJson()
            withContext(Dispatchers.IO) { app.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) } ?: error("파일을 열 수 없습니다.") }
            message("백업 파일을 저장했습니다.")
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { message(error.message ?: "백업에 실패했습니다.") }
    }
    fun import(uri: Uri) = viewModelScope.launch {
        try {
            val text = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 2_000_000) { "백업 파일이 너무 큽니다." }
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                require(bytes.size <= 2_000_000) { "백업 파일이 너무 큽니다." }
                bytes.toString(Charsets.UTF_8)
            } ?: error("파일을 열 수 없습니다.") }
            val count = repository.importJson(text)
            message("구독 ${count}개를 추가했습니다.")
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { message(error.message ?: "백업을 가져오지 못했습니다.") }
    }
    fun sample() = viewModelScope.launch {
        if (repository.dao.all().isNotEmpty()) { message("빈 목록에서만 샘플을 추가할 수 있습니다."); return@launch }
        try {
            listOf("netflix" to 17000L, "spotify" to 10900L, "chatgpt" to 29000L, "icloud" to 3300L).forEachIndexed { index, (key, amount) ->
                val service = ServiceCatalog.get(key)
                val date = today.value.plusDays((index * 5 + 1).toLong())
                repository.save(Subscription(name = service.name, amountMinor = amount, nextBillingDay = date.toEpochDay(), anchorDay = date.dayOfMonth, serviceKey = key, category = service.category, cycle = BillingCycle.MONTHLY))
            }
            message("샘플 구독 4개를 추가했습니다. 언제든 수정·삭제할 수 있습니다.")
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { message(error.message ?: "샘플을 추가하지 못했습니다.") }
    }
}
