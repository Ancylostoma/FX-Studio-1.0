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
 * Pestaña Ofertas del panel: la tarjeta de cada paquete y el diálogo para
 * crearlo o editarlo.
 */

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

            // El código y el nombre van en la MISMA cadena, no en dos Text
            // dentro de un Row: allí el segundo se quedaba con las sobras del
            // ancho y el nombre salía partido letra a letra, en vertical.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.code.isNotBlank()) "[${item.code}] ${item.name}" else item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.category}  •  ${item.getVariants().size} variantes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.includedExtras.isNotBlank()) {
                    Text(
                        text = "Incluye: ${item.includedExtras}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Los tres botones se apilan en columna: en horizontal se comían
            // 144dp del ancho y no dejaban sitio al nombre.
            Column {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicar",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Add / Edit Catalog Item Dialog with Code, IncludedExtras, and Quick Size Templates
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
