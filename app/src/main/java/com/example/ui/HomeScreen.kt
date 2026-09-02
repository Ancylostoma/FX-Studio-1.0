package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.StudioConfig
import com.example.data.StudioInfo
import com.example.ui.theme.FxGold

/** Primer número de la lista de teléfonos, para el botón de llamar. */
private fun primerTelefono(telefonos: String): String =
    telefonos.split("/", ",").firstOrNull()?.filter { it.isDigit() || it == '+' }
        ?.ifBlank { StudioInfo.TELEFONO_1 } ?: StudioInfo.TELEFONO_1

/**
 * Banda superior fija con la marca. Permanece visible en todas las vistas del
 * cliente; solo cambian los dos tercios inferiores.
 */
@Composable
fun StudioHeaderBand(
    onOpenAdminRequest: () -> Unit,
    // Textos editables desde el panel del admin (pestaña Portada).
    config: StudioConfig = StudioConfig(),
    compacto: Boolean = false,
    // Vuelta a la portada. Se pasa en todas las secciones y queda fijo en la
    // esquina de arriba a la izquierda; en la portada misma se pasa null,
    // porque ya se está ahí.
    onInicio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = if (compacto) 12.dp else 22.dp)
    ) {
        Column(
            // Los dos botones de las esquinas van encima de esta columna: se
            // le reserva su ancho para que un título largo no pase por debajo.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (onInicio != null) 44.dp else 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = config.titulo,
                style = if (compacto) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            if (!compacto) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(FxGold)
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = config.lema,
                style = if (compacto) MaterialTheme.typography.titleSmall
                else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }

        // Vuelta a la portada, siempre en la misma esquina.
        if (onInicio != null) {
            IconButton(
                onClick = onInicio,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(44.dp)
                    .testTag("btn_inicio")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Volver al inicio",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Acceso al panel de administración, discreto en la esquina.
        IconButton(
            onClick = onOpenAdminRequest,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(44.dp)
                .testTag("admin_mode_button")
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Modo Administrador",
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            )
        }
    }
}

/** Uno de los cinco accesos de la portada. */
@Composable
fun MenuPrincipalBoton(
    titulo: String,
    // null = sin icono. La portada los quita; las pestañas de categoría
    // dentro del catálogo sí los mantienen, que ahí ayudan a orientarse.
    icono: ImageVector?,
    atenuado: Boolean,
    resaltado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (atenuado) 0.35f else 1f

    // El botón se hunde mientras se mantiene pulsado y vuelve con un rebote
    // corto. Es lo que hace que el toque se sienta físico.
    val interaccion = remember { MutableInteractionSource() }
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsado) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escala_boton"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaccion,
                indication = LocalIndication.current
            ) { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (resaltado) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (resaltado) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icono != null) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = if (resaltado) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (resaltado) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Contenido de la portada: foto del estudio y, debajo, los cinco accesos y la
 * ficha de contacto con el código QR del catálogo.
 */
@Composable
fun HomeContent(
    onCategoria: (String) -> Unit,
    onOfertaPropia: () -> Unit,
    onCalendario: () -> Unit,
    // Textos editables desde el panel del admin (pestaña Portada).
    config: StudioConfig = StudioConfig(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun abrir(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Foto de portada.
        //
        // El original es muy alto (1080x2400) y la modelo está en el tercio de
        // arriba. Con una altura fija y recorte centrado, la ventana caía sobre
        // el torso y le cortaba la cabeza. Con una proporción fija el recorte
        // se comporta igual en teléfono y en tablet, y la alineación sesgada
        // hacia arriba deja dentro el sombrero, la cara y los hombros.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
        ) {
            // Acercamiento lento al aparecer: la foto se asienta en su sitio
            // en vez de plantarse de golpe.
            var asentada by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { asentada = true }
            val zoom by animateFloatAsState(
                targetValue = if (asentada) 1f else 1.07f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                label = "zoom_portada"
            )

            Image(
                painter = painterResource(id = R.drawable.fondo_bienvenida),
                contentDescription = "Estudio FXestudio",
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, -0.72f),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                    }
            )
            // El velo solo entra en el cuarto inferior, donde va la frase: así
            // la cara de la modelo se ve limpia, sin oscurecer.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Color(0x99000000)
                        )
                    )
            )
            // Si el administrador borra la frase, la portada se queda sin
            // frase: no vuelve el texto de fábrica.
            if (config.frasePortada.isNotBlank()) {
                Text(
                    text = config.frasePortada,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Los cinco accesos: 3 categorías arriba, 2 herramientas abajo.
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuPrincipalBoton(
                    titulo = config.btnBodas,
                    icono = null,
                    atenuado = false,
                    resaltado = false,
                    onClick = { onCategoria("Bodas") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_bodas")
                )
                MenuPrincipalBoton(
                    titulo = config.btnQuince,
                    icono = null,
                    atenuado = false,
                    resaltado = false,
                    onClick = { onCategoria("15 años") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quince")
                )
                MenuPrincipalBoton(
                    titulo = config.btnPrimerAno,
                    icono = null,
                    atenuado = false,
                    resaltado = false,
                    onClick = { onCategoria("Primer Año") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_primer_ano")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuPrincipalBoton(
                    titulo = config.btnOfertaPropia,
                    icono = null,
                    atenuado = false,
                    resaltado = false,
                    onClick = onOfertaPropia,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_oferta_propia")
                )
                MenuPrincipalBoton(
                    titulo = config.btnCalendario,
                    icono = null,
                    atenuado = false,
                    resaltado = false,
                    onClick = onCalendario,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_calendario")
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Ficha de contacto con QR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🤗📸 Bienvenido a ${config.titulo}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${config.ubicacion} 🇨🇺",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider()
                Spacer(modifier = Modifier.height(14.dp))

                // Horario
                Row(verticalAlignment = Alignment.Top) {
                    Text("🕘", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.horarioSemana,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = config.horarioSabado,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${config.horarioDomingo} 😴",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Teléfonos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📞", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = config.telefonos,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { abrir("tel:${primerTelefono(config.telefonos)}") }) {
                        Text("Llamar")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dirección
                Row(verticalAlignment = Alignment.Top) {
                    Text("📍", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = config.direccion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // El QR es una imagen ya impresa que apunta al catálogo
                // original: si el admin cambia el enlace, se oculta para no
                // mandar al cliente a la dirección equivocada.
                if (config.catalogoUrl == StudioInfo.CATALOGO_URL) {
                    Text(
                        text = "📖 Escanea para ver nuestro catálogo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(
                        painter = painterResource(id = R.drawable.qr_catalogo),
                        contentDescription = "Código QR del catálogo de FXestudio",
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { abrir(config.catalogoUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir catálogo completo", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { abrir(config.facebookUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "👀 Ver fotos en Facebook",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
