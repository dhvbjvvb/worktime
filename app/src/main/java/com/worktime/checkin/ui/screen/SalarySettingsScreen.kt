package com.worktime.checkin.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.checkin.data.SalarySettings
import com.worktime.checkin.data.SalarySettingsRepository
import kotlinx.coroutines.launch

private val SalaryGreen = Color(0xFF5B9A8B)
private val SalaryOrange = Color(0xFFE85D3A)
private val SalaryRed = Color(0xFFC94A4A)
private val CalcMethods = listOf("正班 + 加班", "底薪 + 加班", "综合工作制", "小时计算")
private val AutoCalcMethods = CalcMethods.take(3).toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySettingsScreen(
    onBack: () -> Unit,
    selectedCalcMethod: String,
    onCalcMethodClick: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository = remember { SalarySettingsRepository(context.applicationContext) }
    val settings by repository.settings.collectAsState(initial = SalarySettings(selectedCalcMethod = selectedCalcMethod))
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "薪资设置",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SalaryModeCard(
                selectedCalcMethod = settings.selectedCalcMethod,
                values = settings,
                onValuesChange = { next ->
                    scope.launch { repository.update { next } }
                },
                onCalcMethodClick = onCalcMethodClick
            )

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "完成设置",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SalaryModeCard(
    selectedCalcMethod: String,
    values: SalarySettings,
    onValuesChange: (SalarySettings) -> Unit,
    onCalcMethodClick: () -> Unit = {}
) {
    val shouldAutoCalc = selectedCalcMethod in AutoCalcMethods

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = selectedCalcMethod,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Card(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable { onCalcMethodClick() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "更换计算方式",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ">",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Text(
                    text = calcMethodDescription(selectedCalcMethod),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            if (selectedCalcMethod == "小时计算") {
                HourlyCalcContent(
                    values = values,
                    onValuesChange = onValuesChange
                )
                return@Column
            }

            SalaryAmountRow(
                title = "底薪工资",
                value = values.baseSalary,
                onValueChange = { onValuesChange(values.copy(baseSalary = it)) },
                color = MaterialTheme.colorScheme.primary
            )
            RowDivider()
            SalaryAmountRow(
                title = "正班时薪 / H",
                value = values.normalHourly,
                onValueChange = { value ->
                    val next = if (shouldAutoCalc) {
                        val hourly = value.toDoubleOrNull()
                        if (hourly == null) {
                            values.copy(
                                normalHourly = value,
                                overtimeRate = "1.5",
                                weekendRate = "2",
                                holidayRate = "3",
                                overtimeHourly = "0",
                                weekendHourly = "0",
                                holidayHourly = "0"
                            )
                        } else {
                            values.copy(
                                normalHourly = value,
                                overtimeRate = "1.5",
                                weekendRate = "2",
                                holidayRate = "3",
                                overtimeHourly = formatSalaryNumber(hourly * 1.5),
                                weekendHourly = formatSalaryNumber(hourly * 2),
                                holidayHourly = formatSalaryNumber(hourly * 3)
                            )
                        }
                    } else {
                        values.copy(normalHourly = value)
                    }
                    onValuesChange(next)
                },
                color = MaterialTheme.colorScheme.primary
            )
            RowDivider()
            SalaryRateAmountRow(
                title = "加班时薪 / H",
                rate = values.overtimeRate,
                onRateChange = { onValuesChange(values.copy(overtimeRate = it)) },
                value = values.overtimeHourly,
                onValueChange = { onValuesChange(values.copy(overtimeHourly = it)) },
                color = SalaryGreen
            )
            RowDivider()
            SalaryRateAmountRow(
                title = "周末时薪 / H",
                rate = values.weekendRate,
                onRateChange = { onValuesChange(values.copy(weekendRate = it)) },
                value = values.weekendHourly,
                onValueChange = { onValuesChange(values.copy(weekendHourly = it)) },
                color = SalaryOrange
            )
            RowDivider()
            SalaryRateAmountRow(
                title = "节假时薪 / H",
                rate = values.holidayRate,
                onRateChange = { onValuesChange(values.copy(holidayRate = it)) },
                value = values.holidayHourly,
                onValueChange = { onValuesChange(values.copy(holidayHourly = it)) },
                color = SalaryRed
            )

            RowDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "正班时薪规则",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "固定时薪",
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HintText("* 时薪计算：底薪 ÷ 21.75 ÷ 8")
                HintText("* 时薪、倍数和金额都可以手动修改，以实际为准")
                HintText("* 选择计算方式后，当前输入内容会保留")
            }
        }
    }
}

@Composable
private fun HourlyCalcContent(
    values: SalarySettings,
    onValuesChange: (SalarySettings) -> Unit
) {
    SalaryAmountRow(
        title = "日常时薪 / H",
        value = values.dailyHourly,
        onValueChange = { onValuesChange(values.copy(dailyHourly = it)) },
        color = MaterialTheme.colorScheme.primary,
        placeholder = "小时工资"
    )
    RowDivider()
    SalaryAmountRow(
        title = "节假时薪 / H",
        value = values.holidayDailyHourly,
        onValueChange = { onValuesChange(values.copy(holidayDailyHourly = it)) },
        color = SalaryRed,
        placeholder = "节假日时薪"
    )
    RowDivider()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        HintText("* 小时工平时加班不进行加倍计算")
        HintText("* 小时工可单独设置节假加班时薪")
        HintText("* 需记录多种时薪请切换至“正班 + 加班”，底薪可填 0")
    }
}

@Composable
private fun SalaryAmountRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    placeholder: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        SalaryInput(
            value = value,
            onValueChange = onValueChange,
            color = color,
            prefix = "￥",
            placeholder = placeholder,
            modifier = Modifier.width(118.dp)
        )
    }
}

@Composable
private fun SalaryRateAmountRow(
    title: String,
    rate: String,
    onRateChange: (String) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        SalaryInput(
            value = rate,
            onValueChange = onRateChange,
            color = color,
            suffix = "倍",
            modifier = Modifier.width(76.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SalaryInput(
            value = value,
            onValueChange = onValueChange,
            color = color,
            prefix = "￥",
            modifier = Modifier.width(96.dp)
        )
    }
}

@Composable
private fun SalaryInput(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    prefix: String = "",
    suffix: String = "",
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = color,
        textAlign = TextAlign.Center
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (prefix.isNotEmpty()) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
                if (suffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = suffix,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun formatSalaryNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun calcMethodDescription(method: String): String = when (method) {
    "底薪 + 加班" -> "打卡时只能记录加班时长，月工资由底薪+加班工资得出，无需记录正班时长"
    "综合工作制" -> "底薪换算成正班时薪*正班时长，正班工作时长上满即为底薪，超出时长算加班时长"
    "正班 + 加班" -> "可同时记正班与加班，打卡后可估算日工资。底薪换算成正班时薪*正班时长，月底薪可能会有偏差"
    else -> "该模式时薪固定，平时加班与周末加班不进行加倍计算。"
}

@Composable
fun CalcMethodSheet(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val methods = CalcMethods
    var selected by remember(selectedMethod) { mutableStateOf(selectedMethod) }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "更换计算方式",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "选择后返回当前薪资输入界面，已输入内容会保留",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    methods.forEach { method ->
                        CalcMethodCard(
                            label = method,
                            selected = method == selected,
                            onClick = { selected = method }
                        )
                    }
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = {
                            onMethodSelected(selected)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "确认",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcMethodCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

