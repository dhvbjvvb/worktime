package com.worktime.checkin.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.checkin.data.CheckInRecordEntity
import com.worktime.checkin.data.CheckInRepository
import com.worktime.checkin.data.SalarySettings
import com.worktime.checkin.data.SalarySettingsRepository
import com.worktime.checkin.data.calculateDailySalary
import com.worktime.checkin.data.epochDayOf
import com.worktime.checkin.ui.HolidayFetcher
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.round

private val ReportBlue = Color(0xFF006FD6)
private val ReportGreen = Color(0xFF008044)
private val ReportOrange = Color(0xFFE65A00)
private val ReportRed = Color(0xFFC93521)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    year: Int,
    month: Int,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedYear by remember(year) { mutableIntStateOf(year) }
    var selectedMonth by remember(month) { mutableIntStateOf(month) }
    val context = LocalContext.current
    val checkInRepository = remember { CheckInRepository(context.applicationContext) }
    val salaryRepository = remember { SalarySettingsRepository(context.applicationContext) }
    val salarySettings by salaryRepository.settings.collectAsState(initial = SalarySettings())
    val monthRecords by remember(selectedYear, selectedMonth) {
        checkInRepository.observeMonth(selectedYear, selectedMonth)
    }.collectAsState(initial = emptyList())
    var holidays by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csv = pendingCsv
        pendingCsv = null
        if (uri != null && csv != null) {
            exportCsv(context, uri, csv)
        }
    }

    LaunchedEffect(selectedYear) {
        holidays = HolidayFetcher.fetch(selectedYear)
    }

    val report = remember(monthRecords, salarySettings, holidays, selectedYear, selectedMonth) {
        monthRecords.toMonthlyReport(selectedYear, selectedMonth, salarySettings, holidays)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(50.dp),
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "\u8FD4\u56DE"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "\u62A5\u8868",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${selectedYear}\u5E74${selectedMonth}\u6708\u8BB0\u5F55\u660E\u7EC6",
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
        bottomBar = {
            ReportActions(
                onExcelClick = {
                    pendingCsv = report.toCsvText()
                    csvLauncher.launch("${selectedYear}-${selectedMonth.toString().padStart(2, '0')}-report.csv")
                },
                onTextClick = {
                    copyReportText(context, "\u6587\u5B57\u62A5\u8868", report.toPlainText())
                    Toast.makeText(context, "\u6587\u5B57\u62A5\u8868\u5DF2\u590D\u5236", Toast.LENGTH_SHORT).show()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                MonthSwitcherCompact(
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
            item {
                Text(
                    text = "\u6BCF\u6708\u660E\u7EC6",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(
                items = report.days,
                key = { day -> day.day },
                contentType = { day -> if (day.hasContent) "recorded-day" else "empty-day" }
            ) { day ->
                if (day.hasContent) {
                    ReportDayCard(day = day)
                } else {
                    EmptyReportDayRow(day = day)
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun MonthSwitcherCompact(
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
                .height(52.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "\u4E0A\u4E2A\u6708"
                )
            }
            Text(
                text = "${year}\u5E74${month}\u6708",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "\u4E0B\u4E2A\u6708"
                )
            }
        }
    }
}

@Composable
private fun ReportDayCard(day: ReportDay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${day.day}\u53F7",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = day.shift.ifBlank { "\u5DF2\u8BB0\u5F55" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatDisplayMoney(day.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (day.amount > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReportHourChip("\u6B63", day.normalHours, ReportBlue, Modifier.weight(1f))
                ReportHourChip("\u52A0", day.overtimeHours, ReportGreen, Modifier.weight(1f))
                ReportHourChip("\u5468", day.weekendHours, ReportOrange, Modifier.weight(1f))
                ReportHourChip("\u8282", day.holidayHours, ReportRed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyReportDayRow(day: ReportDay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.54f))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${day.day}\u53F7",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
        )
        Text(
            text = "\u672A\u8BB0\u5F55",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
        )
    }
}

@Composable
private fun ReportHourChip(
    label: String,
    hours: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = if (hours > 0.0) 0.09f else 0.04f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (hours > 0.0) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.44f)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = formatReportHours(hours),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (hours > 0.0) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.44f),
            maxLines = 1
        )
    }
}

@Composable
private fun ReportActions(
    onExcelClick: () -> Unit,
    onTextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReportActionButton(
            text = "Excel",
            icon = Icons.Default.TableChart,
            onClick = onExcelClick,
            modifier = Modifier.weight(1f)
        )
        ReportActionButton(
            text = "\u6587\u5B57\u5BFC\u51FA",
            icon = Icons.AutoMirrored.Filled.Article,
            onClick = onTextClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReportActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ReportBlue),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun List<CheckInRecordEntity>.toMonthlyReport(
    year: Int,
    month: Int,
    salarySettings: SalarySettings,
    publicHolidays: Set<String>
): MonthlyReport {
    val latestByDay = groupBy { it.dateEpochDay }
        .mapValues { (_, records) -> records.maxByOrNull { it.createdAtMillis } }
    val days = daysInMonth(year, month)
    val dayRows = (1..days).map { day ->
        val record = latestByDay[epochDayOf(year, month, day)]
        record.toReportDay(day, publicHolidays, salarySettings)
    }
    val normalHourly = salarySettings.resolvedNormalHourly()
    val overtimeHourly = salarySettings.resolvedOvertimeHourly(normalHourly)
    val totalHours = dayRows.sumOf { it.totalHours }
    val totalAmount = dayRows.sumOf { it.amount }

    return MonthlyReport(
        year = year,
        month = month,
        days = dayRows,
        baseSalary = salarySettings.baseSalary.moneyValue(),
        normalHourly = normalHourly,
        overtimeHourly = overtimeHourly,
        normalHours = dayRows.sumOf { it.normalHours },
        overtimeHours = dayRows.sumOf { it.overtimeHours },
        weekendHours = dayRows.sumOf { it.weekendHours },
        holidayHours = dayRows.sumOf { it.holidayHours },
        normalAmount = dayRows.sumOf { it.normalHours * normalHourly },
        overtimeAmount = dayRows.sumOf { it.overtimeHours * overtimeHourly },
        totalHours = totalHours,
        totalAmount = totalAmount,
        averageHourly = if (totalHours > 0.0) totalAmount / totalHours else 0.0
    )
}

private fun CheckInRecordEntity?.toReportDay(
    day: Int,
    publicHolidays: Set<String>,
    salarySettings: SalarySettings
): ReportDay {
    if (this == null) return ReportDay(day = day)
    if (leaveType.isNotEmpty()) {
        return ReportDay(day = day, shift = leaveType, hasContent = true)
    }

    val kind = effectiveReportKind(publicHolidays)
    val totalHours = normalHours + overtimeHours + weekendHours + holidayHours
    val amount = calculateDailySalary(this, salarySettings, publicHolidays)
    val hasValue = totalHours > 0.0 || dailyPay > 0.0

    return when (kind) {
        ReportWorkKind.Normal -> ReportDay(
            day = day,
            shift = shift,
            normalHours = normalHours,
            overtimeHours = overtimeHours,
            amount = amount,
            hasContent = hasValue || shift.isNotBlank()
        )
        ReportWorkKind.Weekend -> ReportDay(
            day = day,
            shift = shift,
            weekendHours = totalHours,
            amount = amount,
            hasContent = hasValue || shift.isNotBlank()
        )
        ReportWorkKind.Holiday -> ReportDay(
            day = day,
            shift = shift,
            holidayHours = totalHours,
            amount = amount,
            hasContent = hasValue || shift.isNotBlank()
        )
    }
}

private fun CheckInRecordEntity.effectiveReportKind(publicHolidays: Set<String>): ReportWorkKind {
    return when {
        monthDay() in publicHolidays || workType == "\u8282\u5047" -> ReportWorkKind.Holiday
        isWeekendDate() || workType == "\u5468\u672B" -> ReportWorkKind.Weekend
        else -> ReportWorkKind.Normal
    }
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

private fun daysInMonth(year: Int, month: Int): Int {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun SalarySettings.resolvedNormalHourly(): Double {
    return normalHourly.moneyValue().takeIf { it > 0.0 }
        ?: baseSalary.moneyValue().takeIf { it > 0.0 }?.let { it / 21.75 / 8.0 }
        ?: 0.0
}

private fun SalarySettings.resolvedOvertimeHourly(normalHourly: Double): Double {
    return overtimeHourly.moneyValue().takeIf { it > 0.0 }
        ?: normalHourly * (overtimeRate.moneyValue().takeIf { it > 0.0 } ?: 1.5)
}

private fun String.moneyValue(): Double = toDoubleOrNull() ?: 0.0

private fun formatReportHours(value: Double): String {
    val rounded = round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        "${rounded.toInt()}H"
    } else {
        "${rounded}H"
    }
}

private fun formatDisplayMoney(value: Double): String {
    val rounded = round(value * 10.0) / 10.0
    val text = if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
    return "\uFFE5$text"
}

private fun formatExportMoney(value: Double): String {
    return "%.2f".format(value)
}

private fun copyReportText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun exportCsv(context: Context, uri: Uri, csv: String) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(csv.toByteArray(Charsets.UTF_8))
        } ?: error("Cannot open output stream")
    }.onSuccess {
        Toast.makeText(context, "Excel CSV\u5DF2\u5BFC\u51FA", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "\u5BFC\u51FA\u5931\u8D25", Toast.LENGTH_SHORT).show()
    }
}

private fun MonthlyReport.toCsvText(): String {
    val lines = mutableListOf<String>()
    lines += csvLine("${year}\u5E74${month}\u6708\u8BB0\u5F55\u660E\u7EC6")
    lines += csvLine("\u7C7B\u578B", "\u65F6\u957F", "\u65F6\u85AA", "\u91D1\u989D")
    lines += csvLine("\u5E95\u85AA", "--", "--", formatExportMoney(baseSalary))
    lines += csvLine("\u6B63\u73ED", formatReportHours(normalHours), "${formatExportMoney(normalHourly)}/H", formatExportMoney(normalAmount))
    lines += csvLine("\u52A0\u73ED", formatReportHours(overtimeHours), "${formatExportMoney(overtimeHourly)}/H", formatExportMoney(overtimeAmount))
    lines += csvLine("\u603B\u8BA1", formatReportHours(totalHours), "\u2248${formatExportMoney(averageHourly)}/H", formatExportMoney(totalAmount))
    lines += ""
    lines += csvLine("\u65E5\u671F", "\u73ED\u6B21", "\u6B63\u73ED", "\u52A0\u73ED", "\u5468\u672B", "\u8282\u5047", "\u91D1\u989D")
    days.forEach { day ->
        lines += csvLine(
            "${day.day}\u53F7",
            day.shift.ifBlank { "--" },
            formatReportHours(day.normalHours),
            formatReportHours(day.overtimeHours),
            formatReportHours(day.weekendHours),
            formatReportHours(day.holidayHours),
            formatExportMoney(day.amount)
        )
    }
    return "\uFEFF" + lines.joinToString("\r\n")
}

private fun MonthlyReport.toPlainText(): String {
    return buildString {
        appendLine("${year}\u5E74${month}\u6708\u8BB0\u5F55\u660E\u7EC6")
        appendLine("\u603B\u5DE5\u65F6\uFF1A${formatReportHours(totalHours)}")
        appendLine("\u6B63\u73ED\uFF1A${formatReportHours(normalHours)}\uFF0C\u52A0\u73ED\uFF1A${formatReportHours(overtimeHours)}")
        appendLine("\u5468\u672B\uFF1A${formatReportHours(weekendHours)}\uFF0C\u8282\u5047\uFF1A${formatReportHours(holidayHours)}")
        appendLine("\u603B\u91D1\u989D\uFF1A${formatDisplayMoney(totalAmount)}")
    }
}

private fun csvLine(vararg cells: String): String {
    return cells.joinToString(",") { cell ->
        "\"${cell.replace("\"", "\"\"")}\""
    }
}

private data class MonthlyReport(
    val year: Int,
    val month: Int,
    val days: List<ReportDay>,
    val baseSalary: Double,
    val normalHourly: Double,
    val overtimeHourly: Double,
    val normalHours: Double,
    val overtimeHours: Double,
    val weekendHours: Double,
    val holidayHours: Double,
    val normalAmount: Double,
    val overtimeAmount: Double,
    val totalHours: Double,
    val totalAmount: Double,
    val averageHourly: Double
)

private data class ReportDay(
    val day: Int,
    val shift: String = "",
    val normalHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val weekendHours: Double = 0.0,
    val holidayHours: Double = 0.0,
    val amount: Double = 0.0,
    val hasContent: Boolean = false
) {
    val totalHours: Double
        get() = normalHours + overtimeHours + weekendHours + holidayHours
}

private enum class ReportWorkKind {
    Normal,
    Weekend,
    Holiday
}

private const val MILLIS_PER_DAY = 86_400_000L
