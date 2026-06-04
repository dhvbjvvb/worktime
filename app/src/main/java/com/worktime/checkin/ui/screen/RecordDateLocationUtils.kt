package com.worktime.checkin.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.worktime.checkin.data.CheckInRecordDraft
import com.worktime.checkin.data.CheckInRepository
import com.worktime.checkin.data.epochDayOf
import com.worktime.checkin.ui.HolidayFetcher
import com.worktime.checkin.ui.WeatherFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

import kotlin.math.ceil

private val PrimaryBlue = Color(0xFF3478F6)
private val WarmOrange = Color(0xFFC47B29)
private val DangerRed = Color(0xFFC94A4A)
private val SoftGreen = Color(0xFF2E8B57)

@OptIn(ExperimentalMaterial3Api::class)

fun calendarCells(year: Int, month: Int): List<Int?> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val leadingEmptyDays = (firstDayOfWeek + 5) % 7
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = MutableList<Int?>(leadingEmptyDays) { null }
    for (day in 1..daysInMonth) cells += day
    val rows = ceil(cells.size / 7.0).toInt().coerceAtLeast(6)
    while (cells.size < rows * 7) cells += null
    return cells
}

fun selectedDaysFor(
    year: Int,
    month: Int,
    includeWeekdays: Boolean,
    includeWeekends: Boolean,
    holidays: Set<String> = emptySet()
): Set<Int> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val isAll = includeWeekdays && includeWeekends
    return (1..lastDay).filter { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        val weekend = weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY
        val isHoliday = String.format("%02d-%02d", month, day) in holidays
        // 全选不过滤；否则排除节假日
        if (!isAll && isHoliday) return@filter false
        (weekend && includeWeekends) || (!weekend && includeWeekdays)
    }.toSet()
}

fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}

fun getLastLocation(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val provider = lm.getProviders(true).firstOrNull() ?: return null
    val loc = lm.getLastKnownLocation(provider) ?: return null
    return loc.latitude to loc.longitude
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
suspend fun requestSingleLocation(context: Context): Pair<Double, Double>? {
    return withContext(Dispatchers.IO) {
        withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm == null) { cont.resume(null) {}; return@suspendCancellableCoroutine }
                val providers = lm.getProviders(true)
                if (providers.isEmpty()) { cont.resume(null) {}; return@suspendCancellableCoroutine }
                var listener: android.location.LocationListener? = null
                listener = android.location.LocationListener { loc ->
                    cont.resume(loc.latitude to loc.longitude) {}
                    providers.forEach { lm.removeUpdates(listener!!) }
                }
                cont.invokeOnCancellation { providers.forEach { lm.removeUpdates(listener!!) } }
                providers.forEach { lm.requestLocationUpdates(it, 0L, 0f, listener!!) }
            }
        }
    }
}

fun moveMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month - 1)
    cal.add(Calendar.MONTH, delta)
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH) + 1
}

fun weekdayText(cal: Calendar): String {
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "\u5468\u4E00"
        Calendar.TUESDAY -> "\u5468\u4E8C"
        Calendar.WEDNESDAY -> "\u5468\u4E09"
        Calendar.THURSDAY -> "\u5468\u56DB"
        Calendar.FRIDAY -> "\u5468\u4E94"
        Calendar.SATURDAY -> "\u5468\u516D"
        else -> "\u5468\u65E5"
    }
}

fun monthKey(year: Int, month: Int): String = "$year-$month"

@Composable
fun flatCardElevation() = CardDefaults.cardElevation(defaultElevation = 0.dp)



