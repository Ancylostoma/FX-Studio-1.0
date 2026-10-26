package com.ancylostoma.quemepongo.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Subtipo

enum class Pestana(@StringRes val etiqueta: Int, val icono: ImageVector, val iconoInactivo: ImageVector) {
  HOY(R.string.tab_hoy, Icons.Filled.Today, Icons.Outlined.Today),
  ARMARIO(R.string.tab_armario, Icons.Filled.Checkroom, Icons.Outlined.Checkroom),
  DESCUBRIR(R.string.tab_descubrir, Icons.Filled.Whatshot, Icons.Outlined.Whatshot),
  YO(R.string.tab_yo, Icons.Filled.Person, Icons.Outlined.Person),
}

/** Lo que se abre encima de las pestañas, a pantalla completa. */
private sealed interface Encima {
  data class EditarPrenda(val prenda: Prenda, val nueva: Boolean) : Encima
  data object Editor : Encima
  data object Ajustes : Encima
  data class Premium(val motivo: MotivoPremium) : Encima
  data object Semana : Encima
  data object Maleta : Encima
  data object Estadisticas : Encima
}

@Composable
fun App(vm: AppViewModel = viewModel()) {
  val e by vm.estado.collectAsStateWithLifecycle()
  val perfil = e.datos.perfil
  TemaQueMePongo(perfil.modo, perfil.paleta) {
    // La Surface da el fondo y el color del texto a toda la app, también a la bienvenida.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Pantallas(e, vm) }
  }
}

@Composable
private fun Pantallas(e: Estado, vm: AppViewModel) {
  var pestana by rememberSaveable { mutableStateOf(Pestana.HOY) }
  var encima by remember { mutableStateOf<Encima?>(null) }
  if (!e.cargado) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    return
  }
  val avisos = remember { SnackbarHostState() }
  LaunchedEffect(e.mensaje) {
    val m = e.mensaje ?: return@LaunchedEffect
    avisos.showSnackbar(m)
    vm.mensajeVisto()
  }
  // Atrás vuelve a "Hoy"; desde "Hoy" sale de la app. Lo que está encima tiene su propio Atrás.
  BackHandler(enabled = pestana != Pestana.HOY && encima == null) { pestana = Pestana.HOY }

  // Con el plan gratis, el armario tiene un límite: al pasarlo se ofrece Premium.
  val agregar = {
    if (vm.puedeAgregarPrenda()) {
      encima = Encima.EditarPrenda(Prenda(id = "nueva-" + vm.nuevoId(), nombre = "", subtipo = Subtipo.CAMISETA, color = "negro"), nueva = true)
    }
  }
  // La pantalla de Premium se abre cuando el ViewModel la pide (límite de looks, etc.).
  LaunchedEffect(e.pedirPremium) {
    val motivo = e.pedirPremium ?: return@LaunchedEffect
    encima = Encima.Premium(motivo)
    vm.premiumMostrado()
  }
  val abrirHerramienta = { m: MotivoPremium ->
    encima = if (!e.premium) {
      Encima.Premium(m)
    } else {
      when (m) {
        MotivoPremium.SEMANA -> Encima.Semana
        MotivoPremium.MALETA -> Encima.Maleta
        MotivoPremium.ESTADISTICAS -> Encima.Estadisticas
        else -> Encima.Premium(m)
      }
    }
  }
  val editarPrenda = { p: Prenda -> encima = Encima.EditarPrenda(p, nueva = false) }
  val abrirEditor = { prendas: List<Prenda> ->
    vm.probarLook(prendas)
    encima = Encima.Editor
  }

  if (!e.datos.perfil.bienvenidaHecha) {
    PantallaBienvenida(e, vm) { agregarPrenda ->
      vm.cambiarPerfil { it.copy(bienvenidaHecha = true) }
      if (agregarPrenda) agregar()
    }
    return
  }

  Box(Modifier.fillMaxSize()) {
    Scaffold(
      bottomBar = { BarraNavegacion(pestana, alElegir = { pestana = it }, alAgregar = agregar) },
    ) { relleno ->
      // Al cambiar de pestaña, la nueva entra deslizándose desde el lado en que está.
      AnimatedContent(
        targetState = pestana,
        transitionSpec = {
          val haciaDerecha = targetState.ordinal > initialState.ordinal
          (slideInHorizontally(tween(260)) { if (haciaDerecha) it / 6 else -it / 6 } + fadeIn(tween(260))) togetherWith fadeOut(tween(140))
        },
        modifier = Modifier.fillMaxSize().padding(relleno),
        label = "pestanas",
      ) { actual ->
        when (actual) {
          Pestana.HOY -> PantallaHoy(e, vm, abrirEditor = abrirEditor, alAgregar = agregar)
          Pestana.ARMARIO -> PantallaArmario(e, vm, alAgregar = agregar, alEditar = editarPrenda)
          Pestana.DESCUBRIR -> PantallaDescubrir(e, vm, abrirEditor)
          Pestana.YO -> PantallaYo(e, vm, abrirEditor, abrirAjustes = { encima = Encima.Ajustes }, irAHoy = { pestana = Pestana.HOY }, abrirHerramienta = abrirHerramienta)
        }
      }
    }

    // Lo que se abre encima sube desde abajo y al cerrarse baja.
    AnimatedContent(
      targetState = encima,
      transitionSpec = {
        if (targetState != null) {
          (slideInVertically(tween(300)) { it / 8 } + fadeIn(tween(220))) togetherWith fadeOut(tween(160))
        } else {
          fadeIn(tween(1)) togetherWith (slideOutVertically(tween(260)) { it / 8 } + fadeOut(tween(220)))
        }
      },
      contentKey = { it?.javaClass },
      label = "encima",
    ) { x ->
      if (x == null) return@AnimatedContent
      Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
          val cerrar = { encima = null }
          when (x) {
            is Encima.EditarPrenda -> key(x.prenda.id) { AgregarPrenda(x.prenda, x.nueva, vm, alCerrar = cerrar, alOtra = agregar) }
            Encima.Editor -> EditorLook(e, vm, alCerrar = cerrar)
            Encima.Ajustes -> PantallaAjustes(e, vm, alCerrar = cerrar, abrirPremium = { encima = Encima.Premium(MotivoPremium.GENERAL) })
            is Encima.Premium -> PantallaPremium(e, vm, x.motivo, alCerrar = cerrar)
            Encima.Semana -> PantallaSemana(e, vm, alCerrar = cerrar, abrirEditor = abrirEditor)
            Encima.Maleta -> PantallaMaleta(e, vm, alCerrar = cerrar)
            Encima.Estadisticas -> PantallaEstadisticas(e, vm, alCerrar = cerrar)
          }
        }
      }
    }

    SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp))
  }
}

/**
 * Barra de abajo: la pestaña elegida se marca con una píldora del color de la
 * app que crece con un pequeño rebote; en el centro, el botón de agregar
 * sobresale para que se encuentre sin buscarlo.
 */
@Composable
private fun BarraNavegacion(actual: Pestana, alElegir: (Pestana) -> Unit, alAgregar: () -> Unit) {
  val esquema = MaterialTheme.colorScheme
  Surface(
    color = if (LocalOscuro.current) esquema.surfaceContainer else esquema.surfaceContainerLowest,
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    shadowElevation = 18.dp,
  ) {
    Row(
      Modifier.fillMaxWidth().navigationBarsPadding().height(74.dp).padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ItemBarra(Pestana.HOY, actual, alElegir)
      ItemBarra(Pestana.ARMARIO, actual, alElegir)
      BotonAgregar(alAgregar)
      ItemBarra(Pestana.DESCUBRIR, actual, alElegir)
      ItemBarra(Pestana.YO, actual, alElegir)
    }
  }
}

@Composable
private fun RowScope.ItemBarra(p: Pestana, actual: Pestana, alElegir: (Pestana) -> Unit) {
  val esquema = MaterialTheme.colorScheme
  val elegida = p == actual
  val ancho by animateDpAsState(if (elegida) 58.dp else 34.dp, spring(dampingRatio = 0.55f, stiffness = 500f), label = "pildora")
  Column(
    Modifier
      .weight(1f)
      .fillMaxHeight()
      .selectable(selected = elegida, onClick = { alElegir(p) }, role = Role.Tab, interactionSource = remember { MutableInteractionSource() }, indication = null),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      Modifier
        .size(width = ancho, height = 32.dp)
        .clip(CircleShape)
        .background(if (elegida) Brush.horizontalGradient(listOf(esquema.primary, esquema.secondary)) else SolidColor(Color.Transparent)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(if (elegida) p.icono else p.iconoInactivo, null, tint = if (elegida) esquema.onPrimary else esquema.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.height(4.dp))
    Text(
      t(p.etiqueta),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (elegida) FontWeight.Bold else FontWeight.Medium,
      color = if (elegida) esquema.primary else esquema.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

@Composable
private fun RowScope.BotonAgregar(alAgregar: () -> Unit) {
  val esquema = MaterialTheme.colorScheme
  val interaccion = remember { MutableInteractionSource() }
  val presionado by interaccion.collectIsPressedAsState()
  val escala by animateFloatAsState(if (presionado) 0.9f else 1f, label = "agregar")
  Column(
    Modifier
      .weight(1f)
      .fillMaxHeight()
      .clickable(interactionSource = interaccion, indication = null, role = Role.Button, onClick = alAgregar),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      Modifier
        .offset(y = (-8).dp)
        .graphicsLayer {
          scaleX = escala
          scaleY = escala
        }
        .size(54.dp)
        .shadow(12.dp, CircleShape, ambientColor = esquema.primary, spotColor = esquema.primary)
        .clip(CircleShape)
        .background(Brush.linearGradient(listOf(esquema.primary, esquema.secondary))),
      contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Add, null, tint = esquema.onPrimary, modifier = Modifier.size(30.dp)) }
    Text(
      t(R.string.tab_agregar),
      style = MaterialTheme.typography.labelMedium,
      color = esquema.onSurfaceVariant,
      maxLines = 1,
      modifier = Modifier.offset(y = (-4).dp),
    )
  }
}
