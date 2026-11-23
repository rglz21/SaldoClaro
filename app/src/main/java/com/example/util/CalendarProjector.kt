package com.example.util

import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringKind
import com.example.data.model.ScheduledPaymentEntity
import java.util.Calendar
import kotlin.math.min

enum class ProjectedEventKind(val labelEs: String, val shortBadge: String) {
    RECURRING_INCOME("Ingreso frecuente", "Ingreso"),
    SUBSCRIPTION("Suscripción", "Suscrip."),
    RECURRING_BILL("Pago frecuente", "Pago"),
    CARD_CUTOFF("Corte de tarjeta", "Corte"),
    CARD_PAYMENT_DUE("Pago límite TDC", "Pago TDC"),
    BANK_LOAN_PAYMENT("Cuota de préstamo", "Préstamo"),
    LENT_LOAN_COLLECTION("Cobro de préstamo", "Por cobrar");

    val isScheduledPayment: Boolean
        get() = this == RECURRING_INCOME || this == SUBSCRIPTION || this == RECURRING_BILL
}

data class ProjectedCalendarEvent(
    val id: String,
    val dateMillis: Long, // startOfDay
    val title: String,
    val subtitle: String,
    val kind: ProjectedEventKind,
    val amount: Double,
    val isInflow: Boolean,
    val isOutflow: Boolean,
    val isInformationalOnly: Boolean = false,
    val accountId: Long? = null,
    val accountName: String = "",
    val categoryName: String = "",
    val scheduledPaymentId: Long? = null,
)

object CalendarProjector {

    /**
     * Projects all active recurring bills, subscriptions, recurring incomes, credit card cutoffs,
     * credit card payment due dates, and active loan payments/collections for a given [year] & [month]
     * (Calendar.JANUARY..Calendar.DECEMBER).
     *
     * Past dates strictly before [todayStartMillis] are excluded (except an overdue scheduled
     * payment's nextDueDateMillis if it falls within the viewed month) so realized history is never duplicated.
     */
    fun projectMonth(
        year: Int,
        month: Int,
        accounts: List<AccountEntity>,
        scheduledPayments: List<ScheduledPaymentEntity>,
        todayStartMillis: Long = DateMath.startOfDay(),
    ): Map<Long, List<ProjectedCalendarEvent>> {
        val monthStartCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = monthStartCal.timeInMillis
        val daysInMonth = monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthEndCal = (monthStartCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
        }
        val monthEnd = monthEndCal.timeInMillis

        val accountMap = accounts.associateBy { it.id }
        val eventsByDay = linkedMapOf<Long, MutableList<ProjectedCalendarEvent>>()

        fun addEvent(event: ProjectedCalendarEvent) {
            eventsByDay.getOrPut(event.dateMillis) { mutableListOf() }.add(event)
        }

        // 1. Recurring incomes, subscriptions, and bills
        scheduledPayments.filter { it.isActive }.forEach { item ->
            val accName = accountMap[item.accountId]?.name ?: "Sin cuenta"
            val kind = when (item.recurringKind) {
                RecurringKind.RECURRING_INCOME -> ProjectedEventKind.RECURRING_INCOME
                RecurringKind.SUBSCRIPTION -> ProjectedEventKind.SUBSCRIPTION
                RecurringKind.RECURRING_BILL -> ProjectedEventKind.RECURRING_BILL
            }
            val firstDue = DateMath.startOfDay(item.nextDueDateMillis)
            val occurrences = collectRecurringOccurrencesInRange(
                anchorMillis = firstDue,
                frequency = item.recurrenceFrequency,
                rangeStart = monthStart,
                rangeEnd = monthEnd,
                minAllowedMillis = todayStartMillis,
            )
            occurrences.forEachIndexed { idx, occMillis ->
                addEvent(
                    ProjectedCalendarEvent(
                        id = "sched_${item.id}_${occMillis}_$idx",
                        dateMillis = occMillis,
                        title = item.name,
                        subtitle = "${item.recurrenceFrequency.labelEs} · $accName",
                        kind = kind,
                        amount = item.amount,
                        isInflow = item.recurringKind.isIncome,
                        isOutflow = !item.recurringKind.isIncome,
                        isInformationalOnly = false,
                        accountId = item.accountId,
                        accountName = accName,
                        categoryName = item.categoryLabel,
                        scheduledPaymentId = item.id,
                    )
                )
            }
        }

        // 2. Credit card cutoff dates & payment due dates
        accounts.filter { it.accountType == AccountType.CREDIT }.forEach { card ->
            val cutoffDate = dayInMonthMillis(year, month, card.cutoffDay, daysInMonth)
            if (cutoffDate >= todayStartMillis) {
                val balanceNote = if (card.balance > 0.0) {
                    "Saldo actual: ${FinanceFormatters.currency(card.balance)}"
                } else {
                    "Sin saldo pendiente"
                }
                addEvent(
                    ProjectedCalendarEvent(
                        id = "cc_cutoff_${card.id}_$cutoffDate",
                        dateMillis = cutoffDate,
                        title = "Corte · ${card.name}",
                        subtitle = "Día de corte (${card.cutoffDay}) · $balanceNote",
                        kind = ProjectedEventKind.CARD_CUTOFF,
                        amount = card.balance,
                        isInflow = false,
                        isOutflow = false,
                        isInformationalOnly = true,
                        accountId = card.id,
                        accountName = card.name,
                        categoryName = "Corte de Tarjeta",
                    )
                )
            }

            val dueDate = dayInMonthMillis(year, month, card.paymentDueDay, daysInMonth)
            if (dueDate >= todayStartMillis) {
                val nextDue = DateMath.nextOccurrenceOfDayOfMonth(card.paymentDueDay).dateMillis
                val isImmediateNextDue = dueDate == nextDue
                val hasCurrentDebt = card.balance > 0.0
                val amountToShow = if (isImmediateNextDue && hasCurrentDebt) card.balance else 0.0
                addEvent(
                    ProjectedCalendarEvent(
                        id = "cc_due_${card.id}_$dueDate",
                        dateMillis = dueDate,
                        title = "Pago límite · ${card.name}",
                        subtitle = if (amountToShow > 0.0) {
                            "Fecha límite TDC (día ${card.paymentDueDay})"
                        } else {
                            "Fecha límite TDC (día ${card.paymentDueDay}) · Saldo $0.00"
                        },
                        kind = ProjectedEventKind.CARD_PAYMENT_DUE,
                        amount = amountToShow,
                        isInflow = false,
                        isOutflow = amountToShow > 0.0,
                        isInformationalOnly = amountToShow <= 0.0,
                        accountId = card.id,
                        accountName = card.name,
                        categoryName = "Pago de Tarjeta",
                    )
                )
            }
        }

        // 3. Bank loan future monthly payments
        val todayCal = Calendar.getInstance().apply { timeInMillis = todayStartMillis }
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)

        accounts.filter { it.accountType == AccountType.BANK_LOAN && it.balance > 0.0 }.forEach { loan ->
            val dueDate = dayInMonthMillis(year, month, loan.paymentDueDay, daysInMonth)
            if (dueDate >= todayStartMillis) {
                val monthsDelta = ((year - todayYear) * 12 + (month - todayMonth)).coerceAtLeast(0)
                val installment = loan.monthlyInstallment
                val projectedAmount = if (installment > 0.0) {
                    val estimatedRemaining = (loan.balance - installment * monthsDelta).coerceAtLeast(0.0)
                    if (estimatedRemaining <= 0.0) 0.0 else min(installment, estimatedRemaining)
                } else {
                    0.0
                }
                // If loan would already be paid off before this month, stop projecting
                if (installment > 0.0 && projectedAmount <= 0.0) return@forEach

                addEvent(
                    ProjectedCalendarEvent(
                        id = "bank_loan_${loan.id}_$dueDate",
                        dateMillis = dueDate,
                        title = "Cuota · ${loan.name}",
                        subtitle = if (projectedAmount > 0.0) {
                            "Préstamo bancario · día ${loan.paymentDueDay}"
                        } else {
                            "Pago de préstamo · saldo ${FinanceFormatters.currency(loan.balance)}"
                        },
                        kind = ProjectedEventKind.BANK_LOAN_PAYMENT,
                        amount = if (projectedAmount > 0.0) projectedAmount else loan.balance,
                        isInflow = false,
                        isOutflow = projectedAmount > 0.0,
                        isInformationalOnly = projectedAmount <= 0.0,
                        accountId = loan.id,
                        accountName = loan.name,
                        categoryName = "Pago de Préstamo",
                    )
                )
            }
        }

        // 4. Lent loan future collections
        accounts.filter { it.accountType == AccountType.LENT_LOAN && it.balance > 0.0 }.forEach { loan ->
            val dueDate = dayInMonthMillis(year, month, loan.paymentDueDay, daysInMonth)
            if (dueDate >= todayStartMillis) {
                val nextDue = DateMath.nextOccurrenceOfDayOfMonth(loan.paymentDueDay).dateMillis
                val monthsDelta = ((year - todayYear) * 12 + (month - todayMonth)).coerceAtLeast(0)
                val installment = loan.monthlyInstallment
                val isImmediateNext = dueDate == nextDue

                val projectedAmount = when {
                    installment > 0.0 -> {
                        val rem = (loan.balance - installment * monthsDelta).coerceAtLeast(0.0)
                        if (rem <= 0.0) return@forEach
                        min(installment, rem)
                    }
                    isImmediateNext -> loan.balance
                    else -> return@forEach // Without fixed installment, show reminder only on next due date
                }
                val borrowerLabel = loan.counterpartyName.ifBlank { loan.name }
                addEvent(
                    ProjectedCalendarEvent(
                        id = "lent_loan_${loan.id}_$dueDate",
                        dateMillis = dueDate,
                        title = "Cobro · ${loan.name}",
                        subtitle = "Por cobrar ($borrowerLabel) · día ${loan.paymentDueDay}",
                        kind = ProjectedEventKind.LENT_LOAN_COLLECTION,
                        amount = projectedAmount,
                        isInflow = installment > 0.0,
                        isOutflow = false,
                        isInformationalOnly = installment <= 0.0,
                        accountId = loan.id,
                        accountName = loan.name,
                        categoryName = "Cobro de Préstamo",
                    )
                )
            }
        }

        return eventsByDay
    }

    /**
     * Returns all projected events due on an exact day (used by notification worker & daily summary).
     */
    fun eventsDueOnDay(
        dayStartMillis: Long,
        accounts: List<AccountEntity>,
        scheduledPayments: List<ScheduledPaymentEntity>,
    ): List<ProjectedCalendarEvent> {
        val cal = Calendar.getInstance().apply { timeInMillis = dayStartMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val monthEvents = projectMonth(
            year = year,
            month = month,
            accounts = accounts,
            scheduledPayments = scheduledPayments,
            todayStartMillis = dayStartMillis,
        )
        return monthEvents[dayStartMillis].orEmpty()
    }

    /**
     * Returns upcoming projected events over the next [withinDays] days starting from [todayStartMillis].
     */
    fun nextUpcomingEvents(
        todayStartMillis: Long,
        accounts: List<AccountEntity>,
        scheduledPayments: List<ScheduledPaymentEntity>,
        withinDays: Int = 14,
    ): List<ProjectedCalendarEvent> {
        val endMillis = todayStartMillis + withinDays * 86_400_000L
        val startCal = Calendar.getInstance().apply { timeInMillis = todayStartMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        val collected = mutableListOf<ProjectedCalendarEvent>()
        val cursor = (startCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val endMonth = (endCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }

        while (!cursor.after(endMonth)) {
            val map = projectMonth(
                year = cursor.get(Calendar.YEAR),
                month = cursor.get(Calendar.MONTH),
                accounts = accounts,
                scheduledPayments = scheduledPayments,
                todayStartMillis = todayStartMillis,
            )
            map.forEach { (dayMs, list) ->
                if (dayMs in todayStartMillis..endMillis) {
                    collected.addAll(list)
                }
            }
            cursor.add(Calendar.MONTH, 1)
        }
        return collected.sortedWith(compareBy<ProjectedCalendarEvent> { it.dateMillis }.thenBy { it.kind.ordinal })
    }

    private fun dayInMonthMillis(year: Int, month: Int, desiredDay: Int, daysInMonth: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, desiredDay.coerceIn(1, daysInMonth))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun collectRecurringOccurrencesInRange(
        anchorMillis: Long,
        frequency: RecurrenceFrequency,
        rangeStart: Long,
        rangeEnd: Long,
        minAllowedMillis: Long,
    ): List<Long> {
        val results = mutableListOf<Long>()
        val anchorCal = Calendar.getInstance().apply { timeInMillis = anchorMillis }
        val anchorDayOfMonth = anchorCal.get(Calendar.DAY_OF_MONTH)

        var cursor = anchorMillis
        var safetySteps = 0
        while (cursor <= rangeEnd && safetySteps < 200) {
            if (cursor in rangeStart..rangeEnd && (cursor >= minAllowedMillis || cursor == anchorMillis)) {
                results.add(cursor)
            }
            val nextCal = Calendar.getInstance().apply { timeInMillis = cursor }
            when (frequency) {
                RecurrenceFrequency.WEEKLY -> nextCal.add(Calendar.DAY_OF_YEAR, 7)
                RecurrenceFrequency.BIWEEKLY -> nextCal.add(Calendar.DAY_OF_YEAR, 14)
                RecurrenceFrequency.MONTHLY -> {
                    nextCal.set(Calendar.DAY_OF_MONTH, 1)
                    nextCal.add(Calendar.MONTH, 1)
                    val maxD = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    nextCal.set(Calendar.DAY_OF_MONTH, anchorDayOfMonth.coerceAtMost(maxD))
                }
                RecurrenceFrequency.BIMONTHLY -> {
                    nextCal.set(Calendar.DAY_OF_MONTH, 1)
                    nextCal.add(Calendar.MONTH, 2)
                    val maxD = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    nextCal.set(Calendar.DAY_OF_MONTH, anchorDayOfMonth.coerceAtMost(maxD))
                }
                RecurrenceFrequency.YEARLY -> {
                    nextCal.set(Calendar.DAY_OF_MONTH, 1)
                    nextCal.add(Calendar.YEAR, 1)
                    val maxD = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    nextCal.set(Calendar.DAY_OF_MONTH, anchorDayOfMonth.coerceAtMost(maxD))
                }
            }
            nextCal.set(Calendar.HOUR_OF_DAY, 0)
            nextCal.set(Calendar.MINUTE, 0)
            nextCal.set(Calendar.SECOND, 0)
            nextCal.set(Calendar.MILLISECOND, 0)
            cursor = nextCal.timeInMillis
            safetySteps++
        }
        return results
    }
}
