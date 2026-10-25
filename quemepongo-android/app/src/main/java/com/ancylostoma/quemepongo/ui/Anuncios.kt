package com.ancylostoma.quemepongo.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.ancylostoma.quemepongo.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob con el formulario de consentimiento de Google (obligatorio en Europa y
 * Reino Unido). Hasta que el usuario responda, no se piden anuncios.
 */
object Anuncios {
  var listos by mutableStateOf(false)
    private set
  var opcionesPrivacidad by mutableStateOf(false)
    private set

  private val iniciado = AtomicBoolean(false)
  private lateinit var consentimiento: ConsentInformation

  fun preparar(actividad: Activity) {
    // En las pruebas de Robolectric no hay servicios de Google.
    if (Build.FINGERPRINT == "robolectric") return
    consentimiento = UserMessagingPlatform.getConsentInformation(actividad)
    consentimiento.requestConsentInfoUpdate(
      actividad,
      ConsentRequestParameters.Builder().build(),
      {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(actividad) { _ ->
          opcionesPrivacidad = consentimiento.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
          if (consentimiento.canRequestAds()) iniciar(actividad)
        }
      },
      { _ -> if (consentimiento.canRequestAds()) iniciar(actividad) },
    )
    // Si ya respondió en otra sesión, no hay que esperar.
    if (consentimiento.canRequestAds()) iniciar(actividad)
  }

  fun mostrarOpcionesPrivacidad(actividad: Activity) {
    UserMessagingPlatform.showPrivacyOptionsForm(actividad) { _ -> }
  }

  private fun iniciar(actividad: Activity) {
    if (!iniciado.compareAndSet(false, true)) return
    val app = actividad.applicationContext
    Thread { MobileAds.initialize(app) { listos = true } }.start()
  }
}

@Composable
fun BannerAnuncio(modifier: Modifier = Modifier) {
  if (!Anuncios.listos) return
  val ancho = LocalConfiguration.current.screenWidthDp
  AndroidView(
    modifier = modifier.fillMaxWidth(),
    factory = { ctx ->
      AdView(ctx).apply {
        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, ancho))
        adUnitId = BuildConfig.ADMOB_BANNER
        loadAd(AdRequest.Builder().build())
      }
    },
    onRelease = { it.destroy() },
  )
}
