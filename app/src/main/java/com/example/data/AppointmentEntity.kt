package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val terminosAceptados: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
