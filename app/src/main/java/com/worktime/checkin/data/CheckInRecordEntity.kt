package com.worktime.checkin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_in_records")
data class CheckInRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val shift: String,
    val workType: String,
    val leaveType: String,
    val normalHours: Double,
    val overtimeHours: Double,
    val weekendHours: Double,
    val holidayHours: Double,
    val dailyPay: Double,
    val createdAtMillis: Long
)
