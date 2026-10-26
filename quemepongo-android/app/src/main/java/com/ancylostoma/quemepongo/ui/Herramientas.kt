@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.ancylostoma.quemepongo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.LookGuardado
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Prenda
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private fun fechaCorta(f: LocalDate): String {
  val patron = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM")
  return f.format(DateTimeFormatter.ofPattern(patron, Locale.getDefault())).replaceFirstChar { it.uppercase() }
}

@Composable
private fun BarraSuperior(titulo: String, alCerrar: () -> Unit, accion: (@Composable () -> Unit)? = null) {
  Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = alCerrar) { Icon(Icons.Filled.Close, t(R.string.cerrar)) }
    Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
    accion?.invoke()
  }
}

// ---------------------------------------------------------------------------------------------
// Planear la semana
// ---------------------------------------------------------------------------------------------

@Composable
fun PantallaSemana(e: Estado, vm: AppViewModel, alCerrar: () -> Unit, abrirEditor: (List<Prenda>) -> Unit) {
  BackHandler(onBack = alCerrar)
  LaunchedEffect(Unit) { vm.revisarSemana() }
  Column(Modifier.fillMaxSize()) {
    BarraSuperior(t(R.string.semana_titulo), alCerrar) {
      TextButton(onClick = { vm.armarSemana(nuevaSemilla = true) }) { Text(t(R.string.semana_rehacer)) }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item { Text(t(R.string.semana_texto), color = MaterialTheme.colorScheme.onSurfaceVariant) }
      if (e.datos.plan.isEmpty()) {
        item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
      }
      items(e.datos.plan, key = { it.id }) { l -> DiaDeLaSemana(e, vm, l, abrirEditor) }
    }
  }
}

@Composable
private fun DiaDeLaSemana(e: Estado, vm: AppViewModel, l: LookGuardado, abrirEditor: (List<Prenda>) -> Unit) {
  val fecha = runCatching { LocalDate.parse(l.id.removePrefix("plan-")) }.getOrDefault(LocalDate.now())
  val c = e.clima?.paraDia(fecha)
  val contexto = LocalContext.current
  var menu by remember { mutableStateOf(false) }
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(MaterialTheme.colorScheme.surfaceContainer)
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    VistaLook(l.prendas, e.datos.perfil.cuerpo, e.datos.perfil.vista, vm::archivoFoto, Modifier.width(110.dp).height(170.dp), claveAnimacion = l.id + l.prendas.joinToString { it.id })
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(if (fecha == LocalDate.now()) t(R.string.semana_hoy) else fechaCorta(fecha), style = MaterialTheme.typography.titleLarge)
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (c != null) {
          Icon(iconoClima(c), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.width(6.dp))
          Text("${temperatura(c.temperatura)} · ${t(c.cielo.res())}", style = MaterialTheme.typography.bodyMedium)
        } else {
          Text(t(R.string.semana_sin_tiempo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Box {
        AssistChip(onClick = { menu = true }, label = { Text(t(l.ocasion.res())) })
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
          Ocasion.entries.forEach { o ->
            DropdownMenuItem(text = { Text(t(o.res())) }, onClick = {
              menu = false
              vm.armarSemana(cambiar = fecha to o)
            })
          }
        }
      }
      Text(
        l.prendas.joinToString(" · ") { contexto.nombre(it) },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      TextButton(onClick = { abrirEditor(l.prendas) }, contentPadding = PaddingValues(0.dp)) { Text(t(R.string.hoy_editar)) }
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Maleta de viaje
// ---------------------------------------------------------------------------------------------

@Composable
fun PantallaMaleta(e: Estado, vm: AppViewModel, alCerrar: () -> Unit) {
  BackHandler(onBack = alCerrar)
  val viaje = e.datos.viaje
  var nuevo by remember { mutableStateOf(viaje == null) }
  Column(Modifier.fillMaxSize()) {
    BarraSuperior(t(R.string.maleta_titulo), alCerrar) {
      if (!nuevo && viaje != null) TextButton(onClick = { nuevo = true }) { Text(t(R.string.maleta_nuevo)) }
    }
    if (nuevo || viaje == null) {
      FormularioViaje(e, vm) { nuevo = false }
    } else {
      ResultadoViaje(e, vm, viaje)
    }
  }
}

@Composable
private fun FormularioViaje(e: Estado, vm: AppViewModel, alArmar: () -> Unit) {
  var destino by remember { mutableStateOf("") }
  var desde by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
  var dias by remember { mutableStateOf(4f) }
  var ocasion by remember { mutableStateOf(Ocasion.VIAJE) }
  var eligiendoFecha by remember { mutableStateOf(false) }
  // Al terminar de armar (sin error), se muestra el resultado.
  var armando by remember { mutableStateOf(false) }
  LaunchedEffect(e.maletaCargando, e.datos.viaje) {
    if (armando && !e.maletaCargando && e.maletaError == null && e.datos.viaje != null) {
      armando = false
      alArmar()
    }
  }

  LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item { Text(t(R.string.maleta_texto), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    item {
      OutlinedTextField(value = destino, onValueChange = { destino = it }, label = { Text(t(R.string.maleta_destino)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.maleta_desde, fechaCorta(desde)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = { eligiendoFecha = true }) { Text(t(R.string.maleta_cambiar_fecha)) }
      }
    }
    item {
      Text(stringResource(R.string.maleta_dias, dias.roundToInt()), style = MaterialTheme.typography.titleMedium)
      Slider(value = dias, onValueChange = { dias = it }, valueRange = 1f..14f, steps = 12)
    }
    item {
      Text(t(R.string.maleta_ocasion), style = MaterialTheme.typography.titleMedium)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Ocasion.entries.forEach { o -> FilterChip(selected = o == ocasion, onClick = { ocasion = o }, label = { Text(t(o.res())) }) }
      }
    }
    item {
      e.maletaError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      if (e.maletaCargando) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(10.dp))
          Text(t(R.string.maleta_buscando))
        }
      }
      Button(
        onClick = {
          armando = true
          vm.armarMaleta(destino, desde, dias.roundToInt(), ocasion)
        },
        enabled = destino.isNotBlank() && !e.maletaCargando,
        modifier = Modifier.fillMaxWidth().height(54.dp),
      ) { Text(t(R.string.maleta_armar), style = MaterialTheme.typography.titleMedium) }
    }
  }

  if (eligiendoFecha) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = desde.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
      onDismissRequest = { eligiendoFecha = false },
      confirmButton = {
        TextButton(onClick = {
          estado.selectedDateMillis?.let { desde = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
          eligiendoFecha = false
        }) { Text(t(R.string.listo)) }
      },
      dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text(t(R.string.cancelar)) } },
    ) { DatePicker(state = estado) }
  }
}

@Composable
private fun ResultadoViaje(e: Estado, vm: AppViewModel, v: com.ancylostoma.quemepongo.data.Viaje) {
  val enlaces = LocalUriHandler.current
  val desde = runCatching { LocalDate.parse(v.desde) }.getOrDefault(LocalDate.now())
  LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    item {
      Column(Modifier.padding(horizontal = 20.dp)) {
        Text(v.destino, style = MaterialTheme.typography.headlineMedium)
        Text("${fechaCorta(desde)} – ${fechaCorta(desde.plusDays((v.dias - 1).toLong()))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Más allá de unos 8 días no hay pronóstico: se avisa que el look es para tiempo templado.
        if (desde.plusDays((v.dias - 1).toLong()).isAfter(LocalDate.now().plusDays(8))) {
          Text(t(R.string.maleta_sin_tiempo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
    item { Text(stringResource(R.string.maleta_llevar, v.llevar.size), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp)) }
    item {
      FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        v.llevar.forEach { p ->
          Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MiniaturaPrenda(p, vm.archivoFoto(p.foto), 72.dp)
            Text(nombre(p), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
    if (v.faltan.isNotEmpty()) {
      item { Text(t(R.string.maleta_faltan), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp)) }
      items(v.faltan, key = { "falta-${it.id}" }) { p ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
          IconoForma(p.subtipo.forma, Color(Colores.de(p.color).argb), Modifier.size(34.dp))
          Spacer(Modifier.width(12.dp))
          Text(nombre(p), modifier = Modifier.weight(1f))
          IconButton(onClick = { enlaces.openUri(vm.enlaceTienda(IdeaPrenda(p.subtipo, p.color, p.nombre))) }) {
            Icon(Icons.Filled.ShoppingCart, t(R.string.comprar), tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
    item { Text(t(R.string.maleta_looks), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp)) }
    item {
      LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(v.looks, key = { it.id }) { l ->
          val f = Instant.ofEpochMilli(l.fecha).atZone(ZoneOffset.UTC).toLocalDate()
          Column(
            Modifier
              .width(160.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(MaterialTheme.colorScheme.surfaceContainer)
              .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            VistaLook(l.prendas, e.datos.perfil.cuerpo, e.datos.perfil.vista, vm::archivoFoto, Modifier.fillMaxWidth().height(200.dp), claveAnimacion = l.id)
            Text(fechaCorta(f), style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Estadísticas del armario
// ---------------------------------------------------------------------------------------------

@Composable
fun PantallaEstadisticas(e: Estado, vm: AppViewModel, alCerrar: () -> Unit) {
  BackHandler(onBack = alCerrar)
  val s = remember(e.datos.prendas, e.datos.historial) { vm.estadisticas() }
  val dinero = remember { NumberFormat.getCurrencyInstance() }
  Column(Modifier.fillMaxSize()) {
    BarraSuperior(t(R.string.stats_titulo), alCerrar)
    if (s.total == 0) {
      EstadoVacio(Forma.TOP_MANGA_CORTA, t(R.string.stats_titulo), t(R.string.stats_vacio), null)
      return@Column
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Cifra(s.total.toString(), t(R.string.stats_prendas), Modifier.weight(1f))
          Cifra(s.usosTotales.toString(), t(R.string.stats_usos), Modifier.weight(1f))
          s.valorTotal?.let { Cifra(dinero.format(it), t(R.string.stats_valor), Modifier.weight(1f)) }
        }
      }
      item {
        Seccion(t(R.string.stats_colores)) {
          val maximo = s.porColor.maxOfOrNull { it.second } ?: 1
          s.porColor.take(8).forEach { (color, n) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              MuestraColor(color, 22.dp)
              Spacer(Modifier.width(10.dp))
              Text(t(colorRes(color)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
              Box(
                Modifier
                  .weight(n.toFloat() / maximo)
                  .height(12.dp)
                  .clip(RoundedCornerShape(50))
                  .background(Color(Colores.de(color).argb)),
              )
              if (n < maximo) Spacer(Modifier.weight((maximo - n).toFloat() / maximo))
              Text("  $n", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
      item {
        Seccion(t(R.string.stats_categorias)) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.porCategoria.forEach { (c, n) -> AssistChip(onClick = {}, label = { Text("${t(c.res())} · $n") }) }
          }
        }
      }
      if (s.masUsadas.isNotEmpty()) {
        item {
          Seccion(t(R.string.stats_mas)) {
            s.masUsadas.forEach { p ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                MiniaturaPrenda(p, vm.archivoFoto(p.foto), 48.dp)
                Spacer(Modifier.width(12.dp))
                Text(nombre(p), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.armario_usada, p.usos, p.usos), style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }
      if (s.nuncaUsadas.isNotEmpty()) {
        item {
          Seccion(stringResource(R.string.stats_nunca, s.nuncaUsadas.size)) {
            Text(t(R.string.stats_nunca_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(s.nuncaUsadas, key = { it.id }) { p -> MiniaturaPrenda(p, vm.archivoFoto(p.foto), 72.dp) }
            }
          }
        }
      }
      item {
        Seccion(t(R.string.stats_costo)) {
          if (s.costoPorUso.isEmpty()) {
            Text(t(R.string.stats_costo_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          s.costoPorUso.take(10).forEach { (p, costo) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
              MiniaturaPrenda(p, vm.archivoFoto(p.foto), 48.dp)
              Spacer(Modifier.width(12.dp))
              Text(nombre(p), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(stringResource(R.string.stats_por_uso, dinero.format(costo)), style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier) {
  Column(
    modifier
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.primaryContainer)
      .padding(14.dp),
  ) {
    Text(valor, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
  }
}
