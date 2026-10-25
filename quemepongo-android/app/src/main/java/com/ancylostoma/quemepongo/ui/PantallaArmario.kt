@file:OptIn(ExperimentalLayoutApi::class)

package com.ancylostoma.quemepongo.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Subtipo
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PantallaArmario(e: Estado, vm: AppViewModel) {
  var filtro by remember { mutableStateOf<Categoria?>(null) }
  var editando by remember { mutableStateOf<Prenda?>(null) }
  val reales = e.datos.prendas.filter { !it.virtual }
  val visibles = reales.filter { filtro == null || it.categoria == filtro }.sortedByDescending { it.creada }

  Box(Modifier.fillMaxSize()) {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(108.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Mi armario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
          Text("${reales.size} prendas", color = MaterialTheme.colorScheme.onSurfaceVariant)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = filtro == null, onClick = { filtro = null }, label = { Text("Todo") }) }
            items(Categoria.entries) { c ->
              val n = reales.count { it.categoria == c }
              FilterChip(selected = filtro == c, onClick = { filtro = c }, label = { Text("${c.etiqueta} ($n)") })
            }
          }
        }
      }
      if (visibles.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Text(
            "Aún no hay nada aquí. Toca \"Agregar prenda\" y hazle una foto a tu ropa: sobre la cama o colgada, con buena luz y fondo liso.",
            modifier = Modifier.padding(vertical = 24.dp),
          )
        }
      }
      items(visibles, key = { it.id }) { p ->
        Column(Modifier.clickable { editando = p }, horizontalAlignment = Alignment.CenterHorizontally) {
          MiniaturaPrenda(p, vm.archivoFoto(p.foto), 108.dp)
          Text(p.nombre, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
            if (p.usos == 0) "Sin usar" else "Usada ${p.usos} ${if (p.usos == 1) "vez" else "veces"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    ExtendedFloatingActionButton(
      onClick = {
        val sub = filtro?.let { Subtipo.deCategoria(it).first() } ?: Subtipo.CAMISETA
        editando = Prenda(id = "nueva-" + vm.nuevoId(), nombre = "", subtipo = sub, color = "negro")
      },
      icon = { Icon(Icons.Filled.Add, null) },
      text = { Text("Agregar prenda") },
      modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
    )
  }

  editando?.let { p ->
    FormularioPrenda(
      inicial = p,
      esNueva = p.id.startsWith("nueva-"),
      vm = vm,
      alCerrar = { editando = null },
    )
  }
}

@Composable
private fun FormularioPrenda(inicial: Prenda, esNueva: Boolean, vm: AppViewModel, alCerrar: () -> Unit) {
  val contexto = LocalContext.current
  var p by remember(inicial.id) { mutableStateOf(inicial) }
  var importando by remember { mutableStateOf(false) }
  var confirmarBorrado by remember { mutableStateOf(false) }
  // Fotos importadas en esta edición; las que no se guarden se borran al salir.
  val importadas = remember { mutableListOf<String>() }

  val cerrarSinGuardar = {
    importadas.forEach { vm.descartarFoto(it) }
    alCerrar()
  }
  BackHandler(onBack = cerrarSinGuardar)

  fun alImportar(uri: Uri?) {
    if (uri == null) return
    importando = true
    vm.importarFoto(uri, p.id.removePrefix("nueva-")) { nombre, color ->
      importando = false
      if (nombre != null) {
        importadas.add(nombre)
        p = p.copy(foto = nombre, color = color ?: p.color)
      }
    }
  }

  val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { alImportar(it) }
  val destinoCamara = remember {
    val carpeta = File(contexto.cacheDir, "camara").apply { mkdirs() }
    FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", File(carpeta, "foto.jpg"))
  }
  val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) alImportar(destinoCamara) }

  Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(if (esNueva) "Nueva prenda" else "Editar prenda", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
          MiniaturaPrenda(p, vm.archivoFoto(p.foto), 140.dp)
          if (importando) CircularProgressIndicator()
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          FilledTonalButton(onClick = { camara.launch(destinoCamara) }) {
            Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tomar foto")
          }
          OutlinedButton(onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Galería")
          }
          if (p.foto != null) {
            Text("Color detectado: ${Colores.de(p.color).etiqueta}", style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      OutlinedTextField(
        value = p.nombre,
        onValueChange = { p = p.copy(nombre = it) },
        label = { Text("Nombre (opcional)") },
        placeholder = { Text(p.subtipo.etiqueta) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Seccion("¿Qué es?") {
        Categoria.entries.forEach { c ->
          Text(c.etiqueta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Subtipo.deCategoria(c).forEach { s ->
              FilterChip(
                selected = p.subtipo == s,
                onClick = {
                  // Al cambiar el tipo se ponen sus valores típicos; se pueden ajustar abajo.
                  p = p.copy(subtipo = s, abrigo = s.abrigo, formalidad = s.formalidad, estilos = s.estilos, impermeable = s.impermeable)
                },
                label = { Text(s.etiqueta) },
              )
            }
          }
        }
      }

      Seccion("Color") { SelectorColores(setOf(p.color), { p = p.copy(color = it) }) }

      Seccion("Estilo") {
        SelectorEstilos(p.estilos) { s ->
          val nuevos = if (s in p.estilos) p.estilos - s else p.estilos + s
          if (nuevos.isNotEmpty()) p = p.copy(estilos = nuevos)
        }
      }

      Seccion("¿Cuánto abriga? ${etiquetaAbrigo(p.abrigo)}") {
        Slider(value = p.abrigo.toFloat(), onValueChange = { p = p.copy(abrigo = it.roundToInt()) }, valueRange = 0f..5f, steps = 4)
      }
      Seccion("¿Qué tan formal es? ${etiquetaFormalidad(p.formalidad)}") {
        Slider(value = p.formalidad.toFloat(), onValueChange = { p = p.copy(formalidad = it.roundToInt()) }, valueRange = 1f..5f, steps = 3)
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Sirve para la lluvia", modifier = Modifier.weight(1f))
        Switch(checked = p.impermeable, onCheckedChange = { p = p.copy(impermeable = it) })
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (!esNueva) {
          TextButton(onClick = { confirmarBorrado = true }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = cerrarSinGuardar) { Text("Cancelar") }
        Button(
          enabled = !importando,
          onClick = {
            val final = p.copy(
              id = p.id.removePrefix("nueva-"),
              nombre = p.nombre.trim().ifBlank { "${p.subtipo.etiqueta} ${Colores.de(p.color).etiqueta.lowercase()}" },
            )
            importadas.filter { it != final.foto }.forEach { vm.descartarFoto(it) }
            vm.guardarPrenda(final)
            alCerrar()
          },
        ) { Text("Guardar") }
      }
      Spacer(Modifier.size(24.dp))
    }
  }

  if (confirmarBorrado) {
    AlertDialog(
      onDismissRequest = { confirmarBorrado = false },
      title = { Text("¿Borrar esta prenda?") },
      text = { Text("Se borra del armario junto con su foto.") },
      confirmButton = {
        TextButton(onClick = {
          confirmarBorrado = false
          vm.borrarPrenda(inicial)
          importadas.forEach { vm.descartarFoto(it) }
          alCerrar()
        }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
      },
      dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
    )
  }
}

private fun etiquetaAbrigo(n: Int) = when (n) {
  0 -> "(muy fresca)"
  1 -> "(fresca)"
  2 -> "(media)"
  3 -> "(abriga)"
  4 -> "(abriga mucho)"
  else -> "(para mucho frío)"
}

private fun etiquetaFormalidad(n: Int) = when (n) {
  1 -> "(deporte)"
  2 -> "(informal)"
  3 -> "(arreglada)"
  4 -> "(formal)"
  else -> "(de gala)"
}
