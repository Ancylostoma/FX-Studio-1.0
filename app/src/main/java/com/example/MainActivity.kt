package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import java.io.InputStream

enum class Screen {
    CLIENT,
    ADMIN
}

/**
 * Vistas que ocupan los dos tercios inferiores. La banda de marca superior
 * permanece fija en todas ellas.
 */
enum class ClientView {
    INICIO,      // foto, los cinco accesos y la ficha de contacto
    OFERTAS,     // lista de paquetes de una categoría
    DETALLE,     // un paquete concreto, a pantalla completa
    PROPIA,      // diseñar la oferta a la medida
    CALENDARIO   // disponibilidad y reserva
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val viewModel: StudioViewModel = viewModel()

    var currentScreen by remember { mutableStateOf(Screen.CLIENT) }
    var showPinDialog by remember { mutableStateOf(false) }

    val licenseChecked by viewModel.licenseChecked.collectAsState()
    val licenseValid by viewModel.licenseValid.collectAsState()

    // La app entera queda bloqueada hasta que se verifica la licencia del mes.
    if (!licenseChecked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (!licenseValid) {
        LicenseActivationScreen(viewModel = viewModel)
        return
    }

    // La portada ya es la propia pantalla de inicio del cliente (banda de
    // marca, foto y accesos), así que no hace falta una bienvenida aparte.

    // Edge-to-edge container handling status bars and navigation bars
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.CLIENT -> {
                    ClientScreen(
                        viewModel = viewModel,
                        onOpenAdminRequest = { showPinDialog = true }
                    )
                }
                Screen.ADMIN -> {
                    AdminScreen(
                        viewModel = viewModel,
                        onBackToCatalog = { currentScreen = Screen.CLIENT }
                    )
                }
            }

            if (showPinDialog) {
                PinEntryDialog(
                    onDismiss = { showPinDialog = false },
                    onVerify = { pin ->
                        if (viewModel.verifyPin(pin)) {
                            showPinDialog = false
                            currentScreen = Screen.ADMIN
                        } else {
                            Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

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

    // Vista activa dentro de los dos tercios inferiores. La banda de marca de
    // arriba no cambia nunca.
    var vista by rememberSaveable { mutableStateOf(ClientView.INICIO) }
    var categoriaActiva by rememberSaveable { mutableStateOf("Bodas") }
    var itemDetalleId by rememberSaveable { mutableStateOf(-1) }

    var showSummaryDialog by remember { mutableStateOf(false) }
    var showContractForOrder by remember { mutableStateOf(false) }
    var pedidoConfirmado by remember { mutableStateOf<ContratoFirmado?>(null) }
    var itemForExtrasDialog by remember { mutableStateOf<CatalogItem?>(null) }

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
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- TERCIO SUPERIOR: fijo, siempre visible ----
        StudioHeaderBand(
            onOpenAdminRequest = onOpenAdminRequest,
            config = studioConfig,
            // En la portada se muestra grande; dentro de una sección se
            // compacta para dejar sitio al contenido.
            compacto = vista != ClientView.INICIO
        )

        // ---- DOS TERCIOS INFERIORES: cambian según la vista ----
        Box(modifier = Modifier.weight(1f)) {
            when (vista) {
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
                            config = studioConfig
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
                            onVolver = { vista = ClientView.OFERTAS }
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Botón para regresar a la portada desde cualquier sección.
            if (vista != ClientView.INICIO && vista != ClientView.DETALLE) {
                FilledTonalButton(
                    onClick = { volverAInicio() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp)
                        .testTag("btn_inicio")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inicio", fontWeight = FontWeight.Bold)
                }
            }
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
            onDismiss = { itemForExtrasDialog = null },
            onAddExtra = { title, cat, variant, price, qty ->
                viewModel.addCustomToCart(title, cat, variant, price, qty, "Extra añadido a ${item.name}")
                Toast.makeText(context, "Extra añadido al pedido", Toast.LENGTH_SHORT).show()
            }
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
            title = { Text("Pedido enviado al estudio") },
            text = {
                Text(
                    "El pedido de ${firmado.nombreCliente} ya salió hacia el WhatsApp de " +
                        "FXestudio.\n\n¿Desea enviarle también su copia al " +
                        "${firmado.telefonoCliente}?"
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

// ==========================================
// ADMIN SCREEN
// ==========================================
@Composable
fun AdminScreen(
    viewModel: StudioViewModel,
    onBackToCatalog: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.catalogItems.collectAsState()
    val currentWhatsapp by viewModel.whatsappNumber.collectAsState()
    val currentPin by viewModel.adminPin.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CatalogItem?>(null) }
    var itemPendingDelete by remember { mutableStateOf<CatalogItem?>(null) }

    var adminTabSelected by remember { mutableStateOf(0) } // 0 = Catalog CRUD, 1 = Appointments, 2 = Config, 3 = Backup

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Admin Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToCatalog,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Panel Administrador",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "FXestudio • Configuración & Gestión",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (adminTabSelected == 0) {
                Button(
                    onClick = {
                        itemToEdit = null
                        showAddEditDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("admin_new_item_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin Navigation Tabs (Items, Citas, Ajustes, Respaldo)
        // Cinco pestañas ya no caben repartidas a lo ancho de un teléfono:
        // con ScrollableTabRow se deslizan de lado sin recortar los nombres.
        ScrollableTabRow(
            selectedTabIndex = adminTabSelected,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp
        ) {
            Tab(
                selected = adminTabSelected == 0,
                onClick = { adminTabSelected = 0 },
                text = { Text("Catálogo", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 1,
                onClick = { adminTabSelected = 1 },
                text = { Text("Citas", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 2,
                onClick = { adminTabSelected = 2 },
                text = { Text("Portada", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 3,
                onClick = { adminTabSelected = 3 },
                text = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 4,
                onClick = { adminTabSelected = 4 },
                text = { Text("Respaldo", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Backup, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (adminTabSelected) {
                0 -> {
                    // Catalog CRUD List
                    if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("El catálogo está vacío. Agrega tu primer paquete!")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items) { item ->
                                AdminCatalogItemCard(
                                    item = item,
                                    onEdit = {
                                        itemToEdit = item
                                        showAddEditDialog = true
                                    },
                                    onDuplicate = {
                                        viewModel.duplicateCatalogItem(item)
                                        Toast.makeText(context, "Item duplicado", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = { itemPendingDelete = item }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Appointments View
                    AppointmentsAdminView(viewModel = viewModel)
                }
                2 -> {
                    // Editor de la portada y de la ficha de contacto
                    AdminCoverView(viewModel = viewModel)
                }
                3 -> {
                    // Configuration Form
                    AdminSettingsView(
                        viewModel = viewModel,
                        currentWhatsapp = currentWhatsapp,
                        currentPin = currentPin,
                        onSaveWhatsapp = { num ->
                            viewModel.updateWhatsAppNumber(num)
                            Toast.makeText(context, "Número guardado", Toast.LENGTH_SHORT).show()
                        },
                        onSavePin = { pin ->
                            if (pin.length == 4 && pin.all { it.isDigit() }) {
                                viewModel.updateAdminPin(pin)
                                Toast.makeText(context, "PIN de acceso guardado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "El PIN debe tener exactamente 4 dígitos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                4 -> {
                    // Backup & Import
                    AdminBackupView(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    itemPendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Eliminar este paquete?") },
            text = {
                val etiqueta = if (item.code.isNotBlank()) "[${item.code}] ${item.name}" else item.name
                Text("Se quitará \"$etiqueta\" del catálogo.\n\nEsta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCatalogItem(item.id)
                        itemPendingDelete = null
                        Toast.makeText(context, "Item eliminado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sí, eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemPendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAddEditDialog) {
        AddEditItemDialog(
            item = itemToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { code, name, desc, cat, variants, includedExtras, imgBytes ->
                viewModel.saveCatalogItem(
                    id = itemToEdit?.id ?: 0,
                    code = code,
                    name = name,
                    description = desc,
                    category = cat,
                    variants = variants,
                    includedExtras = includedExtras,
                    imageBytes = imgBytes
                )
                showAddEditDialog = false
                Toast.makeText(context, "Ítem guardado con éxito", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AdminCatalogItemCard(
    item: CatalogItem,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CatalogItemImage(
                imageBytes = item.imageBytes,
                category = item.category,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.code.isNotBlank()) {
                        Text(
                            text = "[${item.code}] ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.getVariants().size} variantes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.includedExtras.isNotBlank()) {
                    Text(
                        text = "Incluye: ${item.includedExtras}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row {
                IconButton(onClick = onDuplicate) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicar",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AdminSettingsView(
    viewModel: StudioViewModel,
    currentWhatsapp: String,
    currentPin: String,
    onSaveWhatsapp: (String) -> Unit,
    onSavePin: (String) -> Unit
) {
    val context = LocalContext.current
    var whatsappInput by remember { mutableStateOf(currentWhatsapp) }
    var pinInput by remember { mutableStateOf(currentPin) }

    val contractText by viewModel.contractText.collectAsState()
    var contractInput by remember(contractText) {
        mutableStateOf(contractText.ifBlank { FULL_CONTRACT_TEXT })
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            LicenseStatusCard(viewModel = viewModel)
        }

        item {
            AdminRatesCard(viewModel = viewModel)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configuración WhatsApp FXestudio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ingresa el número de WhatsApp (ej: 55823513 o 5355823513) al cual se enviarán los pedidos y reservaciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = whatsappInput,
                        onValueChange = { whatsappInput = it },
                        label = { Text("Número de WhatsApp") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_whatsapp_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSaveWhatsapp(whatsappInput) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("admin_save_whatsapp")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar Teléfono")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PIN de Seguridad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cambia el código PIN de 4 dígitos para proteger el ingreso al Panel de Administración.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("PIN de 4 dígitos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSavePin(pinInput) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Actualizar PIN")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Texto del Contrato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Es el texto que el cliente lee y firma. Edítalo aquí cuando cambien " +
                            "tus condiciones, sin necesidad de actualizar la aplicación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contractInput,
                        onValueChange = { contractInput = it },
                        label = { Text("Contrato de Sesión Fotográfica") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .testTag("admin_contract_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                contractInput = FULL_CONTRACT_TEXT
                                viewModel.updateContractText("")
                                Toast.makeText(
                                    context,
                                    "Restaurado el contrato original",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restaurar original")
                        }

                        Button(
                            onClick = {
                                if (contractInput.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "El contrato no puede quedar vacío",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    viewModel.updateContractText(contractInput)
                                    Toast.makeText(context, "Contrato guardado", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LicenseStatusCard(
    viewModel: StudioViewModel
) {
    val isMaster by viewModel.licenseIsMaster.collectAsState()
    val periodLabel = remember { com.example.data.LicenseManager.currentPeriodLabel() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Licencia de la Aplicación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isMaster) {
                    "Desbloqueada de forma permanente con llave maestra."
                } else {
                    "Activa para el periodo: $periodLabel. Debes volver a activarla el próximo mes con el nuevo código."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminBackupView(
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var importTextJson by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    viewModel.importBackupJson(
                        jsonString = text,
                        onSuccess = {
                            Toast.makeText(context, "Catálogo importado con éxito!", Toast.LENGTH_LONG).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, "Error al importar: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al leer archivo de respaldo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Exportar Copia de Seguridad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exporta el catálogo completo (códigos, variantes, extras, precios e imágenes) y también las reservaciones con su firma digital. Guárdalo con frecuencia: es tu única copia de los contratos firmados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.exportBackupJson()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Respaldo FXestudio")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Guardar Respaldo"))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_export_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Catálogo")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Importar Copia de Seguridad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona un archivo JSON exportado previamente para reestablecer todo el catálogo y configuraciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            importLauncher.launch("application/json")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_import_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar archivo JSON de Respaldo")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "O pega el texto JSON directamente aquí:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = importTextJson,
                        onValueChange = { importTextJson = it },
                        label = { Text("Pegar JSON de Respaldo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (importTextJson.isNotBlank()) {
                                viewModel.importBackupJson(
                                    jsonString = importTextJson,
                                    onSuccess = {
                                        importTextJson = ""
                                        Toast.makeText(context, "Catálogo importado desde texto con éxito!", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Error al importar JSON de texto: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = importTextJson.isNotBlank()
                    ) {
                        Text("Importar Texto Pegado")
                    }
                }
            }
        }
    }
}

// ==========================================
// DIALOGS & OVERLAYS
// ==========================================

// 0. License Activation Screen (blocking gate before the whole app)
@Composable
fun LicenseActivationScreen(
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Activación requerida",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "La licencia de este mes venció o aún no se ha activado. Ingresa el código de activación para seguir usando la aplicación.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it
                        showError = false
                    },
                    label = { Text("Código de activación") },
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("license_code_input")
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Código incorrecto. Verifica e intenta de nuevo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (viewModel.activateLicense(codeInput)) {
                            Toast.makeText(context, "Aplicación activada", Toast.LENGTH_SHORT).show()
                            codeInput = ""
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("license_activate_button")
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activar")
                }
            }
        }
    }
}

// 1. PIN Access Dialog
@Composable
fun PinEntryDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Acceso Administrador",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Ingrese el PIN de 4 dígitos del estudio:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 1..4) {
                        val active = pin.length >= i
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(50.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val numbers = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("admin_pin_input")
                ) {
                    numbers.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { char ->
                                val isAction = char == "C" || char == "OK"
                                Button(
                                    onClick = {
                                        when (char) {
                                            "C" -> {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            }
                                            "OK" -> {
                                                if (pin.length == 4) {
                                                    onVerify(pin)
                                                }
                                            }
                                            else -> {
                                                if (pin.length < 4) {
                                                    pin += char
                                                    if (pin.length == 4) {
                                                        onVerify(pin)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAction) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        contentColor = if (char == "OK") MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .testTag("pin_key_$char"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// 2. Receipt Summary Dialog
@Composable
fun SummaryDialog(
    viewModel: StudioViewModel,
    cart: List<CartItem>,
    total: Double,
    subtotal: Double,
    descuento: Double,
    onDiscountChange: (Double) -> Unit,
    cupLabelFor: (Double) -> String? = { null },
    onDismiss: () -> Unit,
    onProceedToContract: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "RESUMEN DE COTIZACIÓN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "FXestudio • Bayamo, Granma, Cuba",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cart) { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(0.7f)
                                )
                                Text(
                                    text = "$${String.format("%.2f", item.subtotal)} USD",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(0.3f),
                                    textAlign = TextAlign.End
                                )
                            }
                            Text(
                                text = "Formato/Var: ${item.variant.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Cant: ${item.quantity} x $${String.format("%.2f", item.variant.price)} USD",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Descuento: el contrato admite negociar el precio, así que se
                // aplica aquí en vez de inventar un ítem con importe negativo.
                // Sin clave en remember: si dependiera de `descuento`, cada tecla
                // reescribiría el campo y sería imposible teclear decimales.
                var descuentoInput by remember {
                    mutableStateOf(if (descuento > 0.0) String.format("%.2f", descuento) else "")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = descuentoInput,
                        onValueChange = { nuevo ->
                            descuentoInput = nuevo.filter { it.isDigit() || it == '.' }
                            onDiscountChange(descuentoInput.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Descuento (USD)") },
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("discount_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (descuento > 0.0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "$${String.format("%.2f", subtotal)} USD",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Descuento:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "-$${String.format("%.2f", descuento)} USD",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL GENERAL:",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%.2f", total)} USD",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        cupLabelFor(total)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onProceedToContract,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_whatsapp_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Draw, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Continuar y Firmar Contrato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// 3. Add / Edit Catalog Item Dialog with Code, IncludedExtras, and Quick Size Templates
@Composable
fun AddEditItemDialog(
    item: CatalogItem?,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, desc: String, cat: String, variants: List<CatalogVariant>, includedExtras: String, imgBytes: ByteArray?) -> Unit
) {
    val context = LocalContext.current

    var code by remember { mutableStateOf(item?.code ?: "") }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Primer Año") }
    var includedExtras by remember { mutableStateOf(item?.includedExtras ?: "") }

    val defaultCategories = listOf("Primer Año", "Bodas", "15 años", "Ofertas Especiales", "Retratos", "Ampliaciones", "Otros")
    var showCategoryMenu by remember { mutableStateOf(false) }

    var variantsList by remember {
        mutableStateOf(item?.getVariants() ?: emptyList())
    }
    var newVariantName by remember { mutableStateOf("") }
    var newVariantPrice by remember { mutableStateOf("") }

    var imageBytesState by remember { mutableStateOf<ByteArray?>(null) }
    var imageUriState by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUriState = it
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                imageBytesState = bytes
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .heightIn(max = 680.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (item == null) "Nuevo Paquete / Servicio" else "Editar Paquete",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Image Selection Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val bytesToShow = imageBytesState ?: item?.imageBytes
                    if (bytesToShow != null) {
                        val bitmap = remember(bytesToShow) {
                            try {
                                BitmapFactory.decodeByteArray(bytesToShow, 0, bytesToShow.size)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Seleccionar Foto de Muestra",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código (ej: A1, B1, Qt1)") },
                        modifier = Modifier.weight(0.35f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Paquete") },
                        modifier = Modifier.weight(0.65f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción corta") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = includedExtras,
                    onValueChange = { includedExtras = it },
                    label = { Text("Extras Incluidos (separados por coma)") },
                    placeholder = { Text("ej: 1 foto 8x12 enmarcada, Maquillaje mamá, Taza") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showCategoryMenu = !showCategoryMenu }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        defaultCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Quick Size Templates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Variantes y Precios:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(
                        onClick = {
                            variantsList = listOf(
                                CatalogVariant("5x7 / 6x8", 50.00),
                                CatalogVariant("8x10 / 8x12", 65.00),
                                CatalogVariant("Digital", 40.00)
                            )
                        }
                    ) {
                        Text("Plantilla Rápida", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (variantsList.isEmpty()) {
                    Text(
                        text = "Sin variantes cargadas. Debe agregar al menos una.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        variantsList.forEachIndexed { idx, variant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${variant.name} -> $${String.format("%.2f", variant.price)} USD",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                IconButton(
                                    onClick = {
                                        variantsList = variantsList.filterIndexed { i, _ -> i != idx }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Quitar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newVariantName,
                        onValueChange = { newVariantName = it },
                        label = { Text("Variante (ej: 6x8)") },
                        modifier = Modifier.weight(0.55f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newVariantPrice,
                        onValueChange = { newVariantPrice = it },
                        label = { Text("Precio") },
                        modifier = Modifier.weight(0.35f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    IconButton(
                        onClick = {
                            val price = newVariantPrice.toDoubleOrNull()
                            if (newVariantName.isNotBlank() && price != null) {
                                variantsList = variantsList + CatalogVariant(newVariantName, price)
                                newVariantName = ""
                                newVariantPrice = ""
                            } else {
                                Toast.makeText(context, "Nombre y precio válidos requeridos", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && description.isNotBlank() && category.isNotBlank() && variantsList.isNotEmpty()) {
                                onSave(code, name, description, category, variantsList, includedExtras, imageBytesState)
                            } else {
                                Toast.makeText(context, "Por favor complete todos los campos y cargue al menos una variante.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_save_item_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
