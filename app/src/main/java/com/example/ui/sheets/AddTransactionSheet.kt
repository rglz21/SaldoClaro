package com.example.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionType
import com.example.ui.components.AccountPickerField
import com.example.ui.components.FinanceComboBox
import com.example.ui.components.GestureSafeModalBottomSheet
import com.example.ui.components.categoryIconByName
import com.example.ui.theme.JetBrainsMono
import com.example.util.DateMath
import com.example.util.FinanceFormatters

enum class MovementSheetMode(val labelEs: String) {
    EXPENSE("Gasto"),
    INCOME("Ingreso"),
    TRANSFER("Transferir"),
    MSI("MSI"),
}

private data class MsiOption(val months: Int, val label: String)

private val MsiPresets = listOf(
    MsiOption(3, "3 meses sin intereses"),
    MsiOption(6, "6 meses sin intereses"),
    MsiOption(9, "9 meses sin intereses"),
    MsiOption(12, "12 meses sin intereses"),
    MsiOption(18, "18 meses sin intereses"),
    MsiOption(24, "24 meses sin intereses"),
    MsiOption(0, "Otro plazo personalizado"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    initialMode: MovementSheetMode = MovementSheetMode.EXPENSE,
    preselectedAccountId: Long? = null,
    preselectedDestinationId: Long? = null,
    initialDateMillis: Long? = null,
    hideBalances: Boolean = false,
    onDismiss: () -> Unit,
    onCreateAccountFirst: (AccountType) -> Unit,
    onRecordIncomeOrExpense: (
        title: String,
        amount: Double,
        type: TransactionType,
        categoryName: String,
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
        categoryName: String,
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
    var selectedDateMillis by remember { mutableLongStateOf(initialDateMillis ?: System.currentTimeMillis()) }
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

    val filteredCategories = remember(categories, mode) {
        val wantIncome = mode == MovementSheetMode.INCOME
        val list = categories.filter { it.isIncome == wantIncome }
        if (list.isNotEmpty()) list else categories
    }

    var selectedCategory by remember(filteredCategories, mode) {
        mutableStateOf(filteredCategories.firstOrNull())
    }

    var selectedMsiPreset by remember { mutableStateOf(MsiPresets[1]) } // 6 MSI default
    var customMsiMonthsText by remember { mutableStateOf("6") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        // Add 12h offset to avoid UTC midnight rolling back a day in local TZ
                        selectedDateMillis = it + 43_200_000L
                    }
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

    GestureSafeModalBottomSheet(
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
                                onCreateAccountFirst(AccountType.DEBIT)
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
                                onCreateAccountFirst(AccountType.CREDIT)
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

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeDecimal(it); errorMessage = null },
                label = {
                    Text(
                        if (mode == MovementSheetMode.MSI) "Monto total de la compra ($)" else "Monto ($)"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                            MovementSheetMode.EXPENSE -> "Concepto (ej. Almuerzo, Gasolina)"
                            MovementSheetMode.INCOME -> "Concepto (ej. Salario, Reembolso)"
                            MovementSheetMode.TRANSFER -> "Concepto (ej. Pago TDC, Ahorro)"
                            MovementSheetMode.MSI -> "Producto / Compra (ej. Laptop, Vuelo)"
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input"),
            )

            // Source Account Picker Sheet
            val selectableAccounts = if (mode == MovementSheetMode.MSI) creditCards else accounts
            val selectedAccount = selectableAccounts.firstOrNull { it.id == selectedAccountId }
            AccountPickerField(
                label = when (mode) {
                    MovementSheetMode.EXPENSE -> "Cuenta o tarjeta de cargo"
                    MovementSheetMode.INCOME -> "Cuenta donde entra el dinero"
                    MovementSheetMode.TRANSFER -> "Cuenta origen"
                    MovementSheetMode.MSI -> "Tarjeta de crédito"
                },
                accounts = selectableAccounts,
                selectedAccount = selectedAccount,
                hideBalances = hideBalances,
                onAccountSelected = { acc ->
                    if (acc != null) {
                        selectedAccountId = acc.id
                        if (destinationAccountId == acc.id) {
                            destinationAccountId = accounts.firstOrNull { it.id != acc.id }?.id
                        }
                        errorMessage = null
                    }
                },
                onAddAccount = { suggestedType ->
                    onCreateAccountFirst(if (mode == MovementSheetMode.MSI) AccountType.CREDIT else suggestedType)
                },
                testTag = "tx_source_account_combobox",
            )

            // Destination Account Picker Sheet for Transfer
            if (mode == MovementSheetMode.TRANSFER && accounts.size >= 2) {
                val destOptions = accounts.filter { it.id != selectedAccountId }
                val selectedDest = destOptions.firstOrNull { it.id == destinationAccountId }
                AccountPickerField(
                    label = "Cuenta, tarjeta o préstamo destino",
                    accounts = destOptions,
                    selectedAccount = selectedDest,
                    hideBalances = hideBalances,
                    onAccountSelected = { acc ->
                        if (acc != null) {
                            destinationAccountId = acc.id
                            errorMessage = null
                        }
                    },
                    onAddAccount = { suggestedType -> onCreateAccountFirst(suggestedType) },
                    testTag = "tx_dest_account_combobox",
                )
            }

            // MSI Term ComboBox
            if (mode == MovementSheetMode.MSI) {
                FinanceComboBox(
                    label = "Plazo en meses sin intereses",
                    options = MsiPresets,
                    selectedOption = selectedMsiPreset,
                    optionLabel = { it.label },
                    onOptionSelected = { preset ->
                        selectedMsiPreset = preset
                        if (preset.months > 0) {
                            customMsiMonthsText = preset.months.toString()
                        }
                    },
                    testTag = "tx_msi_months_combobox",
                )
                if (selectedMsiPreset.months == 0) {
                    OutlinedTextField(
                        value = customMsiMonthsText,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) customMsiMonthsText = it },
                        label = { Text("Número de meses") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val totalAmt = amountText.toDoubleOrNull() ?: 0.0
                val months = if (selectedMsiPreset.months > 0) selectedMsiPreset.months else (customMsiMonthsText.toIntOrNull() ?: 0)
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

            // Category ComboBox
            if (mode != MovementSheetMode.TRANSFER && filteredCategories.isNotEmpty()) {
                FinanceComboBox(
                    label = "Categoría",
                    options = filteredCategories,
                    selectedOption = selectedCategory,
                    optionLabel = { it.name },
                    optionSubtitle = { cat ->
                        if (cat.monthlyBudget > 0.0) "Presupuesto: ${FinanceFormatters.currency(cat.monthlyBudget)}/mes"
                        else null
                    },
                    optionLeadingIcon = { categoryIconByName(it.name, it.iconKey) },
                    onOptionSelected = { selectedCategory = it },
                    testTag = "tx_category_combobox",
                )
            }

            // Date selector with clear active state highlight
            val todayStart = remember { DateMath.startOfDay(System.currentTimeMillis()) }
            val yesterdayStart = remember(todayStart) { todayStart - 86_400_000L }
            val selectedDayStart = DateMath.startOfDay(selectedDateMillis)
            val isToday = selectedDayStart == todayStart
            val isYesterday = selectedDayStart == yesterdayStart
            val isCustomDate = !isToday && !isYesterday

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Fecha: ${FinanceFormatters.fullDate(selectedDateMillis)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateOptionButton(
                        label = "Hoy",
                        selected = isToday,
                        onClick = { selectedDateMillis = System.currentTimeMillis() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_date_today"),
                    )
                    DateOptionButton(
                        label = "Ayer",
                        selected = isYesterday,
                        onClick = { selectedDateMillis = System.currentTimeMillis() - 86_400_000L },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_date_yesterday"),
                    )
                    DateOptionButton(
                        label = if (isCustomDate) FinanceFormatters.shortDate(selectedDateMillis) else "Otra fecha",
                        selected = isCustomDate,
                        showCalendarIcon = !isCustomDate,
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("tx_date_custom"),
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota (opcional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                                val catName = selectedCategory?.name ?: "Otros"
                                val resolvedTitle = title.trim().ifEmpty { catName }
                                onRecordIncomeOrExpense(
                                    resolvedTitle,
                                    amount,
                                    if (mode == MovementSheetMode.INCOME) TransactionType.INCOME else TransactionType.EXPENSE,
                                    catName,
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
                                val months = if (selectedMsiPreset.months > 0) {
                                    selectedMsiPreset.months
                                } else {
                                    customMsiMonthsText.toIntOrNull() ?: 0
                                }
                                if (months < 2) {
                                    errorMessage = "Ingresa al menos 2 meses sin intereses."
                                    return@Button
                                }
                                val catName = selectedCategory?.name ?: "Compras"
                                val desc = title.trim().ifEmpty { catName }
                                onRecordInstallment(
                                    sourceId,
                                    desc,
                                    amount,
                                    months,
                                    catName,
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

@Composable
private fun DateOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCalendarIcon: Boolean = false,
) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            modifier = modifier,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(label, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            modifier = modifier,
        ) {
            if (showCalendarIcon) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(label, maxLines = 1)
        }
    }
}
