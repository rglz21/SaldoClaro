package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AccountEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Accounts
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR destinationAccountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: Long)

    // Installment Plans (MSI)
    @Query("SELECT * FROM installment_plans ORDER BY startDateMillis DESC")
    fun observeInstallmentPlans(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans WHERE id = :id LIMIT 1")
    suspend fun getInstallmentPlanById(id: Long): InstallmentPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentPlan(plan: InstallmentPlanEntity): Long

    @Update
    suspend fun updateInstallmentPlan(plan: InstallmentPlanEntity)

    @Query("DELETE FROM installment_plans WHERE id = :id")
    suspend fun deleteInstallmentPlanById(id: Long)

    @Query("DELETE FROM installment_plans WHERE creditCardAccountId = :accountId")
    suspend fun deleteInstallmentsForAccount(accountId: Long)

    // Scheduled Payments (Subscriptions & Recurring Bills)
    @Query("SELECT * FROM scheduled_payments ORDER BY isActive DESC, nextDueDateMillis ASC")
    fun observeScheduledPayments(): Flow<List<ScheduledPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPayment(payment: ScheduledPaymentEntity): Long

    @Update
    suspend fun updateScheduledPayment(payment: ScheduledPaymentEntity)

    @Delete
    suspend fun deleteScheduledPayment(payment: ScheduledPaymentEntity)

    @Query("DELETE FROM scheduled_payments WHERE accountId = :accountId")
    suspend fun deleteScheduledForAccount(accountId: Long)

    // Category Budgets
    @Query("SELECT * FROM budgets ORDER BY monthlyLimit DESC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)
}
