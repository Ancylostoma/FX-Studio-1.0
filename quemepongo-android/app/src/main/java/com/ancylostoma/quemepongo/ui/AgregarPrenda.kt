@file:OptIn(ExperimentalLayoutApi::class)

package com.ancylostoma.quemepongo.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Subtipo
import java.io.File
import kotlin.math.roundToInt

/**
 * Agregar o editar una prenda en pocos toques:
 * 1) foto (se le quita el fondo), 2) qué es, 3) guardar.
 * El color se detecta solo y el resto queda en "Más detalles".
 */
@Composable
fun AgregarPrenda(inicial: Prenda, esNueva: Boolean, vm: AppViewModel, alCerrar: () -> Unit, alOtra: () -> Unit) {
  val contexto = LocalContext.current
  var p by remember { mutableStateOf(inicial) }
  // En una prenda nueva hay que elegir la foto y el tipo; al editar ya están.
  var enFoto by remember { mutableStateOf(esNueva) }
  var eligioTipo by remember { mutableStateOf(!esNueva) }
  var categoria by remember { mutableStateOf(inicial.categoria) }
  var procesando by remember { mutableStateOf(false) }
  var cambiarColor by remember { mutableStateOf(false) }
  var masDetalles by remember { mutableStateOf(false) }
  var confirmarBorrado by remember { mutableStateOf(false) }
  // Fotos importadas aquí; las que no se guarden se borran al salir.
  val importadas = remember { mutableListOf<String>() }

  val cerrarSinGuardar = {
    importadas.forEach { vm.descartarFoto(it) }
    alCerrar()
  }
  BackHandler(onBack = cerrarSinGuardar)

  fun alImportar(uri: Uri?) {
    if (uri == null) return
    procesando = true
    vm.importarFoto(uri, p.id.removePrefix("nueva-")) { nombre, color ->
      procesando = false
      if (nombre != null) {
        importadas.add(nombre)
        p = p.copy(foto = nombre, color = color ?: p.color)
        enFoto = false
      }
    }
  }

  val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { alImportar(it) }
  val destinoCamara = remember {
    val carpeta = File(contexto.cacheDir, "camara").apply { mkdirs() }
    FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", File(carpeta, "foto.jpg"))
  }
  val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) alImportar(destinoCamara) }
  val tomarFoto = { camara.launch(destinoCamara) }
  val elegirFoto = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

  fun guardar(): Prenda {
    val final = p.copy(
      id = p.id.removePrefix("nueva-"),
      // Sin nombre: se muestra "Tipo · Color" en el idioma del teléfono.
      nombre = p.nombre.trim(),
    )
    importadas.filter { it != final.foto }.forEach { vm.descartarFoto(it) }
    vm.guardarPrenda(final)
    return final
  }

  Column(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = cerrarSinGuardar) { Icon(Icons.Filled.Close, t(R.string.cerrar)) }
      Text(t(if (esNueva) R.string.agregar_nueva else R.string.agregar_editar), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
    }

    if (enFoto) {
      PasoFoto(procesando, tomarFoto, elegirFoto, sinFoto = { enFoto = false })
      return@Column
    }

    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        MiniaturaPrenda(p, vm.archivoFoto(p.foto), 200.dp)
        if (procesando) CircularProgressIndicator()
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = tomarFoto) { Text(t(R.string.agregar_otra_foto)) }
        TextButton(onClick = elegirFoto) { Text(t(R.string.agregar_galeria)) }
      }

      Text(t(R.string.agregar_que_es), style = MaterialTheme.typography.titleMedium)
      // Seis categorías grandes; al elegir una ya queda su tipo más común.
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Categoria.entries.chunked(3).forEach { fila ->
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            fila.forEach { c ->
              BotonCategoria(c, elegida = eligioTipo && c == categoria, Modifier.weight(1f)) {
                categoria = c
                val s = Subtipo.deCategoria(c).first()
                if (p.categoria != c || !eligioTipo) {
                  p = p.copy(subtipo = s, abrigo = s.abrigo, formalidad = s.formalidad, estilos = s.estilos, impermeable = s.impermeable)
                }
                eligioTipo = true
              }
            }
          }
        }
      }
      if (eligioTipo) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Subtipo.deCategoria(categoria).forEach { s ->
            FilterChip(
              selected = p.subtipo == s,
              onClick = { p = p.copy(subtipo = s, abrigo = s.abrigo, formalidad = s.formalidad, estilos = s.estilos, impermeable = s.impermeable) },
              label = { Text(t(s.res())) },
            )
          }
        }
      }

      Row(Modifier.fillMaxWidth().clickable { cambiarColor = !cambiarColor }, verticalAlignment = Alignment.CenterVertically) {
        MuestraColor(p.color, 30.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
          Text(stringResource(R.string.agregar_color, t(colorRes(p.color))), style = MaterialTheme.typography.bodyLarge)
          if (p.foto != null) Text(t(R.string.agregar_detectado), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(t(if (cambiarColor) R.string.listo else R.string.agregar_cambiar), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
      }
      if (cambiarColor) {
        SelectorColores(setOf(p.color), {
          p = p.copy(color = it)
          cambiarColor = false
        })
      }

      Row(Modifier.fillMaxWidth().clickable { masDetalles = !masDetalles }, verticalAlignment = Alignment.CenterVertically) {
        Text(t(R.string.agregar_mas), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(if (masDetalles) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
      }
      if (masDetalles) Detalles(p) { p = it }
      Spacer(Modifier.height(8.dp))
    }

    // Abajo, siempre a mano.
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (esNueva) {
        OutlinedButton(
          onClick = { guardar(); alOtra() },
          enabled = eligioTipo && !procesando,
          modifier = Modifier.height(52.dp),
        ) { Text(t(R.string.agregar_guardar_otra)) }
      } else {
        TextButton(onClick = { confirmarBorrado = true }) { Text(t(R.string.borrar), color = MaterialTheme.colorScheme.error) }
      }
      Button(
        onClick = { guardar(); alCerrar() },
        enabled = eligioTipo && !procesando,
        modifier = Modifier.weight(1f).height(52.dp),
      ) { Text(t(if (eligioTipo) R.string.guardar else R.string.agregar_elige)) }
    }
  }

  if (confirmarBorrado) {
    AlertDialog(
      onDismissRequest = { confirmarBorrado = false },
      title = { Text(t(R.string.borrar_titulo)) },
      text = { Text(t(R.string.borrar_texto)) },
      confirmButton = {
        TextButton(onClick = {
          confirmarBorrado = false
          vm.borrarPrenda(inicial)
          importadas.forEach { vm.descartarFoto(it) }
          alCerrar()
        }) { Text(t(R.string.borrar), color = MaterialTheme.colorScheme.error) }
      },
      dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text(t(R.string.cancelar)) } },
    )
  }
}

@Composable
private fun PasoFoto(procesando: Boolean, tomarFoto: () -> Unit, elegirFoto: () -> Unit, sinFoto: () -> Unit) {
  // Desplazable: en pantallas bajas los botones no deben quedar aplastados.
  Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
  ) {
    if (procesando) {
      CircularProgressIndicator()
      Text(t(R.string.foto_quitando), style = MaterialTheme.typography.titleMedium)
      return@Column
    }
    IconoForma(Forma.TOP_MANGA_LARGA, MaterialTheme.colorScheme.primaryContainer, Modifier.size(96.dp), borde = MaterialTheme.colorScheme.primary)
    Text(t(R.string.foto_titulo), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Text(
      t(R.string.foto_texto),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    BotonGrande(Icons.Filled.CameraAlt, t(R.string.foto_tomar), principal = true, alTocar = tomarFoto)
    BotonGrande(Icons.Filled.PhotoLibrary, t(R.string.foto_galeria), principal = false, alTocar = elegirFoto)
    TextButton(onClick = sinFoto) { Text(t(R.string.foto_sin)) }
  }
}

@Composable
private fun BotonGrande(icono: ImageVector, texto: String, principal: Boolean, alTocar: () -> Unit) {
  val contenido: @Composable () -> Unit = {
    Icon(icono, null, Modifier.size(20.dp))
    Spacer(Modifier.width(10.dp))
    Text(texto, style = MaterialTheme.typography.titleMedium)
  }
  if (principal) {
    Button(onClick = alTocar, modifier = Modifier.fillMaxWidth().height(56.dp)) { contenido() }
  } else {
    OutlinedButton(onClick = alTocar, modifier = Modifier.fillMaxWidth().height(56.dp)) { contenido() }
  }
}

@Composable
private fun BotonCategoria(c: Categoria, elegida: Boolean, modifier: Modifier, alTocar: () -> Unit) {
  val forma = RoundedCornerShape(18.dp)
  Column(
    modifier
      .clip(forma)
      .background(if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
      .border(if (elegida) 2.dp else 0.dp, if (elegida) MaterialTheme.colorScheme.primary else Color.Transparent, forma)
      .clickable(onClick = alTocar)
      .padding(vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    IconoForma(
      formaDeCategoria(c),
      if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
      Modifier.size(44.dp),
      borde = Color.Transparent,
    )
    Spacer(Modifier.height(6.dp))
    Text(t(c.res()), style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
private fun Detalles(p: Prenda, alCambiar: (Prenda) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(
      value = p.nombre,
      onValueChange = { alCambiar(p.copy(nombre = it)) },
      label = { Text(t(R.string.detalle_nombre)) },
      placeholder = { Text(nombre(p.copy(nombre = ""))) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    // El precio es opcional: sirve para el "costo por uso" de las estadísticas Premium.
    var textoPrecio by remember { mutableStateOf(p.precio?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    OutlinedTextField(
      value = textoPrecio,
      onValueChange = { nuevo ->
        textoPrecio = nuevo.filter { it.isDigit() || it == '.' || it == ',' }
        alCambiar(p.copy(precio = textoPrecio.replace(',', '.').toDoubleOrNull()))
      },
      label = { Text(t(R.string.detalle_precio)) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      modifier = Modifier.fillMaxWidth(),
    )
    Text(t(R.string.detalle_estilo), style = MaterialTheme.typography.labelLarge)
    SelectorEstilos(p.estilos) { s ->
      val nuevos = if (s in p.estilos) p.estilos - s else p.estilos + s
      if (nuevos.isNotEmpty()) alCambiar(p.copy(estilos = nuevos))
    }
    Text(stringResource(R.string.detalle_abrigo, etiquetaAbrigo(p.abrigo)), style = MaterialTheme.typography.labelLarge)
    Slider(value = p.abrigo.toFloat(), onValueChange = { alCambiar(p.copy(abrigo = it.roundToInt())) }, valueRange = 0f..5f, steps = 4)
    Text(stringResource(R.string.detalle_formal, etiquetaFormalidad(p.formalidad)), style = MaterialTheme.typography.labelLarge)
    Slider(value = p.formalidad.toFloat(), onValueChange = { alCambiar(p.copy(formalidad = it.roundToInt())) }, valueRange = 1f..5f, steps = 3)
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(t(R.string.detalle_lluvia), modifier = Modifier.weight(1f))
      Switch(checked = p.impermeable, onCheckedChange = { alCambiar(p.copy(impermeable = it)) })
    }
  }
}

@Composable
private fun etiquetaAbrigo(n: Int) = t(
  when (n) {
    0 -> R.string.abrigo_0
    1 -> R.string.abrigo_1
    2 -> R.string.abrigo_2
    3 -> R.string.abrigo_3
    4 -> R.string.abrigo_4
    else -> R.string.abrigo_5
  },
)

@Composable
private fun etiquetaFormalidad(n: Int) = t(
  when (n) {
    1 -> R.string.formal_1
    2 -> R.string.formal_2
    3 -> R.string.formal_3
    4 -> R.string.formal_4
    else -> R.string.formal_5
  },
)
