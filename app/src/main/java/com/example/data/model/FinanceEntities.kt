package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // AccountType.name
    val institution: String = "",
    val lastFourDigits: String = "",
    /**
     * Semantics by AccountType:
     * - SAVINGS / DEBIT / CASH: current available money
     * - CREDIT: current total debt owed on the card
     * - BANK_LOAN: remaining loan balance owed to bank
     * - LENT_LOAN: remaining receivable owed by borrower
     */
    val balance: Double = 0.0,
    val creditLimit: Double = 0.0,       // CREDIT
    val cutoffDay: Int = 15,             // CREDIT (1..31)
    val paymentDueDay: Int = 5,          // CREDIT / BANK_LOAN / LENT_LOAN (1..31)
    val originalPrincipal: Double = 0.0, // BANK_LOAN / LENT_LOAN
    val monthlyInstallment: Double = 0.0,// BANK_LOAN
    val interestRate: Double = 0.0,      // Optional annual %
    val savingsGoal: Double = 0.0,       // Optional goal for SAVINGS
    val counterpartyName: String = "",   // Borrower name for LENT_LOAN
    val linkedAccountId: Long? = null,   // Disbursement / linked account for BANK_LOAN & LENT_LOAN
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val accountType: AccountType get() = AccountType.fromName(type)
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Double,
    val type: String, // TransactionType.name
    val category: String, // Display name of category (supports custom categories!)
    val accountId: Long,
    val destinationAccountId: Long? = null, // Only for TRANSFER
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val installmentPlanId: Long? = null,
    val scheduledPaymentId: Long? = null,
) {
    val transactionType: TransactionType get() = TransactionType.fromName(type)
    val categoryLabel: String get() = resolveCategoryDisplayName(category)
}

@Entity(tableName = "installment_plans")
data class InstallmentPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val creditCardAccountId: Long,
    val description: String,
    val totalAmount: Double,
    val totalMonths: Int,
    val paidMonths: Int = 0,
    val monthlyAmount: Double,
    val category: String = "Compras",
    val startDateMillis: Long = System.currentTimeMillis(),
) {
    val remainingMonths: Int get() = (totalMonths - paidMonths).coerceAtLeast(0)
    val remainingAmount: Double get() = (totalAmount - paidMonths * monthlyAmount).coerceAtLeast(0.0)
    val isCompleted: Boolean get() = paidMonths >= totalMonths
    val categoryLabel: String get() = resolveCategoryDisplayName(category)
}

@Entity(tableName = "scheduled_payments")
data class ScheduledPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val kind: String, // RecurringKind.name (SUBSCRIPTION or RECURRING_BILL)
    val amount: Double,
    val accountId: Long,
    val category: String, // Category display name
    val frequency: String = RecurrenceFrequency.MONTHLY.name,
    val nextDueDateMillis: Long,
    val isActive: Boolean = true,
    val notes: String = "",
) {
    val recurringKind: RecurringKind get() = RecurringKind.fromName(kind)
    val recurrenceFrequency: RecurrenceFrequency get() = RecurrenceFrequency.fromName(frequency)
    val categoryLabel: String get() = resolveCategoryDisplayName(category)
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val isIncome: Boolean = false,
    val iconKey: String = "OTHER",
    val colorIndex: Int = 0,
    val monthlyBudget: Double = 0.0,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val monthlyLimit: Double,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Resolves either a legacy FinanceCategory enum name (e.g. "FOOD") or a custom category name ("Restaurantes")
 * into its user-facing Spanish display name.
 */
fun resolveCategoryDisplayName(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "Otros"
    val legacyMatch = FinanceCategory.entries.firstOrNull { it.name == trimmed }
    return legacyMatch?.labelEs ?: trimmed
}
