package com.worktime.checkin.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.charset.Charset

data class SalarySettings(
    val selectedCalcMethod: String = METHOD_STANDARD,
    val baseSalary: String = "3000",
    val normalHourly: String = "18",
    val overtimeRate: String = "1.5",
    val overtimeHourly: String = "27",
    val weekendRate: String = "2",
    val weekendHourly: String = "36",
    val holidayRate: String = "3",
    val holidayHourly: String = "54",
    val dailyHourly: String = "",
    val holidayDailyHourly: String = ""
)

const val METHOD_STANDARD = "正班 + 加班"
const val METHOD_BASE_PLUS_OVERTIME = "底薪 + 加班"
const val METHOD_COMPREHENSIVE = "综合工作制"
const val METHOD_HOURLY = "小时计算"

class SalarySettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.appDataStore

    val settings: Flow<SalarySettings> = dataStore.data.map { preferences ->
        preferences.toSalarySettings()
    }

    suspend fun update(transform: (SalarySettings) -> SalarySettings) {
        dataStore.edit { preferences ->
            val next = transform(preferences.toSalarySettings())
            preferences[Keys.selectedCalcMethod] = next.selectedCalcMethod
            preferences[Keys.dailyHourly] = next.dailyHourly
            preferences[Keys.holidayDailyHourly] = next.holidayDailyHourly
            preferences[Keys.legacyMigrated] = "true"
            preferences.saveMethodValues(next.selectedCalcMethod, next)
        }
    }

    suspend fun setSelectedCalcMethod(method: String) {
        dataStore.edit { preferences ->
            val current = preferences.toSalarySettings()
            preferences.saveMethodValues(current.selectedCalcMethod, current)
            preferences[Keys.legacyMigrated] = "true"
            preferences[Keys.selectedCalcMethod] = method
        }
    }

    private fun Preferences.toSalarySettings(): SalarySettings {
        val method = (this[Keys.selectedCalcMethod] ?: METHOD_STANDARD).normalizedCalcMethod()
        val methodKeys = keysForMethod(method)
        val useLegacyFallback = this[Keys.legacyMigrated] != "true"
        return SalarySettings(
            selectedCalcMethod = method,
            baseSalary = this[methodKeys.baseSalary] ?: this.legacyValue(Keys.legacyBaseSalary, useLegacyFallback) ?: "3000",
            normalHourly = this[methodKeys.normalHourly] ?: this.legacyValue(Keys.legacyNormalHourly, useLegacyFallback) ?: "18",
            overtimeRate = this[methodKeys.overtimeRate] ?: this.legacyValue(Keys.legacyOvertimeRate, useLegacyFallback) ?: "1.5",
            overtimeHourly = this[methodKeys.overtimeHourly] ?: this.legacyValue(Keys.legacyOvertimeHourly, useLegacyFallback) ?: "27",
            weekendRate = this[methodKeys.weekendRate] ?: this.legacyValue(Keys.legacyWeekendRate, useLegacyFallback) ?: "2",
            weekendHourly = this[methodKeys.weekendHourly] ?: this.legacyValue(Keys.legacyWeekendHourly, useLegacyFallback) ?: "36",
            holidayRate = this[methodKeys.holidayRate] ?: this.legacyValue(Keys.legacyHolidayRate, useLegacyFallback) ?: "3",
            holidayHourly = this[methodKeys.holidayHourly] ?: this.legacyValue(Keys.legacyHolidayHourly, useLegacyFallback) ?: "54",
            dailyHourly = this[Keys.dailyHourly] ?: "",
            holidayDailyHourly = this[Keys.holidayDailyHourly] ?: ""
        )
    }

    private fun Preferences.legacyValue(key: Preferences.Key<String>, enabled: Boolean): String? {
        return if (enabled) this[key] else null
    }

    private fun MutablePreferences.saveMethodValues(method: String, settings: SalarySettings) {
        val methodKeys = keysForMethod(method)
        this[methodKeys.baseSalary] = settings.baseSalary
        this[methodKeys.normalHourly] = settings.normalHourly
        this[methodKeys.overtimeRate] = settings.overtimeRate
        this[methodKeys.overtimeHourly] = settings.overtimeHourly
        this[methodKeys.weekendRate] = settings.weekendRate
        this[methodKeys.weekendHourly] = settings.weekendHourly
        this[methodKeys.holidayRate] = settings.holidayRate
        this[methodKeys.holidayHourly] = settings.holidayHourly
    }

    private data class MethodSalaryKeys(
        val baseSalary: Preferences.Key<String>,
        val normalHourly: Preferences.Key<String>,
        val overtimeRate: Preferences.Key<String>,
        val overtimeHourly: Preferences.Key<String>,
        val weekendRate: Preferences.Key<String>,
        val weekendHourly: Preferences.Key<String>,
        val holidayRate: Preferences.Key<String>,
        val holidayHourly: Preferences.Key<String>
    )

    private fun keysForMethod(method: String): MethodSalaryKeys {
        val prefix = when (method.normalizedCalcMethod()) {
            METHOD_BASE_PLUS_OVERTIME -> "base_plus_overtime"
            METHOD_COMPREHENSIVE -> "comprehensive"
            METHOD_HOURLY -> "hourly"
            else -> "standard"
        }
        return MethodSalaryKeys(
            baseSalary = stringPreferencesKey("salary_${prefix}_base_salary"),
            normalHourly = stringPreferencesKey("salary_${prefix}_normal_hourly"),
            overtimeRate = stringPreferencesKey("salary_${prefix}_overtime_rate"),
            overtimeHourly = stringPreferencesKey("salary_${prefix}_overtime_hourly"),
            weekendRate = stringPreferencesKey("salary_${prefix}_weekend_rate"),
            weekendHourly = stringPreferencesKey("salary_${prefix}_weekend_hourly"),
            holidayRate = stringPreferencesKey("salary_${prefix}_holiday_rate"),
            holidayHourly = stringPreferencesKey("salary_${prefix}_holiday_hourly")
        )
    }

    private object Keys {
        val selectedCalcMethod = stringPreferencesKey("salary_selected_calc_method")
        val legacyMigrated = stringPreferencesKey("salary_legacy_migrated")
        val dailyHourly = stringPreferencesKey("salary_daily_hourly")
        val holidayDailyHourly = stringPreferencesKey("salary_holiday_daily_hourly")

        val legacyBaseSalary = stringPreferencesKey("salary_base_salary")
        val legacyNormalHourly = stringPreferencesKey("salary_normal_hourly")
        val legacyOvertimeRate = stringPreferencesKey("salary_overtime_rate")
        val legacyOvertimeHourly = stringPreferencesKey("salary_overtime_hourly")
        val legacyWeekendRate = stringPreferencesKey("salary_weekend_rate")
        val legacyWeekendHourly = stringPreferencesKey("salary_weekend_hourly")
        val legacyHolidayRate = stringPreferencesKey("salary_holiday_rate")
        val legacyHolidayHourly = stringPreferencesKey("salary_holiday_hourly")
    }
}

private fun String.normalizedCalcMethod(): String {
    return when (this) {
        legacyMojibake(METHOD_STANDARD) -> METHOD_STANDARD
        legacyMojibake(METHOD_BASE_PLUS_OVERTIME) -> METHOD_BASE_PLUS_OVERTIME
        legacyMojibake(METHOD_HOURLY) -> METHOD_HOURLY
        else -> this
    }
}

private val LegacyMojibakeCharset = Charset.forName("GB18030")

private fun legacyMojibake(text: String): String {
    return String(text.toByteArray(Charsets.UTF_8), LegacyMojibakeCharset)
}
