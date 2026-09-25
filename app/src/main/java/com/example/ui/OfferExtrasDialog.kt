package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CatalogItem
import com.example.data.Carpeta

// Qué se ofrece aquí, en qué carpeta y a qué precio lo decide el
// administrador desde el panel (pestaña Servicios → "Agregar algo más").
// Cada servicio dice en qué tipos de paquete aparece; una carpeta que se
// queda sin nada para este paquete no se enseña.

// ------------------------------------------------------------------
// Pantalla
// ------------------------------------------------------------------

@Composable
fun OfferExtrasDialog(
    item: CatalogItem,
    // La lista de "Agregar algo más" que mantiene el administrador.
    carpetas: List<Carpeta>,
    enPulgadas: Boolean,
    // Total del pedido completo, para que se vea crecer con cada toque.
    totalPedido: Double,
    cupLabelFor: (Double) -> String?,
    // Cuántas unidades de este extra hay ya en el pedido. Sale del carrito,
    // que es la única verdad: así el número de la ficha nunca se desfasa.
    cantidadEnPedido: (ExtraOption) -> Int,
    onAgregar: (ExtraOption) -> Unit,
    onQuitar: (ExtraOption) -> Unit,
    onDismiss: () -> Unit
) {
    val ramas = remember(item.category, carpetas, enPulgadas) {
        ramasDe(carpetas, paquete = item.category, enPulgadas = enPulgadas)
    }
    // Solo una carpeta abierta a la vez: al abrir otra, la anterior se cierra
    // y la lista se recoloca sola. Es lo que da la sensación de que las
    // carpetas se apilan y se desplazan.
    var abierta by rememberSaveable { mutableStateOf<String?>(ramas.firstOrNull()?.clave) }

    Dialog(
        onDismissRequest = onDismiss,
        // Como en el contrato: el margen de las barras del sistema se pone a
        // mano para que el botón "Listo" no quede bajo la barra de la tableta.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ---- Cabecera con el total que va subiendo ----
                CabeceraExtras(
                    nombrePaquete = item.code.ifBlank { item.name },
                    total = totalPedido,
                    equivalencia = cupLabelFor(totalPedido),
                    onDismiss = onDismiss
                )

                // ---- Las carpetas ----
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(ramas, key = { _, r -> r.clave }) { _, rama ->
                        CarpetaRama(
                            rama = rama,
                            abierta = abierta == rama.clave,
                            cantidadEnPedido = cantidadEnPedido,
                            onAbrir = {
                                abierta = if (abierta == rama.clave) null else rama.clave
                            },
                            onAgregar = onAgregar,
                            onQuitar = onQuitar
                        )
                    }
                }

                // ---- Cerrar ----
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .height(54.dp)
                            .testTag("btn_listo_extras"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Listo",
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

/** Barra de arriba: el paquete y el total, que sube solo al ir tocando. */
@Composable
private fun CabeceraExtras(
    nombrePaquete: String,
    total: Double,
    equivalencia: String?,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agregar algo más",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "sobre $nombrePaquete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "TOTAL DEL PEDIDO",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                // El importe entra desde abajo cada vez que cambia: se ve subir.
                ImporteAnimado(
                    valor = total,
                    color = MaterialTheme.colorScheme.onPrimary,
                    estilo = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            if (equivalencia != null) {
                Text(
                    text = equivalencia,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
        }
    }
}
