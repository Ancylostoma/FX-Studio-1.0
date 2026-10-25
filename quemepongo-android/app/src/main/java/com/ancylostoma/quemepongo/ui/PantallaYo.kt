@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ancylostoma.quemepongo.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.BuildConfig
import com.ancylostoma.quemepongo.data.AvisoDiario
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.Vista
import com.ancylostoma.quemepongo.data.LookGuardado
import com.ancylostoma.quemepongo.data.Prenda
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private const val URL_PRIVACIDAD = "https://github.com/Ancylostoma/FX-Studio-1.0/blob/main/quemepongo-android/PRIVACIDAD.md"

@Composable
fun PantallaYo(e: Estado, vm: AppViewModel, abrirEditor: (List<Prenda>) -> Unit, abrirAjustes: () -> Unit, irAHoy: () -> Unit) {
  var vista by remember { mutableIntStateOf(0) }
  var elegido by remember { mutableStateOf<LookGuardado?>(null) }
  val contexto = LocalContext.current
  val alcance = rememberCoroutineScope()
  val lista = if (vista == 0) e.datos.looks else e.datos.historial
  // Si la prenda se editó después, se muestra como está ahora.
  fun actuales(l: LookGuardado) = l.prendas.map { p -> e.datos.prendas.firstOrNull { it.id == p.id } ?: p }

  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloPantalla("Yo", accion = {
          IconButton(onClick = abrirAjustes) { Icon(Icons.Filled.Settings, "Ajustes") }
        })
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
          listOf("Guardados (${e.datos.looks.size})", "Me los puse (${e.datos.historial.size})").forEachIndexed { i, texto ->
            SegmentedButton(
              selected = vista == i,
              onClick = { vista = i },
              shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
            ) { Text(texto) }
          }
        }
      }
    }
    if (lista.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        if (vista == 0) {
          EstadoVacio(Forma.VESTIDO_CORTO, "Aún no guardaste looks", "Toca el corazón de un look en Hoy para guardarlo aquí.", "Ver looks de hoy", irAHoy)
        } else {
          EstadoVacio(Forma.TOP_MANGA_CORTA, "Tu diario de looks", "Cada vez que toques \"Me lo pongo\" queda anotado aquí, y no te repito la ropa.", "Elegir el de hoy", irAHoy)
        }
      }
    }
    items(lista, key = { it.id }) { l ->
      Column(
        Modifier
          .clip(RoundedCornerShape(22.dp))
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .clickable { elegido = l }
          .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        VistaLook(actuales(l), e.datos.perfil.cuerpo, e.datos.perfil.vista, vm::archivoFoto, Modifier.fillMaxWidth().height(190.dp), claveAnimacion = l.id)
        Text(l.ocasion.etiqueta, style = MaterialTheme.typography.labelLarge)
        Text(
          DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(l.fecha)),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }

  elegido?.let { l ->
    ModalBottomSheet(onDismissRequest = { elegido = null }) {
      Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(l.ocasion.etiqueta, style = MaterialTheme.typography.titleLarge)
        Text(l.prendas.joinToString(" · ") { it.nombre }, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
          elegido = null
          abrirEditor(actuales(l))
        }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Ponérmelo / editar") }
        OutlinedButton(onClick = {
          val prendas = actuales(l)
          elegido = null
          alcance.launch { runCatching { Compartir.look(contexto, prendas, e.datos.perfil.cuerpo, vm::archivoFoto, "Mi look") } }
        }, modifier = Modifier.fillMaxWidth()) { Text("Compartir") }
        OutlinedButton(onClick = {
          elegido = null
          if (vista == 0) vm.borrarLook(l.id) else vm.borrarDelHistorial(l.id)
        }, modifier = Modifier.fillMaxWidth()) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
      }
    }
  }
}

/** Todo lo que se configura una vez: estilo, colores, maniquí, ciudad y privacidad. */
@Composable
fun PantallaAjustes(e: Estado, vm: AppViewModel, alCerrar: () -> Unit) {
  BackHandler(onBack = alCerrar)
  val perfil = e.datos.perfil
  val enlaces = LocalUriHandler.current
  val contexto = LocalContext.current
  val actividad = contexto as? Activity
  var pidiendoCiudad by remember { mutableStateOf(false) }
  var confirmarReinicio by remember { mutableStateOf(false) }
  val permisoAviso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> vm.cambiarAvisoDiario(ok) }

  Column(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = alCerrar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
      Text("Ajustes", style = MaterialTheme.typography.titleLarge)
    }
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Seccion("Estilos que me gustan") {
        SelectorEstilos(perfil.estilos) { s -> vm.cambiarPerfil { it.copy(estilos = if (s in it.estilos) it.estilos - s else it.estilos + s) } }
      }
      HorizontalDivider()
      Seccion("Colores favoritos") {
        SelectorColores(perfil.coloresFavoritos, { c ->
          vm.cambiarPerfil { it.copy(coloresFavoritos = alternar(it.coloresFavoritos, c), coloresEvitar = it.coloresEvitar - c) }
        })
      }
      Seccion("Colores que no me pongo") {
        SelectorColores(perfil.coloresEvitar, { c ->
          vm.cambiarPerfil { it.copy(coloresEvitar = alternar(it.coloresEvitar, c), coloresFavoritos = it.coloresFavoritos - c) }
        })
      }
      HorizontalDivider()
      Seccion("Maniquí") { SelectorCuerpo(perfil.cuerpo) { c -> vm.cambiarPerfil { it.copy(cuerpo = c) } } }
      Seccion("¿Cuánto te importa ir a la moda?") {
        var valor by remember(perfil.importanciaModa) { mutableStateOf(perfil.importanciaModa.toFloat()) }
        Slider(value = valor, onValueChange = { valor = it }, onValueChangeFinished = { vm.cambiarPerfil { it.copy(importanciaModa = valor.toDouble()) } })
        Text(
          when {
            valor < 0.3f -> "Prefiero mi estilo de siempre"
            valor < 0.7f -> "Un poco de tendencia está bien"
            else -> "Quiero ir a la última"
          },
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Seccion("Ver los looks como") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(Vista.AUTO to "Automático", Vista.COLLAGE to "Collage de fotos", Vista.MANIQUI to "Maniquí").forEach { (v, texto) ->
            FilterChip(selected = perfil.vista == v, onClick = { vm.cambiarVista(v) }, label = { Text(texto) })
          }
        }
        Text("Automático: collage cuando todas tus prendas del look tienen foto.", style = MaterialTheme.typography.bodySmall)
      }
      HorizontalDivider()
      Seccion("Aviso cada mañana") {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Tu look del día a las 7:30, con el tiempo que hará", modifier = Modifier.weight(1f))
          Switch(checked = perfil.avisoDiario, onCheckedChange = { activar ->
            if (activar && !AvisoDiario.puedeAvisar(contexto) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              permisoAviso.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              vm.cambiarAvisoDiario(activar)
            }
          })
        }
      }
      HorizontalDivider()
      Seccion("Ciudad para el tiempo") {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(perfil.ciudad ?: "Sin ciudad", modifier = Modifier.weight(1f))
          TextButton(onClick = { pidiendoCiudad = true }) { Text("Cambiar") }
        }
      }
      Seccion("Aprendizaje") {
        Text("Aprendo de tus corazones, de los looks que descartas y de lo que te pones.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { confirmarReinicio = true }) { Text("Empezar de cero") }
      }
      HorizontalDivider()
      Seccion("Privacidad") {
        Text(
          "Tus fotos y tu armario se quedan en este teléfono. La ubicación solo se usa para pedir el pronóstico.",
          style = MaterialTheme.typography.bodySmall,
        )
        Row {
          TextButton(onClick = { enlaces.openUri(URL_PRIVACIDAD) }) { Text("Política de privacidad") }
          if (Anuncios.opcionesPrivacidad && actividad != null) {
            TextButton(onClick = { Anuncios.mostrarOpcionesPrivacidad(actividad) }) { Text("Anuncios") }
          }
        }
      }
      Text(
        "Qué Me Pongo ${BuildConfig.VERSION_NAME} · Datos del tiempo: MET Norway (CC BY 4.0)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(24.dp))
    }
  }

  if (pidiendoCiudad) DialogoCiudad(alCerrar = { pidiendoCiudad = false }) { vm.buscarCiudad(it) }
  if (confirmarReinicio) {
    AlertDialog(
      onDismissRequest = { confirmarReinicio = false },
      title = { Text("¿Borrar lo aprendido?") },
      text = { Text("Olvido tus gustos. Tu armario y tus looks guardados no se tocan.") },
      confirmButton = {
        TextButton(onClick = {
          confirmarReinicio = false
          vm.reiniciarAprendizaje()
        }) { Text("Borrar") }
      },
      dismissButton = { TextButton(onClick = { confirmarReinicio = false }) { Text("Cancelar") } },
    )
  }
}

internal fun alternar(s: Set<String>, c: String) = if (c in s) s - c else s + c

@Composable
fun DialogoCiudad(alCerrar: () -> Unit, alBuscar: (String) -> Unit) {
  var texto by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = alCerrar,
    title = { Text("¿En qué ciudad estás?") },
    text = {
      androidx.compose.material3.OutlinedTextField(value = texto, onValueChange = { texto = it }, singleLine = true, label = { Text("Ciudad") })
    },
    confirmButton = {
      TextButton(onClick = { alBuscar(texto); alCerrar() }, enabled = texto.isNotBlank()) { Text("Buscar") }
    },
    dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
  )
}
