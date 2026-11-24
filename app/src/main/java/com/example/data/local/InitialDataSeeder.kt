package com.example.data.local

import android.content.Context
import androidx.room.withTransaction
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

object InitialDataSeeder {
    private const val PREFS_NAME = "saldoclaro_seed_prefs"
    private const val KEY_SEEDED_CSV = "seeded_neko_csv_reconciled_v2_20261122"
    private const val KEY_SEEDED_RECURRING_INCOME = "seeded_recurring_income_20261122"

    private data class CsvRow(
        val index: Int,
        val dateStr: String,
        val dateMillis: Long,
        val txType: String,
        val description: String,
        val amount: Double,
        val account: String,
        val categoryGroup: String,
        val category: String,
        val notes: String,
    )

    fun markAsSeeded(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SEEDED_CSV, true)
            .putBoolean(KEY_SEEDED_RECURRING_INCOME, true)
            .apply()
    }

    suspend fun seedIfNeeded(context: Context, database: FinanceDatabase) {
        val dao = database.financeDao()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasExistingUserData = dao.countAccounts() > 0 || dao.countTransactions() > 0
        if (prefs.getBoolean(KEY_SEEDED_CSV, false) || hasExistingUserData) {
            if (!prefs.getBoolean(KEY_SEEDED_CSV, false)) {
                prefs.edit().putBoolean(KEY_SEEDED_CSV, true).apply()
            }
            ensureDefaultCategories(dao)
            ensureRecurringSalary(prefs, dao)
            return
        }

        val rows = readCsvRows(context)
        if (rows.isEmpty()) {
            ensureDefaultCategories(dao)
            return
        }

        database.withTransaction {
            // Wipe all previous data completely before replacing with the full-year dataset
            dao.deleteAllTransactions()
            dao.deleteAllInstallmentPlans()
            dao.deleteAllScheduledPayments()
            dao.deleteAllBudgets()
            dao.deleteAllCategories()
            dao.deleteAllAccounts()

            ensureDefaultCategories(dao)

            // 1. Discover and normalize all unique accounts (ensuring all 25 Neko accounts exist)
            val existingAccounts = mutableMapOf<String, Long>()
            val accountTypes = mutableMapOf<String, AccountType>()
            val accountOrder = linkedSetOf<String>()

            // Neko exact account grouping:
            // DÉBITO: BTC, Criptos, FVP, Multimoney, TOHKN, USDT, XTB
            // AHORROS: Agricola, Atlantida, Binance Card, CREDICOMER, DAVIVIENDA, N1co, NIU deb, Nequi (+ Efectivo as CASH)
            // TARJETAS DE CRÉDITO: DaviviendaClasic, EMMA, FarmaValue, NIU cred, ONE, SIMAN, Visa Gold
            // PRÉSTAMOS: Personal AG
            // PRESTADO: préstamo mami2
            val debitNames = listOf("BTC", "Criptos", "FVP", "Multimoney", "TOHKN", "USDT", "XTB")
            val savingsNames = listOf("Agricola", "Atlantida", "Binance Card", "CREDICOMER", "DAVIVIENDA", "N1co", "NIU deb", "Nequi")
            val cashNames = listOf("Efectivo")
            val creditCardNames = listOf(
                "DaviviendaClasic", "EMMA", "FarmaValue", "NIU cred", "ONE", "SIMAN", "Visa Gold",
            )
            val bankLoanNames = listOf("Personal AG")
            val lentLoanNames = listOf("préstamo mami2")

            accountOrder.addAll(debitNames)
            accountOrder.addAll(savingsNames)
            accountOrder.addAll(cashNames)
            accountOrder.addAll(creditCardNames)
            accountOrder.addAll(bankLoanNames)
            accountOrder.addAll(lentLoanNames)

            rows.forEach { row ->
                if (row.account.isNotBlank()) accountOrder.add(row.account)
            }

            accountOrder.forEach { accName ->
                val type = when {
                    accName in cashNames || accName.equals("Efectivo", ignoreCase = true) -> AccountType.CASH
                    accName in creditCardNames -> AccountType.CREDIT
                    accName in debitNames -> AccountType.DEBIT
                    accName in savingsNames -> AccountType.SAVINGS
                    accName in bankLoanNames -> AccountType.BANK_LOAN
                    accName in lentLoanNames -> AccountType.LENT_LOAN
                    else -> AccountType.SAVINGS
                }
                accountTypes[accName] = type
            }

            // Insert non-loan accounts first so we can link loans to Agricola
            val nonLoans = accountOrder.filter {
                val t = accountTypes[it]
                t != AccountType.BANK_LOAN && t != AccountType.LENT_LOAN
            }
            val loans = accountOrder.filter {
                val t = accountTypes[it]
                t == AccountType.BANK_LOAN || t == AccountType.LENT_LOAN
            }

            nonLoans.forEachIndexed { idx, accName ->
                val type = accountTypes[accName] ?: AccountType.DEBIT
                val id = dao.insertAccount(
                    AccountEntity(
                        name = accName,
                        type = type.name,
                        institution = inferInstitution(accName, type),
                        balance = 0.0,
                        creditLimit = if (type == AccountType.CREDIT) 1000.0 else 0.0,
                        cutoffDay = if (type == AccountType.CREDIT) 15 else 1,
                        paymentDueDay = if (type == AccountType.CREDIT) 5 else 1,
                        colorIndex = idx % 10,
                    )
                )
                existingAccounts[accName] = id
            }

            val agricolaId = existingAccounts["Agricola"]
            loans.forEachIndexed { idx, accName ->
                val type = accountTypes[accName] ?: AccountType.BANK_LOAN
                val id = dao.insertAccount(
                    AccountEntity(
                        name = accName,
                        type = type.name,
                        institution = if (type == AccountType.BANK_LOAN) "Banco Agrícola" else "",
                        balance = 0.0,
                        originalPrincipal = if (accName == "Personal AG") 3850.0 else 100.0,
                        paymentDueDay = if (accName == "Personal AG") 9 else 14,
                        counterpartyName = if (accName == "préstamo mami2") "Mami" else "",
                        linkedAccountId = agricolaId,
                        colorIndex = (nonLoans.size + idx) % 10,
                    )
                )
                existingAccounts[accName] = id
            }

            // 2. Ensure all categories from the CSV exist in categories table
            val knownCategories = dao.getAllCategoriesOnce()
                .associateBy { "${it.isIncome}:${it.name.lowercase(Locale.ROOT)}" }
                .toMutableMap()

            rows.forEach { row ->
                if (row.txType == "Transferencia" || row.txType == "Pago de Tarjeta" || row.txType == "Fondos del préstamo") {
                    return@forEach
                }
                val normCat = normalizeCsvCategory(row.category, row.txType, row.amount)
                val isInc = row.amount > 0.0
                val key = "$isInc:${normCat.lowercase(Locale.ROOT)}"
                if (normCat.isNotBlank() && key !in knownCategories) {
                    val inserted = CategoryEntity(
                        name = normCat,
                        isIncome = isInc,
                        iconKey = inferIconKey(normCat),
                        colorIndex = knownCategories.size % 10,
                        monthlyBudget = 0.0,
                    )
                    val id = dao.insertCategory(inserted)
                    knownCategories[key] = inserted.copy(id = id)
                }
            }

            // 3. Pair double-entry transfers / card payments / loan funds and compute account balances
            val balances = mutableMapOf<Long, Double>()
            existingAccounts.values.forEach { balances[it] = 0.0 }

            val pairedTypes = setOf("Pago de Tarjeta", "Transferencia", "Fondos del préstamo")
            val consumed = BooleanArray(rows.size)

            for (i in rows.indices) {
                if (consumed[i]) continue
                val row = rows[i]
                val accId = existingAccounts[row.account] ?: continue
                val accType = accountTypes[row.account] ?: AccountType.DEBIT

                if (row.txType in pairedTypes && row.amount < 0.0) {
                    val absAmt = abs(row.amount)
                    val partnerIdx = ((i + 1) until rows.size).firstOrNull { j ->
                        !consumed[j] &&
                            rows[j].dateStr == row.dateStr &&
                            rows[j].txType == row.txType &&
                            rows[j].description.equals(row.description, ignoreCase = true) &&
                            rows[j].amount > 0.0 &&
                            abs(rows[j].amount - absAmt) < 0.005
                    }
                    if (partnerIdx != null) {
                        consumed[i] = true
                        consumed[partnerIdx] = true
                        val partner = rows[partnerIdx]
                        val destId = existingAccounts[partner.account] ?: continue
                        val destType = accountTypes[partner.account] ?: AccountType.DEBIT

                        val catLabel = when (row.txType) {
                            "Pago de Tarjeta" -> "Pago de Tarjeta"
                            "Fondos del préstamo" -> "Desembolso de Préstamo"
                            else -> "Transferencia"
                        }

                        balances[accId] = applyTransferSource(balances[accId] ?: 0.0, accType, absAmt)
                        balances[destId] = applyTransferDest(balances[destId] ?: 0.0, destType, absAmt)

                        dao.insertTransaction(
                            TransactionEntity(
                                title = row.description.ifBlank { "${row.account} → ${partner.account}" },
                                amount = absAmt,
                                type = TransactionType.TRANSFER.name,
                                category = catLabel,
                                accountId = accId,
                                destinationAccountId = destId,
                                dateMillis = row.dateMillis,
                                note = row.notes.ifBlank { partner.notes },
                            )
                        )
                        continue
                    }
                }

                if (row.txType in pairedTypes && row.amount > 0.0) {
                    val absAmt = abs(row.amount)
                    val negPartnerIdx = ((i + 1) until rows.size).firstOrNull { j ->
                        !consumed[j] &&
                            rows[j].dateStr == row.dateStr &&
                            rows[j].txType == row.txType &&
                            rows[j].description.equals(row.description, ignoreCase = true) &&
                            rows[j].amount < 0.0 &&
                            abs(abs(rows[j].amount) - absAmt) < 0.005
                    }
                    if (negPartnerIdx != null) {
                        consumed[i] = true
                        consumed[negPartnerIdx] = true
                        val negPartner = rows[negPartnerIdx]
                        val srcId = existingAccounts[negPartner.account] ?: continue
                        val srcType = accountTypes[negPartner.account] ?: AccountType.DEBIT

                        val catLabel = when (row.txType) {
                            "Pago de Tarjeta" -> "Pago de Tarjeta"
                            "Fondos del préstamo" -> "Desembolso de Préstamo"
                            else -> "Transferencia"
                        }

                        balances[srcId] = applyTransferSource(balances[srcId] ?: 0.0, srcType, absAmt)
                        balances[accId] = applyTransferDest(balances[accId] ?: 0.0, accType, absAmt)

                        dao.insertTransaction(
                            TransactionEntity(
                                title = row.description.ifBlank { "${negPartner.account} → ${row.account}" },
                                amount = absAmt,
                                type = TransactionType.TRANSFER.name,
                                category = catLabel,
                                accountId = srcId,
                                destinationAccountId = accId,
                                dateMillis = row.dateMillis,
                                note = row.notes.ifBlank { negPartner.notes },
                            )
                        )
                        continue
                    }
                }

                // Special case: Pago de préstamo matching préstamo mami2 -> collection transfer
                if (row.txType.equals("Pago de préstamo", ignoreCase = true) &&
                    row.description.equals("préstamo mami2", ignoreCase = true) &&
                    row.amount > 0.0
                ) {
                    val loanId = existingAccounts["préstamo mami2"]
                    if (loanId != null) {
                        consumed[i] = true
                        balances[loanId] = applyTransferSource(balances[loanId] ?: 0.0, AccountType.LENT_LOAN, row.amount)
                        balances[accId] = applyTransferDest(balances[accId] ?: 0.0, accType, row.amount)
                        dao.insertTransaction(
                            TransactionEntity(
                                title = "Cobro · ${row.description}",
                                amount = row.amount,
                                type = TransactionType.TRANSFER.name,
                                category = "Cobro de Préstamo",
                                accountId = loanId,
                                destinationAccountId = accId,
                                dateMillis = row.dateMillis,
                                note = row.notes,
                            )
                        )
                        continue
                    }
                }

                // Special case: Compra a meses (MSI) on credit cards
                if (row.txType.equals("Compra a meses", ignoreCase = true) && row.amount < 0.0) {
                    consumed[i] = true
                    val absAmt = abs(row.amount)
                    val categoryName = normalizeCsvCategory(row.category, row.txType, row.amount)
                    val months = 6
                    val planId = dao.insertInstallmentPlan(
                        InstallmentPlanEntity(
                            creditCardAccountId = accId,
                            description = row.description.ifBlank { categoryName },
                            totalAmount = absAmt,
                            totalMonths = months,
                            paidMonths = months, // Feb 2026 6-MSI plans completed by Aug 2026
                            monthlyAmount = absAmt / months,
                            category = categoryName,
                            startDateMillis = row.dateMillis,
                        )
                    )
                    balances[accId] = applySingle(balances[accId] ?: 0.0, accType, TransactionType.EXPENSE, absAmt)
                    dao.insertTransaction(
                        TransactionEntity(
                            title = row.description.ifBlank { categoryName },
                            amount = absAmt,
                            type = TransactionType.EXPENSE.name,
                            category = categoryName,
                            accountId = accId,
                            dateMillis = row.dateMillis,
                            note = row.notes.ifBlank { "Compra a $months MSI" },
                            installmentPlanId = planId,
                        )
                    )
                    continue
                }

                // Unpaired transfer/card-payment rows (e.g. partial CSV counterpart) must stay TRANSFER
                // so they never inflate income or expense totals in Calendar or Analytics
                if (row.txType in pairedTypes) {
                    consumed[i] = true
                    val absAmt = abs(row.amount)
                    val catLabel = when (row.txType) {
                        "Pago de Tarjeta" -> "Pago de Tarjeta"
                        "Fondos del préstamo" -> "Desembolso de Préstamo"
                        else -> "Transferencia"
                    }
                    if (row.amount < 0.0) {
                        balances[accId] = applyTransferSource(balances[accId] ?: 0.0, accType, absAmt)
                    } else {
                        balances[accId] = applyTransferDest(balances[accId] ?: 0.0, accType, absAmt)
                    }
                    dao.insertTransaction(
                        TransactionEntity(
                            title = row.description.ifBlank { catLabel },
                            amount = absAmt,
                            type = TransactionType.TRANSFER.name,
                            category = catLabel,
                            accountId = accId,
                            destinationAccountId = null,
                            dateMillis = row.dateMillis,
                            note = row.notes,
                        )
                    )
                    continue
                }

                // Single-entry income or expense
                consumed[i] = true
                val isExpense = row.amount < 0.0
                val absAmt = abs(row.amount)
                val txType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
                val categoryName = normalizeCsvCategory(row.category, row.txType, row.amount)

                balances[accId] = applySingle(balances[accId] ?: 0.0, accType, txType, absAmt)

                dao.insertTransaction(
                    TransactionEntity(
                        title = row.description.ifBlank { categoryName },
                        amount = absAmt,
                        type = txType.name,
                        category = categoryName,
                        accountId = accId,
                        dateMillis = row.dateMillis,
                        note = row.notes,
                    )
                )
            }

            // 4. Reconcile final balances to exact Neko balances as of 2026-11-22
            // (Neko's CSV export omits pre-Jan-2026 opening balances and manual adjustments)
            val reconciledNekoBalances = mapOf(
                // Débito ($2,311.60)
                "BTC" to 0.00,
                "Criptos" to 89.16,
                "FVP" to 664.35,
                "Multimoney" to 1136.00,
                "TOHKN" to 342.34,
                "USDT" to 0.00,
                "XTB" to 79.75,
                // Ahorros + Efectivo ($258.88)
                "Agricola" to 160.07,
                "Atlantida" to 14.95,
                "Binance Card" to 2.75,
                "CREDICOMER" to 0.19,
                "DAVIVIENDA" to 0.00,
                "Efectivo" to 15.25,
                "N1co" to 0.00,
                "NIU deb" to 65.20,
                "Nequi" to 0.47,
                // Tarjetas de Crédito ($222.89 deuda total)
                "DaviviendaClasic" to 0.00,
                "EMMA" to 0.00,
                "FarmaValue" to 0.00,
                "NIU cred" to 65.05,
                "ONE" to 57.94,
                "SIMAN" to 99.90,
                "Visa Gold" to 0.00,
                // Préstamos bancarios ($3,850.00)
                "Personal AG" to 3850.00,
                // Prestado / Por cobrar ($70.00)
                "préstamo mami2" to 70.00,
            )

            existingAccounts.forEach { (accName, accId) ->
                val current = dao.getAccountById(accId) ?: return@forEach
                val finalBal = reconciledNekoBalances[accName] ?: run {
                    val rawBal = balances[accId] ?: 0.0
                    if (current.accountType == AccountType.CREDIT ||
                        current.accountType == AccountType.BANK_LOAN ||
                        current.accountType == AccountType.LENT_LOAN
                    ) max(0.0, rawBal) else rawBal
                }
                val limit = if (current.accountType == AccountType.CREDIT) {
                    max(1000.0, finalBal * 2.0)
                } else current.creditLimit
                dao.updateAccount(current.copy(balance = finalBal, creditLimit = limit))
            }

            // 5. Seed recurring subscriptions & bills clearly identified in the full-year CSV
            val emmaId = existingAccounts["EMMA"]
            val nequiId = existingAccounts["Nequi"]
            val simanId = existingAccounts["SIMAN"]
            val recurringDefaults = listOfNotNull(
                emmaId?.let {
                    ScheduledPaymentEntity(
                        name = "Spotify",
                        kind = RecurringKind.SUBSCRIPTION.name,
                        amount = 11.99,
                        accountId = it,
                        category = "Suscripciones",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 4),
                    )
                },
                nequiId?.let {
                    ScheduledPaymentEntity(
                        name = "Neko",
                        kind = RecurringKind.SUBSCRIPTION.name,
                        amount = 1.99,
                        accountId = it,
                        category = "Suscripciones",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 15),
                    )
                },
                nequiId?.let {
                    ScheduledPaymentEntity(
                        name = "Google One",
                        kind = RecurringKind.SUBSCRIPTION.name,
                        amount = 4.99,
                        accountId = it,
                        category = "Suscripciones",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 21),
                    )
                },
                nequiId?.let {
                    ScheduledPaymentEntity(
                        name = "Netflix",
                        kind = RecurringKind.SUBSCRIPTION.name,
                        amount = 7.00,
                        accountId = it,
                        category = "Suscripciones",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 23),
                    )
                },
                emmaId?.let {
                    ScheduledPaymentEntity(
                        name = "Internet Movistar",
                        kind = RecurringKind.RECURRING_BILL.name,
                        amount = 25.00,
                        accountId = it,
                        category = "Servicios",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 1),
                    )
                },
                emmaId?.let {
                    ScheduledPaymentEntity(
                        name = "Internet Familia",
                        kind = RecurringKind.RECURRING_BILL.name,
                        amount = 29.99,
                        accountId = it,
                        category = "Familia",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 11),
                    )
                },
                emmaId?.let {
                    ScheduledPaymentEntity(
                        name = "Internet Claro",
                        kind = RecurringKind.RECURRING_BILL.name,
                        amount = 19.00,
                        accountId = it,
                        category = "Servicios",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 24),
                    )
                },
                emmaId?.let {
                    ScheduledPaymentEntity(
                        name = "Pago Universidad",
                        kind = RecurringKind.RECURRING_BILL.name,
                        amount = 81.00,
                        accountId = it,
                        category = "Educación",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 1),
                    )
                },
                simanId?.let {
                    ScheduledPaymentEntity(
                        name = "Seguro Médico",
                        kind = RecurringKind.RECURRING_BILL.name,
                        amount = 5.69,
                        accountId = it,
                        category = "Otros gastos",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.DECEMBER, 12),
                    )
                },
                agricolaId?.let {
                    ScheduledPaymentEntity(
                        name = "Pago de salario",
                        kind = RecurringKind.RECURRING_INCOME.name,
                        amount = 1002.26,
                        accountId = it,
                        category = "Salario",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.NOVEMBER, 29),
                        notes = "Ingreso mensual recurrente",
                    )
                },
            )
            recurringDefaults.forEach { dao.insertScheduledPayment(it) }
        }

        prefs.edit()
            .putBoolean(KEY_SEEDED_CSV, true)
            .putBoolean(KEY_SEEDED_RECURRING_INCOME, true)
            .apply()
    }

    private suspend fun ensureRecurringSalary(prefs: android.content.SharedPreferences, dao: FinanceDao) {
        if (prefs.getBoolean(KEY_SEEDED_RECURRING_INCOME, false)) return
        val existingScheduled = dao.getAllScheduledPaymentsOnce()
        val hasIncome = existingScheduled.any { it.recurringKind == RecurringKind.RECURRING_INCOME }
        if (!hasIncome) {
            val agricola = dao.getAllAccountsOnce().firstOrNull { it.name.equals("Agricola", ignoreCase = true) }
            if (agricola != null) {
                dao.insertScheduledPayment(
                    ScheduledPaymentEntity(
                        name = "Pago de salario",
                        kind = RecurringKind.RECURRING_INCOME.name,
                        amount = 1002.26,
                        accountId = agricola.id,
                        category = "Salario",
                        frequency = RecurrenceFrequency.MONTHLY.name,
                        nextDueDateMillis = makeDate(2026, Calendar.NOVEMBER, 29),
                        notes = "Ingreso mensual recurrente",
                    )
                )
            }
        }
        prefs.edit().putBoolean(KEY_SEEDED_RECURRING_INCOME, true).apply()
    }

    suspend fun ensureDefaultCategories(dao: FinanceDao) {
        if (dao.getAllCategoriesOnce().isNotEmpty()) return
        val defaults = listOf(
            // Expense categories
            CategoryEntity(name = "Restaurantes", isIncome = false, iconKey = "FOOD", colorIndex = 0),
            CategoryEntity(name = "Gustos", isIncome = false, iconKey = "SHOPPING", colorIndex = 1),
            CategoryEntity(name = "Día a día", isIncome = false, iconKey = "OTHER", colorIndex = 2),
            CategoryEntity(name = "Transporte", isIncome = false, iconKey = "TRANSPORT", colorIndex = 3),
            CategoryEntity(name = "Familia", isIncome = false, iconKey = "HOUSING", colorIndex = 4),
            CategoryEntity(name = "Servicios", isIncome = false, iconKey = "UTILITIES", colorIndex = 5),
            CategoryEntity(name = "Suscripciones", isIncome = false, iconKey = "SUBSCRIPTIONS", colorIndex = 6),
            CategoryEntity(name = "Educación", isIncome = false, iconKey = "EDUCATION", colorIndex = 7),
            CategoryEntity(name = "Entretenimiento", isIncome = false, iconKey = "ENTERTAINMENT", colorIndex = 8),
            CategoryEntity(name = "Salud", isIncome = false, iconKey = "HEALTH", colorIndex = 9),
            CategoryEntity(name = "Mandado", isIncome = false, iconKey = "FOOD", colorIndex = 1),
            CategoryEntity(name = "Ropa", isIncome = false, iconKey = "SHOPPING", colorIndex = 2),
            CategoryEntity(name = "Mascotas", isIncome = false, iconKey = "PETS", colorIndex = 3),
            CategoryEntity(name = "Salidas", isIncome = false, iconKey = "ENTERTAINMENT", colorIndex = 4),
            CategoryEntity(name = "Inversiones", isIncome = false, iconKey = "INVESTMENTS", colorIndex = 5),
            CategoryEntity(name = "Préstamo", isIncome = false, iconKey = "LOAN_COLLECTION", colorIndex = 6),
            CategoryEntity(name = "Otros gastos", isIncome = false, iconKey = "OTHER", colorIndex = 7),
            CategoryEntity(name = "Otros", isIncome = false, iconKey = "OTHER", colorIndex = 8),
            // Income categories
            CategoryEntity(name = "Salario", isIncome = true, iconKey = "SALARY", colorIndex = 1),
            CategoryEntity(name = "Freelance", isIncome = true, iconKey = "FREELANCE", colorIndex = 2),
            CategoryEntity(name = "Bonos y comisiones", isIncome = true, iconKey = "FREELANCE", colorIndex = 3),
            CategoryEntity(name = "Inversiones", isIncome = true, iconKey = "INVESTMENTS", colorIndex = 4),
            CategoryEntity(name = "Reembolso / Cashback", isIncome = true, iconKey = "FREELANCE", colorIndex = 6),
            CategoryEntity(name = "Cobro de Préstamo", isIncome = true, iconKey = "LOAN_COLLECTION", colorIndex = 7),
            CategoryEntity(name = "Beneficios del gobierno", isIncome = true, iconKey = "FREELANCE", colorIndex = 8),
            CategoryEntity(name = "Otros Ingresos", isIncome = true, iconKey = "OTHER", colorIndex = 5),
        )
        defaults.forEach { dao.insertCategory(it) }
    }

    private fun readCsvRows(context: Context): List<CsvRow> {
        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val altDateFormat = SimpleDateFormat("d/M/yyyy", Locale.US)
        val result = mutableListOf<CsvRow>()
        try {
            val content = context.assets.open("initial_transactions.csv")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .removePrefix("\uFEFF")

            val records = parseCsvRecords(content)
            for (recordIdx in 1 until records.size) {
                val parts = records[recordIdx]
                if (parts.size < 5) continue
                val dateStr = parts[0].trim()
                val txType = parts[1].trim()
                val desc = parts[2].trim()
                val amt = parts[3].trim().toDoubleOrNull() ?: 0.0
                val acc = parts[4].trim()
                val catGroup = parts.getOrNull(5)?.trim().orEmpty()
                val cat = parts.getOrNull(6)?.trim().orEmpty()
                val notes = parts.getOrNull(7)?.trim()?.replace(Regex("\\s*\\r?\\n\\s*"), " · ").orEmpty()

                val parsedDate = runCatching {
                    if (dateStr.contains("-")) isoDateFormat.parse(dateStr)?.time
                    else altDateFormat.parse(dateStr)?.time
                }.getOrNull() ?: System.currentTimeMillis()

                // Set time to noon so timezone offsets never shift the calendar day
                val noonMillis = Calendar.getInstance().apply {
                    timeInMillis = parsedDate
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                if (amt != 0.0 && acc.isNotBlank()) {
                    result.add(
                        CsvRow(
                            index = recordIdx,
                            dateStr = dateStr,
                            dateMillis = noonMillis,
                            txType = txType,
                            description = desc,
                            amount = amt,
                            account = acc,
                            categoryGroup = catGroup,
                            category = cat,
                            notes = notes,
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return result
    }

    /**
     * Full RFC-4180 CSV parser supporting quoted fields with commas, escaped quotes (""),
     * and embedded newlines.
     */
    private fun parseCsvRecords(input: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = input.length

        while (i < n) {
            val ch = input[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < n && input[i + 1] == '"') {
                        field.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(field.toString())
                        field.clear()
                    }
                    '\r' -> {
                        if (i + 1 < n && input[i + 1] == '\n') i++
                        currentRow.add(field.toString())
                        field.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            records.add(currentRow)
                        }
                        currentRow = mutableListOf()
                    }
                    '\n' -> {
                        currentRow.add(field.toString())
                        field.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            records.add(currentRow)
                        }
                        currentRow = mutableListOf()
                    }
                    else -> field.append(ch)
                }
            }
            i++
        }

        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString())
            if (currentRow.any { it.isNotBlank() }) {
                records.add(currentRow)
            }
        }
        return records
    }

    private fun normalizeCsvCategory(rawCategory: String, txType: String, amount: Double): String {
        val clean = rawCategory.trim()
        if (clean.isEmpty()) {
            return when {
                txType.equals("Cashback", ignoreCase = true) ||
                    txType.equals("Reembolso", ignoreCase = true) -> "Reembolso / Cashback"
                txType.equals("Pago de préstamo", ignoreCase = true) -> "Cobro de Préstamo"
                amount > 0.0 -> "Otros Ingresos"
                else -> "Otros"
            }
        }
        return when (clean.lowercase(Locale.ROOT)) {
            "gustos" -> "Gustos"
            "día a dia", "dia a dia", "día a día" -> "Día a día"
            "otros" -> if (amount > 0.0) "Otros Ingresos" else "Otros"
            "inversiones" -> "Inversiones"
            "otros gastos" -> "Otros gastos"
            "restaurantes" -> "Restaurantes"
            "transporte" -> "Transporte"
            "servicios" -> "Servicios"
            "suscripciones" -> "Suscripciones"
            "familia" -> "Familia"
            "educación", "educacion" -> "Educación"
            "entretenimiento" -> "Entretenimiento"
            "salud" -> "Salud"
            "salario" -> "Salario"
            "mandado" -> "Mandado"
            "ropa" -> "Ropa"
            "mascotas" -> "Mascotas"
            "salidas" -> "Salidas"
            "préstamo", "prestamo" -> "Préstamo"
            "freelance" -> "Freelance"
            "bonos y comisiones" -> "Bonos y comisiones"
            "beneficios del gobierno" -> "Beneficios del gobierno"
            else -> clean.replaceFirstChar { it.uppercase(Locale("es", "MX")) }
        }
    }

    private fun inferInstitution(name: String, type: AccountType): String = when {
        name.contains("Agricola", true) -> "Banco Agrícola"
        name.contains("Davivienda", true) -> "Davivienda"
        name.contains("Atlantida", true) -> "Banco Atlántida"
        name.contains("NIU", true) -> "NIU"
        name.contains("Binance", true) -> "Binance"
        name.contains("CREDICOMER", true) -> "Credicomer"
        name.equals("Multimoney", true) -> "MultiMoney"
        name.equals("Nequi", true) -> "Nequi"
        name.equals("SIMAN", true) -> "Almacenes Simán"
        name.equals("XTB", true) -> "XTB"
        type == AccountType.CASH -> "Efectivo"
        else -> ""
    }

    private fun inferIconKey(name: String): String = when (name.lowercase(Locale.ROOT)) {
        "restaurantes", "mandado", "alimentación" -> "FOOD"
        "transporte" -> "TRANSPORT"
        "servicios" -> "UTILITIES"
        "suscripciones" -> "SUBSCRIPTIONS"
        "familia", "vivienda y renta" -> "HOUSING"
        "educación" -> "EDUCATION"
        "entretenimiento", "salidas" -> "ENTERTAINMENT"
        "salud" -> "HEALTH"
        "ropa", "gustos", "compras" -> "SHOPPING"
        "mascotas" -> "PETS"
        "salario" -> "SALARY"
        "inversiones" -> "INVESTMENTS"
        "reembolso / cashback", "freelance", "bonos y comisiones", "beneficios del gobierno" -> "FREELANCE"
        "cobro de préstamo", "préstamo" -> "LOAN_COLLECTION"
        else -> "OTHER"
    }

    private fun applySingle(
        currentBalance: Double,
        accountType: AccountType,
        txType: TransactionType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> {
            if (txType == TransactionType.EXPENSE) currentBalance + amount
            else max(0.0, currentBalance - amount)
        }
        else -> {
            if (txType == TransactionType.INCOME) currentBalance + amount
            else currentBalance - amount
        }
    }

    private fun applyTransferSource(
        currentBalance: Double,
        accountType: AccountType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> currentBalance + amount
        AccountType.LENT_LOAN -> max(0.0, currentBalance - amount)
        else -> currentBalance - amount
    }

    private fun applyTransferDest(
        currentBalance: Double,
        accountType: AccountType,
        amount: Double,
    ): Double = when (accountType) {
        AccountType.CREDIT, AccountType.BANK_LOAN -> max(0.0, currentBalance - amount)
        else -> currentBalance + amount
    }

    private fun makeDate(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
