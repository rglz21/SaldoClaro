package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.CategorySlice
import com.example.ui.viewmodel.ChartBarPoint
import com.example.util.FinanceFormatters
import kotlin.math.max

@Composable
fun InteractiveCashflowBarChart(
    points: List<ChartBarPoint>,
    showIncomeComparison: Boolean,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    var selectedIndex by remember(points) {
        val lastNonZero = points.indexOfLast { it.expense > 0.0 || it.income > 0.0 }
        mutableIntStateOf(if (lastNonZero >= 0) lastNonZero else -1)
    }

    var played by remember(points) { mutableStateOf(false) }
    LaunchedEffect(points) { played = true }
    val animProgress by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "bar_chart_anim",
    )

    val maxValue = remember(points, showIncomeComparison) {
        val raw = points.maxOfOrNull { p ->
            if (showIncomeComparison) max(p.expense, p.income) else p.expense
        } ?: 0.0
        if (raw <= 0.0) 100.0 else raw * 1.15
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected point callout
        val selected = points.getOrNull(selectedIndex)
        if (selected != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selected.fullLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Gasto: ${FinanceFormatters.currency(selected.expense, hideBalances)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                            color = financeColors.expense,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (showIncomeComparison) {
                            Text(
                                text = "Ing: ${FinanceFormatters.currency(selected.income, hideBalances)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                color = financeColors.income,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Y-axis max hint + Canvas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (hideBalances) "Máx: •••" else "Máx: ${FinanceFormatters.compactCurrency(maxValue)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                color = labelColor,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color = financeColors.expense, label = "Gastos")
                if (showIncomeComparison) {
                    LegendDot(color = financeColors.income, label = "Ingresos")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty() && size.width > 0) {
                            val slotWidth = size.width.toFloat() / points.size.toFloat()
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.isEmpty()) return@Canvas
                val chartHeight = size.height - 8.dp.toPx()
                val slotWidth = size.width / points.size.toFloat()

                // Horizontal grid lines (4 lines)
                for (i in 0..3) {
                    val y = chartHeight * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val isDense = points.size > 15
                val barCorner = CornerRadius(
                    x = if (isDense) 3.dp.toPx() else 6.dp.toPx(),
                    y = if (isDense) 3.dp.toPx() else 6.dp.toPx(),
                )

                points.forEachIndexed { index, point ->
                    val slotLeft = index * slotWidth

                    if (index == selectedIndex) {
                        drawRoundRect(
                            color = highlightBg,
                            topLeft = Offset(slotLeft + 1.dp.toPx(), 0f),
                            size = Size((slotWidth - 2.dp.toPx()).coerceAtLeast(2f), chartHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )
                    }

                    val expenseHeight = ((point.expense / maxValue).toFloat().coerceIn(0f, 1f)) * chartHeight * animProgress
                    val incomeHeight = ((point.income / maxValue).toFloat().coerceIn(0f, 1f)) * chartHeight * animProgress

                    if (showIncomeComparison && !isDense) {
                        val barWidth = (slotWidth * 0.30f).coerceIn(4.dp.toPx(), 16.dp.toPx())
                        val gap = 3.dp.toPx()
                        val groupWidth = barWidth * 2 + gap
                        val startX = slotLeft + (slotWidth - groupWidth) / 2f

                        if (incomeHeight > 0.5f) {
                            drawRoundRect(
                                color = if (index == selectedIndex) financeColors.income else financeColors.income.copy(alpha = 0.78f),
                                topLeft = Offset(startX, chartHeight - incomeHeight),
                                size = Size(barWidth, incomeHeight),
                                cornerRadius = barCorner,
                            )
                        }
                        if (expenseHeight > 0.5f) {
                            drawRoundRect(
                                color = if (index == selectedIndex) financeColors.expense else financeColors.expense.copy(alpha = 0.85f),
                                topLeft = Offset(startX + barWidth + gap, chartHeight - expenseHeight),
                                size = Size(barWidth, expenseHeight),
                                cornerRadius = barCorner,
                            )
                        }
                    } else {
                        val barWidth = if (isDense) {
                            (slotWidth * 0.62f).coerceAtLeast(3.dp.toPx())
                        } else {
                            (slotWidth * 0.48f).coerceIn(8.dp.toPx(), 26.dp.toPx())
                        }
                        val barLeft = slotLeft + (slotWidth - barWidth) / 2f
                        if (expenseHeight > 0.5f) {
                            drawRoundRect(
                                color = if (index == selectedIndex) financeColors.expense else financeColors.expense.copy(alpha = 0.82f),
                                topLeft = Offset(barLeft, chartHeight - expenseHeight),
                                size = Size(barWidth, expenseHeight),
                                cornerRadius = barCorner,
                            )
                        } else {
                            // Subtle baseline dot so user can see every slot
                            drawRoundRect(
                                color = gridColor.copy(alpha = 0.7f),
                                topLeft = Offset(barLeft, chartHeight - 2.dp.toPx()),
                                size = Size(barWidth, 2.dp.toPx()),
                                cornerRadius = CornerRadius(2f, 2f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // X-axis labels
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { index, point ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (point.label.isNotEmpty()) {
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else labelColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CategoryDonutChart(
    slices: List<CategorySlice>,
    totalExpense: Double,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    var played by remember(slices) { mutableStateOf(false) }
    LaunchedEffect(slices) { played = true }
    val sweepProgress by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "donut_anim",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(172.dp)) {
                val strokeWidth = 24.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                if (slices.isEmpty() || totalExpense <= 0.0) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                } else {
                    var startAngle = -90f
                    slices.forEach { slice ->
                        val rawSweep = (slice.ratio * 360.0).toFloat() * sweepProgress
                        val sweep = (rawSweep - if (slices.size > 1) 2.5f else 0f).coerceAtLeast(1.5f)
                        drawArc(
                            color = categoryColor(slice.category),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                        startAngle += rawSweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total gastado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = FinanceFormatters.currency(totalExpense, hideBalances),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Category breakdown rows
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slices.take(7).forEach { slice ->
                val color = categoryColor(slice.category)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = slice.category.labelEs,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${FinanceFormatters.currency(slice.amount, hideBalances)} (${FinanceFormatters.percent(slice.ratio)})",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(slice.ratio.toFloat().coerceIn(0.02f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
