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
 * Panel de administración: la cabecera, las pestañas y qué se enseña en cada una.
 * Cada pestaña vive en su propio archivo.
 */

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
    val ultimoRespaldo by viewModel.ultimoRespaldo.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<CatalogItem?>(null) }
    var itemPendingDelete by remember { mutableStateOf<CatalogItem?>(null) }

    // 0 = Ofertas, 1 = Servicios, 2 = Citas, 3 = Dinero, 4 = Portada,
    // 5 = Ajustes, 6 = Respaldo
    var adminTabSelected by remember { mutableStateOf(0) }

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

        Spacer(modifier = Modifier.height(12.dp))

        // Aviso siempre visible de cuánto hace de la última copia. Los
        // contratos firmados solo están en este teléfono.
        BackupReminderBanner(
            ultimoRespaldo = ultimoRespaldo,
            onIrARespaldo = { adminTabSelected = 6 }
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                text = { Text("Ofertas", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 1,
                onClick = { adminTabSelected = 1 },
                text = { Text("Servicios", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Category, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 2,
                onClick = { adminTabSelected = 2 },
                text = { Text("Citas", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 3,
                onClick = { adminTabSelected = 3 },
                text = { Text("Dinero", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 4,
                onClick = { adminTabSelected = 4 },
                text = { Text("Portada", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 5,
                onClick = { adminTabSelected = 5 },
                text = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
            Tab(
                selected = adminTabSelected == 6,
                onClick = { adminTabSelected = 6 },
                text = { Text("Respaldo", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Backup, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            // Fundido entre pestañas: el salto seco delataba lo casero.
            Crossfade(
                targetState = adminTabSelected,
                animationSpec = tween(220),
                label = "pestana_admin"
            ) { pestana ->
            when (pestana) {
                0 -> {
                    // Las ofertas (paquetes) y, arriba, lo que vale para todas
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { AdminAjustesOfertasCard(viewModel = viewModel) }
                        if (items.isEmpty()) {
                            item {
                                Text(
                                    "El catálogo está vacío. Agrega tu primer paquete con \"Nuevo\".",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
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
                    // Servicios sueltos: "Diseña tu propia oferta" y los extras
                    AdminServiciosView(viewModel = viewModel)
                }
                2 -> {
                    // Appointments View
                    AppointmentsAdminView(viewModel = viewModel)
                }
                3 -> {
                    // Resumen de dinero: cobrado, por cobrar y quién debe
                    MoneyAdminView(viewModel = viewModel)
                }
                4 -> {
                    // Editor de la portada y de la ficha de contacto
                    AdminCoverView(viewModel = viewModel)
                }
                5 -> {
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
                6 -> {
                    // Backup & Import
                    AdminBackupView(
                        viewModel = viewModel
                    )
                }
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
