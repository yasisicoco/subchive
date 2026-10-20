package com.example.submate

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.submate.data.AppDatabase
import com.example.submate.data.SettingsStore
import com.example.submate.data.Subscription
import com.example.submate.data.SubscriptionRepository
import com.example.submate.integration.CalendarSync
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: SubscriptionRepository
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<SubMateApplication>()
        val settings = SettingsStore(context).apply { update { it.copy(calendarSync = false) } }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = SubscriptionRepository(db, settings, CalendarSync(context))
    }
    @After fun close() = db.close()
    private fun value() = Subscription(name = "테스트 구독", amountMinor = 17000, nextBillingDay = LocalDate.of(2026, 10, 31).toEpochDay(), anchorDay = 31)
    @Test fun persistedCreateUpdateDelete() = runBlocking {
        val id = repository.save(value())
        assertEquals(17000L, repository.dao.find(id)?.amountMinor)
        repository.save(repository.dao.find(id)!!.copy(name = "수정한 구독", amountMinor = 19000))
        assertEquals("수정한 구독", repository.dao.find(id)?.name)
        repository.delete(repository.dao.find(id)!!)
        assertNull(repository.dao.find(id))
    }
    @Test fun backupRoundTripsAndAppends() = runBlocking {
        repository.save(value().copy(currency = "USD", amountMinor = 1999, note = "한글 메모"))
        val backup = repository.exportJson()
        assertEquals(1, repository.importJson(backup))
        val all = repository.dao.all()
        assertEquals(2, all.size)
        assertNotEquals(all[0].id, all[1].id)
        assertEquals("한글 메모", all[1].note)
        assertEquals(1999L, all[1].amountMinor)
    }
    @Test fun invalidBackupDoesNotPartiallyInsert() = runBlocking {
        repository.save(value())
        val root = org.json.JSONObject(repository.exportJson())
        val array = root.getJSONArray("subscriptions")
        array.put(org.json.JSONObject(array.getJSONObject(0).toString()).put("amountMinor", -1))
        assertTrue(runCatching { repository.importJson(root.toString()) }.isFailure)
        assertEquals(1, repository.dao.all().size)
    }
    @Test fun blocksNonWebCancelLinks() = runBlocking {
        assertTrue(runCatching { repository.save(value().copy(cancelUrl = "javascript:alert(1)")) }.isFailure)
        assertTrue(repository.dao.all().isEmpty())
    }
    @Test fun changingSubscriptionResetsReminderKey() = runBlocking {
        val id = repository.save(value())
        repository.dao.markNotified(id, "2026-10-31:1")
        val original = repository.dao.find(id)!!
        repository.save(original.copy(amountMinor = 12000))
        assertEquals("", repository.dao.find(id)?.lastNotificationKey)
    }
}
