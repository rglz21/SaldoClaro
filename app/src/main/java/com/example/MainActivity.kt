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
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScheduledScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.sheets.AddAccountSheet
import com.example.ui.sheets.AddBudgetSheet
import com.example.ui.sheets.AddScheduledSheet
import com.example.ui.sheets.AddTransactionSheet
import com.example.ui.sheets.MovementSheetMode
import com.example.ui.theme.SaldoClaroTheme
import com.example.ui.viewmodel.FinanceViewModel

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
)

private data class AccountSheetRequest(
    val editing: AccountEntity? = null,
    val initialType: AccountType = AccountType.DEBIT,
)

private data class ScheduledSheetRequest(
    val editing: ScheduledPaymentEntity? = null,
    val initialKind: RecurringKind = RecurringKind.SUBSCRIPTION,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaldoClaroTheme {
                SaldoClaroApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaldoClaroApp(
    viewModel: FinanceViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var movementSheet by remember { mutableStateOf<MovementSheetRequest?>(null) }
    var accountSheet by remember { mutableStateOf<AccountSheetRequest?>(null) }
    var scheduledSheet by remember { mutableStateOf<ScheduledSheetRequest?>(null) }
    var showBudgetSheet by remember { mutableStateOf(false) }

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
                    onClick = { showBudgetSheet = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Presupuesto") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_budget"),
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
            ROUTE_TRANSACTIONS -> "Movimientos"
            ROUTE_SCHEDULED -> "Suscripciones y Pagos"
            ROUTE_ANALYTICS -> "Análisis y Presupuestos"
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
                                onNavigateToTransactions = { navigateToTab(ROUTE_TRANSACTIONS) },
                                onNavigateToScheduled = { navigateToTab(ROUTE_SCHEDULED) },
                                onPayScheduledNow = viewModel::executeScheduledPaymentNow,
                            )
                        }
                        composable(ROUTE_ACCOUNTS) {
                            AccountsScreen(
                                state = state,
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
                                onOpenAddMovement = { mode -> movementSheet = MovementSheetRequest(mode) },
                                onDeleteTransaction = viewModel::deleteTransaction,
                            )
                        }
                        composable(ROUTE_SCHEDULED) {
                            ScheduledScreen(
                                state = state,
                                onAddScheduled = { kind -> scheduledSheet = ScheduledSheetRequest(initialKind = kind) },
                                onEditScheduled = { item -> scheduledSheet = ScheduledSheetRequest(editing = item) },
                                onToggleScheduled = viewModel::toggleScheduledPayment,
                                onDeleteScheduled = viewModel::deleteScheduledPayment,
                                onPayNow = viewModel::executeScheduledPaymentNow,
                            )
                        }
                        composable(ROUTE_ANALYTICS) {
                            AnalyticsScreen(
                                state = state,
                                onSelectPeriod = viewModel::selectAnalyticsPeriod,
                                onSelectGranularity = viewModel::selectAnalyticsGranularity,
                                onOpenAddBudget = { showBudgetSheet = true },
                                onDeleteBudget = viewModel::deleteBudget,
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

    // Modal Bottom Sheets
    accountSheet?.let { req ->
        AddAccountSheet(
            editingAccount = req.editing,
            initialType = req.initialType,
            onDismiss = { accountSheet = null },
            onSave = { account ->
                viewModel.saveAccount(account)
                accountSheet = null
            },
        )
    }

    movementSheet?.let { req ->
        AddTransactionSheet(
            accounts = state.accounts,
            initialMode = req.mode,
            preselectedAccountId = req.sourceAccountId,
            preselectedDestinationId = req.destinationAccountId,
            onDismiss = { movementSheet = null },
            onCreateAccountFirst = {
                accountSheet = AccountSheetRequest(
                    initialType = if (req.mode == MovementSheetMode.MSI) AccountType.CREDIT else AccountType.DEBIT
                )
            },
            onRecordIncomeOrExpense = { title, amount, type, category, accountId, dateMillis, note ->
                viewModel.recordIncomeOrExpense(title, amount, type, category, accountId, dateMillis, note)
                movementSheet = null
            },
            onRecordTransfer = { title, amount, fromId, toId, dateMillis, note ->
                viewModel.recordTransfer(title, amount, fromId, toId, dateMillis, note)
                movementSheet = null
            },
            onRecordInstallment = { cardId, desc, totalAmt, months, cat, dateMillis, note ->
                viewModel.recordInstallmentPurchase(cardId, desc, totalAmt, months, cat, dateMillis, note)
                movementSheet = null
            },
        )
    }

    scheduledSheet?.let { req ->
        AddScheduledSheet(
            accounts = state.accounts,
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

    if (showBudgetSheet) {
        AddBudgetSheet(
            onDismiss = { showBudgetSheet = false },
            onSave = { category, limit ->
                viewModel.saveBudget(category, limit)
                showBudgetSheet = false
            },
        )
    }
}
