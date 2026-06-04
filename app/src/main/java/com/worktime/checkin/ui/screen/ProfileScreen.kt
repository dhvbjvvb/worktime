package com.worktime.checkin.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worktime.checkin.data.ProfileRepository
import com.worktime.checkin.data.ProfileSettings
import com.worktime.checkin.data.ThemeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.worktime.checkin.ui.navigation.SettingsSubPage
import com.worktime.checkin.ui.theme.ThemeMode

private data class MenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val tint: Color = Color.Unspecified,
    val onClick: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigate: (SettingsSubPage) -> Unit = {}) {
    val context = LocalContext.current
    val themeRepository = remember { ThemeSettingsRepository(context.applicationContext) }
    val profileRepository = remember { ProfileRepository(context.applicationContext) }
    val themeMode by themeRepository.mode.collectAsState(initial = ThemeMode.System)
    val profile by profileRepository.settings.collectAsState(initial = ProfileSettings())

    val themeLabel = when (themeMode) {
        ThemeMode.System -> "跟随系统"
        ThemeMode.Light -> "浅色模式"
        ThemeMode.Dark -> "深色模式"
    }

    val menuSections = listOf(
        listOf(
            MenuItem(Icons.Default.Palette, "主题设置", themeLabel, Color(0xFF7C5CC4), onClick = { onNavigate(SettingsSubPage.Theme) }),
            MenuItem(Icons.Default.NotificationsActive, "消息通知", "", Color(0xFFE85D3A), onClick = { onNavigate(SettingsSubPage.Notifications) }),
            MenuItem(Icons.Default.Payments, "薪资设置", "底薪、时薪与倍率", Color(0xFFC08A3E), onClick = { onNavigate(SettingsSubPage.Salary) })
        ),
        listOf(
            MenuItem(Icons.AutoMirrored.Filled.HelpOutline, "使用教程", "隐藏操作与常用入口", Color(0xFF3478F6), onClick = { onNavigate(SettingsSubPage.Tutorial) }),
            MenuItem(Icons.Default.Security, "隐私与安全", "", Color(0xFF5B9A8B), onClick = { onNavigate(SettingsSubPage.Privacy) }),
            MenuItem(Icons.Default.Feedback, "意见反馈", "", Color(0xFF4A5BC6), onClick = { onNavigate(SettingsSubPage.Feedback) }),
            MenuItem(Icons.Default.Info, "关于", "v2.3.1", Color(0xFF90909A))
        )
    )
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(50.dp),
                windowInsets = TopAppBarDefaults.windowInsets
                    .only(WindowInsetsSides.Horizontal),
                title = {
                    Column {
                        Text(
                            text = "我的",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "管理账户与偏好设置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Profile header card ──
            item {
                val nickname = profile.nickname
                val avatarChar = nickname.firstOrNull()?.toString() ?: "U"
                var avatarBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(profile.avatarUri) {
                    avatarBitmap = profile.avatarUri?.let { path ->
                        try {
                            BitmapFactory.decodeFile(path)
                        } catch (_: Exception) { null }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    onClick = { onNavigate(SettingsSubPage.ProfileEdit) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!.asImageBitmap(),
                                    contentDescription = "头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = avatarChar,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nickname,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Daily quote ──
            item {
                DailyQuoteCard()
            }

            // ── Menu sections ──
            menuSections.forEachIndexed { sectionIndex, items ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column {
                            items.forEachIndexed { index, menuItem ->
                                MenuRow(menuItem)
                                if (index < items.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem) {
    val isClickable = item.onClick != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) Modifier.clickable { item.onClick?.invoke() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(item.tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            if (item.subtitle.isNotEmpty()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isClickable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DailyQuoteCard() {
    var quote by remember { mutableStateOf("加载中…") }
    var from by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun fetch() {
        scope.launch {
            quote = "加载中…"
            from = ""
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = URL("https://v1.hitokoto.cn/").readText()
                    val obj = JSONObject(json)
                    obj.optString("hitokoto") to obj.optString("from")
                } catch (_: Exception) {
                    null
                }
            }
            if (result != null) {
                quote = result.first
                from = result.second.ifEmpty { "佚名" }
            } else {
                quote = "生活不止眼前的苟且，还有诗和远方。"
                from = "网络"
            }
        }
    }

    LaunchedEffect(Unit) { fetch() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "每日一言",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (from.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "—— $from",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = { fetch() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

