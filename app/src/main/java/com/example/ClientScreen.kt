package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CatalogItem
import com.example.data.CatalogVariant
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

/*
 * Pantalla del cliente: portada, ofertas, ficha del paquete, oferta propia y
 * calendario, con la barra del pedido abajo y el botón Atrás por secciones.
 */

// ==========================================
// CLIENT SCREEN WITH 3-TAB NAVIGATION
// ==========================================
@Composable
fun ClientScreen(
    viewModel: StudioViewModel,
    onOpenAdminRequest: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.catalogItems.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val descuento by viewModel.discount.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()
    val contractText by viewModel.contractText.collectAsState()
    val studioConfig by viewModel.studioConfig.collectAsState()
    val reserva by viewModel.reserva.collectAsState()
    val serviciosExtras by viewModel.serviciosExtras.collectAsState()

    // Vista activa dentro de los dos tercios inferiores. La banda de marca de
    // arriba no cambia nunca.
    var vista by rememberSaveable { mutableStateOf(ClientView.INICIO) }
    var categoriaActiva by rememberSaveable { mutableStateOf("Bodas") }
    var itemDetalleId by rememberSaveable { mutableStateOf(-1) }

    var showSummaryDialog by remember { mutableStateOf(false) }
    var showContractForOrder by remember { mutableStateOf(false) }
    var pedidoConfirmado by remember { mutableStateOf<ContratoFirmado?>(null) }
    var itemForExtrasDialog by remember { mutableStateOf<CatalogItem?>(null) }
    var mostrarResumenDelDia by remember { mutableStateOf(false) }

    val itemDetalle = remember(items, itemDetalleId) {
        items.firstOrNull { it.id == itemDetalleId }
    }

    val ofertasCategoria = remember(items, categoriaActiva) {
        items.filter { it.category == categoriaActiva }
    }

    val cupLabelFor: (Double) -> String? = remember(studioConfig) {
        { usd -> viewModel.cupLabel(usd) }
    }

    fun volverAInicio() {
        vista = ClientView.INICIO
        itemDetalleId = -1
        // Volver a la portada abandona el recorrido del calendario. Lo elegido
        // se conserva, pero la marca de "vengo del calendario" se apaga para
        // que el botón del paquete no prometa una vuelta que ya no toca.
        if (reserva.vinoDelCalendario) {
            viewModel.actualizarReserva { it.copy(vinoDelCalendario = false) }
        }
    }

    // Botón Atrás de Android: vuelve a la sección de antes, no cierra la app.
    // Se apunta cada sección por la que se pasa; al volver a la portada el
    // camino se olvida, y solo desde la portada Atrás sale de la app.
    val historial = remember { mutableStateListOf<ClientView>() }
    var vistaAnterior by remember { mutableStateOf(vista) }
    var volviendoAtras by remember { mutableStateOf(false) }
    LaunchedEffect(vista) {
        if (vista != vistaAnterior) {
            if (!volviendoAtras) historial.add(vistaAnterior)
            volviendoAtras = false
            vistaAnterior = vista
        }
        if (vista == ClientView.INICIO) historial.clear()
    }
    BackHandler(enabled = vista != ClientView.INICIO) {
        // Un paquete que se borró desde el panel ya no tiene ficha a la que
        // volver: se salta, para no quedar rebotando entre dos pantallas.
        while (historial.isNotEmpty() && historial.last() == ClientView.DETALLE && itemDetalle == null) {
            historial.removeAt(historial.lastIndex)
        }
        val previa = if (historial.isEmpty()) ClientView.INICIO
        else historial.removeAt(historial.lastIndex)
        volviendoAtras = true
        if (previa == ClientView.INICIO) volverAInicio() else vista = previa
    }

    // En la portada no hace falta ofrecer "ir a la portada".
    val irAInicio: (() -> Unit)? =
        if (vista == ClientView.INICIO) null else { { volverAInicio() } }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- TERCIO SUPERIOR: fijo, siempre visible ----
        StudioHeaderBand(
            onOpenAdminRequest = onOpenAdminRequest,
            config = studioConfig,
            // En la portada se muestra grande; dentro de una sección se
            // compacta para dejar sitio al contenido.
            compacto = vista != ClientView.INICIO,
            // Antes vivía abajo a la izquierda y desaparecía en el detalle del
            // paquete. Ahora está siempre en el mismo sitio, arriba a la
            // izquierda, en todas las secciones.
            onInicio = irAInicio
        )

        // ---- DOS TERCIOS INFERIORES: cambian según la vista ----
        Box(modifier = Modifier.weight(1f)) {
            // Las secciones entran deslizándose desde el lado por el que se
            // avanza y salen hacia el contrario, así el cliente entiende de un
            // vistazo si está entrando o volviendo.
            AnimatedContent(
                targetState = vista,
                transitionSpec = {
                    val avanza = targetState.ordinal > initialState.ordinal
                    val desplazamiento = if (avanza) 1 else -1
                    (
                        slideInHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) { ancho -> desplazamiento * ancho / 6 } +
                            fadeIn(animationSpec = tween(240))
                        ) togetherWith (
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) { ancho -> -desplazamiento * ancho / 10 } +
                            fadeOut(animationSpec = tween(160))
                        )
                },
                label = "vista_cliente"
            ) { vistaActual ->
            when (vistaActual) {
                ClientView.INICIO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        HomeContent(
                            onCategoria = { cat ->
                                categoriaActiva = cat
                                vista = ClientView.OFERTAS
                            },
                            onOfertaPropia = { vista = ClientView.PROPIA },
                            onCalendario = { vista = ClientView.CALENDARIO },
                            config = studioConfig,
                            onResumenDelDia = { mostrarResumenDelDia = true }
                        )
                    }
                }

                ClientView.OFERTAS -> {
                    OffersListScreen(
                        categoria = categoriaActiva,
                        items = ofertasCategoria,
                        cupLabelFor = cupLabelFor,
                        onAbrirOferta = { item ->
                            itemDetalleId = item.id
                            vista = ClientView.DETALLE
                        },
                        onCambiarCategoria = { categoriaActiva = it }
                    )
                }

                ClientView.DETALLE -> {
                    val detalle = itemDetalle
                    if (detalle == null) {
                        // El paquete pudo borrarse desde el panel admin.
                        LaunchedEffect(Unit) { vista = ClientView.OFERTAS }
                    } else {
                        OfferDetailScreen(
                            item = detalle,
                            cupLabelFor = cupLabelFor,
                            onAddToCart = { it0, variant, qty ->
                                viewModel.addToCart(it0, variant, qty)
                            },
                            onOpenExtras = { itemForExtrasDialog = detalle },
                            onVolver = { vista = ClientView.OFERTAS },
                            // Los dos caminos acaban en el mismo sitio: quien
                            // empieza por el paquete pasa ahora a poner la
                            // fecha, y quien empezó por el calendario vuelve
                            // allí con su día ya elegido.
                            onFinalizar = { vista = ClientView.CALENDARIO },
                            vinoDelCalendario = reserva.vinoDelCalendario,
                            incluidoSiempre = studioConfig.incluidoSiempre,
                            medidasEnPulgadas = studioConfig.medidasEnPulgadas
                        )
                    }
                }

                ClientView.PROPIA -> {
                    CustomOfferScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ClientView.CALENDARIO -> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onElegirDelCatalogo = { vista = ClientView.OFERTAS },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            }

            // El regreso a la portada vive ahora en la banda de marca, arriba
            // a la izquierda, para que esté siempre en el mismo sitio.
        }

        // ---- BARRA DEL PEDIDO: solo aparece si hay algo en el carrito ----
        // El carrito ya no ocupa un panel propio; vive aquí abajo y se abre
        // completo al confirmar.
        if (cart.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSummaryDialog = true }
                    .testTag("view_summary_button"),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cartCount.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Tu pedido",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "$${String.format("%.2f", cartTotal)} USD",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            // El mismo total en la moneda del día, con la tasa
                            // que tenga puesta el estudio.
                            cupLabelFor(cartTotal)?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Confirmar",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }

    // Extras del paquete abierto
    itemForExtrasDialog?.let { item ->
        OfferExtrasDialog(
            item = item,
            carpetas = serviciosExtras,
            enPulgadas = studioConfig.medidasEnPulgadas,
            totalPedido = cartTotal,
            cupLabelFor = cupLabelFor,
            // La cuenta sale del carrito, no de un estado aparte del diálogo:
            // así lo que marca la ficha y lo que hay en el pedido no se
            // pueden desfasar.
            cantidadEnPedido = { extra ->
                cart.firstOrNull {
                    it.item.name == extra.title &&
                        it.variant.name == extra.variantName &&
                        it.item.category == extra.category
                }?.quantity ?: 0
            },
            onAgregar = { extra ->
                viewModel.addCustomToCart(
                    extra.title,
                    extra.category,
                    extra.variantName,
                    extra.price,
                    1,
                    "Extra añadido a ${item.name}"
                )
            },
            onQuitar = { extra ->
                viewModel.quitarUnoDelCarrito(extra.title, extra.category, extra.variantName)
            },
            onDismiss = { itemForExtrasDialog = null }
        )
    }

    // Resumen del día, para quien atiende el estudio (sin PIN, solo lectura)
    if (mostrarResumenDelDia) {
        ResumenDelDiaDialog(
            viewModel = viewModel,
            onCerrar = { mostrarResumenDelDia = false }
        )
    }

    // Resumen del pedido, ya al final del recorrido
    if (showSummaryDialog) {
        SummaryDialog(
            viewModel = viewModel,
            cart = cart,
            total = cartTotal,
            subtotal = cartSubtotal,
            descuento = descuento,
            onDiscountChange = { viewModel.setDiscount(it) },
            cupLabelFor = cupLabelFor,
            onDismiss = { showSummaryDialog = false },
            onProceedToContract = {
                showSummaryDialog = false
                showContractForOrder = true
            },
            onBorrarPedido = {
                viewModel.cancelarPedido()
                showSummaryDialog = false
                Toast.makeText(context, "Pedido borrado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Contrato y firma antes de enviar el pedido
    if (showContractForOrder) {
        ContractSignatureDialog(
            title = "Contrato de Sesión - Confirmación de Pedido",
            contractText = contractText,
            pedirDatosCliente = true,
            onDismiss = { showContractForOrder = false },
            onConfirm = { firmado ->
                showContractForOrder = false
                pedidoConfirmado = firmado

                // Firmar el contrato no era el final del recorrido: faltaba lo
                // más importante, que es el día de la sesión. El contrato
                // firmado y sus dos fotos quedan guardados, se pasan a la
                // reserva los datos que el cliente ya escribió, y se sigue al
                // calendario, que es donde termina de verdad.
                viewModel.guardarContratoPendiente(firmado)
                viewModel.actualizarReserva {
                    it.copy(
                        nombre = it.nombre.ifBlank { firmado.nombreCliente },
                        telefono = it.telefono.ifBlank { firmado.telefonoCliente }
                    )
                }

                val uriString = viewModel.generateWhatsAppUri(
                    clientName = firmado.nombreCliente,
                    clientPhone = firmado.telefonoCliente
                )
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir WhatsApp: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }

                vista = ClientView.CALENDARIO
            }
        )
    }

    // WhatsApp solo abre un chat a la vez: la copia para el cliente se envía
    // en un segundo toque, al volver del chat del estudio.
    pedidoConfirmado?.let { firmado ->
        AlertDialog(
            onDismissRequest = { pedidoConfirmado = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Contrato firmado") },
            text = {
                Text(
                    "El pedido de ${firmado.nombreCliente} ya salió hacia el WhatsApp de " +
                        "FXestudio.\n\nAl cerrar este aviso queda el calendario abierto: " +
                        "solo falta elegir el día y la hora para terminar la reservación.\n\n" +
                        "¿Desea enviarle también su copia al ${firmado.telefonoCliente}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uriString = viewModel.generateWhatsAppUri(
                            clientName = firmado.nombreCliente,
                            clientPhone = firmado.telefonoCliente,
                            enviarACliente = true
                        )
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "No se pudo abrir el WhatsApp del cliente",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        pedidoConfirmado = null
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar copia al cliente")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pedidoConfirmado = null }) {
                    Text("Ahora no")
                }
            }
        )
    }
}


@Composable
fun ClientHeader(onOpenAdminRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // weight(1f) cede el espacio sobrante al botón de admin: sin él, el
        // subtítulo largo ocupa todo el ancho y empuja el candado fuera de pantalla.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = "FXestudio",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Bayamo, Granma • Estudio Fotográfico Profesional",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onOpenAdminRequest,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .size(48.dp)
                .testTag("admin_mode_button")
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Modo Administrador",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
@Composable
fun CatalogItemImage(
    imageBytes: ByteArray?,
    category: String = "",
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imageBytes) {
        imageBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    // Foto de ejemplo por categoría, tomada del catálogo impreso. Se usa solo
    // cuando el paquete no tiene una foto propia cargada desde el panel admin,
    // así se evita guardar la misma imagen decenas de veces en la base de datos.
    val fallbackRes = remember(category) {
        when {
            category.contains("Primer Año", ignoreCase = true) -> R.drawable.cat_primer_ano
            category.contains("Bodas", ignoreCase = true) -> R.drawable.cat_bodas
            category.contains("15", ignoreCase = true) ||
                category.contains("quince", ignoreCase = true) -> R.drawable.cat_quince
            else -> null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Foto de muestra",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (fallbackRes != null) {
        Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = "Foto de muestra de $category",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "FXESTUDIO BAYAMO",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
