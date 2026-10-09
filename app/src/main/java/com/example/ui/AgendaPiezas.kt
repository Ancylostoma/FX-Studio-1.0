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

/*
 * Piezas de la pestaña Citas: fechas comparables, selector de estado, resumen
 * del día, calendario del mes y las filas de la tabla de citas.
 */

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

internal val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

internal val DIAS_CORTOS = listOf("L", "M", "M", "J", "V", "S", "D")

/** Formas de ordenar la agenda desde el panel del admin. */
internal enum class OrdenAgenda(val etiqueta: String) {
    FECHA("Por fecha de sesión"),
    RECIENTES("Últimas reservadas"),
    SALDO("Mayor saldo pendiente"),
    NOMBRE("Nombre A – Z")
}

/** Chips con las etapas del trabajo; la actual queda resaltada. */
@Composable
internal fun EstadoSelector(
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
internal fun ResumenDiaCard(
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
internal fun CalendarioAgenda(
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
internal fun CabeceraTabla() {
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
internal fun FilaCita(
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
internal fun DatoFicha(etiqueta: String, valor: String) {
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
