package com.worktime.checkin.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worktime.checkin.data.CheckInRecordEntity
import com.worktime.checkin.data.CheckInRepository
import com.worktime.checkin.data.SalarySettings
import com.worktime.checkin.data.SalarySettingsRepository
import com.worktime.checkin.data.calculateDailySalary
import com.worktime.checkin.data.epochDayOf
import com.worktime.checkin.data.formatMoney
import com.worktime.checkin.ui.HolidayFetcher
import java.util.Calendar
import java.util.TimeZone
import java.nio.charset.Charset
import kotlin.math.max
import kotlin.math.round

private val StatsBlue = Color(0xFF3478F6)
private val StatsGreen = Color(0xFF2E8B57)
private val StatsOrange = Color(0xFFC47B29)
private val StatsRed = Color(0xFFC94A4A)
private val StatsPurple = Color(0xFF7E57C2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val today = remember { currentDateTriple() }
    var selectedYear by remember { mutableIntStateOf(today.first) }
    var selectedMonth by remember { mutableIntStateOf(today.second) }
    val context = LocalContext.current
    val checkInRepository = remember { CheckInRepository(context.applicationContext) }
    val salaryRepository = remember { SalarySettingsRepository(context.applicationContext) }
    val salarySettings by salaryRepository.settings.collectAsState(initial = SalarySettings())
    val monthRecords by remember(selectedYear, selectedMonth) {
        checkInRepository.observeMonth(selectedYear, selectedMonth)
    }.collectAsState(initial = emptyList())
    var holidays by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(selectedYear) {
        holidays = HolidayFetcher.fetch(selectedYear)
    }
    val stats = remember(monthRecords, salarySettings, selectedYear, selectedMonth, today, holidays) {
        monthRecords.toMonthlyStats(
            selectedDate = Triple(selectedYear, selectedMonth, selectedMonthAnchorDay(selectedYear, selectedMonth, today)),
            salarySettings = salarySettings,
            publicHolidays = holidays
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(50.dp),
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = {
                    Column {
                        Text(
                            text = "统计",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "统计每月出勤与薪资",
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                MonthSwitcher(
                    year = selectedYear,
                    month = selectedMonth,
                    onPrevious = {
                        if (selectedMonth == 1) {
                            selectedYear -= 1
                            selectedMonth = 12
                        } else {
                            selectedMonth -= 1
                        }
                    },
                    onNext = {
                        if (selectedMonth == 12) {
                            selectedYear += 1
                            selectedMonth = 1
                        } else {
                            selectedMonth += 1
                        }
                    }
                )
            }

            item { MonthOverviewCard(stats = stats) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        icon = Icons.Default.CalendarMonth,
                        title = "打卡天",
                        value = "${stats.workDays}",
                        suffix = "天",
                        color = StatsBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.AccessTime,
                        title = "总工时",
                        value = formatHours(stats.totalHours),
                        suffix = "H",
                        color = StatsGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.EventBusy,
                        title = "请假",
                        value = "${stats.leaveDays}",
                        suffix = "天",
                        color = StatsRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionCard(title = "工时结构", icon = Icons.Default.QueryStats) {
                    WorkMixBar(stats = stats)
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatLine("正班", formatHoursWithUnit(stats.normalHours), StatsBlue, stats.normalHours, stats.totalHours)
                        StatLine("加班", formatHoursWithUnit(stats.overtimeHours), StatsRed, stats.overtimeHours, stats.totalHours)
                        StatLine("周末", formatHoursWithUnit(stats.weekendHours), StatsPurple, stats.weekendHours, stats.totalHours)
                        StatLine("节假", formatHoursWithUnit(stats.holidayHours), StatsGreen, stats.holidayHours, stats.totalHours)
                    }
                }
            }

            item {
                SectionCard(title = "近7天趋势", icon = Icons.Default.Insights) {
                    WeeklyTrendChart(days = stats.recentDays)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShiftSummaryCard(
                        title = "白 / 早 / 中班",
                        value = "${stats.dayShiftDays + stats.earlyShiftDays + stats.middleShiftDays}天",
                        icon = Icons.Default.WbSunny,
                        color = StatsOrange,
                        modifier = Modifier.weight(1f)
                    )
                    ShiftSummaryCard(
                        title = "晚班",
                        value = "${stats.nightShiftDays}天",
                        icon = Icons.Default.DarkMode,
                        color = StatsPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionCard(title = "班次分布", icon = Icons.Default.CalendarMonth) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatLine("白班", "${stats.dayShiftDays}天", StatsBlue, stats.dayShiftDays.toDouble(), stats.workDays.toDouble())
                        StatLine("早班", "${stats.earlyShiftDays}天", StatsGreen, stats.earlyShiftDays.toDouble(), stats.workDays.toDouble())
                        StatLine("中班", "${stats.middleShiftDays}天", StatsOrange, stats.middleShiftDays.toDouble(), stats.workDays.toDouble())
                        StatLine("晚班", "${stats.nightShiftDays}天", StatsPurple, stats.nightShiftDays.toDouble(), stats.workDays.toDouble())
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MonthSwitcher(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月"
                )
            }
            Text(
                text = "${year}年${month}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月"
                )
            }
        }
    }
}

@Composable
private fun MonthOverviewCard(stats: MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = flatCardElevation()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "本月预计工资",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "￥${formatMoney(stats.estimatedSalary)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconBubble(
                    icon = Icons.Default.Payments,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 46.dp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OverviewPill("日均工时", formatHoursWithUnit(stats.averageHours), Modifier.weight(1f))
                OverviewPill("日均工资", "￥${formatMoney(stats.averageSalary)}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.58f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    suffix: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconBubble(icon = icon, tint = color, size = 34.dp)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = suffix,
                        style = MaterialTheme.typography.labelMedium,
                        color = color.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(icon = icon, tint = MaterialTheme.colorScheme.primary, size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun WorkMixBar(stats: MonthlyStats) {
    val total = stats.totalHours.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
    ) {
        MixSegment(stats.normalHours / total, StatsBlue)
        MixSegment(stats.overtimeHours / total, StatsRed)
        MixSegment(stats.weekendHours / total, StatsPurple)
        MixSegment(stats.holidayHours / total, StatsGreen)
    }
}

@Composable
private fun RowScope.MixSegment(weight: Double, color: Color) {
    if (weight > 0.0) {
        Box(
            modifier = Modifier
                .weight(weight.toFloat())
                .fillMaxHeight()
                .background(color)
        )
    }
}

@Composable
private fun StatLine(
    label: String,
    value: String,
    color: Color,
    amount: Double,
    total: Double
) {
    val ratio = if (total > 0.0) (amount / total).coerceIn(0.0, 1.0).toFloat() else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun WeeklyTrendChart(days: List<DailyStat>) {
    val maxHours = max(1.0, days.maxOfOrNull { it.hours } ?: 0.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            TrendDayBar(
                day = day,
                maxHours = maxHours,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrendDayBar(
    day: DailyStat,
    maxHours: Double,
    modifier: Modifier = Modifier
) {
    val ratio = (day.hours / maxHours).coerceIn(0.0, 1.0).toFloat()
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = formatHours(day.hours),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val width = size.width.coerceAtMost(18.dp.toPx())
            val left = (size.width - width) / 2f
            val barHeight = (size.height * ratio).coerceAtLeast(if (day.hours > 0.0) 4.dp.toPx() else 0f)
            drawRoundRect(
                color = Color(0xFFE4E7EC),
                topLeft = Offset(left, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(width / 2f, width / 2f)
            )
            drawRoundRect(
                color = if (day.isLeave) StatsRed else StatsBlue,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(width, barHeight),
                cornerRadius = CornerRadius(width / 2f, width / 2f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = day.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ShiftSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(icon = icon, tint = color, size = 36.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

private data class MonthlyStats(
    val estimatedSalary: Double,
    val averageSalary: Double,
    val workDays: Int,
    val leaveDays: Int,
    val totalHours: Double,
    val averageHours: Double,
    val normalHours: Double,
    val overtimeHours: Double,
    val weekendHours: Double,
    val holidayHours: Double,
    val dayShiftDays: Int,
    val earlyShiftDays: Int,
    val middleShiftDays: Int,
    val nightShiftDays: Int,
    val recentDays: List<DailyStat>
)

private data class DailyStat(
    val label: String,
    val hours: Double,
    val salary: Double,
    val isLeave: Boolean
)

private fun List<CheckInRecordEntity>.toMonthlyStats(
    selectedDate: Triple<Int, Int, Int>,
    salarySettings: SalarySettings,
    publicHolidays: Set<String>
): MonthlyStats {
    val latestByDay = groupBy { it.dateEpochDay }
        .values
        .mapNotNull { records -> records.maxByOrNull { it.createdAtMillis } }
        .sortedBy { it.dateEpochDay }

    val workingRecords = latestByDay.filter { it.leaveType.isEmpty() }
    val salaries = workingRecords.map { calculateDailySalary(it, salarySettings, publicHolidays) }
    val totalSalary = salaries.sum()
    val totalHours = workingRecords.sumOf { it.totalRecordedHours() }
    val workDays = workingRecords.count { it.hasWorkValue() }
    val recentDays = recentSevenDays(selectedDate).map { date ->
        val epochDay = epochDayOf(date.first, date.second, date.third)
        val record = latestByDay.firstOrNull { it.dateEpochDay == epochDay }
        DailyStat(
            label = "${date.second}/${date.third}",
            hours = record?.takeIf { it.leaveType.isEmpty() }?.totalRecordedHours() ?: 0.0,
            salary = calculateDailySalary(record, salarySettings, publicHolidays),
            isLeave = record?.leaveType?.isNotEmpty() == true
        )
    }

    return MonthlyStats(
        estimatedSalary = totalSalary,
        averageSalary = if (workDays > 0) totalSalary / workDays else 0.0,
        workDays = workDays,
        leaveDays = latestByDay.count { it.leaveType.isNotEmpty() },
        totalHours = totalHours,
        averageHours = if (workDays > 0) totalHours / workDays else 0.0,
        normalHours = workingRecords.sumOf { if (it.effectiveWorkKind(publicHolidays) == WorkKind.Normal) it.normalHours else 0.0 },
        overtimeHours = workingRecords.sumOf { if (it.effectiveWorkKind(publicHolidays) == WorkKind.Normal) it.overtimeHours else 0.0 },
        weekendHours = workingRecords.sumOf { if (it.effectiveWorkKind(publicHolidays) == WorkKind.Weekend) it.totalRecordedHours() else 0.0 },
        holidayHours = workingRecords.sumOf { if (it.effectiveWorkKind(publicHolidays) == WorkKind.Holiday) it.totalRecordedHours() else 0.0 },
        dayShiftDays = workingRecords.count { it.shift.isDayShift() },
        earlyShiftDays = workingRecords.count { it.shift.isEarlyShift() },
        middleShiftDays = workingRecords.count { it.shift.isMiddleShift() },
        nightShiftDays = workingRecords.count { it.shift.isNightShift() },
        recentDays = recentDays
    )
}

private fun CheckInRecordEntity.totalRecordedHours(): Double {
    return normalHours + overtimeHours + weekendHours + holidayHours
}

private fun CheckInRecordEntity.hasWorkValue(): Boolean {
    return totalRecordedHours() > 0.0 || dailyPay > 0.0
}

private fun CheckInRecordEntity.effectiveWorkKind(publicHolidays: Set<String>): WorkKind {
    return when {
        monthDay() in publicHolidays || workType.isHolidayWorkType() -> WorkKind.Holiday
        isWeekendDate() || workType.isWeekendWorkType() -> WorkKind.Weekend
        else -> WorkKind.Normal
    }
}

private fun String.isWeekendWorkType(): Boolean = this == "周末" || this == legacyMojibake("周末")

private fun String.isHolidayWorkType(): Boolean = this == "节假" || this == legacyMojibake("节假")

private fun String.isDayShift(): Boolean = this == "白班" || this == legacyMojibake("白班")

private fun String.isEarlyShift(): Boolean = this == "早班" || this == legacyMojibake("早班")

private fun String.isMiddleShift(): Boolean = this == "中班" || this == legacyMojibake("中班")

private fun String.isNightShift(): Boolean = this == "晚班" || this == legacyMojibake("晚班")

private val LegacyMojibakeCharset = Charset.forName("GB18030")

private fun legacyMojibake(text: String): String {
    return String(text.toByteArray(Charsets.UTF_8), LegacyMojibakeCharset)
}

private fun CheckInRecordEntity.isWeekendDate(): Boolean {
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

private enum class WorkKind {
    Normal,
    Weekend,
    Holiday
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun recentSevenDays(today: Triple<Int, Int, Int>): List<Triple<Int, Int, Int>> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, today.first)
        set(Calendar.MONTH, today.second - 1)
        set(Calendar.DAY_OF_MONTH, today.third)
    }
    return (6 downTo 0).map { offset ->
        val copy = cal.clone() as Calendar
        copy.add(Calendar.DAY_OF_MONTH, -offset)
        Triple(
            copy.get(Calendar.YEAR),
            copy.get(Calendar.MONTH) + 1,
            copy.get(Calendar.DAY_OF_MONTH)
        )
    }
}

private fun currentDateTriple(): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance()
    return Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

private fun selectedMonthAnchorDay(
    year: Int,
    month: Int,
    today: Triple<Int, Int, Int>
): Int {
    if (year == today.first && month == today.second) return today.third
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun formatHoursWithUnit(value: Double): String = "${formatHours(value)}H"

private fun formatHours(value: Double): String {
    val rounded = round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
