package com.example.submate.data

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.submate.domain.BillingCycle
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDay, id")
    fun observeAll(): Flow<List<Subscription>>
    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDay, id")
    suspend fun all(): List<Subscription>
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun find(id: Long): Subscription?
    @Insert
    suspend fun insert(subscription: Subscription): Long
    @Update
    suspend fun update(subscription: Subscription)
    @Delete
    suspend fun delete(subscription: Subscription)
    @Query("UPDATE subscriptions SET lastNotificationKey = :key WHERE id = :id")
    suspend fun markNotified(id: Long, key: String)
}

class Converters {
    @TypeConverter fun cycle(value: String): BillingCycle = BillingCycle.valueOf(value)
    @TypeConverter fun cycle(value: BillingCycle): String = value.name
}

@Database(entities = [Subscription::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptions(): SubscriptionDao
    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "submate.db").build()
    }
}
