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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cartItems by viewModel.cart.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val contractText by viewModel.contractText.collectAsState()

    // Calendar state
    val calendar = remember { Calendar.getInstance() }
    var displayedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var displayedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    var selectedDateString by remember { mutableStateOf("") }
    var selectedDayOfWeek by remember { mutableStateOf(-1) }
    var selectedTimeSlot by remember { mutableStateOf("10:00 AM") }

    // Form inputs
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var packageSelection by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var montoInput by remember { mutableStateOf("") }
    var anticipoInput by remember { mutableStateOf("") }

    var showContractDialog by remember { mutableStateOf(false) }
    var bookingSuccessAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Prefill package selection from cart if empty
    LaunchedEffect(cartItems) {
        if (packageSelection.isBlank() && cartItems.isNotEmpty()) {
            val summary = cartItems.joinToString(", ") {
                "${it.item.name} (${it.variant.name}) x${it.quantity}"
            } + " [Total: \$${String.format("%.2f", cartTotal)} USD]"
            packageSelection = summary
        }
        // El monto acordado se propone desde el carrito; sigue siendo editable
        // porque el contrato admite negociar el precio.
        if (montoInput.isBlank() && cartTotal > 0.0) {
            montoInput = String.format("%.2f", cartTotal)
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

    val availableHours = listOf(
        "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM"
    )

    // Fecha de hoy como número aaaammdd, para comparar días sin líos de zona
    // horaria ni de formato.
    val hoyComparable = remember {
        val c = Calendar.getInstance()
        c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    // Horas ya reservadas para el día elegido: evita agendar dos clientes
    // en el mismo turno.
    val horariosOcupados = remember(appointments, selectedDateString) {
        appointments.filter { it.fecha == selectedDateString }
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
                            text = "FXestudio • Bayamo, Granma\nLunes a Sábado de 9:00 AM a 5:00 PM (Domingos cerrado)",
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
                                    val isSelected = selectedDateString == dateStr
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
                                                selectedDateString = dateStr
                                                selectedDayOfWeek = col
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

                    if (selectedDayOfWeek == 6) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "❌ Estudio cerrado los domingos. Por favor seleccione de Lunes a Sábado.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }

                    if (selectedDateString.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Fecha seleccionada: $selectedDateString",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Seleccione la Hora (Horario: 9:00 AM – 5:00 PM):",
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
                                            selected = selectedTimeSlot == hour && !ocupado,
                                            enabled = !ocupado,
                                            onClick = { selectedTimeSlot = hour },
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

                        if (selectedTimeSlot in horariosOcupados) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Las $selectedTimeSlot ya está reservada ese día. Elija otra hora.",
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

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre completo del cliente o tutor *", style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Teléfono de contacto (WhatsApp) *", style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = packageSelection,
                        onValueChange = { packageSelection = it },
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
                        value = notes,
                        onValueChange = { notes = it },
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
                            value = montoInput,
                            onValueChange = { montoInput = it.filter { c -> c.isDigit() || c == '.' } },
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
                            value = anticipoInput,
                            onValueChange = { anticipoInput = it.filter { c -> c.isDigit() || c == '.' } },
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

                    val montoNum = montoInput.toDoubleOrNull() ?: 0.0
                    val anticipoNum = anticipoInput.toDoubleOrNull() ?: 0.0
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
                            if (selectedDateString.isBlank()) {
                                Toast.makeText(context, "Por favor seleccione un día en el calendario", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (clientName.isBlank()) {
                                Toast.makeText(context, "Por favor ingrese el nombre del cliente", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (clientPhone.isBlank()) {
                                Toast.makeText(context, "Por favor ingrese el teléfono de contacto", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (packageSelection.isBlank()) {
                                Toast.makeText(context, "Por favor especifique la opción o paquete", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Última barrera contra la doble reserva: alguien pudo
                            // agendar ese turno mientras se llenaba el formulario.
                            if (selectedTimeSlot in horariosOcupados) {
                                Toast.makeText(
                                    context,
                                    "Ese horario ya está reservado. Por favor elija otra hora.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }

                            showContractDialog = true
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
                            text = "Continuar a Contrato y Reservación",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }

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
            onConfirm = { signatureBytes ->
                showContractDialog = false
                viewModel.saveAppointment(
                    fecha = selectedDateString,
                    hora = selectedTimeSlot,
                    nombreCliente = clientName,
                    telefono = clientPhone,
                    detalleSeleccion = packageSelection,
                    notas = notes,
                    firmaBytes = signatureBytes,
                    terminosAceptados = true,
                    montoAcordado = montoInput.toDoubleOrNull() ?: 0.0,
                    anticipoPagado = anticipoInput.toDoubleOrNull() ?: 0.0,
                    onSuccess = { savedEntity ->
                        bookingSuccessAppointment = savedEntity
                        Toast.makeText(context, "Reservación registrada con éxito", Toast.LENGTH_LONG).show()

                        // Automatically launch WhatsApp with the pre-formatted appointment
                        val uriStr = viewModel.generateAppointmentWhatsAppUri(savedEntity)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignored if WhatsApp not available
                        }
                    }
                )
            }
        )
    }
}
