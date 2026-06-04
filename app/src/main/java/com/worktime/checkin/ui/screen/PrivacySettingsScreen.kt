package com.worktime.checkin.ui.screen

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class PermissionItem(
    val icon: ImageVector,
    val name: String,
    val purpose: String,
    val tint: Color
)

/** 已知权限的展示映射。在 manifest 新增权限后只需往这里加一行即可。 */
// 新权限加一行 <uses-permission> 后，在这里补一条即可自动展示
// 仅列出需要用户手动授权的敏感权限（存储/相册/录音/相机/悬浮窗/自启动等）
// manifest 新增 <uses-permission> 后在这里补一行即可
private val knownPermissions = mapOf(
    "android.permission.POST_NOTIFICATIONS" to PermissionItem(
        icon = Icons.Default.Notifications,
        name = "消息推送",
        purpose = "用于向你发送打卡提醒和重要通知",
        tint = Color(0xFFE85D3A)
    ),
    "android.permission.READ_MEDIA_IMAGES" to PermissionItem(
        icon = Icons.Default.Image,
        name = "读取存储",
        purpose = "设置头像时从相册选取图片，仅读取你选中的那一张",
        tint = Color(0xFFC08A3E)
    ),
    "android.permission.INTERNET" to PermissionItem(
        icon = Icons.Default.Language,
        name = "网络访问",
        purpose = "用于每日一言功能从网络获取格言内容",
        tint = Color(0xFF4A5BC6)
    ),
    "android.permission.ACCESS_FINE_LOCATION" to PermissionItem(
        icon = Icons.Default.LocationOn,
        name = "精确定位",
        purpose = "获取你当前所在城市的天气信息",
        tint = Color(0xFF4CAF50)
    ),
    "android.permission.ACCESS_COARSE_LOCATION" to PermissionItem(
        icon = Icons.Default.LocationOn,
        name = "大致定位",
        purpose = "GPS不可用时通过网络获取大致位置",
        tint = Color(0xFF4CAF50)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    // 从 manifest 动态读取声明的权限，自动与 knownPermissions 对齐
    val displayedPermissions = remember {
        val declared: Set<String> = try {
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions?.toSet() ?: emptySet()
        } catch (_: Exception) { emptySet() }

        declared.mapNotNull { perm -> knownPermissions[perm] }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "\u9690\u79C1\u4E0E\u5B89\u5168",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "数据安全承诺",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = "本应用不会收集、上传或分享你的任何个人数据。所有打卡记录仅保存在你的设备本地，你可以随时删除。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 12.dp))

            Text(
                text = "权限说明",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                if (displayedPermissions.isEmpty()) {
                    Text(
                        text = "✅ 本应用无需额外敏感权限",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Column {
                        displayedPermissions.forEachIndexed { index, perm ->
                            PermissionRow(perm)
                            if (index < displayedPermissions.lastIndex) {
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
    }
}

@Composable
private fun PermissionRow(perm: PermissionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(perm.tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = perm.icon, contentDescription = null, tint = perm.tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = perm.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = perm.purpose,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

