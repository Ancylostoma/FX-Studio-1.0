package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.StudioConfig
import com.example.data.TasaPago

/** Encabezado con título y explicación, repetido en todas las tarjetas. */
@Composable
private fun TituloTarjeta(titulo: String, explicacion: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = explicacion,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
}

/** Un campo de texto con su etiqueta, para no repetirlo veinte veces. */
@Composable
private fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    ayuda: String = "",
    lineas: Int = 1
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            label = { Text(etiqueta) },
            singleLine = lineas == 1,
            minLines = lineas,
            modifier = Modifier.fillMaxWidth()
        )
        if (ayuda.isNotBlank()) {
            Text(
                text = ayuda,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Editor de las tasas de cambio. Cada forma de pago tiene su tasa (cuántas
 * unidades de esa moneda vale 1 USD) y un interruptor para mostrarla o no.
 * Una tasa en 0 nunca se muestra, para no enseñar una conversión inventada.
 */
@Composable
fun AdminRatesCard(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val config by viewModel.studioConfig.collectAsState()

    // La clave es la config guardada, no el texto: así no se reinicia en cada
    // tecla (haría imposible escribir decimales) pero sí al guardar.
    var borradores by remember(config.tasas) {
        mutableStateOf(
            config.tasas.associate { it.id to (if (it.tasa > 0.0) recortarCero(it.tasa) else "") }
        )
    }
    var visibles by remember(config.tasas) {
        mutableStateOf(config.tasas.associate { it.id to it.visible })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TituloTarjeta(
                titulo = "Precios en otras monedas",
                explicacion = "Los precios del catálogo están en USD. Aquí pones cuánto vale 1 USD " +
                    "en cada forma de pago (por ejemplo 1 USD = 660 CUP) y decides cuáles se " +
                    "muestran junto al precio. La que dejes en 0 no aparece."
            )

            config.tasas.forEach { tasa ->
                val nombreLargo = StudioConfig.NOMBRES_TASA[tasa.id] ?: tasa.nombre
                Column(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nombreLargo,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Switch(
                            checked = visibles[tasa.id] ?: false,
                            onCheckedChange = { nuevo ->
                                visibles = visibles.toMutableMap().apply { put(tasa.id, nuevo) }
                            }
                        )
                    }
                    OutlinedTextField(
                        value = borradores[tasa.id] ?: "",
                        onValueChange = { texto ->
                            val limpio = texto.filter { it.isDigit() || it == '.' }
                            borradores = borradores.toMutableMap().apply { put(tasa.id, limpio) }
                        },
                        label = { Text("${tasa.nombre} por 1 USD") },
                        placeholder = { Text(if (tasa.id == StudioConfig.ID_ZELLE) "ej: 1" else "ej: 660") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_rate_${tasa.id}")
                    )
                    val valor = (borradores[tasa.id] ?: "").toDoubleOrNull() ?: 0.0
                    if (valor > 0.0) {
                        Text(
                            text = "Ejemplo: $100.00 USD = " +
                                TasaPago(tasa.id, tasa.nombre, valor, true).formatear(100.0),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val nuevas = config.tasas.map { t ->
                        t.copy(
                            tasa = (borradores[t.id] ?: "").toDoubleOrNull() ?: 0.0,
                            visible = visibles[t.id] ?: false
                        )
                    }
                    viewModel.updateStudioConfig(config.copy(tasas = nuevas))
                    Toast.makeText(context, "Tasas guardadas", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("admin_save_rates")
            ) {
                Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar tasas")
            }
        }
    }
}

/** Quita el ".0" que sobra al mostrar una tasa entera. */
private fun recortarCero(v: Double): String =
    if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
    else String.format("%.2f", v)

/**
 * Editor de la portada: los textos de cada tercio, los nombres de los cinco
 * botones y la ficha de contacto del pie.
 */
@Composable
fun AdminCoverView(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val config by viewModel.studioConfig.collectAsState()

    // El borrador se reinicia solo cuando cambia la config guardada, no en
    // cada tecla, para poder escribir con normalidad.
    var borrador by remember(config) { mutableStateOf(config) }
    var confirmarRestaurar by remember { mutableStateOf(false) }
    val hayCambios = borrador != config

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TituloTarjeta(
                        titulo = "Tercio de arriba — la banda del estudio",
                        explicacion = "La franja con el nombre del estudio que se ve siempre, " +
                            "en todas las pantallas."
                    )
                    CampoTexto(
                        etiqueta = "Nombre del estudio",
                        valor = borrador.titulo,
                        onCambio = { borrador = borrador.copy(titulo = it) }
                    )
                    CampoTexto(
                        etiqueta = "Lema",
                        valor = borrador.lema,
                        onCambio = { borrador = borrador.copy(lema = it) }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TituloTarjeta(
                        titulo = "Tercio del medio — la foto y la frase",
                        explicacion = "La frase que aparece sobre la foto de la portada."
                    )
                    CampoTexto(
                        etiqueta = "Frase de la portada",
                        valor = borrador.frasePortada,
                        onCambio = { borrador = borrador.copy(frasePortada = it) },
                        ayuda = "Puedes usar varias líneas: pulsa Enter donde quieras el corte.",
                        lineas = 3
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TituloTarjeta(
                        titulo = "Tercio de abajo — los cinco botones",
                        explicacion = "El texto de cada acceso de la portada. Un Enter en el medio " +
                            "hace que el nombre salga en dos líneas."
                    )
                    CampoTexto(
                        etiqueta = "Botón 1 (categoría Bodas)",
                        valor = borrador.btnBodas,
                        onCambio = { borrador = borrador.copy(btnBodas = it) },
                        lineas = 2
                    )
                    CampoTexto(
                        etiqueta = "Botón 2 (categoría Quince)",
                        valor = borrador.btnQuince,
                        onCambio = { borrador = borrador.copy(btnQuince = it) },
                        lineas = 2
                    )
                    CampoTexto(
                        etiqueta = "Botón 3 (categoría 1er Año)",
                        valor = borrador.btnPrimerAno,
                        onCambio = { borrador = borrador.copy(btnPrimerAno = it) },
                        lineas = 2
                    )
                    CampoTexto(
                        etiqueta = "Botón 4 (oferta propia)",
                        valor = borrador.btnOfertaPropia,
                        onCambio = { borrador = borrador.copy(btnOfertaPropia = it) },
                        lineas = 2
                    )
                    CampoTexto(
                        etiqueta = "Botón 5 (calendario)",
                        valor = borrador.btnCalendario,
                        onCambio = { borrador = borrador.copy(btnCalendario = it) },
                        lineas = 2
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TituloTarjeta(
                        titulo = "Pie de la portada — contacto y horario",
                        explicacion = "Los datos que salen abajo, junto al código QR."
                    )
                    CampoTexto(
                        etiqueta = "Ubicación",
                        valor = borrador.ubicacion,
                        onCambio = { borrador = borrador.copy(ubicacion = it) }
                    )
                    CampoTexto(
                        etiqueta = "Dirección",
                        valor = borrador.direccion,
                        onCambio = { borrador = borrador.copy(direccion = it) },
                        lineas = 2
                    )
                    CampoTexto(
                        etiqueta = "Teléfonos",
                        valor = borrador.telefonos,
                        onCambio = { borrador = borrador.copy(telefonos = it) }
                    )
                    CampoTexto(
                        etiqueta = "Horario de lunes a viernes",
                        valor = borrador.horarioSemana,
                        onCambio = { borrador = borrador.copy(horarioSemana = it) }
                    )
                    CampoTexto(
                        etiqueta = "Horario del sábado",
                        valor = borrador.horarioSabado,
                        onCambio = { borrador = borrador.copy(horarioSabado = it) }
                    )
                    CampoTexto(
                        etiqueta = "Horario del domingo",
                        valor = borrador.horarioDomingo,
                        onCambio = { borrador = borrador.copy(horarioDomingo = it) }
                    )
                    CampoTexto(
                        etiqueta = "Enlace del catálogo (QR)",
                        valor = borrador.catalogoUrl,
                        onCambio = { borrador = borrador.copy(catalogoUrl = it) },
                        ayuda = "El código QR de la portada es una imagen ya impresa que lleva " +
                            "al enlace original. Si cambias esta dirección, el QR se oculta y " +
                            "solo queda el botón, para no mandar al cliente al sitio equivocado."
                    )
                    CampoTexto(
                        etiqueta = "Enlace de Facebook",
                        valor = borrador.facebookUrl,
                        onCambio = { borrador = borrador.copy(facebookUrl = it) }
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.updateStudioConfig(borrador)
                        Toast.makeText(context, "Portada actualizada", Toast.LENGTH_SHORT).show()
                    },
                    enabled = hayCambios,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("admin_save_cover"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hayCambios) "Guardar cambios de la portada" else "Todo guardado",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = { confirmarRestaurar = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar los textos originales")
                }
            }
        }
    }

    if (confirmarRestaurar) {
        AlertDialog(
            onDismissRequest = { confirmarRestaurar = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("¿Restaurar los textos originales?") },
            text = {
                Text(
                    "La portada volverá a los textos con los que viene la app. " +
                        "Las tasas de cambio no se tocan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetStudioTexts()
                        confirmarRestaurar = false
                        Toast.makeText(context, "Textos restaurados", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Sí, restaurar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarRestaurar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
