package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppointmentEntity
import com.example.data.StudioInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: StudioViewModel,
    // Si se pasa, el calendario ofrece salir al catálogo a escoger el paquete
    // y volver aquí con la fecha ya puesta. Es el mismo recorrido que el de
    // empezar por la oferta, pero al revés.
    onElegirDelCatalogo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cartItems by viewModel.cart.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val contractText by viewModel.contractText.collectAsState()
    val studioConfig by viewModel.studioConfig.collectAsState()

    // Calendar state
    val calendar = remember { Calendar.getInstance() }
    var displayedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var displayedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    // Lo que el cliente lleva elegido. No se guarda en esta pantalla sino en
    // el ViewModel, porque puede salir al catálogo a escoger un paquete y
    // volver: si viviera aquí, al salir se perdería el día y la hora.
    val reserva by viewModel.reserva.collectAsState()
    fun editar(cambio: (ReservaBorrador) -> ReservaBorrador) =
        viewModel.actualizarReserva(cambio)

    var showContractDialog by remember { mutableStateOf(false) }
    var bookingSuccessAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Quien viene de confirmar un pedido ya firmó el contrato y ya se hizo las
    // dos fotos. Aquí solo le falta el día: no se le vuelve a pedir la firma.
    val contratoDelPedido by viewModel.contratoPendiente.collectAsState()

    fun guardarReserva(firmado: ContratoFirmado) {
        viewModel.saveAppointment(
            fecha = reserva.fecha,
            hora = reserva.hora,
            nombreCliente = reserva.nombre,
            telefono = reserva.telefono,
            detalleSeleccion = reserva.paquete,
            notas = reserva.notas,
            firmaBytes = firmado.firmaBytes,
            fotoClienteBytes = firmado.fotoClienteBytes,
            fotoCliente2Bytes = firmado.fotoCliente2Bytes,
            terminosAceptados = true,
            montoAcordado = reserva.monto.toDoubleOrNull() ?: 0.0,
            anticipoPagado = reserva.anticipo.toDoubleOrNull() ?: 0.0,
            onSuccess = { savedEntity ->
                bookingSuccessAppointment = savedEntity
                // La reserva ya está guardada: el borrador y el contrato en
                // mano se vacían para que la siguiente empiece de cero.
                viewModel.limpiarReserva()
                Toast.makeText(context, "Reservación registrada con éxito", Toast.LENGTH_LONG).show()

                val uriStr = viewModel.generateAppointmentWhatsAppUri(savedEntity)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Sin WhatsApp instalado no pasa nada: la cita ya está guardada.
                }
            }
        )
    }

    // Prefill package selection from cart if empty
    LaunchedEffect(cartItems) {
        if (reserva.paquete.isBlank() && cartItems.isNotEmpty()) {
            val summary = cartItems.joinToString(", ") {
                "${it.item.name} (${it.variant.name}) x${it.quantity}"
            } + " [Total: \$${String.format("%.2f", cartTotal)} USD]"
            editar { it.copy(paquete = summary) }
        }
        // El monto acordado se propone desde el carrito; sigue siendo editable
        // porque el contrato admite negociar el precio.
        if (reserva.monto.isBlank() && cartTotal > 0.0) {
            editar { it.copy(monto = String.format("%.2f", cartTotal)) }
        }
    }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }
    val displayCalendar = remember(displayedMonth, displayedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // Days in current month
    val daysInMonth = displayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = displayCalendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
    // Convert to Monday=0, Sunday=6 offset
    val startingOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    // Los turnos dependen del día: el sábado el estudio cierra al mediodía.
    val availableHours = remember(reserva.diaSemana) {
        if (reserva.diaSemana in 0..6) StudioInfo.turnosPara(reserva.diaSemana)
        else StudioInfo.TURNOS_SEMANA
    }

    // Si el turno elegido no existe el día seleccionado, se pasa al primero
    // disponible para no enviar una reserva a una hora cerrada.
    LaunchedEffect(availableHours) {
        if (availableHours.isNotEmpty() && reserva.hora !in availableHours) {
            editar { it.copy(hora = availableHours.first()) }
        }
    }

    // Fecha de hoy como número aaaammdd, para comparar días sin líos de zona
    // horaria ni de formato.
    val hoyComparable = remember {
        val c = Calendar.getInstance()
        c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    // Horas ya reservadas para el día elegido: evita agendar dos clientes
    // en el mismo turno.
    val horariosOcupados = remember(appointments, reserva.fecha) {
        appointments.filter { it.fecha == reserva.fecha }
            .map { it.hora }
            .toSet()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Mientras no haya reserva hecha se ve el calendario y el formulario.
        // En cuanto se reserva, todo eso se recoge y solo queda la
        // confirmación: dejar el formulario debajo invita a reservar otra vez
        // sin querer, y obliga a bajar toda la pantalla para ver que salió.
        if (bookingSuccessAppointment == null) {

        // Hero / Studio Schedule Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "Agenda tu Sesión Fotográfica",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${StudioInfo.NOMBRE} • ${StudioInfo.UBICACION}\n${StudioInfo.HORARIO_SEMANA}\n${StudioInfo.HORARIO_SABADO} • ${StudioInfo.HORARIO_DOMINGO}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Calendar Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Month Navigation Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear--
                                } else {
                                    displayedMonth--
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                        }

                        Text(
                            text = monthFormat.format(displayCalendar.time).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear++
                                } else {
                                    displayedMonth++
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Day of week labels (Lun, Mar, Mié, Jue, Vie, Sáb, Dom)
                    val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        dayNames.forEachIndexed { idx, name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (idx == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid
                    val totalSlots = startingOffset + daysInMonth
                    val rows = (totalSlots + 6) / 7

                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val slotIndex = row * 7 + col
                                val dayNumber = slotIndex - startingOffset + 1

                                if (slotIndex < startingOffset || dayNumber > daysInMonth) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val isSunday = col == 6
                                    val dateStr = String.format("%02d/%02d/%04d", dayNumber, displayedMonth + 1, displayedYear)
                                    val isSelected = reserva.fecha == dateStr
                                    // No se puede reservar para un día que ya pasó.
                                    val esPasado = (displayedYear * 10000 + (displayedMonth + 1) * 100 + dayNumber) < hoyComparable
                                    val habilitado = !isSunday && !esPasado

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    !habilitado -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                }
                                            )
                                            .clickable(enabled = habilitado) {
                                                editar {
                                                    it.copy(fecha = dateStr, diaSemana = col)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    esPasado -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                    isSunday -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (reserva.diaSemana == 6) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "❌ Los domingos el estudio está cerrado. Elija de lunes a sábado.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }

                    if (reserva.fecha.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Fecha seleccionada: ${reserva.fecha}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (reserva.diaSemana == 5) "Seleccione la Hora (sábado: 9:00 AM – 12:00 PM):" else "Seleccione la Hora (9:00 AM – 5:00 PM):",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Time slots. Los horarios ya reservados para ese día
                        // aparecen deshabilitados para no duplicar la cita.
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableHours.chunked(4).forEach { hourRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    hourRow.forEach { hour ->
                                        val ocupado = hour in horariosOcupados
                                        FilterChip(
                                            selected = reserva.hora == hour && !ocupado,
                                            enabled = !ocupado,
                                            onClick = { editar { r -> r.copy(hora = hour) } },
                                            label = {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        hour,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                    if (ocupado) {
                                                        Text(
                                                            "Ocupado",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        if (reserva.hora in horariosOcupados) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Las ${reserva.hora} ya está reservada ese día. Elija otra hora.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }

        // Reservation Form Fields
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Datos de la Reservación",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Quien acaba de confirmar un pedido ya firmó: se le dice,
                    // para que no se quede esperando el contrato otra vez.
                    if (contratoDelPedido != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Contrato firmado y fotos tomadas. Solo falta " +
                                    "elegir el día y la hora, y confirmar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reserva.nombre,
                        onValueChange = { v -> editar { it.copy(nombre = v) } },
                        label = { Text("Nombre completo del cliente o tutor *", style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = reserva.telefono,
                        onValueChange = { v -> editar { it.copy(telefono = v) } },
                        label = { Text("Teléfono de contacto (WhatsApp) *", style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quien llega aquí sin haber pasado por el catálogo puede
                    // salir a escoger y volver: la fecha y la hora quedan
                    // guardadas, así que no hay que elegirlas otra vez.
                    if (onElegirDelCatalogo != null && reserva.paquete.isBlank()) {
                        OutlinedButton(
                            onClick = {
                                editar { it.copy(vinoDelCalendario = true) }
                                onElegirDelCatalogo()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_elegir_del_catalogo"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Escoger un paquete del catálogo",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reserva.paquete,
                        onValueChange = { v -> editar { it.copy(paquete = v) } },
                        label = { Text("Opción, paquete o servicio seleccionado *", style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("package_selection_input"),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = reserva.notas,
                        onValueChange = { v -> editar { it.copy(notas = v) } },
                        label = { Text("Notas adicionales (ej: temática, vestuario, acompañantes)", style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.NoteAlt, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2,
                        maxLines = 3
                    )

                    // El contrato exige un anticipo al reservar: queda registrado
                    // junto al monto total para poder ver el saldo pendiente.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = reserva.monto,
                            onValueChange = { v ->
                                editar { it.copy(monto = v.filter { c -> c.isDigit() || c == '.' }) }
                            },
                            label = { Text("Monto acordado (USD)") },
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("monto_acordado_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = reserva.anticipo,
                            onValueChange = { v ->
                                editar { it.copy(anticipo = v.filter { c -> c.isDigit() || c == '.' }) }
                            },
                            label = { Text("Anticipo pagado (USD)") },
                            leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anticipo_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    val montoNum = reserva.monto.toDoubleOrNull() ?: 0.0
                    val anticipoNum = reserva.anticipo.toDoubleOrNull() ?: 0.0
                    if (montoNum > 0.0) {
                        val saldo = (montoNum - anticipoNum).coerceAtLeast(0.0)
                        Text(
                            text = "Saldo pendiente: $${String.format("%.2f", saldo)} USD" +
                                (viewModel.cupLabel(saldo)?.let { "  ($it)" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (reserva.fecha.isBlank()) {
                                Toast.makeText(context, "Por favor seleccione un día en el calendario", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (reserva.nombre.isBlank()) {
                                Toast.makeText(context, "Por favor ingrese el nombre del cliente", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (reserva.telefono.isBlank()) {
                                Toast.makeText(context, "Por favor ingrese el teléfono de contacto", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (reserva.paquete.isBlank()) {
                                Toast.makeText(context, "Por favor especifique la opción o paquete", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Última barrera contra la doble reserva: alguien pudo
                            // agendar ese turno mientras se llenaba el formulario.
                            if (reserva.hora in horariosOcupados) {
                                Toast.makeText(
                                    context,
                                    "Ese horario ya está reservado. Por favor elija otra hora.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }

                            // Si viene de confirmar un pedido, el contrato ya
                            // está firmado con sus dos fotos: se reserva sin
                            // hacerle firmar dos veces lo mismo.
                            val yaFirmado = contratoDelPedido
                            if (yaFirmado != null) guardarReserva(yaFirmado)
                            else showContractDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_appointment_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (contratoDelPedido != null) "Confirmar la reservación"
                            else "Continuar a Contrato y Reservación",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }

        } // fin del bloque que se oculta al reservar

        // Success Confirmation Card (if booked)
        bookingSuccessAppointment?.let { appt ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "¡Cita Registrada y Firmada!",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "Cita para ${appt.nombreCliente} el día ${appt.fecha} a las ${appt.hora}.\nSe ha guardado localmente en la base de datos del estudio.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Button(
                            onClick = {
                                val uriStr = viewModel.generateAppointmentWhatsAppUri(appt)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir WhatsApp automáticamente", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366)
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enviar Confirmación por WhatsApp",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // WhatsApp solo abre un chat a la vez, así que la copia
                        // para el cliente se manda en un segundo toque.
                        OutlinedButton(
                            onClick = {
                                val uriStr = viewModel.generateAppointmentWhatsAppUri(
                                    appt,
                                    enviarACliente = true
                                )
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No se pudo abrir el WhatsApp del cliente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_copy_to_client_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF25D366)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enviar copia al cliente (${appt.telefono})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Pasa la cita al calendario del teléfono o la tablet.
                        OutlinedButton(
                            onClick = {
                                agregarCitaAlCalendarioDelEquipo(context, appt, studioConfig)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Agregar al calendario del equipo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Vuelve a enseñar el calendario y el formulario, ya
                        // en blanco, para agendar a otra persona.
                        TextButton(
                            onClick = { bookingSuccessAppointment = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_otra_reserva")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hacer otra reservación",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Full Contract & Signature Dialog
    if (showContractDialog) {
        ContractSignatureDialog(
            title = "Contrato de Sesión - Reservación",
            contractText = contractText,
            onDismiss = { showContractDialog = false },
            onConfirm = { firmado ->
                showContractDialog = false
                guardarReserva(firmado)
            }
        )
    }
}
