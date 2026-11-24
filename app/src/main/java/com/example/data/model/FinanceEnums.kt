package com.example.data.model

enum class AccountType(val labelEs: String, val shortLabelEs: String) {
    SAVINGS("Cuenta de Ahorro", "Ahorro"),
    DEBIT("Tarjeta de Débito", "Débito"),
    CREDIT("Tarjeta de Crédito", "Crédito"),
    BANK_LOAN("Préstamo Bancario", "Préstamo"),
    LENT_LOAN("Préstamo Realizado", "Me deben"),
    CASH("Efectivo", "Efectivo");

    val isLiability: Boolean
        get() = this == CREDIT || this == BANK_LOAN

    val isLiquidAsset: Boolean
        get() = this == SAVINGS || this == DEBIT || this == CASH

    companion object {
        fun fromName(name: String): AccountType =
            entries.firstOrNull { it.name == name } ?: DEBIT
    }
}

enum class TransactionType(val labelEs: String) {
    INCOME("Ingreso"),
    EXPENSE("Gasto"),
    TRANSFER("Transferencia");

    companion object {
        fun fromName(name: String): TransactionType =
            entries.firstOrNull { it.name == name } ?: EXPENSE
    }
}

enum class RecurringKind(val labelEs: String) {
    ONE_TIME_PAYMENT("Pago único"),
    SUBSCRIPTION("Suscripción"),
    RECURRING_BILL("Pago frecuente"),
    RECURRING_INCOME("Ingreso frecuente");

    val isIncome: Boolean get() = this == RECURRING_INCOME
    val isOneTime: Boolean get() = this == ONE_TIME_PAYMENT

    companion object {
        fun fromName(name: String): RecurringKind =
            entries.firstOrNull { it.name == name } ?: SUBSCRIPTION
    }
}

enum class RecurrenceFrequency(val labelEs: String, val monthlyFactor: Double) {
    ONCE("Una sola vez", 0.0),
    WEEKLY("Semanal", 4.333),
    BIWEEKLY("Quincenal", 2.0),
    MONTHLY("Mensual", 1.0),
    BIMONTHLY("Bimestral", 0.5),
    YEARLY("Anual", 1.0 / 12.0);

    val isOneTime: Boolean get() = this == ONCE

    companion object {
        val recurringEntries: List<RecurrenceFrequency> = listOf(
            WEEKLY, BIWEEKLY, MONTHLY, BIMONTHLY, YEARLY
        )

        fun fromName(name: String): RecurrenceFrequency =
            entries.firstOrNull { it.name == name } ?: MONTHLY
    }
}

enum class FinanceCategory(
    val labelEs: String,
    val isIncomeDefault: Boolean = false,
) {
    FOOD("Alimentación"),
    TRANSPORT("Transporte"),
    HOUSING("Vivienda y Renta"),
    UTILITIES("Servicios"),
    ENTERTAINMENT("Entretenimiento"),
    SUBSCRIPTIONS("Suscripciones"),
    SHOPPING("Compras"),
    HEALTH("Salud"),
    EDUCATION("Educación"),
    TRAVEL("Viajes"),
    CARD_PAYMENT("Pago de Tarjeta"),
    LOAN_PAYMENT("Pago de Préstamo"),
    LOAN_DISBURSEMENT("Desembolso de Préstamo"),
    TRANSFER("Transferencia"),
    SALARY("Nómina / Sueldo", isIncomeDefault = true),
    FREELANCE("Ingresos Extra", isIncomeDefault = true),
    INVESTMENTS("Inversiones", isIncomeDefault = true),
    LOAN_COLLECTION("Cobro de Préstamo", isIncomeDefault = true),
    OTHER("Otros");

    companion object {
        fun fromName(name: String): FinanceCategory =
            entries.firstOrNull { it.name == name } ?: OTHER

        val expenseCategories: List<FinanceCategory> = listOf(
            FOOD, TRANSPORT, HOUSING, UTILITIES, SUBSCRIPTIONS,
            ENTERTAINMENT, SHOPPING, HEALTH, EDUCATION, TRAVEL, OTHER
        )

        val incomeCategories: List<FinanceCategory> = listOf(
            SALARY, FREELANCE, INVESTMENTS, LOAN_COLLECTION, OTHER
        )
    }
}

enum class AnalyticsPeriod(val labelEs: String) {
    WEEK("Semana"),
    MONTH("Mes"),
    YEAR("Año"),
    CUSTOM("Rango"),
}

enum class AnalyticsGranularity(val labelEs: String) {
    DAY("Por día"),
    WEEK("Por semana"),
    MONTH("Por mes"),
}
