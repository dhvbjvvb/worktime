package com.worktime.checkin.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class TutorialSection(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tint: Color,
    val steps: List<String>
)

@Composable
fun TutorialScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val sections = remember {
        listOf(
            TutorialSection(
                icon = Icons.Default.CalendarMonth,
                title = "记录页日历",
                subtitle = "切换月份、选择日期、进入隐藏操作",
                tint = Color(0xFF3478F6),
                steps = listOf(
                    "在记录页的日历卡片上左右滑动，可以切换上个月或下个月。",
                    "点击任意日期，会把当天设为当前打卡日期。",
                    "长按日历卡片里的任意号数，可以打开多日期编辑日历。",
                    "点底部“日历 / 多日期编辑”卡片，也可以进入多日期编辑。"
                )
            ),
            TutorialSection(
                icon = Icons.Default.EditCalendar,
                title = "多日期编辑",
                subtitle = "批量记录工时或清除记录",
                tint = Color(0xFF2E8B57),
                steps = listOf(
                    "进入完整日历后，点击多个日期可以批量选中。",
                    "选中日期后点击“记录工时”，可以一次给多天保存打卡记录。",
                    "如果想删除打卡记录，先选中日期，再点击“清除记录”。",
                    "如果没有选中日期，操作按钮会提示先选择日期。"
                )
            ),
            TutorialSection(
                icon = Icons.Default.EventAvailable,
                title = "打卡记录",
                subtitle = "填写班次、工时、请假与日结",
                tint = Color(0xFFE85D3A),
                steps = listOf(
                    "记录页中间的日期卡片显示当前选中的日期。",
                    "点击“打卡”按钮，可以填写班次、工时类型、正班、加班、周末和节假日工时。",
                    "请假时选择请假类型，保存后当天工资会按 0 处理。",
                    "保存后日历会显示当天摘要，报表和统计也会同步更新。"
                )
            ),
            TutorialSection(
                icon = Icons.Default.Payments,
                title = "月度明细报表",
                subtitle = "查看每月记录和导出",
                tint = Color(0xFFC08A3E),
                steps = listOf(
                    "点记录页底部“报表 / 查看月度明细”卡片，可以进入月度明细。",
                    "报表页顶部左右箭头可以切换月份。",
                    "有记录的日期会显示完整明细，未记录日期会显示为淡色短行。",
                    "点击 Excel 会导出 CSV 文件，点击文字导出会复制本月汇总文本。"
                )
            ),
            TutorialSection(
                icon = Icons.Default.QueryStats,
                title = "统计板块",
                subtitle = "看汇总，不重复看明细",
                tint = Color(0xFF7C5CC4),
                steps = listOf(
                    "底部切到“统计”，可以查看本月预计工资、工时结构和趋势。",
                    "统计页适合看总体情况，报表页适合查每天的具体记录。",
                    "统计页也支持切换月份，方便对比不同月份。"
                )
            ),
            TutorialSection(
                icon = Icons.Default.Settings,
                title = "我的与设置",
                subtitle = "工资算法、提醒、主题和隐私",
                tint = Color(0xFF5B9A8B),
                steps = listOf(
                    "在“我的”里点击“薪资设置”，可以修改底薪、时薪和倍率。",
                    "点击“消息通知”，可以设置打卡提醒。",
                    "点击“主题设置”，可以切换浅色、深色或跟随系统。",
                    "点击“隐私与安全”，可以查看本地数据和权限说明。"
                )
            )
        )
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "浣跨敤鏁欑▼",
                onBack = onBack
            )
        }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = flatCardElevation()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "甯哥敤鎿嶄綔鍏ュ彛",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "这里整理了需要点击、长按或滑动才会出现的操作。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                    }
                }
            }
            sections.forEach { section ->
                item { TutorialSectionCard(section) }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TutorialSectionCard(section: TutorialSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = flatCardElevation()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(section.tint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = section.tint,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                section.steps.forEachIndexed { index, step ->
                    TutorialStep(index = index + 1, text = step, tint = section.tint)
                }
            }
        }
    }
}

@Composable
private fun TutorialStep(
    index: Int,
    text: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
    }
}
