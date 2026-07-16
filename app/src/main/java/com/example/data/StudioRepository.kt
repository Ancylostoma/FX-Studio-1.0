package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class StudioRepository(private val studioDao: StudioDao) {
    val allItems: Flow<List<CatalogItem>> = studioDao.getAllItems()

    suspend fun getItemById(id: Int): CatalogItem? {
        return studioDao.getItemById(id)
    }

    suspend fun insertItem(item: CatalogItem): Long {
        return studioDao.insertItem(item)
    }

    suspend fun deleteItemById(id: Int) {
        studioDao.deleteItemById(id)
    }

    suspend fun getAdminPin(): String {
        return studioDao.getConfig("admin_pin")?.value ?: "1234"
    }

    suspend fun setAdminPin(pin: String) {
        studioDao.insertConfig(AppConfig("admin_pin", pin))
    }

    suspend fun getWhatsAppNumber(): String {
        return studioDao.getConfig("whatsapp_number")?.value ?: ""
    }

    suspend fun setWhatsAppNumber(number: String) {
        studioDao.insertConfig(AppConfig("whatsapp_number", number))
    }

    suspend fun getAllConfigs(): List<AppConfig> {
        return studioDao.getAllConfigs()
    }

    // Acceso genérico de configuración (usado por el sistema de licencia)
    suspend fun getConfigValue(key: String): String? {
        return studioDao.getConfig(key)?.value
    }

    suspend fun setConfigValue(key: String, value: String) {
        studioDao.insertConfig(AppConfig(key, value))
    }

    suspend fun importBackup(items: List<CatalogItem>, configs: List<AppConfig>) {
        studioDao.clearCatalog()
        studioDao.clearConfigs()
        items.forEach { studioDao.insertItem(it) }
        configs.forEach { studioDao.insertConfig(it) }
    }

    suspend fun prepopulateIfNeeded() {
        val items = studioDao.getAllItems().firstOrNull()
        if (items.isNullOrEmpty()) {
            val defaultItems = listOf(
                CatalogItem(
                    name = "Retrato Profesional Individual",
                    description = "Sesión fotográfica individual en estudio o locación externa con iluminación y retoque digital.",
                    category = "Retratos",
                    variantsString = CatalogItem.createVariantsString(
                        listOf(
                            CatalogVariant("Básico (5 Fotos Digitales)", 80.0),
                            CatalogVariant("Estándar (15 Fotos Digitales)", 150.0),
                            CatalogVariant("Premium (30 Fotos + Impresiones)", 280.0)
                        )
                    )
                ),
                CatalogItem(
                    name = "Paquete Bodas de Oro/Plata",
                    description = "Cobertura integral de la ceremonia, sesión romántica de novios y brindis familiar.",
                    category = "Bodas y Parejas",
                    variantsString = CatalogItem.createVariantsString(
                        listOf(
                            CatalogVariant("Cobertura Digital Completa", 850.0),
                            CatalogVariant("Digital + Fotolibro Premium", 1200.0),
                            CatalogVariant("Servicio Completo Premium", 1800.0)
                        )
                    )
                ),
                CatalogItem(
                    name = "Book Artístico de Modelos",
                    description = "Producción creativa de moda, portafolio y dirección de poses con múltiples cambios de vestuario.",
                    category = "Books",
                    variantsString = CatalogItem.createVariantsString(
                        listOf(
                            CatalogVariant("Book Express (10 Fotos)", 120.0),
                            CatalogVariant("Book Profesional (25 Fotos)", 250.0)
                        )
                    )
                ),
                CatalogItem(
                    name = "Impresiones de Calidad Museo",
                    description = "Impresión en papel fotográfico profesional con tintas de archivo pigmentadas.",
                    category = "Impresiones",
                    variantsString = CatalogItem.createVariantsString(
                        listOf(
                            CatalogVariant("Tamaño 8x10 pulgadas", 12.0),
                            CatalogVariant("Tamaño 11x14 pulgadas", 20.0),
                            CatalogVariant("Tamaño 16x20 pulgadas", 38.0)
                        )
                    )
                ),
                CatalogItem(
                    name = "Sesión de Fotos Familiar",
                    description = "Captura momentos inolvidables con tu familia en un ambiente divertido y natural.",
                    category = "Sesión Familiar",
                    variantsString = CatalogItem.createVariantsString(
                        listOf(
                            CatalogVariant("Sesión Corta (10 Fotos)", 100.0),
                            CatalogVariant("Sesión Completa (25 Fotos)", 190.0)
                        )
                    )
                )
            )
            for (item in defaultItems) {
                studioDao.insertItem(item)
            }
        }

        // Initialize default PIN and default WhatsApp if they do not exist
        if (studioDao.getConfig("admin_pin") == null) {
            studioDao.insertConfig(AppConfig("admin_pin", "1234"))
        }
        if (studioDao.getConfig("whatsapp_number") == null) {
            studioDao.insertConfig(AppConfig("whatsapp_number", "5359200853"))
        }
    }
}
