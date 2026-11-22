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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.accountTypeIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountSheet(
    editingAccount: AccountEntity? = null,
    initialType: AccountType = AccountType.DEBIT,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember {
        mutableStateOf(editingAccount?.accountType ?: initialType)
    }
    var name by remember { mutableStateOf(editingAccount?.name.orEmpty()) }
    var institution by remember { mutableStateOf(editingAccount?.institution.orEmpty()) }
    var lastFour by remember { mutableStateOf(editingAccount?.lastFourDigits.orEmpty()) }
    var balanceText by remember {
        mutableStateOf(
            if (editingAccount != null && editingAccount.balance != 0.0) {
                editingAccount.balance.toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
    var creditLimitText by remember {
        mutableStateOf(
            if (editingAccount != null && editingAccount.creditLimit > 0.0) {
                editingAccount.creditLimit.toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
    var cutoffDayText by remember {
        mutableStateOf((editingAccount?.cutoffDay ?: 15).toString())
    }
    var dueDayText by remember {
        mutableStateOf((editingAccount?.paymentDueDay ?: 5).toString())
    }
    var originalPrincipalText by remember {
        mutableStateOf(
            if (editingAccount != null && editingAccount.originalPrincipal > 0.0) {
                editingAccount.originalPrincipal.toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
    var monthlyInstallmentText by remember {
        mutableStateOf(
            if (editingAccount != null && editingAccount.monthlyInstallment > 0.0) {
                editingAccount.monthlyInstallment.toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
    var interestRateText by remember {
        mutableStateOf(
            if (editingAccount != null && editingAccount.interestRate > 0.0) {
                editingAccount.interestRate.toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
    var savingsGoalText by remember {
        mutableStateOf(
            if (editingAccount != null && editingAccount.savingsGoal > 0.0) {
                editingAccount.savingsGoal.toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
    var counterpartyName by remember { mutableStateOf(editingAccount?.counterpartyName.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                text = if (editingAccount == null) "Nueva cuenta o producto" else "Editar ${editingAccount.name}",
                style = MaterialTheme.typography.headlineSmall,
            )

            if (editingAccount == null) {
                Text(
                    text = "Tipo de producto",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AccountType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type; errorMessage = null },
                            label = { Text(type.shortLabelEs) },
                            leadingIcon = {
                                Icon(
                                    imageVector = accountTypeIcon(type),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.testTag("account_type_chip_${type.name.lowercase()}"),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = {
                    Text(
                        when (selectedType) {
                            AccountType.CREDIT -> "Nombre de la tarjeta (ej. BBVA Azul)"
                            AccountType.DEBIT -> "Nombre de la tarjeta de débito"
                            AccountType.SAVINGS -> "Nombre de la cuenta de ahorro"
                            AccountType.BANK_LOAN -> "Nombre del préstamo bancario"
                            AccountType.LENT_LOAN -> "Concepto del préstamo realizado"
                            AccountType.CASH -> "Nombre (ej. Cartera / Efectivo)"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_name_input"),
            )

            if (selectedType != AccountType.CASH && selectedType != AccountType.LENT_LOAN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("Banco / Institución") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1.4f),
                    )
                    if (selectedType == AccountType.DEBIT || selectedType == AccountType.CREDIT || selectedType == AccountType.SAVINGS) {
                        OutlinedTextField(
                            value = lastFour,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) lastFour = it },
                            label = { Text("Últ. 4") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                        )
                    }
                }
            }

            if (selectedType == AccountType.LENT_LOAN) {
                OutlinedTextField(
                    value = counterpartyName,
                    onValueChange = { counterpartyName = it },
                    label = { Text("¿A quién le prestaste?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = balanceText,
                onValueChange = { balanceText = sanitizeDecimal(it); errorMessage = null },
                label = {
                    Text(
                        when (selectedType) {
                            AccountType.CREDIT -> "Saldo actual a pagar ($)"
                            AccountType.BANK_LOAN -> "Saldo pendiente por pagar ($)"
                            AccountType.LENT_LOAN -> "Saldo pendiente por cobrar ($)"
                            else -> "Saldo actual ($)"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_balance_input"),
            )

            when (selectedType) {
                AccountType.SAVINGS -> {
                    OutlinedTextField(
                        value = savingsGoalText,
                        onValueChange = { savingsGoalText = sanitizeDecimal(it) },
                        label = { Text("Meta de ahorro (opcional, $)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AccountType.CREDIT -> {
                    OutlinedTextField(
                        value = creditLimitText,
                        onValueChange = { creditLimitText = sanitizeDecimal(it); errorMessage = null },
                        label = { Text("Límite de crédito ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("credit_limit_input"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = cutoffDayText,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) cutoffDayText = it },
                            label = { Text("Día de corte (1–31)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("credit_cutoff_input"),
                        )
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dueDayText = it },
                            label = { Text("Día de pago (1–31)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("credit_due_input"),
                        )
                    }
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = sanitizeDecimal(it) },
                        label = { Text("Tasa anual % (opcional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AccountType.BANK_LOAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = originalPrincipalText,
                            onValueChange = { originalPrincipalText = sanitizeDecimal(it) },
                            label = { Text("Monto original ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = monthlyInstallmentText,
                            onValueChange = { monthlyInstallmentText = sanitizeDecimal(it) },
                            label = { Text("Cuota mensual ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dueDayText = it },
                            label = { Text("Día de pago (1–31)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = interestRateText,
                            onValueChange = { interestRateText = sanitizeDecimal(it) },
                            label = { Text("Tasa anual %") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                AccountType.LENT_LOAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = originalPrincipalText,
                            onValueChange = { originalPrincipalText = sanitizeDecimal(it) },
                            label = { Text("Monto prestado ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dueDayText = it },
                            label = { Text("Día de cobro (1–31)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                else -> Unit
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(4.dp))

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
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty()) {
                            errorMessage = "Ingresa un nombre para identificar esta cuenta."
                            return@Button
                        }
                        val balance = balanceText.toDoubleOrNull() ?: 0.0
                        val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                        if (selectedType == AccountType.CREDIT && limit <= 0.0) {
                            errorMessage = "Ingresa el límite de crédito de la tarjeta."
                            return@Button
                        }
                        val cutoff = (cutoffDayText.toIntOrNull() ?: 15).coerceIn(1, 31)
                        val dueDay = (dueDayText.toIntOrNull() ?: 5).coerceIn(1, 31)
                        val principal = (originalPrincipalText.toDoubleOrNull() ?: balance).coerceAtLeast(balance)
                        val monthly = monthlyInstallmentText.toDoubleOrNull() ?: 0.0
                        val rate = interestRateText.toDoubleOrNull() ?: 0.0
                        val goal = savingsGoalText.toDoubleOrNull() ?: 0.0

                        onSave(
                            AccountEntity(
                                id = editingAccount?.id ?: 0L,
                                name = trimmedName,
                                type = selectedType.name,
                                institution = institution.trim(),
                                lastFourDigits = lastFour.trim(),
                                balance = balance,
                                creditLimit = limit,
                                cutoffDay = cutoff,
                                paymentDueDay = dueDay,
                                originalPrincipal = principal,
                                monthlyInstallment = monthly,
                                interestRate = rate,
                                savingsGoal = goal,
                                counterpartyName = counterpartyName.trim(),
                                colorIndex = editingAccount?.colorIndex ?: selectedType.ordinal,
                                createdAt = editingAccount?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("save_account_button"),
                ) {
                    Text(if (editingAccount == null) "Guardar cuenta" else "Actualizar")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

internal fun sanitizeDecimal(input: String): String {
    var dotSeen = false
    val sb = StringBuilder()
    input.forEach { ch ->
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !dotSeen -> {
                dotSeen = true
                sb.append(ch)
            }
        }
    }
    return sb.toString()
}
