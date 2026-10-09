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
            // El ViewModel se pide aquí arriba para conocer el tema elegido
            // antes de pintar nada. viewModel() devuelve la misma instancia
            // que usa MainApp, así que no se duplica nada.
            val viewModel: StudioViewModel = viewModel()
            val studioConfig by viewModel.studioConfig.collectAsState()
            MyApplicationTheme(temaId = studioConfig.temaId) {
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

    // Entrada suave al arrancar: la app aparece con un fundido corto en vez
    // de plantarse de golpe cuando se va la pantalla de marca de Android.
    var entrada by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entrada = true }
    val opacidad by animateFloatAsState(
        targetValue = if (entrada) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "entrada_app"
    )

    // El hueco de las barras del sistema se mide aquí, en la pantalla
    // principal, y se reparte a las ventanas que se abren encima (ver
    // MargenSistema.kt): en algunas tabletas a ellas no les llega.
    val barrasSistema = WindowInsets.safeDrawing.asPaddingValues()

    // Edge-to-edge container handling status bars and navigation bars
    CompositionLocalProvider(LocalBarrasSistema provides barrasSistema) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = opacidad }
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

    // Atrás desde el panel vuelve a la app del cliente, no la cierra.
    BackHandler(enabled = currentScreen == Screen.ADMIN) {
        currentScreen = Screen.CLIENT
    }
}
