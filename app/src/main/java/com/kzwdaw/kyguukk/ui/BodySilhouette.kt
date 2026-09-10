package com.kzwdaw.kyguukk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kzwdaw.kyguukk.R

enum class SilhouetteGender { MALE, FEMALE }

data class MetricAnchor(
    val key: String,
    val x: Float,
    val y: Float,
    val align: Align = Align.RIGHT
) {
    enum class Align { LEFT, RIGHT, CENTER }
}

val SilhouetteAnchors: List<MetricAnchor> = listOf(
    MetricAnchor("shoulder_width", 0.5f, 0.20f, MetricAnchor.Align.CENTER),
    MetricAnchor("shoulder_thickness", 0.62f, 0.17f, MetricAnchor.Align.LEFT),
    MetricAnchor("chest", 0.5f, 0.29f, MetricAnchor.Align.CENTER),
    MetricAnchor("waist", 0.5f, 0.4f, MetricAnchor.Align.CENTER),
    MetricAnchor("hip", 0.5f, 0.5f, MetricAnchor.Align.CENTER),
    MetricAnchor("upper_arm_left", 0.30f, 0.26f, MetricAnchor.Align.RIGHT),
    MetricAnchor("upper_arm_right", 0.70f, 0.26f, MetricAnchor.Align.LEFT),
    MetricAnchor("forearm_left", 0.24f, 0.40f, MetricAnchor.Align.RIGHT),
    MetricAnchor("forearm_right", 0.76f, 0.40f, MetricAnchor.Align.LEFT),
    MetricAnchor("thigh_left", 0.28f, 0.62f, MetricAnchor.Align.RIGHT),
    MetricAnchor("thigh_right", 0.72f, 0.62f, MetricAnchor.Align.LEFT),
    MetricAnchor("calf_left", 0.26f, 0.78f, MetricAnchor.Align.RIGHT),
    MetricAnchor("calf_right", 0.74f, 0.78f, MetricAnchor.Align.LEFT),
)

data class SilhouetteLabel(
    val text: String,
    val color: Color,
    val anchor: MetricAnchor,
    val metricItemId: String
)

/** 绘制时缓存的标签边界，供点击命中测试使用。 */
private data class LabelBounds(
    val label: SilhouetteLabel,
    val rect: Rect
)

/**
 * 绘制人形图片 + 半透明底色数值标签，支持点击。
 */
@Composable
fun BodySilhouette(
    gender: SilhouetteGender,
    labels: List<SilhouetteLabel> = emptyList(),
    onLabelClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val painter = when (gender) {
        SilhouetteGender.MALE -> painterResource(R.drawable.male)
        SilhouetteGender.FEMALE -> painterResource(R.drawable.female)
    }
    val density = LocalDensity.current
    val textSizePx = with(density) { 13.dp.toPx() }
    val hPadPx = with(density) { 12.dp.toPx() }
    val vPadPx = with(density) { 10.dp.toPx() }
    val cornerRadius = with(density) { 4.dp.toPx() }

    val textPaint = remember(gender) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = textSizePx
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
        }
    }
    val bgPaint = remember(gender) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(90, 0, 0, 0) // ~35% 黑色半透明
        }
    }

    val imageAspect = 1664f / 2496f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(imageAspect)
    ) {
        Image(
            painter = painter,
            contentDescription = if (gender == SilhouetteGender.MALE) "男性剪影" else "女性剪影",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )

        // 缓存标签边界供点击检测
        val boundsList = remember(labels) { mutableListOf<LabelBounds>() }

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(labels) {
                    detectTapGestures { tap ->
                        boundsList.firstOrNull { it.rect.contains(tap) }?.let {
                            onLabelClick(it.label.metricItemId)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            boundsList.clear()

            labels.forEach { label ->
                val x = label.anchor.x * w
                val y = label.anchor.y * h
                val text = label.text

                textPaint.color = label.color.toArgb()
                textPaint.textAlign = when (label.anchor.align) {
                    MetricAnchor.Align.LEFT -> android.graphics.Paint.Align.LEFT
                    MetricAnchor.Align.RIGHT -> android.graphics.Paint.Align.RIGHT
                    MetricAnchor.Align.CENTER -> android.graphics.Paint.Align.CENTER
                }

                val textW = textPaint.measureText(text)
                val textH = textSizePx

                // 计算标签边界（含 padding）
                val (left, right) = when (label.anchor.align) {
                    MetricAnchor.Align.LEFT -> x to x + textW
                    MetricAnchor.Align.RIGHT -> (x - textW) to x
                    MetricAnchor.Align.CENTER -> (x - textW / 2f) to (x + textW / 2f)
                }
                val top = y - textH / 2f
                val bottom = y + textH / 2f

                // 绘制半透明圆角背景
                val bgPath = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = left - hPadPx,
                            top = top - vPadPx,
                            right = right + hPadPx,
                            bottom = bottom + vPadPx,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                        )
                    )
                }
                drawPath(path = bgPath, color = Color.White.copy(alpha = 0.65f))

                // 绘制文字（baseline 调整使垂直居中）
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    x,
                    y + textH / 3f,
                    textPaint
                )

                // 缓存边界
                boundsList.add(
                    LabelBounds(
                        label = label,
                        rect = Rect(
                            left = left - hPadPx,
                            top = top - vPadPx,
                            right = right + hPadPx,
                            bottom = bottom + vPadPx
                        )
                    )
                )
            }
        }
    }
}
