package com.ancylostoma.quemepongo.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.ancylostoma.quemepongo.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Un plan de la suscripción tal como lo configuraste en Play Console. */
data class PlanPremium(
  /** "anual" o "mensual" (el id del plan base en Play Console). */
  val id: String,
  /** Precio ya formateado en la moneda del usuario, por ejemplo "19,99 €". */
  val precio: String,
  val micros: Long,
  /** Código de la moneda, por ejemplo "EUR". */
  val moneda: String,
  /** Duración en formato ISO: "P1Y", "P1M". */
  val periodo: String,
  /** Días de prueba gratis (0 si no tiene). */
  val diasPrueba: Int,
  val token: String,
  val detalles: ProductDetails,
) {
  val anual get() = periodo == "P1Y"
}

/**
 * Suscripción Premium con Google Play Billing. La compra la hace Google: la app
 * solo pregunta qué planes hay, abre la pantalla de pago de Play y comprueba si
 * el usuario tiene la suscripción activa.
 *
 * En Play Console: Monetizar → Productos → Suscripciones → crear "premium" con
 * los planes base "mensual" (1 mes) y "anual" (1 año), y si quieres una oferta
 * de prueba gratis de 7 días en el anual.
 */
class Facturacion(contexto: Context) : PurchasesUpdatedListener {
  private val preferencias = contexto.getSharedPreferences("premium", Context.MODE_PRIVATE)
  private val cliente = BillingClient.newBuilder(contexto)
    .setListener(this)
    .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
    .build()

  // El último estado conocido, para no esperar a Google al abrir la app (o sin internet).
  private val _comprado = MutableStateFlow(preferencias.getBoolean("activo", false))
  private val _prueba = MutableStateFlow(MODO_PRUEBA && preferencias.getBoolean("prueba", false))
  private val _planes = MutableStateFlow<List<PlanPremium>>(emptyList())
  val comprado: StateFlow<Boolean> = _comprado
  val prueba: StateFlow<Boolean> = _prueba
  val planes: StateFlow<List<PlanPremium>> = _planes

  fun conectar() {
    // En las pruebas de Robolectric no hay Google Play.
    if (android.os.Build.FINGERPRINT == "robolectric") return
    if (cliente.isReady) {
      consultar()
      return
    }
    cliente.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(resultado: BillingResult) {
        if (resultado.responseCode == BillingClient.BillingResponseCode.OK) consultar()
      }

      // Se vuelve a conectar la próxima vez que haga falta (abrir la app o comprar).
      override fun onBillingServiceDisconnected() {}
    })
  }

  private fun consultar() {
    cliente.queryPurchasesAsync(
      QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
    ) { resultado, compras ->
      if (resultado.responseCode == BillingClient.BillingResponseCode.OK) procesar(compras)
    }
    val pedido = QueryProductDetailsParams.newBuilder()
      .setProductList(
        listOf(
          QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCTO)
            .setProductType(BillingClient.ProductType.SUBS)
            .build(),
        ),
      )
      .build()
    cliente.queryProductDetailsAsync(pedido) { resultado, respuesta ->
      if (resultado.responseCode == BillingClient.BillingResponseCode.OK) {
        _planes.value = respuesta.productDetailsList.flatMap { planesDe(it) }.sortedByDescending { it.anual }
      }
    }
  }

  /** De cada plan base, la oferta con prueba gratis si la hay; si no, el precio normal. */
  private fun planesDe(p: ProductDetails): List<PlanPremium> =
    p.subscriptionOfferDetails.orEmpty()
      .groupBy { it.basePlanId }
      .mapNotNull { (id, ofertas) ->
        val conPrueba = ofertas.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
        val oferta = conPrueba ?: ofertas.firstOrNull { it.offerId == null } ?: ofertas.first()
        val fases = oferta.pricingPhases.pricingPhaseList
        val normal = fases.lastOrNull() ?: return@mapNotNull null
        val gratis = fases.firstOrNull { it.priceAmountMicros == 0L }
        PlanPremium(id, normal.formattedPrice, normal.priceAmountMicros, normal.priceCurrencyCode, normal.billingPeriod, dias(gratis?.billingPeriod), oferta.offerToken, p)
      }

  /** "P7D" → 7, "P1W" → 7, "P1M" → 30. */
  private fun dias(periodo: String?): Int {
    val m = Regex("P(\\d+)([DWM])").matchEntire(periodo ?: return 0) ?: return 0
    val n = m.groupValues[1].toInt()
    return when (m.groupValues[2]) {
      "W" -> n * 7
      "M" -> n * 30
      else -> n
    }
  }

  fun comprar(actividad: Activity, plan: PlanPremium) {
    val parametros = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(
        listOf(
          BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.detalles)
            .setOfferToken(plan.token)
            .build(),
        ),
      )
      .build()
    cliente.launchBillingFlow(actividad, parametros)
  }

  override fun onPurchasesUpdated(resultado: BillingResult, compras: MutableList<Purchase>?) {
    if (resultado.responseCode == BillingClient.BillingResponseCode.OK && compras != null) procesar(compras)
  }

  private fun procesar(compras: List<Purchase>) {
    val activas = compras.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && PRODUCTO in it.products }
    // Google devuelve el dinero si una compra no se confirma en 3 días.
    activas.filter { !it.isAcknowledged }.forEach { c ->
      cliente.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(c.purchaseToken).build()) {}
    }
    val activo = activas.isNotEmpty()
    _comprado.value = activo
    preferencias.edit().putBoolean("activo", activo).apply()
  }

  /** Solo en las compilaciones de prueba (sin llave de Play): activar Premium sin pagar para probarlo. */
  fun cambiarPrueba(activo: Boolean) {
    if (!MODO_PRUEBA) return
    _prueba.value = activo
    preferencias.edit().putBoolean("prueba", activo).apply()
  }

  companion object {
    const val PRODUCTO = "premium"
    val MODO_PRUEBA = BuildConfig.MODO_PRUEBA
  }
}
