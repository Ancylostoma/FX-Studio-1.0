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
private data class ServicioEnEdicion(
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
private fun ServicioDialog(
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
private fun CarpetaDialog(
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
private fun SubcarpetaDialog(
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

// ------------------------------------------------------------------
// Ajustes que valen para todas las ofertas (pestaña Ofertas)
// ------------------------------------------------------------------

/**
 * Lo que llevan todas las ofertas (el transporte) y en qué unidad están
 * escritas las medidas del catálogo.
 */
@Composable
fun AdminAjustesOfertasCard(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val config by viewModel.studioConfig.collectAsState()
    var incluye by remember(config.incluyeEnOfertas) { mutableStateOf(config.incluyeEnOfertas) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "En todas las ofertas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = incluye,
                onValueChange = { incluye = it },
                label = { Text("Incluido en todas las ofertas") },
                placeholder = { Text("Ej: Transporte") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_incluye_ofertas")
            )
            Text(
                text = "Sale en el \"Incluye\" de cada paquete y en el pedido de WhatsApp. " +
                    "Separa con comas si son varias cosas; en blanco no sale nada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    viewModel.updateStudioConfig(config.copy(incluyeEnOfertas = incluye.trim()))
                    Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show()
                },
                enabled = incluye.trim() != config.incluyeEnOfertas,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar")
            }

            HorizontalDivider()

            Text(
                text = "Las medidas del catálogo están en",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = config.medidasEnPulgadas,
                    onClick = {
                        if (!config.medidasEnPulgadas) {
                            viewModel.updateStudioConfig(config.copy(medidasEnPulgadas = true))
                        }
                    },
                    label = { Text("Pulgadas") }
                )
                FilterChip(
                    selected = !config.medidasEnPulgadas,
                    onClick = {
                        if (config.medidasEnPulgadas) {
                            viewModel.updateStudioConfig(config.copy(medidasEnPulgadas = false))
                        }
                    },
                    label = { Text("Centímetros") }
                )
            }
            Text(
                text = "La app pone al lado la equivalencia en la otra unidad. Ejemplo: " +
                    (Medidas.equivalencias("8x12", config.medidasEnPulgadas).firstOrNull() ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
