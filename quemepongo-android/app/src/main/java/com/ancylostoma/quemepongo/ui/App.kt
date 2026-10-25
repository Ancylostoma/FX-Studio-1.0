package com.ancylostoma.quemepongo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

enum class Pestana(val etiqueta: String, val icono: ImageVector) {
  HOY("Hoy", Icons.Filled.Today),
  ARMARIO("Armario", Icons.Filled.Checkroom),
  PROBADOR("Probador", Icons.Filled.Accessibility),
  TENDENCIAS("Tendencias", Icons.Filled.Whatshot),
  PERFIL("Perfil", Icons.Filled.Person),
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
  val avisos = remember { SnackbarHostState() }
  LaunchedEffect(e.mensaje) {
    val m = e.mensaje ?: return@LaunchedEffect
    avisos.showSnackbar(m)
    vm.mensajeVisto()
  }
  // Atrás vuelve a "Hoy"; desde "Hoy" sale de la app.
  BackHandler(enabled = pestana != Pestana.HOY) { pestana = Pestana.HOY }

  val irAProbador = { pestana = Pestana.PROBADOR }
  Scaffold(
    snackbarHost = { SnackbarHost(avisos) },
    bottomBar = {
      NavigationBar {
        Pestana.entries.forEach { p ->
          NavigationBarItem(
            selected = p == pestana,
            onClick = { pestana = p },
            icon = { Icon(p.icono, null) },
            label = { Text(p.etiqueta) },
          )
        }
      }
    },
  ) { relleno ->
    Box(Modifier.fillMaxSize().padding(relleno)) {
      when (pestana) {
        Pestana.HOY -> PantallaHoy(e, vm, irAProbador, irAArmario = { pestana = Pestana.ARMARIO })
        Pestana.ARMARIO -> PantallaArmario(e, vm)
        Pestana.PROBADOR -> PantallaProbador(e, vm)
        Pestana.TENDENCIAS -> PantallaTendencias(e, vm, irAProbador)
        Pestana.PERFIL -> PantallaPerfil(e, vm, irAProbador)
      }
    }
  }
}
