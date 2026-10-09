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
 * Pestaña Respaldo del panel: exportar e importar la copia de seguridad y el Excel.
 */

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
                        text = "Exportar a Excel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Crea un archivo de Excel con tres hojas: un resumen de cuentas, " +
                            "todas las reservaciones y el catálogo completo con sus precios. " +
                            "Sirve para llevar las cuentas en la computadora; para conservar las " +
                            "firmas y las fotos usa la copia de seguridad de arriba.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val carpeta = File(context.cacheDir, "exports")
                                    val archivo = viewModel.exportarExcel(carpeta)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        archivo
                                    )
                                    val enviar = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.openxmlformats-officedocument" +
                                            ".spreadsheetml.sheet"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, archivo.name)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(enviar, "Guardar o enviar el Excel")
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No se pudo crear el Excel: ${e.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_export_excel_button")
                    ) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar a Excel")
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
