@file:OptIn(ExperimentalLayoutApi::class)

package com.ancylostoma.quemepongo.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.PlanPremium
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.roundToInt

/** Precio del plan anual dividido en 12, en la moneda del usuario. */
private fun precioPorMes(p: PlanPremium): String = runCatching {
  NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(p.moneda) }.format(p.micros / 12_000_000.0)
}.getOrDefault(p.precio)

/**
 * La pantalla de Premium. Se abre solo en el momento justo (al llegar al
 * límite gratis o al tocar una función Premium), nunca al abrir la app.
 */
@Composable
fun PantallaPremium(e: Estado, vm: AppViewModel, motivo: MotivoPremium, alCerrar: () -> Unit) {
  BackHandler(onBack = alCerrar)
  val actividad = LocalContext.current as? Activity
  val enlaces = LocalUriHandler.current
  val anual = e.planes.firstOrNull { it.anual }
  val mensual = e.planes.firstOrNull { !it.anual }
  var elegido by remember(e.planes) { mutableStateOf(anual ?: mensual) }
  // Si se compra (o se activa el modo prueba) mientras está abierta, se cierra sola.
  val eraPremium = remember { e.premium }
  LaunchedEffect(e.premium) { if (e.premium && !eraPremium) alCerrar() }

  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
    Box(
      Modifier
        .fillMaxWidth()
        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
        .padding(bottom = 28.dp),
    ) {
      IconButton(onClick = alCerrar, modifier = Modifier.align(Alignment.TopEnd)) {
        Icon(Icons.Filled.Close, t(R.string.cerrar), tint = Color.White)
      }
      Column(Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
          Icon(Icons.Filled.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(t(R.string.premium_nombre).uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Text(t(R.string.pw_titulo), style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
        Text(t(R.string.pw_sub), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
      }
    }

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      textoMotivo(motivo)?.let { m ->
        Text(
          m,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(14.dp),
        )
      }

      if (e.premium) {
        Text(t(R.string.pw_ya_premium), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(
          onClick = { enlaces.openUri("https://play.google.com/store/account/subscriptions") },
          modifier = Modifier.fillMaxWidth(),
        ) { Text(t(R.string.pw_gestionar)) }
        if (vm.modoPrueba && vm.pruebaActiva) {
          TextButton(onClick = { vm.cambiarPrueba(false) }) { Text(t(R.string.pw_modo_prueba_off)) }
        }
        return@Column
      }

      Beneficio(Icons.Filled.NotInterested, t(R.string.pw_b_anuncios))
      Beneficio(Icons.Filled.Checkroom, t(R.string.pw_b_armario))
      Beneficio(Icons.Filled.CalendarMonth, t(R.string.pw_b_semana))
      Beneficio(Icons.Filled.Luggage, t(R.string.pw_b_maleta))
      Beneficio(Icons.Filled.BarChart, t(R.string.pw_b_stats))
      Beneficio(Icons.Filled.AccessTime, t(R.string.pw_b_hora))

      if (e.planes.isEmpty()) {
        Text(t(R.string.pw_no_disponible), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else {
        val ahorro = if (anual != null && mensual != null && mensual.micros > 0) {
          (100 * (1 - anual.micros.toDouble() / (mensual.micros * 12))).roundToInt()
        } else {
          0
        }
        anual?.let { a ->
          TarjetaPlan(
            titulo = t(R.string.pw_anual),
            precio = stringResource(R.string.pw_por_anio, a.precio),
            detalle = stringResource(R.string.pw_por_mes, precioPorMes(a)),
            insignia = if (ahorro > 0) stringResource(R.string.pw_ahorra, ahorro) else null,
            elegido = elegido == a,
          ) { elegido = a }
        }
        mensual?.let { m ->
          TarjetaPlan(t(R.string.pw_mensual), stringResource(R.string.pw_por_mes, m.precio), null, null, elegido == m) { elegido = m }
        }
        val plan = elegido
        if (plan != null && plan.diasPrueba > 0) {
          Text(
            stringResource(R.string.pw_prueba, plan.diasPrueba, stringResource(if (plan.anual) R.string.pw_por_anio else R.string.pw_por_mes, plan.precio)),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Button(
          onClick = { if (actividad != null && plan != null) vm.comprar(actividad, plan) },
          enabled = plan != null,
          modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
          Text(t(if ((plan?.diasPrueba ?: 0) > 0) R.string.pw_empezar_prueba else R.string.pw_suscribirme), style = MaterialTheme.typography.titleMedium)
        }
      }
      TextButton(onClick = { vm.restaurarCompras() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(t(R.string.pw_restaurar)) }
      Text(t(R.string.pw_legal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
      if (vm.modoPrueba) {
        OutlinedButton(onClick = { vm.cambiarPrueba(true) }, modifier = Modifier.fillMaxWidth()) { Text(t(R.string.pw_modo_prueba)) }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun textoMotivo(m: MotivoPremium): String? = when (m) {
  MotivoPremium.GENERAL -> null
  MotivoPremium.PRENDAS -> stringResource(R.string.pw_motivo_prendas, AppViewModel.LIMITE_PRENDAS)
  MotivoPremium.LOOKS -> stringResource(R.string.pw_motivo_looks, AppViewModel.LIMITE_LOOKS)
  MotivoPremium.SEMANA -> t(R.string.pw_motivo_semana)
  MotivoPremium.MALETA -> t(R.string.pw_motivo_maleta)
  MotivoPremium.ESTADISTICAS -> t(R.string.pw_motivo_stats)
  MotivoPremium.HORA_AVISO -> t(R.string.pw_motivo_hora)
}

@Composable
private fun Beneficio(icono: ImageVector, texto: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
      Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.width(14.dp))
    Text(texto, style = MaterialTheme.typography.bodyLarge)
  }
}

@Composable
private fun TarjetaPlan(titulo: String, precio: String, detalle: String?, insignia: String?, elegido: Boolean, alTocar: () -> Unit) {
  val forma = RoundedCornerShape(18.dp)
  Box {
    Row(
      Modifier
        .fillMaxWidth()
        .clip(forma)
        .background(if (elegido) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
        .border(BorderStroke(if (elegido) 2.dp else 1.dp, if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), forma)
        .clickable(onClick = alTocar)
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        Modifier
          .size(22.dp)
          .clip(CircleShape)
          .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
          .padding(4.dp)
          .clip(CircleShape)
          .background(if (elegido) MaterialTheme.colorScheme.primary else Color.Transparent),
      )
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(precio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (insignia != null) {
      Text(
        insignia,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(y = (-10).dp)
          .padding(end = 14.dp)
          .clip(RoundedCornerShape(50))
          .background(MaterialTheme.colorScheme.secondary)
          .padding(horizontal = 10.dp, vertical = 3.dp),
      )
    }
  }
}
