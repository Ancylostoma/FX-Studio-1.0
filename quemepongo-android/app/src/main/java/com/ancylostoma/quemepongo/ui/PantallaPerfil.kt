package com.ancylostoma.quemepongo.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.BuildConfig
import java.text.DateFormat
import java.util.Date

private const val URL_PRIVACIDAD = "https://github.com/Ancylostoma/FX-Studio-1.0/blob/main/quemepongo-android/PRIVACIDAD.md"

@Composable
fun PantallaPerfil(e: Estado, vm: AppViewModel, irAProbador: () -> Unit) {
  val perfil = e.datos.perfil
  val enlaces = LocalUriHandler.current
  val actividad = LocalContext.current as? Activity
  var pidiendoCiudad by remember { mutableStateOf(false) }
  var confirmarReinicio by remember { mutableStateOf(false) }

  Column(
    Modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text("Mi estilo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

    Seccion("Looks guardados (${e.datos.looks.size})") {
      if (e.datos.looks.isEmpty()) {
        Text("Guarda los looks que te gusten desde \"Hoy\" o el probador.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(e.datos.looks, key = { it.id }) { l ->
            Card(Modifier.width(150.dp)) {
              Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Si la prenda se editó después, se muestra como está ahora.
                val actuales = l.prendas.map { p -> e.datos.prendas.firstOrNull { it.id == p.id } ?: p }
                Maniqui(actuales, perfil.cuerpo, vm::archivoFoto, Modifier.fillMaxWidth().height(200.dp), claveAnimacion = l.id)
                Text(
                  "${l.ocasion.etiqueta} · ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(l.fecha))}",
                  style = MaterialTheme.typography.labelSmall,
                )
                Row {
                  TextButton(onClick = {
                    vm.probarLook(actuales)
                    irAProbador()
                  }) { Text("Probar") }
                  TextButton(onClick = { vm.borrarLook(l.id) }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
                }
              }
            }
          }
        }
      }
    }

    Seccion("Estilos que me gustan") {
      SelectorEstilos(perfil.estilos) { s ->
        vm.cambiarPerfil { it.copy(estilos = if (s in it.estilos) it.estilos - s else it.estilos + s) }
      }
    }
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
    Seccion("Maniquí") { SelectorCuerpo(perfil.cuerpo) { c -> vm.cambiarPerfil { it.copy(cuerpo = c) } } }
    Seccion("¿Cuánto te importa ir a la moda?") {
      var valor by remember(perfil.importanciaModa) { mutableStateOf(perfil.importanciaModa.toFloat()) }
      Slider(
        value = valor,
        onValueChange = { valor = it },
        onValueChangeFinished = { vm.cambiarPerfil { it.copy(importanciaModa = valor.toDouble()) } },
      )
      Text(
        when {
          valor < 0.3f -> "Prefiero mi estilo de siempre"
          valor < 0.7f -> "Un poco de tendencia está bien"
          else -> "Quiero ir a la última"
        },
        style = MaterialTheme.typography.bodySmall,
      )
    }
    Seccion("Ubicación para el tiempo") {
      Text(perfil.ciudad ?: "Sin ubicación")
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { pidiendoCiudad = true }) { Text("Cambiar ciudad") }
      }
    }

    Seccion("Aprendizaje") {
      Text(
        "La app aprende de tus \"me gusta\", \"no me gusta\", looks guardados y lo que te pones.",
        style = MaterialTheme.typography.bodySmall,
      )
      OutlinedButton(onClick = { confirmarReinicio = true }) { Text("Empezar de cero") }
    }

    Seccion("Privacidad") {
      Text(
        "Tus fotos y tu armario se quedan en este teléfono. La ubicación solo se usa para pedir el pronóstico.",
        style = MaterialTheme.typography.bodySmall,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Spacer(Modifier.height(16.dp))
  }

  if (pidiendoCiudad) DialogoCiudad(alCerrar = { pidiendoCiudad = false }) { vm.buscarCiudad(it) }
  if (confirmarReinicio) {
    AlertDialog(
      onDismissRequest = { confirmarReinicio = false },
      title = { Text("¿Borrar lo aprendido?") },
      text = { Text("Se olvidan tus \"me gusta\". Tu armario y tus looks guardados no se tocan.") },
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

private fun alternar(s: Set<String>, c: String) = if (c in s) s - c else s + c

@Composable
fun PantallaBienvenida(e: Estado, vm: AppViewModel) {
  val perfil = e.datos.perfil
  Column(
    Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text("Qué Me Pongo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text(
      "Cada día te armo looks según el tiempo, la ocasión, lo que está de moda y tu estilo. Cuéntame un poco de ti:",
      style = MaterialTheme.typography.bodyLarge,
    )
    Maniqui(
      prendas = e.sugerencias.firstOrNull()?.prendas.orEmpty(),
      cuerpo = perfil.cuerpo,
      foto = vm::archivoFoto,
      claveAnimacion = perfil.cuerpo,
      modifier = Modifier.fillMaxWidth().height(260.dp),
    )
    Seccion("Tu maniquí") { SelectorCuerpo(perfil.cuerpo) { c -> vm.cambiarPerfil { it.copy(cuerpo = c) } } }
    Seccion("Estilos que te gustan") {
      SelectorEstilos(perfil.estilos) { s -> vm.cambiarPerfil { it.copy(estilos = if (s in it.estilos) it.estilos - s else it.estilos + s) } }
    }
    Seccion("Colores favoritos") {
      SelectorColores(perfil.coloresFavoritos, { c -> vm.cambiarPerfil { it.copy(coloresFavoritos = alternar(it.coloresFavoritos, c)) } })
    }
    Button(
      onClick = { vm.cambiarPerfil { it.copy(bienvenidaHecha = true) } },
      modifier = Modifier.fillMaxWidth(),
    ) { Text("Empezar") }
    Text("Puedes cambiar todo esto luego en Perfil.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
