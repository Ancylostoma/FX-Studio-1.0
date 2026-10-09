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
 * Pestaña Ajustes del panel: WhatsApp, PIN, colores, tasas de cambio y contrato.
 */

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
            AdminThemeCard(viewModel = viewModel)
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
