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

        // La frase de la portada es la excepción: si el administrador la
        // borra a propósito, la portada se queda sin frase. Por eso aquí
        // manda que la clave exista, aunque su valor esté vacío; cuando no
        // existe (nunca se tocó, o se restauraron los textos) vale la de
        // fábrica. Restaurar borra la fila, no la guarda vacía.
        fun txtOpcional(clave: String, porDefecto: String) =
            guardado[clave] ?: porDefecto

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
            frasePortada = txtOpcional(KEY_FRASE, d.frasePortada),
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
            temaId = txt(KEY_TEMA, d.temaId),
            incluyeEnOfertas = txtOpcional(KEY_INCLUYE_OFERTAS, d.incluyeEnOfertas),
            medidasEnPulgadas = guardado[KEY_MEDIDAS_PULGADAS]?.toBooleanStrictOrNull()
                ?: d.medidasEnPulgadas,
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
            KEY_FACEBOOK to c.facebookUrl,
            KEY_TEMA to c.temaId,
            KEY_INCLUYE_OFERTAS to c.incluyeEnOfertas,
            KEY_MEDIDAS_PULGADAS to c.medidasEnPulgadas.toString()
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
        ).forEach { studioDao.deleteConfig(it) }
    }

    /** Los servicios de una lista: los del administrador, o los de fábrica. */
    suspend fun getServicios(menu: MenuServicios): List<Carpeta> =
        ServiciosJson.leer(studioDao.getConfig(menu.clave)?.value)
            ?: ServiciosDeFabrica.para(menu)

    suspend fun saveServicios(menu: MenuServicios, carpetas: List<Carpeta>) {
        studioDao.insertConfig(AppConfig(menu.clave, ServiciosJson.escribir(carpetas)))
    }

    /** Borra la lista guardada: vuelve la de fábrica. */
    suspend fun resetServicios(menu: MenuServicios) {
        studioDao.deleteConfig(menu.clave)
    }

    /** Cuándo se exportó el último respaldo. 0 = nunca. */
    suspend fun getUltimoRespaldo(): Long =
        studioDao.getConfig(KEY_ULTIMO_RESPALDO)?.value?.toLongOrNull() ?: 0L

    suspend fun setUltimoRespaldo(cuando: Long) {
        studioDao.insertConfig(AppConfig(KEY_ULTIMO_RESPALDO, cuando.toString()))
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
        const val KEY_TEMA = "app_tema"
        const val KEY_INCLUYE_OFERTAS = "ofertas_incluyen"
        const val KEY_ACTUALIZACION = "catalogo_actualizacion"
        const val KEY_MEDIDAS_PULGADAS = "medidas_en_pulgadas"
        const val KEY_ULTIMO_RESPALDO = "ultimo_respaldo"
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

    /**
     * Aplica una sola vez la actualización de precios del 12-9-26 (ver
     * [ActualizacionCatalogo]) sobre lo que ya hay guardado en el teléfono.
     */
    suspend fun aplicarActualizacionCatalogo() {
        if (studioDao.getConfig(KEY_ACTUALIZACION)?.value == ActualizacionCatalogo.VERSION) return

        val items = studioDao.getAllItems().firstOrNull().orEmpty()
        for (item in items) {
            val precio = ActualizacionCatalogo.PRECIOS[item.code] ?: continue
            var nuevo = ActualizacionCatalogo.conPrecio(item, precio)
            if (item.category == "Bodas") {
                nuevo = nuevo.copy(
                    description = ActualizacionCatalogo.descripcionBodas(item.description),
                    includedExtras = ActualizacionCatalogo.BODAS_INCLUYE
                )
            }
            studioDao.insertItem(nuevo)
        }
        // Los collages se añaden solo si no existen ya con ese código.
        ActualizacionCatalogo.COLLAGES.forEach { collage ->
            if (items.none { it.code == collage.code }) studioDao.insertItem(collage)
        }

        // Las listas de servicios guardadas por el admin. Si no hay ninguna
        // guardada no se toca nada: la de fábrica ya trae los precios nuevos.
        MenuServicios.values().forEach { menu ->
            val guardada = ServiciosJson.leer(studioDao.getConfig(menu.clave)?.value)
            if (guardada != null) {
                saveServicios(menu, ActualizacionCatalogo.aplicarAServicios(menu, guardada))
            }
        }

        studioDao.insertConfig(AppConfig(KEY_ACTUALIZACION, ActualizacionCatalogo.VERSION))
    }

    suspend fun prepopulateIfNeeded() {
        val items = studioDao.getAllItems().firstOrNull()
        if (items.isNullOrEmpty()) {
            // Los paquetes de fábrica están en CatalogoDeFabrica.kt.
            val defaultItems = CatalogoDeFabrica.paquetes()
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
