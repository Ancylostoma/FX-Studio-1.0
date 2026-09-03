package com.example.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.AppointmentEntity
import com.example.data.EstadoCita
import java.io.File
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

private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val DIAS_CORTOS = listOf("L", "M", "M", "J", "V", "S", "D")

/** Formas de ordenar la agenda desde el panel del admin. */
private enum class OrdenAgenda(val etiqueta: String) {
    FECHA("Por fecha de sesión"),
    RECIENTES("Últimas reservadas"),
    SALDO("Mayor saldo pendiente"),
    NOMBRE("Nombre A – Z")
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

/**
 * Calendario del mes con un punto en los días que tienen citas. Al tocar un día
 * la lista de abajo se queda solo con las citas de ese día; al tocarlo otra vez
 * se quita el filtro.
 */
@Composable
private fun CalendarioAgenda(
    citasPorDia: Map<Int, Int>,
    diaSeleccionado: Int?,
    onDiaSeleccionado: (Int?) -> Unit
) {
    val hoyCal = remember { Calendar.getInstance() }
    var anio by rememberSaveable { mutableStateOf(hoyCal.get(Calendar.YEAR)) }
    var mes by rememberSaveable { mutableStateOf(hoyCal.get(Calendar.MONTH)) }
    val hoy = remember { comparableDeHoy(0) }

    val primerDia = remember(anio, mes) {
        Calendar.getInstance().apply {
            clear()
            set(anio, mes, 1)
        }
    }
    // Calendar devuelve 1 = domingo; aquí la semana empieza en lunes.
    val huecoInicial = (primerDia.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val diasDelMes = primerDia.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalCeldas = huecoInicial + diasDelMes
    val filas = (totalCeldas + 6) / 7

    val citasDelMes = remember(citasPorDia, anio, mes) {
        val prefijo = anio * 10000 + (mes + 1) * 100
        citasPorDia.filterKeys { it in (prefijo + 1)..(prefijo + 31) }.values.sum()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (mes == 0) { mes = 11; anio -= 1 } else mes -= 1
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${MESES[mes]} $anio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (citasDelMes == 0) "Sin citas este mes"
                        else "$citasDelMes ${if (citasDelMes == 1) "cita" else "citas"} este mes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    if (mes == 11) { mes = 0; anio += 1 } else mes += 1
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                DIAS_CORTOS.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            for (fila in 0 until filas) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val celda = fila * 7 + col
                        val dia = celda - huecoInicial + 1
                        if (dia < 1 || dia > diasDelMes) {
                            Spacer(modifier = Modifier.weight(1f).height(42.dp))
                        } else {
                            val clave = anio * 10000 + (mes + 1) * 100 + dia
                            val cantidad = citasPorDia[clave] ?: 0
                            val seleccionado = diaSeleccionado == clave
                            val esHoy = clave == hoy

                            val fondo = when {
                                seleccionado -> MaterialTheme.colorScheme.primary
                                cantidad > 0 -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            }
                            val texto = when {
                                seleccionado -> MaterialTheme.colorScheme.onPrimary
                                cantidad > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(fondo)
                                    .then(
                                        if (esHoy && !seleccionado) Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(9.dp)
                                        ) else Modifier
                                    )
                                    .clickable {
                                        onDiaSeleccionado(if (seleccionado) null else clave)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dia.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (cantidad > 0) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = texto
                                    )
                                    if (cantidad > 0) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(texto)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (diaSeleccionado != null) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = { onDiaSeleccionado(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver todos los días")
                }
            }
        }
    }
}


/** Encabezado de la tabla, como la fila de títulos de una hoja de cálculo. */
@Composable
private fun CabeceraTabla() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = "FECHA Y HORA",
            modifier = Modifier.weight(0.27f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1
        )
        Text(
            text = "CLIENTE",
            modifier = Modifier.weight(0.46f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1
        )
        Text(
            text = "SALDO",
            modifier = Modifier.weight(0.27f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

/**
 * Una reservación en dos renglones y tres columnas alineadas, como la vista
 * de detalles de una carpeta del ordenador. Al tocarla se abre la ficha
 * completa con todo lo que se puede hacer con ella.
 */
@Composable
private fun FilaCita(
    cita: AppointmentEntity,
    rayada: Boolean,
    esHoy: Boolean,
    onAbrir: () -> Unit
) {
    val fondo = when {
        esHoy -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        rayada -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo)
            .clickable { onAbrir() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Columna 1: cuándo
            Column(modifier = Modifier.weight(0.27f)) {
                Text(
                    text = cita.fecha,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cita.hora,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Columna 2: quién
            Column(modifier = Modifier.weight(0.46f).padding(horizontal = 6.dp)) {
                Text(
                    text = cita.nombreCliente,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cita.telefono,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Columna 3: cuánto queda
            Column(
                modifier = Modifier.weight(0.27f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = when {
                        cita.montoAcordado <= 0.0 -> "—"
                        cita.saldoPendiente <= 0.0 -> "Pagado"
                        else -> "$${String.format("%.2f", cita.saldoPendiente)}"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = when {
                        cita.montoAcordado <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        cita.saldoPendiente <= 0.0 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                    maxLines = 1
                )
                Text(
                    text = cita.estado,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Segundo renglón: el paquete, que es lo que más se consulta.
        Text(
            text = cita.detalleSeleccion.replace("\n", " · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
    Divider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** Etiqueta y valor, alineados, dentro de la ficha completa. */
@Composable
private fun DatoFicha(etiqueta: String, valor: String) {
    if (valor.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = etiqueta,
            modifier = Modifier.width(104.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AppointmentsAdminView(
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val appointmentsRaw by viewModel.appointments.collectAsState()
    val studioConfig by viewModel.studioConfig.collectAsState()
    var citaAbierta by remember { mutableStateOf<AppointmentEntity?>(null) }
    var selectedAppointmentForSignature by remember { mutableStateOf<AppointmentEntity?>(null) }
    // Una cita firmada es un contrato: se pide confirmación antes de borrarla.
    var appointmentPendingDelete by remember { mutableStateOf<AppointmentEntity?>(null) }
    var appointmentPendingPayment by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Base de datos de la agenda: buscar, ordenar y filtrar.
    var busqueda by rememberSaveable { mutableStateOf("") }
    var orden by rememberSaveable { mutableStateOf(OrdenAgenda.FECHA) }
    var filtroEstado by rememberSaveable { mutableStateOf("") }
    var diaSeleccionado by rememberSaveable { mutableStateOf<Int?>(null) }
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }

    val hoy = remember { comparableDeHoy(0) }
    val manana = remember { comparableDeHoy(1) }
    val citasHoy = remember(appointmentsRaw) {
        appointmentsRaw.filter { fechaComparable(it.fecha) == hoy }
            .sortedBy { horaComparable(it.hora) }
    }
    val citasManana = remember(appointmentsRaw) {
        appointmentsRaw.filter { fechaComparable(it.fecha) == manana }
            .sortedBy { horaComparable(it.hora) }
    }

    // Cuántas citas tiene cada día, para pintarlas en el calendario.
    val citasPorDia = remember(appointmentsRaw) {
        appointmentsRaw.groupingBy { fechaComparable(it.fecha) }.eachCount()
    }

    val appointments = remember(appointmentsRaw, busqueda, orden, filtroEstado, diaSeleccionado) {
        val texto = busqueda.trim().lowercase()
        val soloDigitos = texto.filter { it.isDigit() }
        appointmentsRaw
            .filter { c ->
                val coincideTexto = texto.isBlank() ||
                    c.nombreCliente.lowercase().contains(texto) ||
                    c.detalleSeleccion.lowercase().contains(texto) ||
                    c.notas.lowercase().contains(texto) ||
                    (soloDigitos.isNotEmpty() &&
                        c.telefono.filter { d -> d.isDigit() }.contains(soloDigitos))
                val coincideEstado = filtroEstado.isBlank() || c.estado == filtroEstado
                val coincideDia = diaSeleccionado == null ||
                    fechaComparable(c.fecha) == diaSeleccionado
                coincideTexto && coincideEstado && coincideDia
            }
            .let { lista ->
                when (orden) {
                    OrdenAgenda.FECHA -> lista.sortedWith(
                        compareBy({ fechaComparable(it.fecha) }, { horaComparable(it.hora) })
                    )
                    OrdenAgenda.RECIENTES -> lista.sortedByDescending { it.createdAt }
                    OrdenAgenda.SALDO -> lista.sortedByDescending { it.saldoPendiente }
                    OrdenAgenda.NOMBRE -> lista.sortedBy { it.nombreCliente.lowercase() }
                }
            }
    }

    // La ficha abierta se vuelve a leer de la lista viva, para que al cambiar
    // el estado o el pago se vea al momento sin cerrarla.
    val citaEnFicha = remember(citaAbierta, appointmentsRaw) {
        citaAbierta?.let { abierta -> appointmentsRaw.firstOrNull { it.id == abierta.id } }
    }

    // Sin verticalArrangement: las filas de la tabla van pegadas unas a otras,
    // como en una hoja de cálculo. Los controles de arriba llevan su propio
    // margen inferior.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Resumen de lo inmediato: el contrato pide confirmar la víspera,
        // así el estudio ve a quién llamar sin buscar en toda la lista.
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
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

        item {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                label = { Text("Buscar cliente, teléfono o paquete") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )
        }

        // El calendario se despliega solo si hace falta: si no, se come media
        // pantalla antes de llegar a la lista.
        item {
            OutlinedButton(
                onClick = { mostrarCalendario = !mostrarCalendario },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        diaSeleccionado != null -> "Día filtrado — tocar para cambiar"
                        mostrarCalendario -> "Ocultar el calendario"
                        else -> "Buscar por calendario"
                    }
                )
            }
        }

        if (mostrarCalendario || diaSeleccionado != null) {
            item {
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    CalendarioAgenda(
                        citasPorDia = citasPorDia,
                        diaSeleccionado = diaSeleccionado,
                        onDiaSeleccionado = { diaSeleccionado = it }
                    )
                }
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Text(
                    text = "Ordenar:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OrdenAgenda.values().forEach { opcion ->
                        FilterChip(
                            selected = orden == opcion,
                            onClick = { orden = opcion },
                            label = { Text(opcion.etiqueta) }
                        )
                    }
                }

                Text(
                    text = "Filtrar por estado:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filtroEstado.isBlank(),
                        onClick = { filtroEstado = "" },
                        label = { Text("Todas") }
                    )
                    EstadoCita.TODOS.forEach { estado ->
                        FilterChip(
                            selected = filtroEstado == estado,
                            onClick = { filtroEstado = if (filtroEstado == estado) "" else estado },
                            label = { Text(estado) }
                        )
                    }
                }

                val palabra = if (appointmentsRaw.size == 1) "reservación" else "reservaciones"
                Text(
                    text = "${appointments.size} de ${appointmentsRaw.size} $palabra" +
                        "  •  toca una fila para ver la ficha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (appointments.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
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
                        text = if (appointmentsRaw.isEmpty())
                            "No hay reservaciones registradas aún"
                        else "Ninguna reservación coincide con la búsqueda",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item { CabeceraTabla() }

            itemsIndexed(appointments) { indice, appt ->
                FilaCita(
                    cita = appt,
                    rayada = indice % 2 == 1,
                    esHoy = fechaComparable(appt.fecha) == hoy,
                    onAbrir = { citaAbierta = appt }
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Ficha completa de una reservación
    // ---------------------------------------------------------------------
    citaEnFicha?.let { appt ->
        Dialog(
            onDismissRequest = { citaAbierta = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = appt.nombreCliente,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${appt.fecha} • ${appt.hora}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { citaAbierta = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar ficha")
                        }
                    }

                    Divider()

                    DatoFicha("Teléfono", appt.telefono)
                    DatoFicha("Paquete", appt.detalleSeleccion)
                    DatoFicha("Notas", appt.notas)
                    DatoFicha(
                        "Registrada",
                        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(appt.createdAt))
                    )

                    Divider()

                    EstadoSelector(
                        estadoActual = appt.estado,
                        onCambiar = { nuevo -> viewModel.updateAppointmentStatus(appt.id, nuevo) }
                    )

                    // Pagos
                    if (appt.montoAcordado > 0.0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (appt.saldoPendiente <= 0.0)
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Acordado $${String.format("%.2f", appt.montoAcordado)}  •  " +
                                    "Anticipo $${String.format("%.2f", appt.anticipoPagado)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (appt.saldoPendiente <= 0.0) "✅ Pagado completo"
                                else "Saldo pendiente: $${String.format("%.2f", appt.saldoPendiente)} USD",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (appt.saldoPendiente <= 0.0)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            if (appt.saldoPendiente > 0.0) {
                                viewModel.equivalenciasLinea(appt.saldoPendiente)?.let { eq ->
                                    Text(
                                        text = eq,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = { appointmentPendingPayment = appt },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Editar pago")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { appointmentPendingPayment = appt },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrar pago")
                        }
                    }

                    Divider()

                    // Firma y foto
                    val cuantasFotos =
                        listOfNotNull(appt.fotoClienteBytes, appt.fotoCliente2Bytes).size
                    if (appt.firmaBytes != null || cuantasFotos > 0) {
                        OutlinedButton(
                            onClick = { selectedAppointmentForSignature = appt },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (cuantasFotos) {
                                    0 -> "Ver firma digital"
                                    1 -> "Ver firma y foto"
                                    else -> "Ver firma y fotos"
                                }
                            )
                        }
                    } else {
                        Text(
                            text = "Sin firma registrada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Acciones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val tel = appt.telefono.filter { it.isDigit() || it == '+' }
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir el teléfono", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Llamar")
                        }
                        OutlinedButton(
                            onClick = { agregarCitaAlCalendarioDelEquipo(context, appt, studioConfig) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calendario")
                        }
                    }

                    // El contrato como documento, para entregárselo al cliente.
                    OutlinedButton(
                        onClick = {
                            try {
                                val carpeta = File(context.cacheDir, "exports")
                                val archivo = viewModel.generarContratoPdf(carpeta, appt)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    archivo
                                )
                                val enviar = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, archivo.name)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(
                                    Intent.createChooser(enviar, "Enviar o guardar el contrato")
                                )
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "No se pudo crear el PDF: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contrato_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contrato en PDF para el cliente")
                    }

                    OutlinedButton(
                        onClick = {
                            val uriStr = viewModel.generateAppointmentWhatsAppUri(appt)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar al estudio por WhatsApp")
                    }

                    OutlinedButton(
                        onClick = {
                            val uriStr = viewModel.generateAppointmentWhatsAppUri(appt, enviarACliente = true)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "No se pudo abrir WhatsApp del cliente",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar copia al cliente")
                    }

                    TextButton(
                        onClick = { appointmentPendingDelete = appt },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar reservación", color = MaterialTheme.colorScheme.error)
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
                        else "Saldo pendiente: $${String.format("%.2f", saldo)} USD",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (saldo <= 0.0 && monto > 0.0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    if (saldo > 0.0) {
                        viewModel.equivalencias(saldo).forEach { eq ->
                            Text(
                                text = eq,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                        "${appt.hora}, junto con su firma digital y la foto de confirmación." +
                        "\n\nEsta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAppointment(appt.id)
                        appointmentPendingDelete = null
                        citaAbierta = null
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

    // Firma y foto de confirmación
    selectedAppointmentForSignature?.let { appt ->
        val firma = remember(appt) {
            appt.firmaBytes?.let {
                try {
                    BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
        // Las dos fotos del contrato: el cliente y su documento.
        val fotos = remember(appt) {
            listOfNotNull(appt.fotoClienteBytes, appt.fotoCliente2Bytes).mapNotNull {
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
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Contrato de ${appt.nombreCliente}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${appt.fecha} • ${appt.hora}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Firma digital",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (firma != null) {
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
                                bitmap = firma,
                                contentDescription = "Firma digital",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Esta cita no tiene firma guardada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = if (fotos.size > 1) "Fotos de confirmación"
                        else "Foto de confirmación",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fotos.isEmpty()) {
                        Text(
                            text = "Esta cita no tiene foto de confirmación",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        fotos.forEachIndexed { i, imagen ->
                            Image(
                                bitmap = imagen,
                                contentDescription = "Foto ${i + 1} tomada al firmar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black)
                            )
                        }
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
