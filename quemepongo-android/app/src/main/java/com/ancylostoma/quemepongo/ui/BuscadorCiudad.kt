package com.ancylostoma.quemepongo.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.ServicioClima

/**
 * Elegir dónde estás: con la ubicación del teléfono o escribiendo la ciudad.
 * Al buscar aparece una lista para elegir (hay muchas "Valencia" o "Santiago").
 */
@Composable
fun BuscadorCiudad(e: Estado, vm: AppViewModel, modifier: Modifier = Modifier) {
  val contexto = LocalContext.current
  val foco = LocalFocusManager.current
  var texto by rememberSaveable { mutableStateOf("") }
  val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.usarUbicacion() }
  val buscar = {
    if (texto.isNotBlank()) {
      foco.clearFocus()
      vm.buscarCiudad(texto)
    }
  }
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    FilledTonalButton(
      onClick = { if (ServicioClima.tienePermiso(contexto)) vm.usarUbicacion() else permiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
      modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
      Icon(Icons.Filled.MyLocation, null, Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(t(R.string.contexto_ubicacion))
    }
    OutlinedTextField(
      value = texto,
      onValueChange = { texto = it },
      label = { Text(t(R.string.contexto_ciudad)) },
      singleLine = true,
      leadingIcon = { Icon(Icons.Filled.Search, null) },
      trailingIcon = {
        if (texto.isNotBlank()) IconButton(onClick = buscar) { Icon(Icons.AutoMirrored.Filled.ArrowForward, t(R.string.buscar)) }
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { buscar() }),
      shape = RoundedCornerShape(50),
      modifier = Modifier.fillMaxWidth(),
    )
    if (e.climaCargando) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(t(R.string.b_consultando), style = MaterialTheme.typography.bodyMedium)
      }
    }
    e.climaError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    if (e.ciudades.isNotEmpty()) {
      Text(t(R.string.ciudad_elige), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      e.ciudades.forEach { l ->
        Row(
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
              texto = ""
              vm.elegirCiudad(l)
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.width(12.dp))
          Column {
            Text(l.nombre, style = MaterialTheme.typography.titleSmall)
            if (l.region.isNotBlank()) Text(l.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
      // Los datos de OpenStreetMap piden mencionar su origen.
      if (e.ciudades.any { it.osm }) {
        Text("© OpenStreetMap", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
