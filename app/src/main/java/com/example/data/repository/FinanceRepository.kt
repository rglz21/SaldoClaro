package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.FinanceDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.BudgetEntity
import com.example.data.model.FinanceCategory
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.DateMath
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class FinanceRepository(private val database: FinanceDatabase) {
    private val dao = database.financeDao()

    val accounts: Flow<List<AccountEntity>> = dao.observeAccounts()
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val installmentPlans: Flow<List<InstallmentPlanEntity>> = dao.observeInstallmentPlans()
    val scheduledPayments: Flow<List<ScheduledPaymentEntity>> = dao.observeScheduledPayments()
    val budgets: Flow<List<BudgetEntity>> = dao.observeBudgets()

    suspend fun saveAccount(account: AccountEntity): Long {
        return dao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        dao.updateAccount(account)
    }

    suspend fun deleteAccount(accountId: Long) {
        database.withTransaction {
            dao.deleteScheduledForAccount(accountId)
            dao.deleteInstallmentsForAccount(accountId)
            dao.deleteTransactionsForAccount(accountId)
            dao.deleteAccountById(accountId)
        }
    }

    suspend fun recordIncomeOrExpense(
        title: String,
        amount: Double,
        type: TransactionType,
        category: FinanceCategory,
        accountId: Long,
        dateMillis: Long,
        note: String,
    ) {
        require(amount > 0.0) { "El monto debe ser mayor a cero" }
        database.withTransaction {
            val account = dao.getAccountById(accountId)
                ?: error("Cuenta no encontrada")
            val updatedBalance = computeNewBalanceForSingle(
                currentBalance = account.balance,
                accountType = account.accountType,
                transactionType = type,
                amount = amount,
            )
            dao.updateAccount(account.copy(balance = updatedBalance))
            dao.insertTransaction(
                TransactionEntity(
                    title = title.trim(),
                    amount = amount,
                    type = type.name,
                    category = category.name,
                    accountId = accountId,
                    dateMillis = dateMillis,
                    note = note.trim(),
                )
            )
        }
    }

    suspend fun recordTransfer(
        title: String,
        amount: Double,
        fromAccountId: Long,
        toAccountId: Long,
        dateMillis: Long,
        note: String,
        categoryOverride: FinanceCategory? = null,
    ) {
        require(amount > 0.0) { "El monto debe ser mayor a cero" }
        require(fromAccountId != toAccountId) { "Selecciona dos cuentas diferentes" }
        database.withTransaction {
            val source = dao.getAccountById(fromAccountId)
                ?: error("Cuenta de origen no encontrada")
            val destination = dao.getAccountById(toAccountId)
                ?: error("Cuenta de destino no encontrada")

            val newSourceBalance = computeTransferSourceBalance(source.balance, source.accountType, amount)
            val newDestBalance = computeTransferDestinationBalance(destination.balance, destination.accountType, amount)

            dao.updateAccount(source.copy(balance = newSourceBalance))
            dao.updateAccount(destination.copy(balance = newDestBalance))

            val resolvedCategory = categoryOverride ?: when {
                destination.accountType == AccountType.CREDIT -> FinanceCategory.CARD_PAYMENT
                destination.accountType == AccountType.BANK_LOAN -> FinanceCategory.LOAN_PAYMENT
                source.accountType == AccountType.LENT_LOAN -> FinanceCategory.LOAN_COLLECTION
                else -> FinanceCategory.TRANSFER
            }

            dao.insertTransaction(
                TransactionEntity(
                    title = title.trim().ifEmpty { "${source.name} → ${destination.name}" },
                    amount = amount,
                    type = TransactionType.TRANSFER.name,
                    category = resolvedCategory.name,
                    accountId = fromAccountId,
                    destinationAccountId = toAccountId,
                    dateMillis = dateMillis,
                    note = note.trim(),
                )
            )
        }
    }

    suspend fun recordInstallmentPurchase(
        creditCardId: Long,
        description: String,
        totalAmount: Double,
        totalMonths: Int,
        category: FinanceCategory,
        startDateMillis: Long,
        note: String,
    ) {
        require(totalAmount > 0.0) { "El monto debe ser mayor a cero" }
        require(totalMonths >= 2) { "Los meses sin intereses deben ser al menos 2" }
        database.withTransaction {
            val card = dao.getAccountById(creditCardId)
                ?: error("Tarjeta de crédito no encontrada")
            require(card.accountType == AccountType.CREDIT) {
                "Las compras a meses sin intereses requieren una tarjeta de crédito"
            }

            val monthlyAmount = totalAmount / totalMonths.toDouble()
            val planId = dao.insertInstallmentPlan(
                InstallmentPlanEntity(
                    creditCardAccountId = creditCardId,
                    description = description.trim(),
                    totalAmount = totalAmount,
                    totalMonths = totalMonths,
                    paidMonths = 0,
                    monthlyAmount = monthlyAmount,
                    category = category.name,
                    startDateMillis = startDateMillis,
                )
            )
            // Increases total credit card balance owed
            dao.updateAccount(card.copy(balance = card.balance + totalAmount))

            dao.insertTransaction(
                TransactionEntity(
                    title = "${description.trim()} (${totalMonths} MSI)",
                    amount = totalAmount,
                    type = TransactionType.EXPENSE.name,
                    category = category.name,
                    accountId = creditCardId,
                    dateMillis = startDateMillis,
                    note = note.trim().ifEmpty { "Compra a $totalMonths meses sin intereses" },
                    installmentPlanId = planId,
                )
            )
        }
    }

    suspend fun payInstallmentMonth(
        planId: Long,
        fromAccountId: Long,
        dateMillis: Long = System.currentTimeMillis(),
    ) {
        database.withTransaction {
            val plan = dao.getInstallmentPlanById(planId) ?: error("Plan MSI no encontrado")
            if (plan.isCompleted) return@withTransaction
            val source = dao.getAccountById(fromAccountId) ?: error("Cuenta de pago no encontrada")
            val card = dao.getAccountById(plan.creditCardAccountId) ?: error("Tarjeta no encontrada")

            val payAmount = plan.monthlyAmount.coerceAtMost(plan.remainingAmount)
            val newSourceBalance = computeTransferSourceBalance(source.balance, source.accountType, payAmount)
            val newCardBalance = max(0.0, card.balance - payAmount)

            dao.updateAccount(source.copy(balance = newSourceBalance))
            dao.updateAccount(card.copy(balance = newCardBalance))

            val nextPaidMonths = (plan.paidMonths + 1).coerceAtMost(plan.totalMonths)
            dao.updateInstallmentPlan(plan.copy(paidMonths = nextPaidMonths))

            dao.insertTransaction(
                TransactionEntity(
                    title = "Cuota $nextPaidMonths/${plan.totalMonths} · ${plan.description}",
                    amount = payAmount,
                    type = TransactionType.TRANSFER.name,
                    category = FinanceCategory.CARD_PAYMENT.name,
                    accountId = fromAccountId,
                    destinationAccountId = card.id,
                    dateMillis = dateMillis,
                    note = "Pago de mensualidad MSI a ${card.name}",
                    installmentPlanId = plan.id,
                )
            )
        }
    }

    suspend fun deleteInstallmentPlan(planId: Long) {
        dao.deleteInstallmentPlanById(planId)
    }

    suspend fun deleteTransactionWithRollback(transactionId: Long) {
        database.withTransaction {
            val tx = dao.getTransactionById(transactionId) ?: return@withTransaction
            when (tx.transactionType) {
                TransactionType.INCOME, TransactionType.EXPENSE -> {
                    val account = dao.getAccountById(tx.accountId)
                    if (account != null) {
                        val reverted = revertSingleBalance(
                            currentBalance = account.balance,
                            accountType = account.accountType,
                            transactionType = tx.transactionType,
                            amount = tx.amount,
                        )
                        dao.updateAccount(account.copy(balance = reverted))
                    }
                }
                TransactionType.TRANSFER -> {
                    val source = dao.getAccountById(tx.accountId)
                    val dest = tx.destinationAccountId?.let { dao.getAccountById(it) }
                    if (source != null) {
                        val revertedSource = revertTransferSourceBalance(source.balance, source.accountType, tx.amount)
                        dao.updateAccount(source.copy(balance = revertedSource))
                    }
                    if (dest != null) {
                        val revertedDest = revertTransferDestinationBalance(dest.balance, dest.accountType, tx.amount)
                        dao.updateAccount(dest.copy(balance = revertedDest))
                    }
                }
            }
            dao.deleteTransactionById(transactionId)
        }
    }

    suspend fun saveScheduledPayment(payment: ScheduledPaymentEntity): Long {
        return dao.insertScheduledPayment(payment)
    }

    suspend fun toggleScheduledPayment(payment: ScheduledPaymentEntity) {
        dao.updateScheduledPayment(payment.copy(isActive = !payment.isActive))
    }

    suspend fun deleteScheduledPayment(payment: ScheduledPaymentEntity) {
        dao.deleteScheduledPayment(payment)
    }

    suspend fun executeScheduledPaymentNow(
        payment: ScheduledPaymentEntity,
        paidAtMillis: Long = System.currentTimeMillis(),
    ) {
        database.withTransaction {
            val account = dao.getAccountById(payment.accountId)
                ?: error("Cuenta asociada no encontrada")
            val newBalance = computeNewBalanceForSingle(
                currentBalance = account.balance,
                accountType = account.accountType,
                transactionType = TransactionType.EXPENSE,
                amount = payment.amount,
            )
            dao.updateAccount(account.copy(balance = newBalance))
            dao.insertTransaction(
                TransactionEntity(
                    title = payment.name,
                    amount = payment.amount,
                    type = TransactionType.EXPENSE.name,
                    category = payment.category,
                    accountId = payment.accountId,
                    dateMillis = paidAtMillis,
                    note = "${payment.recurringKind.labelEs} · ${payment.recurrenceFrequency.labelEs}",
                    scheduledPaymentId = payment.id,
                )
            )
            val nextDate = DateMath.advanceDate(payment.nextDueDateMillis, payment.recurrenceFrequency)
            dao.updateScheduledPayment(payment.copy(nextDueDateMillis = nextDate))
        }
    }

    suspend fun upsertBudget(category: FinanceCategory, monthlyLimit: Double) {
        require(monthlyLimit > 0.0)
        dao.upsertBudget(BudgetEntity(category = category.name, monthlyLimit = monthlyLimit))
    }

    suspend fun deleteBudget(category: FinanceCategory) {
        dao.deleteBudget(category.name)
    }

    // --- Balance math helpers ---

    private fun computeNewBalanceForSingle(
        currentBalance: Double,
        accountType: AccountType,
        transactionType: TransactionType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> {
            // Balance represents debt owed
            if (transactionType == TransactionType.EXPENSE) currentBalance + amount
            else max(0.0, currentBalance - amount)
        }
        AccountType.SAVINGS, AccountType.DEBIT, AccountType.CASH, AccountType.LENT_LOAN -> {
            if (transactionType == TransactionType.INCOME) currentBalance + amount
            else currentBalance - amount
        }
    }

    private fun revertSingleBalance(
        currentBalance: Double,
        accountType: AccountType,
        transactionType: TransactionType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> {
            if (transactionType == TransactionType.EXPENSE) max(0.0, currentBalance - amount)
            else currentBalance + amount
        }
        AccountType.SAVINGS, AccountType.DEBIT, AccountType.CASH, AccountType.LENT_LOAN -> {
            if (transactionType == TransactionType.INCOME) currentBalance - amount
            else currentBalance + amount
        }
    }

    private fun computeTransferSourceBalance(
        currentBalance: Double,
        accountType: AccountType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> currentBalance + amount // cash advance increases debt
        AccountType.LENT_LOAN -> max(0.0, currentBalance - amount)           // borrower paid you back
        AccountType.SAVINGS, AccountType.DEBIT, AccountType.CASH -> currentBalance - amount
    }

    private fun computeTransferDestinationBalance(
        currentBalance: Double,
        accountType: AccountType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> max(0.0, currentBalance - amount) // paying off debt
        AccountType.SAVINGS, AccountType.DEBIT, AccountType.CASH, AccountType.LENT_LOAN -> currentBalance + amount
    }

    private fun revertTransferSourceBalance(
        currentBalance: Double,
        accountType: AccountType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> max(0.0, currentBalance - amount)
        AccountType.LENT_LOAN -> currentBalance + amount
        AccountType.SAVINGS, AccountType.DEBIT, AccountType.CASH -> currentBalance + amount
    }

    private fun revertTransferDestinationBalance(
        currentBalance: Double,
        accountType: AccountType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> currentBalance + amount
        AccountType.SAVINGS, AccountType.DEBIT, AccountType.CASH, AccountType.LENT_LOAN -> currentBalance - amount
    }
}
