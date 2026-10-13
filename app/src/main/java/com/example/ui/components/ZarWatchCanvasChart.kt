package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PriceObservation
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min

@Composable
fun ZarWatchCanvasChart(
    observations: List<PriceObservation>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF10B981),
    fillGradientColors: List<Color> = listOf(Color(0x3310B981), Color(0x0010B981)),
    usePersianDigits: Boolean = true,
    useJalaliCalendar: Boolean = true,
    unitLabel: String = "تومان"
) {
    if (observations.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "داده‌ای برای نمایش موجود نیست",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    var selectedIndex by remember(observations) { mutableStateOf<Int?>(null) }

    val prices = remember(observations) { observations.map { it.priceToman.toDouble() } }
    val minPrice = remember(prices) { prices.minOrNull() ?: 0.0 }
    val maxPrice = remember(prices) { prices.maxOrNull() ?: 1.0 }
    val priceRange = remember(minPrice, maxPrice) { max(1.0, maxPrice - minPrice) }

    Column(modifier = modifier) {
        // Inspection Tooltip / Summary Header
        val activeObs = selectedIndex?.let { observations.getOrNull(it) } ?: observations.lastOrNull()
        if (activeObs != null) {
            val firstPrice = observations.first().priceToman
            val currentPrice = activeObs.priceToman
            val change = currentPrice.subtract(firstPrice)
            val changePercent = if (firstPrice > BigDecimal.ZERO) {
                change.multiply(BigDecimal("100")).divide(firstPrice, 2, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = PersianFormatters.formatToman(currentPrice, usePersianDigits, true),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = PersianFormatters.formatDate(activeObs.timestamp, useJalaliCalendar, usePersianDigits, true),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (change >= BigDecimal.ZERO) Color(0x2210B981) else Color(0x22EF4444)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = PersianFormatters.formatPercent(changePercent, usePersianDigits, true),
                        color = if (change >= BigDecimal.ZERO) Color(0xFF10B981) else Color(0xFFEF4444),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(observations) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val count = observations.size
                                if (count > 1) {
                                    val index = ((offset.x / size.width) * (count - 1))
                                        .toInt()
                                        .coerceIn(0, count - 1)
                                    selectedIndex = index
                                }
                            },
                            onDrag = { change, _ ->
                                val count = observations.size
                                if (count > 1) {
                                    val index = ((change.position.x / size.width) * (count - 1))
                                        .toInt()
                                        .coerceIn(0, count - 1)
                                    selectedIndex = index
                                }
                            },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null }
                        )
                    }
                    .pointerInput(observations) {
                        detectTapGestures(
                            onTap = { offset ->
                                val count = observations.size
                                if (count > 1) {
                                    val index = ((offset.x / size.width) * (count - 1))
                                        .toInt()
                                        .coerceIn(0, count - 1)
                                    selectedIndex = index
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val paddingBottom = 24f
                val paddingTop = 16f
                val chartHeight = h - paddingTop - paddingBottom
                val count = observations.size

                if (count < 2) return@Canvas

                val stepX = w / (count - 1)

                // Grid lines (Min and Max horizontal guidelines)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(0f, paddingTop),
                    end = Offset(w, paddingTop),
                    pathEffect = dashEffect,
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(0f, h - paddingBottom),
                    end = Offset(w, h - paddingBottom),
                    pathEffect = dashEffect,
                    strokeWidth = 1.dp.toPx()
                )

                // Calculate points
                val points = observations.mapIndexed { i, obs ->
                    val x = i * stepX
                    val normalizedY = ((obs.priceToman.toDouble() - minPrice) / priceRange).toFloat()
                    val y = (h - paddingBottom) - (normalizedY * chartHeight)
                    Offset(x, y)
                }

                // Construct smooth or segmented path
                val strokePath = Path()
                val fillPath = Path()

                points.forEachIndexed { i, p ->
                    if (i == 0) {
                        strokePath.moveTo(p.x, p.y)
                        fillPath.moveTo(p.x, h - paddingBottom)
                        fillPath.lineTo(p.x, p.y)
                    } else {
                        val prev = points[i - 1]
                        val midX = (prev.x + p.x) / 2f
                        strokePath.cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
                        fillPath.cubicTo(midX, prev.y, midX, p.y, p.x, p.y)
                    }
                }

                fillPath.lineTo(w, h - paddingBottom)
                fillPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = fillGradientColors,
                        startY = paddingTop,
                        endY = h - paddingBottom
                    )
                )

                // Draw Line Stroke
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw Inspection Cursor if active
                selectedIndex?.let { idx ->
                    val selPoint = points.getOrNull(idx)
                    if (selPoint != null) {
                        // Vertical Crosshair
                        drawLine(
                            color = Color(0x88CBD5E1),
                            start = Offset(selPoint.x, paddingTop),
                            end = Offset(selPoint.x, h - paddingBottom),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Highlight dot
                        drawCircle(
                            color = Color(0xFFFFFFFF),
                            radius = 6.dp.toPx(),
                            center = selPoint
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = selPoint
                        )
                    }
                }
            }
        }
    }
}
