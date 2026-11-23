package com.example.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.ui.components.FinanceComboBox
import com.example.ui.components.accountTypeIcon
import com.example.ui.components.categoryIconByName
import com.example.util.DateMath
import com.example.util.FinanceFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduledSheet(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    editingPayment: ScheduledPaymentEntity? = null,
    initialKind: RecurringKind = RecurringKind.SUBSCRIPTION,
    onDismiss: () -> Unit,
    onCreateAccountFirst: () -> Unit,
    onSave: (ScheduledPaymentEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableStateOf(editingPayment?.recurringKind ?: initialKind) }

    val eligibleAccounts = remember(accounts, kind) {
        if (kind.isIncome) {
            accounts.filter { it.accountType.isLiquidAsset }
        } else {
            accounts.filter {
                it.accountType == AccountType.SAVINGS ||
                    it.accountType == AccountType.DEBIT ||
                    it.accountType == AccountType.CREDIT ||
                    it.accountType == AccountType.CASH
            }
        }
    }
    val availableCategories = remember(categories, kind) {
        val filtered = categories.filter { it.isIncome == kind.isIncome }
        if (filtered.isNotEmpty()) filtered else categories
    }

    var name by remember { mutableStateOf(editingPayment?.name.orEmpty()) }
    var amountText by remember {
        mutableStateOf(
            if (editingPayment != null) editingPayment.amount.toBigDecimal().stripTrailingZeros().toPlainString()
            else ""
        )
    }
    var selectedAccountId by remember {
        mutableStateOf(
            editingPayment?.accountId?.takeIf { id -> eligibleAccounts.any { it.id == id } }
                ?: eligibleAccounts.firstOrNull()?.id
        )
    }
    LaunchedEffect(eligibleAccounts) {
        if (selectedAccountId == null || eligibleAccounts.none { it.id == selectedAccountId }) {
            selectedAccountId = eligibleAccounts.firstOrNull()?.id
        }
    }

    var frequency by remember {
        mutableStateOf(editingPayment?.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY)
    }
    var nextDueDateMillis by remember {
        mutableLongStateOf(editingPayment?.nextDueDateMillis ?: DateMath.dateFromDayOfMonth(1))
    }
    var selectedCategory by remember(availableCategories, kind, editingPayment) {
        val targetName = editingPayment?.categoryLabel ?: when (kind) {
            RecurringKind.SUBSCRIPTION -> "Suscripciones"
            RecurringKind.RECURRING_BILL -> "Servicios"
            RecurringKind.RECURRING_INCOME -> "Salario"
        }
        val match = availableCategories.firstOrNull { it.name.equals(targetName, ignoreCase = true) }
            ?: availableCategories.firstOrNull()
        mutableStateOf(match)
    }
    var notes by remember { mutableStateOf(editingPayment?.notes.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = nextDueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { nextDueDateMillis = it + 43_200_000L }
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
                text = when {
                    editingPayment != null && kind.isIncome -> "Editar ingreso frecuente"
                    editingPayment != null -> "Editar pago programado"
                    kind.isIncome -> "Programar ingreso frecuente"
                    else -> "Programar pago o suscripción"
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            FinanceComboBox(
                label = "Tipo de movimiento recurrente",
                options = RecurringKind.entries,
                selectedOption = kind,
                optionLabel = { it.labelEs },
                optionLeadingIcon = {
                    when (it) {
                        RecurringKind.SUBSCRIPTION -> Icons.Rounded.Subscriptions
                        RecurringKind.RECURRING_BILL -> Icons.Rounded.Bolt
                        RecurringKind.RECURRING_INCOME -> Icons.Rounded.TrendingUp
                    }
                },
                onOptionSelected = { newKind ->
                    kind = newKind
                },
                testTag = "scheduled_kind_combobox",
            )

            if (eligibleAccounts.isEmpty()) {
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
                            text = if (kind.isIncome) {
                                "Primero agrega una cuenta de ahorro, débito o efectivo donde recibir tus ingresos."
                            } else {
                                "Primero agrega una cuenta de ahorro, tarjeta de débito o crédito para domiciliar tus pagos."
                            },
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

            if (editingPayment == null) {
                val presets = when (kind) {
                    RecurringKind.SUBSCRIPTION -> listOf(
                        "Personalizado", "Netflix", "Spotify", "YouTube Premium", "Amazon Prime", "Google One", "iCloud+", "Gimnasio"
                    )
                    RecurringKind.RECURRING_BILL -> listOf(
                        "Personalizado", "Renta", "Luz", "Internet", "Agua", "Telefonía", "Universidad", "Seguro"
                    )
                    RecurringKind.RECURRING_INCOME -> listOf(
                        "Personalizado", "Pago de salario", "Quincena", "Servicios profesionales", "Freelance", "Alquiler cobrado", "Rendimientos / Intereses", "Bono"
                    )
                }
                FinanceComboBox(
                    label = if (kind.isIncome) "Plantilla de ingreso (opcional)" else "Servicio o plantilla rápida (opcional)",
                    options = presets,
                    selectedOption = presets.firstOrNull { it.equals(name, ignoreCase = true) } ?: "Personalizado",
                    optionLabel = { it },
                    onOptionSelected = { chosen ->
                        if (chosen != "Personalizado") {
                            name = chosen
                            errorMessage = null
                        }
                    },
                    testTag = "scheduled_preset_combobox",
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = {
                    Text(
                        when (kind) {
                            RecurringKind.SUBSCRIPTION -> "Nombre de la suscripción"
                            RecurringKind.RECURRING_BILL -> "Nombre del pago frecuente"
                            RecurringKind.RECURRING_INCOME -> "Nombre del ingreso frecuente"
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scheduled_name_input"),
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeDecimal(it); errorMessage = null },
                label = { Text(if (kind.isIncome) "Monto por ingreso ($)" else "Monto por cobro ($)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scheduled_amount_input"),
            )

            FinanceComboBox(
                label = if (kind.isIncome) "Cuenta donde ingresa el dinero" else "Cuenta o tarjeta asociada",
                options = eligibleAccounts,
                selectedOption = eligibleAccounts.firstOrNull { it.id == selectedAccountId },
                optionLabel = { it.name },
                optionSubtitle = { "${it.accountType.shortLabelEs} · ${FinanceFormatters.currency(it.balance)}" },
                optionLeadingIcon = { accountTypeIcon(it.accountType) },
                onOptionSelected = { selectedAccountId = it.id },
                testTag = "scheduled_account_combobox",
            )

            if (availableCategories.isNotEmpty()) {
                FinanceComboBox(
                    label = "Categoría",
                    options = availableCategories,
                    selectedOption = selectedCategory,
                    optionLabel = { it.name },
                    optionLeadingIcon = { categoryIconByName(it.name, it.iconKey) },
                    onOptionSelected = { selectedCategory = it },
                    testTag = "scheduled_category_combobox",
                )
            }

            FinanceComboBox(
                label = "Frecuencia",
                options = RecurrenceFrequency.entries,
                selectedOption = frequency,
                optionLabel = { it.labelEs },
                optionLeadingIcon = { Icons.Rounded.EventRepeat },
                onOptionSelected = { frequency = it },
                testTag = "scheduled_frequency_combobox",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (kind.isIncome) "Próxima fecha de ingreso" else "Próxima fecha de cobro",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.fullDate(nextDueDateMillis),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Button(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cambiar fecha")
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Nota o referencia (opcional)") },
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
                        val trimmed = name.trim()
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val accountId = selectedAccountId
                        if (trimmed.isEmpty()) {
                            errorMessage = "Ingresa un nombre."
                            return@Button
                        }
                        if (amount <= 0.0) {
                            errorMessage = "Ingresa un monto mayor a 0."
                            return@Button
                        }
                        if (accountId == null) {
                            errorMessage = "Selecciona una cuenta o tarjeta."
                            return@Button
                        }
                        val fallbackCat = when (kind) {
                            RecurringKind.SUBSCRIPTION -> "Suscripciones"
                            RecurringKind.RECURRING_BILL -> "Servicios"
                            RecurringKind.RECURRING_INCOME -> "Salario"
                        }
                        onSave(
                            ScheduledPaymentEntity(
                                id = editingPayment?.id ?: 0L,
                                name = trimmed,
                                kind = kind.name,
                                amount = amount,
                                accountId = accountId,
                                category = selectedCategory?.name ?: fallbackCat,
                                frequency = frequency.name,
                                nextDueDateMillis = nextDueDateMillis,
                                isActive = editingPayment?.isActive ?: true,
                                notes = notes.trim(),
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("save_scheduled_button"),
                ) {
                    Text("Guardar")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
