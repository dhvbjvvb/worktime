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
import com.worktime.checkin.data.METHOD_BASE_PLUS_OVERTIME
import com.worktime.checkin.data.METHOD_STANDARD
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

@Composable
fun CheckInSheet(
    checkInDate: Triple<Int, Int, Int>,
    checkInDates: List<Triple<Int, Int, Int>> = listOf(checkInDate),
    heightFraction: Float = 0.80f,
    inlineLeaveType: Boolean = false,
    salaryCalcMethod: String = METHOD_STANDARD,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val repository = remember { CheckInRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var selectedShift by remember { mutableStateOf("\u767D\u73ED") }
    var selectedWorkType by remember { mutableStateOf("\u6B63\u73ED") }
    var leaveExpanded by remember { mutableStateOf(false) }
    var selectedLeaveType by remember { mutableStateOf("") }
    var normalHours by remember { mutableStateOf("8") }
    var normalOvertimeHours by remember { mutableStateOf("2") }
    var weekendHours by remember { mutableStateOf("0") }
    var holidayHours by remember { mutableStateOf("0") }
    var dailyPay by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ShiftSelector(
                    selectedShift = selectedShift,
                    onShiftSelected = { shift ->
                        selectedShift = if (selectedShift == shift) "" else shift
                    }
                )
                WorkTypeEditor(
                    selectedType = selectedWorkType,
                    onTypeSelected = { type ->
                        val nextType = if (selectedWorkType == type) "" else type
                        selectedWorkType = nextType
                        selectedLeaveType = ""
                    },
                    selectedLeaveType = selectedLeaveType,
                    onLeaveTypeSelected = { leaveType ->
                        selectedLeaveType = leaveType
                    },
                    inlineLeaveType = inlineLeaveType,
                    salaryCalcMethod = salaryCalcMethod,
                    normalHours = normalHours,
                    onNormalHoursChange = { normalHours = it },
                    normalOvertimeHours = normalOvertimeHours,
                    onNormalOvertimeHoursChange = { normalOvertimeHours = it },
                    weekendHours = weekendHours,
                    onWeekendHoursChange = { weekendHours = it },
                    holidayHours = holidayHours,
                    onHolidayHoursChange = { holidayHours = it },
                    dailyPay = dailyPay,
                    onDailyPayChange = { dailyPay = it }
                )
                if (!inlineLeaveType) {
                    LeaveSelector(
                        expanded = leaveExpanded,
                        selectedLeaveType = selectedLeaveType,
                        onToggle = { leaveExpanded = !leaveExpanded },
                        onLeaveSelected = { leaveType ->
                            selectedLeaveType = if (selectedLeaveType == leaveType) "" else leaveType
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Text(
                        text = "\u53D6\u6D88\u64CD\u4F5C",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = {
                        val targetDates = checkInDates.ifEmpty { listOf(checkInDate) }
                        val isLeaveRecord = selectedLeaveType in listOf("\u4F11\u5047", "\u75C5\u5047", "\u4E8B\u5047", "\u85AA\u5047")
                        val isBasePlusOvertime = salaryCalcMethod == METHOD_BASE_PLUS_OVERTIME
                        val inputNormalHours = if (!isBasePlusOvertime) normalHours.toDoubleOrNull() ?: 0.0 else 0.0
                        val inputOvertimeHours = normalOvertimeHours.toDoubleOrNull() ?: 0.0
                        val inputWeekendHours = weekendHours.toDoubleOrNull() ?: 0.0
                        val inputHolidayHours = holidayHours.toDoubleOrNull() ?: 0.0
                        val recordDailyPay = if (!isLeaveRecord && selectedWorkType == "\u65E5\u7ED3") dailyPay.toDoubleOrNull() ?: 0.0 else 0.0
                        scope.launch {
                            val holidaysByYear = targetDates
                                .map { it.first }
                                .distinct()
                                .associateWith { year -> HolidayFetcher.fetch(year) }
                            targetDates.forEach { date ->
                                val holidayAdjustedDraft = createDateAwareRecordDraft(
                                    date = date,
                                    selectedShift = selectedShift,
                                    selectedWorkType = selectedWorkType,
                                    selectedLeaveType = selectedLeaveType,
                                    isLeaveRecord = isLeaveRecord,
                                    normalHours = inputNormalHours,
                                    overtimeHours = inputOvertimeHours,
                                    weekendHours = inputWeekendHours,
                                    holidayHours = inputHolidayHours,
                                    dailyPay = recordDailyPay,
                                    holidays = holidaysByYear[date.first].orEmpty()
                                )
                                repository.save(
                                    holidayAdjustedDraft
                                )
                            }
                            val message = if (targetDates.size == 1) {
                                "打卡记录已保存"
                            } else {
                                "${targetDates.size}天打卡记录已保存"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "\u4FDD\u5B58\u8BB0\u5F55",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun createDateAwareRecordDraft(
    date: Triple<Int, Int, Int>,
    selectedShift: String,
    selectedWorkType: String,
    selectedLeaveType: String,
    isLeaveRecord: Boolean,
    normalHours: Double,
    overtimeHours: Double,
    weekendHours: Double,
    holidayHours: Double,
    dailyPay: Double,
    holidays: Set<String>
): CheckInRecordDraft {
    if (isLeaveRecord) {
        return CheckInRecordDraft(
            dateEpochDay = epochDayOf(date.first, date.second, date.third),
            shift = selectedShift,
            workType = "休假",
            leaveType = selectedLeaveType,
            normalHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = 0.0,
            holidayHours = 0.0,
            dailyPay = 0.0
        )
    }

    if (selectedWorkType == "日结") {
        return CheckInRecordDraft(
            dateEpochDay = epochDayOf(date.first, date.second, date.third),
            shift = selectedShift,
            workType = selectedWorkType,
            leaveType = selectedLeaveType,
            normalHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = 0.0,
            holidayHours = 0.0,
            dailyPay = dailyPay
        )
    }

    val isHoliday = "%02d-%02d".format(date.second, date.third) in holidays
    val isWeekend = date.isWeekendDate()
    val normalWorkHours = normalHours + overtimeHours

    return when {
        selectedWorkType == "节假" -> CheckInRecordDraft(
            dateEpochDay = epochDayOf(date.first, date.second, date.third),
            shift = selectedShift,
            workType = selectedWorkType,
            leaveType = selectedLeaveType,
            normalHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = 0.0,
            holidayHours = holidayHours,
            dailyPay = 0.0
        )
        selectedWorkType == "周末" -> CheckInRecordDraft(
            dateEpochDay = epochDayOf(date.first, date.second, date.third),
            shift = selectedShift,
            workType = selectedWorkType,
            leaveType = selectedLeaveType,
            normalHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = weekendHours,
            holidayHours = 0.0,
            dailyPay = 0.0
        )
        selectedWorkType == "正班" && isHoliday -> CheckInRecordDraft(
            dateEpochDay = epochDayOf(date.first, date.second, date.third),
            shift = selectedShift,
            workType = "节假",
            leaveType = selectedLeaveType,
            normalHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = 0.0,
            holidayHours = normalWorkHours,
            dailyPay = 0.0
        )
        selectedWorkType == "正班" && isWeekend -> CheckInRecordDraft(
            dateEpochDay = epochDayOf(date.first, date.second, date.third),
            shift = selectedShift,
            workType = "周末",
            leaveType = selectedLeaveType,
            normalHours = 0.0,
            overtimeHours = 0.0,
            weekendHours = normalWorkHours,
            holidayHours = 0.0,
            dailyPay = 0.0
        )
        else -> CheckInRecordDraft(
            dateEpochDay = epochDayOf(date.first, date.second, date.third),
            shift = selectedShift,
            workType = selectedWorkType,
            leaveType = selectedLeaveType,
            normalHours = if (selectedWorkType == "正班") normalHours else 0.0,
            overtimeHours = if (selectedWorkType == "正班") overtimeHours else 0.0,
            weekendHours = 0.0,
            holidayHours = 0.0,
            dailyPay = 0.0
        )
    }
}

private fun Triple<Int, Int, Int>.isWeekendDate(): Boolean {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, first)
        set(Calendar.MONTH, second - 1)
        set(Calendar.DAY_OF_MONTH, third)
    }
    val weekday = cal.get(Calendar.DAY_OF_WEEK)
    return weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    left: String,
    right: String,
    accent: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(icon = icon, tint = accent, size = 36.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = left,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = right,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun LeaveSelector(
    expanded: Boolean,
    selectedLeaveType: String,
    onToggle: () -> Unit,
    onLeaveSelected: (String) -> Unit
) {
    val leaveTypes = listOf("\u4F11\u5047", "\u75C5\u5047", "\u4E8B\u5047", "\u85AA\u5047")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(icon = Icons.Default.DateRange, tint = SoftGreen, size = 36.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (selectedLeaveType.isEmpty()) "\u4F11\u5047" else selectedLeaveType,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (expanded) "^" else "\u2304",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leaveTypes.forEach { type ->
                        val selected = type == selectedLeaveType
                        Text(
                            text = type,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { onLeaveSelected(type) }
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconBubble(icon: ImageVector, tint: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
private fun ShiftSelector(
    selectedShift: String,
    onShiftSelected: (String) -> Unit
) {
    val shifts = listOf(
        ShiftItem(Icons.Default.WbSunny, "\u767D\u73ED"),
        ShiftItem(Icons.Default.WbTwilight, "\u65E9\u73ED"),
        ShiftItem(Icons.Default.WbCloudy, "\u4E2D\u73ED"),
        ShiftItem(Icons.Default.DarkMode, "\u665A\u73ED")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            shifts.forEach { item ->
                val selected = item.label == selectedShift
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onShiftSelected(item.label) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) MaterialTheme.colorScheme.onSurface
                              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

private data class ShiftItem(
    val icon: ImageVector,
    val label: String
)

@Composable
private fun WorkTypeEditor(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedLeaveType: String,
    onLeaveTypeSelected: (String) -> Unit,
    inlineLeaveType: Boolean,
    salaryCalcMethod: String,
    normalHours: String,
    onNormalHoursChange: (String) -> Unit,
    normalOvertimeHours: String,
    onNormalOvertimeHoursChange: (String) -> Unit,
    weekendHours: String,
    onWeekendHoursChange: (String) -> Unit,
    holidayHours: String,
    onHolidayHoursChange: (String) -> Unit,
    dailyPay: String,
    onDailyPayChange: (String) -> Unit
) {
    val isBasePlusOvertime = salaryCalcMethod == METHOD_BASE_PLUS_OVERTIME
    val tabs = listOf(
        "\u6B63\u73ED",
        "\u5468\u672B",
        "\u8282\u5047",
        "\u65E5\u7ED3"
    ) + if (inlineLeaveType) listOf("\u4F11\u5047") else emptyList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = flatCardElevation()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEach { tab ->
                    val selected = tab == selectedType
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clickable { onTypeSelected(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .fillMaxWidth()
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                when (selectedType) {
                    "\u6B63\u73ED" -> {
                        if (isBasePlusOvertime) {
                            WorkInputRow("\u52A0\u73ED\u65F6\u957F", normalOvertimeHours, onNormalOvertimeHoursChange, WarmOrange, "H")
                        } else {
                            WorkInputRow("\u6B63\u5E38\u4E0A\u73ED", normalHours, onNormalHoursChange, PrimaryBlue, "H")
                            WorkInputRow("\u6B63\u5E38\u52A0\u73ED", normalOvertimeHours, onNormalOvertimeHoursChange, WarmOrange, "H")
                        }
                    }
                    "\u8282\u5047" -> WorkInputRow("\u8282\u5047\u52A0\u73ED", holidayHours, onHolidayHoursChange, DangerRed, "H")
                    "\u65E5\u7ED3" -> WorkInputRow("\u65E5\u7ED3\u5DE5\u8D44", dailyPay, onDailyPayChange, WarmOrange, "\u5143", prefix = "\uFFE5", placeholder = "\u8F93\u5165...")
                    "\u4F11\u5047" -> LeaveTypeContent(
                        selectedLeaveType = selectedLeaveType,
                        onLeaveTypeSelected = onLeaveTypeSelected
                    )
                    else -> WorkInputRow("\u5468\u672B\u52A0\u73ED", weekendHours, onWeekendHoursChange, PrimaryBlue, "H")
                }
            }
        }
    }
}

@Composable
private fun LeaveTypeContent(
    selectedLeaveType: String,
    onLeaveTypeSelected: (String) -> Unit
) {
    val leaveTypes = listOf("\u4F11\u5047", "\u75C5\u5047", "\u4E8B\u5047", "\u85AA\u5047")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        leaveTypes.forEach { type ->
            val selected = selectedLeaveType == type
            Text(
                text = type,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onLeaveTypeSelected(type) }
                    .padding(vertical = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WorkInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    suffix: String,
    prefix: String = "",
    placeholder: String = "0"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        WorkValueInput(
            value = value,
            onValueChange = onValueChange,
            color = color,
            prefix = prefix,
            suffix = suffix,
            placeholder = placeholder
        )
    }
}

@Composable
private fun WorkValueInput(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    prefix: String = "",
    suffix: String,
    placeholder: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(138.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = color,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (prefix.isNotEmpty()) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.titleLarge,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}




