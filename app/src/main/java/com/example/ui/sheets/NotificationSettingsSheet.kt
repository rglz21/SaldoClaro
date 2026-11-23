package com.example.ui.sheets

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.ScheduledPaymentEntity
import com.example.notifications.NotificationPreferences
import com.example.notifications.PaymentNotificationManager
import com.example.ui.components.GestureSafeModalBottomSheet
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.util.CalendarProjector
import com.example.util.DateMath
import com.example.util.FinanceFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSheet(
    accounts: List<AccountEntity>,
    scheduledPayments: List<ScheduledPaymentEntity>,
    hideBalances: Boolean,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val financeColors = LocalFinanceColors.current

    var hasPermission by remember {
        mutableStateOf(PaymentNotificationManager.hasNotificationPermission(context))
    }
    var prefs by remember {
        mutableStateOf(PaymentNotificationManager.getPreferences(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) {
            val next = prefs.copy(enabled = true)
            prefs = next
            PaymentNotificationManager.savePreferences(context, next)
            onStatusMessage("Notificaciones de pago activadas")
            scope.launch(Dispatchers.IO) {
                PaymentNotificationManager.checkAndNotifyDueToday(context)
            }
        } else {
            onStatusMessage("Permiso de notificaciones denegado. Puedes activarlo en Ajustes del teléfono.")
        }
    }

    fun updatePrefs(updated: NotificationPreferences) {
        prefs = updated
        PaymentNotificationManager.savePreferences(context, updated)
    }

    val upcomingEvents = remember(accounts, scheduledPayments) {
        CalendarProjector.nextUpcomingEvents(
            todayStartMillis = DateMath.startOfDay(),
            accounts = accounts,
            scheduledPayments = scheduledPayments,
            withinDays = 14,
        ).take(5)
    }

    GestureSafeModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (prefs.enabled && hasPermission) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (prefs.enabled && hasPermission) Icons.Rounded.NotificationsActive
                        else Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = if (prefs.enabled && hasPermission) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recordatorios de pago",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Recibe una alerta el mismo día de pago o corte (9:00 AM).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Permission Banner if needed on Android 13+
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    color = financeColors.warningContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Permiso de notificaciones requerido",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = financeColors.warning,
                        )
                        Text(
                            text = "Autoriza las notificaciones en tu dispositivo para avisarte cuando llegue el día de pago.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("notif_request_permission_button"),
                        ) {
                            Text("Permitir notificaciones")
                        }
                    }
                }
            }

            // Master switch
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activar alertas en día de pago",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (prefs.enabled && hasPermission) "Programado diariamente · 100% local"
                            else "Desactivado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = prefs.enabled && hasPermission,
                        onCheckedChange = { wantEnabled ->
                            if (wantEnabled && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                updatePrefs(prefs.copy(enabled = wantEnabled))
                                onStatusMessage(
                                    if (wantEnabled) "Recordatorios activados" else "Recordatorios pausados"
                                )
                            }
                        },
                        modifier = Modifier.testTag("notif_master_switch"),
                    )
                }
            }

            // Individual event type toggles
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    NotificationToggleRow(
                        title = "Pagos únicos, frecuentes y suscripciones",
                        subtitle = "Pagos puntuales agendados, Spotify, Internet, servicios...",
                        checked = prefs.notifyBillsAndSubscriptions,
                        enabled = prefs.enabled && hasPermission,
                        onCheckedChange = { updatePrefs(prefs.copy(notifyBillsAndSubscriptions = it)) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NotificationToggleRow(
                        title = "Ingresos frecuentes",
                        subtitle = "Salario y entradas programadas",
                        checked = prefs.notifyRecurringIncomes,
                        enabled = prefs.enabled && hasPermission,
                        onCheckedChange = { updatePrefs(prefs.copy(notifyRecurringIncomes = it)) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NotificationToggleRow(
                        title = "Fechas límite de pago TDC",
                        subtitle = "Tarjetas de crédito con día límite",
                        checked = prefs.notifyCardDueDates,
                        enabled = prefs.enabled && hasPermission,
                        onCheckedChange = { updatePrefs(prefs.copy(notifyCardDueDates = it)) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NotificationToggleRow(
                        title = "Cortes de tarjeta",
                        subtitle = "Recordatorio el día de corte de tus TDC",
                        checked = prefs.notifyCardCutoffs,
                        enabled = prefs.enabled && hasPermission,
                        onCheckedChange = { updatePrefs(prefs.copy(notifyCardCutoffs = it)) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NotificationToggleRow(
                        title = "Cuotas y cobros de préstamos",
                        subtitle = "Préstamos bancarios y dinero por cobrar",
                        checked = prefs.notifyLoanPayments,
                        enabled = prefs.enabled && hasPermission,
                        onCheckedChange = { updatePrefs(prefs.copy(notifyLoanPayments = it)) },
                    )
                }
            }

            // Next upcoming reminders preview
            if (upcomingEvents.isNotEmpty()) {
                Text(
                    text = "Próximos avisos (14 días)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        upcomingEvents.forEach { ev ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${FinanceFormatters.shortDate(ev.dateMillis)} · ${ev.kind.labelEs}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (ev.amount > 0.0) {
                                    val prefix = if (ev.isInflow) "+" else if (ev.isOutflow) "-" else ""
                                    Text(
                                        text = prefix + FinanceFormatters.currency(ev.amount, hideBalances),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            ev.isInflow -> financeColors.income
                                            ev.isOutflow -> financeColors.expense
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cerrar")
                }
                FilledTonalButton(
                    onClick = {
                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scope.launch {
                                val msg = withContext(Dispatchers.IO) {
                                    PaymentNotificationManager.sendPreviewNotification(context)
                                }
                                onStatusMessage(msg)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notif_test_button"),
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Probar aviso")
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}
