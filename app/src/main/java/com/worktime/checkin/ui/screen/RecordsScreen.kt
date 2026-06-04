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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.worktime.checkin.data.CheckInRecordEntity
import com.worktime.checkin.data.CheckInRepository
import com.worktime.checkin.data.SalarySettings
import com.worktime.checkin.data.SalarySettingsRepository
import com.worktime.checkin.data.calculateDailySalary
import com.worktime.checkin.data.epochDayOf
import com.worktime.checkin.data.formatMoney
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

@Composable
fun RecordsScreen(
    displayedYear: Int,
    displayedMonth: Int,
    checkInDate: Triple<Int, Int, Int>,
    onYearMonthChange: (Int, Int) -> Unit,
    onDateSelected: (Triple<Int, Int, Int>) -> Unit,
    onCalendarClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onCheckInClick: (Triple<Int, Int, Int>) -> Unit = {}
) {
    val today = remember {
        Calendar.getInstance().let { cal ->
            Triple(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
    val context = LocalContext.current
    val checkInRepository = remember { CheckInRepository(context.applicationContext) }
    val salaryRepository = remember { SalarySettingsRepository(context.applicationContext) }
    val salarySettings by salaryRepository.settings.collectAsState(initial = SalarySettings())
    val monthRecords by remember(displayedYear, displayedMonth) {
        checkInRepository.observeMonth(displayedYear, displayedMonth)
    }.collectAsState(initial = emptyList())
    var holidays by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(displayedYear) {
        holidays = HolidayFetcher.fetch(displayedYear)
    }
    val daySummaries = remember(monthRecords, displayedYear, displayedMonth) {
        monthRecords.toCalendarDaySummaries(displayedYear, displayedMonth)
    }
    val selectedRecord = remember(monthRecords, checkInDate) {
        monthRecords.latestRecordFor(checkInDate)
    }
    val dailySalary = remember(selectedRecord, salarySettings, holidays) {
        calculateDailySalary(selectedRecord, salarySettings, holidays)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(50.dp),
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = {
                    Column {
                        Text(
                            text = "\u8BB0\u5F55",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "\u8BB0\u5F55\u6BCF\u65E5\u85AA\u916C\u4E0E\u6253\u5361",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CalendarCard(
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                    selectedDays = selectedDaysForCheckInDate(checkInDate, displayedYear, displayedMonth),
                    displayedYear = displayedYear,
                    displayedMonth = displayedMonth,
                    daySummaries = daySummaries,
                    neutralSelection = true,
                    highlightedDay = today,
                    onYearMonthChange = onYearMonthChange,
                    onDayToggle = { day ->
                        onDateSelected(Triple(displayedYear, displayedMonth, day))
                    },
                    onDayLongClick = onCalendarClick
                )
            }

            item {
                CheckInCard(
                    checkInDate = checkInDate,
                    dailySalary = dailySalary,
                    onCheckInClick = { onCheckInClick(checkInDate) }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallMenuCard(
                        icon = Icons.Default.CalendarMonth,
                        title = "\u65E5\u5386",
                        value = "\u591A\u65E5\u671F\u7F16\u8F91",
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onCalendarClick
                    )
                    SmallMenuCard(
                        icon = Icons.Default.Payments,
                        title = "\u62A5\u8868",
                        value = "\u67E5\u770B\u6708\u5EA6\u660E\u7EC6",
                        color = WarmOrange,
                        modifier = Modifier.weight(1f),
                        onClick = onReportClick
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun List<CheckInRecordEntity>.latestRecordFor(
    date: Triple<Int, Int, Int>
): CheckInRecordEntity? {
    val epochDay = epochDayOf(date.first, date.second, date.third)
    return filter { it.dateEpochDay == epochDay }
        .maxByOrNull { it.createdAtMillis }
}

fun List<CheckInRecordEntity>.toCalendarDaySummaries(
    year: Int,
    month: Int
): Map<Int, CalendarDaySummary> {
    val firstEpochDay = epochDayOf(year, month, 1)
    return groupBy { (it.dateEpochDay - firstEpochDay + 1).toInt() }
        .filterKeys { it > 0 }
        .mapValues { (_, records) ->
            val latest = records.maxBy { it.createdAtMillis }
            CalendarDaySummary(
                normalHours = if (latest.workType == "\u6B63\u73ED") latest.normalHours else 0.0,
                overtimeHours = when (latest.workType) {
                    "\u6B63\u73ED" -> latest.overtimeHours
                    "\u5468\u672B" -> latest.weekendHours
                    "\u8282\u5047" -> latest.holidayHours
                    else -> 0.0
                },
                dailyPay = if (latest.workType == "\u65E5\u7ED3") latest.dailyPay else 0.0,
                leaveType = latest.leaveType
            )
        }
        .filterValues { it.hasValue }
}

fun selectedDaysForCheckInDate(
    checkInDate: Triple<Int, Int, Int>,
    displayedYear: Int,
    displayedMonth: Int
): Set<Int> {
    return if (checkInDate.first == displayedYear && checkInDate.second == displayedMonth) {
        setOf(checkInDate.third)
    } else {
        emptySet()
    }
}

@Composable
private fun CheckInCard(
    checkInDate: Triple<Int, Int, Int>,
    dailySalary: Double,
    onCheckInClick: () -> Unit
) {
    val context = LocalContext.current
    var weatherText by remember { mutableStateOf("天气加载……") }
    var retryKey by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) retryKey++
    }

    LaunchedEffect(retryKey) {
        var loc = getLastLocation(context)
        // 无定位权限 → 请求权限
        if (loc == null && !hasLocationPermission(context)) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return@LaunchedEffect
        }
        // 有权限但无 GPS 修复 → 请求单次定位
        if (loc == null) {
            loc = requestSingleLocation(context)
        }
        val w = if (loc != null) WeatherFetcher.fetch(loc.first, loc.second)
                else WeatherFetcher.fetchByAutoIp()
        weatherText = if (w != null) "${w.emoji} ${w.description} ${w.temp}\u00B0C"
        else "天气加载……"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${checkInDate.first} - ${checkInDate.second.toString().padStart(2, '0')} - ${checkInDate.third.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "\u672C\u65E5\u85AA\u916C",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoPill(text = weatherText, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), textColor = MaterialTheme.colorScheme.onPrimaryContainer)
                InfoPill(text = "\uFFE5${formatMoney(dailySalary)}", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), textColor = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onCheckInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "\u6253\u5361",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SmallMenuCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon = icon, tint = color, size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}




