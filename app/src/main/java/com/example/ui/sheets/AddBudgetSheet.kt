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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.model.CategoryEntity
import com.example.ui.components.AvailableCategoryIcons
import com.example.ui.components.FinanceComboBox
import com.example.ui.components.categoryIconByName
import com.example.util.FinanceFormatters

private enum class CategoryKindOption(val labelEs: String, val isIncome: Boolean) {
    EXPENSE("Gasto", false),
    INCOME("Ingreso", true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategorySheet(
    editingCategory: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(editingCategory?.name.orEmpty()) }
    var kind by remember {
        mutableStateOf(
            if (editingCategory?.isIncome == true) CategoryKindOption.INCOME else CategoryKindOption.EXPENSE
        )
    }
    var selectedIcon by remember {
        mutableStateOf(
            AvailableCategoryIcons.firstOrNull { it.key == editingCategory?.iconKey }
                ?: AvailableCategoryIcons.first()
        )
    }
    var budgetText by remember {
        mutableStateOf(
            if (editingCategory != null && editingCategory.monthlyBudget > 0.0) {
                editingCategory.monthlyBudget.toBigDecimal().stripTrailingZeros().toPlainString()
            } else ""
        )
    }
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
                text = if (editingCategory == null) "Nueva categoría" else "Editar categoría",
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("Nombre de la categoría (ej. Mascotas, Gimnasio)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_name_input"),
            )

            FinanceComboBox(
                label = "Tipo de categoría",
                options = CategoryKindOption.entries,
                selectedOption = kind,
                optionLabel = { it.labelEs },
                optionLeadingIcon = {
                    if (it.isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
                },
                onOptionSelected = { kind = it },
                testTag = "category_kind_combobox",
            )

            FinanceComboBox(
                label = "Ícono",
                options = AvailableCategoryIcons,
                selectedOption = selectedIcon,
                optionLabel = { it.labelEs },
                optionLeadingIcon = { it.icon },
                onOptionSelected = { selectedIcon = it },
                testTag = "category_icon_combobox",
            )

            if (!kind.isIncome) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = sanitizeDecimal(it); errorMessage = null },
                    label = { Text("Presupuesto mensual opcional ($)") },
                    supportingText = { Text("Déjalo vacío o en 0 si no deseas fijar límite todavía.") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_budget_input"),
                )
            }

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
                        if (trimmed.isEmpty()) {
                            errorMessage = "Ingresa un nombre para la categoría."
                            return@Button
                        }
                        val budget = if (kind.isIncome) 0.0 else (budgetText.toDoubleOrNull() ?: 0.0)
                        onSave(
                            CategoryEntity(
                                id = editingCategory?.id ?: 0L,
                                name = trimmed,
                                isIncome = kind.isIncome,
                                iconKey = selectedIcon.key,
                                colorIndex = editingCategory?.colorIndex ?: (trimmed.hashCode() and 0x7FFFFFFF) % 10,
                                monthlyBudget = budget,
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("save_category_button"),
                ) {
                    Text("Guardar")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    categories: List<CategoryEntity>,
    preselectedCategory: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity, Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseCategories = remember(categories) { categories.filter { !it.isIncome } }
    var selectedCategory by remember(expenseCategories, preselectedCategory) {
        mutableStateOf(preselectedCategory ?: expenseCategories.firstOrNull())
    }
    var limitText by remember(selectedCategory) {
        val current = selectedCategory?.monthlyBudget ?: 0.0
        mutableStateOf(
            if (current > 0.0) current.toBigDecimal().stripTrailingZeros().toPlainString() else ""
        )
    }
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
                text = "Asignar presupuesto mensual",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Selecciona una categoría y define su tope mensual para recibir alertas visuales.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (expenseCategories.isNotEmpty()) {
                FinanceComboBox(
                    label = "Categoría",
                    options = expenseCategories,
                    selectedOption = selectedCategory,
                    optionLabel = { it.name },
                    optionSubtitle = { cat ->
                        if (cat.monthlyBudget > 0.0) "Actual: ${FinanceFormatters.currency(cat.monthlyBudget)}/mes"
                        else "Sin presupuesto asignado"
                    },
                    optionLeadingIcon = { categoryIconByName(it.name, it.iconKey) },
                    onOptionSelected = {
                        selectedCategory = it
                        limitText = if (it.monthlyBudget > 0.0) {
                            it.monthlyBudget.toBigDecimal().stripTrailingZeros().toPlainString()
                        } else ""
                        errorMessage = null
                    },
                    testTag = "budget_category_combobox",
                )
            }

            OutlinedTextField(
                value = limitText,
                onValueChange = { limitText = sanitizeDecimal(it); errorMessage = null },
                label = { Text("Presupuesto mensual ($)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_limit_input"),
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
                        val target = selectedCategory
                        val limit = limitText.toDoubleOrNull() ?: 0.0
                        if (target == null) {
                            errorMessage = "Selecciona una categoría."
                            return@Button
                        }
                        if (limit <= 0.0) {
                            errorMessage = "Ingresa un presupuesto mayor a 0."
                            return@Button
                        }
                        onSave(target, limit)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("save_budget_button"),
                ) {
                    Text("Guardar presupuesto")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
