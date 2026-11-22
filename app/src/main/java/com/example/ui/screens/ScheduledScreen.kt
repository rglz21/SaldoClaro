package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
    SUBSCRIPTIONS("Suscripciones"),
    BILLS("Pagos frecuentes"),
}

@Composable
fun ScheduledScreen(
    state: FinanceUiState,
    onAddScheduled: (RecurringKind) -> Unit,
    onEditScheduled: (ScheduledPaymentEntity) -> Unit,
    onToggleScheduled: (ScheduledPaymentEntity) -> Unit,
    onDeleteScheduled: (ScheduledPaymentEntity) -> Unit,
    onPayNow: (ScheduledPaymentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    var filter by remember { mutableStateOf(ScheduledFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<ScheduledPaymentEntity?>(null) }

    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }

    val filtered = remember(state.scheduledPayments, filter) {
        when (filter) {
            ScheduledFilter.ALL -> state.scheduledPayments
            ScheduledFilter.SUBSCRIPTIONS -> state.scheduledPayments.filter { it.recurringKind == RecurringKind.SUBSCRIPTION }
            ScheduledFilter.BILLS -> state.scheduledPayments.filter { it.recurringKind == RecurringKind.RECURRING_BILL }
        }
    }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar ${target.name}") },
            text = { Text("Se eliminará este pago programado. Los movimientos ya registrados se conservarán.") },
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Monthly Recurring Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Compromiso mensual programado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = FinanceFormatters.currency(
                            state.monthlySubscriptionsTotal + state.monthlyRecurringBillsTotal,
                            state.hideBalances,
                        ),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Suscripciones / mes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                            Text(
                                text = FinanceFormatters.currency(state.monthlySubscriptionsTotal, state.hideBalances),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pagos frecuentes / mes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                            Text(
                                text = FinanceFormatters.currency(state.monthlyRecurringBillsTotal, state.hideBalances),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
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
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyFinanceCard(
                    icon = Icons.Rounded.EventRepeat,
                    title = "Sin pagos programados",
                    subtitle = "Asocia tus suscripciones (Netflix, Spotify, etc.) y pagos frecuentes (renta, luz, internet) a tus cuentas o tarjetas.",
                    actionLabel = "Programar suscripción o pago",
                    actionTestTag = "scheduled_empty_add_button",
                    onActionClick = {
                        onAddScheduled(
                            if (filter == ScheduledFilter.BILLS) RecurringKind.RECURRING_BILL
                            else RecurringKind.SUBSCRIPTION
                        )
                    },
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val account = accountMap[item.accountId]
                val daysLeft = DateMath.daysBetween(System.currentTimeMillis(), item.nextDueDateMillis)
                val isSub = item.recurringKind == RecurringKind.SUBSCRIPTION

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (item.isActive) 1f else 0.62f),
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
                                    .background(if (isSub) financeColors.transferContainer else financeColors.warningContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isSub) Icons.Rounded.Subscriptions else Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (isSub) financeColors.transfer else financeColors.warning,
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
                                    text = FinanceFormatters.currency(item.amount, state.hideBalances),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                                    fontWeight = FontWeight.Bold,
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
                                            containerColor = if (daysLeft <= 3) financeColors.expenseContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (daysLeft <= 3) financeColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                modifier = Modifier.testTag("pay_scheduled_${item.id}"),
                            ) {
                                Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Registrar pago hoy")
                            }
                            Row {
                                IconButton(onClick = { onEditScheduled(item) }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Editar pago programado")
                                }
                                IconButton(onClick = { pendingDelete = item }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar pago programado")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
