package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class StudioRepository(private val studioDao: StudioDao) {
    val allItems: Flow<List<CatalogItem>> = studioDao.getAllItems()
    val allAppointments: Flow<List<AppointmentEntity>> = studioDao.getAllAppointments()

    suspend fun getItemById(id: Int): CatalogItem? {
        return studioDao.getItemById(id)
    }

    suspend fun insertItem(item: CatalogItem): Long {
        return studioDao.insertItem(item)
    }

    suspend fun deleteItemById(id: Int) {
        studioDao.deleteItemById(id)
    }

    suspend fun insertAppointment(appointment: AppointmentEntity): Long {
        return studioDao.insertAppointment(appointment)
    }

    suspend fun deleteAppointmentById(id: Int) {
        studioDao.deleteAppointmentById(id)
    }

    suspend fun getAllAppointmentsOnce(): List<AppointmentEntity> {
        return studioDao.getAllAppointmentsOnce()
    }

    suspend fun updateAppointmentPayment(id: Int, monto: Double, anticipo: Double) {
        studioDao.updateAppointmentPayment(id, monto, anticipo)
    }

    suspend fun updateAppointmentStatus(id: Int, estado: String) {
        studioDao.updateAppointmentStatus(id, estado)
    }

    /** Reemplaza la agenda completa al restaurar un respaldo que la incluya. */
    suspend fun replaceAppointments(appointments: List<AppointmentEntity>) {
        studioDao.clearAppointments()
        appointments.forEach { studioDao.insertAppointment(it) }
    }

    suspend fun getAdminPin(): String {
        return studioDao.getConfig("admin_pin")?.value ?: "1234"
    }

    suspend fun setAdminPin(pin: String) {
        studioDao.insertConfig(AppConfig("admin_pin", pin))
    }

    suspend fun getWhatsAppNumber(): String {
        return studioDao.getConfig("whatsapp_number")?.value ?: "55823513"
    }

    suspend fun setWhatsAppNumber(number: String) {
        studioDao.insertConfig(AppConfig("whatsapp_number", number))
    }

    suspend fun getAllConfigs(): List<AppConfig> {
        return studioDao.getAllConfigs()
    }

    /** Tasa USD→CUP. 0 significa "no mostrar precios en CUP". */
    suspend fun getCupRate(): Double {
        return studioDao.getConfig(KEY_CUP_RATE)?.value?.toDoubleOrNull() ?: 0.0
    }

    suspend fun setCupRate(rate: Double) {
        studioDao.insertConfig(AppConfig(KEY_CUP_RATE, rate.toString()))
    }

    /**
     * Carga la configuración editable. Cada campo cae en su valor por defecto
     * mientras el administrador no lo haya cambiado.
     */
    suspend fun getStudioConfig(): StudioConfig {
        val d = StudioConfig()
        // Se lee todo de un tirón: así basta una consulta para los veinte
        // campos, y los ayudantes de abajo no necesitan tocar la base de datos.
        val guardado = studioDao.getAllConfigs().associate { it.key to it.value }

        fun txt(clave: String, porDefecto: String) =
            guardado[clave]?.takeIf { it.isNotBlank() } ?: porDefecto

        // La tasa de CUP se hereda de la clave antigua para no perder lo que
        // el estudio ya hubiera configurado.
        val cupHeredada = guardado[KEY_CUP_RATE]?.toDoubleOrNull() ?: 0.0

        val tasas = StudioConfig.TASAS_POR_DEFECTO.map { base ->
            val tasa = guardado["tasa_${base.id}"]?.toDoubleOrNull()
                ?: if (base.id == StudioConfig.ID_CUP) cupHeredada else base.tasa
            val visible = guardado["tasa_${base.id}_visible"]?.toBooleanStrictOrNull()
                ?: base.visible
            base.copy(tasa = tasa, visible = visible)
        }

        return StudioConfig(
            titulo = txt(KEY_TITULO, d.titulo),
            lema = txt(KEY_LEMA, d.lema),
            frasePortada = txt(KEY_FRASE, d.frasePortada),
            btnBodas = txt(KEY_BTN_BODAS, d.btnBodas),
            btnQuince = txt(KEY_BTN_QUINCE, d.btnQuince),
            btnPrimerAno = txt(KEY_BTN_PRIMER, d.btnPrimerAno),
            btnOfertaPropia = txt(KEY_BTN_PROPIA, d.btnOfertaPropia),
            btnCalendario = txt(KEY_BTN_CALENDARIO, d.btnCalendario),
            ubicacion = txt(KEY_UBICACION, d.ubicacion),
            direccion = txt(KEY_DIRECCION, d.direccion),
            telefonos = txt(KEY_TELEFONOS, d.telefonos),
            horarioSemana = txt(KEY_HOR_SEMANA, d.horarioSemana),
            horarioSabado = txt(KEY_HOR_SABADO, d.horarioSabado),
            horarioDomingo = txt(KEY_HOR_DOMINGO, d.horarioDomingo),
            catalogoUrl = txt(KEY_CATALOGO, d.catalogoUrl),
            facebookUrl = txt(KEY_FACEBOOK, d.facebookUrl),
            tasas = tasas
        )
    }

    suspend fun saveStudioConfig(c: StudioConfig) {
        val pares = listOf(
            KEY_TITULO to c.titulo,
            KEY_LEMA to c.lema,
            KEY_FRASE to c.frasePortada,
            KEY_BTN_BODAS to c.btnBodas,
            KEY_BTN_QUINCE to c.btnQuince,
            KEY_BTN_PRIMER to c.btnPrimerAno,
            KEY_BTN_PROPIA to c.btnOfertaPropia,
            KEY_BTN_CALENDARIO to c.btnCalendario,
            KEY_UBICACION to c.ubicacion,
            KEY_DIRECCION to c.direccion,
            KEY_TELEFONOS to c.telefonos,
            KEY_HOR_SEMANA to c.horarioSemana,
            KEY_HOR_SABADO to c.horarioSabado,
            KEY_HOR_DOMINGO to c.horarioDomingo,
            KEY_CATALOGO to c.catalogoUrl,
            KEY_FACEBOOK to c.facebookUrl
        )
        pares.forEach { (k, v) -> studioDao.insertConfig(AppConfig(k, v)) }

        c.tasas.forEach { t ->
            studioDao.insertConfig(AppConfig("tasa_${t.id}", t.tasa.toString()))
            studioDao.insertConfig(AppConfig("tasa_${t.id}_visible", t.visible.toString()))
        }
        // Se mantiene la clave antigua sincronizada por compatibilidad.
        c.tasas.firstOrNull { it.id == StudioConfig.ID_CUP }?.let {
            studioDao.insertConfig(AppConfig(KEY_CUP_RATE, it.tasa.toString()))
        }
    }

    /** Borra los textos personalizados para volver a los de fábrica. */
    suspend fun resetStudioTexts() {
        listOf(
            KEY_TITULO, KEY_LEMA, KEY_FRASE, KEY_BTN_BODAS, KEY_BTN_QUINCE,
            KEY_BTN_PRIMER, KEY_BTN_PROPIA, KEY_BTN_CALENDARIO, KEY_UBICACION,
            KEY_DIRECCION, KEY_TELEFONOS, KEY_HOR_SEMANA, KEY_HOR_SABADO,
            KEY_HOR_DOMINGO, KEY_CATALOGO, KEY_FACEBOOK
        ).forEach { studioDao.insertConfig(AppConfig(it, "")) }
    }

    /** Texto del contrato. Vacío significa "usar el texto por defecto". */
    suspend fun getContractText(): String {
        return studioDao.getConfig(KEY_CONTRACT)?.value ?: ""
    }

    suspend fun setContractText(text: String) {
        studioDao.insertConfig(AppConfig(KEY_CONTRACT, text))
    }

    companion object {
        const val KEY_CUP_RATE = "usd_to_cup_rate"
        const val KEY_CONTRACT = "contract_text"

        // Textos de la portada y de la ficha de contacto, editables por el admin.
        const val KEY_TITULO = "home_titulo"
        const val KEY_LEMA = "home_lema"
        const val KEY_FRASE = "home_frase"
        const val KEY_BTN_BODAS = "home_btn_bodas"
        const val KEY_BTN_QUINCE = "home_btn_quince"
        const val KEY_BTN_PRIMER = "home_btn_primer"
        const val KEY_BTN_PROPIA = "home_btn_propia"
        const val KEY_BTN_CALENDARIO = "home_btn_calendario"
        const val KEY_UBICACION = "info_ubicacion"
        const val KEY_DIRECCION = "info_direccion"
        const val KEY_TELEFONOS = "info_telefonos"
        const val KEY_HOR_SEMANA = "info_horario_semana"
        const val KEY_HOR_SABADO = "info_horario_sabado"
        const val KEY_HOR_DOMINGO = "info_horario_domingo"
        const val KEY_CATALOGO = "info_catalogo_url"
        const val KEY_FACEBOOK = "info_facebook_url"
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
                // -------------------------------------------------------------
                // Categoría: Primer Año (A1 - A16)
                // -------------------------------------------------------------
                CatalogItem(
                    code = "A1",
                    name = "Paquete A1 - 10 Fotos 6x8 + Ampliación 12x18",
                    description = "10 Fotos 6x8 + Álbum/FotoBook + Ampliación 12x18 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:119.00|FotoBook:119.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A2",
                    name = "Paquete A2 - 10 Fotos 6x8 + Ampliación 16x24",
                    description = "10 Fotos 6x8 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:133.00|FotoBook:133.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A3",
                    name = "Paquete A3 - 10 Fotos 6x8 + Ampliación 24x32",
                    description = "10 Fotos 6x8 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:149.00|FotoBook:149.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A4",
                    name = "Paquete A4 - 10 Fotos 6x8 + Ampliación 24x39",
                    description = "10 Fotos 6x8 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:156.00|FotoBook:156.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A5",
                    name = "Paquete A5 - 15 Fotos 6x8 + Ampliación 12x18",
                    description = "15 Fotos 6x8 + Álbum/FotoBook + Ampliación 12x18 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:149.90|FotoBook:149.90",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A6",
                    name = "Paquete A6 - 15 Fotos 6x8 + Ampliación 16x24",
                    description = "15 Fotos 6x8 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:163.90|FotoBook:163.90",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A7",
                    name = "Paquete A7 - 15 Fotos 6x8 + Ampliación 24x32",
                    description = "15 Fotos 6x8 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:179.90|FotoBook:179.90",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A8",
                    name = "Paquete A8 - 15 Fotos 6x8 + Ampliación 24x39",
                    description = "15 Fotos 6x8 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:186.90|FotoBook:186.90",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A9",
                    name = "Paquete A9 - 10 Fotos 8x12 + Ampliación 12x18",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 12x18 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:128.00|FotoBook:128.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A10",
                    name = "Paquete A10 - 10 Fotos 8x12 + Ampliación 16x24",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:142.00|FotoBook:142.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A11",
                    name = "Paquete A11 - 10 Fotos 8x12 + Ampliación 24x32",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:158.00|FotoBook:158.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A12",
                    name = "Paquete A12 - 10 Fotos 8x12 + Ampliación 24x39",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:165.00|FotoBook:165.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A13",
                    name = "Paquete A13 - 15 Fotos 8x12 + Ampliación 12x18",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 12x18 con marco.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:163.00|FotoBook:163.00",
                    includedExtras = "3 cambios de ropa, Maquillaje y vestido mamá, Marco"
                ),
                CatalogItem(
                    code = "A14",
                    name = "Paquete A14 - 15 Fotos 8x12 + Ampliación 16x24 + Souvenirs",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco + Taza + Llavero + Pullover.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:187.60|FotoBook:187.60",
                    includedExtras = "Taza, Llavero, Pullover, Marco, Vestuario y Maquillaje"
                ),
                CatalogItem(
                    code = "A15",
                    name = "Paquete A15 - 15 Fotos 8x12 + Ampliación 24x32 + Souvenirs",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco + Taza + Llavero + Pullover.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:203.60|FotoBook:203.60",
                    includedExtras = "Taza, Llavero, Pullover, Marco, Vestuario y Maquillaje"
                ),
                CatalogItem(
                    code = "A16",
                    name = "Paquete A16 - 15 Fotos 8x12 + Ampliación 24x39 + Souvenirs",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco + Taza + Llavero + Pullover.\nIncluye 3 piezas de ropa o disfraces + maquillaje y vestido para mamá.",
                    category = "Primer Año",
                    variantsString = "Álbum:210.60|FotoBook:210.60",
                    includedExtras = "Taza, Llavero, Pullover, Marco, Vestuario y Maquillaje"
                ),

                // -------------------------------------------------------------
                // Categoría: Bodas (B1 - B12)
                // -------------------------------------------------------------
                CatalogItem(
                    code = "B1",
                    name = "Paquete B1 - 10 Fotos 8x12 + Ampliación 12x18",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 12x18 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:156.60|FotoBook:156.60",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B2",
                    name = "Paquete B2 - 10 Fotos 8x12 + Ampliación 16x24",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:167.60|FotoBook:167.60",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B3",
                    name = "Paquete B3 - 10 Fotos 8x12 + Ampliación 24x32",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:183.60|FotoBook:183.60",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B4",
                    name = "Paquete B4 - 10 Fotos 8x12 + Ampliación 24x39",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:190.60|FotoBook:190.60",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B5",
                    name = "Paquete B5 - 15 Fotos 8x12 + Ampliación 12x18",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 12x18 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:192.50|FotoBook:192.50",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B6",
                    name = "Paquete B6 - 15 Fotos 8x12 + Ampliación 16x24",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:206.50|FotoBook:206.50",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B7",
                    name = "Paquete B7 - 15 Fotos 8x12 + Ampliación 24x32",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:222.50|FotoBook:222.50",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B8",
                    name = "Paquete B8 - 15 Fotos 8x12 + Ampliación 24x39",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:229.50|FotoBook:229.50",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B9",
                    name = "Paquete B9 - 20 Fotos 8x12 + Ampliación 12x18",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 12x18 con marco.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:231.00|FotoBook:231.00",
                    includedExtras = "Maquillaje, Peinado, Alquiler vestido de novia, Marco"
                ),
                CatalogItem(
                    code = "B10",
                    name = "Paquete B10 - 20 Fotos 8x12 + Ampliación 16x24 + Souvenirs",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco + 2 Tazas + 2 Llaveros + 2 Pullovers.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:266.00|FotoBook:266.00",
                    includedExtras = "2 Tazas, 2 Llaveros, 2 Pullovers, Maquillaje, Peinado, Vestido"
                ),
                CatalogItem(
                    code = "B11",
                    name = "Paquete B11 - 20 Fotos 8x12 + Ampliación 24x32 + Souvenirs",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco + 2 Tazas + 2 Llaveros + 2 Pullovers.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:282.00|FotoBook:282.00",
                    includedExtras = "2 Tazas, 2 Llaveros, 2 Pullovers, Maquillaje, Peinado, Vestido"
                ),
                CatalogItem(
                    code = "B12",
                    name = "Paquete B12 - 20 Fotos 8x12 + Ampliación 24x39 + Souvenirs",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco + 2 Tazas + 2 Llaveros + 2 Pullovers.\nIncluye maquillaje, peinado y alquiler de vestido de bodas.",
                    category = "Bodas",
                    variantsString = "Álbum:289.00|FotoBook:289.00",
                    includedExtras = "2 Tazas, 2 Llaveros, 2 Pullovers, Maquillaje, Peinado, Vestido"
                ),

                // -------------------------------------------------------------
                // Categoría: 15 años (Qt1 - Qt18 & Oferta FX)
                // -------------------------------------------------------------
                CatalogItem(
                    code = "Qt1",
                    name = "Paquete Qt1 - 10 Fotos 8x12 + Ampliación 16x24",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:199.30|FotoBook:199.30",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt2",
                    name = "Paquete Qt2 - 10 Fotos 8x12 + Ampliación 24x32",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:215.30|FotoBook:215.30",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt3",
                    name = "Paquete Qt3 - 10 Fotos 8x12 + Ampliación 24x39",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:222.30|FotoBook:222.30",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt4",
                    name = "Paquete Qt4 - 10 Fotos 8x12 + Ampliación 16x24 + Revista 20 pág",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:362.00|FotoBook:362.00",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt5",
                    name = "Paquete Qt5 - 10 Fotos 8x12 + Ampliación 24x32 + Revista 20 pág",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:378.00|FotoBook:378.00",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt6",
                    name = "Paquete Qt6 - 10 Fotos 8x12 + Ampliación 24x39 + Revista 20 pág",
                    description = "10 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:385.00|FotoBook:385.00",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt7",
                    name = "Paquete Qt7 - 15 Fotos 8x12 + Ampliación 16x24",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:240.20|FotoBook:240.20",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt8",
                    name = "Paquete Qt8 - 15 Fotos 8x12 + Ampliación 24x32",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:256.20|FotoBook:256.20",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt9",
                    name = "Paquete Qt9 - 15 Fotos 8x12 + Ampliación 24x39",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:263.20|FotoBook:263.20",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt10",
                    name = "Paquete Qt10 - 15 Fotos 8x12 + Ampliación 16x24 + Revista 20 pág",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:402.90|FotoBook:402.90",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt11",
                    name = "Paquete Qt11 - 15 Fotos 8x12 + Ampliación 24x32 + Revista 20 pág",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:418.90|FotoBook:418.90",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt12",
                    name = "Paquete Qt12 - 15 Fotos 8x12 + Ampliación 24x39 + Revista 20 pág",
                    description = "15 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:425.90|FotoBook:425.90",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt13",
                    name = "Paquete Qt13 - 20 Fotos 8x12 + Ampliación 16x24",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:273.70|FotoBook:273.70",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt14",
                    name = "Paquete Qt14 - 20 Fotos 8x12 + Ampliación 24x32",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:289.70|FotoBook:289.70",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt15",
                    name = "Paquete Qt15 - 20 Fotos 8x12 + Ampliación 24x39",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:296.70|FotoBook:296.70",
                    includedExtras = "3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover, Marco"
                ),
                CatalogItem(
                    code = "Qt16",
                    name = "Paquete Qt16 - 20 Fotos 8x12 + Ampliación 16x24 + Revista 20 pág",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 16x24 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:436.40|FotoBook:436.40",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt17",
                    name = "Paquete Qt17 - 20 Fotos 8x12 + Ampliación 24x32 + Revista 20 pág",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x32 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:452.40|FotoBook:452.40",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Qt18",
                    name = "Paquete Qt18 - 20 Fotos 8x12 + Ampliación 24x39 + Revista 20 pág",
                    description = "20 Fotos 8x12 + Álbum/FotoBook + Ampliación 24x39 con marco + Revista 20 pág.\nIncluye maquillaje, peinado, alquiler de tres vestidos, taza, llavero y pullover.",
                    category = "15 años",
                    variantsString = "Álbum:459.40|FotoBook:459.40",
                    includedExtras = "Revista 20 pág, 3 Vestidos, Maquillaje y Peinado, Taza, Llavero, Pullover"
                ),
                CatalogItem(
                    code = "Oferta FX",
                    name = "Gran Oferta FX - Paquete VIP Máximo",
                    description = "20 Fotos 12x18 + Álbum + Revista 20 pág + Super Ampliación 39x82.67 con marco + Ampliación 24x39 con marco + Ampliación 16x24 con marco + 3 Tazas + 3 Llaveros + 3 Pullovers.",
                    category = "15 años",
                    variantsString = "Álbum Premium:795.80",
                    includedExtras = "Super Ampliación 39x82.67, Ampliación 24x39, Ampliación 16x24, Revista 20 pág, 3 Tazas, 3 Llaveros, 3 Pullovers"
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
            studioDao.insertConfig(AppConfig("whatsapp_number", "55823513"))
        }
    }
}
