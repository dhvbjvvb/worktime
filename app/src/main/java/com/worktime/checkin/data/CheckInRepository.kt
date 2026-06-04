package com.worktime.checkin.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.TimeZone

class CheckInRepository(context: Context) {
    private val dao = AppDatabase.get(context).checkInRecordDao()

    val records: Flow<List<CheckInRecordEntity>> = dao.observeAll()

    fun observeMonth(year: Int, month: Int): Flow<List<CheckInRecordEntity>> {
        val start = epochDayOf(year, month, 1)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return dao.observeRange(start, epochDayOf(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)))
    }

    suspend fun save(record: CheckInRecordDraft): Long {
        return dao.insert(record.toEntity())
    }

    suspend fun deleteByDates(dates: List<Triple<Int, Int, Int>>) {
        dao.deleteByEpochDays(dates.map { (year, month, day) -> epochDayOf(year, month, day) })
    }
}

data class CheckInRecordDraft(
    val dateEpochDay: Long,
    val shift: String,
    val workType: String,
    val leaveType: String,
    val normalHours: Double,
    val overtimeHours: Double,
    val weekendHours: Double,
    val holidayHours: Double,
    val dailyPay: Double
) {
    fun toEntity(): CheckInRecordEntity {
        return CheckInRecordEntity(
            dateEpochDay = dateEpochDay,
            shift = shift,
            workType = workType,
            leaveType = leaveType,
            normalHours = normalHours,
            overtimeHours = overtimeHours,
            weekendHours = weekendHours,
            holidayHours = holidayHours,
            dailyPay = dailyPay,
            createdAtMillis = System.currentTimeMillis()
        )
    }
}

fun epochDayOf(year: Int, month: Int, day: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
    }
    return cal.timeInMillis / MILLIS_PER_DAY
}

private const val MILLIS_PER_DAY = 86_400_000L
