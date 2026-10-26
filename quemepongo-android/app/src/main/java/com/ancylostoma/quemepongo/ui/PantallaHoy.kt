@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.ancylostoma.quemepongo.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.CondicionClima
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.Look
import com.ancylostoma.quemepongo.data.Momento
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.ServicioClima
import com.ancylostoma.quemepongo.data.Vista
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Una sola cosa importante: el look. Arriba, el contexto en una línea; en el
 * centro, los looks para deslizar; abajo, "Me lo pongo".
 */
@Composable
fun PantallaHoy(e: Estado, vm: AppViewModel, abrirEditor: (List<Prenda>) -> Unit, alAgregar: () -> Unit) {
  var verContexto by remember { mutableStateOf(false) }
  var detalles by remember { mutableStateOf<Look?>(null) }
  val looks = e.sugerencias
  val paginas = rememberPagerState { looks.size + 1 }
  // Tanda nueva de looks: volver al primero.
  LaunchedEffect(e.tanda) { paginas.scrollToPage(0) }
  LaunchedEffect(paginas.currentPage) { if (paginas.currentPage < looks.size) vm.verLook(paginas.currentPage) }

  // En pantallas bajas el look conserva un mínimo de alto y la pantalla se desplaza.
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val altoLook = (maxHeight - 290.dp).coerceAtLeast(340.dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp)) {
      val hoy = LocalDate.now()
      // "domingo, 25 de octubre", "Sunday, October 25", "Sonntag, 25. Oktober"...
      val patron = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")
      Text(
        hoy.format(DateTimeFormatter.ofPattern(patron, Locale.getDefault())).replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(t(R.string.hoy_titulo), style = MaterialTheme.typography.headlineMedium)
      Spacer(Modifier.height(10.dp))
      BarraContexto(e) { verContexto = true }
      Spacer(Modifier.height(10.dp))

      if (looks.isEmpty()) {
        Box(Modifier.height(altoLook).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      } else {
        HorizontalPager(
          state = paginas,
          modifier = Modifier.height(altoLook).fillMaxWidth(),
          pageSpacing = 12.dp,
          key = { i -> looks.getOrNull(i)?.id ?: "mas" },
        ) { i ->
          val look = looks.getOrNull(i)
          if (look == null) {
            PaginaMasLooks { vm.masLooks() }
          } else {
            PaginaLook(e, vm, look)
          }
        }
        Puntos(total = looks.size + 1, actual = paginas.currentPage)
      }

      val look = e.sugerencias.getOrNull(paginas.currentPage)
      if (look != null) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
          Text(
            look.razones.firstOrNull()?.let { texto(it) }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = { detalles = look }) { Text(t(R.string.hoy_por_que)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
          FilledTonalButton(onClick = { abrirEditor(look.prendas) }, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Filled.Tune, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(t(R.string.hoy_editar))
          }
          BotonPrincipal(onClick = { vm.meLoPongo(look) }, modifier = Modifier.weight(1f).height(52.dp)) {
            Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(t(R.string.hoy_me_lo_pongo), style = MaterialTheme.typography.titleMedium)
          }
        }
      } else {
        Spacer(Modifier.height(64.dp))
      }
      if (e.datos.prendas.count { !it.virtual } < 4) {
        TextButton(onClick = alAgregar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
          Text(t(R.string.hoy_agrega_ropa), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }

  if (verContexto) HojaContexto(e, vm) { verContexto = false }
  detalles?.let { l -> HojaDetalles(l, vm, alEditar = { detalles = null; abrirEditor(l.prendas) }) { detalles = null } }
}

/** El contexto en una línea: tiempo, lugar, ocasión y momento. Al tocarla se cambia. */
@Composable
private fun BarraContexto(e: Estado, alTocar: () -> Unit) {
  val c = e.clima?.para(e.momento)
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(50))
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
      .clickable(onClick = alTocar)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    when {
      e.climaCargando -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
      c != null -> {
        Icon(iconoClima(c), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(temperatura(c.temperatura), style = MaterialTheme.typography.titleMedium)
      }
      else -> Icon(Icons.Filled.LocationOn, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.width(10.dp))
    Text(
      listOfNotNull(
        if (c == null && !e.climaCargando) t(R.string.hoy_elige_ciudad) else e.clima?.lugar?.takeIf { it.isNotBlank() },
        t(e.ocasion.res()),
        t(e.momento.res()),
      ).joinToString(" · "),
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Icon(Icons.Filled.ExpandMore, t(R.string.hoy_cambiar), tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

fun iconoClima(c: CondicionClima): ImageVector = when {
  c.nieve -> Icons.Filled.AcUnit
  c.lluvia -> Icons.Filled.Umbrella
  c.soleado -> Icons.Filled.WbSunny
  else -> Icons.Filled.Cloud
}

@Composable
private fun PaginaLook(e: Estado, vm: AppViewModel, look: Look) {
  val guardado = e.estaGuardado(look)
  val contexto = LocalContext.current
  val alcance = rememberCoroutineScope()
  val collage = usarCollage(look.prendas, e.datos.perfil.vista)
  Box(Modifier.fillMaxSize().escenario()) {
    VistaLook(
      prendas = look.prendas,
      cuerpo = e.datos.perfil.cuerpo,
      vista = e.datos.perfil.vista,
      foto = vm::archivoFoto,
      claveAnimacion = look.id,
      modifier = Modifier.fillMaxSize().padding(vertical = 16.dp, horizontal = if (collage) 12.dp else 24.dp),
    )
    // Cambiar entre maniquí y collage de fotos.
    BotonFlotante(
      onClick = { vm.cambiarVista(if (collage) Vista.MANIQUI else Vista.COLLAGE) },
      modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
    ) { Icon(if (collage) Icons.Filled.Accessibility else Icons.Filled.Dashboard, t(if (collage) R.string.hoy_ver_maniqui else R.string.hoy_ver_collage)) }
    look.tendencia?.let { t ->
      Row(
        Modifier
          .align(Alignment.TopStart)
          .padding(14.dp)
          .clip(RoundedCornerShape(50))
          .background(MaterialTheme.colorScheme.secondaryContainer)
          .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Whatshot, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(4.dp))
        Text(t, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 170.dp))
      }
    }
    Column(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val rojo by animateColorAsState(if (guardado) Color(0xFFE53958) else MaterialTheme.colorScheme.onSurfaceVariant, label = "corazon")
      val tamano by animateDpAsState(if (guardado) 26.dp else 22.dp, label = "latido")
      BotonFlotante(onClick = { vm.alternarGuardado(look) }) {
        Icon(if (guardado) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, t(if (guardado) R.string.hoy_quitar_guardado else R.string.hoy_guardar), tint = rojo, modifier = Modifier.size(tamano))
      }
      BotonFlotante(onClick = { vm.descartar(look) }) { Icon(Icons.Filled.Close, t(R.string.hoy_no_me_gusta)) }
      BotonFlotante(onClick = {
        alcance.launch { runCatching { Compartir.look(contexto, look.prendas, e.datos.perfil.cuerpo, vm::archivoFoto, contexto.getString(R.string.compartir_titulo_hoy)) } }
      }) { Icon(Icons.Filled.Share, t(R.string.hoy_compartir)) }
    }
    val faltan = look.prendas.count { it.virtual }
    if (faltan > 0) {
      Text(
        pluralStringResource(R.plurals.hoy_ideas_compra, faltan, faltan),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
      )
    }
  }
}

@Composable
private fun PaginaMasLooks(alPedir: () -> Unit) {
  Box(
    Modifier.fillMaxSize().escenario(),
    contentAlignment = Alignment.Center,
  ) {
    EstadoVacio(Forma.TOP_MANGA_LARGA, t(R.string.hoy_mas_titulo), t(R.string.hoy_mas_texto), t(R.string.hoy_mas_boton), alPedir)
  }
}

@Composable
private fun Puntos(total: Int, actual: Int) {
  Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
    repeat(total) { i ->
      val ancho by animateDpAsState(if (i == actual) 18.dp else 6.dp, label = "punto")
      Box(
        Modifier
          .padding(horizontal = 3.dp)
          .size(width = ancho, height = 6.dp)
          .clip(CircleShape)
          .background(if (i == actual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
      )
    }
  }
}

/** Cambiar dónde, cuándo y para qué. */
@Composable
private fun HojaContexto(e: Estado, vm: AppViewModel, alCerrar: () -> Unit) {
  val contexto = LocalContext.current
  var ciudad by remember { mutableStateOf("") }
  val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.usarUbicacion() }
  ModalBottomSheet(onDismissRequest = alCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(t(R.string.contexto_donde), style = MaterialTheme.typography.titleMedium)
      val c = e.clima?.para(e.momento)
      if (c != null) {
        Text(
          stringResource(R.string.contexto_pronostico, e.clima?.lugar.orEmpty(), t(c.cielo.res()).lowercase(Locale.getDefault()), temperatura(c.temperatura), temperatura(c.sensacion)),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      e.climaError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      FilledTonalButton(onClick = {
        if (ServicioClima.tienePermiso(contexto)) vm.usarUbicacion() else permiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
      }) {
        Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(t(R.string.contexto_ubicacion))
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = ciudad,
          onValueChange = { ciudad = it },
          label = { Text(t(R.string.contexto_ciudad)) },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { vm.buscarCiudad(ciudad); ciudad = "" }, enabled = ciudad.isNotBlank()) { Text(t(R.string.buscar)) }
      }

      Text(t(R.string.contexto_cuando), style = MaterialTheme.typography.titleMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Momento.entries.forEach { m -> FilterChip(selected = m == e.momento, onClick = { vm.elegirMomento(m) }, label = { Text(t(m.res())) }) }
      }

      Text(t(R.string.contexto_para_que), style = MaterialTheme.typography.titleMedium)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Ocasion.entries.forEach { o -> FilterChip(selected = o == e.ocasion, onClick = { vm.elegirOcasion(o) }, label = { Text(t(o.res())) }) }
      }

      BotonPrincipal(onClick = alCerrar, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(R.string.listo)) }
      if (e.clima != null) {
        Text(t(R.string.clima_atribucion), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(Modifier.height(12.dp))
    }
  }
}

/** Todas las razones del look y sus piezas, con las que faltan para comprar. */
@Composable
private fun HojaDetalles(look: Look, vm: AppViewModel, alEditar: () -> Unit, alCerrar: () -> Unit) {
  val enlaces = LocalUriHandler.current
  ModalBottomSheet(onDismissRequest = alCerrar) {
    Column(
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(t(R.string.detalles_titulo), style = MaterialTheme.typography.titleLarge)
      ListaRazones(look.razones.map { texto(it) })
      Text(t(R.string.piezas), style = MaterialTheme.typography.titleMedium)
      look.prendas.forEach { p ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          MiniaturaPrenda(p, vm.archivoFoto(p.foto), 52.dp)
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) {
            Text(nombre(p), style = MaterialTheme.typography.bodyLarge)
            Text(
              if (p.virtual) t(R.string.detalles_no_la_tienes) else t(p.subtipo.res()),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (p.virtual) {
            TextButton(onClick = { enlaces.openUri(vm.enlaceTienda(IdeaPrenda(p.subtipo, p.color, p.nombre))) }) {
              Icon(Icons.Filled.ShoppingCart, null, Modifier.size(16.dp))
              Text(" " + t(R.string.buscar))
            }
          }
        }
      }
      OutlinedButton(onClick = alEditar, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.detalles_editar)) }
      Spacer(Modifier.height(12.dp))
    }
  }
}

/** Botón redondo sobre el escenario del look: claro con sombra, para que se vea encima de cualquier prenda. */
@Composable
private fun BotonFlotante(onClick: () -> Unit, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
  val esquema = MaterialTheme.colorScheme
  val fondo = if (LocalOscuro.current) esquema.surfaceContainerHighest else esquema.surfaceContainerLowest
  FilledIconButton(
    onClick = onClick,
    modifier = modifier.shadow(4.dp, CircleShape),
    colors = IconButtonDefaults.filledIconButtonColors(containerColor = fondo, contentColor = esquema.onSurface),
    content = contenido,
  )
}
