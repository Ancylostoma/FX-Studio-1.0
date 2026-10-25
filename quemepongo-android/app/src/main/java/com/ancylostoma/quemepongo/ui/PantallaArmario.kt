package com.ancylostoma.quemepongo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.Prenda

@Composable
fun PantallaArmario(e: Estado, vm: AppViewModel, alAgregar: () -> Unit, alEditar: (Prenda) -> Unit) {
  var filtro by remember { mutableStateOf<Categoria?>(null) }
  val reales = e.datos.prendas.filter { !it.virtual }
  val visibles = reales.filter { filtro == null || it.categoria == filtro }.sortedByDescending { it.creada }

  LazyVerticalGrid(
    columns = GridCells.Adaptive(104.dp),
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TituloPantalla("Armario", if (reales.isEmpty()) null else if (reales.size == 1) "1 prenda" else "${reales.size} prendas")
        if (reales.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = filtro == null, onClick = { filtro = null }, label = { Text("Todo") }) }
            items(Categoria.entries.filter { c -> reales.any { it.categoria == c } }) { c ->
              FilterChip(
                selected = filtro == c,
                onClick = { filtro = if (filtro == c) null else c },
                label = { Text(c.etiqueta) },
                leadingIcon = { IconoForma(formaDeCategoria(c), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(18.dp)) },
              )
            }
          }
        }
      }
    }
    if (reales.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        EstadoVacio(
          Forma.CHAQUETA,
          "Tu armario está vacío",
          "Fotografía tu ropa y los looks se armarán con tus prendas. Le quitamos el fondo a cada foto.",
          "Fotografía tu primera prenda",
          alAgregar,
        )
      }
    }
    items(visibles, key = { it.id }) { p ->
      Column(Modifier.clickable { alEditar(p) }) {
        MiniaturaPrenda(p, vm.archivoFoto(p.foto), 104.dp, Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(p.nombre, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          if (p.usos == 0) "Sin estrenar" else "Usada ${p.usos} ${if (p.usos == 1) "vez" else "veces"}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
