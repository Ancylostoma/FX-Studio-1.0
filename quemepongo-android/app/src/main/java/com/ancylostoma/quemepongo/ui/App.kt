package com.ancylostoma.quemepongo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Subtipo

enum class Pestana(val etiqueta: String, val icono: ImageVector) {
  HOY("Hoy", Icons.Filled.Today),
  ARMARIO("Armario", Icons.Filled.Checkroom),
  DESCUBRIR("Descubrir", Icons.Filled.Whatshot),
  YO("Yo", Icons.Filled.Person),
}

/** Lo que se abre encima de las pestañas, a pantalla completa. */
private sealed interface Encima {
  data class EditarPrenda(val prenda: Prenda, val nueva: Boolean) : Encima
  data object Editor : Encima
  data object Ajustes : Encima
}

@Composable
fun App(vm: AppViewModel = viewModel()) {
  val e by vm.estado.collectAsStateWithLifecycle()
  if (!e.cargado) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    return
  }
  if (!e.datos.perfil.bienvenidaHecha) {
    PantallaBienvenida(e, vm)
    return
  }

  var pestana by rememberSaveable { mutableStateOf(Pestana.HOY) }
  var encima by remember { mutableStateOf<Encima?>(null) }
  val avisos = remember { SnackbarHostState() }
  LaunchedEffect(e.mensaje) {
    val m = e.mensaje ?: return@LaunchedEffect
    avisos.showSnackbar(m)
    vm.mensajeVisto()
  }
  // Atrás vuelve a "Hoy"; desde "Hoy" sale de la app. Lo que está encima tiene su propio Atrás.
  BackHandler(enabled = pestana != Pestana.HOY && encima == null) { pestana = Pestana.HOY }

  val agregar = { encima = Encima.EditarPrenda(Prenda(id = "nueva-" + vm.nuevoId(), nombre = "", subtipo = Subtipo.CAMISETA, color = "negro"), nueva = true) }
  val editarPrenda = { p: Prenda -> encima = Encima.EditarPrenda(p, nueva = false) }
  val abrirEditor = { prendas: List<Prenda> ->
    vm.probarLook(prendas)
    encima = Encima.Editor
  }

  Box(Modifier.fillMaxSize()) {
    Scaffold(
      bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
          ItemPestana(Pestana.HOY, pestana) { pestana = it }
          ItemPestana(Pestana.ARMARIO, pestana) { pestana = it }
          NavigationBarItem(
            selected = false,
            onClick = agregar,
            icon = {
              Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
              ) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary) }
            },
            label = { Text("Agregar") },
          )
          ItemPestana(Pestana.DESCUBRIR, pestana) { pestana = it }
          ItemPestana(Pestana.YO, pestana) { pestana = it }
        }
      },
    ) { relleno ->
      Box(Modifier.fillMaxSize().padding(relleno)) {
        when (pestana) {
          Pestana.HOY -> PantallaHoy(e, vm, abrirEditor = abrirEditor, alAgregar = agregar)
          Pestana.ARMARIO -> PantallaArmario(e, vm, alAgregar = agregar, alEditar = editarPrenda)
          Pestana.DESCUBRIR -> PantallaDescubrir(e, vm, abrirEditor)
          Pestana.YO -> PantallaYo(e, vm, abrirEditor, abrirAjustes = { encima = Encima.Ajustes }, irAHoy = { pestana = Pestana.HOY })
        }
      }
    }

    encima?.let { x ->
      Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
          val cerrar = { encima = null }
          when (x) {
            is Encima.EditarPrenda -> key(x.prenda.id) { AgregarPrenda(x.prenda, x.nueva, vm, alCerrar = cerrar, alOtra = agregar) }
            Encima.Editor -> EditorLook(e, vm, alCerrar = cerrar)
            Encima.Ajustes -> PantallaAjustes(e, vm, alCerrar = cerrar)
          }
        }
      }
    }

    SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp))
  }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ItemPestana(p: Pestana, actual: Pestana, alElegir: (Pestana) -> Unit) {
  NavigationBarItem(
    selected = p == actual,
    onClick = { alElegir(p) },
    icon = { Icon(p.icono, null) },
    label = { Text(p.etiqueta) },
  )
}
