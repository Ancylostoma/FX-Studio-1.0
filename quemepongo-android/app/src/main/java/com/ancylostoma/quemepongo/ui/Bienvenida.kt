package com.ancylostoma.quemepongo.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.data.AvisoDiario
import com.ancylostoma.quemepongo.data.Cuerpo
import com.ancylostoma.quemepongo.data.Estilo
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.ServicioClima
import com.ancylostoma.quemepongo.data.Subtipo
import com.ancylostoma.quemepongo.motor.Catalogo
import kotlin.math.roundToInt

private const val PASOS = 6

/**
 * Bienvenida en pasos cortos, uno por pantalla:
 * 0 qué hace · 1 maniquí · 2 estilos · 3 ciudad · 4 aviso diario · 5 primera prenda.
 * [alTerminar] recibe true si el usuario quiere agregar su primera prenda ya.
 */
@Composable
fun PantallaBienvenida(e: Estado, vm: AppViewModel, alTerminar: (agregarPrenda: Boolean) -> Unit) {
  var paso by rememberSaveable { mutableIntStateOf(0) }
  BackHandler(enabled = paso > 0) { paso-- }

  Column(Modifier.fillMaxSize().systemBarsPadding()) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      if (paso > 0) {
        IconButton(onClick = { paso-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
      } else {
        Spacer(Modifier.width(48.dp))
      }
      Spacer(Modifier.weight(1f))
      PuntosProgreso(paso)
      Spacer(Modifier.weight(1f))
      Spacer(Modifier.width(48.dp))
    }
    AnimatedContent(
      targetState = paso,
      transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() },
      modifier = Modifier.weight(1f),
      label = "bienvenida",
    ) { p ->
      when (p) {
        0 -> PasoInicio(e, vm) { paso = 1 }
        1 -> PasoCuerpo(e, vm) { paso = 2 }
        2 -> PasoEstilos(e, vm) { paso = 3 }
        3 -> PasoCiudad(e, vm) { paso = 4 }
        4 -> PasoAviso(vm) { paso = 5 }
        else -> PasoPrimeraPrenda(alTerminar)
      }
    }
  }
}

@Composable
private fun PuntosProgreso(paso: Int) {
  Row {
    repeat(PASOS) { i ->
      val ancho by animateDpAsState(if (i == paso) 20.dp else 7.dp, label = "progreso")
      Box(
        Modifier
          .padding(horizontal = 3.dp)
          .size(width = ancho, height = 7.dp)
          .clip(CircleShape)
          .background(if (i <= paso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
      )
    }
  }
}

/** Estructura común: contenido desplazable y los botones siempre abajo. */
@Composable
private fun Paso(
  titulo: String,
  texto: String,
  principal: String,
  alPrincipal: () -> Unit,
  secundario: String? = null,
  alSecundario: () -> Unit = {},
  principalActivo: Boolean = true,
  contenido: @Composable ColumnScope.() -> Unit,
) {
  Column(Modifier.fillMaxSize()) {
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(titulo, style = MaterialTheme.typography.headlineMedium)
      Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      contenido()
      Spacer(Modifier.height(8.dp))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Button(onClick = alPrincipal, enabled = principalActivo, modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Text(principal, style = MaterialTheme.typography.titleMedium)
      }
      if (secundario != null) TextButton(onClick = alSecundario) { Text(secundario) }
    }
  }
}

@Composable
private fun PasoInicio(e: Estado, vm: AppViewModel, siguiente: () -> Unit) {
  Paso(
    titulo = "Qué Me Pongo",
    texto = "Cada mañana te armo el look según el tiempo que hará, la ocasión, lo que está de moda y tu estilo. Y aprendo de lo que te gusta.",
    principal = "Empezar",
    alPrincipal = siguiente,
  ) {
    Maniqui(
      prendas = e.sugerencias.firstOrNull()?.prendas.orEmpty(),
      cuerpo = e.datos.perfil.cuerpo,
      foto = vm::archivoFoto,
      claveAnimacion = "inicio",
      marcarIdeas = false,
      modifier = Modifier.fillMaxWidth().height(320.dp),
    )
  }
}

@Composable
private fun PasoCuerpo(e: Estado, vm: AppViewModel, siguiente: () -> Unit) {
  val actual = e.datos.perfil.cuerpo
  Paso(
    titulo = "Tu maniquí",
    texto = "Elige la silueta en la que quieres ver tus looks.",
    principal = "Siguiente",
    alPrincipal = siguiente,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Cuerpo.entries.forEach { c ->
        Tarjeta(elegida = c == actual, modifier = Modifier.weight(1f), alTocar = { vm.cambiarPerfil { it.copy(cuerpo = c) } }) {
          Maniqui(lookDeEstilo(Estilo.CASUAL), c, vm::archivoFoto, Modifier.fillMaxWidth().height(200.dp), claveAnimacion = c, marcarIdeas = false)
          Text(c.etiqueta, style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

@Composable
private fun PasoEstilos(e: Estado, vm: AppViewModel, siguiente: () -> Unit) {
  val elegidos = e.datos.perfil.estilos
  Paso(
    titulo = "¿Qué estilo va contigo?",
    texto = "Elige todos los que quieras. Los looks se parecerán más a estos.",
    principal = if (elegidos.isEmpty()) "Saltar" else "Siguiente",
    alPrincipal = siguiente,
  ) {
    Estilo.entries.chunked(2).forEach { fila ->
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        fila.forEach { s ->
          Tarjeta(
            elegida = s in elegidos,
            modifier = Modifier.weight(1f),
            alTocar = { vm.cambiarPerfil { it.copy(estilos = if (s in it.estilos) it.estilos - s else it.estilos + s) } },
          ) {
            Maniqui(lookDeEstilo(s), e.datos.perfil.cuerpo, vm::archivoFoto, Modifier.fillMaxWidth().height(170.dp), claveAnimacion = s, marcarIdeas = false)
            Text(s.etiqueta, style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }
}

@Composable
private fun PasoCiudad(e: Estado, vm: AppViewModel, siguiente: () -> Unit) {
  val contexto = LocalContext.current
  var ciudad by remember { mutableStateOf("") }
  val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.usarUbicacion() }
  val c = e.clima?.para(e.momento)
  Paso(
    titulo = "¿Dónde estás?",
    texto = "Para vestirte según el tiempo que hará. Solo se usa para pedir el pronóstico.",
    principal = if (c != null) "Siguiente" else "Ahora no",
    alPrincipal = siguiente,
  ) {
    Dibujo(Icons.Filled.LocationOn)
    when {
      e.climaCargando -> Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text("Consultando el tiempo…")
      }
      c != null -> Text(
        "✓ ${e.clima?.lugar.orEmpty()}: ${c.temperatura.roundToInt()}°, ${c.descripcion.lowercase()}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      else -> {
        e.climaError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FilledTonalButton(
          onClick = { if (ServicioClima.tienePermiso(contexto)) vm.usarUbicacion() else permiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
          modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Usar mi ubicación") }
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(value = ciudad, onValueChange = { ciudad = it }, label = { Text("O escribe tu ciudad") }, singleLine = true, modifier = Modifier.weight(1f))
          TextButton(onClick = { vm.buscarCiudad(ciudad) }, enabled = ciudad.isNotBlank()) { Text("Buscar") }
        }
      }
    }
  }
}

@Composable
private fun PasoAviso(vm: AppViewModel, siguiente: () -> Unit) {
  val contexto = LocalContext.current
  val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
    vm.cambiarAvisoDiario(ok)
    siguiente()
  }
  Paso(
    titulo = "¿Te aviso cada mañana?",
    texto = "A las 7:30 te llega tu look del día con el tiempo que hará. Sin más avisos que ese.",
    principal = "Sí, avísame",
    alPrincipal = {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !AvisoDiario.puedeAvisar(contexto)) {
        permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        vm.cambiarAvisoDiario(true)
        siguiente()
      }
    },
    secundario = "No, gracias",
    alSecundario = siguiente,
  ) {
    Dibujo(Icons.Filled.NotificationsActive)
  }
}

@Composable
private fun PasoPrimeraPrenda(alTerminar: (Boolean) -> Unit) {
  Paso(
    titulo = "Lo último: tu ropa",
    texto = "Fotografía 3 prendas que uses mucho. Les quito el fondo y los looks se arman con tu ropa de verdad.",
    principal = "Agregar mi primera prenda",
    alPrincipal = { alTerminar(true) },
    secundario = "Lo haré luego",
    alSecundario = { alTerminar(false) },
  ) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      IconoForma(Forma.CHAQUETA, MaterialTheme.colorScheme.primaryContainer, Modifier.size(170.dp), borde = MaterialTheme.colorScheme.primary)
    }
  }
}

@Composable
private fun Tarjeta(elegida: Boolean, modifier: Modifier, alTocar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
  val forma = RoundedCornerShape(22.dp)
  Column(
    modifier
      .clip(forma)
      .background(if (elegida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
      .border(2.dp, if (elegida) MaterialTheme.colorScheme.primary else Color.Transparent, forma)
      .clickable(onClick = alTocar)
      .padding(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    content = contenido,
  )
}

@Composable
private fun Dibujo(icono: ImageVector) {
  Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
    Box(Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
      Icon(icono, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
    }
  }
}

/** Un look de ejemplo para mostrar cada estilo en el maniquí. */
private fun lookDeEstilo(s: Estilo): List<Prenda> = when (s) {
  Estilo.CASUAL -> listOf(IdeaPrenda(Subtipo.CAMISETA, "blanco", "Camiseta"), IdeaPrenda(Subtipo.JEANS, "denim", "Jeans"), IdeaPrenda(Subtipo.TENIS, "blanco", "Tenis"))
  Estilo.ELEGANTE -> listOf(IdeaPrenda(Subtipo.CAMISA, "blanco", "Camisa"), IdeaPrenda(Subtipo.PANTALON_VESTIR, "negro", "Pantalón"), IdeaPrenda(Subtipo.BLAZER, "negro", "Blazer"), IdeaPrenda(Subtipo.ZAPATOS, "negro", "Zapatos"))
  Estilo.DEPORTIVO -> listOf(IdeaPrenda(Subtipo.SUDADERA, "gris", "Sudadera"), IdeaPrenda(Subtipo.JOGGER, "negro", "Jogger"), IdeaPrenda(Subtipo.TENIS, "blanco", "Tenis"))
  Estilo.URBANO -> listOf(IdeaPrenda(Subtipo.CAMISETA, "negro", "Camiseta"), IdeaPrenda(Subtipo.JEANS, "negro", "Jeans"), IdeaPrenda(Subtipo.CAZADORA, "negro", "Cazadora"), IdeaPrenda(Subtipo.BOTAS, "negro", "Botas"), IdeaPrenda(Subtipo.GORRA, "negro", "Gorra"))
  Estilo.CLASICO -> listOf(IdeaPrenda(Subtipo.CAMISA, "celeste", "Camisa"), IdeaPrenda(Subtipo.PANTALON_VESTIR, "beige", "Chino"), IdeaPrenda(Subtipo.CARDIGAN, "marino", "Cárdigan"), IdeaPrenda(Subtipo.ZAPATOS, "marron", "Mocasines"))
  Estilo.MINIMALISTA -> listOf(IdeaPrenda(Subtipo.SUETER, "gris", "Suéter"), IdeaPrenda(Subtipo.PANTALON_VESTIR, "negro", "Pantalón"), IdeaPrenda(Subtipo.ZAPATOS, "blanco", "Zapatos"))
  Estilo.BOHEMIO -> listOf(IdeaPrenda(Subtipo.BLUSA, "estampado", "Blusa"), IdeaPrenda(Subtipo.FALDA, "beige", "Falda"), IdeaPrenda(Subtipo.SANDALIAS, "marron", "Sandalias"))
  Estilo.ROMANTICO -> listOf(IdeaPrenda(Subtipo.VESTIDO, "rosa", "Vestido"), IdeaPrenda(Subtipo.CARDIGAN, "blanco", "Cárdigan"), IdeaPrenda(Subtipo.TACONES, "beige", "Tacones"))
}.map { Catalogo.desdeIdea(it, "estilo-${s.name}") }
