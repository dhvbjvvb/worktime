package com.worktime.checkin.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object HolidayFetcher {
    /** year 鈫?set of "MM-DD" strings that are public holidays */
    private val cache = mutableMapOf<Int, Set<String>>()

    suspend fun fetch(year: Int): Set<String> {
        cache[year]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/$year.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val days = JSONObject(json).optJSONArray("days") ?: return@withContext emptySet<String>()
                val dates = mutableSetOf<String>()
                for (i in 0 until days.length()) {
                    val day = days.optJSONObject(i) ?: continue
                    if (day.optBoolean("isOffDay", false)) {
                        val date = day.optString("date", "") // "YYYY-MM-DD"
                        if (date.length >= 10) {
                            dates.add(date.substring(5)) // "MM-DD"
                        }
                    }
                }
                cache[year] = dates
                dates
            } catch (_: Exception) {
                emptySet()
            }
        }
    }
}
