package com.ancylostoma.quemepongo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.motor.Motor
import java.util.Locale

/** Cambiar prendas de un look sobre el maniquí (antes "Probador"). */
@Composable
fun EditorLook(e: Estado, vm: AppViewModel, alCerrar: () -> Unit) {
  BackHandler(onBack = alCerrar)
  var categoria by remember { mutableStateOf(Categoria.SUPERIOR) }
  // La última prenda puesta es la que aparece animada.
  var ultima by remember { mutableStateOf<String?>(null) }
  val reales = e.datos.prendas.filter { !it.virtual }
  val puestas = e.probador.map { it.id }.toSet()

  Column(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = alCerrar) { Icon(Icons.Filled.Close, t(R.string.cerrar)) }
      Text(t(R.string.editor_titulo), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
      TextButton(
        onClick = {
          vm.puntuarProbador()?.let { vm.guardarLook(it) }
          alCerrar()
        },
        enabled = e.probador.isNotEmpty(),
      ) { Text(t(R.string.guardar)) }
    }

    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Maniqui(
        prendas = e.probador,
        cuerpo = e.datos.perfil.cuerpo,
        foto = vm::archivoFoto,
        claveAnimacion = ultima ?: "inicio",
        animarSolo = ultima,
        modifier = Modifier.fillMaxWidth().height(380.dp),
      )

      if (e.probador.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(e.probador, key = { it.id }) { p ->
            InputChip(
              selected = false,
              onClick = { vm.quitarDelProbador(p) },
              label = { Text(nombre(p), maxLines = 1, overflow = TextOverflow.Ellipsis) },
              trailingIcon = { Icon(Icons.Filled.Close, t(R.string.editor_quitar), Modifier.size(16.dp)) },
            )
          }
        }
        Valoracion(e, vm)
      }

      Text(t(R.string.editor_toca), style = MaterialTheme.typography.titleMedium)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Categoria.entries) { c ->
          FilterChip(
            selected = c == categoria,
            onClick = { categoria = c },
            label = { Text(t(c.res())) },
            leadingIcon = { IconoForma(formaDeCategoria(c), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(18.dp)) },
          )
        }
      }
      val deCategoria = reales.filter { it.categoria == categoria }
      if (deCategoria.isEmpty()) {
        Text(t(R.string.editor_sin_prendas), color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(deCategoria, key = { it.id }) { p ->
            Column(
              Modifier
                .width(84.dp)
                .clickable {
                  if (p.id in puestas) {
                    vm.quitarDelProbador(p)
                  } else {
                    ultima = p.id
                    vm.ponerEnProbador(p)
                  }
                },
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              MiniaturaPrenda(p, vm.archivoFoto(p.foto), 76.dp)
              Text(
                (if (p.id in puestas) "✓ " else "") + nombre(p),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
      if (e.probador.isNotEmpty()) {
        TextButton(onClick = { vm.vaciarProbador(); ultima = null }) { Text(t(R.string.editor_quitar_todo)) }
      }
      Spacer(Modifier.height(16.dp))
    }

    Button(
      onClick = {
        vm.puntuarProbador()?.let { vm.meLoPongo(it) }
        alCerrar()
      },
      enabled = e.probador.isNotEmpty(),
      modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
    ) { Text(t(R.string.hoy_me_lo_pongo), style = MaterialTheme.typography.titleMedium) }
  }
}

/** Qué tal queda lo puesto: tiempo, lluvia, colores y ocasión. */
@Composable
private fun Valoracion(e: Estado, vm: AppViewModel) {
  val look = vm.puntuarProbador() ?: return
  val condicion = e.clima?.para(e.momento)
  val avisos = mutableListOf<Pair<ImageVector, String>>()
  if (condicion != null) {
    val temp = temperatura(condicion.sensacion)
    val diferencia = Motor.abrigoDelLook(look.prendas) - Motor.abrigoIdeal(condicion.sensacion)
    avisos.add(
      Icons.Filled.Thermostat to when {
        diferencia > 1.2 -> stringResource(R.string.val_abrigado, temp)
        diferencia < -1.2 -> stringResource(R.string.val_frio, temp)
        else -> stringResource(R.string.val_justo, temp)
      },
    )
    if (condicion.lluvia && look.prendas.none { it.impermeable }) avisos.add(Icons.Filled.Umbrella to t(R.string.val_lluvia))
  }
  val armonia = Colores.armonia(look.prendas.map { it.color })
  avisos.add(
    Icons.Filled.Palette to when {
      armonia >= 1.0 -> t(R.string.val_colores_muy)
      armonia >= 0.0 -> t(R.string.val_colores_bien)
      else -> t(R.string.val_colores_mal)
    },
  )
  val dif = look.prendas.map { it.formalidad }.average() - e.ocasion.formalidad
  val ocasion = t(e.ocasion.res()).lowercase(Locale.getDefault())
  avisos.add(
    Icons.Filled.Work to when {
      dif > 1.2 -> stringResource(R.string.val_muy_arreglado, ocasion)
      dif < -1.2 -> stringResource(R.string.val_muy_informal, ocasion)
      else -> stringResource(R.string.val_ocasion_bien, ocasion)
    },
  )
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    avisos.forEach { (icono, texto) ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}
