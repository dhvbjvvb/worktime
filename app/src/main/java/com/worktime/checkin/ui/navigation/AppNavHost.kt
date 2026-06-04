package com.worktime.checkin.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.worktime.checkin.data.SalarySettings
import com.worktime.checkin.data.SalarySettingsRepository
import com.worktime.checkin.ui.screen.CheckInSheet
import com.worktime.checkin.ui.screen.CalcMethodSheet
import com.worktime.checkin.ui.screen.FullCalendarScreen
import com.worktime.checkin.ui.screen.MonthlyReportScreen
import com.worktime.checkin.ui.screen.ProfileScreen
import com.worktime.checkin.ui.screen.RecordsScreen
import com.worktime.checkin.ui.screen.StatsScreen
import com.worktime.checkin.ui.screen.FeedbackSettingsScreen
import com.worktime.checkin.ui.screen.NotificationsSettingsScreen
import com.worktime.checkin.ui.screen.PrivacySettingsScreen
import com.worktime.checkin.ui.screen.ProfileEditScreen
import com.worktime.checkin.ui.screen.SalarySettingsScreen
import com.worktime.checkin.ui.screen.ThemeSettingsScreen
import com.worktime.checkin.ui.screen.TutorialScreen
import kotlinx.coroutines.launch
import java.util.Calendar

private data class BottomTab(
    val label: String,
    val icon: ImageVector
)

enum class SettingsSubPage { Theme, Notifications, Salary, Feedback, Privacy, ProfileEdit, Tutorial }

private val tabs = listOf(
    BottomTab("记录", Icons.AutoMirrored.Filled.EventNote),
    BottomTab("统计", Icons.Filled.QueryStats),
    BottomTab("我的", Icons.Filled.AccountCircle)
)

@Composable
fun AppNavHost(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    settingsSubPage: SettingsSubPage?,
    onSettingsBack: () -> Unit,
    onSettingsNavigate: (SettingsSubPage) -> Unit
) {
    var showCalendarOverlay by remember { mutableStateOf(false) }
    var showCheckInOverlay by remember { mutableStateOf(false) }
    var showCalcMethodOverlay by remember { mutableStateOf(false) }
    var showMonthlyReport by remember { mutableStateOf(false) }
    var checkInDate by remember { mutableStateOf(currentDateTriple()) }
    var checkInDates by remember { mutableStateOf(listOf(checkInDate)) }
    var checkInSheetHeightFraction by remember { mutableStateOf(0.80f) }
    var inlineCheckInLeaveType by remember { mutableStateOf(false) }
    var calendarYear by remember { mutableStateOf(checkInDate.first) }
    var calendarMonth by remember { mutableStateOf(checkInDate.second) }
    val context = LocalContext.current
    val salaryRepository = remember { SalarySettingsRepository(context.applicationContext) }
    val salarySettings by salaryRepository.settings.collectAsState(initial = SalarySettings())
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!showMonthlyReport) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                showMonthlyReport = false
                                onTabSelected(index)
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when {
                    selectedTab == 0 && showMonthlyReport ->
                        MonthlyReportScreen(
                            year = calendarYear,
                            month = calendarMonth,
                            onBack = { showMonthlyReport = false }
                        )
                    selectedTab == 2 && settingsSubPage == SettingsSubPage.Theme ->
                        ThemeSettingsScreen(onBack = onSettingsBack)
                    selectedTab == 2 && settingsSubPage == SettingsSubPage.Notifications ->
                        NotificationsSettingsScreen(onBack = onSettingsBack)
                    selectedTab == 2 && settingsSubPage == SettingsSubPage.Salary ->
                        SalarySettingsScreen(
                            onBack = onSettingsBack,
                            selectedCalcMethod = salarySettings.selectedCalcMethod,
                            onCalcMethodClick = { showCalcMethodOverlay = true }
                        )
                    selectedTab == 2 && settingsSubPage == SettingsSubPage.Feedback ->
                        FeedbackSettingsScreen(onBack = onSettingsBack)
                    selectedTab == 2 && settingsSubPage == SettingsSubPage.Privacy ->
                        PrivacySettingsScreen(onBack = onSettingsBack)
                    selectedTab == 2 && settingsSubPage == SettingsSubPage.ProfileEdit ->
                        ProfileEditScreen(onBack = onSettingsBack)
                    selectedTab == 2 && settingsSubPage == SettingsSubPage.Tutorial ->
                        TutorialScreen(onBack = onSettingsBack)
                    else ->
                        when (selectedTab) {
                            0 -> RecordsScreen(
                                displayedYear = calendarYear,
                                displayedMonth = calendarMonth,
                                checkInDate = checkInDate,
                                onYearMonthChange = { year, month ->
                                    calendarYear = year
                                    calendarMonth = month
                                },
                                onDateSelected = { date ->
                                    checkInDate = date
                                },
                                onCalendarClick = { showCalendarOverlay = true },
                                onReportClick = { showMonthlyReport = true },
                                onCheckInClick = { date ->
                                    checkInDate = date
                                    checkInDates = listOf(date)
                                    checkInSheetHeightFraction = 0.80f
                                    inlineCheckInLeaveType = false
                                    showCheckInOverlay = true
                                }
                            )
                            1 -> StatsScreen()
                            2 -> ProfileScreen(onNavigate = onSettingsNavigate)
                        }
                }
            }
        }

        if (showCalendarOverlay || showCheckInOverlay || showCalcMethodOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .noRippleClickable {
                        showCalendarOverlay = false
                        showCheckInOverlay = false
                        showCalcMethodOverlay = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showCalendarOverlay,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(
                animationSpec = tween(400),
                initialOffsetY = { it }
            ),
            exit = slideOutVertically(
                animationSpec = tween(0),
                targetOffsetY = { it }
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(modifier = Modifier.noRippleClickable {}) {
                    FullCalendarScreen(
                        displayedYear = calendarYear,
                        displayedMonth = calendarMonth,
                        onYearMonthChange = { year, month ->
                            calendarYear = year
                            calendarMonth = month
                        },
                        onRecordWork = { dates ->
                            checkInDate = dates.first()
                            checkInDates = dates
                            checkInSheetHeightFraction = 0.62f
                            inlineCheckInLeaveType = true
                            showCheckInOverlay = true
                        },
                        onBack = { showCalendarOverlay = false }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showCheckInOverlay,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(
                animationSpec = tween(400),
                initialOffsetY = { it }
            ),
            exit = slideOutVertically(
                animationSpec = tween(0),
                targetOffsetY = { it }
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(modifier = Modifier.noRippleClickable {}) {
                    CheckInSheet(
                        checkInDate = checkInDate,
                        checkInDates = checkInDates,
                        heightFraction = checkInSheetHeightFraction,
                        inlineLeaveType = inlineCheckInLeaveType,
                        salaryCalcMethod = salarySettings.selectedCalcMethod,
                        onDismiss = { showCheckInOverlay = false }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showCalcMethodOverlay,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(
                animationSpec = tween(400),
                initialOffsetY = { it }
            ),
            exit = slideOutVertically(
                animationSpec = tween(0),
                targetOffsetY = { it }
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(modifier = Modifier.noRippleClickable {}) {
                    CalcMethodSheet(
                        selectedMethod = salarySettings.selectedCalcMethod,
                        onMethodSelected = { method ->
                            scope.launch { salaryRepository.setSelectedCalcMethod(method) }
                        },
                        onDismiss = { showCalcMethodOverlay = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

private fun currentDateTriple(): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance()
    return Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
