package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A partir de aquí el aviso se pone en rojo. */
private const val DIAS_PARA_AVISAR = 7

/** Días transcurridos desde [cuando]. -1 si nunca se hizo copia. */
internal fun diasDesde(cuando: Long, ahora: Long = System.currentTimeMillis()): Int {
    if (cuando <= 0L) return -1
    val ms = ahora - cuando
    if (ms < 0) return 0
    return (ms / (1000L * 60 * 60 * 24)).toInt()
}

/** El texto del aviso, según cuánto haga de la última copia. */
internal fun textoRespaldo(dias: Int): String = when {
    dias < 0 -> "Nunca has hecho una copia de seguridad"
    dias == 0 -> "Copia de seguridad hecha hoy"
    dias == 1 -> "Última copia: ayer"
    else -> "Última copia: hace $dias días"
}

/**
 * Aviso permanente en el panel del administrador.
 *
 * Los contratos firmados y las fotos de los clientes viven solo en este
 * teléfono. Si se pierde, se pierden. Por eso el aviso no se puede cerrar:
 * se pone en rojo en cuanto pasa una semana sin exportar nada.
 */
@Composable
fun BackupReminderBanner(
    ultimoRespaldo: Long,
    onIrARespaldo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dias = diasDesde(ultimoRespaldo)
    val urgente = dias < 0 || dias >= DIAS_PARA_AVISAR

    val fondo = if (urgente) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    val tinte = if (urgente) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary
    val textoColor = if (urgente) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .then(
                if (urgente) Modifier.border(
                    1.5.dp,
                    MaterialTheme.colorScheme.error,
                    RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable { onIrARespaldo() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("backup_reminder_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (urgente) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = tinte,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = textoRespaldo(dias),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (urgente) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = textoColor
            )
            if (urgente) {
                Text(
                    text = "Los contratos firmados y las fotos solo están en este " +
                        "teléfono. Toca aquí para guardarlos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = textoColor
                )
            }
        }

        if (urgente) {
            Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = "Ir a copia de seguridad",
                tint = tinte,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
