package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/** Crea el archivo temporal donde la cámara escribirá la foto. */
private fun crearArchivoTemporal(context: Context): File {
    val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
    return File(dir, "confirmacion_${System.currentTimeMillis()}.jpg")
}

/** Reduce y comprime la foto para no guardar megas en la base de datos. */
private fun comprimirFoto(context: Context, uri: Uri, maxLado: Int = 900): ByteArray? {
    return try {
        // Primera pasada: solo medidas, sin cargar la imagen entera en memoria.
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, medidas)
        }
        var escala = 1
        while (medidas.outWidth / escala > maxLado * 2 || medidas.outHeight / escala > maxLado * 2) {
            escala *= 2
        }

        val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        } ?: return null

        val ratio = minOf(
            maxLado.toFloat() / bitmap.width,
            maxLado.toFloat() / bitmap.height,
            1f
        )
        val finalBitmap = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else bitmap

        ByteArrayOutputStream().use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Toma una foto del cliente como respaldo de la confirmación. La captura la
 * hace la app de cámara del sistema, así que la app no necesita el permiso
 * de CAMARA.
 */
@Composable
fun ClientPhotoCapture(
    fotoBytes: ByteArray?,
    onFotoTomada: (ByteArray?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var uriPendiente by remember { mutableStateOf<Uri?>(null) }

    val camara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val uri = uriPendiente
        if (exito && uri != null) {
            val bytes = comprimirFoto(context, uri)
            if (bytes != null) {
                onFotoTomada(bytes)
            } else {
                Toast.makeText(context, "No se pudo procesar la foto", Toast.LENGTH_SHORT).show()
            }
        }
        uriPendiente = null
    }

    fun abrirCamara() {
        try {
            val archivo = crearArchivoTemporal(context)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )
            uriPendiente = uri
            camara.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No se pudo abrir la cámara: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val bitmap = remember(fotoBytes) {
        fotoBytes?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Foto de confirmación (opcional)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (bitmap != null) {
                TextButton(
                    onClick = { onFotoTomada(null) },
                    modifier = Modifier.testTag("borrar_foto_cliente")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Quitar foto",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quitar", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto del cliente",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { abrirCamara() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Repetir foto")
            }
        } else {
            Text(
                text = "Tómale una foto al cliente para dejar constancia de quién firmó la reservación.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { abrirCamara() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("tomar_foto_cliente"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Tomar foto con la cámara",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
