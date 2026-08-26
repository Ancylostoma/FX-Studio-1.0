package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.AppointmentEntity
import com.example.data.StudioConfig
import java.util.Calendar

/**
 * Pasa la cita al calendario del teléfono o la tablet.
 *
 * Se abre la app de calendario que el equipo ya tenga (Google Calendar,
 * Samsung, etc.) con todo relleno; el usuario solo pulsa Guardar. Por hacerlo
 * así, la app NO necesita permiso para leer ni escribir el calendario.
 */

/** Duración por defecto de una sesión, en horas. */
private const val HORAS_SESION = 1

/**
 * Convierte "dd/MM/yyyy" + "09:00 AM" en el momento exacto del inicio.
 * Devuelve null si la fecha no viene en el formato esperado.
 */
internal fun inicioDeLaCita(fecha: String, hora: String): Calendar? {
    val p = fecha.trim().split("/")
    if (p.size != 3) return null
    val dia = p[0].toIntOrNull() ?: return null
    val mes = p[1].toIntOrNull() ?: return null
    val anio = p[2].toIntOrNull() ?: return null

    val minutos = horaComparable(hora)
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, anio)
        set(Calendar.MONTH, mes - 1)
        set(Calendar.DAY_OF_MONTH, dia)
        set(Calendar.HOUR_OF_DAY, minutos / 60)
        set(Calendar.MINUTE, minutos % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

/**
 * Abre el calendario del equipo con la cita lista para guardar.
 * Devuelve false si no hay ninguna app de calendario instalada.
 */
fun agregarCitaAlCalendarioDelEquipo(
    context: Context,
    cita: AppointmentEntity,
    config: StudioConfig
): Boolean {
    val inicio = inicioDeLaCita(cita.fecha, cita.hora)
    if (inicio == null) {
        Toast.makeText(
            context,
            "La fecha de la cita no tiene el formato dd/mm/aaaa",
            Toast.LENGTH_LONG
        ).show()
        return false
    }

    val fin = (inicio.clone() as Calendar).apply {
        add(Calendar.HOUR_OF_DAY, HORAS_SESION)
    }

    val descripcion = buildString {
        append("Cliente: ${cita.nombreCliente}\n")
        append("Teléfono: ${cita.telefono}\n\n")
        append("Selección:\n${cita.detalleSeleccion}\n")
        if (cita.notas.isNotBlank()) append("\nNotas: ${cita.notas}\n")
        if (cita.montoAcordado > 0.0) {
            append("\nAcordado: $${String.format("%.2f", cita.montoAcordado)} USD")
            append("\nAnticipo: $${String.format("%.2f", cita.anticipoPagado)} USD")
            append("\nSaldo pendiente: $${String.format("%.2f", cita.saldoPendiente)} USD")
        }
    }

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "Sesión FXestudio — ${cita.nombreCliente}")
        putExtra(CalendarContract.Events.DESCRIPTION, descripcion)
        putExtra(CalendarContract.Events.EVENT_LOCATION, config.direccion)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio.timeInMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fin.timeInMillis)
        putExtra(CalendarContract.Events.HAS_ALARM, 1)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "No se encontró una app de calendario en este equipo",
            Toast.LENGTH_LONG
        ).show()
        false
    }
}
