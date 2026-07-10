package com.deniz.pedometer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: StepRecordEntity)

    @Query("SELECT * FROM step_records WHERE synced = 0")
    suspend fun getUnsynced(): List<StepRecordEntity>

    @Query("UPDATE step_records SET synced = 1 WHERE deviceId = :deviceId AND day = :day")
    suspend fun markSynced(deviceId: String, day: String)

    @Query("SELECT * FROM step_records WHERE deviceId = :deviceId AND day = :day LIMIT 1")
    suspend fun getForDay(deviceId: String, day: String): StepRecordEntity?

    @Query("SELECT * FROM step_records ORDER BY day DESC")
    suspend fun getAll(): List<StepRecordEntity>
}
