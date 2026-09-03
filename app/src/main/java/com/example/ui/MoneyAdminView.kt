package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppointmentEntity
import com.example.data.EstadoCita
import java.util.Calendar
import java.util.Locale

/**
 * Pestaña "Dinero" del panel del administrador.
 *
 * No pide ningún dato nuevo: todo sale de lo que ya se guarda en cada cita
 * (monto acordado, anticipo pagado y etapa del trabajo). Hasta ahora esos
 * números solo se veían de uno en uno, dentro de la ficha de cada cliente;
 * aquí se suman para que el estudio pueda ver el mes completo de un vistazo
 * y, sobre todo, para que no se le olvide cobrar un saldo.
 */

// ------------------------------------------------------------------
// Cálculos
// ------------------------------------------------------------------

/** Convierte "dd/MM/yyyy" en aaaamm, para agrupar por mes. */
internal fun mesComparable(fecha: String): Int {
    val dia = fechaComparable(fecha)
    return if (dia == Int.MAX_VALUE) Int.MAX_VALUE else dia / 100
}

/** Las cifras del mes que se enseñan arriba del todo. */
internal data class ResumenMes(
    val cobrado: Double,
    val porCobrar: Double,
    val acordado: Double,
    val trabajos: Int
)

/**
 * Suma un mes concreto. El anticipo nunca se cuenta por encima del monto
 * acordado, para que una cifra mal tecleada no infle el total cobrado.
 */
internal fun resumenDelMes(citas: List<AppointmentEntity>): ResumenMes {
    var cobrado = 0.0
    var acordado = 0.0
    for (c in citas) {
        val total = c.montoAcordado.coerceAtLeast(0.0)
        acordado += total
        cobrado += c.anticipoPagado.coerceIn(0.0, total)
    }
    return ResumenMes(
        cobrado = cobrado,
        porCobrar = (acordado - cobrado).coerceAtLeast(0.0),
        acordado = acordado,
        trabajos = citas.size
    )
}

/** Cuántos trabajos hay en cada etapa, en el orden del contrato. */
internal fun conteoPorEstado(citas: List<AppointmentEntity>): List<Pair<String, Int>> =
    EstadoCita.TODOS.map { estado -> estado to citas.count { it.estado == estado } }

/** "$1,234.56" — siempre con punto de miles y dos decimales. */
private fun usd(valor: Double): String = "$" + String.format(Locale.US, "%,.2f", valor)

private val MESES_DINERO = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

// ------------------------------------------------------------------
// Pantalla
// ------------------------------------------------------------------

@Composable
fun MoneyAdminView(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val citas by viewModel.appointments.collectAsState()

    // Mes que se está mirando. Arranca en el mes actual del teléfono.
    val hoy = remember { Calendar.getInstance() }
    var anio by rememberSaveable { mutableStateOf(hoy.get(Calendar.YEAR)) }
    var mes by rememberSaveable { mutableStateOf(hoy.get(Calendar.MONTH) + 1) }

    val clave = anio * 100 + mes
    val citasDelMes = remember(citas, clave) {
        citas.filter { mesComparable(it.fecha) == clave }
    }
    val resumen = remember(citasDelMes) { resumenDelMes(citasDelMes) }
    val estados = remember(citasDelMes) { conteoPorEstado(citasDelMes) }

    // La lista de deudas es de TODOS los meses: un saldo de junio se cobra
    // igual en agosto, y esconderlo al cambiar de mes sería justo el error
    // que esta pantalla viene a evitar.
    val deudores = remember(citas) {
        citas.filter { it.saldoPendiente > 0.009 }
            .sortedByDescending { it.saldoPendiente }
    }
    val totalPendiente = remember(deudores) { deudores.sumOf { it.saldoPendiente } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- selector de mes ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (mes == 1) { mes = 12; anio -= 1 } else mes -= 1
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                }
                Text(
                    text = "${MESES_DINERO[(mes - 1).coerceIn(0, 11)]} $anio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                IconButton(onClick = {
                    if (mes == 12) { mes = 1; anio += 1 } else mes += 1
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                }
            }
        }

        // ---- las tres cifras ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CifraGrande(
                    modifier = Modifier.weight(1f),
                    titulo = "COBRADO",
                    valor = usd(resumen.cobrado),
                    detalle = viewModel.cupLabel(resumen.cobrado),
                    color = MaterialTheme.colorScheme.tertiary,
                    fondo = MaterialTheme.colorScheme.tertiaryContainer
                )
                CifraGrande(
                    modifier = Modifier.weight(1f),
                    titulo = "POR COBRAR",
                    valor = usd(resumen.porCobrar),
                    detalle = viewModel.cupLabel(resumen.porCobrar),
                    color = MaterialTheme.colorScheme.error,
                    fondo = MaterialTheme.colorScheme.surfaceVariant
                )
                CifraGrande(
                    modifier = Modifier.weight(1f),
                    titulo = "TRABAJOS",
                    valor = resumen.trabajos.toString(),
                    detalle = if (resumen.trabajos == 1) "sesión" else "sesiones",
                    color = MaterialTheme.colorScheme.primary,
                    fondo = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        if (resumen.acordado > 0.009) {
            item {
                Text(
                    text = "Acordado en el mes: ${usd(resumen.acordado)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- quién me debe ----
        item {
            TituloSeccion(
                titulo = "Quién me debe",
                apoyo = if (deudores.isEmpty()) "nadie, todo cobrado"
                        else "de todos los meses, mayor deuda primero"
            )
        }

        if (deudores.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No hay saldos pendientes.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        } else {
            itemsIndexed(deudores) { indice, cita ->
                FilaDeuda(
                    cita = cita,
                    par = indice % 2 == 0,
                    equivalencia = viewModel.cupLabel(cita.saldoPendiente),
                    onLlamar = {
                        val tel = cita.telefono.filter { it.isDigit() || it == '+' }
                        if (tel.isBlank()) {
                            Toast.makeText(
                                context,
                                "Esta cita no tiene teléfono guardado",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            abrirIntent(
                                context,
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")),
                                "No se pudo abrir el teléfono"
                            )
                        }
                    },
                    onCobrar = {
                        abrirIntent(
                            context,
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(viewModel.generateCobroWhatsAppUri(cita))
                            ),
                            "WhatsApp no está instalado"
                        )
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "Total pendiente: ${usd(totalPendiente)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        viewModel.cupLabel(totalPendiente)?.let { linea ->
                            Text(
                                text = linea,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${deudores.size} " +
                                if (deudores.size == 1) "cliente" else "clientes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---- en qué va cada trabajo ----
        item {
            TituloSeccion(
                titulo = "En qué va cada trabajo",
                apoyo = "${MESES_DINERO[(mes - 1).coerceIn(0, 11)]} $anio"
            )
        }

        item {
            val mayor = estados.maxOfOrNull { it.second } ?: 0
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mayor == 0) {
                        Text(
                            text = "Todavía no hay citas en este mes.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        estados.forEach { (nombre, cantidad) ->
                            BarraEstado(
                                nombre = nombre,
                                cantidad = cantidad,
                                mayor = mayor,
                                color = colorDeEstado(nombre)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Estas cifras salen del monto acordado y del anticipo que se " +
                    "anota en cada cita. Nada de esto sale del teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

// ------------------------------------------------------------------
// Piezas sueltas
// ------------------------------------------------------------------

/** Abre una app externa avisando en pantalla si no hay ninguna que sirva. */
private fun abrirIntent(
    context: android.content.Context,
    intent: Intent,
    mensajeSiFalla: String
) {
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, mensajeSiFalla, Toast.LENGTH_SHORT).show()
    }
}

/** Cada etapa con su color, para que la barra se lea sin leer el nombre. */
@Composable
private fun colorDeEstado(estado: String): Color = when (estado) {
    EstadoCita.RESERVADA -> MaterialTheme.colorScheme.primary
    EstadoCita.SESION_HECHA -> MaterialTheme.colorScheme.secondary
    EstadoCita.EN_EDICION -> MaterialTheme.colorScheme.tertiary
    EstadoCita.LISTA -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}

@Composable
private fun TituloSeccion(titulo: String, apoyo: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = apoyo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Una de las tres cifras de arriba. El ancho lo reparte el Row que la llama
 * con weight(1f); aquí todo va centrado y en una sola línea.
 */
@Composable
private fun CifraGrande(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String,
    detalle: String?,
    color: Color,
    fondo: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detalle ?: " ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Una línea de "Quién me debe", con el botón de llamar y el de cobrar. */
@Composable
private fun FilaDeuda(
    cita: AppointmentEntity,
    par: Boolean,
    equivalencia: String?,
    onLlamar: () -> Unit,
    onCobrar: () -> Unit
) {
    val fondo = if (par) MaterialTheme.colorScheme.surface
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Los datos del cliente se llevan todo el ancho sobrante; sin este
            // weight el texto se estrujaría en una letra por línea.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cita.nombreCliente.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cita.fecha} · ${cita.detalleSeleccion.replace("\n", " ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cita.estado,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = usd(cita.saldoPendiente),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
                if (equivalencia != null) {
                    Text(
                        text = equivalencia,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    IconButton(onClick = onLlamar) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Llamar a ${cita.nombreCliente}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onCobrar) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Recordar el pago por WhatsApp",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/** Una barra de "En qué va cada trabajo". */
@Composable
private fun BarraEstado(
    nombre: String,
    cantidad: Int,
    mayor: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nombre,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(130.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (cantidad > 0 && mayor > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((cantidad.toFloat() / mayor.toFloat()).coerceIn(0.08f, 1f))
                        .clip(RoundedCornerShape(11.dp))
                        .background(color)
                )
            }
        }
        Text(
            text = cantidad.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(40.dp)
        )
    }
}
