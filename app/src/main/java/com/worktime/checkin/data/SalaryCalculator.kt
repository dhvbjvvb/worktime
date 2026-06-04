package com.worktime.checkin.data

import java.util.Calendar
import java.util.TimeZone
import java.nio.charset.Charset

fun calculateDailySalary(
    record: CheckInRecordEntity?,
    settings: SalarySettings,
    publicHolidays: Set<String> = emptySet()
): Double {
    if (record == null) return 0.0
    if (record.leaveType.isNotEmpty()) return 0.0
    if (record.dailyPay > 0.0) return record.dailyPay

    val normalHourly = settings.normalHourly.toMoney()
        .takeIf { it > 0.0 }
        ?: settings.baseSalary.toMoney().monthlyBaseToHourly()
    val overtimeHourly = settings.overtimeHourly.toMoney()
        .takeIf { it > 0.0 }
        ?: normalHourly * settings.overtimeRate.toMoney().takeIf { it > 0.0 }.orDefault(1.5)
    val weekendHourly = settings.weekendHourly.toMoney()
        .takeIf { it > 0.0 }
        ?: normalHourly * settings.weekendRate.toMoney().takeIf { it > 0.0 }.orDefault(2.0)
    val holidayHourly = settings.holidayHourly.toMoney()
        .takeIf { it > 0.0 }
        ?: normalHourly * settings.holidayRate.toMoney().takeIf { it > 0.0 }.orDefault(3.0)
    val dailyHourly = settings.dailyHourly.toMoney()
    val holidayDailyHourly = settings.holidayDailyHourly.toMoney()
    val dayKind = record.effectiveDayKind(publicHolidays)

    return when (settings.selectedCalcMethod) {
        METHOD_STANDARD -> calculateStandard(record, dayKind, normalHourly, overtimeHourly, weekendHourly, holidayHourly)
        METHOD_BASE_PLUS_OVERTIME -> calculateBasePlusOvertime(record, dayKind, overtimeHourly, weekendHourly, holidayHourly)
        METHOD_COMPREHENSIVE -> calculateComprehensive(record, dayKind, normalHourly, overtimeHourly, weekendHourly, holidayHourly)
        METHOD_HOURLY -> calculateHourly(record, dayKind, dailyHourly, holidayDailyHourly)
        else -> calculateStandard(record, dayKind, normalHourly, overtimeHourly, weekendHourly, holidayHourly)
    }
}

fun formatMoney(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun calculateStandard(
    record: CheckInRecordEntity,
    dayKind: DayKind,
    normalHourly: Double,
    overtimeHourly: Double,
    weekendHourly: Double,
    holidayHourly: Double
): Double {
    return when (dayKind) {
        DayKind.Holiday -> record.totalWorkHours() * holidayHourly
        DayKind.Weekend -> record.totalWorkHours() * weekendHourly
        DayKind.Workday -> record.normalHoursByWorkType() * normalHourly +
            record.overtimeHoursByWorkType() * overtimeHourly
    }
}

private fun calculateBasePlusOvertime(
    record: CheckInRecordEntity,
    dayKind: DayKind,
    overtimeHourly: Double,
    weekendHourly: Double,
    holidayHourly: Double
): Double {
    return when (dayKind) {
        DayKind.Holiday -> record.totalWorkHours() * holidayHourly
        DayKind.Weekend -> record.totalWorkHours() * weekendHourly
        DayKind.Workday -> record.overtimeHoursByWorkType() * overtimeHourly
    }
}

private fun calculateComprehensive(
    record: CheckInRecordEntity,
    dayKind: DayKind,
    normalHourly: Double,
    overtimeHourly: Double,
    weekendHourly: Double,
    holidayHourly: Double
): Double {
    return calculateStandard(record, dayKind, normalHourly, overtimeHourly, weekendHourly, holidayHourly)
}

private fun calculateHourly(
    record: CheckInRecordEntity,
    dayKind: DayKind,
    dailyHourly: Double,
    holidayDailyHourly: Double
): Double {
    val holidayHourly = if (holidayDailyHourly > 0.0) holidayDailyHourly else dailyHourly
    return when (dayKind) {
        DayKind.Holiday -> record.totalWorkHours() * holidayHourly
        else -> record.totalWorkHours() * dailyHourly
    }
}

private fun String.toMoney(): Double = toDoubleOrNull() ?: 0.0

private fun Double.monthlyBaseToHourly(): Double {
    return if (this > 0.0) this / 21.75 / 8.0 else 0.0
}

private fun Double?.orDefault(default: Double): Double = this ?: default

private fun CheckInRecordEntity.normalHoursByWorkType(): Double {
    return if (workType.isNormalWorkType()) normalHours else 0.0
}

private fun CheckInRecordEntity.overtimeHoursByWorkType(): Double {
    return if (workType.isNormalWorkType()) overtimeHours else 0.0
}

private fun CheckInRecordEntity.weekendHoursByWorkType(): Double {
    return if (workType.isWeekendWorkType()) weekendHours else 0.0
}

private fun CheckInRecordEntity.holidayHoursByWorkType(): Double {
    return if (workType.isHolidayWorkType()) holidayHours else 0.0
}

private fun CheckInRecordEntity.totalWorkHours(): Double {
    return normalHours + overtimeHours + weekendHours + holidayHours
}

private fun CheckInRecordEntity.effectiveDayKind(publicHolidays: Set<String>): DayKind {
    return when {
        isPublicHoliday(publicHolidays) || workType.isHolidayWorkType() -> DayKind.Holiday
        isWeekend() || workType.isWeekendWorkType() -> DayKind.Weekend
        else -> DayKind.Workday
    }
}

private fun String.isNormalWorkType(): Boolean = this == "正班" || this == legacyMojibake("正班")

private fun String.isWeekendWorkType(): Boolean = this == "周末" || this == legacyMojibake("周末")

private fun String.isHolidayWorkType(): Boolean = this == "节假" || this == legacyMojibake("节假")

private val LegacyMojibakeCharset = Charset.forName("GB18030")

private fun legacyMojibake(text: String): String {
    return String(text.toByteArray(Charsets.UTF_8), LegacyMojibakeCharset)
}

private fun CheckInRecordEntity.isPublicHoliday(publicHolidays: Set<String>): Boolean {
    return monthDay() in publicHolidays
}

private fun CheckInRecordEntity.isWeekend(): Boolean {
    val cal = utcCalendar()
    val weekday = cal.get(Calendar.DAY_OF_WEEK)
    return weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY
}

private fun CheckInRecordEntity.monthDay(): String {
    val cal = utcCalendar()
    return "%02d-%02d".format(
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

private fun CheckInRecordEntity.utcCalendar(): Calendar {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateEpochDay * MILLIS_PER_DAY
    }
}

private enum class DayKind {
    Workday,
    Weekend,
    Holiday
}

private const val MILLIS_PER_DAY = 86_400_000L
