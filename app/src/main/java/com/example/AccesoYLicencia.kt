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
 * Entrada a la app y al panel: la licencia mensual (pantalla de activación y
 * tarjeta de estado) y el diálogo del PIN del administrador.
 */

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

// License Activation Screen (blocking gate before the whole app)
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

// PIN Access Dialog
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
