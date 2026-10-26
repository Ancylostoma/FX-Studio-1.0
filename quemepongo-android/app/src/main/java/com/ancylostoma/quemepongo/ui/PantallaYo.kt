@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.ancylostoma.quemepongo.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.BuildConfig
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.AvisoDiario
import com.ancylostoma.quemepongo.data.Forma
import com.ancylostoma.quemepongo.data.LookGuardado
import com.ancylostoma.quemepongo.data.Modo
import com.ancylostoma.quemepongo.data.Paleta
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.Vista
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private const val URL_PRIVACIDAD = "https://github.com/Ancylostoma/FX-Studio-1.0/blob/main/quemepongo-android/PRIVACIDAD.md"

@Composable
fun PantallaYo(
  e: Estado,
  vm: AppViewModel,
  abrirEditor: (List<Prenda>) -> Unit,
  abrirAjustes: () -> Unit,
  irAHoy: () -> Unit,
  abrirHerramienta: (MotivoPremium) -> Unit,
) {
  var vista by remember { mutableIntStateOf(0) }
  var elegido by remember { mutableStateOf<LookGuardado?>(null) }
  val contexto = LocalContext.current
  val alcance = rememberCoroutineScope()
  val lista = if (vista == 0) e.datos.looks else e.datos.historial
  // Si la prenda se editó después, se muestra como está ahora.
  fun actuales(l: LookGuardado) = l.prendas.map { p -> e.datos.prendas.firstOrNull { it.id == p.id } ?: p }

  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val oscuro = LocalOscuro.current
        TituloPantalla(t(R.string.tab_yo), accion = {
          Row {
            // Atajo para cambiar entre claro y oscuro sin entrar a Ajustes.
            IconButton(onClick = { vm.cambiarPerfil { it.copy(modo = if (oscuro) Modo.CLARO else Modo.OSCURO) } }) {
              Icon(if (oscuro) Icons.Filled.LightMode else Icons.Filled.DarkMode, t(if (oscuro) R.string.modo_claro else R.string.modo_oscuro))
            }
            IconButton(onClick = abrirAjustes) { Icon(Icons.Filled.Settings, t(R.string.ajustes)) }
          }
        })
        Herramientas(e.premium, abrirHerramienta)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
          listOf(stringResource(R.string.yo_guardados, e.datos.looks.size), stringResource(R.string.yo_puestos, e.datos.historial.size)).forEachIndexed { i, texto ->
            SegmentedButton(
              selected = vista == i,
              onClick = { vista = i },
              shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
            ) { Text(texto) }
          }
        }
      }
    }
    if (lista.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        if (vista == 0) {
          EstadoVacio(Forma.VESTIDO_CORTO, t(R.string.yo_vacio_g_titulo), t(R.string.yo_vacio_g_texto), t(R.string.yo_vacio_g_boton), irAHoy)
        } else {
          EstadoVacio(Forma.TOP_MANGA_CORTA, t(R.string.yo_vacio_p_titulo), t(R.string.yo_vacio_p_texto), t(R.string.yo_vacio_p_boton), irAHoy)
        }
      }
    }
    items(lista, key = { it.id }) { l ->
      Column(
        Modifier
          .escenario(RoundedCornerShape(22.dp))
          .clickable { elegido = l }
          .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        VistaLook(actuales(l), e.datos.perfil.cuerpo, e.datos.perfil.vista, vm::archivoFoto, Modifier.fillMaxWidth().height(190.dp), claveAnimacion = l.id)
        Text(t(l.ocasion.res()), style = MaterialTheme.typography.labelLarge)
        Text(
          DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(l.fecha)),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }

  elegido?.let { l ->
    ModalBottomSheet(onDismissRequest = { elegido = null }) {
      Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t(l.ocasion.res()), style = MaterialTheme.typography.titleLarge)
        Text(l.prendas.joinToString(" · ") { contexto.nombre(it) }, style = MaterialTheme.typography.bodyMedium)
        BotonPrincipal(onClick = {
          elegido = null
          abrirEditor(actuales(l))
        }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(R.string.yo_ponermelo)) }
        OutlinedButton(onClick = {
          val prendas = actuales(l)
          elegido = null
          alcance.launch { runCatching { Compartir.look(contexto, prendas, e.datos.perfil.cuerpo, vm::archivoFoto, contexto.getString(R.string.compartir_titulo)) } }
        }, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.hoy_compartir)) }
        OutlinedButton(onClick = {
          elegido = null
          if (vista == 0) vm.borrarLook(l.id) else vm.borrarDelHistorial(l.id)
        }, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.borrar), color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
      }
    }
  }
}

/** Las funciones Premium, siempre a la vista. Sin Premium, abren la pantalla para pasarse. */
@Composable
private fun Herramientas(premium: Boolean, abrir: (MotivoPremium) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (!premium) {
      Row(
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
          .clickable { abrir(MotivoPremium.GENERAL) }
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.WorkspacePremium, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
          Text(t(R.string.yo_hazte), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
          Text(t(R.string.yo_hazte_texto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
        }
      }
    }
    Text(t(R.string.yo_herramientas), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Herramienta(Icons.Filled.CalendarMonth, t(R.string.yo_semana), premium, Modifier.weight(1f)) { abrir(MotivoPremium.SEMANA) }
      Herramienta(Icons.Filled.Luggage, t(R.string.maleta_titulo), premium, Modifier.weight(1f)) { abrir(MotivoPremium.MALETA) }
      Herramienta(Icons.Filled.BarChart, t(R.string.stats_titulo), premium, Modifier.weight(1f)) { abrir(MotivoPremium.ESTADISTICAS) }
    }
  }
}

@Composable
private fun Herramienta(icono: ImageVector, texto: String, premium: Boolean, modifier: Modifier, alTocar: () -> Unit) {
  Box(modifier) {
    Column(
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .clickable(onClick = alTocar)
        .padding(vertical = 14.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
      Spacer(Modifier.height(6.dp))
      Text(texto, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
    }
    if (!premium) {
      Icon(
        Icons.Filled.WorkspacePremium,
        t(R.string.premium_etiqueta),
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
      )
    }
  }
}

/** Todo lo que se configura una vez: estilo, colores, maniquí, ciudad y privacidad. */
@Composable
fun PantallaAjustes(e: Estado, vm: AppViewModel, alCerrar: () -> Unit, abrirPremium: () -> Unit) {
  BackHandler(onBack = alCerrar)
  val perfil = e.datos.perfil
  val enlaces = LocalUriHandler.current
  val contexto = LocalContext.current
  val actividad = contexto as? Activity
  var pidiendoCiudad by remember { mutableStateOf(false) }
  var confirmarReinicio by remember { mutableStateOf(false) }
  val permisoAviso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> vm.cambiarAvisoDiario(ok) }

  Column(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = alCerrar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t(R.string.atras)) }
      Text(t(R.string.ajustes), style = MaterialTheme.typography.titleLarge)
    }
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).clickable(onClick = abrirPremium).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.WorkspacePremium, null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(10.dp))
        Text(t(if (e.premium) R.string.pw_ya_premium else R.string.yo_hazte), style = MaterialTheme.typography.titleMedium)
      }
      Seccion(t(R.string.aj_apariencia)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            Triple(Modo.SISTEMA, R.string.modo_sistema, Icons.Filled.BrightnessAuto),
            Triple(Modo.CLARO, R.string.modo_claro, Icons.Filled.LightMode),
            Triple(Modo.OSCURO, R.string.modo_oscuro, Icons.Filled.DarkMode),
          ).forEach { (m, texto, icono) ->
            FilterChip(
              selected = perfil.modo == m,
              onClick = { vm.cambiarPerfil { it.copy(modo = m) } },
              label = { Text(t(texto)) },
              leadingIcon = { Icon(icono, null, Modifier.size(18.dp)) },
            )
          }
        }
        Text(t(R.string.aj_paleta), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Paleta.entries.forEach { p ->
            MuestraPaleta(p, elegida = perfil.paleta == p) { vm.cambiarPerfil { it.copy(paleta = p) } }
          }
        }
      }
      HorizontalDivider()
      Seccion(t(R.string.aj_estilos)) {
        SelectorEstilos(perfil.estilos) { s -> vm.cambiarPerfil { it.copy(estilos = if (s in it.estilos) it.estilos - s else it.estilos + s) } }
      }
      HorizontalDivider()
      Seccion(t(R.string.aj_favoritos)) {
        SelectorColores(perfil.coloresFavoritos, { c ->
          vm.cambiarPerfil { it.copy(coloresFavoritos = alternar(it.coloresFavoritos, c), coloresEvitar = it.coloresEvitar - c) }
        })
      }
      Seccion(t(R.string.aj_evitar)) {
        SelectorColores(perfil.coloresEvitar, { c ->
          vm.cambiarPerfil { it.copy(coloresEvitar = alternar(it.coloresEvitar, c), coloresFavoritos = it.coloresFavoritos - c) }
        })
      }
      HorizontalDivider()
      Seccion(t(R.string.aj_maniqui)) { SelectorCuerpo(perfil.cuerpo) { c -> vm.cambiarPerfil { it.copy(cuerpo = c) } } }
      Seccion(t(R.string.aj_moda)) {
        var valor by remember(perfil.importanciaModa) { mutableStateOf(perfil.importanciaModa.toFloat()) }
        Slider(value = valor, onValueChange = { valor = it }, onValueChangeFinished = { vm.cambiarPerfil { it.copy(importanciaModa = valor.toDouble()) } })
        Text(
          when {
            valor < 0.3f -> t(R.string.aj_moda_0)
            valor < 0.7f -> t(R.string.aj_moda_1)
            else -> t(R.string.aj_moda_2)
          },
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Seccion(t(R.string.aj_vista)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(Vista.AUTO to t(R.string.vista_auto), Vista.COLLAGE to t(R.string.vista_collage), Vista.MANIQUI to t(R.string.vista_maniqui)).forEach { (v, texto) ->
            FilterChip(selected = perfil.vista == v, onClick = { vm.cambiarVista(v) }, label = { Text(texto) })
          }
        }
        Text(t(R.string.aj_vista_texto), style = MaterialTheme.typography.bodySmall)
      }
      HorizontalDivider()
      Seccion(t(R.string.aj_aviso)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(t(R.string.aj_aviso_texto), modifier = Modifier.weight(1f))
          Switch(checked = perfil.avisoDiario, onCheckedChange = { activar ->
            if (activar && !AvisoDiario.puedeAvisar(contexto) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              permisoAviso.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              vm.cambiarAvisoDiario(activar)
            }
          })
        }
      }
      if (perfil.avisoDiario) {
        Seccion(t(R.string.aj_hora)) {
          if (e.premium) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("06:30", "07:00", "07:30", "08:00", "08:30", "09:00", "09:30").forEach { h ->
                FilterChip(selected = perfil.horaAviso == h, onClick = { vm.cambiarHoraAviso(h) }, label = { Text(h) })
              }
            }
          } else {
            TextButton(onClick = { vm.pedirPremium(MotivoPremium.HORA_AVISO) }) {
              Icon(Icons.Filled.WorkspacePremium, null, Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.aj_hora_premium, perfil.horaAviso))
            }
          }
        }
      }
      HorizontalDivider()
      Seccion(t(R.string.aj_ciudad)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(perfil.ciudad ?: t(R.string.aj_sin_ciudad), modifier = Modifier.weight(1f))
          TextButton(onClick = { pidiendoCiudad = true }) { Text(t(R.string.hoy_cambiar)) }
        }
      }
      Seccion(t(R.string.aj_aprendizaje)) {
        Text(t(R.string.aj_aprendizaje_texto), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { confirmarReinicio = true }) { Text(t(R.string.aj_reiniciar)) }
      }
      HorizontalDivider()
      Seccion(t(R.string.aj_privacidad)) {
        Text(
          t(R.string.aj_privacidad_texto),
          style = MaterialTheme.typography.bodySmall,
        )
        Row {
          TextButton(onClick = { enlaces.openUri(URL_PRIVACIDAD) }) { Text(t(R.string.aj_politica)) }
          if (Anuncios.opcionesPrivacidad && actividad != null) {
            TextButton(onClick = { Anuncios.mostrarOpcionesPrivacidad(actividad) }) { Text(t(R.string.aj_anuncios)) }
          }
        }
      }
      Text(
        stringResource(R.string.aj_version, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(24.dp))
    }
  }

  if (pidiendoCiudad) DialogoCiudad(alCerrar = { pidiendoCiudad = false }) { vm.buscarCiudad(it) }
  if (confirmarReinicio) {
    AlertDialog(
      onDismissRequest = { confirmarReinicio = false },
      title = { Text(t(R.string.reiniciar_titulo)) },
      text = { Text(t(R.string.reiniciar_texto)) },
      confirmButton = {
        TextButton(onClick = {
          confirmarReinicio = false
          vm.reiniciarAprendizaje()
        }) { Text(t(R.string.borrar)) }
      },
      dismissButton = { TextButton(onClick = { confirmarReinicio = false }) { Text(t(R.string.cancelar)) } },
    )
  }
}

internal fun alternar(s: Set<String>, c: String) = if (c in s) s - c else s + c

@Composable
fun DialogoCiudad(alCerrar: () -> Unit, alBuscar: (String) -> Unit) {
  var texto by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = alCerrar,
    title = { Text(t(R.string.ciudad_titulo)) },
    text = {
      androidx.compose.material3.OutlinedTextField(value = texto, onValueChange = { texto = it }, singleLine = true, label = { Text(t(R.string.ciudad_campo)) })
    },
    confirmButton = {
      TextButton(onClick = { alBuscar(texto); alCerrar() }, enabled = texto.isNotBlank()) { Text(t(R.string.buscar)) }
    },
    dismissButton = { TextButton(onClick = alCerrar) { Text(t(R.string.cancelar)) } },
  )
}

@StringRes
private fun Paleta.res(): Int = when (this) {
  Paleta.CORAL -> R.string.paleta_coral
  Paleta.ROSA -> R.string.paleta_rosa
  Paleta.LAVANDA -> R.string.paleta_lavanda
  Paleta.OCEANO -> R.string.paleta_oceano
  Paleta.TERRACOTA -> R.string.paleta_terracota
  Paleta.MONOCROMO -> R.string.paleta_monocromo
}

/** Un círculo con los dos tonos de la paleta y su nombre. */
@Composable
private fun MuestraPaleta(p: Paleta, elegida: Boolean, alTocar: () -> Unit) {
  val esquema = MaterialTheme.colorScheme
  val nombre = t(p.res())
  Column(
    Modifier.width(66.dp).clip(RoundedCornerShape(14.dp)).selectable(selected = elegida, onClick = alTocar, role = Role.RadioButton).padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      Modifier
        .size(46.dp)
        .border(if (elegida) 3.dp else 1.dp, if (elegida) esquema.onSurface else esquema.outlineVariant, CircleShape)
        .padding(if (elegida) 5.dp else 2.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(p.muestra(LocalOscuro.current))),
      contentAlignment = Alignment.Center,
    ) {
      if (elegida) Icon(Icons.Filled.Check, null, tint = if (LocalOscuro.current) Color.Black else Color.White, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.height(4.dp))
    Text(nombre, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
  }
}
