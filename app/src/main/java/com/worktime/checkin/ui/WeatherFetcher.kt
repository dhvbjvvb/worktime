package com.worktime.checkin.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WeatherFetcher {
    private const val KEY = "01155e10462444319b0d011bd005700b"

    data class Weather(val emoji: String, val description: String, val temp: Int)

    suspend fun fetchByAutoIp(): Weather? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://devapi.qweather.com/v7/weather/now?location=auto_ip&key=$KEY")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                if (obj.optString("code") != "200") return@withContext null
                val nowObj = obj.optJSONObject("now") ?: return@withContext null
                val desc = nowObj.optString("text", "")
                val temp = nowObj.optString("temp", "").toIntOrNull() ?: return@withContext null
                val emoji = iconToEmoji(nowObj.optString("icon", ""))
                Weather(emoji, desc, temp)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun fetch(lat: Double, lon: Double): Weather? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://devapi.qweather.com/v7/weather/now?location=$lon,$lat&key=$KEY")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                if (obj.optString("code") != "200") return@withContext null
                val nowObj = obj.optJSONObject("now") ?: return@withContext null
                val desc = nowObj.optString("text", "")
                val temp = nowObj.optString("temp", "").toIntOrNull() ?: return@withContext null
                val emoji = iconToEmoji(nowObj.optString("icon", ""))
                Weather(emoji, desc, temp)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun iconToEmoji(icon: String): String = when (icon.toIntOrNull()) {
        100 -> "\u2600\uFE0F"
        101, 102 -> "\u26C5"
        103, 104 -> "\u2601\uFE0F"
        150 -> "\uD83C\uDF19"
        300, 301, 305, 306, 307, 308, 309, 310, 311, 312, 313 -> "\uD83C\uDF27\uFE0F"
        302, 303 -> "\u26C8\uFE0F"
        304, 314, 315, 316, 317, 318, 400, 401, 402, 405, 406, 407 -> "\uD83C\uDF28\uFE0F"
        403, 404 -> "\u2744\uFE0F"
        500, 501, 503 -> "\uD83C\uDF2B\uFE0F"
        502, 504, 507, 508 -> "\uD83C\uDF2A\uFE0F"
        else -> "\u2600\uFE0F"
    }
}
