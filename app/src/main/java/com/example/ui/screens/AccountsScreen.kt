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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.InstallmentPlanEntity
import com.example.ui.components.AccountPickerField
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.PillBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.accountTypeIcon
import com.example.ui.sheets.MovementSheetMode
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.CreditCardSummary
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.DateMath
import com.example.util.FinanceFormatters

private enum class AccountsFilter(val labelEs: String) {
    ALL("Todas"),
    LIQUID("Ahorro y Débito"),
    CREDIT("Crédito"),
    LOANS("Préstamos"),
}

@Composable
fun AccountsScreen(
    state: FinanceUiState,
    onAccountClick: (AccountEntity) -> Unit,
    onAddAccount: (AccountType) -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onOpenMovementSheet: (MovementSheetMode, Long?, Long?) -> Unit,
    onPayInstallmentMonth: (planId: Long, fromAccountId: Long) -> Unit,
    onDeleteInstallmentPlan: (InstallmentPlanEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(AccountsFilter.ALL) }
    var accountPendingDelete by remember { mutableStateOf<AccountEntity?>(null) }
    var installmentPendingPay by remember { mutableStateOf<InstallmentPlanEntity?>(null) }
    var installmentPendingDelete by remember { mutableStateOf<InstallmentPlanEntity?>(null) }

    val debitAccounts = state.accounts.filter { it.accountType == AccountType.DEBIT }
    val savingsAndCashAccounts = state.accounts.filter {
        it.accountType == AccountType.SAVINGS || it.accountType == AccountType.CASH
    }
    val liquidAccounts = state.accounts.filter { it.accountType.isLiquidAsset }
    val bankLoans = state.accounts.filter { it.accountType == AccountType.BANK_LOAN }
    val lentLoans = state.accounts.filter { it.accountType == AccountType.LENT_LOAN }

    if (accountPendingDelete != null) {
        val acc = accountPendingDelete!!
        AlertDialog(
            onDismissRequest = { accountPendingDelete = null },
            title = { Text("Eliminar ${acc.name}") },
            text = {
                Text("Se eliminará esta cuenta junto con sus pagos programados y movimientos asociados. ¿Deseas continuar?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAccount(acc)
                    accountPendingDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountPendingDelete = null }) { Text("Cancelar") }
            },
        )
    }

    if (installmentPendingPay != null) {
        val plan = installmentPendingPay!!
        val payers = state.accounts.filter { it.accountType.isLiquidAsset }
        var chosenPayerId by remember(payers) { mutableStateOf(payers.firstOrNull()?.id) }
        AlertDialog(
            onDismissRequest = { installmentPendingPay = null },
            title = { Text("Pagar mensualidad MSI") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${plan.description} · Cuota ${plan.paidMonths + 1}/${plan.totalMonths} por ${FinanceFormatters.currency(plan.monthlyAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (payers.isEmpty()) {
                        Text(
                            text = "Agrega primero una cuenta de ahorro, débito o efectivo para realizar el pago.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        AccountPickerField(
                            label = "Cuenta de pago",
                            accounts = payers,
                            selectedAccount = payers.firstOrNull { it.id == chosenPayerId },
                            hideBalances = state.hideBalances,
                            onAccountSelected = { acc -> if (acc != null) chosenPayerId = acc.id },
                            onAddAccount = { type ->
                                installmentPendingPay = null
                                onAddAccount(type)
                            },
                            testTag = "msi_pay_account_combobox",
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = chosenPayerId != null,
                    onClick = {
                        chosenPayerId?.let { onPayInstallmentMonth(plan.id, it) }
                        installmentPendingPay = null
                    },
                ) { Text("Confirmar pago") }
            },
            dismissButton = {
                TextButton(onClick = { installmentPendingPay = null }) { Text("Cancelar") }
            },
        )
    }

    if (installmentPendingDelete != null) {
        val plan = installmentPendingDelete!!
        AlertDialog(
            onDismissRequest = { installmentPendingDelete = null },
            title = { Text("Eliminar compra MSI") },
            text = {
                Text(
                    "Se eliminará \"${plan.description}\" junto con sus movimientos MSI asociados y se revertirá el saldo en las cuentas afectadas.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteInstallmentPlan(plan)
                    installmentPendingDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { installmentPendingDelete = null }) { Text("Cancelar") }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccountsFilter.entries) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.labelEs) },
                        modifier = Modifier.testTag("accounts_filter_${item.name.lowercase()}"),
                    )
                }
            }
        }

        if (state.accounts.isEmpty()) {
            item {
                EmptyFinanceCard(
                    icon = Icons.Rounded.AccountBalance,
                    title = "Sin cuentas registradas",
                    subtitle = "Organiza tus cuentas de ahorro, débito, tarjetas de crédito (con corte, pago y MSI), préstamos bancarios y dinero prestado.",
                    actionLabel = "Agregar cuenta o tarjeta",
                    actionTestTag = "accounts_empty_add_button",
                    onActionClick = { onAddAccount(AccountType.DEBIT) },
                )
            }
            return@LazyColumn
        }

        // 1. Debit & Savings/Cash Accounts (separated with exact subtotals like Neko)
        if (filter == AccountsFilter.ALL || filter == AccountsFilter.LIQUID) {
            val debitSubtotal = debitAccounts.sumOf { it.balance }
            item {
                SectionHeader(
                    title = "Débito · ${FinanceFormatters.currency(debitSubtotal, state.hideBalances)}",
                    actionLabel = "+ Débito",
                    onActionClick = { onAddAccount(AccountType.DEBIT) },
                )
            }
            if (debitAccounts.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.Rounded.AccountBalance,
                        title = "Sin cuentas de débito",
                        subtitle = "Agrega tus cuentas de débito o inversión para llevar el control exacto.",
                        actionLabel = "Crear cuenta",
                        onActionClick = { onAddAccount(AccountType.DEBIT) },
                    )
                }
            } else {
                items(debitAccounts, key = { "deb_${it.id}" }) { acc ->
                    LiquidAccountCard(
                        account = acc,
                        hideBalances = state.hideBalances,
                        onCardClick = { onAccountClick(acc) },
                        onTransferClick = { onOpenMovementSheet(MovementSheetMode.TRANSFER, acc.id, null) },
                        onEditClick = { onEditAccount(acc) },
                        onDeleteClick = { accountPendingDelete = acc },
                    )
                }
            }

            val savingsSubtotal = savingsAndCashAccounts.sumOf { it.balance }
            item {
                SectionHeader(
                    title = "Ahorros y Efectivo · ${FinanceFormatters.currency(savingsSubtotal, state.hideBalances)}",
                    actionLabel = "+ Ahorro",
                    onActionClick = { onAddAccount(AccountType.SAVINGS) },
                )
            }
            if (savingsAndCashAccounts.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.Rounded.AccountBalance,
                        title = "Sin cuentas de ahorro o efectivo",
                        subtitle = "Agrega tus cuentas bancarias de ahorro o efectivo.",
                        actionLabel = "Crear ahorro",
                        onActionClick = { onAddAccount(AccountType.SAVINGS) },
                    )
                }
            } else {
                items(savingsAndCashAccounts, key = { "sav_${it.id}" }) { acc ->
                    LiquidAccountCard(
                        account = acc,
                        hideBalances = state.hideBalances,
                        onCardClick = { onAccountClick(acc) },
                        onTransferClick = { onOpenMovementSheet(MovementSheetMode.TRANSFER, acc.id, null) },
                        onEditClick = { onEditAccount(acc) },
                        onDeleteClick = { accountPendingDelete = acc },
                    )
                }
            }
        }

        // 2. Credit Cards with Cutoff, Payment Date, Total Owed & MSI Plans
        if (filter == AccountsFilter.ALL || filter == AccountsFilter.CREDIT) {
            item {
                SectionHeader(
                    title = "Tarjetas de Crédito · ${FinanceFormatters.currency(state.creditDebtTotal, state.hideBalances)}",
                    actionLabel = "+ Tarjeta",
                    onActionClick = { onAddAccount(AccountType.CREDIT) },
                )
            }
            if (state.creditCardSummaries.isEmpty()) {
                item {
                    EmptyFinanceCard(
                        icon = Icons.Rounded.CreditCard,
                        title = "Sin tarjetas de crédito",
                        subtitle = "Controla fecha de corte, fecha límite de pago, saldo total a pagar y compras a meses sin intereses.",
                        actionLabel = "Agregar tarjeta",
                        onActionClick = { onAddAccount(AccountType.CREDIT) },
                    )
                }
            } else {
                items(state.creditCardSummaries, key = { "credit_${it.card.id}" }) { summary ->
                    CreditCardDetailCard(
                        summary = summary,
                        hideBalances = state.hideBalances,
                        onCardClick = { onAccountClick(summary.card) },
                        onPayCard = {
                            val payer = liquidAccounts.firstOrNull()?.id
                            onOpenMovementSheet(MovementSheetMode.TRANSFER, payer, summary.card.id)
                        },
                        onAddMsi = {
                            onOpenMovementSheet(MovementSheetMode.MSI, summary.card.id, null)
                        },
                        onPayInstallment = { plan -> installmentPendingPay = plan },
                        onDeleteInstallment = { plan -> installmentPendingDelete = plan },
                        onEdit = { onEditAccount(summary.card) },
                        onDelete = { accountPendingDelete = summary.card },
                    )
                }
            }
        }

        // 3. Bank Loans & Lent Loans
        if (filter == AccountsFilter.ALL || filter == AccountsFilter.LOANS) {
            item {
                SectionHeader(
                    title = "Préstamos · ${FinanceFormatters.currency(state.bankLoansTotal, state.hideBalances)}",
                    actionLabel = "+ Préstamo",
                    onActionClick = { onAddAccount(AccountType.BANK_LOAN) },
                )
            }
            if (bankLoans.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Sin préstamos bancarios activos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { onAddAccount(AccountType.BANK_LOAN) }) {
                                Text("Agregar")
                            }
                        }
                    }
                }
            } else {
                val accountMap = state.accounts.associateBy { it.id }
                items(bankLoans, key = { "bl_${it.id}" }) { loan ->
                    val linkedName = loan.linkedAccountId?.let { accountMap[it]?.name }
                    LoanCard(
                        account = loan,
                        isLent = false,
                        linkedAccountName = linkedName,
                        hideBalances = state.hideBalances,
                        onCardClick = { onAccountClick(loan) },
                        onActionClick = {
                            val payer = loan.linkedAccountId
                                ?.takeIf { id -> state.accounts.any { it.id == id } }
                                ?: liquidAccounts.firstOrNull()?.id
                            onOpenMovementSheet(MovementSheetMode.TRANSFER, payer, loan.id)
                        },
                        onEdit = { onEditAccount(loan) },
                        onDelete = { accountPendingDelete = loan },
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Prestado (Por cobrar) · ${FinanceFormatters.currency(state.lentLoansTotal, state.hideBalances)}",
                    actionLabel = "+ Registrar",
                    onActionClick = { onAddAccount(AccountType.LENT_LOAN) },
                )
            }
            if (lentLoans.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "No tienes préstamos realizados por cobrar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { onAddAccount(AccountType.LENT_LOAN) }) {
                                Text("Registrar")
                            }
                        }
                    }
                }
            } else {
                val accountMap = state.accounts.associateBy { it.id }
                items(lentLoans, key = { "ll_${it.id}" }) { loan ->
                    val linkedName = loan.linkedAccountId?.let { accountMap[it]?.name }
                    LoanCard(
                        account = loan,
                        isLent = true,
                        linkedAccountName = linkedName,
                        hideBalances = state.hideBalances,
                        onCardClick = { onAccountClick(loan) },
                        onActionClick = {
                            val dest = loan.linkedAccountId
                                ?.takeIf { id -> state.accounts.any { it.id == id } }
                                ?: liquidAccounts.firstOrNull()?.id
                            onOpenMovementSheet(MovementSheetMode.TRANSFER, loan.id, dest)
                        },
                        onEdit = { onEditAccount(loan) },
                        onDelete = { accountPendingDelete = loan },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidAccountCard(
    account: AccountEntity,
    hideBalances: Boolean,
    onCardClick: () -> Unit,
    onTransferClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = accountTypeIcon(account.accountType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(account.accountType.labelEs)
                            if (account.institution.isNotBlank()) append(" · ${account.institution}")
                            if (account.lastFourDigits.isNotBlank()) append(" · •••• ${account.lastFourDigits}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar cuenta")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar cuenta")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Saldo disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.currency(account.balance, hideBalances),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = if (account.balance >= 0) financeColors.income else financeColors.expense,
                    )
                }
                OutlinedButton(
                    onClick = onTransferClick,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Transferir")
                }
            }

            if (account.accountType == AccountType.SAVINGS && account.savingsGoal > 0.0) {
                val progress = (account.balance / account.savingsGoal).toFloat().coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Meta de ahorro: ${FinanceFormatters.currency(account.savingsGoal, hideBalances)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = FinanceFormatters.percent(progress.toDouble()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditCardDetailCard(
    summary: CreditCardSummary,
    hideBalances: Boolean,
    onCardClick: () -> Unit,
    onPayCard: () -> Unit,
    onAddMsi: () -> Unit,
    onPayInstallment: (InstallmentPlanEntity) -> Unit,
    onDeleteInstallment: (InstallmentPlanEntity) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current
    val card = summary.card
    val utilizationColor = when {
        summary.utilizationRatio >= 0.70 -> financeColors.expense
        summary.utilizationRatio >= 0.35 -> financeColors.warning
        else -> financeColors.goldAccent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            // Stylized Fintech Credit Card Top Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(financeColors.heroStart, financeColors.heroEnd)
                        )
                    )
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = buildString {
                                    if (card.institution.isNotBlank()) append(card.institution)
                                    if (card.lastFourDigits.isNotBlank()) {
                                        if (isNotEmpty()) append(" · ")
                                        append("•••• ${card.lastFourDigits}")
                                    }
                                }.ifEmpty { "Tarjeta de Crédito" },
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = Color.White.copy(alpha = 0.78f),
                            )
                        }
                        Row {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Editar tarjeta", tint = Color.White)
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar tarjeta", tint = Color.White)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Saldo total a pagar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.76f),
                            )
                            Text(
                                text = FinanceFormatters.currency(card.balance, hideBalances),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Crédito disponible",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.76f),
                            )
                            Text(
                                text = FinanceFormatters.currency(summary.availableCredit, hideBalances),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.SemiBold,
                                color = financeColors.goldAccent,
                            )
                        }
                    }

                    // Utilization bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Límite: ${FinanceFormatters.currency(card.creditLimit, hideBalances)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.76f),
                            )
                            Text(
                                text = "Uso: ${FinanceFormatters.percent(summary.utilizationRatio)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = utilizationColor,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { summary.utilizationRatio.toFloat().coerceIn(0f, 1f) },
                            color = utilizationColor,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }

            // Dates & Actions Body
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Fecha de corte (día ${card.cutoffDay})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = FinanceFormatters.shortDate(summary.nextCutoff.dateMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "En ${summary.nextCutoff.daysRemaining} días",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (summary.nextPaymentDue.daysRemaining <= 5) financeColors.warningContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Fecha de pago (día ${card.paymentDueDay})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = FinanceFormatters.shortDate(summary.nextPaymentDue.dateMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "En ${summary.nextPaymentDue.daysRemaining} días",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (summary.nextPaymentDue.daysRemaining <= 5) financeColors.warning
                                else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Revolvente vs MSI breakdown
                if (summary.activeInstallments.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Saldo revolvente: ${FinanceFormatters.currency(summary.revolvingBalance, hideBalances)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "MSI del mes: ${FinanceFormatters.currency(summary.monthlyMsiAmount, hideBalances)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = financeColors.warning,
                        )
                    }
                }

                // Card Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onPayCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pagar tarjeta")
                    }
                    OutlinedButton(
                        onClick = onAddMsi,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compra MSI")
                    }
                }

                // Active MSI Installments List
                if (summary.activeInstallments.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "Compras a Meses Sin Intereses (${summary.activeInstallments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    summary.activeInstallments.forEach { plan ->
                        val progress = if (plan.totalMonths > 0) plan.paidMonths.toFloat() / plan.totalMonths else 0f
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plan.description,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "Cuota: ${FinanceFormatters.currency(plan.monthlyAmount, hideBalances)}/mes · Restan ${plan.remainingMonths} de ${plan.totalMonths}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Button(
                                            onClick = { onPayInstallment(plan) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                            ),
                                            modifier = Modifier.height(34.dp),
                                        ) {
                                            Text("Pagar mes", style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(
                                            onClick = { onDeleteInstallment(plan) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("msi_delete_${plan.id}"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Eliminar compra MSI",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
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
private fun LoanCard(
    account: AccountEntity,
    isLent: Boolean,
    linkedAccountName: String?,
    hideBalances: Boolean,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current
    val nextDate = DateMath.nextOccurrenceOfDayOfMonth(account.paymentDueDay)
    val principal = account.originalPrincipal.coerceAtLeast(account.balance)
    val paidRatio = if (principal > 0.0) ((principal - account.balance) / principal).toFloat().coerceIn(0f, 1f) else 0f

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isLent) financeColors.warningContainer else financeColors.expenseContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = accountTypeIcon(account.accountType),
                        contentDescription = null,
                        tint = if (isLent) financeColors.warning else financeColors.expense,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isLent) {
                            account.counterpartyName.ifBlank { "Préstamo personal realizado" }
                        } else {
                            buildString {
                                append(account.institution.ifBlank { "Préstamo bancario" })
                                if (account.interestRate > 0.0) append(" · ${account.interestRate}% anual")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!linkedAccountName.isNullOrBlank()) {
                        Text(
                            text = if (isLent) "Desembolsado desde: $linkedAccountName"
                            else "Desembolso en: $linkedAccountName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Editar préstamo") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar préstamo") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (isLent) "Pendiente por cobrar" else "Saldo pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.currency(account.balance, hideBalances),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = if (isLent) financeColors.warning else financeColors.expense,
                    )
                }
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isLent) "Cobrar" else "Abonar")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (isLent) "Recuperado: ${FinanceFormatters.percent(paidRatio.toDouble())}"
                        else "Liquidado: ${FinanceFormatters.percent(paidRatio.toDouble())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = buildString {
                            if (!isLent && account.monthlyInstallment > 0.0) {
                                append("Cuota ${FinanceFormatters.currency(account.monthlyInstallment, hideBalances)} · ")
                            }
                            append("Próximo: ${FinanceFormatters.shortDate(nextDate.dateMillis)}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { paidRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}
