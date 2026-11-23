package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import java.util.Calendar
import java.util.Locale

private data class DayFlowTotals(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val loanCollected: Double = 0.0,
    val cardPayments: Double = 0.0,
    val transferCount: Int = 0,
    val transactions: List<TransactionEntity> = emptyList(),
) {
    val totalInflow: Double get() = income + loanCollected
}

private data class AccountDaySummary(
    val accountId: Long,
    val accountName: String,
    val account: AccountEntity?,
    val outflow: Double,
    val inflow: Double,
    val loanRepayment: Double,
)

private enum class DayCategoryFlowKind {
    INCOME,
    EXPENSE,
    LOAN_COLLECTION,
    TRANSFER,
}

private data class CategoryDaySummary(
    val categoryName: String,
    val flowKind: DayCategoryFlowKind,
    val total: Double,
    val count: Int,
)

@Composable
fun CalendarCashflowView(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    hideBalances: Boolean,
    onAddForDate: (Long) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onRequestDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val todayCal = remember { Calendar.getInstance() }
    val todayStart = remember { DateMath.startOfDay(todayCal.timeInMillis) }

    var displayedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedDayStart by remember { mutableLongStateOf(todayStart) }
    var includeCardPaymentsInCells by remember { mutableStateOf(true) }

    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.name.lowercase(Locale.ROOT) } }

    // Group all transactions by start-of-day
    val txByDay = remember(transactions, accountMap) {
        transactions.groupBy { DateMath.startOfDay(it.dateMillis) }.mapValues { (_, list) ->
            var inc = 0.0
            var exp = 0.0
            var loanCol = 0.0
            var cardPay = 0.0
            var transfers = 0
            list.forEach { tx ->
                when (tx.transactionType) {
                    TransactionType.INCOME -> inc += tx.amount
                    TransactionType.EXPENSE -> exp += tx.amount
                    TransactionType.TRANSFER -> {
                        if (isLoanRepaymentCollection(tx, accountMap)) {
                            loanCol += tx.amount
                        } else if (tx.categoryLabel.equals("Pago de Tarjeta", ignoreCase = true) ||
                            accountMap[tx.destinationAccountId]?.accountType == AccountType.CREDIT
                        ) {
                            cardPay += tx.amount
                        }
                        transfers += 1
                    }
                }
            }
            DayFlowTotals(
                income = inc,
                expense = exp,
                loanCollected = loanCol,
                cardPayments = cardPay,
                transferCount = transfers,
                transactions = list,
            )
        }
    }

    // Build month calendar matrix (Monday-first)
    val monthMeta = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthMillis = cal.timeInMillis
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOffset = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }
        val dayMillisList = (1..daysInMonth).map { day ->
            (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }.timeInMillis
        }
        Triple(monthMillis, firstDayOffset, dayMillisList)
    }
    val monthTitleMillis = monthMeta.first
    val leadingEmptyCells = monthMeta.second
    val monthDayMillis = monthMeta.third

    val monthTotals = remember(monthDayMillis, txByDay) {
        var inc = 0.0
        var exp = 0.0
        monthDayMillis.forEach { dayMs ->
            val info = txByDay[dayMs]
            if (info != null) {
                inc += info.totalInflow
                exp += info.expense
            }
        }
        Pair(inc, exp)
    }

    val selectedDayFlow = txByDay[selectedDayStart] ?: DayFlowTotals()

    // Account breakdown for the selected day
    val accountSummaries = remember(selectedDayFlow.transactions, accountMap) {
        val outMap = linkedMapOf<Long, Double>()
        val inMap = linkedMapOf<Long, Double>()
        val loanRepayMap = linkedMapOf<Long, Double>()
        val seenIds = linkedSetOf<Long>()

        selectedDayFlow.transactions.forEach { tx ->
            when (tx.transactionType) {
                TransactionType.EXPENSE -> {
                    seenIds.add(tx.accountId)
                    outMap[tx.accountId] = (outMap[tx.accountId] ?: 0.0) + tx.amount
                }
                TransactionType.INCOME -> {
                    seenIds.add(tx.accountId)
                    inMap[tx.accountId] = (inMap[tx.accountId] ?: 0.0) + tx.amount
                }
                TransactionType.TRANSFER -> {
                    val isLoanCol = isLoanRepaymentCollection(tx, accountMap)
                    seenIds.add(tx.accountId)
                    if (isLoanCol) {
                        loanRepayMap[tx.accountId] = (loanRepayMap[tx.accountId] ?: 0.0) + tx.amount
                    } else {
                        outMap[tx.accountId] = (outMap[tx.accountId] ?: 0.0) + tx.amount
                    }
                    tx.destinationAccountId?.let { destId ->
                        seenIds.add(destId)
                        inMap[destId] = (inMap[destId] ?: 0.0) + tx.amount
                    }
                }
            }
        }

        seenIds.map { id ->
            val acc = accountMap[id]
            AccountDaySummary(
                accountId = id,
                accountName = acc?.name ?: "Cuenta",
                account = acc,
                outflow = outMap[id] ?: 0.0,
                inflow = inMap[id] ?: 0.0,
                loanRepayment = loanRepayMap[id] ?: 0.0,
            )
        }.sortedByDescending { it.outflow + it.inflow + it.loanRepayment }
    }

    // Category breakdown for the selected day
    val categorySummaries = remember(selectedDayFlow.transactions, accountMap) {
        selectedDayFlow.transactions
            .groupBy { tx ->
                val kind = when (tx.transactionType) {
                    TransactionType.INCOME -> DayCategoryFlowKind.INCOME
                    TransactionType.EXPENSE -> DayCategoryFlowKind.EXPENSE
                    TransactionType.TRANSFER -> if (isLoanRepaymentCollection(tx, accountMap)) {
                        DayCategoryFlowKind.LOAN_COLLECTION
                    } else {
                        DayCategoryFlowKind.TRANSFER
                    }
                }
                Pair(tx.categoryLabel, kind)
            }
            .map { (key, list) ->
                CategoryDaySummary(
                    categoryName = key.first,
                    flowKind = key.second,
                    total = list.sumOf { it.amount },
                    count = list.size,
                )
            }
            .sortedByDescending { it.total }
    }

    fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        displayedYear = cal.get(Calendar.YEAR)
        displayedMonth = cal.get(Calendar.MONTH)
        selectedDayStart = DateMath.startOfDay(cal.timeInMillis)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Calendar Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Month navigation header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = { shiftMonth(-1) },
                            modifier = Modifier.testTag("calendar_prev_month"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Mes anterior",
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = FinanceFormatters.monthYear(monthTitleMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(
                                onClick = {
                                    displayedYear = todayCal.get(Calendar.YEAR)
                                    displayedMonth = todayCal.get(Calendar.MONTH)
                                    selectedDayStart = todayStart
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("calendar_jump_today"),
                            ) {
                                Text("Ir a hoy", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(
                            onClick = { shiftMonth(1) },
                            modifier = Modifier.testTag("calendar_next_month"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Mes siguiente",
                            )
                        }
                    }

                    // Visible month totals strip
                    val monthNet = monthTotals.first - monthTotals.second
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MiniFlowColumn(
                                label = "Ingresos mes",
                                value = FinanceFormatters.currency(monthTotals.first, hideBalances),
                                color = financeColors.income,
                                alignment = Alignment.Start,
                                modifier = Modifier.weight(1f),
                            )
                            MiniFlowColumn(
                                label = "Gastos mes",
                                value = FinanceFormatters.currency(monthTotals.second, hideBalances),
                                color = financeColors.expense,
                                alignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                            )
                            MiniFlowColumn(
                                label = "Balance",
                                value = FinanceFormatters.currency(monthNet, hideBalances, showSign = true),
                                color = if (monthNet >= 0) financeColors.income else financeColors.expense,
                                alignment = Alignment.End,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Toggle whether credit-card payments count in daily cell outflow (like Neko Flujo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Montos exactos (sin redondeo)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilterChip(
                            selected = includeCardPaymentsInCells,
                            onClick = { includeCardPaymentsInCells = !includeCardPaymentsInCells },
                            label = {
                                Text(
                                    text = if (includeCardPaymentsInCells) "Con pagos TDC" else "Solo gastos",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("calendar_toggle_card_payments"),
                        )
                    }

                    // Weekday headers (Lun - Dom)
                    val weekDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekDays.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Calendar Grid rows
                    val totalCells = leadingEmptyCells + monthDayMillis.size
                    val rowCount = (totalCells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayIndex = cellIndex - leadingEmptyCells
                                    if (dayIndex in monthDayMillis.indices) {
                                        val dayMs = monthDayMillis[dayIndex]
                                        val flow = txByDay[dayMs]
                                        val isSelected = dayMs == selectedDayStart
                                        val isToday = dayMs == todayStart
                                        val cellOutflow = (flow?.expense ?: 0.0) +
                                            if (includeCardPaymentsInCells) (flow?.cardPayments ?: 0.0) else 0.0
                                        CalendarDayCell(
                                            dayNumber = dayIndex + 1,
                                            income = flow?.totalInflow ?: 0.0,
                                            expense = cellOutflow,
                                            hasTransfers = (flow?.transferCount ?: 0) > 0,
                                            isSelected = isSelected,
                                            isToday = isToday,
                                            hideBalances = hideBalances,
                                            onClick = { selectedDayStart = dayMs },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("calendar_day_${dayIndex + 1}"),
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Summary Card
        item {
            val dayInflow = selectedDayFlow.totalInflow
            val dayOutflow = selectedDayFlow.expense +
                if (includeCardPaymentsInCells) selectedDayFlow.cardPayments else 0.0
            val dayNet = dayInflow - dayOutflow
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_day_summary_card"),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Resumen del día",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = FinanceFormatters.fullDate(selectedDayStart),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        FilledTonalButton(
                            onClick = { onAddForDate(selectedDayStart + 43_200_000L) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("calendar_add_for_day_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Daily Income / Expense / Net Totals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DayMetricBox(
                            label = "Ingresado",
                            value = FinanceFormatters.currency(dayInflow, hideBalances),
                            accent = financeColors.income,
                            container = financeColors.incomeContainer,
                            modifier = Modifier.weight(1f),
                        )
                        DayMetricBox(
                            label = if (includeCardPaymentsInCells && selectedDayFlow.cardPayments > 0.0) "Salidas" else "Gastado",
                            value = FinanceFormatters.currency(dayOutflow, hideBalances),
                            accent = financeColors.expense,
                            container = financeColors.expenseContainer,
                            modifier = Modifier.weight(1f),
                        )
                        DayMetricBox(
                            label = "Flujo neto",
                            value = FinanceFormatters.currency(dayNet, hideBalances, showSign = true),
                            accent = if (dayNet >= 0) financeColors.income else financeColors.expense,
                            container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (selectedDayFlow.cardPayments > 0.0) {
                        Surface(
                            color = financeColors.transferContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Pagos de tarjeta (gastos puros: ${FinanceFormatters.currency(selectedDayFlow.expense, hideBalances)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = financeColors.transfer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = FinanceFormatters.currency(selectedDayFlow.cardPayments, hideBalances),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                    color = financeColors.transfer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    if (selectedDayFlow.loanCollected > 0.0) {
                        Surface(
                            color = financeColors.incomeContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Cobros de préstamos recibidos (a favor)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = financeColors.income,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "+${FinanceFormatters.currency(selectedDayFlow.loanCollected, hideBalances)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                                    color = financeColors.income,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Breakdown by Account (De qué cuenta salió / a cuál entró)
                    if (accountSummaries.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "Resumen por cuenta",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            accountSummaries.forEach { accSum ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        if (accSum.account != null) {
                                            Icon(
                                                imageVector = accountTypeIcon(accSum.account.accountType),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = accSum.accountName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (accSum.inflow > 0.0) {
                                            Text(
                                                text = "+${FinanceFormatters.currency(accSum.inflow, hideBalances)}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                                fontWeight = FontWeight.Bold,
                                                color = financeColors.income,
                                            )
                                        }
                                        if (accSum.loanRepayment > 0.0) {
                                            Text(
                                                text = "Abono ${FinanceFormatters.currency(accSum.loanRepayment, hideBalances)}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        if (accSum.outflow > 0.0) {
                                            Text(
                                                text = "-${FinanceFormatters.currency(accSum.outflow, hideBalances)}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                                fontWeight = FontWeight.Bold,
                                                color = financeColors.expense,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Breakdown by Category
                    if (categorySummaries.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "Resumen por categoría",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categorySummaries.forEach { catSum ->
                                val meta = categoryMap[catSum.categoryName.lowercase(Locale.ROOT)]
                                val catColor = categoryColorByName(catSum.categoryName, meta?.colorIndex)
                                val catPrefix = when (catSum.flowKind) {
                                    DayCategoryFlowKind.INCOME, DayCategoryFlowKind.LOAN_COLLECTION -> "+"
                                    DayCategoryFlowKind.EXPENSE -> "-"
                                    DayCategoryFlowKind.TRANSFER -> ""
                                }
                                val catAmountColor = when (catSum.flowKind) {
                                    DayCategoryFlowKind.INCOME, DayCategoryFlowKind.LOAN_COLLECTION -> financeColors.income
                                    DayCategoryFlowKind.EXPENSE -> financeColors.expense
                                    DayCategoryFlowKind.TRANSFER -> financeColors.transfer
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = categoryIconByName(catSum.categoryName, meta?.iconKey),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${catSum.categoryName} (${catSum.count})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text(
                                        text = catPrefix + FinanceFormatters.currency(catSum.total, hideBalances),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.Bold,
                                        color = catAmountColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detailed Transactions List for Selected Day
        item {
            SectionHeader(
                title = "Detalle del día (${selectedDayFlow.transactions.size})",
            )
        }

        if (selectedDayFlow.transactions.isEmpty()) {
            item {
                EmptyFinanceCard(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Sin movimientos el ${FinanceFormatters.shortDate(selectedDayStart)}",
                    subtitle = "Selecciona otro día en el calendario o registra un movimiento en esta fecha.",
                    actionLabel = "Registrar en esta fecha",
                    onActionClick = { onAddForDate(selectedDayStart + 43_200_000L) },
                )
            }
        } else {
            items(selectedDayFlow.transactions, key = { "cal_tx_${it.id}" }) { tx ->
                val srcName = accountMap[tx.accountId]?.name ?: "Cuenta"
                val dstName = tx.destinationAccountId?.let { accountMap[it]?.name }
                val catMeta = categoryMap[tx.categoryLabel.lowercase(Locale.ROOT)]
                val catColor = categoryColorByName(tx.categoryLabel, catMeta?.colorIndex)
                val isLoanCol = isLoanRepaymentCollection(tx, accountMap)
                val amountColor = when {
                    tx.transactionType == TransactionType.INCOME || isLoanCol -> financeColors.income
                    tx.transactionType == TransactionType.EXPENSE -> financeColors.expense
                    else -> financeColors.transfer
                }
                val prefix = when {
                    tx.transactionType == TransactionType.INCOME || isLoanCol -> "+"
                    tx.transactionType == TransactionType.EXPENSE -> "-"
                    else -> ""
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onTransactionClick(tx) },
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = categoryIconByName(tx.categoryLabel, catMeta?.iconKey),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = when {
                                    isLoanCol && dstName != null -> "Cobro → $dstName (a favor)"
                                    dstName != null -> "$srcName → $dstName"
                                    else -> "$srcName · ${tx.categoryLabel}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (tx.note.isNotBlank()) {
                                Text(
                                    text = tx.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$prefix${FinanceFormatters.currency(tx.amount, hideBalances)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                fontWeight = FontWeight.Bold,
                                color = amountColor,
                            )
                            IconButton(
                                onClick = { onRequestDelete(tx) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Eliminar movimiento",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    income: Double,
    expense: Double,
    hasTransfers: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        income > 0.0 || expense > 0.0 || hasTransfers -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            if (hasTransfers) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(financeColors.transfer),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (income > 0.0) {
                Text(
                    text = if (hideBalances) "+•" else "+${exactCellAmount(income)}",
                    fontSize = 7.sp,
                    lineHeight = 9.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = financeColors.income,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
            if (expense > 0.0) {
                Text(
                    text = if (hideBalances) "-•" else "-${exactCellAmount(expense)}",
                    fontSize = 7.sp,
                    lineHeight = 9.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = financeColors.expense,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

private fun exactCellAmount(amount: Double): String {
    return String.format(Locale.US, "%.2f", amount)
}

@Composable
private fun MiniFlowColumn(
    label: String,
    value: String,
    color: Color,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DayMetricBox(
    label: String,
    value: String,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = container,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
