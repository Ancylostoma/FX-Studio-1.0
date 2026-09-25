package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * "Diseña tu propia oferta".
 *
 * Antes eran ocho tarjetas abiertas a la vez, cada una con sus chips, su
 * selector de cantidad y su botón de añadir: había que pasar por delante de
 * todo el catálogo aunque solo se quisiera una foto suelta.
 *
 * Ahora es el mismo árbol de carpetas de "Agregar algo más": una abierta a la
 * vez, el nombre de la carpeta siempre a la vista, un toque añade y el total
 * sube arriba. Las dos pantallas comparten los componentes de
 * ArbolDeOpciones.kt, así que se comportan igual.
 */

// Las carpetas y sus precios ya no están aquí: las edita el administrador
// desde el panel (pestaña Servicios) y viven en la base de datos. La lista
// con la que viene la app está en data/ServiciosDeFabrica.kt.

// ------------------------------------------------------------------
// Pantalla
// ------------------------------------------------------------------

@Composable
fun CustomOfferScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val carrito by viewModel.cart.collectAsState()
    val total by viewModel.cartTotal.collectAsState()
    val carpetas by viewModel.serviciosPropia.collectAsState()
    val config by viewModel.studioConfig.collectAsState()
    val ramas = remember(carpetas, config.medidasEnPulgadas) {
        ramasDe(carpetas, paquete = null, enPulgadas = config.medidasEnPulgadas)
    }
    var confirmarBorrado by remember { mutableStateOf(false) }

    // Solo una carpeta abierta a la vez. Se arranca con todas cerradas: la
    // primera pantalla que ve el cliente es la lista corta de ocho nombres,
    // no ochenta precios.
    var abierta by rememberSaveable { mutableStateOf<String?>(null) }

    val cantidadEnPedido: (ExtraOption) -> Int = { extra ->
        carrito.firstOrNull {
            it.item.name == extra.title &&
                it.variant.name == extra.variantName &&
                it.item.category == extra.category
        }?.quantity ?: 0
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ---- Cabecera fija con el total ----
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "Diseña tu propia oferta",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Abre una sección, toca lo que quieras y se va sumando.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "TU PEDIDO",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    ImporteAnimado(
                        valor = total,
                        color = MaterialTheme.colorScheme.onPrimary,
                        estilo = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                viewModel.cupLabel(total)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }

                // Por si el cliente se arrepiente: borra de una vez todo lo
                // que lleva elegido. Solo aparece cuando hay algo que borrar.
                AnimatedVisibility(visible = carrito.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { confirmarBorrado = true },
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .testTag("btn_borrar_oferta_propia"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancelar y borrar mi oferta")
                    }
                }
            }
        }

        // ---- Las carpetas ----
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ramas, key = { it.clave }) { rama ->
                CarpetaRama(
                    rama = rama,
                    abierta = abierta == rama.clave,
                    cantidadEnPedido = cantidadEnPedido,
                    onAbrir = {
                        abierta = if (abierta == rama.clave) null else rama.clave
                    },
                    onAgregar = { extra ->
                        viewModel.addCustomToCart(
                            extra.title,
                            extra.category,
                            extra.variantName,
                            extra.price,
                            1,
                            extra.description
                        )
                    },
                    onQuitar = { extra ->
                        viewModel.quitarUnoDelCarrito(
                            extra.title,
                            extra.category,
                            extra.variantName
                        )
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "FXestudio se reserva el derecho de cambiar estos precios " +
                            "sin previo aviso. Todos los precios están en USD; se acepta " +
                            "Zelle y CUP al cambio del día.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (confirmarBorrado) {
        ConfirmarBorrarPedido(
            onConfirmar = {
                viewModel.cancelarPedido()
                abierta = null
                confirmarBorrado = false
            },
            onCancelar = { confirmarBorrado = false }
        )
    }
}

/**
 * Pregunta antes de borrar el pedido. Se usa aquí y en el resumen del
 * pedido, para que las dos digan lo mismo.
 */
@Composable
fun ConfirmarBorrarPedido(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("¿Borrar todo el pedido?") },
        text = {
            Text(
                "Se quitará todo lo que lleva elegido y el total vuelve a cero. " +
                    "Si ya había firmado el contrato de este pedido, también se descarta."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirmar_borrar_pedido")
            ) {
                Text("Sí, borrar todo")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar) {
                Text("No, seguir")
            }
        }
    )
}
