package com.example.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.FinanceCategory
import com.example.data.model.TransactionType
import com.example.ui.components.categoryIcon
import com.example.ui.theme.JetBrainsMono
import com.example.util.FinanceFormatters

enum class MovementSheetMode(val labelEs: String) {
    EXPENSE("Gasto"),
    INCOME("Ingreso"),
    TRANSFER("Transferir"),
    MSI("MSI"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountEntity>,
    initialMode: MovementSheetMode = MovementSheetMode.EXPENSE,
    preselectedAccountId: Long? = null,
    preselectedDestinationId: Long? = null,
    onDismiss: () -> Unit,
    onCreateAccountFirst: () -> Unit,
    onRecordIncomeOrExpense: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: FinanceCategory,
        accountId: Long,
        dateMillis: Long,
        note: String,
    ) -> Unit,
    onRecordTransfer: (
        title: String,
        amount: Double,
        fromAccountId: Long,
        toAccountId: Long,
        dateMillis: Long,
        note: String,
    ) -> Unit,
    onRecordInstallment: (
        creditCardId: Long,
        description: String,
        totalAmount: Double,
        totalMonths: Int,
        category: FinanceCategory,
        startDateMillis: Long,
        note: String,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val creditCards = remember(accounts) { accounts.filter { it.accountType == AccountType.CREDIT } }

    var mode by remember { mutableStateOf(initialMode) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedAccountId by remember(accounts, preselectedAccountId, mode) {
        val defaultId = when {
            mode == MovementSheetMode.MSI -> {
                creditCards.firstOrNull { it.id == preselectedAccountId }?.id
                    ?: creditCards.firstOrNull()?.id
            }
            preselectedAccountId != null && accounts.any { it.id == preselectedAccountId } -> preselectedAccountId
            mode == MovementSheetMode.TRANSFER -> {
                accounts.firstOrNull { it.accountType.isLiquidAsset && it.id != preselectedDestinationId }?.id
                    ?: accounts.firstOrNull { it.id != preselectedDestinationId }?.id
            }
            else -> accounts.firstOrNull()?.id
        }
        mutableStateOf(defaultId)
    }

    var destinationAccountId by remember(accounts, preselectedDestinationId, selectedAccountId) {
        val defaultDest = when {
            preselectedDestinationId != null && accounts.any { it.id == preselectedDestinationId } -> preselectedDestinationId
            else -> accounts.firstOrNull { it.id != selectedAccountId }?.id
        }
        mutableStateOf(defaultDest)
    }

    var selectedCategory by remember(mode) {
        mutableStateOf(
            when (mode) {
                MovementSheetMode.INCOME -> FinanceCategory.SALARY
                MovementSheetMode.MSI -> FinanceCategory.SHOPPING
                else -> FinanceCategory.FOOD
            }
        )
    }

    var msiMonthsText by remember { mutableStateOf("6") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("Elegir") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
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
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Registrar movimiento",
                style = MaterialTheme.typography.headlineSmall,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MovementSheetMode.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = mode == item,
                        onClick = {
                            mode = item
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, MovementSheetMode.entries.size),
                        modifier = Modifier.testTag("movement_mode_${item.name.lowercase()}"),
                    ) {
                        Text(item.labelEs, maxLines = 1)
                    }
                }
            }

            if (accounts.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Primero crea al menos una cuenta o tarjeta para asociar tus movimientos.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = {
                                onDismiss()
                                onCreateAccountFirst()
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Agregar cuenta o tarjeta")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                return@Column
            }

            if (mode == MovementSheetMode.MSI && creditCards.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Necesitas registrar una Tarjeta de Crédito para agregar compras a Meses Sin Intereses.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Button(
                            onClick = {
                                onDismiss()
                                onCreateAccountFirst()
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Agregar Tarjeta de Crédito")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                return@Column
            }

            if (mode == MovementSheetMode.TRANSFER && accounts.size < 2) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Agrega al menos dos cuentas o tarjetas para transferir dinero entre ellas o pagar tu tarjeta de crédito.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeDecimal(it); errorMessage = null },
                label = {
                    Text(
                        if (mode == MovementSheetMode.MSI) "Monto total de la compra ($)" else "Monto ($)"
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input"),
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMessage = null },
                label = {
                    Text(
                        when (mode) {
                            MovementSheetMode.EXPENSE -> "Concepto (ej. Supermercado, Café)"
                            MovementSheetMode.INCOME -> "Concepto (ej. Quincena, Venta)"
                            MovementSheetMode.TRANSFER -> "Concepto (ej. Pago TDC, Ahorro)"
                            MovementSheetMode.MSI -> "Producto / Compra (ej. Laptop, Vuelo)"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input"),
            )

            // Account selection
            val selectableAccounts = if (mode == MovementSheetMode.MSI) creditCards else accounts
            Text(
                text = when (mode) {
                    MovementSheetMode.EXPENSE -> "Cuenta o tarjeta de cargo"
                    MovementSheetMode.INCOME -> "Cuenta donde entra el dinero"
                    MovementSheetMode.TRANSFER -> "Desde cuenta origen"
                    MovementSheetMode.MSI -> "Tarjeta de crédito"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                selectableAccounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = {
                            selectedAccountId = acc.id
                            if (destinationAccountId == acc.id) {
                                destinationAccountId = accounts.firstOrNull { it.id != acc.id }?.id
                            }
                            errorMessage = null
                        },
                        label = { Text("${acc.name} (${acc.accountType.shortLabelEs})") },
                    )
                }
            }

            if (mode == MovementSheetMode.TRANSFER && accounts.size >= 2) {
                Text(
                    text = "Hacia cuenta, tarjeta o préstamo destino",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                        FilterChip(
                            selected = destinationAccountId == acc.id,
                            onClick = { destinationAccountId = acc.id; errorMessage = null },
                            label = { Text("${acc.name} (${acc.accountType.shortLabelEs})") },
                        )
                    }
                }
            }

            if (mode == MovementSheetMode.MSI) {
                Text(
                    text = "Plazo en meses sin intereses",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(3, 6, 9, 12, 18, 24).forEach { m ->
                        FilterChip(
                            selected = msiMonthsText == m.toString(),
                            onClick = { msiMonthsText = m.toString() },
                            label = { Text("$m MSI") },
                        )
                    }
                }
                OutlinedTextField(
                    value = msiMonthsText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) msiMonthsText = it },
                    label = { Text("Meses personalizados") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                val totalAmt = amountText.toDoubleOrNull() ?: 0.0
                val months = msiMonthsText.toIntOrNull() ?: 0
                if (totalAmt > 0.0 && months >= 2) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Cuota mensual estimada",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = FinanceFormatters.currency(totalAmt / months),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            if (mode != MovementSheetMode.TRANSFER) {
                val categories = if (mode == MovementSheetMode.INCOME) {
                    FinanceCategory.incomeCategories
                } else {
                    FinanceCategory.expenseCategories
                }
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.labelEs) },
                            leadingIcon = {
                                Icon(imageVector = categoryIcon(cat), contentDescription = null)
                            },
                        )
                    }
                }
            }

            // Date selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { selectedDateMillis = System.currentTimeMillis() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Hoy")
                }
                OutlinedButton(
                    onClick = { selectedDateMillis = System.currentTimeMillis() - 86_400_000L },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Ayer")
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.4f),
                ) {
                    Text(FinanceFormatters.shortDate(selectedDateMillis))
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota (opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            errorMessage = "Ingresa un monto válido mayor a 0."
                            return@Button
                        }
                        val sourceId = selectedAccountId
                        if (sourceId == null) {
                            errorMessage = "Selecciona una cuenta o tarjeta."
                            return@Button
                        }

                        when (mode) {
                            MovementSheetMode.EXPENSE, MovementSheetMode.INCOME -> {
                                val resolvedTitle = title.trim().ifEmpty { selectedCategory.labelEs }
                                onRecordIncomeOrExpense(
                                    resolvedTitle,
                                    amount,
                                    if (mode == MovementSheetMode.INCOME) TransactionType.INCOME else TransactionType.EXPENSE,
                                    selectedCategory,
                                    sourceId,
                                    selectedDateMillis,
                                    note,
                                )
                            }

                            MovementSheetMode.TRANSFER -> {
                                val destId = destinationAccountId
                                if (destId == null || destId == sourceId) {
                                    errorMessage = "Selecciona una cuenta de destino diferente."
                                    return@Button
                                }
                                onRecordTransfer(title, amount, sourceId, destId, selectedDateMillis, note)
                            }

                            MovementSheetMode.MSI -> {
                                val months = msiMonthsText.toIntOrNull() ?: 0
                                if (months < 2) {
                                    errorMessage = "Ingresa al menos 2 meses sin intereses."
                                    return@Button
                                }
                                val desc = title.trim().ifEmpty { selectedCategory.labelEs }
                                onRecordInstallment(
                                    sourceId,
                                    desc,
                                    amount,
                                    months,
                                    selectedCategory,
                                    selectedDateMillis,
                                    note,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("save_tx_button"),
                ) {
                    Text("Guardar")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
