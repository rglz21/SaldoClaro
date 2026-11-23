package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AccountEntity
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.LocalStorageNoticeBanner
import com.example.ui.components.PillBadge
import com.example.ui.components.QuickActionTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.categoryColorByName
import com.example.ui.components.categoryIconByName
import com.example.ui.components.isLoanRepaymentCollection
import com.example.ui.sheets.MovementSheetMode
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.CreditCardSummary
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import java.util.Locale

@Composable
fun DashboardScreen(
    state: FinanceUiState,
    onTogglePrivacy: () -> Unit,
    onOpenMovementSheet: (MovementSheetMode, Long?, Long?) -> Unit,
    onOpenAddAccount: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToScheduled: () -> Unit,
    onPayScheduledNow: (ScheduledPaymentEntity) -> Unit,
    onAccountClick: (AccountEntity) -> Unit,
    onScheduledClick: (ScheduledPaymentEntity) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val accountMap = state.accounts.associateBy { it.id }
    val categoryMap = state.categories.associateBy { it.name.lowercase(Locale.ROOT) }
    val activeUpcoming = state.scheduledPayments.filter { it.isActive }.take(3)
    val recentTransactions = state.transactions.take(5)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Hero Net Worth Card with generated emerald-gold artwork
        item {
            HeroNetWorthCard(
                state = state,
                onTogglePrivacy = onTogglePrivacy,
            )
        }

        // Quick Actions Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionTile(
                    icon = Icons.Rounded.ArrowUpward,
                    label = stringResource(R.string.quick_expense),
                    accentColor = financeColors.expense,
                    containerColor = financeColors.expenseContainer,
                    testTag = "quick_action_expense",
                    onClick = { onOpenMovementSheet(MovementSheetMode.EXPENSE, null, null) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    icon = Icons.Rounded.ArrowDownward,
                    label = stringResource(R.string.quick_income),
                    accentColor = financeColors.income,
                    containerColor = financeColors.incomeContainer,
                    testTag = "quick_action_income",
                    onClick = { onOpenMovementSheet(MovementSheetMode.INCOME, null, null) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    icon = Icons.AutoMirrored.Rounded.CompareArrows,
                    label = stringResource(R.string.quick_transfer),
                    accentColor = financeColors.transfer,
                    containerColor = financeColors.transferContainer,
                    testTag = "quick_action_transfer",
                    onClick = { onOpenMovementSheet(MovementSheetMode.TRANSFER, null, null) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    icon = Icons.Rounded.CreditCard,
                    label = stringResource(R.string.quick_msi),
                    accentColor = financeColors.warning,
                    containerColor = financeColors.warningContainer,
                    testTag = "quick_action_msi",
                    onClick = { onOpenMovementSheet(MovementSheetMode.MSI, null, null) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Monthly Cashflow Strip (tappable to jump into Calendar view)
        item {
            MonthlyCashflowCard(
                monthIncome = state.monthIncome,
                monthExpense = state.monthExpense,
                hideBalances = state.hideBalances,
                onOpenCalendar = onNavigateToCalendar,
            )
        }

        // Empty setup onboarding if no accounts yet
        if (state.accounts.isEmpty()) {
            item {
                EmptyFinanceCard(
                    icon = Icons.Rounded.AccountBalance,
                    title = "Comienza registrando tus cuentas",
                    subtitle = "Agrega tus cuentas de ahorro, tarjetas de débito, crédito o préstamos para llevar el control completo de tu dinero.",
                    actionLabel = "Agregar mi primera cuenta",
                    actionTestTag = "dashboard_first_account_button",
                    onActionClick = onOpenAddAccount,
                )
            }
        }

        // Credit Cards Cycle Summary
        if (state.creditCardSummaries.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Tarjetas de Crédito",
                    actionLabel = "Administrar",
                    onActionClick = onNavigateToAccounts,
                )
            }
            items(state.creditCardSummaries, key = { "cc_${it.card.id}" }) { summary ->
                DashboardCreditCardRow(
                    summary = summary,
                    hideBalances = state.hideBalances,
                    onCardClick = { onAccountClick(summary.card) },
                    onPayClick = {
                        val defaultSource = state.accounts.firstOrNull { it.accountType.isLiquidAsset }?.id
                        onOpenMovementSheet(MovementSheetMode.TRANSFER, defaultSource, summary.card.id)
                    },
                )
            }
        }

        // Upcoming Scheduled Payments, Subscriptions & Recurring Incomes
        item {
            SectionHeader(
                title = "Próximos pagos e ingresos",
                actionLabel = "Ver todos",
                onActionClick = onNavigateToScheduled,
            )
        }
        if (activeUpcoming.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToScheduled),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventRepeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sin pagos programados",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Asocia suscripciones como Netflix o Spotify y pagos frecuentes a tus cuentas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.cd_add_scheduled),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        } else {
            items(activeUpcoming, key = { "sched_${it.id}" }) { item ->
                val accountName = accountMap[item.accountId]?.name ?: "Cuenta"
                val daysLeft = DateMath.daysBetween(System.currentTimeMillis(), item.nextDueDateMillis)
                val isInc = item.recurringKind.isIncome
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onScheduledClick(item) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                PillBadge(
                                    text = when {
                                        daysLeft < 0 -> "Vencido"
                                        daysLeft == 0 -> "Hoy"
                                        daysLeft == 1 -> "Mañana"
                                        else -> "En $daysLeft días"
                                    },
                                    containerColor = when {
                                        isInc -> financeColors.incomeContainer
                                        daysLeft <= 3 -> financeColors.warningContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = when {
                                        isInc -> financeColors.income
                                        daysLeft <= 3 -> financeColors.warning
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$accountName · ${FinanceFormatters.shortDate(item.nextDueDateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (isInc) "+" else "") + FinanceFormatters.currency(item.amount, state.hideBalances),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = if (isInc) financeColors.income else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { onPayScheduledNow(item) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = if (isInc) ButtonDefaults.buttonColors(containerColor = financeColors.income)
                                else ButtonDefaults.buttonColors(),
                                modifier = Modifier.height(32.dp),
                            ) {
                                Text(if (isInc) "Recibir" else "Pagar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions
        item {
            SectionHeader(
                title = "Movimientos recientes",
                actionLabel = "Ver historial",
                onActionClick = onNavigateToTransactions,
            )
        }
        if (recentTransactions.isEmpty()) {
            item {
                EmptyFinanceCard(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "Aún no hay movimientos",
                    subtitle = "Registra tus ingresos, gastos, transferencias o compras a meses sin intereses.",
                )
            }
        } else {
            items(recentTransactions, key = { "tx_${it.id}" }) { tx ->
                val srcName = accountMap[tx.accountId]?.name ?: "Cuenta"
                val dstName = tx.destinationAccountId?.let { accountMap[it]?.name }
                val isLoanCol = isLoanRepaymentCollection(tx, accountMap)
                val amountColor = when {
                    tx.transactionType == TransactionType.INCOME || isLoanCol -> financeColors.income
                    tx.transactionType == TransactionType.EXPENSE -> financeColors.expense
                    else -> financeColors.transfer
                }
                val prefix = when {
                    tx.transactionType == TransactionType.INCOME || isLoanCol -> "+"
                    tx.transactionType == TransactionType.EXPENSE -> "-"
                    else -> ""
                }
                val catMeta = categoryMap[tx.categoryLabel.lowercase(Locale.ROOT)]
                val catColor = categoryColorByName(tx.categoryLabel, catMeta?.colorIndex)
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
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = categoryIconByName(tx.categoryLabel, catMeta?.iconKey),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = when {
                                    isLoanCol && dstName != null -> "Cobro → $dstName (a favor)"
                                    dstName != null -> "$srcName → $dstName"
                                    else -> "$srcName · ${FinanceFormatters.shortDate(tx.dateMillis)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$prefix${FinanceFormatters.currency(tx.amount, state.hideBalances)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                        )
                    }
                }
            }
        }

        item {
            LocalStorageNoticeBanner()
        }
    }
}

@Composable
private fun HeroNetWorthCard(
    state: FinanceUiState,
    onTogglePrivacy: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = financeColors.heroStart),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_finance_banner_1795301634759),
                contentDescription = stringResource(R.string.cd_hero_background),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                financeColors.heroEnd.copy(alpha = 0.90f),
                                financeColors.heroStart.copy(alpha = 0.62f),
                                financeColors.heroEnd.copy(alpha = 0.45f),
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Patrimonio neto",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.testTag("toggle_privacy_button"),
                    ) {
                        Icon(
                            imageVector = if (state.hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = stringResource(R.string.cd_toggle_privacy),
                            tint = Color.White,
                        )
                    }
                }
                Text(
                    text = FinanceFormatters.currency(state.netWorth, state.hideBalances),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.testTag("net_worth_value"),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HeroMiniMetric(
                        label = "Disponible",
                        value = FinanceFormatters.currency(state.liquidTotal, state.hideBalances),
                        accent = Color(0xFF7CF0BE),
                        modifier = Modifier.weight(1f),
                    )
                    HeroMiniMetric(
                        label = "Deuda total",
                        value = FinanceFormatters.currency(
                            state.creditDebtTotal + state.bankLoansTotal,
                            state.hideBalances,
                        ),
                        accent = Color(0xFFFFB4AB),
                        modifier = Modifier.weight(1f),
                    )
                    HeroMiniMetric(
                        label = "Por cobrar",
                        value = FinanceFormatters.currency(state.lentLoansTotal, state.hideBalances),
                        accent = financeColors.goldAccent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMiniMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.24f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.76f),
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MonthlyCashflowCard(
    monthIncome: Double,
    monthExpense: Double,
    hideBalances: Boolean,
    onOpenCalendar: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current
    val balance = monthIncome - monthExpense
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenCalendar)
            .testTag("dashboard_open_calendar_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Flujo · ${FinanceFormatters.monthYear(System.currentTimeMillis())}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PillBadge(
                    text = FinanceFormatters.currency(balance, hideBalances, showSign = true),
                    containerColor = if (balance >= 0) financeColors.incomeContainer else financeColors.expenseContainer,
                    contentColor = if (balance >= 0) financeColors.income else financeColors.expense,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Entradas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.currency(monthIncome, hideBalances),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = financeColors.income,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Salidas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.currency(monthExpense, hideBalances),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = financeColors.expense,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Toca para ver el calendario diario de gastos e ingresos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DashboardCreditCardRow(
    summary: CreditCardSummary,
    hideBalances: Boolean,
    onCardClick: () -> Unit,
    onPayClick: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.card.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = buildString {
                            if (summary.card.institution.isNotBlank()) append("${summary.card.institution} · ")
                            append("Corte ${FinanceFormatters.shortDate(summary.nextCutoff.dateMillis)} · Pago ${FinanceFormatters.shortDate(summary.nextPaymentDue.dateMillis)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PillBadge(
                    text = "Pago en ${summary.nextPaymentDue.daysRemaining}d",
                    containerColor = if (summary.nextPaymentDue.daysRemaining <= 5) financeColors.warningContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (summary.nextPaymentDue.daysRemaining <= 5) financeColors.warning else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Saldo total a pagar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.currency(summary.card.balance, hideBalances),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = financeColors.expense,
                    )
                    if (summary.monthlyMsiAmount > 0.0) {
                        Text(
                            text = "Incluye ${FinanceFormatters.currency(summary.monthlyMsiAmount, hideBalances)}/mes en MSI",
                            style = MaterialTheme.typography.labelSmall,
                            color = financeColors.warning,
                        )
                    }
                }
                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pagar")
                }
            }
        }
    }
}
