package com.example.ui.sheets

import android.net.Uri
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.FinanceBackupManager
import com.example.data.backup.FinanceBackupSnapshot
import com.example.ui.components.GestureSafeModalBottomSheet
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.FinanceUiState
import com.example.util.FinanceFormatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupDataSheet(
    state: FinanceUiState,
    onExportBackupToUri: suspend (Uri) -> Result<FinanceBackupSnapshot>,
    onExportCsvToUri: suspend (Uri) -> Result<Int>,
    onInspectBackupFromUri: suspend (Uri) -> Result<FinanceBackupSnapshot>,
    onConfirmRestoreBackup: suspend (FinanceBackupSnapshot) -> Result<FinanceBackupSnapshot>,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val financeColors = LocalFinanceColors.current

    var isBusy by remember { mutableStateOf(false) }
    var busyLabel by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<FinanceBackupSnapshot?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            busyLabel = "Exportando respaldo completo..."
            feedbackMessage = null
            val res = onExportBackupToUri(uri)
            isBusy = false
            res.onSuccess { snap ->
                feedbackIsError = false
                feedbackMessage = "Respaldo guardado: ${snap.accounts.size} cuentas, ${snap.transactions.size} movimientos, ${snap.scheduledPayments.size} pagos programados y ${snap.categories.size} categorías."
            }.onFailure { err ->
                feedbackIsError = true
                feedbackMessage = err.message ?: "No se pudo guardar el archivo de respaldo."
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            busyLabel = "Exportando movimientos en CSV..."
            feedbackMessage = null
            val res = onExportCsvToUri(uri)
            isBusy = false
            res.onSuccess { count ->
                feedbackIsError = false
                feedbackMessage = "Archivo CSV guardado con $count movimientos."
            }.onFailure { err ->
                feedbackIsError = true
                feedbackMessage = err.message ?: "No se pudo guardar el archivo CSV."
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            busyLabel = "Leyendo y validando archivo..."
            feedbackMessage = null
            pendingRestore = null
            val res = onInspectBackupFromUri(uri)
            isBusy = false
            res.onSuccess { snap ->
                pendingRestore = snap
            }.onFailure { err ->
                feedbackIsError = true
                feedbackMessage = err.message ?: "No se pudo leer el archivo de respaldo."
            }
        }
    }

    GestureSafeModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ImportExport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Importar y exportar datos",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Guarda o restaura tu base local completa en formato .json",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Current data summary card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Información actual en tu dispositivo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BackupMetricTile(
                            label = "Cuentas",
                            value = state.accounts.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        BackupMetricTile(
                            label = "Movimientos",
                            value = state.transactions.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        BackupMetricTile(
                            label = "Programados",
                            value = state.scheduledPayments.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BackupMetricTile(
                            label = "Categorías",
                            value = state.categories.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        BackupMetricTile(
                            label = "Planes MSI",
                            value = state.installmentPlans.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        BackupMetricTile(
                            label = "Recordatorios",
                            value = "Sí",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Busy indicator
            if (isBusy) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                        )
                        Text(
                            text = busyLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Status feedback banner
            feedbackMessage?.let { msg ->
                val containerColor = if (feedbackIsError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    financeColors.incomeContainer
                }
                val contentColor = if (feedbackIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    financeColors.income
                }
                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (feedbackIsError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Two-step restore preview & confirmation card
            pendingRestore?.let { preview ->
                Surface(
                    color = financeColors.warningContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, financeColors.warning.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_restore_preview_card"),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = financeColors.warning,
                            )
                            Text(
                                text = "Confirmar restauración de respaldo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "Respaldo creado el ${FinanceFormatters.fullDate(preview.exportedAtMillis)}:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "• ${preview.accounts.size} cuentas, tarjetas y préstamos\n" +
                                "• ${preview.transactions.size} movimientos\n" +
                                "• ${preview.scheduledPayments.size} pagos e ingresos programados\n" +
                                "• ${preview.categories.size} categorías y presupuestos\n" +
                                "• ${preview.installmentPlans.size} planes a meses (MSI)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Al continuar, se reemplazará toda la información actual en este dispositivo por los datos del archivo seleccionado.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { pendingRestore = null },
                                enabled = !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_cancel_restore_button"),
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        isBusy = true
                                        busyLabel = "Restaurando base de datos..."
                                        feedbackMessage = null
                                        val res = onConfirmRestoreBackup(preview)
                                        isBusy = false
                                        res.onSuccess {
                                            pendingRestore = null
                                            feedbackIsError = false
                                            feedbackMessage = "Información restaurada correctamente."
                                            onDismiss()
                                        }.onFailure { err ->
                                            feedbackIsError = true
                                            feedbackMessage = err.message ?: "Error al restaurar el respaldo."
                                        }
                                    }
                                },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_confirm_restore_button"),
                            ) {
                                Text("Restaurar todo")
                            }
                        }
                    }
                }
            }

            // Export section
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Exportar información",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Guarda una copia completa en Descargas, Google Drive o cualquier carpeta de tu teléfono para respaldar o migrar tus datos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            exportJsonLauncher.launch(FinanceBackupManager.suggestedBackupFileName())
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_export_json_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar todo (.json)")
                    }
                    FilledTonalButton(
                        onClick = {
                            exportCsvLauncher.launch(FinanceBackupManager.suggestedCsvFileName())
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_export_csv_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar movimientos (.csv para Excel)")
                    }
                }
            }

            // Import section
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Importar respaldo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Selecciona un archivo .json generado por SaldoClaro. Primero verás un resumen de lo que contiene antes de confirmar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            importJsonLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")
                            )
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_import_json_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Seleccionar respaldo (.json)")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_close_button"),
            ) {
                Text("Cerrar")
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BackupMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
