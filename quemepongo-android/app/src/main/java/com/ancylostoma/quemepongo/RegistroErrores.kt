package com.ancylostoma.quemepongo

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File

/**
 * Si la app se cierra por un error, lo guarda en un archivo para mostrarlo la
 * próxima vez que se abra. Así se puede copiar y corregir sin conectar el
 * teléfono a una computadora.
 */
object RegistroErrores {
  private const val ARCHIVO = "ultimo_error.txt"

  fun instalar(contexto: Context) {
    val archivo = File(contexto.filesDir, ARCHIVO)
    val anterior = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
      runCatching {
        archivo.writeText(
          "Qué Me Pongo ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" +
            " · ${Build.MANUFACTURER} ${Build.MODEL}\nHilo: ${hilo.name}\n\n${error.stackTraceToString()}",
        )
      }
      anterior?.uncaughtException(hilo, error)
    }
  }

  fun leer(contexto: Context): String? =
    File(contexto.filesDir, ARCHIVO).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

  fun borrar(contexto: Context) {
    File(contexto.filesDir, ARCHIVO).delete()
  }
}

class AppQueMePongo : Application() {
  override fun onCreate() {
    super.onCreate()
    RegistroErrores.instalar(this)
  }
}
