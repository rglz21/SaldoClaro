package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryEntity
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.notifications.PaymentNotificationManager
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AnalyticsTabMode
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScheduledScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.TransactionsViewMode
import com.example.ui.sheets.AccountDetailSheet
import com.example.ui.sheets.AddAccountSheet
import com.example.ui.sheets.AddBudgetSheet
import com.example.ui.sheets.AddCategorySheet
import com.example.ui.sheets.AddScheduledSheet
import com.example.ui.sheets.AddTransactionSheet
import com.example.ui.sheets.AnalyticsCategoryDetailSheet
import com.example.ui.sheets.AnalyticsKpiDetailSheet
import com.example.ui.sheets.AnalyticsKpiKind
import com.example.ui.sheets.MovementSheetMode
import com.example.ui.sheets.NotificationSettingsSheet
import com.example.ui.sheets.ScheduledDetailSheet
import com.example.ui.sheets.TransactionDetailSheet
import com.example.ui.theme.SaldoClaroTheme
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_ACCOUNTS = "accounts"
const val ROUTE_TRANSACTIONS = "transactions"
const val ROUTE_SCHEDULED = "scheduled"
const val ROUTE_ANALYTICS = "analytics"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TopDestinations = listOf(
    NavDestination(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    NavDestination(ROUTE_ACCOUNTS, R.string.nav_accounts, Icons.Rounded.AccountBalance, Icons.Outlined.AccountBalance),
    NavDestination(ROUTE_TRANSACTIONS, R.string.nav_transactions, Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong),
    NavDestination(ROUTE_SCHEDULED, R.string.nav_scheduled, Icons.Rounded.EventRepeat, Icons.Outlined.EventRepeat),
    NavDestination(ROUTE_ANALYTICS, R.string.nav_analytics, Icons.Rounded.Analytics, Icons.Outlined.Analytics),
)

private data class MovementSheetRequest(
    val mode: MovementSheetMode = MovementSheetMode.EXPENSE,
    val sourceAccountId: Long? = null,
    val destinationAccountId: Long? = null,
    val initialDateMillis: Long? = null,
)

private data class AccountSheetRequest(
    val editing: AccountEntity? = null,
    val initialType: AccountType = AccountType.DEBIT,
)

private data class ScheduledSheetRequest(
    val editing: ScheduledPaymentEntity? = null,
    val initialKind: RecurringKind = RecurringKind.SUBSCRIPTION,
)

private data class CategorySheetRequest(
    val editing: CategoryEntity? = null,
)

private data class BudgetSheetRequest(
    val preselectedCategory: CategoryEntity? = null,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openCalendar = intent?.getBooleanExtra(PaymentNotificationManager.EXTRA_OPEN_CALENDAR, false) == true
        setContent {
            SaldoClaroTheme {
                SaldoClaroApp(openCalendarOnStart = openCalendar)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaldoClaroApp(
    openCalendarOnStart: Boolean = false,
    viewModel: FinanceViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val appScope = androidx.compose.runtime.rememberCoroutineScope()

    // Create / Edit Modal Requests
    var movementSheet by remember { mutableStateOf<MovementSheetRequest?>(null) }
    var accountSheet by remember { mutableStateOf<AccountSheetRequest?>(null) }
    var scheduledSheet by remember { mutableStateOf<ScheduledSheetRequest?>(null) }
    var categorySheet by remember { mutableStateOf<CategorySheetRequest?>(null) }
    var budgetSheet by remember { mutableStateOf<BudgetSheetRequest?>(null) }
    var showNotificationSettings by remember { mutableStateOf(false) }

    // Sub-view modes
    var transactionsViewMode by remember { mutableStateOf(TransactionsViewMode.CALENDAR) }
    var analyticsTabMode by remember { mutableStateOf(AnalyticsTabMode.ANALYTICS) }

    // Drill-down Detail Sheets
    var detailAccountId by remember { mutableStateOf<Long?>(null) }
    var detailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var detailScheduledId by remember { mutableStateOf<Long?>(null) }
    var detailKpiKind by remember { mutableStateOf<AnalyticsKpiKind?>(null) }
    var detailCategoryName by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTxFromDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDeleteScheduledFromDetail by remember { mutableStateOf<ScheduledPaymentEntity?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            PaymentNotificationManager.ensureChannel(context)
            val prefs = PaymentNotificationManager.getPreferences(context)
            if (prefs.enabled && PaymentNotificationManager.hasNotificationPermission(context)) {
                PaymentNotificationManager.scheduleDailyReminder(context)
                PaymentNotificationManager.checkAndNotifyDueToday(context)
            }
        }
        if (openCalendarOnStart) {
            transactionsViewMode = TransactionsViewMode.CALENDAR
            navController.navigate(ROUTE_TRANSACTIONS) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (pendingDeleteTxFromDetail != null) {
        val tx = pendingDeleteTxFromDetail!!
        AlertDialog(
            onDismissRequest = { pendingDeleteTxFromDetail = null },
            title = { Text("Eliminar movimiento") },
            text = { Text("Se eliminará \"${tx.title}\" y se revertirá el saldo en las cuentas afectadas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(tx.id)
                    pendingDeleteTxFromDetail = null
                    detailTransaction = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTxFromDetail = null }) { Text("Cancelar") }
            },
        )
    }

    if (pendingDeleteScheduledFromDetail != null) {
        val item = pendingDeleteScheduledFromDetail!!
        AlertDialog(
            onDismissRequest = { pendingDeleteScheduledFromDetail = null },
            title = { Text("Eliminar ${item.name}") },
            text = { Text("Se eliminará este registro programado. Los movimientos ya registrados se conservarán.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteScheduledPayment(item)
                    pendingDeleteScheduledFromDetail = null
                    detailScheduledId = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteScheduledFromDetail = null }) { Text("Cancelar") }
            },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 680.dp

        val fabContent: @Composable () -> Unit = {
            when (currentRoute) {
                ROUTE_ACCOUNTS -> ExtendedFloatingActionButton(
                    onClick = { accountSheet = AccountSheetRequest() },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Cuenta") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_account"),
                )
                ROUTE_SCHEDULED -> ExtendedFloatingActionButton(
                    onClick = { scheduledSheet = ScheduledSheetRequest() },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Programar") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_scheduled"),
                )
                ROUTE_ANALYTICS -> ExtendedFloatingActionButton(
                    onClick = { categorySheet = CategorySheetRequest() },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Categoría") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_category"),
                )
                else -> FloatingActionButton(
                    onClick = { movementSheet = MovementSheetRequest(MovementSheetMode.EXPENSE) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_movement"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.cd_add_movement),
                    )
                }
            }
        }

        val currentTitle = when (currentRoute) {
            ROUTE_DASHBOARD -> stringResource(R.string.app_name)
            ROUTE_ACCOUNTS -> "Cuentas, Tarjetas y Préstamos"
            ROUTE_TRANSACTIONS -> "Movimientos y Calendario"
            ROUTE_SCHEDULED -> "Pagos e Ingresos Frecuentes"
            ROUTE_ANALYTICS -> if (analyticsTabMode == AnalyticsTabMode.CATEGORIES) "Categorías y Presupuestos"
            else "Análisis y Categorías"
            else -> stringResource(R.string.app_name)
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    TopDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        val label = stringResource(dest.labelRes)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = label,
                                )
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("nav_rail_${dest.route}"),
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showNotificationSettings = true },
                                modifier = Modifier.testTag("topbar_notifications_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = "Recordatorios de pago",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (currentRoute != ROUTE_ANALYTICS || analyticsTabMode != AnalyticsTabMode.CATEGORIES) {
                                IconButton(
                                    onClick = {
                                        analyticsTabMode = AnalyticsTabMode.CATEGORIES
                                        navigateToTab(ROUTE_ANALYTICS)
                                    },
                                    modifier = Modifier.testTag("topbar_categories_shortcut"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Category,
                                        contentDescription = "Categorías y presupuestos",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (currentRoute != ROUTE_TRANSACTIONS) {
                                IconButton(
                                    onClick = {
                                        transactionsViewMode = TransactionsViewMode.CALENDAR
                                        navigateToTab(ROUTE_TRANSACTIONS)
                                    },
                                    modifier = Modifier.testTag("topbar_calendar_shortcut"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "Abrir calendario de flujo diario",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = fabContent,
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                        ) {
                            TopDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                val label = stringResource(dest.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_bar_${dest.route}"),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                state = state,
                                onTogglePrivacy = viewModel::togglePrivacy,
                                onOpenMovementSheet = { mode, src, dst ->
                                    movementSheet = MovementSheetRequest(mode, src, dst)
                                },
                                onOpenAddAccount = { accountSheet = AccountSheetRequest() },
                                onNavigateToAccounts = { navigateToTab(ROUTE_ACCOUNTS) },
                                onNavigateToTransactions = {
                                    transactionsViewMode = TransactionsViewMode.LIST
                                    navigateToTab(ROUTE_TRANSACTIONS)
                                },
                                onNavigateToCalendar = {
                                    transactionsViewMode = TransactionsViewMode.CALENDAR
                                    navigateToTab(ROUTE_TRANSACTIONS)
                                },
                                onNavigateToScheduled = { navigateToTab(ROUTE_SCHEDULED) },
                                onPayScheduledNow = viewModel::executeScheduledPaymentNow,
                                onAccountClick = { acc -> detailAccountId = acc.id },
                                onScheduledClick = { item -> detailScheduledId = item.id },
                                onTransactionClick = { tx -> detailTransaction = tx },
                            )
                        }
                        composable(ROUTE_ACCOUNTS) {
                            AccountsScreen(
                                state = state,
                                onAccountClick = { acc -> detailAccountId = acc.id },
                                onAddAccount = { type -> accountSheet = AccountSheetRequest(initialType = type) },
                                onEditAccount = { acc -> accountSheet = AccountSheetRequest(editing = acc) },
                                onDeleteAccount = viewModel::deleteAccount,
                                onOpenMovementSheet = { mode, src, dst ->
                                    movementSheet = MovementSheetRequest(mode, src, dst)
                                },
                                onPayInstallmentMonth = viewModel::payInstallmentMonth,
                            )
                        }
                        composable(ROUTE_TRANSACTIONS) {
                            TransactionsScreen(
                                state = state,
                                viewMode = transactionsViewMode,
                                onViewModeChange = { transactionsViewMode = it },
                                onTransactionClick = { tx -> detailTransaction = tx },
                                onOpenAddMovement = { mode, dateMillis ->
                                    movementSheet = MovementSheetRequest(mode = mode, initialDateMillis = dateMillis)
                                },
                                onDeleteTransaction = viewModel::deleteTransaction,
                                onScheduledClick = { item -> detailScheduledId = item.id },
                                onExecuteScheduledNow = viewModel::executeScheduledPaymentNow,
                                onAccountClick = { acc -> detailAccountId = acc.id },
                            )
                        }
                        composable(ROUTE_SCHEDULED) {
                            ScheduledScreen(
                                state = state,
                                onAddScheduled = { kind -> scheduledSheet = ScheduledSheetRequest(initialKind = kind) },
                                onScheduledClick = { item -> detailScheduledId = item.id },
                                onEditScheduled = { item -> scheduledSheet = ScheduledSheetRequest(editing = item) },
                                onToggleScheduled = viewModel::toggleScheduledPayment,
                                onDeleteScheduled = viewModel::deleteScheduledPayment,
                                onPayNow = viewModel::executeScheduledPaymentNow,
                                onOpenNotifications = { showNotificationSettings = true },
                            )
                        }
                        composable(ROUTE_ANALYTICS) {
                            AnalyticsScreen(
                                state = state,
                                tabMode = analyticsTabMode,
                                onTabModeChange = { analyticsTabMode = it },
                                onSelectPeriod = viewModel::selectAnalyticsPeriod,
                                onSelectGranularity = viewModel::selectAnalyticsGranularity,
                                onKpiClick = { kind -> detailKpiKind = kind },
                                onCategoryDetailClick = { name -> detailCategoryName = name },
                                onOpenAddCategory = { categorySheet = CategorySheetRequest() },
                                onEditCategory = { cat -> categorySheet = CategorySheetRequest(editing = cat) },
                                onDeleteCategory = viewModel::deleteCategory,
                                onOpenAddBudget = { cat -> budgetSheet = BudgetSheetRequest(preselectedCategory = cat) },
                                onRemoveBudget = { cat -> viewModel.updateCategoryBudget(cat, 0.0) },
                                onExportCsv = {
                                    val csv = viewModel.buildCsvExport()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_SUBJECT, "Reporte financiero SaldoClaro")
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Exportar movimientos CSV"))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Modal Sheets
    detailAccountId?.let { accId ->
        val account = state.accounts.firstOrNull { it.id == accId }
        if (account != null) {
            val ccSummary = state.creditCardSummaries.firstOrNull { it.card.id == account.id }
            val liquidDefault = state.accounts.firstOrNull { it.accountType.isLiquidAsset }?.id
            AccountDetailSheet(
                account = account,
                creditCardSummary = ccSummary,
                allAccounts = state.accounts,
                transactions = state.transactions,
                categories = state.categories,
                hideBalances = state.hideBalances,
                onDismiss = { detailAccountId = null },
                onEdit = {
                    detailAccountId = null
                    accountSheet = AccountSheetRequest(editing = account)
                },
                onTransferOrPay = {
                    detailAccountId = null
                    when (account.accountType) {
                        AccountType.CREDIT, AccountType.BANK_LOAN -> {
                            val payer = account.linkedAccountId?.takeIf { id -> state.accounts.any { it.id == id } } ?: liquidDefault
                            movementSheet = MovementSheetRequest(MovementSheetMode.TRANSFER, payer, account.id)
                        }
                        AccountType.LENT_LOAN -> {
                            val dst = account.linkedAccountId?.takeIf { id -> state.accounts.any { it.id == id } } ?: liquidDefault
                            movementSheet = MovementSheetRequest(MovementSheetMode.TRANSFER, account.id, dst)
                        }
                        else -> {
                            movementSheet = MovementSheetRequest(MovementSheetMode.TRANSFER, account.id, null)
                        }
                    }
                },
                onAddMovement = {
                    detailAccountId = null
                    movementSheet = MovementSheetRequest(MovementSheetMode.EXPENSE, account.id, null)
                },
                onTransactionClick = { tx ->
                    detailTransaction = tx
                },
            )
        } else {
            detailAccountId = null
        }
    }

    detailScheduledId?.let { schedId ->
        val item = state.scheduledPayments.firstOrNull { it.id == schedId }
        if (item != null) {
            ScheduledDetailSheet(
                item = item,
                accounts = state.accounts,
                transactions = state.transactions,
                hideBalances = state.hideBalances,
                onDismiss = { detailScheduledId = null },
                onExecuteNow = {
                    viewModel.executeScheduledPaymentNow(item)
                    detailScheduledId = null
                },
                onEdit = {
                    detailScheduledId = null
                    scheduledSheet = ScheduledSheetRequest(editing = item)
                },
                onDelete = {
                    pendingDeleteScheduledFromDetail = item
                },
            )
        } else {
            detailScheduledId = null
        }
    }

    detailKpiKind?.let { kind ->
        AnalyticsKpiDetailSheet(
            kind = kind,
            state = state,
            onDismiss = { detailKpiKind = null },
        )
    }

    detailCategoryName?.let { catName ->
        AnalyticsCategoryDetailSheet(
            categoryName = catName,
            state = state,
            onDismiss = { detailCategoryName = null },
            onEditCategoryBudget = { cat ->
                detailCategoryName = null
                budgetSheet = BudgetSheetRequest(preselectedCategory = cat)
            },
            onTransactionClick = { tx ->
                detailTransaction = tx
            },
        )
    }

    detailTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            accounts = state.accounts,
            categories = state.categories,
            installmentPlans = state.installmentPlans,
            hideBalances = state.hideBalances,
            onDismiss = { detailTransaction = null },
            onDelete = { target -> pendingDeleteTxFromDetail = target },
        )
    }

    // Create / Edit Modal Bottom Sheets
    accountSheet?.let { req ->
        AddAccountSheet(
            existingAccounts = state.accounts,
            editingAccount = req.editing,
            initialType = req.initialType,
            onDismiss = { accountSheet = null },
            onSave = { account, applyDisbursement ->
                viewModel.saveAccount(account, applyDisbursement)
                accountSheet = null
            },
        )
    }

    movementSheet?.let { req ->
        AddTransactionSheet(
            accounts = state.accounts,
            categories = state.categories,
            initialMode = req.mode,
            preselectedAccountId = req.sourceAccountId,
            preselectedDestinationId = req.destinationAccountId,
            initialDateMillis = req.initialDateMillis,
            onDismiss = { movementSheet = null },
            onCreateAccountFirst = {
                accountSheet = AccountSheetRequest(
                    initialType = if (req.mode == MovementSheetMode.MSI) AccountType.CREDIT else AccountType.DEBIT
                )
            },
            onRecordIncomeOrExpense = { title, amount, type, categoryName, accountId, dateMillis, note ->
                viewModel.recordIncomeOrExpense(title, amount, type, categoryName, accountId, dateMillis, note)
                movementSheet = null
            },
            onRecordTransfer = { title, amount, fromId, toId, dateMillis, note ->
                viewModel.recordTransfer(title, amount, fromId, toId, dateMillis, note)
                movementSheet = null
            },
            onRecordInstallment = { cardId, desc, totalAmt, months, catName, dateMillis, note ->
                viewModel.recordInstallmentPurchase(cardId, desc, totalAmt, months, catName, dateMillis, note)
                movementSheet = null
            },
        )
    }

    scheduledSheet?.let { req ->
        AddScheduledSheet(
            accounts = state.accounts,
            categories = state.categories,
            editingPayment = req.editing,
            initialKind = req.initialKind,
            onDismiss = { scheduledSheet = null },
            onCreateAccountFirst = { accountSheet = AccountSheetRequest(initialType = AccountType.DEBIT) },
            onSave = { payment ->
                viewModel.saveScheduledPayment(payment)
                scheduledSheet = null
            },
        )
    }

    categorySheet?.let { req ->
        AddCategorySheet(
            editingCategory = req.editing,
            onDismiss = { categorySheet = null },
            onSave = { category ->
                viewModel.saveCategory(category)
                categorySheet = null
            },
        )
    }

    budgetSheet?.let { req ->
        AddBudgetSheet(
            categories = state.categories,
            preselectedCategory = req.preselectedCategory,
            onDismiss = { budgetSheet = null },
            onSave = { category, limit ->
                viewModel.updateCategoryBudget(category, limit)
                budgetSheet = null
            },
        )
    }

    if (showNotificationSettings) {
        NotificationSettingsSheet(
            accounts = state.accounts,
            scheduledPayments = state.scheduledPayments,
            hideBalances = state.hideBalances,
            onDismiss = { showNotificationSettings = false },
            onStatusMessage = { msg ->
                appScope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }
}
