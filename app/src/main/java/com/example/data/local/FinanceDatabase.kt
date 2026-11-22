package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        InstallmentPlanEntity::class,
        ScheduledPaymentEntity::class,
        BudgetEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "saldoclaro_finance.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
