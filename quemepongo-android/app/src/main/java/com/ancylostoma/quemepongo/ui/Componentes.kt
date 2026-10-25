@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.ancylostoma.quemepongo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ancylostoma.quemepongo.data.Colores
import com.ancylostoma.quemepongo.data.Cuerpo
import com.ancylostoma.quemepongo.data.Estilo
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.Fotos
import com.ancylostoma.quemepongo.data.Prenda
import java.io.File

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
  Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
fun MuestraColor(clave: String, tamano: Dp = 32.dp, elegido: Boolean = false, alTocar: (() -> Unit)? = null) {
  val c = Colores.de(clave)
  val color = Color(c.argb)
  Box(
    Modifier
      .size(tamano)
      .clip(CircleShape)
      .background(
        if (clave == Colores.ESTAMPADO) {
          androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Color(0xFFF2C94C), Color(0xFFEE9DB8), Color(0xFF2E8B57), Color(0xFF6A3D9A), Color(0xFFF2C94C)))
        } else {
          androidx.compose.ui.graphics.SolidColor(color)
        },
      )
      .border(BorderStroke(if (elegido) 3.dp else 1.dp, if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), CircleShape)
      .let { if (alTocar != null) it.clickable(onClick = alTocar) else it },
    contentAlignment = Alignment.Center,
  ) {
    if (elegido) Icon(Icons.Filled.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(tamano * 0.55f))
  }
}

@Composable
fun SelectorColores(elegidos: Set<String>, alCambiar: (String) -> Unit, incluirEstampado: Boolean = true) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Colores.todos.filter { incluirEstampado || it.clave != Colores.ESTAMPADO }.forEach { c ->
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        MuestraColor(c.clave, 36.dp, c.clave in elegidos) { alCambiar(c.clave) }
        Text(t(colorRes(c.clave)), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
fun SelectorEstilos(elegidos: Set<Estilo>, alCambiar: (Estilo) -> Unit) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Estilo.entries.forEach { s ->
      FilterChip(selected = s in elegidos, onClick = { alCambiar(s) }, label = { Text(t(s.res())) })
    }
  }
}

@Composable
fun SelectorCuerpo(actual: Cuerpo, alElegir: (Cuerpo) -> Unit) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Cuerpo.entries.forEach { c ->
      FilterChip(selected = c == actual, onClick = { alElegir(c) }, label = { Text(t(c.res())) })
    }
  }
}

/**
 * Miniatura de una prenda. Con foto sin fondo: la prenda sobre un fondo suave,
 * como en una tienda. Con foto normal: recortada al cuadro. Sin foto: su silueta
 * en su color.
 */
@Composable
fun MiniaturaPrenda(p: Prenda, archivo: File?, tamano: Dp, modifier: Modifier = Modifier) {
  val forma = RoundedCornerShape(16.dp)
  val recorte = Fotos.esRecorte(p.foto)
  Box(
    modifier
      .size(tamano)
      .clip(forma)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    contentAlignment = Alignment.Center,
  ) {
    when {
      archivo != null && recorte -> AsyncImage(
        model = archivo,
        contentDescription = nombre(p),
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(tamano).padding(tamano * 0.08f),
      )
      archivo != null -> AsyncImage(model = archivo, contentDescription = nombre(p), contentScale = ContentScale.Crop, modifier = Modifier.size(tamano))
      else -> IconoForma(p.subtipo.forma, Color(Colores.de(p.color).argb), Modifier.size(tamano * 0.72f))
    }
  }
}

/** Pantalla vacía: un dibujo, una frase y una sola acción. */
@Composable
fun EstadoVacio(forma: Forma, titulo: String, texto: String, boton: String?, alTocar: () -> Unit = {}) {
  Column(
    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      Modifier.size(132.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      IconoForma(forma, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), Modifier.size(84.dp), borde = Color.Transparent)
    }
    Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    if (boton != null) Button(onClick = alTocar) { Text(boton) }
  }
}

/** Título de pantalla con estilo de revista y, si hace falta, una acción a la derecha. */
@Composable
fun TituloPantalla(texto: String, subtitulo: String? = null, accion: (@Composable () -> Unit)? = null) {
  Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(texto, style = MaterialTheme.typography.headlineMedium)
      if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    accion?.invoke()
  }
}

@Composable
fun ListaRazones(razones: List<String>) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    razones.forEach { r ->
      Row(verticalAlignment = Alignment.Top) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(r, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

@Composable
fun Etiqueta(texto: String, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
  Text(
    texto,
    style = MaterialTheme.typography.labelMedium,
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(color)
      .padding(horizontal = 10.dp, vertical = 4.dp),
  )
}

@Composable
fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
  Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Titulo(titulo)
    contenido()
  }
}
