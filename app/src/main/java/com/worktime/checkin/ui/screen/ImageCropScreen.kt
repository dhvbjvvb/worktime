package com.worktime.checkin.ui.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCropScreen(
    imageUri: Uri,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)

    val context = LocalContext.current
    val density = LocalDensity.current
    val cropDiameterDp = 280.dp
    val cropDiameterPx = with(density) { cropDiameterDp.toPx() }
    val cropRadius = cropDiameterPx / 2f

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageUri) {
        sourceBitmap = try {
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
                Text(
                    text = "调整头像",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(52.dp))
            }

            val bitmap = sourceBitmap
            if (bitmap == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            CropEditor(
                bitmap = bitmap,
                cropDiameterDp = cropDiameterDp,
                cropDiameterPx = cropDiameterPx,
                cropRadius = cropRadius,
                onCancel = onCancel,
                onConfirm = { output ->
                    val dir = File(context.filesDir, "avatars")
                    dir.mkdirs()
                    val file = File(dir, "avatar_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { fos ->
                        output.compress(Bitmap.CompressFormat.PNG, 90, fos)
                    }
                    output.recycle()
                    onConfirm(file.absolutePath)
                }
            )
        }
    }
}

@Composable
private fun CropEditor(
    bitmap: Bitmap,
    cropDiameterDp: androidx.compose.ui.unit.Dp,
    cropDiameterPx: Float,
    cropRadius: Float,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    val bmpW = bitmap.width.toFloat()
    val bmpH = bitmap.height.toFloat()
    val baseScale = cropDiameterPx / min(bmpW, bmpH)
    val displayW = bmpW * baseScale
    val displayH = bmpH * baseScale

    var userScale by remember(bitmap) { mutableFloatStateOf(1f) }
    var userOffsetX by remember(bitmap) { mutableFloatStateOf(0f) }
    var userOffsetY by remember(bitmap) { mutableFloatStateOf(0f) }
    var containerW by remember { mutableFloatStateOf(0f) }
    var containerH by remember { mutableFloatStateOf(0f) }
    val overlayColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f)
    val cropStrokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)

    fun constrainedOffsetX(value: Float, scale: Float): Float {
        val maxOffset = max(0f, (displayW * scale - cropDiameterPx) / 2f)
        return value.coerceIn(-maxOffset, maxOffset)
    }

    fun constrainedOffsetY(value: Float, scale: Float): Float {
        val maxOffset = max(0f, (displayH * scale - cropDiameterPx) / 2f)
        return value.coerceIn(-maxOffset, maxOffset)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .onSizeChanged { size ->
                    containerW = size.width.toFloat()
                    containerH = size.height.toFloat()
                    userOffsetX = constrainedOffsetX(userOffsetX, userScale)
                    userOffsetY = constrainedOffsetY(userOffsetY, userScale)
                }
                .clipToBounds()
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (userScale * zoom).coerceIn(1f, 4f)
                        userScale = nextScale
                        userOffsetX = constrainedOffsetX(userOffsetX + pan.x, nextScale)
                        userOffsetY = constrainedOffsetY(userOffsetY + pan.y, nextScale)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(
                        width = with(density) { displayW.toDp() },
                        height = with(density) { displayH.toDp() }
                    )
                    .graphicsLayer {
                        scaleX = userScale
                        scaleY = userScale
                        translationX = userOffsetX
                        translationY = userOffsetY
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val path = Path().apply {
                            addOval(
                                Rect(cx - cropRadius, cy - cropRadius, cx + cropRadius, cy + cropRadius)
                            )
                            addRect(Rect(Offset.Zero, size))
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(path, color = overlayColor)
                        drawOval(
                            color = cropStrokeColor,
                            topLeft = Offset(cx - cropRadius, cy - cropRadius),
                            size = Size(cropDiameterPx, cropDiameterPx),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )

            Box(
                modifier = Modifier
                    .size(cropDiameterDp)
                    .align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("取消", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    if (containerW <= 0f || containerH <= 0f) return@Button

                    val centerX = containerW / 2f
                    val centerY = containerH / 2f
                    val imageCenterX = centerX + userOffsetX
                    val imageCenterY = centerY + userOffsetY
                    val scaledDisplayW = displayW * userScale
                    val scaledDisplayH = displayH * userScale
                    val totalScale = baseScale * userScale
                    val cropLeft = centerX - cropRadius
                    val cropTop = centerY - cropRadius

                    val srcLeft = (cropLeft - (imageCenterX - scaledDisplayW / 2f)) / totalScale
                    val srcTop = (cropTop - (imageCenterY - scaledDisplayH / 2f)) / totalScale
                    val srcSize = cropDiameterPx / totalScale

                    val srcRect = android.graphics.Rect(
                        srcLeft.roundToInt().coerceIn(0, bitmap.width - 1),
                        srcTop.roundToInt().coerceIn(0, bitmap.height - 1),
                        (srcLeft + srcSize).roundToInt().coerceIn(1, bitmap.width),
                        (srcTop + srcSize).roundToInt().coerceIn(1, bitmap.height)
                    )

                    val outputSize = 256
                    val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(output)
                    val clipPath = android.graphics.Path().apply {
                        addOval(
                            0f,
                            0f,
                            outputSize.toFloat(),
                            outputSize.toFloat(),
                            android.graphics.Path.Direction.CW
                        )
                    }
                    canvas.clipPath(clipPath)
                    canvas.drawBitmap(
                        bitmap,
                        srcRect,
                        android.graphics.RectF(0f, 0f, outputSize.toFloat(), outputSize.toFloat()),
                        null
                    )
                    onConfirm(output)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("确认", fontWeight = FontWeight.Bold)
            }
        }
    }
}
