package com.example.util

import com.example.data.model.AccountEntity
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FinanceFormatters {
    private val esLocale = Locale("es", "MX")

    fun currency(amount: Double, hidden: Boolean = false, showSign: Boolean = false): String {
        if (hidden) return "$ •••••"
        val formatter = NumberFormat.getCurrencyInstance(esLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formattedAbs = formatter.format(abs(amount))
        return when {
            amount < 0 -> "-$formattedAbs"
            showSign && amount > 0 -> "+$formattedAbs"
            else -> formattedAbs
        }
    }

    fun compactCurrency(amount: Double): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000 -> String.format(esLocale, "%s$%.1fM", sign, absVal / 1_000_000.0)
            absVal >= 1_000 -> String.format(esLocale, "%s$%.1fk", sign, absVal / 1_000.0)
            else -> String.format(esLocale, "%s$%.0f", sign, absVal)
        }
    }

    fun percent(ratio: Double): String {
        return String.format(esLocale, "%.0f%%", ratio * 100.0)
    }

    fun shortDate(millis: Long): String {
        val fmt = SimpleDateFormat("d MMM", esLocale)
        return fmt.format(Date(millis))
    }

    fun fullDate(millis: Long): String {
        val fmt = SimpleDateFormat("d MMM yyyy", esLocale)
        return fmt.format(Date(millis))
    }

    fun dayMonthYear(millis: Long): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", esLocale)
        return fmt.format(Date(millis))
    }

    fun monthYear(millis: Long): String {
        val fmt = SimpleDateFormat("MMMM yyyy", esLocale)
        return fmt.format(Date(millis)).replaceFirstChar { it.uppercase(esLocale) }
    }

    fun buildCsvReport(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
    ): String {
        val accountNames = accounts.associate { it.id to it.name }
        val sb = StringBuilder()
        sb.appendLine("Fecha,Tipo,Título,Categoría,Cuenta Origen,Cuenta Destino,Monto,Nota")
        transactions.forEach { tx ->
            val date = dayMonthYear(tx.dateMillis)
            val from = accountNames[tx.accountId].orEmpty().replace(",", " ")
            val to = tx.destinationAccountId?.let { accountNames[it] }.orEmpty().replace(",", " ")
            val title = tx.title.replace(",", " ")
            val note = tx.note.replace(",", " ")
            sb.appendLine(
                "$date,${tx.transactionType.labelEs},$title,${tx.categoryLabel},$from,$to,${String.format(Locale.US, "%.2f", tx.amount)},$note"
            )
        }
        return sb.toString()
    }
}

data class NextDateInfo(
    val dateMillis: Long,
    val daysRemaining: Int,
)

object DateMath {
    private const val DAY_MILLIS = 86_400_000L

    fun startOfDay(millis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfCurrentWeek(): Long {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfCurrentMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfCurrentYear(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun daysInCurrentMonth(): Int {
        return Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun currentDayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    fun daysBetween(fromMillis: Long, toMillis: Long): Int {
        val diff = startOfDay(toMillis) - startOfDay(fromMillis)
        return (diff / DAY_MILLIS).toInt()
    }

    fun nextOccurrenceOfDayOfMonth(dayOfMonth: Int): NextDateInfo {
        val clampedDay = dayOfMonth.coerceIn(1, 31)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val candidate = (today.clone() as Calendar).apply {
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, clampedDay.coerceAtMost(maxDay))
        }
        if (candidate.timeInMillis < today.timeInMillis) {
            candidate.add(Calendar.MONTH, 1)
            val maxDay = candidate.getActualMaximum(Calendar.DAY_OF_MONTH)
            candidate.set(Calendar.DAY_OF_MONTH, clampedDay.coerceAtMost(maxDay))
        }
        val days = ((candidate.timeInMillis - today.timeInMillis) / DAY_MILLIS).toInt()
        return NextDateInfo(dateMillis = candidate.timeInMillis, daysRemaining = days)
    }

    fun dateFromDayOfMonth(dayOfMonth: Int): Long {
        return nextOccurrenceOfDayOfMonth(dayOfMonth).dateMillis
    }

    fun advanceDate(currentMillis: Long, frequency: RecurrenceFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        when (frequency) {
            RecurrenceFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
            RecurrenceFrequency.BIWEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 14)
            RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceFrequency.BIMONTHLY -> cal.add(Calendar.MONTH, 2)
            RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}
