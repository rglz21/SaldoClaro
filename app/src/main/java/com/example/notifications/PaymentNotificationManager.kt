package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.FinanceDatabase
import com.example.util.CalendarProjector
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import com.example.util.ProjectedCalendarEvent
import com.example.util.ProjectedEventKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NotificationPreferences(
    val enabled: Boolean = true,
    val notifyBillsAndSubscriptions: Boolean = true,
    val notifyRecurringIncomes: Boolean = true,
    val notifyCardCutoffs: Boolean = true,
    val notifyCardDueDates: Boolean = true,
    val notifyLoanPayments: Boolean = true,
)

object PaymentNotificationManager {
    const val CHANNEL_ID = "saldoclaro_payment_reminders"
    const val EXTRA_OPEN_CALENDAR = "extra_open_calendar"

    private const val PREFS_NAME = "saldoclaro_notification_prefs"
    private const val KEY_ENABLED = "notif_enabled"
    private const val KEY_BILLS = "notif_bills"
    private const val KEY_INCOMES = "notif_incomes"
    private const val KEY_CUTOFFS = "notif_cutoffs"
    private const val KEY_CARD_DUE = "notif_card_due"
    private const val KEY_LOANS = "notif_loans"
    private const val KEY_NOTIFIED_PREFIX = "notified_day_"
    private const val ALARM_REQUEST_CODE = 9042

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de pagos y vencimientos",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Notifica el día de pago de suscripciones, pagos frecuentes, cortes/pagos de tarjeta, préstamos e ingresos programados."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun getPreferences(context: Context): NotificationPreferences {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return NotificationPreferences(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            notifyBillsAndSubscriptions = prefs.getBoolean(KEY_BILLS, true),
            notifyRecurringIncomes = prefs.getBoolean(KEY_INCOMES, true),
            notifyCardCutoffs = prefs.getBoolean(KEY_CUTOFFS, true),
            notifyCardDueDates = prefs.getBoolean(KEY_CARD_DUE, true),
            notifyLoanPayments = prefs.getBoolean(KEY_LOANS, true),
        )
    }

    fun savePreferences(context: Context, updated: NotificationPreferences) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, updated.enabled)
            .putBoolean(KEY_BILLS, updated.notifyBillsAndSubscriptions)
            .putBoolean(KEY_INCOMES, updated.notifyRecurringIncomes)
            .putBoolean(KEY_CUTOFFS, updated.notifyCardCutoffs)
            .putBoolean(KEY_CARD_DUE, updated.notifyCardDueDates)
            .putBoolean(KEY_LOANS, updated.notifyLoanPayments)
            .apply()

        if (updated.enabled && hasNotificationPermission(context)) {
            scheduleDailyReminder(context)
        } else {
            cancelDailyReminder(context)
        }
    }

    fun scheduleDailyReminder(context: Context) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)

        val nextTrigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTrigger,
            AlarmManager.INTERVAL_DAY,
            pendingIntent,
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildAlarmPendingIntent(context))
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, PaymentReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    suspend fun checkAndNotifyDueToday(context: Context, forceResend: Boolean = false): Int {
        val prefs = getPreferences(context)
        if (!prefs.enabled || !hasNotificationPermission(context)) return 0

        ensureChannel(context)
        val dao = FinanceDatabase.getInstance(context).financeDao()
        val accounts = dao.getAllAccountsOnce()
        val scheduled = dao.getAllScheduledPaymentsOnce()
        val todayStart = DateMath.startOfDay()

        val dueEvents = CalendarProjector.eventsDueOnDay(
            dayStartMillis = todayStart,
            accounts = accounts,
            scheduledPayments = scheduled,
        ).filter { matchesPreferences(it, prefs) }

        if (dueEvents.isEmpty()) return 0

        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(todayStart))
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyNotified = if (forceResend) emptySet() else {
            sp.getStringSet(KEY_NOTIFIED_PREFIX + dayKey, emptySet()).orEmpty()
        }

        val toNotify = dueEvents.filter { it.id !in alreadyNotified }
        if (toNotify.isEmpty()) return 0

        toNotify.forEachIndexed { idx, event ->
            postEventNotification(
                context = context,
                notificationId = (event.id.hashCode() and 0x7FFFFFFF) + idx,
                title = buildNotificationTitle(event),
                body = buildNotificationBody(event),
            )
        }

        val updatedSet = alreadyNotified.toMutableSet().apply {
            addAll(toNotify.map { it.id })
        }
        sp.edit().putStringSet(KEY_NOTIFIED_PREFIX + dayKey, updatedSet).apply()
        return toNotify.size
    }

    suspend fun sendPreviewNotification(context: Context): String {
        if (!hasNotificationPermission(context)) {
            return "Activa el permiso de notificaciones primero"
        }
        ensureChannel(context)
        val dao = FinanceDatabase.getInstance(context).financeDao()
        val accounts = dao.getAllAccountsOnce()
        val scheduled = dao.getAllScheduledPaymentsOnce()
        val prefs = getPreferences(context)
        val todayStart = DateMath.startOfDay()

        val upcoming = CalendarProjector.nextUpcomingEvents(
            todayStartMillis = todayStart,
            accounts = accounts,
            scheduledPayments = scheduled,
            withinDays = 31,
        ).filter { matchesPreferences(it, prefs) }

        val first = upcoming.firstOrNull()
        return if (first != null) {
            val isToday = first.dateMillis == todayStart
            val datePrefix = if (isToday) "Hoy" else FinanceFormatters.shortDate(first.dateMillis)
            val title = "Próximo recordatorio ($datePrefix): ${first.title}"
            val body = buildNotificationBody(first)
            postEventNotification(
                context = context,
                notificationId = 777001,
                title = title,
                body = body,
            )
            "Notificación de prueba enviada"
        } else {
            postEventNotification(
                context = context,
                notificationId = 777001,
                title = "Recordatorios de SaldoClaro activos",
                body = "Te avisaremos automáticamente el día de cada pago, corte o ingreso frecuente.",
            )
            "Notificación de prueba enviada"
        }
    }

    private fun matchesPreferences(
        event: ProjectedCalendarEvent,
        prefs: NotificationPreferences,
    ): Boolean = when (event.kind) {
        ProjectedEventKind.SUBSCRIPTION, ProjectedEventKind.RECURRING_BILL -> prefs.notifyBillsAndSubscriptions
        ProjectedEventKind.RECURRING_INCOME -> prefs.notifyRecurringIncomes
        ProjectedEventKind.CARD_CUTOFF -> prefs.notifyCardCutoffs
        ProjectedEventKind.CARD_PAYMENT_DUE -> prefs.notifyCardDueDates
        ProjectedEventKind.BANK_LOAN_PAYMENT, ProjectedEventKind.LENT_LOAN_COLLECTION -> prefs.notifyLoanPayments
    }

    private fun buildNotificationTitle(event: ProjectedCalendarEvent): String {
        val amountPart = if (event.amount > 0.0) {
            val sign = if (event.isInflow) "+" else ""
            " ($sign${FinanceFormatters.currency(event.amount)})"
        } else ""
        return when (event.kind) {
            ProjectedEventKind.RECURRING_INCOME -> "Ingreso programado hoy: ${event.title}$amountPart"
            ProjectedEventKind.SUBSCRIPTION -> "Hoy se cobra suscripción: ${event.title}$amountPart"
            ProjectedEventKind.RECURRING_BILL -> "Hoy vence pago: ${event.title}$amountPart"
            ProjectedEventKind.CARD_CUTOFF -> "Hoy es corte de tarjeta: ${event.accountName}"
            ProjectedEventKind.CARD_PAYMENT_DUE -> "Hoy vence pago de tarjeta: ${event.accountName}$amountPart"
            ProjectedEventKind.BANK_LOAN_PAYMENT -> "Hoy vence cuota de préstamo: ${event.accountName}$amountPart"
            ProjectedEventKind.LENT_LOAN_COLLECTION -> "Hoy toca cobro de préstamo: ${event.accountName}$amountPart"
        }
    }

    private fun buildNotificationBody(event: ProjectedCalendarEvent): String {
        return when (event.kind) {
            ProjectedEventKind.CARD_CUTOFF -> event.subtitle
            ProjectedEventKind.CARD_PAYMENT_DUE -> if (event.amount > 0.0) {
                "Saldo actual a liquidar: ${FinanceFormatters.currency(event.amount)}. Toca para abrir el calendario."
            } else {
                "Fecha límite de pago registrada (saldo actual $0.00)."
            }
            else -> "${event.subtitle}. Toca para ver el detalle en el calendario."
        }
    }

    private fun postEventNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
    ) {
        if (!hasNotificationPermission(context)) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CALENDAR, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}

class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PaymentNotificationManager.checkAndNotifyDueToday(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PaymentNotificationManager.getPreferences(context)
            if (prefs.enabled && PaymentNotificationManager.hasNotificationPermission(context)) {
                PaymentNotificationManager.scheduleDailyReminder(context)
            }
        }
    }
}
