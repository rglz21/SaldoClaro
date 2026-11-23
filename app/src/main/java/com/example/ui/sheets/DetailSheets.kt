package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.GestureSafeModalBottomSheet
import com.example.ui.components.PillBadge
import com.example.ui.components.accountTypeIcon
import com.example.ui.components.categoryColorByName
import com.example.ui.components.categoryIconByName
import com.example.ui.components.isLoanRepaymentCollection
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.CreditCardSummary
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailSheet(
    account: AccountEntity,
    creditCardSummary: CreditCardSummary?,
    allAccounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    hideBalances: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTransferOrPay: () -> Unit,
    onAddMovement: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current
    val accountMap = remember(allAccounts) { allAccounts.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.name.lowercase(Locale.ROOT) } }

    val accountTxs = remember(transactions, account.id) {
        transactions.filter { it.accountId == account.id || it.destinationAccountId == account.id }
    }

    val (totalIn, totalOut) = remember(accountTxs, account.id, account.accountType) {
        var inflows = 0.0
        var outflows = 0.0
        accountTxs.forEach { tx ->
            when (tx.transactionType) {
                TransactionType.INCOME -> if (tx.accountId == account.id) inflows += tx.amount
                TransactionType.EXPENSE -> if (tx.accountId == account.id) outflows += tx.amount
                TransactionType.TRANSFER -> {
                    if (tx.destinationAccountId == account.id) {
                        inflows += tx.amount
                    } else if (tx.accountId == account.id) {
                        outflows += tx.amount
                    }
                }
            }
        }
        Pair(inflows, outflows)
    }

    val linkedAccountName = account.linkedAccountId?.let { accountMap[it]?.name }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = accountTypeIcon(account.accountType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.headlineSmall,
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
            }

            // Balance Hero Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val balanceLabel = when (account.accountType) {
                        AccountType.CREDIT -> "Saldo total a pagar"
                        AccountType.BANK_LOAN -> "Saldo pendiente por pagar"
                        AccountType.LENT_LOAN -> "Pendiente por cobrar"
                        else -> "Saldo actual disponible"
                    }
                    val balanceColor = when (account.accountType) {
                        AccountType.CREDIT, AccountType.BANK_LOAN -> financeColors.expense
                        AccountType.LENT_LOAN -> financeColors.warning
                        else -> if (account.balance >= 0) financeColors.income else financeColors.expense
                    }
                    Text(
                        text = balanceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.currency(account.balance, hideBalances),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = balanceColor,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val inLabel = when (account.accountType) {
                            AccountType.CREDIT -> "Abonos / Pagos"
                            AccountType.BANK_LOAN -> "Abonos realizados"
                            AccountType.LENT_LOAN -> "Monto prestado"
                            else -> "Total entradas"
                        }
                        val outLabel = when (account.accountType) {
                            AccountType.CREDIT -> "Cargos / Compras"
                            AccountType.BANK_LOAN -> "Desembolsos"
                            AccountType.LENT_LOAN -> "Cobros recibidos"
                            else -> "Total salidas"
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = inLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = FinanceFormatters.currency(totalIn, hideBalances),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = if (account.accountType == AccountType.LENT_LOAN) financeColors.warning
                                else financeColors.income,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = outLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = FinanceFormatters.currency(totalOut, hideBalances),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = if (account.accountType == AccountType.LENT_LOAN) financeColors.income
                                else financeColors.expense,
                            )
                        }
                    }
                }
            }

            // Specific Metadata by Account Type
            when (account.accountType) {
                AccountType.CREDIT -> if (creditCardSummary != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailInfoRow("Límite de crédito", FinanceFormatters.currency(account.creditLimit, hideBalances), mono = true)
                            DetailInfoRow("Crédito disponible", FinanceFormatters.currency(creditCardSummary.availableCredit, hideBalances), mono = true, valueColor = financeColors.income)
                            DetailInfoRow("Utilización", FinanceFormatters.percent(creditCardSummary.utilizationRatio), mono = true)
                            DetailInfoRow("Próximo corte (día ${account.cutoffDay})", "${FinanceFormatters.shortDate(creditCardSummary.nextCutoff.dateMillis)} (en ${creditCardSummary.nextCutoff.daysRemaining}d)")
                            DetailInfoRow("Próximo pago (día ${account.paymentDueDay})", "${FinanceFormatters.shortDate(creditCardSummary.nextPaymentDue.dateMillis)} (en ${creditCardSummary.nextPaymentDue.daysRemaining}d)")
                            if (creditCardSummary.activeInstallments.isNotEmpty()) {
                                DetailInfoRow("Saldo revolvente", FinanceFormatters.currency(creditCardSummary.revolvingBalance, hideBalances), mono = true)
                                DetailInfoRow("MSI mensual (${creditCardSummary.activeInstallments.size})", FinanceFormatters.currency(creditCardSummary.monthlyMsiAmount, hideBalances), mono = true, valueColor = financeColors.warning)
                            }
                        }
                    }
                }

                AccountType.BANK_LOAN, AccountType.LENT_LOAN -> {
                    val isLent = account.accountType == AccountType.LENT_LOAN
                    val principal = account.originalPrincipal.coerceAtLeast(account.balance)
                    val progress = if (principal > 0.0) ((principal - account.balance) / principal).coerceIn(0.0, 1.0) else 0.0
                    val nextDate = DateMath.nextOccurrenceOfDayOfMonth(account.paymentDueDay)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isLent && account.counterpartyName.isNotBlank()) {
                                DetailInfoRow("Deudor / Persona", account.counterpartyName)
                            }
                            if (!linkedAccountName.isNullOrBlank()) {
                                DetailInfoRow(
                                    if (isLent) "Cuenta de desembolso / cobro" else "Cuenta asociada",
                                    linkedAccountName,
                                )
                            }
                            if (principal > 0.0) {
                                DetailInfoRow("Monto original", FinanceFormatters.currency(principal, hideBalances), mono = true)
                                DetailInfoRow(
                                    if (isLent) "Recuperado" else "Liquidado",
                                    "${FinanceFormatters.currency((principal - account.balance).coerceAtLeast(0.0), hideBalances)} (${FinanceFormatters.percent(progress)})",
                                    mono = true,
                                    valueColor = financeColors.income,
                                )
                                LinearProgressIndicator(
                                    progress = { progress.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                )
                            }
                            if (!isLent && account.monthlyInstallment > 0.0) {
                                DetailInfoRow("Cuota mensual", FinanceFormatters.currency(account.monthlyInstallment, hideBalances), mono = true)
                            }
                            if (account.interestRate > 0.0) {
                                DetailInfoRow("Tasa anual", "${account.interestRate}%", mono = true)
                            }
                            DetailInfoRow("Próxima fecha (día ${account.paymentDueDay})", FinanceFormatters.shortDate(nextDate.dateMillis))
                        }
                    }
                }

                AccountType.SAVINGS -> if (account.savingsGoal > 0.0) {
                    val ratio = (account.balance / account.savingsGoal).coerceIn(0.0, 1.0)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailInfoRow("Meta de ahorro", FinanceFormatters.currency(account.savingsGoal, hideBalances), mono = true)
                            DetailInfoRow("Avance", FinanceFormatters.percent(ratio), mono = true, valueColor = MaterialTheme.colorScheme.primary)
                            LinearProgressIndicator(
                                progress = { ratio.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                }

                else -> Unit
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onTransferOrPay,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("account_detail_primary_action"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when (account.accountType) {
                            AccountType.CREDIT -> "Pagar"
                            AccountType.BANK_LOAN -> "Abonar"
                            AccountType.LENT_LOAN -> "Cobrar"
                            else -> "Transferir"
                        }
                    )
                }
                if (account.accountType.isLiquidAsset || account.accountType == AccountType.CREDIT) {
                    FilledTonalButton(
                        onClick = onAddMovement,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Movimiento")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar cuenta", modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Movimientos de esta cuenta (${accountTxs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (accountTxs.isEmpty()) {
                Text(
                    text = "Aún no hay movimientos registrados en esta cuenta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accountTxs.take(30).forEach { tx ->
                        val catMeta = categoryMap[tx.categoryLabel.lowercase(Locale.ROOT)]
                        val catColor = categoryColorByName(tx.categoryLabel, catMeta?.colorIndex)
                        val isRepayment = isLoanRepaymentCollection(tx, accountMap)
                        val isPositiveForThisAccount = when (tx.transactionType) {
                            TransactionType.INCOME -> true
                            TransactionType.EXPENSE -> false
                            TransactionType.TRANSFER -> when {
                                isRepayment && account.accountType == AccountType.LENT_LOAN -> true
                                tx.destinationAccountId == account.id -> true
                                else -> false
                            }
                        }
                        val amountColor = when {
                            tx.transactionType == TransactionType.INCOME -> financeColors.income
                            tx.transactionType == TransactionType.EXPENSE -> financeColors.expense
                            isRepayment -> financeColors.income
                            isPositiveForThisAccount -> financeColors.income
                            else -> financeColors.transfer
                        }
                        val prefix = when {
                            tx.transactionType == TransactionType.INCOME -> "+"
                            tx.transactionType == TransactionType.EXPENSE -> "-"
                            isRepayment && tx.destinationAccountId == account.id -> "+"
                            isRepayment && account.accountType == AccountType.LENT_LOAN -> "Abono "
                            tx.destinationAccountId == account.id -> "+"
                            else -> "-"
                        }
                        val otherName = if (tx.accountId == account.id) {
                            tx.destinationAccountId?.let { accountMap[it]?.name }
                        } else {
                            accountMap[tx.accountId]?.name
                        }

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
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = categoryIconByName(tx.categoryLabel, catMeta?.iconKey),
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = buildString {
                                            append(FinanceFormatters.shortDate(tx.dateMillis))
                                            append(" · ")
                                            if (otherName != null) {
                                                if (tx.destinationAccountId == account.id) append("Desde $otherName")
                                                else append("Hacia $otherName")
                                            } else {
                                                append(tx.categoryLabel)
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "$prefix${FinanceFormatters.currency(tx.amount, hideBalances)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor,
                                )
                            }
                        }
                    }
                    if (accountTxs.size > 30) {
                        Text(
                            text = "Mostrando los 30 movimientos más recientes de ${accountTxs.size}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    installmentPlans: List<InstallmentPlanEntity>,
    hideBalances: Boolean,
    onDismiss: () -> Unit,
    onDelete: (TransactionEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.name.lowercase(Locale.ROOT) } }

    val srcAccount = accountMap[transaction.accountId]
    val dstAccount = transaction.destinationAccountId?.let { accountMap[it] }
    val catMeta = categoryMap[transaction.categoryLabel.lowercase(Locale.ROOT)]
    val catColor = categoryColorByName(transaction.categoryLabel, catMeta?.colorIndex)
    val isLoanRepayment = isLoanRepaymentCollection(transaction, accountMap)
    val linkedPlan = transaction.installmentPlanId?.let { pid -> installmentPlans.firstOrNull { it.id == pid } }

    val amountColor = when {
        transaction.transactionType == TransactionType.INCOME || isLoanRepayment -> financeColors.income
        transaction.transactionType == TransactionType.EXPENSE -> financeColors.expense
        else -> financeColors.transfer
    }
    val prefix = when {
        transaction.transactionType == TransactionType.INCOME || isLoanRepayment -> "+"
        transaction.transactionType == TransactionType.EXPENSE -> "-"
        else -> ""
    }
    val typeBadgeLabel = when {
        isLoanRepayment -> "Cobro de préstamo (a favor)"
        transaction.transactionType == TransactionType.INCOME -> "Ingreso"
        transaction.transactionType == TransactionType.EXPENSE -> "Gasto"
        else -> "Transferencia"
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIconByName(transaction.categoryLabel, catMeta?.iconKey),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    PillBadge(
                        text = typeBadgeLabel,
                        containerColor = amountColor.copy(alpha = 0.14f),
                        contentColor = amountColor,
                    )
                }
            }

            Surface(
                color = amountColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$prefix${FinanceFormatters.currency(transaction.amount, hideBalances)}",
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                    )
                    if (isLoanRepayment && dstAccount != null && srcAccount != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ingresó a favor en ${dstAccount.name} y redujo el saldo por cobrar de ${srcAccount.name}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailInfoRow("Fecha", FinanceFormatters.fullDate(transaction.dateMillis))
                    DetailInfoRow("Categoría", transaction.categoryLabel)
                    if (transaction.transactionType == TransactionType.TRANSFER && dstAccount != null) {
                        DetailInfoRow(
                            if (isLoanRepayment) "Préstamo cobrado" else "Cuenta origen",
                            srcAccount?.name ?: "Cuenta",
                        )
                        DetailInfoRow(
                            if (isLoanRepayment) "Cuenta receptora (a favor)" else "Cuenta destino",
                            dstAccount.name,
                            valueColor = if (isLoanRepayment) financeColors.income else MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        DetailInfoRow("Cuenta", srcAccount?.name ?: "Cuenta")
                    }
                    if (linkedPlan != null) {
                        DetailInfoRow(
                            "Plan MSI",
                            "${linkedPlan.description} (${linkedPlan.paidMonths}/${linkedPlan.totalMonths} meses)",
                            valueColor = financeColors.warning,
                        )
                    }
                    if (transaction.note.isNotBlank()) {
                        DetailInfoRow("Nota", transaction.note)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cerrar")
                }
                Button(
                    onClick = { onDelete(transaction) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_detail_delete_button"),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledDetailSheet(
    item: ScheduledPaymentEntity,
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    hideBalances: Boolean,
    onDismiss: () -> Unit,
    onExecuteNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current
    val account = accounts.firstOrNull { it.id == item.accountId }
    val isIncome = item.recurringKind.isIncome
    val isSub = item.recurringKind == RecurringKind.SUBSCRIPTION
    val isOneTime = item.recurringKind.isOneTime || item.recurrenceFrequency.isOneTime
    val daysLeft = DateMath.daysBetween(System.currentTimeMillis(), item.nextDueDateMillis)
    val monthlyEquivalent = item.amount * item.recurrenceFrequency.monthlyFactor
    val yearlyEquivalent = monthlyEquivalent * 12.0

    val history = remember(transactions, item.id) {
        transactions.filter { it.scheduledPaymentId == item.id }
    }

    val accentColor = when {
        isIncome -> financeColors.income
        isOneTime -> MaterialTheme.colorScheme.primary
        isSub -> financeColors.transfer
        else -> financeColors.warning
    }
    val containerColor = when {
        isIncome -> financeColors.incomeContainer
        isOneTime -> MaterialTheme.colorScheme.primaryContainer
        isSub -> financeColors.transferContainer
        else -> financeColors.warningContainer
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (item.recurringKind) {
                            RecurringKind.ONE_TIME_PAYMENT -> Icons.Rounded.EventNote
                            RecurringKind.RECURRING_INCOME -> Icons.Rounded.TrendingUp
                            RecurringKind.SUBSCRIPTION -> Icons.Rounded.Subscriptions
                            RecurringKind.RECURRING_BILL -> Icons.Rounded.Bolt
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${item.recurringKind.labelEs} · ${when {
                            item.isActive -> "Pendiente / Activo"
                            isOneTime -> "Completado"
                            else -> "Pausado"
                        }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailInfoRow(
                        label = when {
                            isIncome -> "Monto por ingreso"
                            isOneTime -> "Monto a pagar"
                            else -> "Monto por cobro"
                        },
                        value = (if (isIncome) "+" else "") + FinanceFormatters.currency(item.amount, hideBalances),
                        mono = true,
                        valueColor = if (isIncome) financeColors.income else MaterialTheme.colorScheme.onSurface,
                    )
                    DetailInfoRow("Frecuencia", item.recurrenceFrequency.labelEs)
                    if (!isOneTime) {
                        DetailInfoRow("Equivalente mensual", FinanceFormatters.currency(monthlyEquivalent, hideBalances), mono = true)
                        DetailInfoRow("Proyección anual", FinanceFormatters.currency(yearlyEquivalent, hideBalances), mono = true)
                    }
                    DetailInfoRow(
                        if (isIncome) "Cuenta receptora" else "Cuenta / tarjeta",
                        account?.name ?: "Sin cuenta",
                    )
                    DetailInfoRow("Categoría", item.categoryLabel)
                    DetailInfoRow(
                        label = when {
                            isOneTime -> "Fecha de pago"
                            isIncome -> "Próximo ingreso"
                            else -> "Próximo cobro"
                        },
                        value = "${FinanceFormatters.fullDate(item.nextDueDateMillis)} (${when {
                            !item.isActive && isOneTime -> "Pagado"
                            daysLeft < 0 -> "Vencido"
                            daysLeft == 0 -> "Hoy"
                            daysLeft == 1 -> "Mañana"
                            else -> "en ${daysLeft}d"
                        }})",
                    )
                    if (item.notes.isNotBlank()) {
                        DetailInfoRow("Notas", item.notes)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onExecuteNow,
                    enabled = item.isActive,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f),
                ) {
                    Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isIncome) "Registrar ingreso" else "Registrar pago")
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (history.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Historial de registros (${history.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    history.take(10).forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = FinanceFormatters.fullDate(tx.dateMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = (if (isIncome) "+" else "-") + FinanceFormatters.currency(tx.amount, hideBalances),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) financeColors.income else financeColors.expense,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun DetailInfoRow(
    label: String,
    value: String,
    mono: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = if (mono) MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono)
            else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}
