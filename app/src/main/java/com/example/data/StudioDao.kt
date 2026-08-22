package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {
    @Query("SELECT * FROM catalog_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<CatalogItem>>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun getItemById(id: Int): CatalogItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CatalogItem): Long

    @Delete
    suspend fun deleteItem(item: CatalogItem)

    @Query("DELETE FROM catalog_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("SELECT * FROM app_config WHERE `key` = :key")
    suspend fun getConfig(key: String): AppConfig?

    @Query("SELECT * FROM app_config")
    suspend fun getAllConfigs(): List<AppConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfig)

    @Query("DELETE FROM catalog_items")
    suspend fun clearCatalog()

    @Query("DELETE FROM app_config")
    suspend fun clearConfigs()

    // Appointments
    @Query("SELECT * FROM appointments ORDER BY createdAt DESC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    // Lectura puntual, usada al exportar el respaldo.
    @Query("SELECT * FROM appointments ORDER BY createdAt DESC")
    suspend fun getAllAppointmentsOnce(): List<AppointmentEntity>

    @Query("DELETE FROM appointments")
    suspend fun clearAppointments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Int)
}
