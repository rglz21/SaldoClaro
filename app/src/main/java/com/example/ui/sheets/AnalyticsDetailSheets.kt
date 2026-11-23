package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.categoryColorByName
import com.example.ui.components.categoryIconByName
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import java.util.Locale
import kotlin.math.max

enum class AnalyticsKpiKind(val titleEs: String) {
    SAVINGS_RATE("Tasa de ahorro"),
    DAILY_EXPENSE("Gasto diario promedio"),
    MONTH_PROJECTION("Proyección del mes"),
    FIXED_COMMITMENT("Compromiso fijo mensual"),
    CREDIT_UTILIZATION("Utilización de líneas de crédito"),
}

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

    ModalBottomSheet(
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
                            val elapsedDays = max(1, DateMath.daysBetween(analytics.periodStartMillis, System.currentTimeMillis()) + 1)
                            DetailInfoRow(
                                "Total gastado (${analytics.period.labelEs.lowercase()})",
                                FinanceFormatters.currency(analytics.totalExpense, state.hideBalances),
                                mono = true,
                                valueColor = financeColors.expense,
                            )
                            DetailInfoRow("Días transcurridos", "$elapsedDays días", mono = true)
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
    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val catEntity = remember(state.categories, categoryName) {
        state.categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
    }
    val catColor = categoryColorByName(categoryName, catEntity?.colorIndex)

    val periodTxs = remember(state.transactions, state.analytics.periodStartMillis, categoryName) {
        state.transactions.filter {
            it.dateMillis >= state.analytics.periodStartMillis &&
                it.transactionType == TransactionType.EXPENSE &&
                it.categoryLabel.equals(categoryName, ignoreCase = true)
        }
    }
    val periodTotal = remember(periodTxs) { periodTxs.sumOf { it.amount } }
    val share = if (state.analytics.totalExpense > 0.0) periodTotal / state.analytics.totalExpense else 0.0

    val monthStart = remember { DateMath.startOfCurrentMonth() }
    val monthSpent = remember(state.transactions, monthStart, categoryName) {
        state.transactions
            .filter {
                it.dateMillis >= monthStart &&
                    it.transactionType == TransactionType.EXPENSE &&
                    it.categoryLabel.equals(categoryName, ignoreCase = true)
            }
            .sumOf { it.amount }
    }

    ModalBottomSheet(
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
                        text = "${state.analytics.periodSubtitle} · ${periodTxs.size} movimientos",
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
                    periodTxs.take(30).forEach { tx ->
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
