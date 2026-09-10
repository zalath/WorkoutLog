package com.kzwdaw.kyguukk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChartSeries(
    val name: String,
    val color: Color,
    val points: List<Pair<Long, Double>> // (timestamp, value)
)

/**
 * 简易折线图。支持单条或多条（左右对比）曲线。
 * 横轴时间、纵轴数值，含网格与刻度。
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier
) {
    if (series.all { it.points.isEmpty() }) {
        Box(modifier.height(200.dp), contentAlignment = Alignment.Center) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val allPoints = series.flatMap { it.points }
    val minVal = (allPoints.minOf { it.second })
    val maxVal = (allPoints.maxOf { it.second })
    val valPadding = ((maxVal - minVal) * 0.15f).coerceAtLeast(1.0)
    val yMin = minVal - valPadding
    val yMax = maxVal + valPadding

    val minT = allPoints.minOf { it.first }
    val maxT = allPoints.maxOf { it.first }

    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current

    val labelTextSize = with(density) { 11.dp.toPx() }
    val titleTextSize = with(density) { 12.dp.toPx() }

    val timeLabelPaint = android.graphics.Paint().apply {
        color = textColor.toArgb()
        textSize = labelTextSize
        isAntiAlias = true
    }
    val valueLabelPaint = android.graphics.Paint().apply {
        color = textColor.toArgb()
        textSize = labelTextSize
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.RIGHT
    }

    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        val leftPad = 44f
        val rightPad = 16f
        val topPad = 16f
        val bottomPad = 40f
        val plotLeft = leftPad
        val plotTop = topPad
        val plotRight = size.width - rightPad
        val plotBottom = size.height - bottomPad
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        // 纵向网格 + Y刻度（4段）
        val steps = 4
        for (i in 0..steps) {
            val y = plotTop + plotH * i / steps
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1f
            )
            val v = yMax - (yMax - yMin) * i / steps
            drawContext.canvas.nativeCanvas.drawText(
                "%.1f".format(v),
                plotLeft - 6f,
                y + labelTextSize / 3f,
                valueLabelPaint
            )
        }

        // 坐标轴
        drawLine(
            color = axisColor, start = Offset(plotLeft, plotTop),
            end = Offset(plotLeft, plotBottom), strokeWidth = 1.5f
        )
        drawLine(
            color = axisColor, start = Offset(plotLeft, plotBottom),
            end = Offset(plotRight, plotBottom), strokeWidth = 1.5f
        )

        fun xFor(t: Long): Float {
            return if (maxT == minT) plotLeft + plotW / 2f
            else plotLeft + (t - minT).toFloat() / (maxT - minT).toFloat() * plotW
        }

        fun yFor(v: Double): Float {
            return plotBottom - ((v - yMin) / (yMax - yMin)).toFloat() * plotH
        }

        // X轴时间标签（首/中/尾）
        val timeLabels = listOf(minT, (minT + maxT) / 2, maxT).distinct()
        timeLabels.forEach { t ->
            val x = xFor(t)
            drawContext.canvas.nativeCanvas.drawText(
                t.toDateStr(),
                x,
                plotBottom + labelTextSize + 8f,
                timeLabelPaint.apply { textAlign = android.graphics.Paint.Align.CENTER }
            )
        }

        // 每条曲线
        series.forEach { s ->
            if (s.points.isEmpty()) return@forEach
            val path = Path()
            s.points.forEachIndexed { i, (t, v) ->
                val x = xFor(t)
                val y = yFor(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = s.color, style = Stroke(width = 4f))

            // 数据点
            s.points.forEach { (t, v) ->
                val x = xFor(t)
                val y = yFor(v)
                drawCircle(color = s.color, radius = 5f, center = Offset(x, y))
                drawCircle(color = Color.White, radius = 2.5f, center = Offset(x, y))
            }
        }
    }
}

@Composable
fun ChartLegend(series: List<ChartSeries>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(s.color, CircleShape)
                )
                Text(
                    text = s.name,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                )
            }
        }
    }
}

// 提供给 LineChart 内部使用的小工具
private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
