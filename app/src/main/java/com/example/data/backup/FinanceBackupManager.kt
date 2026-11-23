package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.InstallmentPlanEntity
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.notifications.NotificationPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FinanceBackupSnapshot(
    val schemaVersion: Int = FinanceBackupManager.CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val transactions: List<TransactionEntity>,
    val installmentPlans: List<InstallmentPlanEntity>,
    val scheduledPayments: List<ScheduledPaymentEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity> = emptyList(),
    val notificationPreferences: NotificationPreferences? = null,
) {
    val totalRecords: Int
        get() = accounts.size + transactions.size + installmentPlans.size +
            scheduledPayments.size + categories.size
}

object FinanceBackupManager {
    const val APP_IDENTIFIER = "SaldoClaro"
    const val CURRENT_SCHEMA_VERSION = 1
    private const val MAX_BACKUP_BYTES = 25 * 1024 * 1024 // 25 MB safety guard

    fun suggestedBackupFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(nowMillis))
        return "SaldoClaro_respaldo_$stamp.json"
    }

    fun suggestedCsvFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(nowMillis))
        return "SaldoClaro_movimientos_$stamp.csv"
    }

    fun serializeToJson(snapshot: FinanceBackupSnapshot): String {
        val root = JSONObject().apply {
            put("app", APP_IDENTIFIER)
            put("schemaVersion", snapshot.schemaVersion)
            put("exportedAtMillis", snapshot.exportedAtMillis)
            put(
                "exportedAtIso",
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(snapshot.exportedAtMillis)),
            )
            put(
                "counts",
                JSONObject().apply {
                    put("accounts", snapshot.accounts.size)
                    put("transactions", snapshot.transactions.size)
                    put("installmentPlans", snapshot.installmentPlans.size)
                    put("scheduledPayments", snapshot.scheduledPayments.size)
                    put("categories", snapshot.categories.size)
                    put("budgets", snapshot.budgets.size)
                },
            )
            put("accounts", JSONArray().apply {
                snapshot.accounts.forEach { acc ->
                    put(
                        JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("type", acc.type)
                            put("institution", acc.institution)
                            put("lastFourDigits", acc.lastFourDigits)
                            put("balance", acc.balance)
                            put("creditLimit", acc.creditLimit)
                            put("cutoffDay", acc.cutoffDay)
                            put("paymentDueDay", acc.paymentDueDay)
                            put("originalPrincipal", acc.originalPrincipal)
                            put("monthlyInstallment", acc.monthlyInstallment)
                            put("interestRate", acc.interestRate)
                            put("savingsGoal", acc.savingsGoal)
                            put("counterpartyName", acc.counterpartyName)
                            if (acc.linkedAccountId != null) {
                                put("linkedAccountId", acc.linkedAccountId)
                            } else {
                                put("linkedAccountId", JSONObject.NULL)
                            }
                            put("colorIndex", acc.colorIndex)
                            put("createdAt", acc.createdAt)
                        }
                    )
                }
            })
            put("transactions", JSONArray().apply {
                snapshot.transactions.forEach { tx ->
                    put(
                        JSONObject().apply {
                            put("id", tx.id)
                            put("title", tx.title)
                            put("amount", tx.amount)
                            put("type", tx.type)
                            put("category", tx.category)
                            put("accountId", tx.accountId)
                            if (tx.destinationAccountId != null) {
                                put("destinationAccountId", tx.destinationAccountId)
                            } else {
                                put("destinationAccountId", JSONObject.NULL)
                            }
                            put("dateMillis", tx.dateMillis)
                            put("note", tx.note)
                            if (tx.installmentPlanId != null) {
                                put("installmentPlanId", tx.installmentPlanId)
                            } else {
                                put("installmentPlanId", JSONObject.NULL)
                            }
                            if (tx.scheduledPaymentId != null) {
                                put("scheduledPaymentId", tx.scheduledPaymentId)
                            } else {
                                put("scheduledPaymentId", JSONObject.NULL)
                            }
                        }
                    )
                }
            })
            put("installmentPlans", JSONArray().apply {
                snapshot.installmentPlans.forEach { plan ->
                    put(
                        JSONObject().apply {
                            put("id", plan.id)
                            put("creditCardAccountId", plan.creditCardAccountId)
                            put("description", plan.description)
                            put("totalAmount", plan.totalAmount)
                            put("totalMonths", plan.totalMonths)
                            put("paidMonths", plan.paidMonths)
                            put("monthlyAmount", plan.monthlyAmount)
                            put("category", plan.category)
                            put("startDateMillis", plan.startDateMillis)
                        }
                    )
                }
            })
            put("scheduledPayments", JSONArray().apply {
                snapshot.scheduledPayments.forEach { item ->
                    put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("name", item.name)
                            put("kind", item.kind)
                            put("amount", item.amount)
                            put("accountId", item.accountId)
                            put("category", item.category)
                            put("frequency", item.frequency)
                            put("nextDueDateMillis", item.nextDueDateMillis)
                            put("isActive", item.isActive)
                            put("notes", item.notes)
                        }
                    )
                }
            })
            put("categories", JSONArray().apply {
                snapshot.categories.forEach { cat ->
                    put(
                        JSONObject().apply {
                            put("id", cat.id)
                            put("name", cat.name)
                            put("isIncome", cat.isIncome)
                            put("iconKey", cat.iconKey)
                            put("colorIndex", cat.colorIndex)
                            put("monthlyBudget", cat.monthlyBudget)
                        }
                    )
                }
            })
            put("budgets", JSONArray().apply {
                snapshot.budgets.forEach { b ->
                    put(
                        JSONObject().apply {
                            put("category", b.category)
                            put("monthlyLimit", b.monthlyLimit)
                            put("updatedAt", b.updatedAt)
                        }
                    )
                }
            })
            snapshot.notificationPreferences?.let { prefs ->
                put(
                    "notificationPreferences",
                    JSONObject().apply {
                        put("enabled", prefs.enabled)
                        put("notifyBillsAndSubscriptions", prefs.notifyBillsAndSubscriptions)
                        put("notifyRecurringIncomes", prefs.notifyRecurringIncomes)
                        put("notifyCardCutoffs", prefs.notifyCardCutoffs)
                        put("notifyCardDueDates", prefs.notifyCardDueDates)
                        put("notifyLoanPayments", prefs.notifyLoanPayments)
                    },
                )
            }
        }
        return root.toString(2)
    }

    fun parseFromJson(rawJson: String): FinanceBackupSnapshot {
        val trimmed = rawJson.trim().removePrefix("\uFEFF")
        require(trimmed.isNotEmpty()) { "El archivo seleccionado está vacío." }
        val root = try {
            JSONObject(trimmed)
        } catch (_: Exception) {
            throw IllegalArgumentException("El archivo no tiene un formato JSON válido de SaldoClaro.")
        }

        val appTag = root.optString("app", "")
        val hasCoreArrays = root.has("accounts") && root.has("transactions")
        require(appTag == APP_IDENTIFIER || hasCoreArrays) {
            "Este archivo no corresponde a un respaldo de SaldoClaro."
        }

        val schemaVersion = root.optInt("schemaVersion", 1)
        require(schemaVersion in 1..CURRENT_SCHEMA_VERSION) {
            "Versión de respaldo no compatible ($schemaVersion). Actualiza SaldoClaro para importarlo."
        }

        val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())

        val accounts = parseAccounts(root.optJSONArray("accounts"))
        val transactions = parseTransactions(root.optJSONArray("transactions"))
        val installmentPlans = parseInstallmentPlans(root.optJSONArray("installmentPlans"))
        val scheduledPayments = parseScheduledPayments(root.optJSONArray("scheduledPayments"))
        val categories = parseCategories(root.optJSONArray("categories"))
        val budgets = parseBudgets(root.optJSONArray("budgets"))
        val notifPrefs = root.optJSONObject("notificationPreferences")?.let { obj ->
            NotificationPreferences(
                enabled = obj.optBoolean("enabled", true),
                notifyBillsAndSubscriptions = obj.optBoolean("notifyBillsAndSubscriptions", true),
                notifyRecurringIncomes = obj.optBoolean("notifyRecurringIncomes", true),
                notifyCardCutoffs = obj.optBoolean("notifyCardCutoffs", true),
                notifyCardDueDates = obj.optBoolean("notifyCardDueDates", true),
                notifyLoanPayments = obj.optBoolean("notifyLoanPayments", true),
            )
        }

        return FinanceBackupSnapshot(
            schemaVersion = schemaVersion,
            exportedAtMillis = exportedAtMillis,
            accounts = accounts,
            transactions = transactions,
            installmentPlans = installmentPlans,
            scheduledPayments = scheduledPayments,
            categories = categories,
            budgets = budgets,
            notificationPreferences = notifPrefs,
        )
    }

    fun writeTextToUri(context: Context, uri: Uri, text: String) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo de destino para escritura.")
        output.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo leer el archivo seleccionado.")
        return input.use { stream ->
            val buffer = ByteArray(8192)
            val out = ByteArrayOutputStream()
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                total += read
                if (total > MAX_BACKUP_BYTES) {
                    throw IllegalArgumentException("El archivo es demasiado grande (límite 25 MB).")
                }
                out.write(buffer, 0, read)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }

    private fun parseAccounts(array: JSONArray?): List<AccountEntity> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                val rawType = obj.optString("type", AccountType.DEBIT.name)
                val normalizedType = AccountType.fromName(rawType).name
                add(
                    AccountEntity(
                        id = obj.optLong("id", 0L),
                        name = name,
                        type = normalizedType,
                        institution = obj.optString("institution", ""),
                        lastFourDigits = obj.optString("lastFourDigits", ""),
                        balance = obj.safeDouble("balance", 0.0),
                        creditLimit = obj.safeDouble("creditLimit", 0.0),
                        cutoffDay = obj.optInt("cutoffDay", 15).coerceIn(1, 31),
                        paymentDueDay = obj.optInt("paymentDueDay", 5).coerceIn(1, 31),
                        originalPrincipal = obj.safeDouble("originalPrincipal", 0.0),
                        monthlyInstallment = obj.safeDouble("monthlyInstallment", 0.0),
                        interestRate = obj.safeDouble("interestRate", 0.0),
                        savingsGoal = obj.safeDouble("savingsGoal", 0.0),
                        counterpartyName = obj.optString("counterpartyName", ""),
                        linkedAccountId = obj.optNullableLong("linkedAccountId"),
                        colorIndex = obj.optInt("colorIndex", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }
    }

    private fun parseTransactions(array: JSONArray?): List<TransactionEntity> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rawType = obj.optString("type", TransactionType.EXPENSE.name)
                val normalizedType = TransactionType.fromName(rawType).name
                add(
                    TransactionEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.optString("title", "Movimiento"),
                        amount = obj.safeDouble("amount", 0.0),
                        type = normalizedType,
                        category = obj.optString("category", "Otros").ifBlank { "Otros" },
                        accountId = obj.optLong("accountId", 0L),
                        destinationAccountId = obj.optNullableLong("destinationAccountId"),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        note = obj.optString("note", ""),
                        installmentPlanId = obj.optNullableLong("installmentPlanId"),
                        scheduledPaymentId = obj.optNullableLong("scheduledPaymentId"),
                    )
                )
            }
        }
    }

    private fun parseInstallmentPlans(array: JSONArray?): List<InstallmentPlanEntity> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val totalMonths = obj.optInt("totalMonths", 1).coerceAtLeast(1)
                add(
                    InstallmentPlanEntity(
                        id = obj.optLong("id", 0L),
                        creditCardAccountId = obj.optLong("creditCardAccountId", 0L),
                        description = obj.optString("description", "Compra MSI"),
                        totalAmount = obj.safeDouble("totalAmount", 0.0),
                        totalMonths = totalMonths,
                        paidMonths = obj.optInt("paidMonths", 0).coerceIn(0, totalMonths),
                        monthlyAmount = obj.safeDouble("monthlyAmount", 0.0),
                        category = obj.optString("category", "Compras").ifBlank { "Compras" },
                        startDateMillis = obj.optLong("startDateMillis", System.currentTimeMillis()),
                    )
                )
            }
        }
    }

    private fun parseScheduledPayments(array: JSONArray?): List<ScheduledPaymentEntity> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                val normalizedKind = RecurringKind.fromName(obj.optString("kind", RecurringKind.RECURRING_BILL.name)).name
                val normalizedFreq = RecurrenceFrequency.fromName(obj.optString("frequency", RecurrenceFrequency.MONTHLY.name)).name
                add(
                    ScheduledPaymentEntity(
                        id = obj.optLong("id", 0L),
                        name = name,
                        kind = normalizedKind,
                        amount = obj.safeDouble("amount", 0.0),
                        accountId = obj.optLong("accountId", 0L),
                        category = obj.optString("category", "Servicios").ifBlank { "Servicios" },
                        frequency = normalizedFreq,
                        nextDueDateMillis = obj.optLong("nextDueDateMillis", System.currentTimeMillis()),
                        isActive = obj.optBoolean("isActive", true),
                        notes = obj.optString("notes", ""),
                    )
                )
            }
        }
    }

    private fun parseCategories(array: JSONArray?): List<CategoryEntity> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                add(
                    CategoryEntity(
                        id = obj.optLong("id", 0L),
                        name = name,
                        isIncome = obj.optBoolean("isIncome", false),
                        iconKey = obj.optString("iconKey", "OTHER").ifBlank { "OTHER" },
                        colorIndex = obj.optInt("colorIndex", 0),
                        monthlyBudget = obj.safeDouble("monthlyBudget", 0.0).coerceAtLeast(0.0),
                    )
                )
            }
        }
    }

    private fun parseBudgets(array: JSONArray?): List<BudgetEntity> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val category = obj.optString("category", "").trim()
                if (category.isEmpty()) continue
                add(
                    BudgetEntity(
                        category = category,
                        monthlyLimit = obj.safeDouble("monthlyLimit", 0.0).coerceAtLeast(0.0),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    )
                )
            }
        }
    }

    private fun JSONObject.safeDouble(key: String, default: Double): Double {
        if (!has(key) || isNull(key)) return default
        val v = optDouble(key, default)
        return if (v.isFinite()) v else default
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key)
    }
}
