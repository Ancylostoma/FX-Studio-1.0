package com.ancylostoma.quemepongo.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.data.IdeaPrenda
import com.ancylostoma.quemepongo.data.Look
import com.ancylostoma.quemepongo.data.Momento
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.ServicioClima
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PantallaHoy(e: Estado, vm: AppViewModel, irAProbador: () -> Unit, irAArmario: () -> Unit) {
  LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    item { Encabezado() }
    item { TarjetaClima(e, vm) }
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Ocasion.entries) { o ->
          FilterChip(selected = o == e.ocasion, onClick = { vm.elegirOcasion(o) }, label = { Text(o.etiqueta) })
        }
      }
    }
    item {
      val look = e.lookActual
      if (look == null) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      } else {
        TarjetaLook(e, vm, look, irAProbador)
      }
    }
    if (e.datos.prendas.count { !it.virtual } < 4) {
      item {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
          Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Titulo("Viste al maniquí con tu propia ropa")
            Text("Fotografía lo que tienes en el armario y los looks se armarán con tus prendas. Lo que te falte aparece como idea de compra.")
            Button(onClick = irAArmario) { Text("Agregar mis prendas") }
          }
        }
      }
    }
    item { BannerAnuncio() }
  }
}

@Composable
private fun Encabezado() {
  val hoy = LocalDate.now()
  val dia = hoy.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }
  val mes = hoy.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
  Column {
    Text("$dia ${hoy.dayOfMonth} de $mes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text("¿Qué me pongo hoy?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun TarjetaClima(e: Estado, vm: AppViewModel) {
  val contexto = LocalContext.current
  var pidiendoCiudad by remember { mutableStateOf(false) }
  var avisoPermiso by remember { mutableStateOf(false) }
  val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
    if (ok) vm.usarUbicacion() else avisoPermiso = true
  }
  val pedirUbicacion = {
    if (ServicioClima.tienePermiso(contexto)) vm.usarUbicacion() else permiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
  }

  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      val condicion = e.clima?.para(e.momento)
      when {
        e.climaCargando -> Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(12.dp))
          Text("Consultando el tiempo…")
        }
        condicion != null -> Row(verticalAlignment = Alignment.CenterVertically) {
          Text("${condicion.temperatura.roundToInt()}°", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
          Spacer(Modifier.width(14.dp))
          Column(Modifier.weight(1f)) {
            Text(condicion.descripcion.ifBlank { "Pronóstico" }, fontWeight = FontWeight.SemiBold)
            Text(
              "Sensación ${condicion.sensacion.roundToInt()}° · ${e.clima?.lugar.orEmpty()}",
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(onClick = { vm.cargarClima() }) { Icon(Icons.Filled.Refresh, "Actualizar el tiempo") }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            e.climaError ?: "Dime dónde estás y adapto la ropa al tiempo que hará.",
            style = MaterialTheme.typography.bodyMedium,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = pedirUbicacion) {
              Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Mi ubicación")
            }
            OutlinedButton(onClick = { pidiendoCiudad = true }) { Text("Escribir ciudad") }
          }
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Momento.entries.forEach { m ->
          FilterChip(selected = m == e.momento, onClick = { vm.elegirMomento(m) }, label = { Text(m.etiqueta) })
        }
      }
      if (e.clima != null) {
        Text("Datos del tiempo: MET Norway", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }

  if (pidiendoCiudad) DialogoCiudad(alCerrar = { pidiendoCiudad = false }) { vm.buscarCiudad(it) }
  if (avisoPermiso) {
    AlertDialog(
      onDismissRequest = { avisoPermiso = false },
      confirmButton = { TextButton(onClick = { avisoPermiso = false; pidiendoCiudad = true }) { Text("Escribir ciudad") } },
      dismissButton = { TextButton(onClick = { avisoPermiso = false }) { Text("Cerrar") } },
      title = { Text("Sin permiso de ubicación") },
      text = { Text("Sin tu ubicación no sé qué tiempo hace. Puedes escribir tu ciudad.") },
    )
  }
}

@Composable
fun DialogoCiudad(alCerrar: () -> Unit, alBuscar: (String) -> Unit) {
  var texto by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = alCerrar,
    title = { Text("¿En qué ciudad estás?") },
    text = {
      OutlinedTextField(value = texto, onValueChange = { texto = it }, singleLine = true, label = { Text("Ciudad") })
    },
    confirmButton = {
      TextButton(onClick = { alBuscar(texto); alCerrar() }, enabled = texto.isNotBlank()) { Text("Buscar") }
    },
    dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
  )
}

@Composable
private fun TarjetaLook(e: Estado, vm: AppViewModel, look: Look, irAProbador: () -> Unit) {
  val enlaces = LocalUriHandler.current
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Look ${e.indice + 1} de ${e.sugerencias.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        look.tendencia?.let { Etiqueta("🔥 $it") }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { vm.anterior() }, enabled = e.indice > 0) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior") }
        Maniqui(
          prendas = look.prendas,
          cuerpo = e.datos.perfil.cuerpo,
          foto = vm::archivoFoto,
          claveAnimacion = look.id,
          modifier = Modifier.weight(1f).height(400.dp),
        )
        IconButton(onClick = { vm.siguiente() }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Otro look") }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(look.prendas, key = { it.id }) { p ->
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
            MiniaturaPrenda(p, vm.archivoFoto(p.foto), 64.dp)
            Text(p.nombre, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (p.virtual) {
              TextButton(
                onClick = { enlaces.openUri(vm.enlaceTienda(IdeaPrenda(p.subtipo, p.color, p.nombre))) },
                contentPadding = PaddingValues(0.dp),
              ) {
                Icon(Icons.Filled.ShoppingCart, null, Modifier.size(14.dp))
                Text(" Buscar", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }

      ListaRazones(look.razones)

      Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        AccionLook(Icons.Filled.ThumbDown, "No") { vm.noMeGusta(look) }
        AccionLook(Icons.Filled.Favorite, "Me gusta") { vm.meGusta(look) }
        AccionLook(Icons.Filled.Bookmark, "Guardar") { vm.guardarLook(look) }
        AccionLook(Icons.Filled.Check, "Me lo pongo") { vm.meLoPongo(look) }
      }
      OutlinedButton(
        onClick = {
          vm.probarLook(look.prendas)
          irAProbador()
        },
        modifier = Modifier.fillMaxWidth(),
      ) { Text("Cambiar prendas en el probador") }
    }
  }
}

@Composable
private fun AccionLook(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, alTocar: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    IconButton(onClick = alTocar) { Icon(icono, texto, tint = MaterialTheme.colorScheme.primary) }
    Text(texto, style = MaterialTheme.typography.labelSmall)
  }
}
