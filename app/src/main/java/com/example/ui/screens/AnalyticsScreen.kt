package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnalyticsGranularity
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.FinanceCategory
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.InteractiveCashflowBarChart
import com.example.ui.components.PillBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetProgress
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.FinanceFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    state: FinanceUiState,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onSelectGranularity: (AnalyticsGranularity) -> Unit,
    onOpenAddBudget: () -> Unit,
    onDeleteBudget: (FinanceCategory) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val analytics = state.analytics
    var compareIncome by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Period Selector (Semana / Mes / Año)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Análisis financiero",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = analytics.periodSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_csv_button"),
                    ) {
                        Icon(
                            Icons.Rounded.FileDownload,
                            contentDescription = stringResource(R.string.cd_export_report),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("CSV", style = MaterialTheme.typography.labelLarge)
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AnalyticsPeriod.entries.forEachIndexed { idx, period ->
                        SegmentedButton(
                            selected = analytics.period == period,
                            onClick = { onSelectPeriod(period) },
                            shape = SegmentedButtonDefaults.itemShape(idx, AnalyticsPeriod.entries.size),
                            modifier = Modifier.testTag("analytics_period_${period.name.lowercase()}"),
                        ) {
                            Text(period.labelEs)
                        }
                    }
                }

                // Granularity Chips (Por día / Por semana / Por mes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        analytics.availableGranularities.forEach { gran ->
                            FilterChip(
                                selected = analytics.granularity == gran,
                                onClick = { onSelectGranularity(gran) },
                                label = { Text(gran.labelEs) },
                                modifier = Modifier.testTag("analytics_gran_${gran.name.lowercase()}"),
                            )
                        }
                    }
                    if (analytics.granularity != AnalyticsGranularity.DAY || analytics.period == AnalyticsPeriod.WEEK) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = compareIncome,
                                onCheckedChange = { compareIncome = it },
                            )
                        }
                    }
                }
            }
        }

        // Main Interactive Bar Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Gastos (${analytics.period.labelEs.lowercase()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = FinanceFormatters.currency(analytics.totalExpense, state.hideBalances),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = financeColors.expense,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Ingresos (${analytics.period.labelEs.lowercase()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = FinanceFormatters.currency(analytics.totalIncome, state.hideBalances),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = financeColors.income,
                            )
                        }
                    }

                    InteractiveCashflowBarChart(
                        points = analytics.barPoints,
                        showIncomeComparison = compareIncome,
                        hideBalances = state.hideBalances,
                    )
                }
            }
        }

        // Personal Finance Health KPIs (2x2 Grid)
        item {
            SectionHeader(title = "Salud financiera e indicadores")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HealthKpiCard(
                        title = "Tasa de ahorro",
                        value = if (analytics.totalIncome > 0.0) FinanceFormatters.percent(analytics.savingsRate) else "—",
                        subtitle = when {
                            analytics.totalIncome <= 0.0 -> "Registra ingresos"
                            analytics.savingsRate >= 0.20 -> "Excelente (≥20%)"
                            analytics.savingsRate >= 0.0 -> "Puedes mejorar"
                            else -> "Gastas más de lo que entra"
                        },
                        valueColor = when {
                            analytics.totalIncome <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            analytics.savingsRate >= 0.20 -> financeColors.income
                            analytics.savingsRate >= 0.0 -> financeColors.warning
                            else -> financeColors.expense
                        },
                        modifier = Modifier.weight(1f),
                    )
                    HealthKpiCard(
                        title = "Gasto diario prom.",
                        value = FinanceFormatters.currency(analytics.averageDailyExpense, state.hideBalances),
                        subtitle = "En este periodo",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HealthKpiCard(
                        title = "Proyección del mes",
                        value = FinanceFormatters.currency(analytics.projectedMonthExpense, state.hideBalances),
                        subtitle = "Al ritmo actual de gasto",
                        valueColor = financeColors.warning,
                        modifier = Modifier.weight(1f),
                    )
                    HealthKpiCard(
                        title = "Compromiso fijo/mes",
                        value = FinanceFormatters.currency(analytics.fixedMonthlyCommitment, state.hideBalances),
                        subtitle = "Suscrip. + Fijos + MSI + Prést.",
                        valueColor = financeColors.transfer,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.creditLimitTotal > 0.0) {
                    val ratio = analytics.creditUtilizationRatio
                    HealthKpiCard(
                        title = "Utilización total de líneas de crédito",
                        value = FinanceFormatters.percent(ratio),
                        subtitle = when {
                            ratio <= 0.30 -> "Nivel saludable (recomendado mantener bajo 30%)"
                            ratio <= 0.60 -> "Uso moderado · vigila tus cortes"
                            else -> "Uso alto · prioriza reducir saldo en tarjetas"
                        },
                        valueColor = when {
                            ratio <= 0.30 -> financeColors.income
                            ratio <= 0.60 -> financeColors.warning
                            else -> financeColors.expense
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Category Distribution Donut Chart
        item {
            SectionHeader(title = "Gastos por categoría")
        }
        item {
            if (analytics.categoryBreakdown.isEmpty()) {
                EmptyFinanceCard(
                    icon = Icons.Rounded.PieChart,
                    title = "Sin gastos en este periodo",
                    subtitle = "Cuando registres gastos verás aquí la distribución por categoría y tu mayor rubro de gasto.",
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        analytics.topCategory?.let { top ->
                            PillBadge(
                                text = "Mayor gasto: ${top.category.labelEs} (${FinanceFormatters.percent(top.ratio)})",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        CategoryDonutChart(
                            slices = analytics.categoryBreakdown,
                            totalExpense = analytics.totalExpense,
                            hideBalances = state.hideBalances,
                        )
                    }
                }
            }
        }

        // Monthly Category Budgets (Value-Added Personal Finance Feature)
        item {
            SectionHeader(
                title = "Presupuestos mensuales",
                actionLabel = "+ Presupuesto",
                onActionClick = onOpenAddBudget,
            )
        }
        if (analytics.budgetProgress.isEmpty()) {
            item {
                EmptyFinanceCard(
                    icon = Icons.Rounded.Savings,
                    title = "Fija límites de gasto por categoría",
                    subtitle = "Crea presupuestos mensuales (ej. Alimentación, Entretenimiento) para controlar tus gastos antes de excederte.",
                    actionLabel = "Crear presupuesto",
                    actionTestTag = "analytics_add_budget_button",
                    onActionClick = onOpenAddBudget,
                )
            }
        } else {
            items(analytics.budgetProgress, key = { it.budget.category }) { item ->
                BudgetProgressCard(
                    progress = item,
                    hideBalances = state.hideBalances,
                    onDelete = { onDeleteBudget(item.budget.financeCategory) },
                )
            }
        }
    }
}

@Composable
private fun HealthKpiCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BudgetProgressCard(
    progress: BudgetProgress,
    hideBalances: Boolean,
    onDelete: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current
    val barColor = when {
        progress.ratio >= 1.0 -> financeColors.expense
        progress.ratio >= 0.80 -> financeColors.warning
        else -> financeColors.income
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.budget.financeCategory.labelEs,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${FinanceFormatters.currency(progress.spentThisMonth, hideBalances)} de ${FinanceFormatters.currency(progress.budget.monthlyLimit, hideBalances)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillBadge(
                        text = FinanceFormatters.percent(progress.ratio),
                        containerColor = barColor.copy(alpha = 0.14f),
                        contentColor = barColor,
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Eliminar presupuesto",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress.ratio.toFloat().coerceIn(0f, 1f) },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}
