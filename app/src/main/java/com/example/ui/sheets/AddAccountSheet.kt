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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.AccountPickerField
import com.example.ui.components.FinanceComboBox
import com.example.ui.components.GestureSafeModalBottomSheet
import com.example.ui.components.accountTypeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    existingAccounts: List<AccountEntity>,
    editingAccount: AccountEntity? = null,
    initialType: AccountType = AccountType.DEBIT,
    onDismiss: () -> Unit,
    onSave: (AccountEntity, Boolean) -> Unit,
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

    val disbursementCandidates = remember(existingAccounts, selectedType, editingAccount) {
        existingAccounts.filter { acc ->
            acc.id != editingAccount?.id && when (selectedType) {
                AccountType.BANK_LOAN -> acc.accountType.isLiquidAsset
                AccountType.LENT_LOAN -> acc.accountType.isLiquidAsset || acc.accountType == AccountType.CREDIT
                else -> false
            }
        }
    }

    var linkedAccountId by remember(disbursementCandidates, editingAccount) {
        val initial = editingAccount?.linkedAccountId?.takeIf { id ->
            disbursementCandidates.any { it.id == id }
        } ?: if (editingAccount == null) disbursementCandidates.firstOrNull()?.id else null
        mutableStateOf(initial)
    }

    var applyDisbursementNow by remember(editingAccount) {
        mutableStateOf(editingAccount == null)
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                text = if (editingAccount == null) "Nueva cuenta o producto" else "Editar ${editingAccount.name}",
                style = MaterialTheme.typography.headlineSmall,
            )

            FinanceComboBox(
                label = "Tipo de cuenta o producto",
                options = AccountType.entries,
                selectedOption = selectedType,
                optionLabel = { it.labelEs },
                optionLeadingIcon = { accountTypeIcon(it) },
                onOptionSelected = {
                    selectedType = it
                    errorMessage = null
                },
                testTag = "account_type_combobox",
            )

            if (editingAccount != null && selectedType != editingAccount.accountType) {
                val bothLiquid = editingAccount.accountType.isLiquidAsset && selectedType.isLiquidAsset
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (bothLiquid) {
                                "Se cambiará de ${editingAccount.accountType.labelEs} a ${selectedType.labelEs} conservando tu saldo y movimientos."
                            } else {
                                "Se cambiará de ${editingAccount.accountType.labelEs} a ${selectedType.labelEs}. Revisa el saldo y los datos propios de este tipo antes de actualizar."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                            AccountType.CREDIT -> "Nombre de la tarjeta (ej. EMMA, Visa Gold)"
                            AccountType.DEBIT -> "Nombre de la tarjeta de débito"
                            AccountType.SAVINGS -> "Nombre de la cuenta de ahorro"
                            AccountType.BANK_LOAN -> "Nombre del préstamo bancario"
                            AccountType.LENT_LOAN -> "Concepto del préstamo realizado"
                            AccountType.CASH -> "Nombre (ej. Cartera / Efectivo)"
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1.4f),
                    )
                    if (selectedType == AccountType.DEBIT || selectedType == AccountType.CREDIT || selectedType == AccountType.SAVINGS) {
                        OutlinedTextField(
                            value = lastFour,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) lastFour = it },
                            label = { Text("Últ. 4") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(14.dp),
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
                shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = monthlyInstallmentText,
                            onValueChange = { monthlyInstallmentText = sanitizeDecimal(it) },
                            label = { Text("Cuota mensual ($)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = interestRateText,
                            onValueChange = { interestRateText = sanitizeDecimal(it) },
                            label = { Text("Tasa anual %") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    LoanDisbursementSection(
                        isLent = false,
                        isNewLoan = editingAccount == null,
                        candidates = disbursementCandidates,
                        selectedAccountId = linkedAccountId,
                        applyDisbursementNow = applyDisbursementNow,
                        onSelectAccount = { linkedAccountId = it },
                        onToggleApplyNow = { applyDisbursementNow = it },
                    )
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
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dueDayText = it },
                            label = { Text("Día de cobro (1–31)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    LoanDisbursementSection(
                        isLent = true,
                        isNewLoan = editingAccount == null,
                        candidates = disbursementCandidates,
                        selectedAccountId = linkedAccountId,
                        applyDisbursementNow = applyDisbursementNow,
                        onSelectAccount = { linkedAccountId = it },
                        onToggleApplyNow = { applyDisbursementNow = it },
                    )
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
                        val resolvedLinkedId = if (selectedType == AccountType.BANK_LOAN || selectedType == AccountType.LENT_LOAN) {
                            linkedAccountId
                        } else null
                        val shouldDisburse = editingAccount == null && resolvedLinkedId != null && applyDisbursementNow

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
                                linkedAccountId = resolvedLinkedId,
                                colorIndex = if (editingAccount == null || editingAccount.accountType != selectedType) {
                                    selectedType.ordinal
                                } else {
                                    editingAccount.colorIndex
                                },
                                createdAt = editingAccount?.createdAt ?: System.currentTimeMillis(),
                            ),
                            shouldDisburse,
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

@Composable
private fun LoanDisbursementSection(
    isLent: Boolean,
    isNewLoan: Boolean,
    candidates: List<AccountEntity>,
    selectedAccountId: Long?,
    applyDisbursementNow: Boolean,
    onSelectAccount: (Long?) -> Unit,
    onToggleApplyNow: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (candidates.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Aún no tienes cuentas de ahorro, débito o efectivo registradas. Puedes guardar el préstamo ahora y asociar la cuenta más tarde.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        } else {
            AccountPickerField(
                label = if (isLent) "Cuenta de donde sale el desembolso" else "Cuenta donde se realiza el desembolso",
                accounts = candidates,
                selectedAccount = candidates.firstOrNull { it.id == selectedAccountId },
                allowNone = true,
                noneLabel = "Sin asociar",
                noneSubtitle = "Solo llevar control del préstamo",
                onAccountSelected = { onSelectAccount(it?.id) },
                testTag = "loan_disbursement_combobox",
            )

            if (isNewLoan && selectedAccountId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLent) "Descontar desembolso ahora" else "Depositar desembolso ahora",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (isLent) {
                                    "Descuenta el monto prestado de esta cuenta y crea el movimiento."
                                } else {
                                    "Suma el monto recibido al saldo de esta cuenta y crea el movimiento."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = applyDisbursementNow,
                            onCheckedChange = onToggleApplyNow,
                            modifier = Modifier.testTag("loan_apply_disbursement_switch"),
                        )
                    }
                }
            }
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
