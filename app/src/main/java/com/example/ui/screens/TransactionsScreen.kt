package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CalendarCashflowView
import com.example.ui.components.EmptyFinanceCard
import com.example.ui.components.PillBadge
import com.example.ui.components.categoryColorByName
import com.example.ui.components.categoryIconByName
import com.example.ui.components.isLoanRepaymentCollection
import com.example.ui.sheets.MovementSheetMode
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.FinanceFormatters
import java.util.Locale

enum class TransactionsViewMode(val labelEs: String) {
    CALENDAR("Calendario"),
    LIST("Lista"),
}

private enum class TxTypeFilter(val labelEs: String) {
    ALL("Todos"),
    EXPENSE("Gastos"),
    INCOME("Ingresos"),
    TRANSFER("Transferencias"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: FinanceUiState,
    viewMode: TransactionsViewMode,
    onViewModeChange: (TransactionsViewMode) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onOpenAddMovement: (MovementSheetMode, Long?) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onScheduleForDate: (Long) -> Unit = {},
    onScheduledClick: (ScheduledPaymentEntity) -> Unit = {},
    onExecuteScheduledNow: (ScheduledPaymentEntity) -> Unit = {},
    onAccountClick: (AccountEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isImeVisible) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TxTypeFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val accountMap = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val categoryMap = remember(state.categories) {
        state.categories.associateBy { it.name.lowercase(Locale.ROOT) }
    }

    val filtered = remember(state.transactions, searchQuery, typeFilter) {
        val q = searchQuery.trim().lowercase()
        state.transactions.filter { tx ->
            val matchesType = when (typeFilter) {
                TxTypeFilter.ALL -> true
                TxTypeFilter.EXPENSE -> tx.transactionType == TransactionType.EXPENSE
                TxTypeFilter.INCOME -> tx.transactionType == TransactionType.INCOME
                TxTypeFilter.TRANSFER -> tx.transactionType == TransactionType.TRANSFER
            }
            val matchesSearch = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.note.lowercase().contains(q) ||
                tx.categoryLabel.lowercase().contains(q) ||
                (accountMap[tx.accountId]?.name?.lowercase()?.contains(q) == true)
            matchesType && matchesSearch
        }
    }

    val filteredIncome = filtered.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
    val filteredExpense = filtered.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }

    if (pendingDelete != null) {
        val tx = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar movimiento") },
            text = {
                Text("Se eliminará \"${tx.title}\" y se revertirá automáticamente el saldo en las cuentas afectadas.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTransaction(tx.id)
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

    Column(modifier = modifier.fillMaxSize()) {
        // Mode switcher: Calendario / Lista
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TransactionsViewMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = viewMode == mode,
                    onClick = { onViewModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(idx, TransactionsViewMode.entries.size),
                    icon = {
                        Icon(
                            imageVector = if (mode == TransactionsViewMode.CALENDAR) Icons.Rounded.CalendarMonth
                            else Icons.Rounded.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.testTag("tx_mode_${mode.name.lowercase()}"),
                ) {
                    Text(mode.labelEs)
                }
            }
        }

        if (viewMode == TransactionsViewMode.CALENDAR) {
            CalendarCashflowView(
                transactions = state.transactions,
                accounts = state.accounts,
                categories = state.categories,
                scheduledPayments = state.scheduledPayments,
                hideBalances = state.hideBalances,
                onAddForDate = { dateMillis -> onOpenAddMovement(MovementSheetMode.EXPENSE, dateMillis) },
                onScheduleForDate = onScheduleForDate,
                onTransactionClick = onTransactionClick,
                onRequestDelete = { tx -> pendingDelete = tx },
                onScheduledClick = onScheduledClick,
                onExecuteScheduledNow = onExecuteScheduledNow,
                onAccountClick = onAccountClick,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por concepto, categoría o cuenta...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_search_input"),
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TxTypeFilter.entries) { item ->
                            FilterChip(
                                selected = typeFilter == item,
                                onClick = { typeFilter = item },
                                label = { Text(item.labelEs) },
                            )
                        }
                    }
                }

                if (filtered.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Ingresos mostrados",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = FinanceFormatters.currency(filteredIncome, state.hideBalances),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.Bold,
                                        color = financeColors.income,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Gastos mostrados",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = FinanceFormatters.currency(filteredExpense, state.hideBalances),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.Bold,
                                        color = financeColors.expense,
                                    )
                                }
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyFinanceCard(
                            icon = Icons.Rounded.ReceiptLong,
                            title = if (state.transactions.isEmpty()) "Sin movimientos registrados" else "Sin resultados",
                            subtitle = if (state.transactions.isEmpty()) {
                                "Aquí verás todas tus entradas, salidas, transferencias y compras a MSI."
                            } else {
                                "Prueba cambiando el filtro o los términos de búsqueda."
                            },
                            actionLabel = if (state.transactions.isEmpty()) "Registrar movimiento" else null,
                            onActionClick = if (state.transactions.isEmpty()) {
                                { onOpenAddMovement(MovementSheetMode.EXPENSE, null) }
                            } else null,
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { tx ->
                        val srcName = accountMap[tx.accountId]?.name ?: "Cuenta"
                        val dstName = tx.destinationAccountId?.let { accountMap[it]?.name }
                        val catMeta = categoryMap[tx.categoryLabel.lowercase(Locale.ROOT)]
                        val catColor = categoryColorByName(tx.categoryLabel, catMeta?.colorIndex)
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
                                modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = tx.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (tx.installmentPlanId != null) {
                                            PillBadge(
                                                text = "MSI",
                                                containerColor = financeColors.warningContainer,
                                                contentColor = financeColors.warning,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            isLoanCol && dstName != null -> "Cobro → $dstName (a favor)"
                                            dstName != null -> "$srcName → $dstName"
                                            else -> "$srcName · ${tx.categoryLabel}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = buildString {
                                            append(FinanceFormatters.fullDate(tx.dateMillis))
                                            if (tx.note.isNotBlank()) append(" · ${tx.note}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$prefix${FinanceFormatters.currency(tx.amount, state.hideBalances)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.Bold,
                                        color = amountColor,
                                    )
                                    IconButton(
                                        onClick = { pendingDelete = tx },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Eliminar movimiento",
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
    }
}
