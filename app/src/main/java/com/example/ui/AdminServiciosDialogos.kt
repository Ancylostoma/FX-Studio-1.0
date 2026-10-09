package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Carpeta
import com.example.data.Medidas
import com.example.data.MenuServicios
import com.example.data.PAQUETES_CON_EXTRAS
import com.example.data.Servicio
import com.example.data.Subcarpeta
import com.example.data.guardarCarpeta
import com.example.data.guardarServicio
import com.example.data.guardarSubcarpeta
import com.example.data.moverCarpeta
import com.example.data.moverServicio
import com.example.data.moverSubcarpeta
import com.example.data.quitarCarpeta
import com.example.data.quitarServicio
import com.example.data.quitarSubcarpeta

/*
 * Diálogos de la pestaña Servicios: cambiar o crear un servicio, una carpeta o
 * una subcarpeta, con su confirmación para borrar.
 */

// ------------------------------------------------------------------
// Diálogos
// ------------------------------------------------------------------

/** Un sitio donde puede ir un servicio: carpeta y subcarpeta. */
private data class Destino(val subId: String, val etiqueta: String)

private fun destinos(carpetas: List<Carpeta>): List<Destino> =
    carpetas.flatMap { c ->
        c.subcarpetas.map { s ->
            Destino(
                s.id,
                "${c.simbolo} ${c.nombre}" + if (s.nombre.isNotBlank()) "  ›  ${s.nombre}" else ""
            )
        }
    }

private fun precioTexto(v: Double): String =
    if (v <= 0.0) "" else if (v == Math.floor(v)) v.toLong().toString() else String.format("%.2f", v)

@Composable
internal fun ServicioDialog(
    edicion: ServicioEnEdicion,
    carpetas: List<Carpeta>,
    mostrarPaquetes: Boolean,
    enPulgadas: Boolean,
    onGuardar: (subId: String, servicio: Servicio) -> Unit,
    onBorrar: () -> Unit,
    onCerrar: () -> Unit
) {
    val original = edicion.servicio
    var titulo by remember { mutableStateOf(original.titulo) }
    var formato by remember { mutableStateOf(original.formato) }
    var descripcion by remember { mutableStateOf(original.descripcion) }
    var precio by remember { mutableStateOf(precioTexto(original.precio)) }
    var paquetes by remember { mutableStateOf(original.paquetes.toSet()) }
    // Uno nuevo nace visible; uno sin precio que se está completando, también
    // se enciende solo en cuanto se le pone precio (ver abajo).
    var visible by remember { mutableStateOf(original.visible || edicion.esNuevo) }
    var subDestino by remember { mutableStateOf(edicion.subId) }
    var menuDestino by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val lugares = remember(carpetas) { destinos(carpetas) }
    val precioNumero = precio.replace(',', '.').toDoubleOrNull()
    val valido = titulo.isNotBlank() && precioNumero != null && precioNumero >= 0.0

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (edicion.esNuevo) "Nuevo servicio" else "Cambiar servicio") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Nombre *") },
                    placeholder = { Text("Ej: Alquiler - Traje azul de hombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("servicio_titulo")
                )
                OutlinedTextField(
                    value = formato,
                    onValueChange = { formato = it.replace('|', '/') },
                    label = { Text("Formato o medida (opcional)") },
                    placeholder = { Text("Ej: 8x12, Talla M, Editado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Medidas.resumen("$titulo $formato", enPulgadas)?.let {
                    Text(
                        text = "📏 El cliente verá: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precio,
                    onValueChange = { nuevo ->
                        precio = nuevo.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    label = { Text("Precio en USD *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("servicio_precio")
                )

                // Dónde va
                Text(
                    text = "Carpeta",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Box {
                    OutlinedButton(
                        onClick = { menuDestino = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = lugares.firstOrNull { it.subId == subDestino }?.etiqueta ?: "Elegir…",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuDestino,
                        onDismissRequest = { menuDestino = false }
                    ) {
                        lugares.forEach { lugar ->
                            DropdownMenuItem(
                                text = { Text(lugar.etiqueta) },
                                onClick = {
                                    subDestino = lugar.subId
                                    menuDestino = false
                                }
                            )
                        }
                    }
                }

                if (mostrarPaquetes) {
                    Text(
                        text = "Aparece en los paquetes de",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PAQUETES_CON_EXTRAS.forEach { p ->
                            FilterChip(
                                selected = p in paquetes,
                                onClick = {
                                    paquetes = if (p in paquetes) paquetes - p else paquetes + p
                                },
                                label = { Text(p) }
                            )
                        }
                    }
                    Text(
                        text = if (paquetes.isEmpty()) "Sin marcar ninguno, sale en todos."
                        else "Solo sale en los marcados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visible para los clientes",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Apagado, no se borra: solo deja de salir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = visible, onCheckedChange = { visible = it })
                }

                if (!edicion.esNuevo) {
                    TextButton(
                        onClick = { confirmarBorrado = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Borrar este servicio")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    precioNumero?.let { p ->
                        // Quien completa el precio de uno que venía "sin
                        // precio" quiere que se vea: se enciende solo.
                        val encender = !original.visible && original.precio <= 0.0 && p > 0.0
                        onGuardar(
                            subDestino,
                            original.copy(
                                titulo = titulo.trim(),
                                formato = formato.trim(),
                                descripcion = descripcion.trim(),
                                precio = p,
                                paquetes = PAQUETES_CON_EXTRAS.filter { it in paquetes },
                                visible = visible || encender
                            )
                        )
                    }
                },
                enabled = valido,
                modifier = Modifier.testTag("servicio_guardar")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar \"${original.titulo}\"?") },
            text = {
                Text(
                    "Deja de salir y no se puede deshacer. Si solo quieres quitarlo " +
                        "por un tiempo, mejor apaga \"Visible para los clientes\"."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarBorrado = false
                        onBorrar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sí, borrar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarBorrado = false }) { Text("No") }
            }
        )
    }
}

private val SIMBOLOS = listOf(
    "📸", "📔", "📖", "🖼️", "🖨️", "💄", "👗", "👔", "🎬", "🎁",
    "📰", "👶", "💍", "🎂", "🤰", "⭐", "🚗", "📦", "📁"
)

@Composable
internal fun CarpetaDialog(
    carpeta: Carpeta,
    esNueva: Boolean,
    onGuardar: (nombre: String, simbolo: String) -> Unit,
    onBorrar: () -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf(carpeta.nombre) }
    var simbolo by remember { mutableStateOf(carpeta.simbolo) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (esNueva) "Nueva carpeta" else "Cambiar carpeta") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la carpeta *") },
                    placeholder = { Text("Ej: Trajes de hombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("carpeta_nombre")
                )
                Text(
                    text = "Símbolo",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                // Los emojis en filas de cinco, para que quepan en el teléfono.
                SIMBOLOS.chunked(5).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        fila.forEach { e ->
                            Text(
                                text = e,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (e == simbolo) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { simbolo = e }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                if (!esNueva) {
                    TextButton(
                        onClick = { confirmarBorrado = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Borrar la carpeta entera")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre.trim(), simbolo) },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )

    if (confirmarBorrado) {
        val n = carpeta.servicios.size
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar \"${carpeta.nombre}\"?") },
            text = {
                Text(
                    "Se borra la carpeta con todo lo que tiene dentro " +
                        "(${if (n == 1) "1 servicio" else "$n servicios"}). No se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarBorrado = false
                        onBorrar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sí, borrar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarBorrado = false }) { Text("No") }
            }
        )
    }
}

@Composable
internal fun SubcarpetaDialog(
    sub: Subcarpeta,
    esNueva: Boolean,
    onGuardar: (nombre: String) -> Unit,
    onBorrar: () -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf(sub.nombre) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (esNueva) "Nueva subcarpeta" else "Cambiar subcarpeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la subcarpeta *") },
                    placeholder = { Text("Ej: Formato 8x12") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Sirve para agrupar dentro de una carpeta, como los temas de " +
                        "\"Fotos al momento\" o los formatos de los FotoBooks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!esNueva) {
                    TextButton(
                        onClick = { confirmarBorrado = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Borrar la subcarpeta")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre.trim()) },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )

    if (confirmarBorrado) {
        val n = sub.servicios.size
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar \"${sub.nombre}\"?") },
            text = {
                Text(
                    "Se borra con lo que tiene dentro " +
                        "(${if (n == 1) "1 servicio" else "$n servicios"}). No se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarBorrado = false
                        onBorrar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sí, borrar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarBorrado = false }) { Text("No") }
            }
        )
    }
}
