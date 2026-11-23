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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.PillBadge
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.DateMath
import com.example.util.FinanceFormatters

private enum class ScheduledFilter(val labelEs: String) {
    ALL("Todos"),
    INCOMES("Ingresos frecuentes"),
    SUBSCRIPTIONS("Suscripciones"),
    BILLS("Pagos frecuentes"),
}

@Composable
fun ScheduledScreen(
    state: FinanceUiState,
    onAddScheduled: (RecurringKind) -> Unit,
    onScheduledClick: (ScheduledPaymentEntity) -> Unit,
    onEditScheduled: (ScheduledPaymentEntity) -> Unit,
    onToggleScheduled: (ScheduledPaymentEntity) -> Unit,
    onDeleteScheduled: (ScheduledPaymentEntity) -> Unit,
    onPayNow: (ScheduledPaymentEntity) -> Unit,
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    var filter by remember { mutableStateOf(ScheduledFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<ScheduledPaymentEntity?>(null) }

    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }

    val filtered = remember(state.scheduledPayments, filter) {
        when (filter) {
            ScheduledFilter.ALL -> state.scheduledPayments
            ScheduledFilter.INCOMES -> state.scheduledPayments.filter { it.recurringKind == RecurringKind.RECURRING_INCOME }
            ScheduledFilter.SUBSCRIPTIONS -> state.scheduledPayments.filter { it.recurringKind == RecurringKind.SUBSCRIPTION }
            ScheduledFilter.BILLS -> state.scheduledPayments.filter { it.recurringKind == RecurringKind.RECURRING_BILL }
        }
    }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar ${target.name}") },
            text = { Text("Se eliminará este registro programado. Los movimientos ya registrados se conservarán.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteScheduled(target)
                    pendingDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }

    val totalMonthlyOut = state.monthlySubscriptionsTotal + state.monthlyRecurringBillsTotal
    val totalMonthlyIn = state.monthlyRecurringIncomeTotal
    val recurringNet = totalMonthlyIn - totalMonthlyOut

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Monthly Recurring Summary Card (Ingresos frecuentes vs Pagos programados)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Balance recurrente mensual",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                            Text(
                                text = FinanceFormatters.currency(recurringNet, state.hideBalances, showSign = true),
                                style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = if (recurringNet >= 0) financeColors.income else financeColors.expense,
                            )
                        }
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("scheduled_notifications_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Configurar recordatorios de pago",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ingresos frec. / mes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            )
                            Text(
                                text = "+${FinanceFormatters.currency(totalMonthlyIn, state.hideBalances)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = financeColors.income,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Suscrip. + Pagos / mes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            )
                            Text(
                                text = "-${FinanceFormatters.currency(totalMonthlyOut, state.hideBalances)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = financeColors.expense,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { onAddScheduled(RecurringKind.RECURRING_INCOME) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scheduled_add_income_button"),
                        ) {
                            Icon(Icons.Rounded.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ingreso frecuente", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { onAddScheduled(RecurringKind.SUBSCRIPTION) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scheduled_add_expense_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pago / Suscripción", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ScheduledFilter.entries) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.labelEs) },
                        modifier = Modifier.testTag("scheduled_filter_${item.name.lowercase()}"),
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                val isIncomeFilter = filter == ScheduledFilter.INCOMES
                EmptyFinanceCard(
                    icon = if (isIncomeFilter) Icons.Rounded.TrendingUp else Icons.Rounded.EventRepeat,
                    title = if (isIncomeFilter) "Sin ingresos frecuentes" else "Sin movimientos programados",
                    subtitle = if (isIncomeFilter) {
                        "Registra tus ingresos recurrentes como salario, quincena, alquileres o honorarios para proyectar tu flujo mensual."
                    } else {
                        "Asocia tus suscripciones, pagos frecuentes e ingresos recurrentes a tus cuentas o tarjetas."
                    },
                    actionLabel = if (isIncomeFilter) "Programar ingreso frecuente" else "Programar suscripción o pago",
                    actionTestTag = "scheduled_empty_add_button",
                    onActionClick = {
                        onAddScheduled(
                            when (filter) {
                                ScheduledFilter.INCOMES -> RecurringKind.RECURRING_INCOME
                                ScheduledFilter.BILLS -> RecurringKind.RECURRING_BILL
                                else -> RecurringKind.SUBSCRIPTION
                            }
                        )
                    },
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val account = accountMap[item.accountId]
                val daysLeft = DateMath.daysBetween(System.currentTimeMillis(), item.nextDueDateMillis)
                val isIncome = item.recurringKind.isIncome
                val isSub = item.recurringKind == RecurringKind.SUBSCRIPTION

                val iconBg = when {
                    isIncome -> financeColors.incomeContainer
                    isSub -> financeColors.transferContainer
                    else -> financeColors.warningContainer
                }
                val iconTint = when {
                    isIncome -> financeColors.income
                    isSub -> financeColors.transfer
                    else -> financeColors.warning
                }
                val iconVector = when (item.recurringKind) {
                    RecurringKind.RECURRING_INCOME -> Icons.Rounded.TrendingUp
                    RecurringKind.SUBSCRIPTION -> Icons.Rounded.Subscriptions
                    RecurringKind.RECURRING_BILL -> Icons.Rounded.Bolt
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (item.isActive) 1f else 0.62f)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onScheduledClick(item) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(iconBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = iconTint,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${item.recurringKind.labelEs} · ${item.recurrenceFrequency.labelEs} · ${account?.name ?: "Sin cuenta"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Switch(
                                checked = item.isActive,
                                onCheckedChange = { onToggleScheduled(item) },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = (if (isIncome) "+" else "") + FinanceFormatters.currency(item.amount, state.hideBalances),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) financeColors.income else MaterialTheme.colorScheme.onSurface,
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Próximo: ${FinanceFormatters.fullDate(item.nextDueDateMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (item.isActive) {
                                        PillBadge(
                                            text = when {
                                                daysLeft < 0 -> "Vencido"
                                                daysLeft == 0 -> "Hoy"
                                                daysLeft == 1 -> "Mañana"
                                                else -> "En ${daysLeft}d"
                                            },
                                            containerColor = when {
                                                isIncome -> financeColors.incomeContainer
                                                daysLeft <= 3 -> financeColors.expenseContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            contentColor = when {
                                                isIncome -> financeColors.income
                                                daysLeft <= 3 -> financeColors.expense
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { onPayNow(item) },
                                enabled = item.isActive,
                                shape = RoundedCornerShape(12.dp),
                                colors = if (isIncome) {
                                    ButtonDefaults.buttonColors(containerColor = financeColors.income)
                                } else {
                                    ButtonDefaults.buttonColors()
                                },
                                modifier = Modifier.testTag("pay_scheduled_${item.id}"),
                            ) {
                                Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isIncome) "Registrar ingreso hoy" else "Registrar pago hoy")
                            }
                            Row {
                                IconButton(onClick = { onEditScheduled(item) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Editar registro programado")
                                }
                                IconButton(onClick = { pendingDelete = item }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar registro programado")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
