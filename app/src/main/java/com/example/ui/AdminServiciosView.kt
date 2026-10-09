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


/**
 * Pestaña "Servicios" del panel: las carpetas de "Diseña tu propia oferta" y
 * de "Agregar algo más", con la misma forma que las ve el cliente, pero
 * editables. Desde aquí se cambia un precio, se añade un traje nuevo, se
 * crea una carpeta o se oculta algo que ya no se ofrece.
 *
 * Cada cambio se guarda en el momento y el cliente lo ve enseguida.
 */
@Composable
fun AdminServiciosView(viewModel: StudioViewModel) {
    val context = LocalContext.current
    var menu by rememberSaveable { mutableStateOf(MenuServicios.PROPIA) }
    val carpetas by viewModel.servicios(menu).collectAsState()
    val config by viewModel.studioConfig.collectAsState()

    var abierta by rememberSaveable { mutableStateOf<String?>(null) }

    // Qué se está editando. Solo uno de los tres diálogos a la vez.
    var editandoServicio by remember { mutableStateOf<ServicioEnEdicion?>(null) }
    var editandoCarpeta by remember { mutableStateOf<Carpeta?>(null) }
    var editandoSub by remember { mutableStateOf<Pair<String, Subcarpeta>?>(null) }
    var confirmarRestaurar by remember { mutableStateOf(false) }

    fun editar(cambio: (List<Carpeta>) -> List<Carpeta>) = viewModel.editarServicios(menu, cambio)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ---- Qué lista se edita ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuServicios.values().forEach { m ->
                        FilterChip(
                            selected = menu == m,
                            onClick = {
                                menu = m
                                abierta = null
                            },
                            label = {
                                Text(
                                    text = m.titulo,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_menu_${m.name}")
                        )
                    }
                }
                Text(
                    text = if (menu == MenuServicios.PROPIA)
                        "Lo que el cliente elige suelto en \"Diseña tu propia oferta\"."
                    else
                        "Lo que se puede sumar a un paquete. Cada servicio dice en qué " +
                            "paquetes aparece (Bodas, 15 años, Primer Año).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Toca una carpeta para abrirla y un servicio para cambiarlo. " +
                        "Las flechas cambian el orden. Todo se guarda al momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Las carpetas ----
        items(carpetas, key = { it.id }) { carpeta ->
            CarpetaAdmin(
                carpeta = carpeta,
                abierta = abierta == carpeta.id,
                esPrimera = carpetas.firstOrNull()?.id == carpeta.id,
                esUltima = carpetas.lastOrNull()?.id == carpeta.id,
                mostrarPaquetes = menu == MenuServicios.EXTRAS,
                enPulgadas = config.medidasEnPulgadas,
                onAbrir = { abierta = if (abierta == carpeta.id) null else carpeta.id },
                onEditar = { editandoCarpeta = carpeta },
                onMover = { delta -> editar { it.moverCarpeta(carpeta.id, delta) } },
                onEditarSub = { sub -> editandoSub = carpeta.id to sub },
                onMoverSub = { sub, delta -> editar { it.moverSubcarpeta(sub.id, delta) } },
                onNuevaSub = {
                    editandoSub = carpeta.id to Subcarpeta(nombre = "")
                },
                onEditarServicio = { sub, servicio ->
                    editandoServicio = ServicioEnEdicion(sub.id, servicio, esNuevo = false)
                },
                onNuevoServicio = { sub ->
                    editandoServicio = ServicioEnEdicion(
                        sub.id,
                        Servicio(titulo = "", precio = 0.0, grupoPedido = carpeta.nombre),
                        esNuevo = true
                    )
                },
                onMoverServicio = { servicio, delta ->
                    editar { it.moverServicio(servicio.id, delta) }
                }
            )
        }

        // ---- Carpeta nueva y restaurar ----
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { editandoCarpeta = Carpeta(nombre = "", simbolo = "📁") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("admin_nueva_carpeta"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva carpeta", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { confirmarRestaurar = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Volver a la lista de fábrica")
                }
            }
        }
    }

    // ---- Diálogos ----

    editandoServicio?.let { edicion ->
        ServicioDialog(
            edicion = edicion,
            carpetas = carpetas,
            mostrarPaquetes = menu == MenuServicios.EXTRAS,
            enPulgadas = config.medidasEnPulgadas,
            onGuardar = { subDestino, servicio ->
                editar { it.guardarServicio(subDestino, servicio) }
                editandoServicio = null
                Toast.makeText(context, "Servicio guardado", Toast.LENGTH_SHORT).show()
            },
            onBorrar = {
                editar { it.quitarServicio(edicion.servicio.id) }
                editandoServicio = null
                Toast.makeText(context, "Servicio borrado", Toast.LENGTH_SHORT).show()
            },
            onCerrar = { editandoServicio = null }
        )
    }

    editandoCarpeta?.let { carpeta ->
        val esNueva = carpetas.none { it.id == carpeta.id }
        CarpetaDialog(
            carpeta = carpeta,
            esNueva = esNueva,
            onGuardar = { nombre, simbolo ->
                if (esNueva) {
                    // Una carpeta nueva ya trae su grupo sin nombre, para
                    // poder meterle servicios sin crear subcarpetas.
                    val nueva = carpeta.copy(
                        nombre = nombre,
                        simbolo = simbolo,
                        subcarpetas = listOf(Subcarpeta(nombre = ""))
                    )
                    editar { it.guardarCarpeta(nueva) }
                    abierta = nueva.id
                } else {
                    editar { it.guardarCarpeta(carpeta.copy(nombre = nombre, simbolo = simbolo)) }
                }
                editandoCarpeta = null
            },
            onBorrar = {
                editar { it.quitarCarpeta(carpeta.id) }
                editandoCarpeta = null
                Toast.makeText(context, "Carpeta borrada", Toast.LENGTH_SHORT).show()
            },
            onCerrar = { editandoCarpeta = null }
        )
    }

    editandoSub?.let { (carpetaId, sub) ->
        val esNueva = carpetas.none { c -> c.subcarpetas.any { it.id == sub.id } }
        SubcarpetaDialog(
            sub = sub,
            esNueva = esNueva,
            onGuardar = { nombre ->
                editar { it.guardarSubcarpeta(carpetaId, sub.copy(nombre = nombre)) }
                editandoSub = null
            },
            onBorrar = {
                editar { it.quitarSubcarpeta(sub.id) }
                editandoSub = null
                Toast.makeText(context, "Subcarpeta borrada", Toast.LENGTH_SHORT).show()
            },
            onCerrar = { editandoSub = null }
        )
    }

    if (confirmarRestaurar) {
        AlertDialog(
            onDismissRequest = { confirmarRestaurar = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("¿Volver a la lista de fábrica?") },
            text = {
                Text(
                    "La lista de \"${menu.titulo}\" vuelve a la que trae la app. " +
                        "Se pierden los precios cambiados y los servicios que hayas " +
                        "añadido en esta lista. La otra lista no se toca."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restaurarServicios(menu)
                        abierta = null
                        confirmarRestaurar = false
                        Toast.makeText(context, "Lista restaurada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Sí, restaurar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarRestaurar = false }) { Text("Cancelar") }
            }
        )
    }
}

/** Qué servicio se edita y en qué subcarpeta está. */
internal data class ServicioEnEdicion(
    val subId: String,
    val servicio: Servicio,
    val esNuevo: Boolean
)

// ------------------------------------------------------------------
// La carpeta, como la ve el cliente pero con lápices y flechas
// ------------------------------------------------------------------

@Composable
private fun CarpetaAdmin(
    carpeta: Carpeta,
    abierta: Boolean,
    esPrimera: Boolean,
    esUltima: Boolean,
    mostrarPaquetes: Boolean,
    enPulgadas: Boolean,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onMover: (Int) -> Unit,
    onEditarSub: (Subcarpeta) -> Unit,
    onMoverSub: (Subcarpeta, Int) -> Unit,
    onNuevaSub: () -> Unit,
    onEditarServicio: (Subcarpeta, Servicio) -> Unit,
    onNuevoServicio: (Subcarpeta) -> Unit,
    onMoverServicio: (Servicio, Int) -> Unit
) {
    val total = carpeta.servicios.size
    val ocultos = carpeta.servicios.count { !it.visible }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (abierta) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAbrir() }
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                .testTag("admin_carpeta_${carpeta.nombre}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = carpeta.simbolo, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = carpeta.nombre.ifBlank { "(sin nombre)" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (total == 1) "1 servicio" else "$total servicios")
                        if (ocultos > 0) append("  •  $ocultos oculto" + if (ocultos == 1) "" else "s")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BotonIcono(Icons.Default.ArrowUpward, "Subir carpeta", enabled = !esPrimera) { onMover(-1) }
            BotonIcono(Icons.Default.ArrowDownward, "Bajar carpeta", enabled = !esUltima) { onMover(1) }
            BotonIcono(Icons.Default.Edit, "Cambiar nombre o borrar carpeta") { onEditar() }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedVisibility(visible = abierta) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                carpeta.subcarpetas.forEachIndexed { i, sub ->
                    if (sub.nombre.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "└  ${sub.nombre}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            BotonIcono(Icons.Default.ArrowUpward, "Subir subcarpeta", enabled = i > 0) {
                                onMoverSub(sub, -1)
                            }
                            BotonIcono(
                                Icons.Default.ArrowDownward, "Bajar subcarpeta",
                                enabled = i < carpeta.subcarpetas.lastIndex
                            ) { onMoverSub(sub, 1) }
                            BotonIcono(Icons.Default.Edit, "Cambiar nombre o borrar subcarpeta") {
                                onEditarSub(sub)
                            }
                        }
                    }

                    sub.servicios.forEachIndexed { j, servicio ->
                        FilaServicioAdmin(
                            servicio = servicio,
                            sangrado = sub.nombre.isNotBlank(),
                            mostrarPaquetes = mostrarPaquetes,
                            enPulgadas = enPulgadas,
                            esPrimero = j == 0,
                            esUltimo = j == sub.servicios.lastIndex,
                            onEditar = { onEditarServicio(sub, servicio) },
                            onMover = { delta -> onMoverServicio(servicio, delta) }
                        )
                    }

                    TextButton(
                        onClick = { onNuevoServicio(sub) },
                        modifier = Modifier
                            .padding(start = if (sub.nombre.isNotBlank()) 14.dp else 0.dp)
                            .testTag("admin_nuevo_servicio")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (sub.nombre.isNotBlank()) "Añadir servicio en \"${sub.nombre}\""
                            else "Añadir servicio"
                        )
                    }
                }

                OutlinedButton(
                    onClick = onNuevaSub,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nueva subcarpeta")
                }
            }
        }
    }
}

@Composable
private fun FilaServicioAdmin(
    servicio: Servicio,
    sangrado: Boolean,
    mostrarPaquetes: Boolean,
    enPulgadas: Boolean,
    esPrimero: Boolean,
    esUltimo: Boolean,
    onEditar: () -> Unit,
    onMover: (Int) -> Unit
) {
    val sinPrecio = servicio.precio <= 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (sangrado) 14.dp else 0.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onEditar() }
            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = servicio.titulo.ifBlank { "(sin nombre)" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (servicio.visible) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (servicio.formato.isNotBlank() &&
                !servicio.titulo.contains(servicio.formato, ignoreCase = true)
            ) {
                Text(
                    text = servicio.formato,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Medidas.resumen(servicio.titulo + " " + servicio.formato, enPulgadas)?.let {
                Text(
                    text = "📏 $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$${String.format("%.2f", servicio.precio)} USD",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (sinPrecio) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                if (!servicio.visible) {
                    Etiqueta(
                        texto = if (sinPrecio) "Falta el precio · oculto" else "Oculto",
                        error = sinPrecio
                    )
                }
            }
            if (mostrarPaquetes) {
                Text(
                    text = "En: " + if (servicio.paquetes.isEmpty()) "todos los paquetes"
                    else servicio.paquetes.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BotonIcono(Icons.Default.ArrowUpward, "Subir", enabled = !esPrimero) { onMover(-1) }
        BotonIcono(Icons.Default.ArrowDownward, "Bajar", enabled = !esUltimo) { onMover(1) }
    }
}

@Composable
private fun Etiqueta(texto: String, error: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (error) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = if (error) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = if (error) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BotonIcono(
    icono: ImageVector,
    descripcion: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
    }
}
