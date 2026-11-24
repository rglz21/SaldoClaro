package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.util.FinanceFormatters
import kotlin.math.max

private enum class AccountPickerTab(
    val labelEs: String,
    val defaultCreateType: AccountType,
) {
    ACCOUNTS("Cuentas", AccountType.DEBIT),
    CREDIT_CARDS("Tarjetas de crédito", AccountType.CREDIT),
    LOANS("Préstamos", AccountType.BANK_LOAN),
}

private data class AccountPickerSection(
    val headerEs: String,
    val accounts: List<AccountEntity>,
)

/**
 * Form field that looks like an OutlinedTextField and opens a rich, tabbed bottom sheet
 * grouped by account type (Débito, Ahorro, Efectivo | Tarjetas de crédito | Préstamos),
 * with exact balances and an optional "Agregar Cuenta" CTA button at the bottom.
 */
@Composable
fun AccountPickerField(
    label: String,
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    onAccountSelected: (AccountEntity?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Seleccionar cuenta...",
    allowNone: Boolean = false,
    noneLabel: String = "Sin asociar",
    noneSubtitle: String = "Solo llevar control del registro",
    hideBalances: Boolean = false,
    enabled: Boolean = true,
    onAddAccount: ((AccountType) -> Unit)? = null,
    testTag: String = "account_picker_field",
) {
    var showPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val displayText = when {
        selectedAccount != null -> {
            val bal = FinanceFormatters.currency(selectedAccount.balance, hideBalances)
            "${selectedAccount.name}  ·  $bal"
        }
        allowNone -> noneLabel
        else -> ""
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = when {
                        selectedAccount != null -> accountTypeIcon(selectedAccount.accountType)
                        allowNone -> Icons.Rounded.Block
                        else -> accountTypeIcon(AccountType.DEBIT)
                    },
                    contentDescription = null,
                    tint = if (selectedAccount != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Abrir selector de cuentas",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Transparent clickable overlay so tapping anywhere on the field reliably opens the sheet
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .testTag(testTag)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    showPicker = true
                },
        )
    }

    if (showPicker) {
        AccountPickerModalSheet(
            accounts = accounts,
            selectedAccountId = selectedAccount?.id,
            allowNone = allowNone,
            noneLabel = noneLabel,
            noneSubtitle = noneSubtitle,
            hideBalances = hideBalances,
            onSelect = { chosen ->
                onAccountSelected(chosen)
                showPicker = false
            },
            onAddAccount = onAddAccount?.let { callback ->
                { suggestedType ->
                    showPicker = false
                    callback(suggestedType)
                }
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerModalSheet(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    allowNone: Boolean = false,
    noneLabel: String = "Sin asociar",
    noneSubtitle: String = "Solo llevar control del registro",
    hideBalances: Boolean = false,
    onSelect: (AccountEntity?) -> Unit,
    onAddAccount: ((AccountType) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current

    val liquidAccounts = remember(accounts) {
        accounts.filter { it.accountType.isLiquidAsset }
    }
    val creditCards = remember(accounts) {
        accounts.filter { it.accountType == AccountType.CREDIT }
    }
    val loanAccounts = remember(accounts) {
        accounts.filter {
            it.accountType == AccountType.BANK_LOAN || it.accountType == AccountType.LENT_LOAN
        }
    }

    val availableTabs = remember(liquidAccounts, creditCards, loanAccounts) {
        buildList {
            if (liquidAccounts.isNotEmpty() || (creditCards.isEmpty() && loanAccounts.isEmpty())) {
                add(AccountPickerTab.ACCOUNTS)
            }
            if (creditCards.isNotEmpty()) {
                add(AccountPickerTab.CREDIT_CARDS)
            }
            if (loanAccounts.isNotEmpty()) {
                add(AccountPickerTab.LOANS)
            }
        }
    }

    val initialTab = remember(selectedAccountId, accounts, availableTabs) {
        val current = accounts.firstOrNull { it.id == selectedAccountId }
        val preferred = when (current?.accountType) {
            AccountType.CREDIT -> AccountPickerTab.CREDIT_CARDS
            AccountType.BANK_LOAN, AccountType.LENT_LOAN -> AccountPickerTab.LOANS
            else -> AccountPickerTab.ACCOUNTS
        }
        if (preferred in availableTabs) preferred else availableTabs.first()
    }

    var activeTab by remember { mutableStateOf(initialTab) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val sections = remember(activeTab, liquidAccounts, creditCards, loanAccounts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        fun List<AccountEntity>.filterQuery(): List<AccountEntity> =
            if (q.isEmpty()) this
            else filter {
                it.name.lowercase().contains(q) ||
                    it.institution.lowercase().contains(q) ||
                    it.accountType.labelEs.lowercase().contains(q)
            }

        when (activeTab) {
            AccountPickerTab.ACCOUNTS -> buildList {
                val debits = liquidAccounts.filter { it.accountType == AccountType.DEBIT }.filterQuery()
                val savings = liquidAccounts.filter { it.accountType == AccountType.SAVINGS }.filterQuery()
                val cash = liquidAccounts.filter { it.accountType == AccountType.CASH }.filterQuery()
                if (debits.isNotEmpty()) add(AccountPickerSection("DÉBITO", debits))
                if (savings.isNotEmpty()) add(AccountPickerSection("AHORRO", savings))
                if (cash.isNotEmpty()) add(AccountPickerSection("EFECTIVO", cash))
            }

            AccountPickerTab.CREDIT_CARDS -> {
                val filtered = creditCards.filterQuery()
                if (filtered.isNotEmpty()) {
                    listOf(AccountPickerSection("TARJETAS DE CRÉDITO", filtered))
                } else emptyList()
            }

            AccountPickerTab.LOANS -> buildList {
                val bank = loanAccounts.filter { it.accountType == AccountType.BANK_LOAN }.filterQuery()
                val lent = loanAccounts.filter { it.accountType == AccountType.LENT_LOAN }.filterQuery()
                if (bank.isNotEmpty()) add(AccountPickerSection("PRÉSTAMOS BANCARIOS", bank))
                if (lent.isNotEmpty()) add(AccountPickerSection("DINERO PRESTADO", lent))
            }
        }
    }

    GestureSafeModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Top bar with optional search toggle and circular close button (matching Neko reference)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (accounts.size > 6) {
                    IconButton(
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) searchQuery = ""
                        },
                        modifier = Modifier.testTag("account_picker_search_toggle"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Buscar cuenta",
                            tint = if (searchOpen) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("account_picker_close"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar selector",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Tabs row ("Cuentas" | "Tarjetas de crédito" | "Préstamos") with clean underline indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                availableTabs.forEach { tab ->
                    val isSelected = activeTab == tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .clickable {
                                activeTab = tab
                                searchQuery = ""
                            }
                            .padding(top = 6.dp)
                            .testTag("account_picker_tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = tab.labelEs,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                        )
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 54.dp else 0.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                ),
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Expandable quick search bar
            if (searchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre o banco...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda", modifier = Modifier.size(18.dp))
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("account_picker_search_input"),
                )
            }

            // Grouped Accounts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 420.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
            ) {
                if (allowNone) {
                    item(key = "none_option") {
                        val isNoneSelected = selectedAccountId == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(null) }
                                .background(
                                    if (isNoneSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .testTag("account_picker_item_none"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = noneLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = noneSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isNoneSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }

                if (sections.isEmpty()) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No se encontraron cuentas"
                                else "Sin cuentas en esta sección",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Prueba con otro nombre o limpia el filtro."
                                else "Puedes agregar una nueva cuenta con el botón inferior.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    sections.forEach { section ->
                        item(key = "header_${section.headerEs}") {
                            Text(
                                text = section.headerEs,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.1.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 12.dp,
                                    bottom = 6.dp,
                                ),
                            )
                        }
                        itemsIndexed(
                            items = section.accounts,
                            key = { _, acc -> "acc_${acc.id}" },
                        ) { index, account ->
                            val isSelected = account.id == selectedAccountId
                            val iconTint = when (account.accountType) {
                                AccountType.CREDIT -> financeColors.transfer
                                AccountType.BANK_LOAN -> financeColors.expense
                                AccountType.LENT_LOAN -> financeColors.income
                                AccountType.SAVINGS -> financeColors.income
                                AccountType.CASH -> financeColors.warning
                                AccountType.DEBIT -> MaterialTheme.colorScheme.primary
                            }
                            val iconContainer = when (account.accountType) {
                                AccountType.CREDIT -> financeColors.transferContainer
                                AccountType.BANK_LOAN -> financeColors.expenseContainer
                                AccountType.LENT_LOAN -> financeColors.incomeContainer
                                AccountType.SAVINGS -> financeColors.incomeContainer
                                AccountType.CASH -> financeColors.warningContainer
                                AccountType.DEBIT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            }
                            val balanceLine = when (account.accountType) {
                                AccountType.CREDIT -> {
                                    val debt = FinanceFormatters.currency(account.balance, hideBalances)
                                    if (account.creditLimit > 0.0) {
                                        val avail = FinanceFormatters.currency(
                                            max(0.0, account.creditLimit - account.balance),
                                            hideBalances,
                                        )
                                        "$debt  ·  Disp. $avail"
                                    } else debt
                                }
                                else -> FinanceFormatters.currency(account.balance, hideBalances)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(account) }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .testTag("account_picker_item_${account.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(iconContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = accountTypeIcon(account.accountType),
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = balanceLine,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                                        color = if (account.balance < 0.0) financeColors.expense
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Cuenta seleccionada",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            if (index < section.accounts.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Pinned bottom "Agregar Cuenta" CTA button (matching reference screenshot)
            if (onAddAccount != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    OutlinedButton(
                        onClick = { onAddAccount(activeTab.defaultCreateType) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("account_picker_add_account_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Agregar Cuenta",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
