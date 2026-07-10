package com.deniz.pedometer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_records", primaryKeys = ["deviceId", "day"])
data class StepRecordEntity(
    val deviceId: String,
    val day: String,      // ISO date string, e.g. "2026-07-09"
    val steps: Int,
    val synced: Boolean = false
)
