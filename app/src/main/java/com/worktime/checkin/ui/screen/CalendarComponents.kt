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
import androidx.compose.runtime.collectAsState
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
import com.worktime.checkin.data.CheckInRecordEntity
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

data class CalendarDaySummary(
    val normalHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val dailyPay: Double = 0.0,
    val leaveType: String = ""
) {
    val hasValue: Boolean
        get() = normalHours > 0.0 || overtimeHours > 0.0 || dailyPay > 0.0 || leaveType.isNotEmpty()
}

data class WorkSummary(
    val normalHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val weekendHours: Double = 0.0,
    val holidayHours: Double = 0.0,
    val dayShiftDays: Int = 0,
    val earlyShiftDays: Int = 0,
    val middleShiftDays: Int = 0,
    val nightShiftDays: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun FullCalendarScreen(
    displayedYear: Int,
    displayedMonth: Int,
    onYearMonthChange: (Int, Int) -> Unit,
    onRecordWork: (List<Triple<Int, Int, Int>>) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val checkInRepository = remember { CheckInRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val monthRecords by remember(displayedYear, displayedMonth) {
        checkInRepository.observeMonth(displayedYear, displayedMonth)
    }.collectAsState(initial = emptyList())
    val daySummaries = remember(monthRecords, displayedYear, displayedMonth) {
        monthRecords.toCalendarDaySummaries(displayedYear, displayedMonth)
    }
    val workSummary = remember(monthRecords) {
        monthRecords.toWorkSummary()
    }
    var selectedDaysByMonth by remember { mutableStateOf<Map<String, Set<Int>>>(emptyMap()) }
    var activeButtonByMonth by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    val currentKey = monthKey(displayedYear, displayedMonth)
    val selectedDays = selectedDaysByMonth[currentKey] ?: emptySet()
    val selectedDates = selectedDays
        .sorted()
        .map { day -> Triple(displayedYear, displayedMonth, day) }
    val activeSelectionButton = activeButtonByMonth[currentKey]
    var holidays by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(displayedYear) { holidays = HolidayFetcher.fetch(displayedYear) }
    fun requireSelectedDates(action: () -> Unit) {
        if (selectedDates.isEmpty()) {
            Toast.makeText(context, "请选择至少一个日期", Toast.LENGTH_SHORT).show()
        } else {
            action()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            CalendarCard(
                modifier = Modifier.fillMaxWidth(),
                compact = true,
                selectedDays = selectedDays,
                selectedDaysByMonth = selectedDaysByMonth,
                daySummaries = daySummaries,
                workSummary = workSummary,
                neutralSelection = true,
                showWorkSummary = true,
                displayedYear = displayedYear,
                displayedMonth = displayedMonth,
                highlightedDay = null,
                onYearMonthChange = onYearMonthChange,
                onDayToggle = { day ->
                    val currentSelectedDays = selectedDaysByMonth[currentKey] ?: emptySet()
                    val nextSelectedDays = if (day in currentSelectedDays) {
                        currentSelectedDays - day
                    } else {
                        currentSelectedDays + day
                    }
                    selectedDaysByMonth = selectedDaysByMonth + (currentKey to nextSelectedDays)
                    activeButtonByMonth = activeButtonByMonth + (currentKey to null)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val choices = listOf(
                    Triple("\u9009\u62E9\u5DE5\u4F5C\u65E5", "work", selectedDaysFor(displayedYear, displayedMonth, includeWeekdays = true, includeWeekends = false, holidays = holidays)),
                    Triple("\u9009\u62E9\u5468\u672B\u65E5", "weekend", selectedDaysFor(displayedYear, displayedMonth, includeWeekdays = false, includeWeekends = true, holidays = holidays)),
                    Triple("\u5168\u9009", "all", selectedDaysFor(displayedYear, displayedMonth, includeWeekdays = true, includeWeekends = true))
                )
                choices.forEach { (label, key, days) ->
                    SelectionButton(
                        label = label,
                        selected = activeSelectionButton == key,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (activeSelectionButton == key) {
                            selectedDaysByMonth = selectedDaysByMonth + (currentKey to emptySet())
                            activeButtonByMonth = activeButtonByMonth + (currentKey to null)
                        } else {
                            selectedDaysByMonth = selectedDaysByMonth + (currentKey to days)
                            activeButtonByMonth = activeButtonByMonth + (currentKey to key)
                        }
                    }
                }
            }

            CalendarActionCard(
                onRecordWork = {
                    requireSelectedDates { onRecordWork(selectedDates) }
                },
                onClearRecords = {
                    requireSelectedDates {
                        scope.launch {
                            checkInRepository.deleteByDates(selectedDates)
                            Toast.makeText(context, "已清除选中日期记录", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun SelectionButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = foreground
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
private fun CalendarActionCard(
    onRecordWork: () -> Unit,
    onClearRecords: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CalendarActionButton(Icons.Default.Timer, "\u8BB0\u5F55\u5DE5\u65F6", Color(0xFFE3F2FD), Color(0xFF1565C0), Modifier.weight(1f), onRecordWork)
                CalendarActionButton(Icons.Default.Delete, "\u6E05\u9664\u8BB0\u5F55", Color(0xFFFFE9EA), Color(0xFFC94A4A), Modifier.weight(1f), onClearRecords)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CalendarActionButton(
                    Icons.Default.Home,
                    "\u8FD4\u56DE\u4E3B\u9875",
                    Color(0xFFE8F5E9),
                    Color(0xFF2E7D32),
                    Modifier.fillMaxWidth(0.48f),
                    onBack
                )
            }
        }
    }
}

@Composable
private fun CalendarActionButton(
    icon: ImageVector,
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = foreground
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CalendarCard(
    modifier: Modifier = Modifier,
    compact: Boolean,
    selectedDays: Set<Int> = emptySet(),
    selectedDaysByMonth: Map<String, Set<Int>> = emptyMap(),
    daySummaries: Map<Int, CalendarDaySummary> = emptyMap(),
    workSummary: WorkSummary = WorkSummary(),
    neutralSelection: Boolean = false,
    showWorkSummary: Boolean = !compact,
    displayedYear: Int = 2026,
    displayedMonth: Int = 5,
    highlightedDay: Triple<Int, Int, Int>? = null,
    onYearMonthChange: (Int, Int) -> Unit = { _, _ -> },
    onDayToggle: (Int) -> Unit = {},
    onDayLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    var slideDirection by remember { mutableStateOf(0) }
    var holidays by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(displayedYear) {
        holidays = HolidayFetcher.fetch(displayedYear)
    }
    fun changeMonth(delta: Int) {
        slideDirection = if (delta >= 0) 1 else -1
        val next = moveMonth(displayedYear, displayedMonth, delta)
        onYearMonthChange(next.first, next.second)
    }

    Card(
        modifier = modifier
            .pointerInput(displayedYear, displayedMonth) {
                var dragAmount = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onHorizontalDrag = { _, amount -> dragAmount += amount },
                    onDragEnd = {
                        when {
                            dragAmount < -60f -> changeMonth(1)
                            dragAmount > 60f -> changeMonth(-1)
                        }
                    },
                    onDragCancel = { dragAmount = 0f }
                )
            }
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(if (compact) 20.dp else 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            CalendarHeader(
                displayedYear = displayedYear,
                displayedMonth = displayedMonth,
                compact = compact,
                onYearChange = { year ->
                    slideDirection = if (year >= displayedYear) 1 else -1
                    onYearMonthChange(year, displayedMonth)
                },
                onMonthChange = { delta -> changeMonth(delta) }
            )
            AnimatedContent(
                targetState = displayedYear to displayedMonth,
                transitionSpec = {
                    val direction = if (slideDirection == 0) 1 else slideDirection
                    (
                        slideInHorizontally(
                            animationSpec = tween(240),
                            initialOffsetX = { fullWidth -> fullWidth * direction }
                        ) + fadeIn(animationSpec = tween(180))
                    ).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(240),
                            targetOffsetX = { fullWidth -> -fullWidth * direction }
                        ) + fadeOut(animationSpec = tween(180))
                    )
                },
                label = "calendar-month-transition"
            ) { (year, month) ->
                val currentSelected = selectedDaysByMonth[monthKey(year, month)] ?: selectedDays
                CalendarGrid(
                    year = year,
                    month = month,
                    compact = compact,
                    selectedDays = currentSelected,
                    daySummaries = if (year == displayedYear && month == displayedMonth) daySummaries else emptyMap(),
                    neutralSelection = neutralSelection,
                    highlightedDay = highlightedDay,
                    holidays = holidays,
                    onDayToggle = onDayToggle,
                    onDayLongClick = onDayLongClick
                )
            }
            if (showWorkSummary) {
                WorkSummaryCard(summary = workSummary)
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    displayedYear: Int,
    displayedMonth: Int,
    compact: Boolean,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        CalendarHeaderControl(
            text = "${displayedYear}\u5E74",
            compact = compact,
            onPrevious = { onYearChange(displayedYear - 1) },
            onNext = { onYearChange(displayedYear + 1) }
        )
        CalendarHeaderControl(
            text = "${displayedMonth}\u6708",
            compact = compact,
            onPrevious = { onMonthChange(-1) },
            onNext = { onMonthChange(1) }
        )
    }
    if (!compact) {
        Text(
            text = "\u5DE6\u53F3\u6ED1\u52A8\u53EF\u5207\u6362\u5F53\u524D\u5E74\u6708",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CalendarHeaderControl(
    text: String,
    compact: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 6.dp)
    ) {
        CalendarHeaderArrow(label = "<", compact = compact, onClick = onPrevious)
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        CalendarHeaderArrow(label = ">", compact = compact, onClick = onNext)
    }
}

@Composable
private fun CalendarHeaderArrow(
    label: String,
    compact: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (compact) 24.dp else 28.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    compact: Boolean,
    selectedDays: Set<Int>,
    daySummaries: Map<Int, CalendarDaySummary> = emptyMap(),
    neutralSelection: Boolean = false,
    highlightedDay: Triple<Int, Int, Int>?,
    holidays: Set<String> = emptySet(),
    onDayToggle: (Int) -> Unit,
    onDayLongClick: (() -> Unit)? = null
) {
    val weekdays = listOf(
        "\u4E00",
        "\u4E8C",
        "\u4E09",
        "\u56DB",
        "\u4E94",
        "\u516D",
        "\u65E5"
    )
    val days = calendarCells(year, month)

    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        compact = compact,
                        selected = day != null && day in selectedDays,
                        summary = if (day != null) daySummaries[day] else null,
                        neutralSelection = neutralSelection,
                        highlighted = day != null &&
                            highlightedDay?.first == year &&
                            highlightedDay.second == month &&
                            highlightedDay.third == day,
                        isHoliday = day != null && String.format("%02d-%02d", month, day) in holidays,
                        onClick = { if (day != null) onDayToggle(day) },
                        onLongClick = onDayLongClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayCell(
    day: Int?,
    compact: Boolean,
    selected: Boolean,
    summary: CalendarDaySummary?,
    neutralSelection: Boolean,
    highlighted: Boolean,
    isHoliday: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasSummary = summary != null && summary.hasValue
    val cellHeight = when {
        compact && hasSummary -> 46.dp
        compact -> 34.dp
        hasSummary -> 58.dp
        else -> 38.dp
    }
    val active = selected || highlighted
    Box(
        modifier = modifier
            .height(cellHeight)
            .padding(horizontal = 1.dp, vertical = if (compact) 1.dp else 2.dp)
            .clip(RoundedCornerShape(if (compact) 9.dp else 12.dp))
            .background(
                when {
                    highlighted -> MaterialTheme.colorScheme.primary
                    selected && neutralSelection -> MaterialTheme.colorScheme.surfaceContainer
                    selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    else -> Color.Transparent
                }
            )
            .border(
                border = BorderStroke(
                    width = if (active) 1.dp else 0.dp,
                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent
                ),
                shape = RoundedCornerShape(if (compact) 9.dp else 12.dp)
            )
            .combinedClickable(
                enabled = day != null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.toString(),
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        highlighted -> MaterialTheme.colorScheme.onPrimary
                        selected && neutralSelection -> MaterialTheme.colorScheme.onSurface
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (summary != null && summary.hasValue) {
                    CalendarDaySummaryText(
                        summary = summary,
                        compact = compact,
                        highlighted = highlighted
                    )
                }
            }
            if (isHoliday) {
                Text(
                    text = "\u4F11",
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 2.dp),
                    fontSize = if (compact) 7.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
            }
        }
    }
}

@Composable
private fun CalendarDaySummaryText(
    summary: CalendarDaySummary,
    compact: Boolean,
    highlighted: Boolean
) {
    val summaryStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    val normalColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else PrimaryBlue
    val accentColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else WarmOrange
    val leaveColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else SoftGreen
    if (summary.leaveType.isNotEmpty()) {
        Text(
            text = summary.leaveType,
            style = summaryStyle,
            fontWeight = FontWeight.Black,
            color = leaveColor,
            maxLines = 1
        )
    } else if (summary.dailyPay > 0.0) {
        Text(
            text = "\uFFE5${formatCompactNumber(summary.dailyPay)}",
            style = summaryStyle,
            fontWeight = FontWeight.Black,
            color = accentColor,
            maxLines = 1
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (summary.normalHours > 0.0) {
                Text(
                    text = formatCompactNumber(summary.normalHours),
                    style = summaryStyle,
                    fontWeight = FontWeight.Black,
                    color = normalColor,
                    maxLines = 1
                )
            }
            if (summary.overtimeHours > 0.0) {
                Text(
                    text = "+${formatCompactNumber(summary.overtimeHours)}",
                    style = summaryStyle,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatCompactNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun List<CheckInRecordEntity>.toWorkSummary(): WorkSummary {
    val latestByDay = groupBy { it.dateEpochDay }
        .values
        .mapNotNull { records -> records.maxByOrNull { it.createdAtMillis } }

    return WorkSummary(
        normalHours = latestByDay.sumOf { if (it.workType == "\u6B63\u73ED") it.normalHours else 0.0 },
        overtimeHours = latestByDay.sumOf { if (it.workType == "\u6B63\u73ED") it.overtimeHours else 0.0 },
        weekendHours = latestByDay.sumOf { if (it.workType == "\u5468\u672B") it.weekendHours else 0.0 },
        holidayHours = latestByDay.sumOf { if (it.workType == "\u8282\u5047") it.holidayHours else 0.0 },
        dayShiftDays = latestByDay.count { it.shift == "\u767D\u73ED" },
        earlyShiftDays = latestByDay.count { it.shift == "\u65E9\u73ED" },
        middleShiftDays = latestByDay.count { it.shift == "\u4E2D\u73ED" },
        nightShiftDays = latestByDay.count { it.shift == "\u665A\u73ED" }
    )
}

@Composable
private fun WorkSummaryCard(summary: WorkSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SummaryChip("\u6B63\u73ED ${formatHoursForSummary(summary.normalHours)}H", Color(0xFFF7E7D5), WarmOrange, Modifier.weight(1f))
            SummaryChip("\u52A0\u73ED ${formatHoursForSummary(summary.overtimeHours)}H", Color(0xFFF5DADA), DangerRed, Modifier.weight(1f))
            SummaryChip("\u5468\u672B ${formatHoursForSummary(summary.weekendHours)}H", Color(0xFFEADDF7), Color(0xFF8E44AD), Modifier.weight(1f))
            SummaryChip("\u8282\u5047 ${formatHoursForSummary(summary.holidayHours)}H", Color(0xFFDFF2EA), SoftGreen, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SummaryChip("\u767D\u73ED ${formatDaysForSummary(summary.dayShiftDays)}\u5929", Color(0xFFDFF2FF), Color(0xFF3182C9), Modifier.weight(1f))
            SummaryChip("\u65E9\u73ED ${formatDaysForSummary(summary.earlyShiftDays)}\u5929", Color(0xFFDDF4EA), Color(0xFF46A77B), Modifier.weight(1f))
            SummaryChip("\u4E2D\u73ED ${formatDaysForSummary(summary.middleShiftDays)}\u5929", Color(0xFFFFEBD2), WarmOrange, Modifier.weight(1f))
            SummaryChip("\u665A\u73ED ${formatDaysForSummary(summary.nightShiftDays)}\u5929", Color(0xFFDCEAF7), Color(0xFF2374A9), Modifier.weight(1f))
        }
    }
}

private fun formatHoursForSummary(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun formatDaysForSummary(value: Int): String = value.toString()

@Composable
private fun SummaryChip(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = foreground,
            maxLines = 1
        )
    }
}

@Composable
fun InfoPill(text: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = textColor
        )
    }
}



