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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.FinanceCategory
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.util.DateMath
import com.example.util.FinanceFormatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddScheduledSheet(
    accounts: List<AccountEntity>,
    editingPayment: ScheduledPaymentEntity? = null,
    initialKind: RecurringKind = RecurringKind.SUBSCRIPTION,
    onDismiss: () -> Unit,
    onCreateAccountFirst: () -> Unit,
    onSave: (ScheduledPaymentEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val eligibleAccounts = remember(accounts) {
        accounts.filter {
            it.accountType == AccountType.SAVINGS ||
                it.accountType == AccountType.DEBIT ||
                it.accountType == AccountType.CREDIT ||
                it.accountType == AccountType.CASH
        }
    }

    var kind by remember { mutableStateOf(editingPayment?.recurringKind ?: initialKind) }
    var name by remember { mutableStateOf(editingPayment?.name.orEmpty()) }
    var amountText by remember {
        mutableStateOf(
            if (editingPayment != null) editingPayment.amount.toBigDecimal().stripTrailingZeros().toPlainString()
            else ""
        )
    }
    var selectedAccountId by remember(eligibleAccounts) {
        mutableStateOf(
            editingPayment?.accountId?.takeIf { id -> eligibleAccounts.any { it.id == id } }
                ?: eligibleAccounts.firstOrNull()?.id
        )
    }
    var frequency by remember {
        mutableStateOf(editingPayment?.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY)
    }
    var nextDueDateMillis by remember {
        mutableLongStateOf(editingPayment?.nextDueDateMillis ?: DateMath.dateFromDayOfMonth(1))
    }
    var category by remember(kind) {
        mutableStateOf(
            editingPayment?.financeCategory
                ?: if (kind == RecurringKind.SUBSCRIPTION) FinanceCategory.SUBSCRIPTIONS else FinanceCategory.UTILITIES
        )
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
                    pickerState.selectedDateMillis?.let { nextDueDateMillis = it }
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
                text = if (editingPayment == null) "Programar pago" else "Editar pago programado",
                style = MaterialTheme.typography.headlineSmall,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RecurringKind.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = kind == item,
                        onClick = {
                            kind = item
                            category = if (item == RecurringKind.SUBSCRIPTION) {
                                FinanceCategory.SUBSCRIPTIONS
                            } else {
                                FinanceCategory.UTILITIES
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, RecurringKind.entries.size),
                    ) {
                        Text(item.labelEs)
                    }
                }
            }

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
                            text = "Primero agrega una cuenta de ahorro, tarjeta de débito o crédito para domiciliar tus pagos.",
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

            // Quick presets for fast entry
            val presets = if (kind == RecurringKind.SUBSCRIPTION) {
                listOf("Netflix", "Spotify", "YouTube Premium", "Amazon Prime", "iCloud+", "Gimnasio")
            } else {
                listOf("Renta", "Luz", "Internet", "Agua", "Telefonía", "Seguro")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                presets.forEach { preset ->
                    AssistChip(
                        onClick = { name = preset; errorMessage = null },
                        label = { Text(preset) },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = {
                    Text(if (kind == RecurringKind.SUBSCRIPTION) "Nombre de la suscripción" else "Nombre del pago frecuente")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scheduled_name_input"),
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeDecimal(it); errorMessage = null },
                label = { Text("Monto por cobro ($)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scheduled_amount_input"),
            )

            Text(
                text = "Cuenta o tarjeta asociada",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                eligibleAccounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = { selectedAccountId = acc.id },
                        label = { Text("${acc.name} (${acc.accountType.shortLabelEs})") },
                    )
                }
            }

            Text(
                text = "Frecuencia",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RecurrenceFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(freq.labelEs) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Próxima fecha de cobro",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = FinanceFormatters.fullDate(nextDueDateMillis),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(
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
                        onSave(
                            ScheduledPaymentEntity(
                                id = editingPayment?.id ?: 0L,
                                name = trimmed,
                                kind = kind.name,
                                amount = amount,
                                accountId = accountId,
                                category = category.name,
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
