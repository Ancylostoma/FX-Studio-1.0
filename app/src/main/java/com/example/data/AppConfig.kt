package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val key: String, // e.g. "admin_pin", "whatsapp_number"
    val value: String
)
