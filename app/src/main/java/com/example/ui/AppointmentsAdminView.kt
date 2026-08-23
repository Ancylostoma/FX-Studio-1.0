package com.example.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AppointmentEntity
import com.example.data.EstadoCita
import java.util.Calendar

/**
 * Convierte "dd/MM/yyyy" en un número aaaammdd para ordenar y comparar días.
 * Las fechas con otro formato van al final, en vez de romper el orden.
 */
internal fun fechaComparable(fecha: String): Int {
    val p = fecha.trim().split("/")
    if (p.size != 3) return Int.MAX_VALUE
    val d = p[0].toIntOrNull() ?: return Int.MAX_VALUE
    val m = p[1].toIntOrNull() ?: return Int.MAX_VALUE
    val a = p[2].toIntOrNull() ?: return Int.MAX_VALUE
    return a * 10000 + m * 100 + d
}

/** Convierte "09:00 AM" / "01:00 PM" en minutos desde medianoche. */
internal fun horaComparable(hora: String): Int {
    val limpio = hora.trim().uppercase()
    val esPm = limpio.endsWith("PM")
    val hm = limpio.removeSuffix("AM").removeSuffix("PM").trim().split(":")
    if (hm.size != 2) return 0
    var h = hm[0].toIntOrNull() ?: return 0
    val m = hm[1].toIntOrNull() ?: 0
    if (esPm && h != 12) h += 12
    if (!esPm && h == 12) h = 0
    return h * 60 + m
}

/** Hoy (offset 0) o mañana (offset 1) como número aaaammdd. */
internal fun comparableDeHoy(offsetDias: Int): Int {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_MONTH, offsetDias)
    return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
}

/** Chips con las etapas del trabajo; la actual queda resaltada. */
@Composable
private fun EstadoSelector(
    estadoActual: String,
    onCambiar: (String) -> Unit
) {
    Column {
        Text(
            text = "Estado del trabajo:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EstadoCita.TODOS.forEach { estado ->
                val activo = estado == estadoActual
                FilterChip(
                    selected = activo,
                    onClick = { if (!activo) onCambiar(estado) },
                    label = {
                        Text(
                            estado,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    leadingIcon = if (activo) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ResumenDiaCard(
    titulo: String,
    citas: List<AppointmentEntity>,
    destacado: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (destacado) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = if (destacado) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (citas.isEmpty()) "Sin citas"
                else "${citas.size} ${if (citas.size == 1) "cita" else "citas"}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (destacado) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            citas.take(3).forEach { c ->
                Text(
                    text = "• ${c.hora} — ${c.nombreCliente}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (destacado) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (citas.size > 3) {
                Text(
                    text = "y ${citas.size - 3} más…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppointmentsAdminView(
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val appointmentsRaw by viewModel.appointments.collectAsState()
    var selectedAppointmentForSignature by remember { mutableStateOf<AppointmentEntity?>(null) }
    // Una cita firmada es un contrato: se pide confirmación antes de borrarla.
    var appointmentPendingDelete by remember { mutableStateOf<AppointmentEntity?>(null) }
    var appointmentPendingPayment by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Se ordena por fecha de la sesión (no por cuándo se creó la cita), que es
    // lo que importa para saber qué toca primero.
    val appointments = remember(appointmentsRaw) {
        appointmentsRaw.sortedWith(
            compareBy({ fechaComparable(it.fecha) }, { horaComparable(it.hora) })
        )
    }

    val hoy = remember { comparableDeHoy(0) }
    val manana = remember { comparableDeHoy(1) }
    val citasHoy = remember(appointments) { appointments.filter { fechaComparable(it.fecha) == hoy } }
    val citasManana = remember(appointments) { appointments.filter { fechaComparable(it.fecha) == manana } }

    if (appointments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No hay reservaciones registradas aún",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Resumen de lo inmediato: el contrato pide confirmar la víspera,
            // así el estudio ve a quién llamar sin buscar en toda la lista.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResumenDiaCard(
                        titulo = "HOY",
                        citas = citasHoy,
                        destacado = true,
                        modifier = Modifier.weight(1f)
                    )
                    ResumenDiaCard(
                        titulo = "MAÑANA",
                        citas = citasManana,
                        destacado = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            items(appointments) { appt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = appt.nombreCliente,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tel: ${appt.telefono}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${appt.fecha} • ${appt.hora}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        // Etapa del trabajo: se cambia tocando el chip.
                        EstadoSelector(
                            estadoActual = appt.estado,
                            onCambiar = { nuevo ->
                                viewModel.updateAppointmentStatus(appt.id, nuevo)
                            }
                        )

                        // Pagos: solo se muestra si hay un monto registrado.
                        if (appt.montoAcordado > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (appt.saldoPendiente <= 0.0)
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Acordado $${String.format("%.2f", appt.montoAcordado)}  •  " +
                                            "Anticipo $${String.format("%.2f", appt.anticipoPagado)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (appt.saldoPendiente <= 0.0) "✅ Pagado completo"
                                        else "Saldo pendiente: $${String.format("%.2f", appt.saldoPendiente)} USD",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (appt.saldoPendiente <= 0.0)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                                    )
                                }
                                TextButton(onClick = { appointmentPendingPayment = appt }) {
                                    Text("Editar")
                                }
                            }
                        } else {
                            TextButton(
                                onClick = { appointmentPendingPayment = appt },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar pago")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Opción / Paquete:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = appt.detalleSeleccion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (appt.notas.isNotBlank()) {
                            Text(
                                text = "Notas: ${appt.notas}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (appt.firmaBytes != null) {
                                OutlinedButton(
                                    onClick = { selectedAppointmentForSignature = appt },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver Firma Digital", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                Text(
                                    text = "Sin firma registrada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        val tel = appt.telefono.filter { it.isDigit() || it == '+' }
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "No se pudo abrir el teléfono",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Llamar al cliente",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val uriStr = viewModel.generateAppointmentWhatsAppUri(appt)
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Enviar por WhatsApp",
                                        tint = Color(0xFF25D366)
                                    )
                                }

                                IconButton(
                                    onClick = { appointmentPendingDelete = appt }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Registro / edición de pagos
    appointmentPendingPayment?.let { appt ->
        var montoTxt by remember(appt.id) {
            mutableStateOf(if (appt.montoAcordado > 0.0) String.format("%.2f", appt.montoAcordado) else "")
        }
        var anticipoTxt by remember(appt.id) {
            mutableStateOf(if (appt.anticipoPagado > 0.0) String.format("%.2f", appt.anticipoPagado) else "")
        }
        val monto = montoTxt.toDoubleOrNull() ?: 0.0
        val anticipo = anticipoTxt.toDoubleOrNull() ?: 0.0
        val saldo = (monto - anticipo).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { appointmentPendingPayment = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Pago de ${appt.nombreCliente}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = montoTxt,
                        onValueChange = { montoTxt = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto acordado (USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = anticipoTxt,
                        onValueChange = { anticipoTxt = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Anticipo pagado (USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (saldo <= 0.0 && monto > 0.0) "✅ Queda pagado completo"
                        else "Saldo pendiente: $${String.format("%.2f", saldo)} USD" +
                            (viewModel.cupLabel(saldo)?.let { "  ($it)" } ?: ""),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (saldo <= 0.0 && monto > 0.0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAppointmentPayment(appt.id, monto, anticipo)
                        appointmentPendingPayment = null
                        Toast.makeText(context, "Pago actualizado", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { appointmentPendingPayment = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmación de borrado
    appointmentPendingDelete?.let { appt ->
        AlertDialog(
            onDismissRequest = { appointmentPendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Eliminar esta reservación?") },
            text = {
                Text(
                    "Se borrará la cita de ${appt.nombreCliente} del ${appt.fecha} a las " +
                        "${appt.hora}, junto con su firma digital.\n\nEsta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAppointment(appt.id)
                        appointmentPendingDelete = null
                        Toast.makeText(context, "Cita eliminada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sí, eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { appointmentPendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Signature preview dialog
    selectedAppointmentForSignature?.let { appt ->
        val bitmap = remember(appt) {
            appt.firmaBytes?.let {
                try {
                    BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }

        Dialog(onDismissRequest = { selectedAppointmentForSignature = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Firma de ${appt.nombreCliente}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Firma digital",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    } else {
                        Text("No se pudo cargar la imagen de la firma")
                    }

                    Button(
                        onClick = { selectedAppointmentForSignature = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}
