package com.ancylostoma.quemepongo.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ancylostoma.quemepongo.MainActivity
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.motor.Contexto
import com.ancylostoma.quemepongo.motor.Motor
import com.ancylostoma.quemepongo.motor.Tendencias
import com.ancylostoma.quemepongo.ui.nombre
import com.ancylostoma.quemepongo.ui.temperatura
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import java.util.Locale
import kotlin.random.Random

/** El aviso de cada mañana: "Tu look de hoy está listo", con el tiempo y las prendas. */
object AvisoDiario {
  private const val CANAL = "look-del-dia"
  private const val TRABAJO = "aviso-diario"
  /** [hora]: "HH:mm"; con el plan gratis siempre 07:30. */
  fun programar(contexto: Context, activo: Boolean, hora: String = "07:30") {
    val wm = WorkManager.getInstance(contexto)
    if (!activo) {
      wm.cancelUniqueWork(TRABAJO)
      return
    }
    val ahora = LocalDateTime.now()
    val a = runCatching { LocalTime.parse(hora) }.getOrDefault(LocalTime.of(7, 30))
    var proxima = ahora.toLocalDate().atTime(a)
    if (!proxima.isAfter(ahora)) proxima = proxima.plusDays(1)
    // Cada teléfono a un minuto distinto en los 20 minutos siguientes a la hora elegida:
    // así miles de usuarios no piden el tiempo en el mismo segundo.
    val reparto = Random.nextLong(20 * 60_000L)
    val pedido = PeriodicWorkRequestBuilder<TrabajoAvisoDiario>(1, TimeUnit.DAYS)
      .setInitialDelay(Duration.between(ahora, proxima).toMillis() + reparto, TimeUnit.MILLISECONDS)
      .build()
    wm.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.UPDATE, pedido)
  }

  fun puedeAvisar(contexto: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

  @SuppressLint("MissingPermission")
  fun mostrar(contexto: Context, titulo: String, texto: String) {
    if (!puedeAvisar(contexto)) return
    val nm = contexto.getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel(CANAL, contexto.getString(R.string.aviso_canal), NotificationManager.IMPORTANCE_DEFAULT))
    val abrir = PendingIntent.getActivity(
      contexto,
      0,
      Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val aviso = NotificationCompat.Builder(contexto, CANAL)
      .setSmallIcon(R.drawable.ic_notificacion)
      .setColor(0xFF7A4B36.toInt())
      .setContentTitle(titulo)
      .setContentText(texto)
      .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
      .setContentIntent(abrir)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(contexto).notify(1, aviso)
  }
}

class TrabajoAvisoDiario(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
  override suspend fun doWork(): Result {
    val datos = Almacen(applicationContext).cargar()
    if (!datos.perfil.avisoDiario) return Result.success()
    val texto = runCatching { resumen(datos) }.getOrNull() ?: applicationContext.getString(R.string.aviso_generico)
    AvisoDiario.mostrar(applicationContext, applicationContext.getString(R.string.aviso_titulo), texto)
    return Result.success()
  }

  /** "14° y lluvia: camisa + jeans + gabardina". */
  private suspend fun resumen(d: Datos): String {
    val lat = d.perfil.latitud
    val lon = d.perfil.longitud
    val clima = if (lat != null && lon != null) {
      runCatching { ServicioClima.pronostico(lat, lon, d.perfil.ciudad ?: "", applicationContext.cacheDir) }.getOrNull()
    } else {
      null
    }
    val condicion = clima?.para(Momento.MANANA)
    val tendencias = Tendencias.activas(ServicioTendencias(applicationContext).local().tendencias, LocalDate.now(), lat)
    val ctx = Contexto(condicion, Ocasion.CASUAL, d.perfil, d.preferencias, tendencias, System.currentTimeMillis())
    val look = Motor.sugerir(d.prendas, ctx, cantidad = 1).firstOrNull()
    val piezas = look?.prendas
      ?.filter { it.categoria != Categoria.ACCESORIO }
      ?.joinToString(" + ") { applicationContext.nombre(it).lowercase(Locale.getDefault()) }
      ?: return applicationContext.getString(R.string.aviso_generico)
    if (condicion == null) return piezas.replaceFirstChar { it.uppercase() }
    val temp = temperatura(condicion.temperatura)
    return applicationContext.getString(if (condicion.lluvia) R.string.aviso_lluvia else R.string.aviso_tiempo, temp, piezas)
  }
}
