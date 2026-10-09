package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Carpeta
import com.example.data.Medidas
import com.example.data.MenuServicios
import com.example.data.PAQUETES_CON_EXTRAS
import com.example.data.Servicio
import com.example.data.Subcarpeta
import com.example.data.guardarCarpeta
import com.example.data.guardarServicio
import com.example.data.guardarSubcarpeta
import com.example.data.moverCarpeta
import com.example.data.moverServicio
import com.example.data.moverSubcarpeta
import com.example.data.quitarCarpeta
import com.example.data.quitarServicio
import com.example.data.quitarSubcarpeta

/*
 * Tarjeta de arriba de la pestaña Ofertas: lo que llevan todas las ofertas
 * (el transporte) y la unidad de las medidas.
 */

// ------------------------------------------------------------------
// Ajustes que valen para todas las ofertas (pestaña Ofertas)
// ------------------------------------------------------------------

/**
 * Lo que llevan todas las ofertas (el transporte) y en qué unidad están
 * escritas las medidas del catálogo.
 */
@Composable
fun AdminAjustesOfertasCard(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val config by viewModel.studioConfig.collectAsState()
    var incluye by remember(config.incluyeEnOfertas) { mutableStateOf(config.incluyeEnOfertas) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "En todas las ofertas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = incluye,
                onValueChange = { incluye = it },
                label = { Text("Incluido en todas las ofertas") },
                placeholder = { Text("Ej: Transporte") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_incluye_ofertas")
            )
            Text(
                text = "Sale en el \"Incluye\" de cada paquete y en el pedido de WhatsApp. " +
                    "Separa con comas si son varias cosas; en blanco no sale nada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    viewModel.updateStudioConfig(config.copy(incluyeEnOfertas = incluye.trim()))
                    Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show()
                },
                enabled = incluye.trim() != config.incluyeEnOfertas,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar")
            }

            HorizontalDivider()

            Text(
                text = "Las medidas del catálogo están en",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = config.medidasEnPulgadas,
                    onClick = {
                        if (!config.medidasEnPulgadas) {
                            viewModel.updateStudioConfig(config.copy(medidasEnPulgadas = true))
                        }
                    },
                    label = { Text("Pulgadas") }
                )
                FilterChip(
                    selected = !config.medidasEnPulgadas,
                    onClick = {
                        if (config.medidasEnPulgadas) {
                            viewModel.updateStudioConfig(config.copy(medidasEnPulgadas = false))
                        }
                    },
                    label = { Text("Centímetros") }
                )
            }
            Text(
                text = "La app pone al lado la equivalencia en la otra unidad. Ejemplo: " +
                    (Medidas.equivalencias("8x12", config.medidasEnPulgadas).firstOrNull() ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
