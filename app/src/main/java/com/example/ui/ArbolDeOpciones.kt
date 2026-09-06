package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * El árbol de carpetas con el que se eligen cosas sueltas.
 *
 * Lo usan las dos pantallas donde el cliente arma su pedido a mano —"Agregar
 * algo más" dentro de un paquete, y "Diseña tu propia oferta"—, y viven aquí
 * juntas para que se comporten y se vean exactamente igual: si una cambia,
 * cambian las dos.
 */

/** Una cosa que se puede añadir al paquete. */
data class ExtraOption(
    val title: String,
    val description: String,
    val category: String,
    val variantName: String,
    val price: Double
)

// ------------------------------------------------------------------
// El árbol: ramas, subramas y hojas
// ------------------------------------------------------------------

/** Un grupo dentro de una rama. Sin nombre = las opciones cuelgan directas. */
internal data class Subrama(
    val nombre: String?,
    val opciones: List<ExtraOption>
)

/** Una carpeta principal del catálogo. */
internal data class Rama(
    val nombre: String,
    val simbolo: String,
    val subramas: List<Subrama>
) {
    val opciones: List<ExtraOption> get() = subramas.flatMap { it.opciones }
}

internal fun hoja(nombre: String?, vararg opciones: ExtraOption) =
    Subrama(nombre, opciones.toList())


/** El importe grande de la cabecera, que entra desde abajo cuando cambia. */
@Composable
fun ImporteAnimado(
    valor: Double,
    color: Color,
    estilo: androidx.compose.ui.text.TextStyle
) {
    AnimatedContent(
        targetState = valor,
        transitionSpec = {
            val sube = targetState > initialState
            val desde = if (sube) 1 else -1
            (
                slideInVertically(tween(260, easing = FastOutSlowInEasing)) { alto ->
                    desde * alto
                } + fadeIn(tween(200))
                ) togetherWith (
                slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { alto ->
                    -desde * alto
                } + fadeOut(tween(140))
                )
        },
        label = "importe_animado"
    ) { v ->
        Text(
            text = "$${String.format("%.2f", v)} USD",
            style = estilo,
            color = color,
            maxLines = 1
        )
    }
}

/** Una carpeta: su nombre queda siempre visible, abierta o cerrada. */
@Composable
fun CarpetaRama(
    rama: Rama,
    abierta: Boolean,
    cantidadEnPedido: (ExtraOption) -> Int,
    onAbrir: () -> Unit,
    onAgregar: (ExtraOption) -> Unit,
    onQuitar: (ExtraOption) -> Unit
) {
    val enPedido = rama.opciones.sumOf { cantidadEnPedido(it) }

    val giro by animateFloatAsState(
        targetValue = if (abierta) 90f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "giro_carpeta"
    )
    val fondo by animateColorAsState(
        targetValue = if (abierta) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(240),
        label = "fondo_carpeta"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        elevation = CardDefaults.cardElevation(defaultElevation = if (abierta) 6.dp else 1.dp)
    ) {
        // Cabecera de la carpeta: se queda a la vista aunque esté abierta, para
        // no perder de dónde cuelga lo que se está viendo.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAbrir() }
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .testTag("rama_${rama.nombre}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = rama.simbolo, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = rama.nombre,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (enPedido > 0) {
                Text(
                    text = enPedido.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (abierta) "Cerrar" else "Abrir",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = giro }
            )
        }

        // El contenido se despliega hacia abajo, como una carpeta que se abre.
        AnimatedVisibility(
            visible = abierta,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rama.subramas.forEach { sub ->
                    if (sub.opciones.isEmpty()) return@forEach
                    if (sub.nombre != null) {
                        Text(
                            text = "└  ${sub.nombre}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    sub.opciones.forEach { opcion ->
                        FilaExtra(
                            opcion = opcion,
                            cantidad = cantidadEnPedido(opcion),
                            sangrado = sub.nombre != null,
                            onAgregar = { onAgregar(opcion) },
                            onQuitar = { onQuitar(opcion) }
                        )
                    }
                }
            }
        }
    }
}

/** Una cosa concreta. Un toque la añade; no hay botón aparte. */
@Composable
private fun FilaExtra(
    opcion: ExtraOption,
    cantidad: Int,
    sangrado: Boolean,
    onAgregar: () -> Unit,
    onQuitar: () -> Unit
) {
    val elegida = cantidad > 0
    val fondo by animateColorAsState(
        targetValue = if (elegida) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "fondo_extra"
    )
    val textoColor = if (elegida) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (sangrado) 14.dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .clickable { onAgregar() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = opcion.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = textoColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = opcion.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (elegida) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$${String.format("%.2f", opcion.price)} USD",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = if (elegida) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (elegida) {
            // Solo aparece cuando ya hay algo puesto, para poder deshacer un
            // toque de más. No es un botón de añadir: eso lo hace la fila.
            Text(
                text = "−",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f))
                    .clickable { onQuitar() }
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .testTag("quitar_${opcion.variantName}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "×$cantidad",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Tocar para añadir",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
