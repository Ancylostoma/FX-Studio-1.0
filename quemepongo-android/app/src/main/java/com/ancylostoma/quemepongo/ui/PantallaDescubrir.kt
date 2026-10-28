@file:OptIn(ExperimentalMaterial3Api::class)

package com.ancylostoma.quemepongo.ui

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Tendencia
import com.ancylostoma.quemepongo.motor.IdeaCompra
import com.ancylostoma.quemepongo.motor.Motivo
import kotlinx.coroutines.delay

/**
 * Descubrir como una tienda: buscador, carrusel de tendencias, categorías en
 * círculos y una cuadrícula de prendas para comprar. Los enlaces van a la
 * tienda configurada en tendencias.json (ahí va el enlace de afiliado).
 */
@Composable
fun PantallaDescubrir(e: Estado, vm: AppViewModel, abrirEditor: (List<Prenda>) -> Unit) {
  val activas = e.tendenciasActivas.sortedByDescending { it.peso }
  val todas = remember(e.datos.prendas, e.datos.preferencias, e.tendencias, e.datos.perfil) { vm.ideasDeCompra(max = 24) }
  var categoria by rememberSaveable { mutableStateOf<Categoria?>(null) }
  val ideas = todas.filter { categoria == null || it.idea.subtipo.categoria == categoria }
  var abierta by remember { mutableStateOf<Tendencia?>(null) }
  val enlaces = LocalUriHandler.current

  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TituloPantalla(t(R.string.tab_descubrir), t(R.string.descubrir_sub))
        BuscadorTienda { q -> enlaces.openUri(vm.enlaceBusqueda(q)) }
      }
    }
    if (activas.isNotEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) { Carrusel(e, vm, activas) { abierta = it } }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t(R.string.descubrir_categorias), style = MaterialTheme.typography.titleLarge)
        Categorias(categoria) { c -> categoria = if (c == categoria) null else c }
      }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(Modifier.padding(top = 4.dp)) {
        Text(t(R.string.para_ti), style = MaterialTheme.typography.titleLarge)
        Text(t(R.string.para_ti_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    if (ideas.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
          t(R.string.descubrir_sin_ideas),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      }
    }
    items(ideas, key = { "${it.idea.subtipo}-${it.idea.color}" }) { i ->
      TarjetaProducto(i) { enlaces.openUri(vm.enlaceTienda(i.idea)) }
    }
    // Con Premium no hay anuncios.
    if (!e.premium) item(span = { GridItemSpan(maxLineSpan) }) { BannerAnuncio(Modifier.padding(top = 8.dp)) }
  }

  abierta?.let { t ->
    HojaTendencia(t, vm, alProbar = {
      abierta = null
      abrirEditor(vm.lookDeTendencia(t).prendas)
    }) { abierta = null }
  }
}

/** Buscador en forma de píldora: lo escrito se busca en la tienda. */
@Composable
private fun BuscadorTienda(alBuscar: (String) -> Unit) {
  var texto by rememberSaveable { mutableStateOf("") }
  val foco = LocalFocusManager.current
  val buscar = {
    if (texto.isNotBlank()) {
      foco.clearFocus()
      alBuscar(texto)
    }
  }
  val esquema = MaterialTheme.colorScheme
  OutlinedTextField(
    value = texto,
    onValueChange = { texto = it },
    placeholder = { Text(t(R.string.descubrir_buscar)) },
    singleLine = true,
    leadingIcon = { Icon(Icons.Filled.Search, null) },
    trailingIcon = {
      if (texto.isNotBlank()) IconButton(onClick = buscar) { Icon(Icons.AutoMirrored.Filled.ArrowForward, t(R.string.buscar)) }
    },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { buscar() }),
    shape = RoundedCornerShape(50),
    colors = OutlinedTextFieldDefaults.colors(
      unfocusedContainerColor = esquema.surfaceContainerHigh,
      focusedContainerColor = esquema.surfaceContainerHigh,
      unfocusedBorderColor = Color.Transparent,
    ),
    modifier = Modifier.fillMaxWidth(),
  )
}

/** Tendencias grandes, una tras otra; pasan solas cada pocos segundos. */
@Composable
private fun Carrusel(e: Estado, vm: AppViewModel, tendencias: List<Tendencia>, alTocar: (Tendencia) -> Unit) {
  val paginas = rememberPagerState { tendencias.size }
  // En las pruebas (Robolectric) no pasan solas: la prueba esperaría para siempre.
  if (tendencias.size > 1 && Build.FINGERPRINT != "robolectric") {
    LaunchedEffect(paginas, tendencias.size) {
      while (true) {
        delay(5_000)
        if (!paginas.isScrollInProgress) paginas.animateScrollToPage((paginas.currentPage + 1) % tendencias.size)
      }
    }
  }
  Column {
    HorizontalPager(state = paginas, pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth().height(210.dp)) { i ->
      Banner(e, vm, tendencias[i]) { alTocar(tendencias[i]) }
    }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
      repeat(tendencias.size) { i ->
        val ancho by animateDpAsState(if (i == paginas.currentPage) 18.dp else 6.dp, label = "punto")
        Box(
          Modifier
            .padding(horizontal = 3.dp)
            .size(width = ancho, height = 6.dp)
            .clip(CircleShape)
            .background(if (i == paginas.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        )
      }
    }
  }
}

/**
 * Una tendencia como cartel: fondo con sus colores (o su foto, si tendencias.json
 * la trae), el look en el maniquí a la derecha y el texto sobre una sombra
 * oscura para que siempre se lea.
 */
@Composable
private fun Banner(e: Estado, vm: AppViewModel, t: Tendencia, alTocar: () -> Unit) {
  val look = remember(t.id, e.datos.prendas) { vm.lookDeTendencia(t) }
  val esquema = MaterialTheme.colorScheme
  val tonos = t.colores.take(2).map { Color(Colores.de(it).argb) }.ifEmpty { listOf(esquema.primary, esquema.secondary) }
  Box(
    Modifier
      .fillMaxSize()
      .clip(RoundedCornerShape(24.dp))
      .background(Brush.linearGradient(if (tonos.size == 1) tonos + lerp(tonos[0], Color.Black, 0.3f) else tonos))
      .clickable(onClick = alTocar),
  ) {
    if (t.imagen.isNotBlank()) {
      AsyncImage(model = t.imagen, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
      Box(Modifier.fillMaxHeight().fillMaxWidth(0.42f).align(Alignment.CenterEnd).padding(vertical = 12.dp)) {
        Maniqui(look.prendas, e.datos.perfil.cuerpo, vm::archivoFoto, Modifier.fillMaxSize(), claveAnimacion = t.id, marcarIdeas = false)
      }
    }
    // Sombra de izquierda a derecha: el texto blanco se lee sobre cualquier color.
    Box(
      Modifier
        .fillMaxSize()
        .background(Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.66f), 0.62f to Color.Black.copy(alpha = 0.25f), 1f to Color.Transparent)),
    )
    Column(
      Modifier.fillMaxHeight().fillMaxWidth(0.62f).padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Whatshot, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
          when (t.tipo) {
            "evento" -> t(R.string.tipo_evento)
            "famosos" -> t(R.string.tipo_famosos)
            else -> t(R.string.tipo_temporada)
          },
          style = MaterialTheme.typography.labelMedium,
          color = Color.White,
        )
      }
      Text(t.titulo, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Text(t.descripcion, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.92f), maxLines = 2, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.weight(1f))
      Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(t(R.string.ver_look), style = MaterialTheme.typography.labelLarge, color = Color(0xFF15120F), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF15120F), modifier = Modifier.size(16.dp))
      }
    }
  }
}

/** Categorías en círculos, como en las tiendas de moda. Tocar otra vez la elegida la quita. */
@Composable
private fun Categorias(elegida: Categoria?, alElegir: (Categoria?) -> Unit) {
  val opciones = listOf<Categoria?>(null) + Categoria.entries
  LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
    items(opciones, key = { it?.name ?: "todo" }) { c ->
      val activa = c == elegida
      val esquema = MaterialTheme.colorScheme
      Column(
        Modifier.width(68.dp).clip(RoundedCornerShape(14.dp)).clickable { alElegir(c) }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (activa) Brush.linearGradient(listOf(esquema.primary, esquema.secondary)) else Brush.linearGradient(listOf(esquema.surfaceContainerHigh, esquema.surfaceContainerHighest))),
          contentAlignment = Alignment.Center,
        ) {
          if (c == null) {
            Icon(Icons.Filled.Apps, null, tint = if (activa) esquema.onPrimary else esquema.primary, modifier = Modifier.size(28.dp))
          } else {
            IconoForma(
              formaDeCategoria(c),
              if (activa) esquema.onPrimary else esquema.primary,
              Modifier.size(34.dp),
              borde = if (activa) esquema.onPrimary.copy(alpha = 0.6f) else esquema.onSurface.copy(alpha = 0.35f),
            )
          }
        }
        Spacer(Modifier.height(6.dp))
        Text(
          if (c == null) t(R.string.armario_todo) else t(c.res()),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
          color = if (activa) esquema.primary else esquema.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Una prenda sugerida como producto de tienda: dibujo grande sobre un fondo de
 * su color, etiqueta de por qué se sugiere y botón para verla en la tienda.
 */
@Composable
private fun TarjetaProducto(i: IdeaCompra, alComprar: () -> Unit) {
  val esquema = MaterialTheme.colorScheme
  val oscuro = LocalOscuro.current
  val colorPrenda = Color(Colores.de(i.idea.color).argb)
  val fondo = if (oscuro) esquema.surfaceContainer else esquema.surfaceContainerLowest
  val forma = RoundedCornerShape(20.dp)
  Column(
    Modifier
      .shadow(if (oscuro) 0.dp else 3.dp, forma)
      .clip(forma)
      .background(fondo)
      .border(1.dp, esquema.outlineVariant, forma)
      .clickable(onClick = alComprar),
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .aspectRatio(0.9f)
        .background(Brush.verticalGradient(listOf(lerp(colorPrenda, fondo, 0.86f), lerp(colorPrenda, fondo, 0.7f)))),
      contentAlignment = Alignment.Center,
    ) {
      IconoForma(i.idea.subtipo.forma, colorPrenda, Modifier.fillMaxSize(0.6f))
      EtiquetaMotivo(i.motivo, Modifier.align(Alignment.TopStart).padding(8.dp))
      Box(
        Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .size(34.dp)
          .shadow(3.dp, CircleShape)
          .clip(CircleShape)
          .background(if (oscuro) esquema.surfaceContainerHighest else Color.White),
        contentAlignment = Alignment.Center,
      ) { Icon(Icons.Filled.ShoppingCart, t(R.string.buscar_tienda), tint = esquema.primary, modifier = Modifier.size(18.dp)) }
    }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(nombre(i.idea), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        texto(i.motivo),
        style = MaterialTheme.typography.bodySmall,
        color = esquema.onSurfaceVariant,
        maxLines = 2,
        minLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(4.dp))
      Text(t(R.string.ver_tienda), style = MaterialTheme.typography.labelLarge, color = esquema.primary, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun EtiquetaMotivo(m: Motivo, modifier: Modifier = Modifier) {
  val esquema = MaterialTheme.colorScheme
  val (texto, tendencia) = when (m) {
    is Motivo.EnTendencia -> t(R.string.etiqueta_tendencia) to true
    is Motivo.FaltaCategoria -> t(R.string.etiqueta_te_falta) to false
    Motivo.Gusto -> t(R.string.etiqueta_para_ti) to false
  }
  Row(
    modifier
      .clip(RoundedCornerShape(50))
      .background(if (tendencia) Brush.horizontalGradient(listOf(esquema.primary, esquema.secondary)) else Brush.linearGradient(listOf(esquema.inverseSurface, esquema.inverseSurface)))
      .padding(horizontal = 8.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (tendencia) {
      Icon(Icons.Filled.Whatshot, null, tint = esquema.onPrimary, modifier = Modifier.size(12.dp))
      Spacer(Modifier.width(3.dp))
    }
    Text(texto, style = MaterialTheme.typography.labelSmall, color = if (tendencia) esquema.onPrimary else esquema.inverseOnSurface, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun HojaTendencia(t: Tendencia, vm: AppViewModel, alProbar: () -> Unit, alCerrar: () -> Unit) {
  val enlaces = LocalUriHandler.current
  ModalBottomSheet(onDismissRequest = alCerrar) {
    Column(
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(t.titulo, style = MaterialTheme.typography.headlineSmall)
      Text(t.descripcion, style = MaterialTheme.typography.bodyMedium)
      if (t.fuente.isNotBlank()) Text(t.fuente, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { t.colores.forEach { MuestraColor(it, 22.dp) } }
      Text(t(R.string.piezas), style = MaterialTheme.typography.titleMedium)
      t.prendas.forEach { idea ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconoForma(idea.subtipo.forma, Color(Colores.de(idea.color).argb), Modifier.size(34.dp))
          Spacer(Modifier.width(12.dp))
          Text(nombre(idea), modifier = Modifier.weight(1f))
          TextButton(onClick = { enlaces.openUri(vm.enlaceTienda(idea)) }) { Text(t(R.string.comprar)) }
        }
      }
      BotonPrincipal(onClick = alProbar, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(R.string.probar_look)) }
      OutlinedButton(onClick = alCerrar, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.cerrar)) }
      Spacer(Modifier.height(12.dp))
    }
  }
}
