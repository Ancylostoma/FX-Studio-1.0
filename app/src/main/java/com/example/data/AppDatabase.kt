package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [CatalogItem::class, AppConfig::class, AppointmentEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Conserva el catálogo, la configuración y el desbloqueo de licencia ya
        // guardados en los dispositivos existentes: solo agrega la tabla de citas
        // y las columnas nuevas de catalog_items.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `appointments` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `fecha` TEXT NOT NULL,
                        `hora` TEXT NOT NULL,
                        `nombreCliente` TEXT NOT NULL,
                        `telefono` TEXT NOT NULL,
                        `detalleSeleccion` TEXT NOT NULL,
                        `notas` TEXT NOT NULL DEFAULT '',
                        `firmaBytes` BLOB,
                        `terminosAceptados` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `catalog_items` ADD COLUMN `code` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `catalog_items` ADD COLUMN `includedExtras` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studio_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
