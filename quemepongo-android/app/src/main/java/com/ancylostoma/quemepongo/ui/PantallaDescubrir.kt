@file:OptIn(ExperimentalMaterial3Api::class)

package com.ancylostoma.quemepongo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Tendencia

@Composable
fun PantallaDescubrir(e: Estado, vm: AppViewModel, abrirEditor: (List<Prenda>) -> Unit) {
  val activas = e.tendenciasActivas.sortedByDescending { it.peso }
  val ideas = remember(e.datos.prendas, e.datos.preferencias, e.tendencias, e.datos.perfil) { vm.ideasDeCompra() }
  var abierta by remember { mutableStateOf<Tendencia?>(null) }
  val enlaces = LocalUriHandler.current

  LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item { Box(Modifier.padding(horizontal = 16.dp)) { TituloPantalla(t(R.string.tab_descubrir), t(R.string.descubrir_sub)) } }
    item {
      LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(activas, key = { it.id }) { t -> TarjetaTendencia(e, vm, t) { abierta = t } }
      }
    }
    if (ideas.isNotEmpty()) {
      item {
        Column(Modifier.padding(horizontal = 16.dp)) {
          Text(t(R.string.para_ti), style = MaterialTheme.typography.titleLarge)
          Text(
            t(R.string.para_ti_texto),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      items(ideas, key = { "${it.idea.subtipo}-${it.idea.color}" }) { i ->
        Row(
          Modifier
            .fillMaxWidth()
            .clickable { enlaces.openUri(vm.enlaceTienda(i.idea)) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
          ) {
            IconoForma(i.idea.subtipo.forma, androidx.compose.ui.graphics.Color(com.ancylostoma.quemepongo.data.Colores.de(i.idea.color).argb), Modifier.size(38.dp))
          }
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) {
            Text(nombre(i.idea), style = MaterialTheme.typography.bodyLarge)
            Text(texto(i.motivo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Icon(Icons.Filled.ShoppingCart, t(R.string.buscar_tienda), tint = MaterialTheme.colorScheme.primary)
        }
      }
    }
    // Con Premium no hay anuncios.
    if (!e.premium) item { BannerAnuncio(Modifier.padding(top = 8.dp)) }
  }

  abierta?.let { t ->
    HojaTendencia(t, vm, alProbar = {
      abierta = null
      abrirEditor(vm.lookDeTendencia(t).prendas)
    }) { abierta = null }
  }
}

/** Cada tendencia con su look dibujado en el maniquí: sin fotos de famosos ni derechos de imagen. */
@Composable
private fun TarjetaTendencia(e: Estado, vm: AppViewModel, t: Tendencia, alTocar: () -> Unit) {
  val look = remember(t.id, e.datos.prendas) { vm.lookDeTendencia(t) }
  Column(
    Modifier
      .width(250.dp)
      .escenario(RoundedCornerShape(24.dp))
      .clickable(onClick = alTocar)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Maniqui(look.prendas, e.datos.perfil.cuerpo, vm::archivoFoto, Modifier.fillMaxWidth().height(230.dp), claveAnimacion = t.id)
    Etiqueta(
      when (t.tipo) {
        "evento" -> t(R.string.tipo_evento)
        "famosos" -> t(R.string.tipo_famosos)
        else -> t(R.string.tipo_temporada)
      },
    )
    Text(t.titulo, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(t.descripcion, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(t(R.string.ver_piezas), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
          IconoForma(idea.subtipo.forma, androidx.compose.ui.graphics.Color(com.ancylostoma.quemepongo.data.Colores.de(idea.color).argb), Modifier.size(34.dp))
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
