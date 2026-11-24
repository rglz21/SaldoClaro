package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.FinanceBackupManager
import com.example.data.backup.FinanceBackupSnapshot
import com.example.data.local.FinanceDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AnalyticsGranularity
import com.example.data.model.AnalyticsPeriod
import com.example.data.model.CategoryEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.notifications.PaymentNotificationManager
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import com.example.util.NextDateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

data class CreditCardSummary(
    val card: AccountEntity,
    val nextCutoff: NextDateInfo,
    val nextPaymentDue: NextDateInfo,
    val activeInstallments: List<InstallmentPlanEntity>,
    val monthlyMsiAmount: Double,
    val totalMsiRemaining: Double,
    val revolvingBalance: Double,
    val availableCredit: Double,
    val utilizationRatio: Double,
)

data class ChartBarPoint(
    val label: String,
    val fullLabel: String,
    val expense: Double,
    val income: Double,
)

data class CategorySlice(
    val categoryName: String,
    val colorIndex: Int?,
    val amount: Double,
    val ratio: Double,
    val count: Int,
)

data class BudgetProgress(
    val category: CategoryEntity,
    val spentThisMonth: Double,
    val ratio: Double,
)

data class AnalyticsSummary(
    val period: AnalyticsPeriod,
    val granularity: AnalyticsGranularity,
    val availableGranularities: List<AnalyticsGranularity>,
    val periodSubtitle: String,
    val periodNavLabel: String = "",
    val isCurrentPeriod: Boolean = true,
    val periodStartMillis: Long = 0L,
    val periodEndMillis: Long = Long.MAX_VALUE,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val totalExpense: Double,
    val totalIncome: Double,
    val netFlow: Double,
    val savingsRate: Double,
    val averageDailyExpense: Double,
    val projectedMonthExpense: Double,
    val topCategory: CategorySlice?,
    val barPoints: List<ChartBarPoint>,
    val categoryBreakdown: List<CategorySlice>,
    val budgetProgress: List<BudgetProgress>,
    val creditUtilizationRatio: Double,
    val fixedMonthlyCommitment: Double,
)

data class FinanceUiState(
    val isLoading: Boolean = true,
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    val scheduledPayments: List<ScheduledPaymentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val hideBalances: Boolean = false,
    val liquidTotal: Double = 0.0,
    val creditDebtTotal: Double = 0.0,
    val creditLimitTotal: Double = 0.0,
    val bankLoansTotal: Double = 0.0,
    val lentLoansTotal: Double = 0.0,
    val netWorth: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthlySubscriptionsTotal: Double = 0.0,
    val monthlyRecurringBillsTotal: Double = 0.0,
    val monthlyRecurringIncomeTotal: Double = 0.0,
    val monthlyMsiTotal: Double = 0.0,
    val monthlyLoanTotal: Double = 0.0,
    val creditCardSummaries: List<CreditCardSummary> = emptyList(),
    val analytics: AnalyticsSummary = AnalyticsSummary(
        period = AnalyticsPeriod.MONTH,
        granularity = AnalyticsGranularity.WEEK,
        availableGranularities = listOf(AnalyticsGranularity.DAY, AnalyticsGranularity.WEEK),
        periodSubtitle = "",
        totalExpense = 0.0,
        totalIncome = 0.0,
        netFlow = 0.0,
        savingsRate = 0.0,
        averageDailyExpense = 0.0,
        projectedMonthExpense = 0.0,
        topCategory = null,
        barPoints = emptyList(),
        categoryBreakdown = emptyList(),
        budgetProgress = emptyList(),
        creditUtilizationRatio = 0.0,
        fixedMonthlyCommitment = 0.0,
    ),
    val snackbarMessage: String? = null,
)

private data class UiPrefsState(
    val hideBalances: Boolean = false,
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    val granularity: AnalyticsGranularity = AnalyticsGranularity.WEEK,
    val anchorYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val anchorMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val weekOffset: Int = 0,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val snackbarMessage: String? = null,
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanceRepository(FinanceDatabase.getInstance(application))
    private val uiPrefs = MutableStateFlow(UiPrefsState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.ensureSeeded(getApplication())
            }
        }
    }

    private val coreDataFlow = combine(
        repository.accounts,
        repository.transactions,
        repository.installmentPlans,
        repository.scheduledPayments,
        repository.categories,
    ) { accounts, transactions, plans, scheduled, categories ->
        CoreData(accounts, transactions, plans, scheduled, categories)
    }

    val uiState: StateFlow<FinanceUiState> = combine(coreDataFlow, uiPrefs) { data, prefs ->
        buildUiState(data, prefs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState(isLoading = true),
    )

    private data class CoreData(
        val accounts: List<AccountEntity>,
        val transactions: List<TransactionEntity>,
        val plans: List<InstallmentPlanEntity>,
        val scheduled: List<ScheduledPaymentEntity>,
        val categories: List<CategoryEntity>,
    )

    fun togglePrivacy() {
        uiPrefs.value = uiPrefs.value.copy(hideBalances = !uiPrefs.value.hideBalances)
    }

    fun clearSnackbar() {
        uiPrefs.value = uiPrefs.value.copy(snackbarMessage = null)
    }

    private fun postMessage(msg: String) {
        uiPrefs.value = uiPrefs.value.copy(snackbarMessage = msg)
    }

    fun selectAnalyticsPeriod(period: AnalyticsPeriod) {
        val current = uiPrefs.value
        val defaultGranularity = when (period) {
            AnalyticsPeriod.WEEK -> AnalyticsGranularity.DAY
            AnalyticsPeriod.MONTH -> AnalyticsGranularity.WEEK
            AnalyticsPeriod.YEAR -> AnalyticsGranularity.MONTH
            AnalyticsPeriod.CUSTOM -> AnalyticsGranularity.DAY
        }
        val todayStart = DateMath.startOfDay()
        val defaultStart = current.customStartMillis ?: (todayStart - 29L * 86_400_000L)
        val defaultEnd = current.customEndMillis ?: todayStart
        uiPrefs.value = current.copy(
            period = period,
            granularity = defaultGranularity,
            customStartMillis = if (period == AnalyticsPeriod.CUSTOM) defaultStart else current.customStartMillis,
            customEndMillis = if (period == AnalyticsPeriod.CUSTOM) defaultEnd else current.customEndMillis,
        )
    }

    fun selectAnalyticsGranularity(granularity: AnalyticsGranularity) {
        uiPrefs.value = uiPrefs.value.copy(granularity = granularity)
    }

    fun shiftAnalyticsPeriod(delta: Int) {
        val current = uiPrefs.value
        when (current.period) {
            AnalyticsPeriod.WEEK -> {
                uiPrefs.value = current.copy(weekOffset = current.weekOffset + delta)
            }
            AnalyticsPeriod.MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, current.anchorYear)
                    set(Calendar.MONTH, current.anchorMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, delta)
                }
                uiPrefs.value = current.copy(
                    anchorYear = cal.get(Calendar.YEAR),
                    anchorMonth = cal.get(Calendar.MONTH),
                )
            }
            AnalyticsPeriod.YEAR -> {
                uiPrefs.value = current.copy(anchorYear = (current.anchorYear + delta).coerceIn(2000, 2100))
            }
            AnalyticsPeriod.CUSTOM -> {
                val start = current.customStartMillis ?: (DateMath.startOfDay() - 29L * 86_400_000L)
                val end = current.customEndMillis ?: DateMath.startOfDay()
                val spanDays = max(1, DateMath.daysBetween(start, end) + 1)
                val shiftMillis = delta.toLong() * spanDays.toLong() * 86_400_000L
                uiPrefs.value = current.copy(
                    customStartMillis = DateMath.startOfDay(start + shiftMillis),
                    customEndMillis = DateMath.startOfDay(end + shiftMillis),
                )
            }
        }
    }

    fun selectAnalyticsMonth(year: Int, monthZeroBased: Int) {
        uiPrefs.value = uiPrefs.value.copy(
            period = AnalyticsPeriod.MONTH,
            anchorYear = year,
            anchorMonth = monthZeroBased.coerceIn(0, 11),
        )
    }

    fun resetAnalyticsToCurrent() {
        val now = Calendar.getInstance()
        val todayStart = DateMath.startOfDay()
        uiPrefs.value = uiPrefs.value.copy(
            anchorYear = now.get(Calendar.YEAR),
            anchorMonth = now.get(Calendar.MONTH),
            weekOffset = 0,
            customStartMillis = todayStart - 29L * 86_400_000L,
            customEndMillis = todayStart,
        )
    }

    fun setAnalyticsCustomRange(startMillis: Long, endMillisInclusive: Long) {
        val cleanStart = DateMath.startOfDay(minOf(startMillis, endMillisInclusive))
        val cleanEnd = DateMath.startOfDay(maxOf(startMillis, endMillisInclusive))
        val spanDays = max(1, DateMath.daysBetween(cleanStart, cleanEnd) + 1)
        val defaultGran = when {
            spanDays <= 16 -> AnalyticsGranularity.DAY
            spanDays <= 65 -> AnalyticsGranularity.WEEK
            else -> AnalyticsGranularity.MONTH
        }
        uiPrefs.value = uiPrefs.value.copy(
            period = AnalyticsPeriod.CUSTOM,
            granularity = defaultGran,
            customStartMillis = cleanStart,
            customEndMillis = cleanEnd,
        )
    }

    // --- Account Actions ---

    fun saveAccount(account: AccountEntity, applyDisbursementNow: Boolean = false) {
        viewModelScope.launch {
            runCatching {
                val isNewLoan = account.id == 0L &&
                    (account.accountType == AccountType.BANK_LOAN || account.accountType == AccountType.LENT_LOAN)
                when {
                    isNewLoan -> repository.saveLoanWithDisbursement(
                        loan = account,
                        applyDisbursementNow = applyDisbursementNow,
                    )
                    account.id == 0L -> repository.saveAccount(account)
                    else -> repository.updateAccount(account)
                }
            }.onSuccess {
                val msg = when {
                    account.id != 0L -> "Cuenta \"${account.name}\" actualizada (${account.accountType.labelEs})"
                    applyDisbursementNow && account.linkedAccountId != null &&
                        account.accountType == AccountType.BANK_LOAN ->
                        "Préstamo guardado y desembolso depositado en tu cuenta"
                    applyDisbursementNow && account.linkedAccountId != null &&
                        account.accountType == AccountType.LENT_LOAN ->
                        "Préstamo registrado y desembolso descontado de tu cuenta"
                    else -> "Cuenta \"${account.name}\" agregada"
                }
                postMessage(msg)
            }.onFailure {
                postMessage(it.message ?: "No se pudo guardar la cuenta")
            }
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            runCatching {
                repository.deleteAccount(account.id)
            }.onSuccess {
                postMessage("Se eliminó \"${account.name}\"")
            }.onFailure {
                postMessage("Error al eliminar la cuenta")
            }
        }
    }

    // --- Transaction Actions ---

    fun recordIncomeOrExpense(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryName: String,
        accountId: Long,
        dateMillis: Long,
        note: String,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.recordIncomeOrExpense(
                    title = title,
                    amount = amount,
                    type = type,
                    categoryName = categoryName,
                    accountId = accountId,
                    dateMillis = dateMillis,
                    note = note,
                )
            }.onSuccess {
                postMessage(
                    if (type == TransactionType.INCOME) "Ingreso registrado"
                    else "Gasto registrado"
                )
            }.onFailure {
                postMessage(it.message ?: "Error al registrar el movimiento")
            }
        }
    }

    fun recordTransfer(
        title: String,
        amount: Double,
        fromAccountId: Long,
        toAccountId: Long,
        dateMillis: Long,
        note: String,
        categoryOverride: String? = null,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.recordTransfer(
                    title = title,
                    amount = amount,
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    dateMillis = dateMillis,
                    note = note,
                    categoryOverride = categoryOverride,
                )
            }.onSuccess {
                postMessage("Transferencia realizada correctamente")
            }.onFailure {
                postMessage(it.message ?: "Error al realizar la transferencia")
            }
        }
    }

    fun recordInstallmentPurchase(
        creditCardId: Long,
        description: String,
        totalAmount: Double,
        totalMonths: Int,
        categoryName: String,
        startDateMillis: Long,
        note: String,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.recordInstallmentPurchase(
                    creditCardId = creditCardId,
                    description = description,
                    totalAmount = totalAmount,
                    totalMonths = totalMonths,
                    categoryName = categoryName,
                    startDateMillis = startDateMillis,
                    note = note,
                )
            }.onSuccess {
                postMessage("Compra a $totalMonths MSI registrada")
            }.onFailure {
                postMessage(it.message ?: "No se pudo registrar la compra MSI")
            }
        }
    }

    fun payInstallmentMonth(planId: Long, fromAccountId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.payInstallmentMonth(planId = planId, fromAccountId = fromAccountId)
            }.onSuccess {
                postMessage("Mensualidad MSI pagada")
            }.onFailure {
                postMessage(it.message ?: "Error al registrar el pago MSI")
            }
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            runCatching {
                repository.deleteTransactionWithRollback(transactionId)
            }.onSuccess {
                postMessage("Movimiento eliminado y saldo revertido")
            }.onFailure {
                postMessage("No se pudo eliminar el movimiento")
            }
        }
    }

    // --- Scheduled Payments & Subscriptions ---

    fun saveScheduledPayment(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            runCatching {
                repository.saveScheduledPayment(payment)
            }.onSuccess {
                val label = payment.recurringKind.labelEs
                postMessage(
                    if (payment.recurringKind.isOneTime || payment.recurrenceFrequency.isOneTime) {
                        "Pago único \"${payment.name}\" programado para el ${FinanceFormatters.shortDate(payment.nextDueDateMillis)}"
                    } else {
                        "$label \"${payment.name}\" guardado"
                    }
                )
                viewModelScope.launch(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val prefs = PaymentNotificationManager.getPreferences(app)
                    if (prefs.enabled && PaymentNotificationManager.hasNotificationPermission(app)) {
                        PaymentNotificationManager.scheduleDailyReminder(app)
                        PaymentNotificationManager.checkAndNotifyDueToday(app)
                    }
                }
            }.onFailure {
                postMessage(it.message ?: "Error al guardar el registro programado")
            }
        }
    }

    fun toggleScheduledPayment(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            runCatching {
                repository.toggleScheduledPayment(payment)
            }.onSuccess {
                val noun = if (payment.recurringKind.isIncome) "Ingreso" else "Pago"
                postMessage(if (payment.isActive) "$noun pausado" else "$noun reactivado")
            }
        }
    }

    fun deleteScheduledPayment(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            runCatching {
                repository.deleteScheduledPayment(payment)
            }.onSuccess {
                postMessage("Se eliminó \"${payment.name}\"")
            }
        }
    }

    fun executeScheduledPaymentNow(payment: ScheduledPaymentEntity) {
        viewModelScope.launch {
            runCatching {
                repository.executeScheduledPaymentNow(payment)
            }.onSuccess {
                val isOneTime = payment.recurringKind.isOneTime || payment.recurrenceFrequency.isOneTime
                val msg = when {
                    isOneTime -> "Pago único \"${payment.name}\" registrado y completado"
                    payment.recurringKind.isIncome -> "Ingreso de \"${payment.name}\" registrado y próxima fecha actualizada"
                    else -> "Pago de \"${payment.name}\" registrado y próxima fecha actualizada"
                }
                postMessage(msg)
            }.onFailure {
                postMessage(it.message ?: "No se pudo registrar el movimiento")
            }
        }
    }

    // --- Categories & Budgets ---

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            runCatching {
                repository.saveCategory(category)
            }.onSuccess {
                postMessage(
                    if (category.id == 0L) "Categoría \"${category.name}\" creada"
                    else "Categoría \"${category.name}\" actualizada"
                )
            }.onFailure {
                postMessage(it.message ?: "No se pudo guardar la categoría")
            }
        }
    }

    fun updateCategoryBudget(category: CategoryEntity, monthlyLimit: Double) {
        viewModelScope.launch {
            runCatching {
                repository.updateCategoryBudget(category.id, monthlyLimit)
            }.onSuccess {
                postMessage(
                    if (monthlyLimit > 0.0) "Presupuesto asignado a ${category.name}"
                    else "Presupuesto quitado de ${category.name}"
                )
            }.onFailure {
                postMessage(it.message ?: "Monto de presupuesto inválido")
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            runCatching {
                repository.deleteCategory(category.id)
            }.onSuccess {
                postMessage("Categoría \"${category.name}\" eliminada")
            }
        }
    }

    fun buildCsvExport(): String {
        val current = uiState.value
        return FinanceFormatters.buildCsvReport(current.accounts, current.transactions)
    }

    // --- Full Backup Export & Import ---

    suspend fun exportBackupToUri(uri: Uri): Result<FinanceBackupSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val app = getApplication<Application>()
            val snapshot = repository.createBackupSnapshot(app)
            val json = FinanceBackupManager.serializeToJson(snapshot)
            FinanceBackupManager.writeTextToUri(app, uri, json)
            postMessage("Respaldo guardado (${snapshot.accounts.size} cuentas, ${snapshot.transactions.size} movimientos)")
            snapshot
        }
    }

    suspend fun exportCsvToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val app = getApplication<Application>()
            val snapshot = repository.createBackupSnapshot(app)
            val csv = FinanceFormatters.buildCsvReport(snapshot.accounts, snapshot.transactions)
            FinanceBackupManager.writeTextToUri(app, uri, csv)
            postMessage("CSV guardado con ${snapshot.transactions.size} movimientos")
            snapshot.transactions.size
        }
    }

    suspend fun inspectBackupFromUri(uri: Uri): Result<FinanceBackupSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val app = getApplication<Application>()
            val raw = FinanceBackupManager.readTextFromUri(app, uri)
            FinanceBackupManager.parseFromJson(raw)
        }
    }

    suspend fun restoreBackup(snapshot: FinanceBackupSnapshot): Result<FinanceBackupSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val app = getApplication<Application>()
            repository.restoreBackupSnapshot(app, snapshot)
            val prefs = PaymentNotificationManager.getPreferences(app)
            if (prefs.enabled && PaymentNotificationManager.hasNotificationPermission(app)) {
                PaymentNotificationManager.scheduleDailyReminder(app)
                PaymentNotificationManager.checkAndNotifyDueToday(app)
            }
            postMessage(
                "Respaldo restaurado: ${snapshot.accounts.size} cuentas y ${snapshot.transactions.size} movimientos"
            )
            snapshot
        }
    }

    // --- Derived Analytics & Summaries ---

    private fun buildUiState(data: CoreData, prefs: UiPrefsState): FinanceUiState {
        val accounts = data.accounts
        val transactions = data.transactions
        val plans = data.plans
        val scheduled = data.scheduled
        val categories = data.categories

        val liquidTotal = accounts
            .filter { it.accountType.isLiquidAsset }
            .sumOf { it.balance }
        val creditCards = accounts.filter { it.accountType == AccountType.CREDIT }
        val creditDebtTotal = creditCards.sumOf { max(0.0, it.balance) }
        val creditLimitTotal = creditCards.sumOf { max(0.0, it.creditLimit) }
        val bankLoansTotal = accounts
            .filter { it.accountType == AccountType.BANK_LOAN }
            .sumOf { max(0.0, it.balance) }
        val lentLoansTotal = accounts
            .filter { it.accountType == AccountType.LENT_LOAN }
            .sumOf { max(0.0, it.balance) }

        val netWorth = liquidTotal + lentLoansTotal - creditDebtTotal - bankLoansTotal

        val monthStart = DateMath.startOfCurrentMonth()
        val accountTypeMap = accounts.associate { it.id to it.accountType }
        val monthTxs = transactions.filter { it.dateMillis >= monthStart }
        val monthIncome = monthTxs
            .filter { tx ->
                tx.transactionType == TransactionType.INCOME ||
                    (tx.transactionType == TransactionType.TRANSFER &&
                        (accountTypeMap[tx.accountId] == AccountType.LENT_LOAN ||
                            tx.categoryLabel.equals("Cobro de Préstamo", ignoreCase = true)))
            }
            .sumOf { it.amount }
        val monthExpense = monthTxs
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val activeScheduled = scheduled.filter { it.isActive }
        val monthlySubs = activeScheduled
            .filter { it.recurringKind == RecurringKind.SUBSCRIPTION }
            .sumOf { it.amount * it.recurrenceFrequency.monthlyFactor }
        val monthlyBills = activeScheduled
            .filter { it.recurringKind == RecurringKind.RECURRING_BILL }
            .sumOf { it.amount * it.recurrenceFrequency.monthlyFactor }
        val monthlyRecurringIncome = activeScheduled
            .filter { it.recurringKind == RecurringKind.RECURRING_INCOME }
            .sumOf { it.amount * it.recurrenceFrequency.monthlyFactor }

        val activePlans = plans.filter { !it.isCompleted }
        val monthlyMsiTotal = activePlans.sumOf { it.monthlyAmount }
        val monthlyLoanTotal = accounts
            .filter { it.accountType == AccountType.BANK_LOAN && it.balance > 0.0 }
            .sumOf { it.monthlyInstallment }

        val creditCardSummaries = creditCards.map { card ->
            val cardPlans = activePlans.filter { it.creditCardAccountId == card.id }
            val monthlyMsi = cardPlans.sumOf { it.monthlyAmount }
            val totalMsiRem = cardPlans.sumOf { it.remainingAmount }
            val revolving = max(0.0, card.balance - totalMsiRem)
            val available = max(0.0, card.creditLimit - card.balance)
            val utilization = if (card.creditLimit > 0.0) {
                (card.balance / card.creditLimit).coerceIn(0.0, 1.5)
            } else 0.0
            CreditCardSummary(
                card = card,
                nextCutoff = DateMath.nextOccurrenceOfDayOfMonth(card.cutoffDay),
                nextPaymentDue = DateMath.nextOccurrenceOfDayOfMonth(card.paymentDueDay),
                activeInstallments = cardPlans,
                monthlyMsiAmount = monthlyMsi,
                totalMsiRemaining = totalMsiRem,
                revolvingBalance = revolving,
                availableCredit = available,
                utilizationRatio = utilization,
            )
        }

        val creditUtilizationRatio = if (creditLimitTotal > 0.0) {
            (creditDebtTotal / creditLimitTotal).coerceIn(0.0, 1.5)
        } else 0.0

        val fixedMonthlyCommitment = monthlySubs + monthlyBills + monthlyMsiTotal + monthlyLoanTotal

        val analytics = computeAnalytics(
            transactions = transactions,
            categories = categories,
            monthExpense = monthExpense,
            prefs = prefs,
            creditUtilizationRatio = creditUtilizationRatio,
            fixedMonthlyCommitment = fixedMonthlyCommitment,
        )

        return FinanceUiState(
            isLoading = false,
            accounts = accounts,
            transactions = transactions,
            installmentPlans = plans,
            scheduledPayments = scheduled,
            categories = categories,
            hideBalances = prefs.hideBalances,
            liquidTotal = liquidTotal,
            creditDebtTotal = creditDebtTotal,
            creditLimitTotal = creditLimitTotal,
            bankLoansTotal = bankLoansTotal,
            lentLoansTotal = lentLoansTotal,
            netWorth = netWorth,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            monthlySubscriptionsTotal = monthlySubs,
            monthlyRecurringBillsTotal = monthlyBills,
            monthlyRecurringIncomeTotal = monthlyRecurringIncome,
            monthlyMsiTotal = monthlyMsiTotal,
            monthlyLoanTotal = monthlyLoanTotal,
            creditCardSummaries = creditCardSummaries,
            analytics = analytics,
            snackbarMessage = prefs.snackbarMessage,
        )
    }

    private fun computeAnalytics(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        monthExpense: Double,
        prefs: UiPrefsState,
        creditUtilizationRatio: Double,
        fixedMonthlyCommitment: Double,
    ): AnalyticsSummary {
        val period = prefs.period
        val now = System.currentTimeMillis()
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        val todayStart = DateMath.startOfDay(now)

        val periodStart: Long
        val periodEnd: Long
        val periodSubtitle: String
        val periodNavLabel: String
        val isCurrentPeriod: Boolean
        val budgetWindowStart: Long
        val budgetWindowEnd: Long

        when (period) {
            AnalyticsPeriod.WEEK -> {
                periodStart = DateMath.startOfWeekWithOffset(prefs.weekOffset)
                periodEnd = DateMath.endOfWeekExclusive(periodStart)
                val weekLastDay = periodEnd - 86_400_000L
                isCurrentPeriod = prefs.weekOffset == 0
                periodNavLabel = "${FinanceFormatters.shortDate(periodStart)} – ${FinanceFormatters.shortDate(weekLastDay)}"
                periodSubtitle = if (isCurrentPeriod) {
                    "Semana actual ($periodNavLabel)"
                } else {
                    "Semana del $periodNavLabel"
                }
                budgetWindowStart = DateMath.startOfCurrentMonth()
                budgetWindowEnd = DateMath.endOfMonthExclusive(currentYear, currentMonth)
            }

            AnalyticsPeriod.MONTH -> {
                periodStart = DateMath.startOfMonth(prefs.anchorYear, prefs.anchorMonth)
                periodEnd = DateMath.endOfMonthExclusive(prefs.anchorYear, prefs.anchorMonth)
                isCurrentPeriod = prefs.anchorYear == currentYear && prefs.anchorMonth == currentMonth
                val label = FinanceFormatters.monthYear(prefs.anchorYear, prefs.anchorMonth)
                periodNavLabel = label
                periodSubtitle = label
                budgetWindowStart = periodStart
                budgetWindowEnd = periodEnd
            }

            AnalyticsPeriod.YEAR -> {
                periodStart = DateMath.startOfYear(prefs.anchorYear)
                periodEnd = DateMath.endOfYearExclusive(prefs.anchorYear)
                isCurrentPeriod = prefs.anchorYear == currentYear
                periodNavLabel = "Año ${prefs.anchorYear}"
                periodSubtitle = periodNavLabel
                budgetWindowStart = DateMath.startOfCurrentMonth()
                budgetWindowEnd = DateMath.endOfMonthExclusive(currentYear, currentMonth)
            }

            AnalyticsPeriod.CUSTOM -> {
                val rawStart = prefs.customStartMillis ?: (todayStart - 29L * 86_400_000L)
                val rawEnd = prefs.customEndMillis ?: todayStart
                periodStart = DateMath.startOfDay(minOf(rawStart, rawEnd))
                val inclusiveEnd = DateMath.startOfDay(maxOf(rawStart, rawEnd))
                periodEnd = DateMath.endOfDayExclusive(inclusiveEnd)
                isCurrentPeriod = false
                periodNavLabel = "${FinanceFormatters.shortDate(periodStart)} – ${FinanceFormatters.fullDate(inclusiveEnd)}"
                periodSubtitle = "Rango: $periodNavLabel"
                budgetWindowStart = DateMath.startOfCurrentMonth()
                budgetWindowEnd = DateMath.endOfMonthExclusive(currentYear, currentMonth)
            }
        }

        val totalSpanDays = max(1, DateMath.daysBetween(periodStart, periodEnd - 1L) + 1)
        val availableGranularities = when (period) {
            AnalyticsPeriod.WEEK -> listOf(AnalyticsGranularity.DAY)
            AnalyticsPeriod.MONTH -> listOf(AnalyticsGranularity.DAY, AnalyticsGranularity.WEEK)
            AnalyticsPeriod.YEAR -> listOf(AnalyticsGranularity.MONTH, AnalyticsGranularity.WEEK)
            AnalyticsPeriod.CUSTOM -> when {
                totalSpanDays <= 16 -> listOf(AnalyticsGranularity.DAY)
                totalSpanDays <= 65 -> listOf(AnalyticsGranularity.DAY, AnalyticsGranularity.WEEK)
                else -> listOf(AnalyticsGranularity.WEEK, AnalyticsGranularity.MONTH)
            }
        }
        val granularity = if (prefs.granularity in availableGranularities) {
            prefs.granularity
        } else {
            availableGranularities.first()
        }

        val periodTxs = transactions.filter { it.dateMillis in periodStart until periodEnd }
        val periodExpenseTxs = periodTxs.filter { it.transactionType == TransactionType.EXPENSE }
        val periodIncomeTxs = periodTxs.filter { it.transactionType == TransactionType.INCOME }

        val totalExpense = periodExpenseTxs.sumOf { it.amount }
        val totalIncome = periodIncomeTxs.sumOf { it.amount }
        val netFlow = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0.0) (netFlow / totalIncome).coerceIn(-1.0, 1.0) else 0.0

        val effectiveEndForAverage = if (now in periodStart until periodEnd) now else (periodEnd - 1L)
        val elapsedDays = max(1, DateMath.daysBetween(periodStart, effectiveEndForAverage) + 1)
        val averageDailyExpense = totalExpense / elapsedDays.toDouble()

        val projectedMonthExpense = if (period == AnalyticsPeriod.MONTH && !isCurrentPeriod) {
            totalExpense
        } else {
            val daysPassedInMonth = max(1, DateMath.currentDayOfMonth())
            val daysInMonth = DateMath.daysInCurrentMonth()
            (monthExpense / daysPassedInMonth.toDouble()) * daysInMonth.toDouble()
        }

        val barPoints = buildBarPoints(
            period = period,
            granularity = granularity,
            periodStart = periodStart,
            periodEnd = periodEnd,
            anchorYear = prefs.anchorYear,
            anchorMonth = prefs.anchorMonth,
            periodTxs = periodTxs,
        )

        val categoryMetaByLower = categories.associateBy { it.name.lowercase(Locale.ROOT) }
        val categoryBreakdown = periodExpenseTxs
            .groupBy { it.categoryLabel }
            .map { (catName, list) ->
                val sum = list.sumOf { it.amount }
                CategorySlice(
                    categoryName = catName,
                    colorIndex = categoryMetaByLower[catName.lowercase(Locale.ROOT)]?.colorIndex,
                    amount = sum,
                    ratio = if (totalExpense > 0.0) sum / totalExpense else 0.0,
                    count = list.size,
                )
            }
            .sortedByDescending { it.amount }

        val monthExpenseByLowerCat = transactions
            .filter { it.dateMillis in budgetWindowStart until budgetWindowEnd && it.transactionType == TransactionType.EXPENSE }
            .groupBy { it.categoryLabel.lowercase(Locale.ROOT) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val budgetProgress = categories
            .filter { it.monthlyBudget > 0.0 }
            .map { cat ->
                val spent = monthExpenseByLowerCat[cat.name.lowercase(Locale.ROOT)] ?: 0.0
                val ratio = spent / cat.monthlyBudget
                BudgetProgress(category = cat, spentThisMonth = spent, ratio = ratio)
            }
            .sortedByDescending { it.ratio }

        return AnalyticsSummary(
            period = period,
            granularity = granularity,
            availableGranularities = availableGranularities,
            periodSubtitle = periodSubtitle,
            periodNavLabel = periodNavLabel,
            isCurrentPeriod = isCurrentPeriod,
            periodStartMillis = periodStart,
            periodEndMillis = periodEnd,
            selectedYear = prefs.anchorYear,
            selectedMonth = prefs.anchorMonth,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netFlow = netFlow,
            savingsRate = savingsRate,
            averageDailyExpense = averageDailyExpense,
            projectedMonthExpense = projectedMonthExpense,
            topCategory = categoryBreakdown.firstOrNull(),
            barPoints = barPoints,
            categoryBreakdown = categoryBreakdown,
            budgetProgress = budgetProgress,
            creditUtilizationRatio = creditUtilizationRatio,
            fixedMonthlyCommitment = fixedMonthlyCommitment,
        )
    }

    private fun buildBarPoints(
        period: AnalyticsPeriod,
        granularity: AnalyticsGranularity,
        periodStart: Long,
        periodEnd: Long,
        anchorYear: Int,
        anchorMonth: Int,
        periodTxs: List<TransactionEntity>,
    ): List<ChartBarPoint> {
        val cal = Calendar.getInstance()
        return when {
            period == AnalyticsPeriod.WEEK && granularity == AnalyticsGranularity.DAY -> {
                val dayLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                val fullLabels = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
                val expenseBuckets = DoubleArray(7)
                val incomeBuckets = DoubleArray(7)
                periodTxs.forEach { tx ->
                    cal.timeInMillis = tx.dateMillis
                    val idx = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        Calendar.SATURDAY -> 5
                        else -> 6
                    }
                    when (tx.transactionType) {
                        TransactionType.EXPENSE -> expenseBuckets[idx] += tx.amount
                        TransactionType.INCOME -> incomeBuckets[idx] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
                dayLabels.indices.map { i ->
                    val dayDate = periodStart + i * 86_400_000L
                    ChartBarPoint(
                        label = dayLabels[i],
                        fullLabel = "${fullLabels[i]} (${FinanceFormatters.shortDate(dayDate)})",
                        expense = expenseBuckets[i],
                        income = incomeBuckets[i],
                    )
                }
            }

            period == AnalyticsPeriod.MONTH && granularity == AnalyticsGranularity.WEEK -> {
                val expenseBuckets = DoubleArray(5)
                val incomeBuckets = DoubleArray(5)
                val daysCount = DateMath.daysInMonth(anchorYear, anchorMonth)
                periodTxs.forEach { tx ->
                    cal.timeInMillis = tx.dateMillis
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    val weekIdx = ((day - 1) / 7).coerceIn(0, 4)
                    when (tx.transactionType) {
                        TransactionType.EXPENSE -> expenseBuckets[weekIdx] += tx.amount
                        TransactionType.INCOME -> incomeBuckets[weekIdx] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
                val ranges = listOf("Días 1–7", "Días 8–14", "Días 15–21", "Días 22–28", "Días 29–$daysCount")
                val bucketCount = if (daysCount > 28) 5 else 4
                (0 until bucketCount).map { i ->
                    ChartBarPoint(
                        label = "S${i + 1}",
                        fullLabel = "Semana ${i + 1} (${ranges[i]})",
                        expense = expenseBuckets[i],
                        income = incomeBuckets[i],
                    )
                }
            }

            period == AnalyticsPeriod.MONTH && granularity == AnalyticsGranularity.DAY -> {
                val daysCount = DateMath.daysInMonth(anchorYear, anchorMonth)
                val expenseBuckets = DoubleArray(daysCount)
                val incomeBuckets = DoubleArray(daysCount)
                periodTxs.forEach { tx ->
                    cal.timeInMillis = tx.dateMillis
                    val idx = (cal.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, daysCount - 1)
                    when (tx.transactionType) {
                        TransactionType.EXPENSE -> expenseBuckets[idx] += tx.amount
                        TransactionType.INCOME -> incomeBuckets[idx] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
                val monthName = FinanceFormatters.shortMonth(anchorMonth)
                (0 until daysCount).map { i ->
                    val dayNum = i + 1
                    ChartBarPoint(
                        label = if (dayNum == 1 || dayNum % 5 == 0 || dayNum == daysCount) "$dayNum" else "",
                        fullLabel = "$dayNum $monthName $anchorYear",
                        expense = expenseBuckets[i],
                        income = incomeBuckets[i],
                    )
                }
            }

            period == AnalyticsPeriod.YEAR && granularity == AnalyticsGranularity.WEEK -> {
                val isCurrentYear = anchorYear == Calendar.getInstance().get(Calendar.YEAR)
                val endWeek = if (isCurrentYear) Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) else 52
                val startWeek = max(1, endWeek - 11)
                val weekCount = (endWeek - startWeek + 1).coerceAtLeast(1)
                val expenseBuckets = DoubleArray(weekCount)
                val incomeBuckets = DoubleArray(weekCount)
                periodTxs.forEach { tx ->
                    cal.timeInMillis = tx.dateMillis
                    val wk = cal.get(Calendar.WEEK_OF_YEAR)
                    val idx = wk - startWeek
                    if (idx in 0 until weekCount) {
                        when (tx.transactionType) {
                            TransactionType.EXPENSE -> expenseBuckets[idx] += tx.amount
                            TransactionType.INCOME -> incomeBuckets[idx] += tx.amount
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                }
                (0 until weekCount).map { i ->
                    val wkNum = startWeek + i
                    ChartBarPoint(
                        label = "S$wkNum",
                        fullLabel = "Semana $wkNum ($anchorYear)",
                        expense = expenseBuckets[i],
                        income = incomeBuckets[i],
                    )
                }
            }

            period == AnalyticsPeriod.CUSTOM -> {
                buildCustomRangeBarPoints(periodStart, periodEnd, granularity, periodTxs)
            }

            else -> {
                val shortMonths = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                val fullMonths = listOf(
                    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
                )
                val expenseBuckets = DoubleArray(12)
                val incomeBuckets = DoubleArray(12)
                periodTxs.forEach { tx ->
                    cal.timeInMillis = tx.dateMillis
                    val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
                    when (tx.transactionType) {
                        TransactionType.EXPENSE -> expenseBuckets[m] += tx.amount
                        TransactionType.INCOME -> incomeBuckets[m] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
                shortMonths.indices.map { i ->
                    ChartBarPoint(
                        label = shortMonths[i],
                        fullLabel = "${fullMonths[i]} $anchorYear",
                        expense = expenseBuckets[i],
                        income = incomeBuckets[i],
                    )
                }
            }
        }
    }

    private fun buildCustomRangeBarPoints(
        periodStart: Long,
        periodEnd: Long,
        granularity: AnalyticsGranularity,
        periodTxs: List<TransactionEntity>,
    ): List<ChartBarPoint> {
        val inclusiveEnd = (periodEnd - 1L).coerceAtLeast(periodStart)
        val totalDays = max(1, DateMath.daysBetween(periodStart, inclusiveEnd) + 1)
        return when (granularity) {
            AnalyticsGranularity.DAY -> {
                val count = totalDays.coerceAtMost(65)
                val exp = DoubleArray(count)
                val inc = DoubleArray(count)
                periodTxs.forEach { tx ->
                    val idx = DateMath.daysBetween(periodStart, tx.dateMillis).coerceIn(0, count - 1)
                    when (tx.transactionType) {
                        TransactionType.EXPENSE -> exp[idx] += tx.amount
                        TransactionType.INCOME -> inc[idx] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
                val labelStep = when {
                    count <= 7 -> 1
                    count <= 16 -> 3
                    else -> 5
                }
                (0 until count).map { i ->
                    val dayMs = periodStart + i * 86_400_000L
                    val short = FinanceFormatters.shortDate(dayMs)
                    ChartBarPoint(
                        label = if (i == 0 || i == count - 1 || i % labelStep == 0) short.substringBefore(" ") else "",
                        fullLabel = FinanceFormatters.fullDate(dayMs),
                        expense = exp[i],
                        income = inc[i],
                    )
                }
            }

            AnalyticsGranularity.WEEK -> {
                val weekCount = ((totalDays + 6) / 7).coerceIn(1, 20)
                val exp = DoubleArray(weekCount)
                val inc = DoubleArray(weekCount)
                periodTxs.forEach { tx ->
                    val dayOffset = DateMath.daysBetween(periodStart, tx.dateMillis).coerceAtLeast(0)
                    val idx = (dayOffset / 7).coerceIn(0, weekCount - 1)
                    when (tx.transactionType) {
                        TransactionType.EXPENSE -> exp[idx] += tx.amount
                        TransactionType.INCOME -> inc[idx] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
                (0 until weekCount).map { i ->
                    val wStart = periodStart + i * 7L * 86_400_000L
                    val wEnd = minOf(inclusiveEnd, wStart + 6L * 86_400_000L)
                    ChartBarPoint(
                        label = "S${i + 1}",
                        fullLabel = "${FinanceFormatters.shortDate(wStart)} – ${FinanceFormatters.shortDate(wEnd)}",
                        expense = exp[i],
                        income = inc[i],
                    )
                }
            }

            AnalyticsGranularity.MONTH -> {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = periodStart
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = inclusiveEnd
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val buckets = mutableListOf<Pair<Int, Int>>()
                val cursor = startCal.clone() as Calendar
                while (!cursor.after(endCal) && buckets.size < 24) {
                    buckets.add(cursor.get(Calendar.YEAR) to cursor.get(Calendar.MONTH))
                    cursor.add(Calendar.MONTH, 1)
                }
                val exp = DoubleArray(buckets.size)
                val inc = DoubleArray(buckets.size)
                val txCal = Calendar.getInstance()
                periodTxs.forEach { tx ->
                    txCal.timeInMillis = tx.dateMillis
                    val key = txCal.get(Calendar.YEAR) to txCal.get(Calendar.MONTH)
                    val idx = buckets.indexOf(key)
                    if (idx >= 0) {
                        when (tx.transactionType) {
                            TransactionType.EXPENSE -> exp[idx] += tx.amount
                            TransactionType.INCOME -> inc[idx] += tx.amount
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                }
                buckets.mapIndexed { idx, (yr, mo) ->
                    ChartBarPoint(
                        label = FinanceFormatters.shortMonth(mo),
                        fullLabel = FinanceFormatters.monthYear(yr, mo),
                        expense = exp[idx],
                        income = inc[idx],
                    )
                }
            }
        }
    }
}
