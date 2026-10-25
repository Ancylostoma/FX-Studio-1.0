package com.ancylostoma.quemepongo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.motor.Motor

@Composable
fun PantallaProbador(e: Estado, vm: AppViewModel) {
  var categoria by remember { mutableStateOf(Categoria.SUPERIOR) }
  // La última prenda puesta es la que se anima al aparecer.
  var ultima by remember { mutableStateOf<String?>(null) }
  val reales = e.datos.prendas.filter { !it.virtual }
  val puestas = e.probador.map { it.id }.toSet()

  Column(
    Modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("Probador", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Toca una prenda para ponérsela al maniquí. Se cambia sola por la anterior del mismo tipo.", color = MaterialTheme.colorScheme.onSurfaceVariant)

    Maniqui(
      prendas = e.probador,
      cuerpo = e.datos.perfil.cuerpo,
      foto = vm::archivoFoto,
      claveAnimacion = ultima ?: "inicio",
      animarSolo = ultima,
      modifier = Modifier.fillMaxWidth().height(420.dp),
    )

    if (e.probador.isNotEmpty()) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(e.probador, key = { it.id }) { p ->
          InputChip(
            selected = false,
            onClick = { vm.quitarDelProbador(p) },
            label = { Text(p.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Filled.Close, "Quitar", Modifier.size(16.dp)) },
          )
        }
      }
      Valoracion(e, vm)
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(Categoria.entries) { c ->
        FilterChip(selected = c == categoria, onClick = { categoria = c }, label = { Text(c.etiqueta) })
      }
    }
    val deCategoria = reales.filter { it.categoria == categoria }
    if (deCategoria.isEmpty()) {
      Text("No tienes prendas de este tipo. Agrégalas en Armario.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
              (if (p.id in puestas) "✓ " else "") + p.nombre,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = { vm.vaciarProbador(); ultima = null }, enabled = e.probador.isNotEmpty()) { Text("Vaciar") }
      Button(onClick = { vm.puntuarProbador()?.let { vm.guardarLook(it) } }, enabled = e.probador.isNotEmpty()) { Text("Guardar look") }
    }
  }
}

/** Qué tal queda lo que lleva puesto: tiempo, colores y ocasión. */
@Composable
private fun Valoracion(e: Estado, vm: AppViewModel) {
  val look = vm.puntuarProbador() ?: return
  val condicion = e.clima?.para(e.momento)
  val avisos = mutableListOf<String>()
  if (condicion != null) {
    val diferencia = Motor.abrigoDelLook(look.prendas) - Motor.abrigoIdeal(condicion.sensacion)
    avisos.add(
      when {
        diferencia > 1.2 -> "🥵 Vas demasiado abrigado para ${condicion.sensacion.toInt()} °C"
        diferencia < -1.2 -> "🥶 Vas a pasar frío con ${condicion.sensacion.toInt()} °C"
        else -> "👌 Abrigo justo para el tiempo de hoy"
      },
    )
    if (condicion.lluvia && look.prendas.none { it.impermeable }) avisos.add("☔ Va a llover y no llevas nada impermeable")
  }
  val armonia = Colores.armonia(look.prendas.map { it.color })
  avisos.add(
    when {
      armonia >= 1.0 -> "🎨 Los colores combinan muy bien"
      armonia >= 0.0 -> "🎨 Los colores van bien"
      else -> "🎨 Demasiados colores fuertes juntos: prueba con un neutro"
    },
  )
  val formalidad = look.prendas.map { it.formalidad }.average()
  val dif = formalidad - e.ocasion.formalidad
  if (dif > 1.2) avisos.add("👔 Muy arreglado para \"${e.ocasion.etiqueta}\"")
  if (dif < -1.2) avisos.add("👕 Muy informal para \"${e.ocasion.etiqueta}\"")

  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("¿Qué tal queda? (para: ${e.ocasion.etiqueta.lowercase()})", fontWeight = FontWeight.SemiBold)
      avisos.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
  }
}
