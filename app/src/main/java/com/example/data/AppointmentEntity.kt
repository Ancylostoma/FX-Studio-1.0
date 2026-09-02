package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Etapas del trabajo, en el mismo orden que describe el contrato del estudio. */
object EstadoCita {
    const val RESERVADA = "Reservada"
    const val SESION_HECHA = "Sesión hecha"
    const val EN_EDICION = "En edición"
    const val LISTA = "Lista"
    const val ENTREGADA = "Entregada"

    val TODOS = listOf(RESERVADA, SESION_HECHA, EN_EDICION, LISTA, ENTREGADA)
}

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: String,             // e.g. "2026-08-25" or "Martes, 25 de Agosto de 2026"
    val hora: String,              // e.g. "10:00 AM"
    val nombreCliente: String,
    val telefono: String,
    val detalleSeleccion: String,
    val notas: String = "",
    val firmaBytes: ByteArray? = null,
    // Fotos tomadas al firmar, como respaldo de que la reservación se hizo
    // con esa persona delante. El contrato pide dos: normalmente el rostro
    // del cliente y su documento de identidad.
    val fotoClienteBytes: ByteArray? = null,
    val fotoCliente2Bytes: ByteArray? = null,
    val terminosAceptados: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    // El contrato exige un anticipo al reservar: se registra aquí junto al
    // monto total acordado para poder ver el saldo pendiente.
    val montoAcordado: Double = 0.0,
    val anticipoPagado: Double = 0.0,
    val estado: String = EstadoCita.RESERVADA
) {
    val saldoPendiente: Double
        get() = (montoAcordado - anticipoPagado).coerceAtLeast(0.0)
}
