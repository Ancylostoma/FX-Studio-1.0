package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppointmentEntity
import com.example.data.CatalogItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Resumen del día para quien atiende el estudio, sin pasar por el PIN del
 * panel: cuántas reservaciones se hicieron, de qué tipo de sesión, cuánto se
 * acordó y cuánto entró de anticipo, y qué sesiones tocan ese día.
 *
 * Solo lee: desde aquí no se puede cambiar ni borrar nada. Tampoco enseña
 * los teléfonos de los clientes, porque la pantalla la puede ver cualquiera.
 */

// ------------------------------------------------------------------
// Cálculos
// ------------------------------------------------------------------

private const val SIN_TIPO = "Oferta propia / servicios"

/**
 * De qué tipo de sesión es una reservación, leyendo lo que se apuntó en
 * "paquete o servicio". Primero busca el nombre de un paquete del catálogo;
 * si no, el código (A1, B3, Qt5…); si tampoco, es una oferta armada a mano.
 */
internal fun tipoDeSesion(detalle: String, catalogo: List<CatalogItem>): String {
    if (detalle.isBlank()) return "Sin especificar"
    catalogo.firstOrNull { it.name.isNotBlank() && detalle.contains(it.name, ignoreCase = true) }
        ?.let { return it.category }
    val codigo = Regex("""\b(Qt|A|B|C)\d{1,2}\b""").find(detalle)?.groupValues?.get(1)
    return when {
        codigo == "A" -> "Primer Año"
        codigo == "B" -> "Bodas"
        codigo == "Qt" || codigo == "C" -> "15 años"
        detalle.contains("Oferta FX", ignoreCase = true) -> "15 años"
        else -> SIN_TIPO
    }
}

private val formatoCalendario = SimpleDateFormat("dd/MM/yyyy", Locale.US)
private val formatoClave = SimpleDateFormat("yyyyMMdd", Locale.US)
private val formatoHora = SimpleDateFormat("h:mm a", Locale.US)

private fun dinero(v: Double) = "$${String.format("%.2f", v)}"

/** Todo lo que se enseña de un día, ya calculado. */
private data class Informe(
    val fechaTexto: String,
    val reservasHechas: List<AppointmentEntity>,
    val sesionesDelDia: List<AppointmentEntity>,
    val porTipo: List<Triple<String, Int, Double>>,
    val acordado: Double,
    val anticipos: Double
)

private fun informeDe(
    dia: Date,
    citas: List<AppointmentEntity>,
    catalogo: List<CatalogItem>
): Informe {
    val clave = formatoClave.format(dia)
    val fechaTexto = formatoCalendario.format(dia)
    val hechas = citas
        .filter { formatoClave.format(Date(it.createdAt)) == clave }
        .sortedBy { it.createdAt }
    val sesiones = citas
        .filter { it.fecha == fechaTexto }
        .sortedBy { horaEnMinutos(it.hora) }
    val porTipo = hechas
        .groupBy { tipoDeSesion(it.detalleSeleccion, catalogo) }
        .map { (tipo, lista) -> Triple(tipo, lista.size, lista.sumOf { it.montoAcordado }) }
        .sortedByDescending { it.second }
    return Informe(
        fechaTexto = fechaTexto,
        reservasHechas = hechas,
        sesionesDelDia = sesiones,
        porTipo = porTipo,
        acordado = hechas.sumOf { it.montoAcordado },
        anticipos = hechas.sumOf { it.anticipoPagado }
    )
}

/** "10:00 AM" → minutos del día, para ordenar las sesiones. */
private fun horaEnMinutos(hora: String): Int {
    val m = Regex("""(\d{1,2}):(\d{2})\s*([AaPp])""").find(hora) ?: return 24 * 60
    var h = m.groupValues[1].toInt() % 12
    if (m.groupValues[3].equals("p", ignoreCase = true)) h += 12
    return h * 60 + m.groupValues[2].toInt()
}

/** El mismo informe en texto, para mandarlo por WhatsApp o guardarlo. */
private fun informeEnTexto(inf: Informe, catalogo: List<CatalogItem>, estudio: String): String {
    val sb = StringBuilder()
    sb.append("📋 *$estudio — Resumen del ${inf.fechaTexto}*\n\n")
    sb.append("Reservaciones hechas: ${inf.reservasHechas.size}\n")
    sb.append("Importe acordado: ${dinero(inf.acordado)} USD\n")
    sb.append("Cobrado en anticipos: ${dinero(inf.anticipos)} USD\n")
    sb.append("Sesiones de este día: ${inf.sesionesDelDia.size}\n")
    if (inf.porTipo.isNotEmpty()) {
        sb.append("\n*Por tipo de sesión*\n")
        inf.porTipo.forEach { (tipo, n, monto) ->
            sb.append("• $tipo: $n — ${dinero(monto)}\n")
        }
    }
    if (inf.reservasHechas.isNotEmpty()) {
        sb.append("\n*Reservaciones hechas*\n")
        inf.reservasHechas.forEach { c ->
            sb.append(
                "• ${c.nombreCliente} — ${tipoDeSesion(c.detalleSeleccion, catalogo)} — " +
                    "sesión ${c.fecha} ${c.hora} — ${dinero(c.montoAcordado)}" +
                    " (anticipo ${dinero(c.anticipoPagado)})\n"
            )
        }
    }
    if (inf.sesionesDelDia.isNotEmpty()) {
        sb.append("\n*Sesiones de este día*\n")
        inf.sesionesDelDia.forEach { c ->
            sb.append(
                "• ${c.hora} ${c.nombreCliente} — ${tipoDeSesion(c.detalleSeleccion, catalogo)} — " +
                    "${c.estado} — debe ${dinero(c.saldoPendiente)}\n"
            )
        }
    }
    return sb.toString()
}

// ------------------------------------------------------------------
// Pantalla
// ------------------------------------------------------------------

@Composable
fun ResumenDelDiaDialog(
    viewModel: StudioViewModel,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val citas by viewModel.appointments.collectAsState()
    val catalogo by viewModel.catalogItems.collectAsState()
    val config by viewModel.studioConfig.collectAsState()

    // Días hacia atrás desde hoy: 0 = hoy, 1 = ayer…
    var diasAtras by remember { mutableStateOf(0) }
    val dia = remember(diasAtras) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -diasAtras) }.time
    }
    val informe = remember(dia, citas, catalogo) { informeDe(dia, citas, catalogo) }

    Dialog(
        onDismissRequest = onCerrar,
        properties = VentanaCompleta
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .margenBarrasSistema()
                .padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ---- Cabecera: título y día ----
                Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Resumen del día",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onCerrar) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { diasAtras += 1 }) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Día anterior",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = when (diasAtras) {
                                    0 -> "Hoy, ${informe.fechaTexto}"
                                    1 -> "Ayer, ${informe.fechaTexto}"
                                    else -> informe.fechaTexto
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { if (diasAtras > 0) diasAtras -= 1 },
                                enabled = diasAtras > 0
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Día siguiente",
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(
                                        alpha = if (diasAtras > 0) 1f else 0.3f
                                    )
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ---- Las cuatro cifras ----
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Cifra(
                                    "Reservaciones hechas",
                                    informe.reservasHechas.size.toString(),
                                    null,
                                    Modifier.weight(1f)
                                )
                                Cifra(
                                    "Sesiones de este día",
                                    informe.sesionesDelDia.size.toString(),
                                    null,
                                    Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Cifra(
                                    "Importe acordado",
                                    dinero(informe.acordado),
                                    viewModel.cupLabel(informe.acordado),
                                    Modifier.weight(1f)
                                )
                                Cifra(
                                    "Cobrado en anticipos",
                                    dinero(informe.anticipos),
                                    viewModel.cupLabel(informe.anticipos),
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ---- Por tipo de sesión ----
                    item {
                        Tabla(
                            titulo = "Por tipo de sesión",
                            columnas = listOf("Tipo" to 1.6f, "Reservas" to 0.8f, "Importe" to 1f),
                            filas = informe.porTipo.map { (tipo, n, monto) ->
                                listOf(tipo, n.toString(), dinero(monto))
                            },
                            vacio = "No se hizo ninguna reservación este día."
                        )
                    }

                    // ---- Reservaciones hechas ----
                    item {
                        Tabla(
                            titulo = "Reservaciones hechas",
                            columnas = listOf(
                                "Hora" to 0.7f, "Cliente" to 1.3f, "Tipo" to 1.1f,
                                "Sesión" to 1.1f, "Importe" to 0.9f
                            ),
                            filas = informe.reservasHechas.map { c ->
                                listOf(
                                    formatoHora.format(Date(c.createdAt)),
                                    c.nombreCliente,
                                    tipoDeSesion(c.detalleSeleccion, catalogo),
                                    "${c.fecha}\n${c.hora}",
                                    dinero(c.montoAcordado) +
                                        if (c.anticipoPagado > 0) "\nant. ${dinero(c.anticipoPagado)}" else ""
                                )
                            },
                            vacio = "Ninguna."
                        )
                    }

                    // ---- Sesiones que tocan ese día ----
                    item {
                        Tabla(
                            titulo = "Sesiones de este día",
                            columnas = listOf(
                                "Hora" to 0.8f, "Cliente" to 1.3f, "Tipo" to 1.1f,
                                "Estado" to 1f, "Debe" to 0.8f
                            ),
                            filas = informe.sesionesDelDia.map { c ->
                                listOf(
                                    c.hora,
                                    c.nombreCliente,
                                    tipoDeSesion(c.detalleSeleccion, catalogo),
                                    c.estado,
                                    dinero(c.saldoPendiente)
                                )
                            },
                            vacio = "No hay sesiones agendadas para este día."
                        )
                    }
                }

                // ---- Compartir ----
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val texto = informeEnTexto(informe, catalogo, config.titulo)
                            val envio = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, texto)
                            }
                            try {
                                context.startActivity(Intent.createChooser(envio, "Enviar resumen"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo compartir", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .height(50.dp)
                            .testTag("btn_compartir_resumen"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar este resumen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Una cifra grande con su etiqueta. */
@Composable
private fun Cifra(etiqueta: String, valor: String, equivalencia: String?, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1
            )
            if (equivalencia != null) {
                Text(
                    text = equivalencia,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Una tabla sencilla: cabecera de color y filas alternas. */
@Composable
private fun Tabla(
    titulo: String,
    columnas: List<Pair<String, Float>>,
    filas: List<List<String>>,
    vacio: String
) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (filas.isEmpty()) {
            Text(
                text = vacio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else Column(modifier = Modifier.clip(RoundedCornerShape(10.dp))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                columnas.forEach { (nombre, peso) ->
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(peso).padding(end = 4.dp)
                    )
                }
            }
            filas.forEachIndexed { i, fila ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (i % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    fila.forEachIndexed { j, celda ->
                        Text(
                            text = celda,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (j == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(columnas.getOrNull(j)?.second ?: 1f)
                                .padding(end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
