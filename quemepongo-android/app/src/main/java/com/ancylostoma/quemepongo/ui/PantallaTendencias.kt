package com.ancylostoma.quemepongo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.Tendencia

@Composable
fun PantallaTendencias(e: Estado, vm: AppViewModel, irAProbador: () -> Unit) {
  val activas = e.tendenciasActivas.sortedByDescending { it.peso }
  val ideas = remember(e.datos.prendas, e.datos.preferencias, e.tendencias, e.datos.perfil) { vm.ideasDeCompra() }
  val enlaces = LocalUriHandler.current

  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    item {
      Column {
        Text("Tendencias", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
          "Temporada, eventos y looks de famosos. Se actualizan solas" +
            (if (e.tendencias.actualizado.isNotBlank()) " (última: ${e.tendencias.actualizado})" else "") + ".",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    items(activas, key = { it.id }) { t ->
      TarjetaTendencia(t, vm, alProbar = {
        vm.probarLook(vm.lookDeTendencia(t).prendas)
        irAProbador()
      })
    }
    item { BannerAnuncio() }
    if (ideas.isNotEmpty()) {
      item {
        Column {
          Titulo("Para ti: más ropa como la que te gusta")
          Text(
            "Según tus \"me gusta\", tus looks guardados y lo que falta en tu armario.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      items(ideas, key = { "${it.idea.subtipo}-${it.idea.color}" }) { i ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          MuestraColor(i.idea.color, 36.dp)
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) {
            Text(i.idea.nombre, fontWeight = FontWeight.SemiBold)
            Text(i.motivo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          TextButton(onClick = { enlaces.openUri(vm.enlaceTienda(i.idea)) }) {
            Icon(Icons.Filled.ShoppingCart, null, Modifier.size(16.dp))
            Text(" Ver")
          }
        }
      }
    }
  }
}

@Composable
private fun TarjetaTendencia(t: Tendencia, vm: AppViewModel, alProbar: () -> Unit) {
  val enlaces = LocalUriHandler.current
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(t.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Etiqueta(
          when (t.tipo) {
            "evento" -> "Evento"
            "famosos" -> "Famosos"
            else -> "Temporada"
          },
        )
      }
      Text(t.descripcion)
      if (t.fuente.isNotBlank()) {
        Text(t.fuente, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { t.colores.forEach { MuestraColor(it, 24.dp) } }
      HorizontalDivider()
      t.prendas.forEach { idea: IdeaPrenda ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          MuestraColor(idea.color, 20.dp)
          Spacer(Modifier.width(10.dp))
          Text(idea.nombre, modifier = Modifier.weight(1f))
          TextButton(onClick = { enlaces.openUri(vm.enlaceTienda(idea)) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("Comprar")
          }
        }
      }
      Button(onClick = alProbar, modifier = Modifier.fillMaxWidth()) { Text("Ver este look en el maniquí") }
    }
  }
}
