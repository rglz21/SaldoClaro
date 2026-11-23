package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.FinanceCategory
import com.example.ui.theme.ChartPalette
import kotlin.math.abs

data class CategoryIconOption(
    val key: String,
    val labelEs: String,
    val icon: ImageVector,
)

val AvailableCategoryIcons = listOf(
    CategoryIconOption("FOOD", "Comida / Restaurantes", Icons.Rounded.Restaurant),
    CategoryIconOption("SHOPPING", "Compras / Gustos", Icons.Rounded.ShoppingBag),
    CategoryIconOption("TRANSPORT", "Transporte / Gasolina", Icons.Rounded.DirectionsCar),
    CategoryIconOption("HOUSING", "Hogar / Familia", Icons.Rounded.Home),
    CategoryIconOption("UTILITIES", "Servicios (Luz, Internet)", Icons.Rounded.Bolt),
    CategoryIconOption("SUBSCRIPTIONS", "Suscripciones", Icons.Rounded.Subscriptions),
    CategoryIconOption("ENTERTAINMENT", "Entretenimiento", Icons.Rounded.Movie),
    CategoryIconOption("HEALTH", "Salud / Medicinas", Icons.Rounded.HealthAndSafety),
    CategoryIconOption("EDUCATION", "Educación", Icons.Rounded.School),
    CategoryIconOption("TRAVEL", "Viajes", Icons.Rounded.Flight),
    CategoryIconOption("FITNESS", "Deporte / Gimnasio", Icons.Rounded.SportsGymnastics),
    CategoryIconOption("PETS", "Mascotas", Icons.Rounded.Pets),
    CategoryIconOption("SALARY", "Salario / Nómina", Icons.Rounded.Work),
    CategoryIconOption("INVESTMENTS", "Inversiones", Icons.Rounded.TrendingUp),
    CategoryIconOption("FREELANCE", "Reembolso / Ingreso extra", Icons.Rounded.Payments),
    CategoryIconOption("LOAN_COLLECTION", "Préstamos", Icons.Rounded.Handshake),
    CategoryIconOption("OTHER", "General / Otros", Icons.Rounded.Autorenew),
)

fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.SAVINGS -> Icons.Rounded.Savings
    AccountType.DEBIT -> Icons.Rounded.AccountBalance
    AccountType.CREDIT -> Icons.Rounded.CreditCard
    AccountType.BANK_LOAN -> Icons.Rounded.ReceiptLong
    AccountType.LENT_LOAN -> Icons.Rounded.Handshake
    AccountType.CASH -> Icons.Rounded.AccountBalanceWallet
}

fun iconForKey(iconKey: String): ImageVector {
    return AvailableCategoryIcons.firstOrNull { it.key == iconKey }?.icon ?: Icons.Rounded.Autorenew
}

fun categoryIcon(category: FinanceCategory): ImageVector = when (category) {
    FinanceCategory.FOOD -> Icons.Rounded.Restaurant
    FinanceCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
    FinanceCategory.HOUSING -> Icons.Rounded.Home
    FinanceCategory.UTILITIES -> Icons.Rounded.Bolt
    FinanceCategory.ENTERTAINMENT -> Icons.Rounded.Movie
    FinanceCategory.SUBSCRIPTIONS -> Icons.Rounded.Subscriptions
    FinanceCategory.SHOPPING -> Icons.Rounded.ShoppingBag
    FinanceCategory.HEALTH -> Icons.Rounded.HealthAndSafety
    FinanceCategory.EDUCATION -> Icons.Rounded.School
    FinanceCategory.TRAVEL -> Icons.Rounded.Flight
    FinanceCategory.CARD_PAYMENT -> Icons.Rounded.CreditCard
    FinanceCategory.LOAN_PAYMENT -> Icons.Rounded.ReceiptLong
    FinanceCategory.LOAN_DISBURSEMENT -> Icons.Rounded.Payments
    FinanceCategory.TRANSFER -> Icons.AutoMirrored.Rounded.CompareArrows
    FinanceCategory.SALARY -> Icons.Rounded.Work
    FinanceCategory.FREELANCE -> Icons.Rounded.Payments
    FinanceCategory.INVESTMENTS -> Icons.Rounded.TrendingUp
    FinanceCategory.LOAN_COLLECTION -> Icons.Rounded.Handshake
    FinanceCategory.OTHER -> Icons.Rounded.Autorenew
}

fun categoryIconByName(name: String, iconKeyHint: String? = null): ImageVector {
    if (!iconKeyHint.isNullOrBlank()) {
        AvailableCategoryIcons.firstOrNull { it.key == iconKeyHint }?.let { return it.icon }
    }
    return when (name.trim().lowercase()) {
        "restaurantes", "alimentación", "mandado" -> Icons.Rounded.Restaurant
        "transporte" -> Icons.Rounded.DirectionsCar
        "familia", "vivienda y renta" -> Icons.Rounded.Home
        "servicios" -> Icons.Rounded.Bolt
        "entretenimiento", "salidas" -> Icons.Rounded.Movie
        "suscripciones" -> Icons.Rounded.Subscriptions
        "gustos", "compras", "ropa" -> Icons.Rounded.ShoppingBag
        "salud" -> Icons.Rounded.HealthAndSafety
        "educación" -> Icons.Rounded.School
        "viajes" -> Icons.Rounded.Flight
        "mascotas" -> Icons.Rounded.Pets
        "pago de tarjeta" -> Icons.Rounded.CreditCard
        "pago de préstamo" -> Icons.Rounded.ReceiptLong
        "desembolso de préstamo" -> Icons.Rounded.Payments
        "transferencia" -> Icons.AutoMirrored.Rounded.CompareArrows
        "salario", "nómina / sueldo" -> Icons.Rounded.Work
        "inversiones" -> Icons.Rounded.TrendingUp
        "reembolso / cashback", "ingresos extra", "freelance", "bonos y comisiones", "beneficios del gobierno" -> Icons.Rounded.Payments
        "cobro de préstamo", "préstamo" -> Icons.Rounded.Handshake
        else -> Icons.Rounded.Autorenew
    }
}

fun categoryColorByName(name: String, colorIndexHint: Int? = null): Color {
    val idx = colorIndexHint?.let { abs(it) % ChartPalette.size }
        ?: (abs(name.trim().lowercase().hashCode()) % ChartPalette.size)
    return ChartPalette[idx]
}

fun isLoanRepaymentCollection(
    tx: com.example.data.model.TransactionEntity,
    accountMap: Map<Long, com.example.data.model.AccountEntity>,
): Boolean {
    if (tx.transactionType != com.example.data.model.TransactionType.TRANSFER) return false
    val srcType = accountMap[tx.accountId]?.accountType
    return srcType == AccountType.LENT_LOAN ||
        tx.categoryLabel.equals("Cobro de Préstamo", ignoreCase = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FinanceComboBox(
    label: String,
    options: List<T>,
    selectedOption: T?,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionSubtitle: ((T) -> String?)? = null,
    optionLeadingIcon: ((T) -> ImageVector)? = null,
    placeholder: String = "Seleccionar...",
    enabled: Boolean = true,
    testTag: String = "combo_box",
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = selectedOption?.let(optionLabel).orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = if (selectedOption != null && optionLeadingIcon != null) {
                {
                    Icon(
                        imageVector = optionLeadingIcon(selectedOption),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .testTag(testTag),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                val subtitle = optionSubtitle?.invoke(option)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = optionLabel(option),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    leadingIcon = optionLeadingIcon?.let { iconProvider ->
                        {
                            Icon(
                                imageVector = iconProvider(option),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun EmptyFinanceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    actionTestTag: String = "empty_action_button",
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag(actionTestTag),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun PillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    containerColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun LocalStorageNoticeBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Base de datos 100% local en tu dispositivo (privada y sin conexión).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
