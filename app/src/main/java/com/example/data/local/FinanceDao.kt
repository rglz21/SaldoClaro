package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AccountEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Accounts
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC, id ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAccounts(): Int

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR destinationAccountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: Long)

    // Installment Plans (MSI)
    @Query("SELECT * FROM installment_plans ORDER BY startDateMillis DESC")
    fun observeInstallmentPlans(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans ORDER BY id ASC")
    suspend fun getAllInstallmentPlansOnce(): List<InstallmentPlanEntity>

    @Query("SELECT * FROM installment_plans WHERE id = :id LIMIT 1")
    suspend fun getInstallmentPlanById(id: Long): InstallmentPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentPlan(plan: InstallmentPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentPlans(plans: List<InstallmentPlanEntity>)

    @Update
    suspend fun updateInstallmentPlan(plan: InstallmentPlanEntity)

    @Query("DELETE FROM installment_plans WHERE id = :id")
    suspend fun deleteInstallmentPlanById(id: Long)

    @Query("DELETE FROM installment_plans WHERE creditCardAccountId = :accountId")
    suspend fun deleteInstallmentsForAccount(accountId: Long)

    // Scheduled Payments (Subscriptions & Recurring Bills)
    @Query("SELECT * FROM scheduled_payments ORDER BY isActive DESC, nextDueDateMillis ASC")
    fun observeScheduledPayments(): Flow<List<ScheduledPaymentEntity>>

    @Query("SELECT * FROM scheduled_payments")
    suspend fun getAllScheduledPaymentsOnce(): List<ScheduledPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPayment(payment: ScheduledPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPayments(payments: List<ScheduledPaymentEntity>)

    @Update
    suspend fun updateScheduledPayment(payment: ScheduledPaymentEntity)

    @Delete
    suspend fun deleteScheduledPayment(payment: ScheduledPaymentEntity)

    @Query("DELETE FROM scheduled_payments WHERE accountId = :accountId")
    suspend fun deleteScheduledForAccount(accountId: Long)

    // Categories (with integrated monthly budget)
    @Query("SELECT * FROM categories ORDER BY isIncome ASC, name COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET monthlyBudget = :monthlyBudget WHERE id = :categoryId")
    suspend fun updateCategoryBudget(categoryId: Long, monthlyBudget: Double)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    // Legacy Category Budgets (kept for migration compatibility)
    @Query("SELECT * FROM budgets ORDER BY monthlyLimit DESC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    // Full reset helpers (for dataset replacement)
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM installment_plans")
    suspend fun deleteAllInstallmentPlans()

    @Query("DELETE FROM scheduled_payments")
    suspend fun deleteAllScheduledPayments()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
