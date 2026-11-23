package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.CategoryEntity
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.InteractiveCashflowBarChart
import com.example.ui.components.PillBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.categoryColorByName
import com.example.ui.components.categoryIconByName
import com.example.ui.sheets.AnalyticsKpiKind
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetProgress
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.FinanceFormatters

enum class AnalyticsTabMode(val labelEs: String) {
    ANALYTICS("Análisis"),
    CATEGORIES("Categorías y Presup."),
}

private enum class CategoryTabFilter(val labelEs: String) {
    ALL("Todas"),
    EXPENSE("Gastos"),
    INCOME("Ingresos"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    state: FinanceUiState,
    tabMode: AnalyticsTabMode,
    onTabModeChange: (AnalyticsTabMode) -> Unit,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onSelectGranularity: (AnalyticsGranularity) -> Unit,
    onKpiClick: (AnalyticsKpiKind) -> Unit,
    onCategoryDetailClick: (String) -> Unit,
    onOpenAddCategory: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onOpenAddBudget: (CategoryEntity?) -> Unit,
    onRemoveBudget: (CategoryEntity) -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val analytics = state.analytics
    var compareIncome by remember { mutableStateOf(true) }
    var categoryTab by remember { mutableStateOf(CategoryTabFilter.ALL) }
    var categoryQuery by remember { mutableStateOf("") }
    var categoryPendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val displayedCategories = remember(state.categories, categoryTab, categoryQuery) {
        val q = categoryQuery.trim().lowercase()
        state.categories.filter { cat ->
            val matchesTab = when (categoryTab) {
                CategoryTabFilter.ALL -> true
                CategoryTabFilter.EXPENSE -> !cat.isIncome
                CategoryTabFilter.INCOME -> cat.isIncome
            }
            val matchesQuery = q.isEmpty() || cat.name.lowercase().contains(q)
            matchesTab && matchesQuery
        }
    }

    if (categoryPendingDelete != null) {
        val cat = categoryPendingDelete!!
        AlertDialog(
            onDismissRequest = { categoryPendingDelete = null },
            title = { Text("Eliminar categoría") },
            text = {
                Text("Se eliminará la categoría \"${cat.name}\". Tus movimientos anteriores conservarán su historial.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(cat)
                    categoryPendingDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingDelete = null }) { Text("Cancelar") }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Prominent Top Segmented Switcher: Análisis | Categorías y Presup.
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AnalyticsTabMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = tabMode == mode,
                    onClick = { onTabModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(idx, AnalyticsTabMode.entries.size),
                    icon = {
                        Icon(
                            imageVector = if (mode == AnalyticsTabMode.ANALYTICS) Icons.Rounded.Analytics
                            else Icons.Rounded.Category,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.testTag("analytics_tab_${mode.name.lowercase()}"),
                ) {
                    Text(mode.labelEs, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (tabMode == AnalyticsTabMode.ANALYTICS) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
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
                                onClick = onOpenBackup,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_backup_button"),
                            ) {
                                Icon(
                                    Icons.Rounded.ImportExport,
                                    contentDescription = stringResource(R.string.cd_backup_data),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Respaldo", style = MaterialTheme.typography.labelLarge)
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

                // Personal Finance Health KPIs (tappable for breakdown!)
                item {
                    SectionHeader(title = "Salud financiera (toca para detalle)")
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
                                onClick = { onKpiClick(AnalyticsKpiKind.SAVINGS_RATE) },
                                modifier = Modifier.weight(1f),
                            )
                            HealthKpiCard(
                                title = "Gasto diario prom.",
                                value = FinanceFormatters.currency(analytics.averageDailyExpense, state.hideBalances),
                                subtitle = "En este periodo",
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                onClick = { onKpiClick(AnalyticsKpiKind.DAILY_EXPENSE) },
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
                                onClick = { onKpiClick(AnalyticsKpiKind.MONTH_PROJECTION) },
                                modifier = Modifier.weight(1f),
                            )
                            HealthKpiCard(
                                title = "Compromiso fijo/mes",
                                value = FinanceFormatters.currency(analytics.fixedMonthlyCommitment, state.hideBalances),
                                subtitle = "Suscrip. + Fijos + MSI + Prést.",
                                valueColor = financeColors.transfer,
                                onClick = { onKpiClick(AnalyticsKpiKind.FIXED_COMMITMENT) },
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
                                onClick = { onKpiClick(AnalyticsKpiKind.CREDIT_UTILIZATION) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // Category Distribution Donut Chart (each row clickable for category detail!)
                item {
                    SectionHeader(
                        title = "Gastos por categoría",
                        actionLabel = "Gestionar",
                        onActionClick = { onTabModeChange(AnalyticsTabMode.CATEGORIES) },
                    )
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
                                        text = "Mayor gasto: ${top.categoryName} (${FinanceFormatters.percent(top.ratio)})",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }
                                CategoryDonutChart(
                                    slices = analytics.categoryBreakdown,
                                    totalExpense = analytics.totalExpense,
                                    hideBalances = state.hideBalances,
                                    onSliceClick = { slice -> onCategoryDetailClick(slice.categoryName) },
                                )
                            }
                        }
                    }
                }

                // Quick Preview of Monthly Budgets
                if (analytics.budgetProgress.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Presupuestos activos (${analytics.budgetProgress.size})",
                            actionLabel = "Ver todos",
                            onActionClick = { onTabModeChange(AnalyticsTabMode.CATEGORIES) },
                        )
                    }
                    items(analytics.budgetProgress.take(3), key = { "bprev_${it.category.id}" }) { item ->
                        BudgetProgressCard(
                            progress = item,
                            hideBalances = state.hideBalances,
                            onClick = { onCategoryDetailClick(item.category.name) },
                            onEdit = { onOpenAddBudget(item.category) },
                            onRemoveBudget = { onRemoveBudget(item.category) },
                        )
                    }
                }
            }
        } else {
            // Dedicated Categories & Budgets Management Tab
            val totalBudgeted = analytics.budgetProgress.sumOf { it.category.monthlyBudget }
            val totalBudgetSpent = analytics.budgetProgress.sumOf { it.spentThisMonth }
            val globalRatio = if (totalBudgeted > 0.0) totalBudgetSpent / totalBudgeted else 0.0

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Global Budget Overview Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Presupuesto mensual asignado",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                            Text(
                                text = FinanceFormatters.currency(totalBudgeted, state.hideBalances),
                                style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            if (totalBudgeted > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Gastado este mes: ${FinanceFormatters.currency(totalBudgetSpent, state.hideBalances)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = FinanceFormatters.percent(globalRatio),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { globalRatio.toFloat().coerceIn(0f, 1f) },
                                    color = when {
                                        globalRatio >= 1.0 -> financeColors.expense
                                        globalRatio >= 0.80 -> financeColors.warning
                                        else -> financeColors.income
                                    },
                                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                            } else {
                                Text(
                                    text = "Asigna límites mensuales a tus categorías de gasto para controlar tu consumo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = onOpenAddCategory,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("analytics_add_category_button"),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Nueva categoría", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                                FilledTonalButton(
                                    onClick = { onOpenAddBudget(null) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("analytics_add_budget_button"),
                                ) {
                                    Icon(Icons.Rounded.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Asignar presup.", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Active Monthly Budgets
                item {
                    SectionHeader(
                        title = "Presupuestos activos (${analytics.budgetProgress.size})",
                        actionLabel = "+ Asignar",
                        onActionClick = { onOpenAddBudget(null) },
                    )
                }
                if (analytics.budgetProgress.isEmpty()) {
                    item {
                        EmptyFinanceCard(
                            icon = Icons.Rounded.Savings,
                            title = "Aún no tienes presupuestos asignados",
                            subtitle = "Toca el botón \"Presupuesto\" en cualquier categoría de gasto o usa \"Asignar presup.\" arriba.",
                            actionLabel = "Asignar presupuesto",
                            onActionClick = { onOpenAddBudget(null) },
                        )
                    }
                } else {
                    items(analytics.budgetProgress, key = { "bprog_${it.category.id}" }) { item ->
                        BudgetProgressCard(
                            progress = item,
                            hideBalances = state.hideBalances,
                            onClick = { onCategoryDetailClick(item.category.name) },
                            onEdit = { onOpenAddBudget(item.category) },
                            onRemoveBudget = { onRemoveBudget(item.category) },
                        )
                    }
                }

                // Full Categories Management
                item {
                    SectionHeader(
                        title = "Administrar categorías (${state.categories.size})",
                        actionLabel = "+ Nueva",
                        onActionClick = onOpenAddCategory,
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = categoryQuery,
                            onValueChange = { categoryQuery = it },
                            placeholder = { Text("Buscar categoría...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (categoryQuery.isNotEmpty()) {
                                    IconButton(onClick = { categoryQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryTabFilter.entries.forEach { tab ->
                                FilterChip(
                                    selected = categoryTab == tab,
                                    onClick = { categoryTab = tab },
                                    label = { Text(tab.labelEs) },
                                )
                            }
                        }
                    }
                }

                items(displayedCategories, key = { "cat_${it.id}" }) { category ->
                    val catColor = categoryColorByName(category.name, category.colorIndex)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                if (!category.isIncome) onCategoryDetailClick(category.name)
                                else onEditCategory(category)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = categoryIconByName(category.name, category.iconKey),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!category.isIncome) {
                                    Text(
                                        text = if (category.monthlyBudget > 0.0) {
                                            "Presupuesto: ${FinanceFormatters.currency(category.monthlyBudget, state.hideBalances)}/mes"
                                        } else {
                                            "Sin presupuesto asignado"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (category.monthlyBudget > 0.0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Text(
                                        text = "Categoría de ingreso",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = financeColors.income,
                                    )
                                }
                            }
                            if (!category.isIncome) {
                                TextButton(
                                    onClick = { onOpenAddBudget(category) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = if (category.monthlyBudget > 0.0) "Límite" else "+ Presup.",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onEditCategory(category) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Editar categoría",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = { categoryPendingDelete = category },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Eliminar categoría",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
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
private fun HealthKpiCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Ver detalle de $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
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
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemoveBudget: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current
    val cat = progress.category
    val barColor = when {
        progress.ratio >= 1.0 -> financeColors.expense
        progress.ratio >= 0.80 -> financeColors.warning
        else -> financeColors.income
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${FinanceFormatters.currency(progress.spentThisMonth, hideBalances)} de ${FinanceFormatters.currency(cat.monthlyBudget, hideBalances)}",
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
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Editar presupuesto",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onRemoveBudget, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Quitar presupuesto",
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
