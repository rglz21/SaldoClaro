package com.example.ui.sheets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.GestureSafeModalBottomSheet
import com.example.ui.components.categoryColorByName
import com.example.ui.components.categoryIconByName
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import java.util.Calendar
import kotlin.math.max

enum class AnalyticsKpiKind(val titleEs: String) {
    SAVINGS_RATE("Tasa de ahorro"),
    DAILY_EXPENSE("Gasto diario promedio"),
    MONTH_PROJECTION("Proyección del mes"),
    FIXED_COMMITMENT("Compromiso fijo mensual"),
    CREDIT_UTILIZATION("Utilización de líneas de crédito"),
}

private data class CategoryBarBucket(
    val label: String,
    val fullLabel: String,
    val amount: Double,
    val count: Int,
    val highlighted: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsKpiDetailSheet(
    kind: AnalyticsKpiKind,
    state: FinanceUiState,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current
    val analytics = state.analytics

    GestureSafeModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = kind.titleEs,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = analytics.periodSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (kind) {
                        AnalyticsKpiKind.SAVINGS_RATE -> {
                            DetailInfoRow(
                                "Ingresos del periodo",
                                FinanceFormatters.currency(analytics.totalIncome, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.income,
                            )
                            DetailInfoRow(
                                "Gastos del periodo",
                                FinanceFormatters.currency(analytics.totalExpense, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.expense,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailInfoRow(
                                "Flujo neto ahorrado",
                                FinanceFormatters.currency(analytics.netFlow, state.hideBalances, showSign = true),
                                mono = true,
                                valueColor = if (analytics.netFlow >= 0) financeColors.income else financeColors.expense,
                            )
                            DetailInfoRow(
                                "Tasa de ahorro",
                                if (analytics.totalIncome > 0.0) FinanceFormatters.percent(analytics.savingsRate) else "Sin ingresos",
                                mono = true,
                            )
                            Text(
                                text = "Fórmula: (Ingresos − Gastos) ÷ Ingresos. Una meta saludable de finanzas personales es mantener al menos 20%.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        AnalyticsKpiKind.DAILY_EXPENSE -> {
                            val now = System.currentTimeMillis()
                            val effectiveEnd = if (now in analytics.periodStartMillis until analytics.periodEndMillis) {
                                now
                            } else {
                                (analytics.periodEndMillis - 1L).coerceAtLeast(analytics.periodStartMillis)
                            }
                            val elapsedDays = max(1, DateMath.daysBetween(analytics.periodStartMillis, effectiveEnd) + 1)
                            DetailInfoRow(
                                "Total gastado (${analytics.period.labelEs.lowercase()})",
                                FinanceFormatters.currency(analytics.totalExpense, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.expense,
                            )
                            DetailInfoRow("Días considerados", "$elapsedDays días", mono = true)
                            DetailInfoRow(
                                "Promedio diario",
                                FinanceFormatters.currency(analytics.averageDailyExpense, state.hideBalances),
                                mono = true,
                            )
                            analytics.topCategory?.let { top ->
                                DetailInfoRow("Categoría principal", "${top.categoryName} (${FinanceFormatters.percent(top.ratio)})")
                            }
                        }

                        AnalyticsKpiKind.MONTH_PROJECTION -> {
                            val daysPassed = max(1, DateMath.currentDayOfMonth())
                            val daysInMonth = DateMath.daysInCurrentMonth()
                            val remainingDays = (daysInMonth - daysPassed).coerceAtLeast(0)
                            DetailInfoRow(
                                "Gastado en lo que va del mes",
                                FinanceFormatters.currency(state.monthExpense, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.expense,
                            )
                            DetailInfoRow("Avance del mes", "Día $daysPassed de $daysInMonth (restan $remainingDays d)")
                            DetailInfoRow(
                                "Ritmo diario del mes",
                                FinanceFormatters.currency(state.monthExpense / daysPassed.toDouble(), state.hideBalances),
                                mono = true,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailInfoRow(
                                "Proyección de cierre mensual",
                                FinanceFormatters.currency(analytics.projectedMonthExpense, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.warning,
                            )
                            if (state.monthIncome > 0.0) {
                                val projectedNet = state.monthIncome - analytics.projectedMonthExpense
                                DetailInfoRow(
                                    "Balance estimado vs ingresos",
                                    FinanceFormatters.currency(projectedNet, state.hideBalances, showSign = true),
                                    mono = true,
                                    valueColor = if (projectedNet >= 0) financeColors.income else financeColors.expense,
                                )
                            }
                        }

                        AnalyticsKpiKind.FIXED_COMMITMENT -> {
                            DetailInfoRow(
                                "Suscripciones / mes",
                                FinanceFormatters.currency(state.monthlySubscriptionsTotal, state.hideBalances),
                                mono = true,
                            )
                            DetailInfoRow(
                                "Pagos frecuentes / mes",
                                FinanceFormatters.currency(state.monthlyRecurringBillsTotal, state.hideBalances),
                                mono = true,
                            )
                            DetailInfoRow(
                                "Mensualidades MSI",
                                FinanceFormatters.currency(state.monthlyMsiTotal, state.hideBalances),
                                mono = true,
                            )
                            DetailInfoRow(
                                "Cuotas de préstamos bancarios",
                                FinanceFormatters.currency(state.monthlyLoanTotal, state.hideBalances),
                                mono = true,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailInfoRow(
                                "Total compromiso fijo mensual",
                                FinanceFormatters.currency(analytics.fixedMonthlyCommitment, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.transfer,
                            )
                            if (state.monthlyRecurringIncomeTotal > 0.0) {
                                DetailInfoRow(
                                    "Ingresos frecuentes / mes",
                                    "+${FinanceFormatters.currency(state.monthlyRecurringIncomeTotal, state.hideBalances)}",
                                    mono = true,
                                    valueColor = financeColors.income,
                                )
                            }
                        }

                        AnalyticsKpiKind.CREDIT_UTILIZATION -> {
                            DetailInfoRow(
                                "Deuda total en tarjetas",
                                FinanceFormatters.currency(state.creditDebtTotal, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.expense,
                            )
                            DetailInfoRow(
                                "Límite de crédito total",
                                FinanceFormatters.currency(state.creditLimitTotal, state.hideBalances),
                                mono = true,
                            )
                            DetailInfoRow(
                                "Utilización global",
                                FinanceFormatters.percent(analytics.creditUtilizationRatio),
                                mono = true,
                            )
                            if (state.creditCardSummaries.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = "Desglose por tarjeta",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                state.creditCardSummaries.forEach { summary ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = summary.card.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "${FinanceFormatters.currency(summary.card.balance, state.hideBalances)} / ${FinanceFormatters.currency(summary.card.creditLimit, state.hideBalances)} (${FinanceFormatters.percent(summary.utilizationRatio)})",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { summary.utilizationRatio.toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cerrar")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsCategoryDetailSheet(
    categoryName: String,
    state: FinanceUiState,
    onDismiss: () -> Unit,
    onEditCategoryBudget: (CategoryEntity) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current
    val analytics = state.analytics
    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val catEntity = remember(state.categories, categoryName) {
        state.categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
    }
    val catColor = categoryColorByName(categoryName, catEntity?.colorIndex)

    val allCategoryExpenses = remember(state.transactions, categoryName) {
        state.transactions.filter {
            it.transactionType == TransactionType.EXPENSE &&
                it.categoryLabel.equals(categoryName, ignoreCase = true)
        }
    }

    val periodTxs = remember(allCategoryExpenses, analytics.periodStartMillis, analytics.periodEndMillis) {
        allCategoryExpenses.filter {
            it.dateMillis in analytics.periodStartMillis until analytics.periodEndMillis
        }
    }
    val periodTotal = remember(periodTxs) { periodTxs.sumOf { it.amount } }
    val share = if (analytics.totalExpense > 0.0) periodTotal / analytics.totalExpense else 0.0

    val inspectedMonthStart = remember(analytics.selectedYear, analytics.selectedMonth, analytics.period) {
        if (analytics.period == AnalyticsPeriod.MONTH) {
            DateMath.startOfMonth(analytics.selectedYear, analytics.selectedMonth)
        } else {
            DateMath.startOfCurrentMonth()
        }
    }
    val inspectedMonthEnd = remember(analytics.selectedYear, analytics.selectedMonth, analytics.period) {
        if (analytics.period == AnalyticsPeriod.MONTH) {
            DateMath.endOfMonthExclusive(analytics.selectedYear, analytics.selectedMonth)
        } else {
            val now = Calendar.getInstance()
            DateMath.endOfMonthExclusive(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        }
    }
    val monthSpent = remember(allCategoryExpenses, inspectedMonthStart, inspectedMonthEnd) {
        allCategoryExpenses
            .filter { it.dateMillis in inspectedMonthStart until inspectedMonthEnd }
            .sumOf { it.amount }
    }

    // Chart 1: Breakdown within the selected period
    val periodBuckets = remember(periodTxs, analytics.period, analytics.periodStartMillis, analytics.periodEndMillis, analytics.selectedYear, analytics.selectedMonth) {
        buildCategoryPeriodBuckets(
            txs = periodTxs,
            period = analytics.period,
            periodStart = analytics.periodStartMillis,
            periodEnd = analytics.periodEndMillis,
            anchorYear = analytics.selectedYear,
            anchorMonth = analytics.selectedMonth,
        )
    }

    // Chart 2: 6-Month rolling trend ending at inspected month
    val sixMonthBuckets = remember(allCategoryExpenses, analytics.selectedYear, analytics.selectedMonth, analytics.period) {
        buildCategorySixMonthTrend(
            allCategoryExpenses = allCategoryExpenses,
            endYear = if (analytics.period == AnalyticsPeriod.MONTH) analytics.selectedYear else Calendar.getInstance().get(Calendar.YEAR),
            endMonth = if (analytics.period == AnalyticsPeriod.MONTH) analytics.selectedMonth else Calendar.getInstance().get(Calendar.MONTH),
        )
    }
    val sixMonthAverage = remember(sixMonthBuckets) {
        if (sixMonthBuckets.isEmpty()) 0.0 else sixMonthBuckets.sumOf { it.amount } / sixMonthBuckets.size.toDouble()
    }

    // Chart 3: Breakdown by Account / Card in this period
    val accountBreakdown = remember(periodTxs, accountMap, periodTotal) {
        if (periodTotal <= 0.0) emptyList()
        else periodTxs
            .groupBy { it.accountId }
            .map { (accId, list) ->
                val sum = list.sumOf { it.amount }
                Triple(accountMap[accId]?.name ?: "Cuenta", sum, sum / periodTotal)
            }
            .sortedByDescending { it.second }
    }

    GestureSafeModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIconByName(categoryName, catEntity?.iconKey),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${analytics.periodSubtitle} · ${periodTxs.size} movimientos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailInfoRow(
                        "Total en el periodo",
                        FinanceFormatters.currency(periodTotal, state.hideBalances),
                        mono = true,
                        valueColor = financeColors.expense,
                    )
                    DetailInfoRow("Participación del gasto", FinanceFormatters.percent(share), mono = true)
                    if (periodTxs.isNotEmpty()) {
                        val avgTx = periodTotal / periodTxs.size.toDouble()
                        DetailInfoRow(
                            "Ticket promedio",
                            FinanceFormatters.currency(avgTx, state.hideBalances),
                            mono = true,
                        )
                    }
                    if (catEntity != null && catEntity.monthlyBudget > 0.0) {
                        val budgetRatio = monthSpent / catEntity.monthlyBudget
                        DetailInfoRow(
                            "Presupuesto mensual",
                            "${FinanceFormatters.currency(monthSpent, state.hideBalances)} / ${FinanceFormatters.currency(catEntity.monthlyBudget, state.hideBalances)}",
                            mono = true,
                        )
                        LinearProgressIndicator(
                            progress = { budgetRatio.toFloat().coerceIn(0f, 1f) },
                            color = when {
                                budgetRatio >= 1.0 -> financeColors.expense
                                budgetRatio >= 0.80 -> financeColors.warning
                                else -> financeColors.income
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }

            // Chart 1: Spending evolution inside the selected period
            if (periodBuckets.isNotEmpty()) {
                CategoryInteractiveBarChartCard(
                    title = "Evolución en el periodo",
                    subtitle = "Toca una barra para ver el detalle exacto",
                    buckets = periodBuckets,
                    barColor = catColor,
                    highlightColor = MaterialTheme.colorScheme.primary,
                    hideBalances = state.hideBalances,
                    modifier = Modifier.testTag("category_detail_period_chart"),
                )
            }

            // Chart 2: 6-Month trend comparison
            if (sixMonthBuckets.any { it.amount > 0.0 }) {
                CategoryInteractiveBarChartCard(
                    title = "Tendencia últimos 6 meses",
                    subtitle = "Promedio mensual: ${FinanceFormatters.currency(sixMonthAverage, state.hideBalances)}",
                    buckets = sixMonthBuckets,
                    barColor = MaterialTheme.colorScheme.secondary,
                    highlightColor = MaterialTheme.colorScheme.primary,
                    hideBalances = state.hideBalances,
                    modifier = Modifier.testTag("category_detail_trend_chart"),
                )
            }

            // Chart 3: Distribution by Account / Card
            if (accountBreakdown.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_detail_account_chart"),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Distribución por cuenta",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        accountBreakdown.forEach { (accName, amount, ratio) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = accName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${FinanceFormatters.currency(amount, state.hideBalances)} (${FinanceFormatters.percent(ratio)})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio.toFloat().coerceIn(0.03f, 1f))
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(catColor),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (catEntity != null && !catEntity.isIncome) {
                Button(
                    onClick = { onEditCategoryBudget(catEntity) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (catEntity.monthlyBudget > 0.0) "Editar presupuesto mensual" else "Asignar presupuesto mensual")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Detalle de movimientos (${periodTxs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (periodTxs.isEmpty()) {
                Text(
                    text = "No hay gastos registrados en esta categoría para el periodo seleccionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    periodTxs.take(40).forEach { tx ->
                        val accName = accountMap[tx.accountId]?.name ?: "Cuenta"
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onTransactionClick(tx) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "$accName · ${FinanceFormatters.shortDate(tx.dateMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "-${FinanceFormatters.currency(tx.amount, state.hideBalances)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                                    fontWeight = FontWeight.Bold,
                                    color = financeColors.expense,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryInteractiveBarChartCard(
    title: String,
    subtitle: String,
    buckets: List<CategoryBarBucket>,
    barColor: Color,
    highlightColor: Color,
    hideBalances: Boolean,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val slotHighlight = highlightColor.copy(alpha = 0.10f)

    var selectedIndex by remember(buckets) {
        val highlighted = buckets.indexOfLast { it.highlighted }
        val nonZero = buckets.indexOfLast { it.amount > 0.0 }
        mutableIntStateOf(
            when {
                highlighted >= 0 -> highlighted
                nonZero >= 0 -> nonZero
                else -> buckets.lastIndex
            }
        )
    }

    var played by remember(buckets) { mutableStateOf(false) }
    LaunchedEffect(buckets) { played = true }
    val animProgress by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = tween(durationMillis = 480),
        label = "cat_bar_anim",
    )

    val maxVal = remember(buckets) {
        val raw = buckets.maxOfOrNull { it.amount } ?: 0.0
        if (raw <= 0.0) 100.0 else raw * 1.18
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            buckets.getOrNull(selectedIndex)?.let { sel ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${sel.fullLabel} (${sel.count})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = FinanceFormatters.currency(sel.amount, hideBalances),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                            fontWeight = FontWeight.Bold,
                            color = highlightColor,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .pointerInput(buckets) {
                        detectTapGestures { offset ->
                            if (buckets.isNotEmpty() && size.width > 0) {
                                val slotW = size.width.toFloat() / buckets.size.toFloat()
                                selectedIndex = (offset.x / slotW).toInt().coerceIn(0, buckets.lastIndex)
                            }
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (buckets.isEmpty()) return@Canvas
                    val chartH = size.height - 6.dp.toPx()
                    val slotW = size.width / buckets.size.toFloat()

                    for (i in 0..2) {
                        val y = chartH * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val corner = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    buckets.forEachIndexed { idx, bucket ->
                        val slotLeft = idx * slotW
                        val isSel = idx == selectedIndex
                        if (isSel) {
                            drawRoundRect(
                                color = slotHighlight,
                                topLeft = Offset(slotLeft + 1.dp.toPx(), 0f),
                                size = Size((slotW - 2.dp.toPx()).coerceAtLeast(2f), chartH),
                                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                            )
                        }
                        val barW = (slotW * 0.52f).coerceIn(6.dp.toPx(), 26.dp.toPx())
                        val barLeft = slotLeft + (slotW - barW) / 2f
                        val h = ((bucket.amount / maxVal).toFloat().coerceIn(0f, 1f)) * chartH * animProgress
                        if (h > 0.5f) {
                            val fill = when {
                                isSel -> highlightColor
                                bucket.highlighted -> barColor
                                else -> barColor.copy(alpha = 0.72f)
                            }
                            drawRoundRect(
                                color = fill,
                                topLeft = Offset(barLeft, chartH - h),
                                size = Size(barW, h),
                                cornerRadius = corner,
                            )
                        } else {
                            drawRoundRect(
                                color = gridColor.copy(alpha = 0.7f),
                                topLeft = Offset(barLeft, chartH - 2.dp.toPx()),
                                size = Size(barW, 2.dp.toPx()),
                                cornerRadius = CornerRadius(2f, 2f),
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                buckets.forEachIndexed { idx, bucket ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == selectedIndex) highlightColor else labelColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun buildCategoryPeriodBuckets(
    txs: List<TransactionEntity>,
    period: AnalyticsPeriod,
    periodStart: Long,
    periodEnd: Long,
    anchorYear: Int,
    anchorMonth: Int,
): List<CategoryBarBucket> {
    val cal = Calendar.getInstance()
    return when (period) {
        AnalyticsPeriod.WEEK -> {
            val short = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
            val full = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            val totals = DoubleArray(7)
            val counts = IntArray(7)
            txs.forEach { tx ->
                cal.timeInMillis = tx.dateMillis
                val idx = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    else -> 6
                }
                totals[idx] += tx.amount
                counts[idx]++
            }
            short.indices.map { i ->
                val d = periodStart + i * 86_400_000L
                CategoryBarBucket(
                    label = short[i],
                    fullLabel = "${full[i]} ${FinanceFormatters.shortDate(d)}",
                    amount = totals[i],
                    count = counts[i],
                )
            }
        }

        AnalyticsPeriod.MONTH -> {
            val daysInMonth = DateMath.daysInMonth(anchorYear, anchorMonth)
            val bucketCount = if (daysInMonth > 28) 5 else 4
            val totals = DoubleArray(bucketCount)
            val counts = IntArray(bucketCount)
            txs.forEach { tx ->
                cal.timeInMillis = tx.dateMillis
                val idx = ((cal.get(Calendar.DAY_OF_MONTH) - 1) / 7).coerceIn(0, bucketCount - 1)
                totals[idx] += tx.amount
                counts[idx]++
            }
            val ranges = listOf("Días 1–7", "Días 8–14", "Días 15–21", "Días 22–28", "Días 29–$daysInMonth")
            (0 until bucketCount).map { i ->
                CategoryBarBucket(
                    label = "S${i + 1}",
                    fullLabel = "Semana ${i + 1} (${ranges[i]})",
                    amount = totals[i],
                    count = counts[i],
                )
            }
        }

        AnalyticsPeriod.YEAR -> {
            val totals = DoubleArray(12)
            val counts = IntArray(12)
            txs.forEach { tx ->
                cal.timeInMillis = tx.dateMillis
                val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
                totals[m] += tx.amount
                counts[m]++
            }
            (0..11).map { m ->
                CategoryBarBucket(
                    label = FinanceFormatters.shortMonth(m),
                    fullLabel = FinanceFormatters.monthYear(anchorYear, m),
                    amount = totals[m],
                    count = counts[m],
                )
            }
        }

        AnalyticsPeriod.CUSTOM -> {
            val inclusiveEnd = (periodEnd - 1L).coerceAtLeast(periodStart)
            val totalDays = max(1, DateMath.daysBetween(periodStart, inclusiveEnd) + 1)
            if (totalDays <= 10) {
                val totals = DoubleArray(totalDays)
                val counts = IntArray(totalDays)
                txs.forEach { tx ->
                    val idx = DateMath.daysBetween(periodStart, tx.dateMillis).coerceIn(0, totalDays - 1)
                    totals[idx] += tx.amount
                    counts[idx]++
                }
                (0 until totalDays).map { i ->
                    val d = periodStart + i * 86_400_000L
                    CategoryBarBucket(
                        label = FinanceFormatters.shortDate(d).substringBefore(" "),
                        fullLabel = FinanceFormatters.fullDate(d),
                        amount = totals[i],
                        count = counts[i],
                    )
                }
            } else {
                val weekCount = ((totalDays + 6) / 7).coerceIn(1, 12)
                val totals = DoubleArray(weekCount)
                val counts = IntArray(weekCount)
                txs.forEach { tx ->
                    val offset = DateMath.daysBetween(periodStart, tx.dateMillis).coerceAtLeast(0)
                    val idx = (offset / 7).coerceIn(0, weekCount - 1)
                    totals[idx] += tx.amount
                    counts[idx]++
                }
                (0 until weekCount).map { i ->
                    val ws = periodStart + i * 7L * 86_400_000L
                    val we = minOf(inclusiveEnd, ws + 6L * 86_400_000L)
                    CategoryBarBucket(
                        label = "S${i + 1}",
                        fullLabel = "${FinanceFormatters.shortDate(ws)} – ${FinanceFormatters.shortDate(we)}",
                        amount = totals[i],
                        count = counts[i],
                    )
                }
            }
        }
    }
}

private fun buildCategorySixMonthTrend(
    allCategoryExpenses: List<TransactionEntity>,
    endYear: Int,
    endMonth: Int,
): List<CategoryBarBucket> {
    val months = mutableListOf<Pair<Int, Int>>()
    val cursor = Calendar.getInstance().apply {
        set(Calendar.YEAR, endYear)
        set(Calendar.MONTH, endMonth)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, -5)
    }
    repeat(6) {
        months.add(cursor.get(Calendar.YEAR) to cursor.get(Calendar.MONTH))
        cursor.add(Calendar.MONTH, 1)
    }
    val totals = DoubleArray(6)
    val counts = IntArray(6)
    val txCal = Calendar.getInstance()
    allCategoryExpenses.forEach { tx ->
        txCal.timeInMillis = tx.dateMillis
        val key = txCal.get(Calendar.YEAR) to txCal.get(Calendar.MONTH)
        val idx = months.indexOf(key)
        if (idx >= 0) {
            totals[idx] += tx.amount
            counts[idx]++
        }
    }
    return months.mapIndexed { idx, (yr, mo) ->
        CategoryBarBucket(
            label = FinanceFormatters.shortMonth(mo),
            fullLabel = FinanceFormatters.monthYear(yr, mo),
            amount = totals[idx],
            count = counts[idx],
            highlighted = idx == 5,
        )
    }
}
