package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CartItem(
    val item: CatalogItem,
    val variant: CatalogVariant,
    val quantity: Int
) {
    val subtotal: Double get() = variant.price * quantity
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = StudioRepository(db.studioDao())

    // All catalog items
    val catalogItems: StateFlow<List<CatalogItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All registered appointments
    val appointments: StateFlow<List<AppointmentEntity>> = repository.allAppointments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Configurations
    private val _whatsappNumber = MutableStateFlow("55823513")
    val whatsappNumber: StateFlow<String> = _whatsappNumber.asStateFlow()

    private val _adminPin = MutableStateFlow("1234")
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    // Textos de la portada, ficha de contacto y tasas de cambio, todo
    // editable por el administrador desde Ajustes.
    private val _studioConfig = MutableStateFlow(StudioConfig())
    val studioConfig: StateFlow<StudioConfig> = _studioConfig.asStateFlow()

    // Cuándo se exportó la última copia de seguridad. 0 = nunca.
    // Reserva a medio hacer. Vive aquí y no dentro de la pantalla del
    // calendario porque el cliente puede salir a escoger un paquete y volver:
    // si el estado se guardara en la pantalla, al salir se perdería el día y
    // la hora ya elegidos y habría que empezar de nuevo.
    private val _reserva = MutableStateFlow(ReservaBorrador())
    val reserva: StateFlow<ReservaBorrador> = _reserva.asStateFlow()

    // Contrato ya firmado al confirmar el pedido, con su firma y sus dos
    // fotos. Se guarda aquí para que el calendario no vuelva a pedir lo mismo:
    // el cliente firma una sola vez y allí solo elige el día.
    private val _contratoPendiente = MutableStateFlow<ContratoFirmado?>(null)
    val contratoPendiente: StateFlow<ContratoFirmado?> = _contratoPendiente.asStateFlow()

    private val _ultimoRespaldo = MutableStateFlow(0L)
    val ultimoRespaldo: StateFlow<Long> = _ultimoRespaldo.asStateFlow()

    // Tasa USD→CUP. En 0 la app no muestra precios en CUP, para no enseñar
    // una conversión inventada antes de que el estudio fije la tasa real.
    private val _cupRate = MutableStateFlow(0.0)
    val cupRate: StateFlow<Double> = _cupRate.asStateFlow()

    // Texto del contrato: vacío = se usa el de fábrica.
    private val _contractText = MutableStateFlow("")
    val contractText: StateFlow<String> = _contractText.asStateFlow()

    // Descuento aplicado al pedido actual, en USD.
    private val _discount = MutableStateFlow(0.0)
    val discount: StateFlow<Double> = _discount.asStateFlow()

    // Licencia mensual
    private val _licenseChecked = MutableStateFlow(false)
    val licenseChecked: StateFlow<Boolean> = _licenseChecked.asStateFlow()

    private val _licenseValid = MutableStateFlow(false)
    val licenseValid: StateFlow<Boolean> = _licenseValid.asStateFlow()

    private val _licenseIsMaster = MutableStateFlow(false)
    val licenseIsMaster: StateFlow<Boolean> = _licenseIsMaster.asStateFlow()

    // Shopping Cart
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Calculated fields
    val cartSubtotal: StateFlow<Double> = _cart.map { list ->
        list.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total a cobrar, ya con el descuento aplicado (nunca baja de cero).
    val cartTotal: StateFlow<Double> = combine(_cart, _discount) { list, desc ->
        (list.sumOf { it.subtotal } - desc).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartCount: StateFlow<Int> = _cart.map { list ->
        list.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
            loadConfigs()
            checkLicense()
        }
    }

    private suspend fun loadConfigs() {
        val num = repository.getWhatsAppNumber()
        _whatsappNumber.value = if (num.isNotBlank()) num else "55823513"
        _adminPin.value = repository.getAdminPin()
        _cupRate.value = repository.getCupRate()
        _contractText.value = repository.getContractText()
        _studioConfig.value = repository.getStudioConfig()
        _ultimoRespaldo.value = repository.getUltimoRespaldo()
    }

    fun updateCupRate(rate: Double) {
        viewModelScope.launch {
            repository.setCupRate(rate)
            _cupRate.value = rate
            _studioConfig.value = repository.getStudioConfig()
        }
    }

    fun updateStudioConfig(config: StudioConfig) {
        viewModelScope.launch {
            repository.saveStudioConfig(config)
            _studioConfig.value = config
            _cupRate.value = config.tasas.firstOrNull { it.id == StudioConfig.ID_CUP }?.tasa ?: 0.0
        }
    }

    /** Devuelve los textos de la portada a los de fábrica. */
    fun resetStudioTexts() {
        viewModelScope.launch {
            repository.resetStudioTexts()
            _studioConfig.value = repository.getStudioConfig()
        }
    }

    /** Cambia un campo de la reserva en curso. */
    fun actualizarReserva(cambio: (ReservaBorrador) -> ReservaBorrador) {
        _reserva.value = cambio(_reserva.value)
    }

    /** Guarda el contrato firmado en el pedido, a la espera de la fecha. */
    fun guardarContratoPendiente(contrato: ContratoFirmado?) {
        _contratoPendiente.value = contrato
    }

    /** Se llama al terminar de reservar, para que la próxima empiece limpia. */
    fun limpiarReserva() {
        _reserva.value = ReservaBorrador()
        _contratoPendiente.value = null
    }

    fun updateContractText(text: String) {
        viewModelScope.launch {
            repository.setContractText(text)
            _contractText.value = text
        }
    }

    fun setDiscount(amount: Double) {
        _discount.value = amount.coerceAtLeast(0.0)
    }

    /**
     * Equivalencias del importe en las formas de pago que el administrador
     * dejó visibles. Lista vacía cuando no hay ninguna tasa configurada, para
     * que la interfaz no muestre una conversión inventada.
     */
    fun equivalencias(usd: Double): List<String> = _studioConfig.value.equivalencias(usd)

    /**
     * Primera equivalencia, en una línea. Devuelve null si no hay tasas, para
     * que quien la use simplemente no pinte nada.
     */
    fun cupLabel(usd: Double): String? = equivalencias(usd).firstOrNull()

    /** Todas las equivalencias en una sola línea, para los mensajes. */
    fun equivalenciasLinea(usd: Double): String? = _studioConfig.value.equivalenciasLinea(usd)

    private val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val formatoNombreArchivo = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)

    // Revisa si la app está activada para el mes actual (o desbloqueada con llave maestra)
    private suspend fun checkLicense() {
        val masterUnlocked = repository.getConfigValue(LicenseManager.CONFIG_KEY_MASTER) == "true"
        if (masterUnlocked) {
            _licenseIsMaster.value = true
            _licenseValid.value = true
        } else {
            val savedPeriod = repository.getConfigValue(LicenseManager.CONFIG_KEY_PERIOD)
            _licenseValid.value = savedPeriod == LicenseManager.currentPeriod()
        }
        _licenseChecked.value = true
    }

    /** Intenta activar la app con un código mensual o la llave maestra. Devuelve true si fue aceptado. */
    fun activateLicense(inputCode: String): Boolean {
        val trimmed = inputCode.trim()

        if (trimmed == LicenseManager.MASTER_KEY) {
            viewModelScope.launch {
                repository.setConfigValue(LicenseManager.CONFIG_KEY_MASTER, "true")
            }
            _licenseIsMaster.value = true
            _licenseValid.value = true
            return true
        }

        if (trimmed == LicenseManager.expectedCodeForCurrentMonth()) {
            viewModelScope.launch {
                repository.setConfigValue(LicenseManager.CONFIG_KEY_PERIOD, LicenseManager.currentPeriod())
            }
            _licenseValid.value = true
            return true
        }

        return false
    }

    // Cart Management
    fun addToCart(item: CatalogItem, variant: CatalogVariant, quantity: Int) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.item.name == item.name && it.variant.name == variant.name && it.item.category == item.category
        }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(CartItem(item, variant, quantity))
        }
        _cart.value = currentList
    }

    fun addCustomToCart(title: String, category: String, variantName: String, price: Double, quantity: Int, description: String = "") {
        val customItem = CatalogItem(
            id = 0,
            name = title,
            description = description,
            category = category,
            variantsString = "$variantName:$price"
        )
        addToCart(customItem, CatalogVariant(variantName, price), quantity)
    }

    /**
     * Quita una unidad de un extra del pedido. Se usa desde "Agregar algo
     * más", donde un toque añade y el signo menos deshace: si baja de uno, la
     * línea desaparece del pedido.
     */
    fun quitarUnoDelCarrito(title: String, category: String, variantName: String) {
        val actual = _cart.value.firstOrNull {
            it.item.name == title && it.variant.name == variantName && it.item.category == category
        } ?: return
        updateCartQuantity(actual, actual.quantity - 1)
    }

    fun removeFromCart(cartItem: CartItem) {
        val currentList = _cart.value.filterNot {
            it.item.name == cartItem.item.name && it.variant.name == cartItem.variant.name && it.item.category == cartItem.item.category
        }
        _cart.value = currentList
    }

    fun updateCartQuantity(cartItem: CartItem, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(cartItem)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.item.name == cartItem.item.name && it.variant.name == cartItem.variant.name && it.item.category == cartItem.item.category
        }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = newQty)
        }
        _cart.value = currentList
    }

    fun clearCart() {
        _cart.value = emptyList()
        _discount.value = 0.0
    }

    fun getCartSummaryText(): String {
        if (_cart.value.isEmpty()) return ""
        val itemsSummary = _cart.value.joinToString("; ") {
            "${it.item.name} (${it.variant.name}) x${it.quantity} [\$${String.format("%.2f", it.subtotal)}]"
        }
        return "Total: \$${String.format("%.2f", cartTotal.value)} — $itemsSummary"
    }

    // Config updates
    fun updateWhatsAppNumber(num: String) {
        viewModelScope.launch {
            repository.setWhatsAppNumber(num)
            _whatsappNumber.value = num
        }
    }

    fun updateAdminPin(pin: String) {
        viewModelScope.launch {
            repository.setAdminPin(pin)
            _adminPin.value = pin
        }
    }

    // Catalog CRUD
    fun saveCatalogItem(
        id: Int,
        code: String = "",
        name: String,
        description: String,
        category: String,
        variants: List<CatalogVariant>,
        includedExtras: String = "",
        imageBytes: ByteArray?
    ) {
        viewModelScope.launch {
            // Compress image if provided
            val finalImageBytes = imageBytes?.let { resizeAndCompressImage(it) }
            val item = CatalogItem(
                id = id,
                code = code,
                name = name,
                description = description,
                category = category,
                variantsString = CatalogItem.createVariantsString(variants),
                includedExtras = includedExtras,
                imageBytes = finalImageBytes ?: (if (id != 0) repository.getItemById(id)?.imageBytes else null)
            )
            repository.insertItem(item)
        }
    }

    fun duplicateCatalogItem(item: CatalogItem) {
        viewModelScope.launch {
            val duplicated = item.copy(
                id = 0,
                name = "${item.name} (Copia)",
                code = if (item.code.isNotBlank()) "${item.code}-C" else ""
            )
            repository.insertItem(duplicated)
        }
    }

    fun deleteCatalogItem(id: Int) {
        viewModelScope.launch {
            repository.deleteItemById(id)
            // Clean cart if deleted item was inside
            _cart.value = _cart.value.filterNot { it.item.id == id && id != 0 }
        }
    }

    // Appointments CRUD
    fun saveAppointment(
        fecha: String,
        hora: String,
        nombreCliente: String,
        telefono: String,
        detalleSeleccion: String,
        notas: String,
        firmaBytes: ByteArray?,
        fotoClienteBytes: ByteArray? = null,
        fotoCliente2Bytes: ByteArray? = null,
        terminosAceptados: Boolean = true,
        montoAcordado: Double = 0.0,
        anticipoPagado: Double = 0.0,
        onSuccess: (AppointmentEntity) -> Unit
    ) {
        viewModelScope.launch {
            val appointment = AppointmentEntity(
                fecha = fecha,
                hora = hora,
                nombreCliente = nombreCliente,
                telefono = telefono,
                detalleSeleccion = detalleSeleccion,
                notas = notas,
                firmaBytes = firmaBytes,
                fotoClienteBytes = fotoClienteBytes,
                fotoCliente2Bytes = fotoCliente2Bytes,
                terminosAceptados = terminosAceptados,
                montoAcordado = montoAcordado,
                anticipoPagado = anticipoPagado
            )
            val generatedId = repository.insertAppointment(appointment)
            val savedAppointment = appointment.copy(id = generatedId.toInt())
            onSuccess(savedAppointment)
        }
    }

    fun deleteAppointment(id: Int) {
        viewModelScope.launch {
            repository.deleteAppointmentById(id)
        }
    }

    fun updateAppointmentPayment(id: Int, montoAcordado: Double, anticipoPagado: Double) {
        viewModelScope.launch {
            repository.updateAppointmentPayment(id, montoAcordado, anticipoPagado)
        }
    }

    fun updateAppointmentStatus(id: Int, estado: String) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(id, estado)
        }
    }

    // Verify PIN
    fun verifyPin(entered: String): Boolean {
        return entered == _adminPin.value
    }

    /**
     * Arma el contrato de una cita como PDF y devuelve el archivo listo para
     * compartir. Es el documento que se le entrega al cliente.
     */
    fun generarContratoPdf(carpeta: File, cita: AppointmentEntity): File {
        val texto = _contractText.value.ifBlank { FULL_CONTRACT_TEXT }
        return ContratoPdf.generar(
            destino = File(carpeta, ContratoPdf.nombreArchivo(cita)),
            cita = cita,
            config = _studioConfig.value,
            textoContrato = texto,
            equivalencias = { usd -> equivalenciasLinea(usd) }
        )
    }

    /**
     * Vuelca la agenda y el catálogo en un archivo .xlsx dentro de la carpeta
     * temporal, y devuelve el archivo para compartirlo. Las fotos y las firmas
     * no caben en una hoja de cálculo: para eso está el respaldo JSON.
     */
    suspend fun exportarExcel(carpeta: File): File {
        val citas = repository.getAllAppointmentsOnce()
            .sortedWith(compareBy({ fechaComparable(it.fecha) }, { horaComparable(it.hora) }))

        val hojaCitas = ExcelExport.Hoja(
            nombre = "Citas",
            encabezados = listOf(
                "N.º", "Fecha", "Hora", "Cliente", "Teléfono", "Selección", "Notas",
                "Estado", "Acordado USD", "Anticipo USD", "Saldo USD",
                "Firmada", "Fotos", "Registrada el"
            ),
            filas = citas.map { c ->
                listOf(
                    ExcelExport.numero(c.id.toDouble()),
                    ExcelExport.texto(c.fecha),
                    ExcelExport.texto(c.hora),
                    ExcelExport.texto(c.nombreCliente),
                    ExcelExport.texto(c.telefono),
                    ExcelExport.texto(c.detalleSeleccion),
                    ExcelExport.texto(c.notas),
                    ExcelExport.texto(c.estado),
                    ExcelExport.numero(c.montoAcordado),
                    ExcelExport.numero(c.anticipoPagado),
                    ExcelExport.numero(c.saldoPendiente),
                    ExcelExport.siNo(c.firmaBytes != null),
                    ExcelExport.numero(
                        listOfNotNull(c.fotoClienteBytes, c.fotoCliente2Bytes).size.toDouble()
                    ),
                    ExcelExport.texto(formatoFechaHora.format(Date(c.createdAt)))
                )
            }
        )

        // Una fila por variante: así se puede filtrar y sumar por precio.
        val filasCatalogo = mutableListOf<List<ExcelExport.Celda>>()
        catalogItems.value.forEach { item ->
            val variantes = item.getVariants()
            if (variantes.isEmpty()) {
                filasCatalogo.add(
                    listOf(
                        ExcelExport.texto(item.code),
                        ExcelExport.texto(item.name),
                        ExcelExport.texto(item.category),
                        ExcelExport.texto(item.description),
                        ExcelExport.texto(item.includedExtras),
                        ExcelExport.texto(""),
                        ExcelExport.numero(0.0)
                    )
                )
            } else {
                variantes.forEach { v ->
                    filasCatalogo.add(
                        listOf(
                            ExcelExport.texto(item.code),
                            ExcelExport.texto(item.name),
                            ExcelExport.texto(item.category),
                            ExcelExport.texto(item.description),
                            ExcelExport.texto(item.includedExtras),
                            ExcelExport.texto(v.name),
                            ExcelExport.numero(v.price)
                        )
                    )
                }
            }
        }

        val hojaCatalogo = ExcelExport.Hoja(
            nombre = "Catalogo",
            encabezados = listOf(
                "Código", "Paquete", "Categoría", "Descripción", "Incluye",
                "Variante", "Precio USD"
            ),
            filas = filasCatalogo
        )

        // Resumen corto, para no tener que sacar cuentas a mano.
        val totalAcordado = citas.sumOf { it.montoAcordado }
        val totalCobrado = citas.sumOf { it.anticipoPagado }
        val hojaResumen = ExcelExport.Hoja(
            nombre = "Resumen",
            encabezados = listOf("Concepto", "Valor"),
            filas = buildList {
                add(listOf(ExcelExport.texto("Reservaciones registradas"), ExcelExport.numero(citas.size.toDouble())))
                add(listOf(ExcelExport.texto("Paquetes en el catálogo"), ExcelExport.numero(catalogItems.value.size.toDouble())))
                add(listOf(ExcelExport.texto("Total acordado USD"), ExcelExport.numero(totalAcordado)))
                add(listOf(ExcelExport.texto("Total cobrado USD"), ExcelExport.numero(totalCobrado)))
                add(listOf(ExcelExport.texto("Saldo por cobrar USD"), ExcelExport.numero(totalAcordado - totalCobrado)))
                EstadoCita.TODOS.forEach { estado ->
                    add(
                        listOf(
                            ExcelExport.texto("Citas en \"$estado\""),
                            ExcelExport.numero(citas.count { it.estado == estado }.toDouble())
                        )
                    )
                }
            }
        )

        val nombre = "FXestudio_${formatoNombreArchivo.format(Date())}.xlsx"
        return ExcelExport.escribir(File(carpeta, nombre), listOf(hojaResumen, hojaCitas, hojaCatalogo))
    }

    // Backup Export
    suspend fun exportBackupJson(): String {
        val configs = repository.getAllConfigs()
        val items = catalogItems.value

        val root = JSONObject()

        // Configs array
        val configsArray = JSONArray()
        for (config in configs) {
            val obj = JSONObject()
            obj.put("key", config.key)
            obj.put("value", config.value)
            configsArray.put(obj)
        }
        root.put("configs", configsArray)

        // Items array
        val itemsArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("code", item.code)
            obj.put("name", item.name)
            obj.put("description", item.description)
            obj.put("category", item.category)
            obj.put("variantsString", item.variantsString)
            obj.put("includedExtras", item.includedExtras)
            val base64Img = item.imageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
            obj.put("imageBytes", base64Img)
            itemsArray.put(obj)
        }
        root.put("items", itemsArray)

        // Las citas firmadas son documentos legales: se incluyen en el respaldo
        // junto con la imagen de la firma, para no perderlas si falla el equipo.
        val appointmentsArray = JSONArray()
        for (appt in repository.getAllAppointmentsOnce()) {
            val obj = JSONObject()
            obj.put("fecha", appt.fecha)
            obj.put("hora", appt.hora)
            obj.put("nombreCliente", appt.nombreCliente)
            obj.put("telefono", appt.telefono)
            obj.put("detalleSeleccion", appt.detalleSeleccion)
            obj.put("notas", appt.notas)
            obj.put("terminosAceptados", appt.terminosAceptados)
            obj.put("createdAt", appt.createdAt)
            obj.put("montoAcordado", appt.montoAcordado)
            obj.put("anticipoPagado", appt.anticipoPagado)
            obj.put("estado", appt.estado)
            val firma = appt.firmaBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
            obj.put("firmaBytes", firma)
            val fotoCliente = appt.fotoClienteBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
            obj.put("fotoClienteBytes", fotoCliente)
            val fotoCliente2 = appt.fotoCliente2Bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
            obj.put("fotoCliente2Bytes", fotoCliente2)
            appointmentsArray.put(obj)
        }
        root.put("appointments", appointmentsArray)

        // Queda anotado para poder avisar cuando lleve mucho sin hacerse.
        val ahora = System.currentTimeMillis()
        repository.setUltimoRespaldo(ahora)
        _ultimoRespaldo.value = ahora

        return root.toString(2)
    }

    // Backup Import
    fun importBackupJson(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                val configsArray = root.getJSONArray("configs")
                val importedConfigs = mutableListOf<AppConfig>()
                for (i in 0 until configsArray.length()) {
                    val obj = configsArray.getJSONObject(i)
                    importedConfigs.add(AppConfig(obj.getString("key"), obj.getString("value")))
                }

                val itemsArray = root.getJSONArray("items")
                val importedItems = mutableListOf<CatalogItem>()
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val base64 = obj.optString("imageBytes", "")
                    val imgBytes = if (base64.isNotEmpty()) {
                        try {
                            Base64.decode(base64, Base64.NO_WRAP)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                    importedItems.add(
                        CatalogItem(
                            code = obj.optString("code", ""),
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            category = obj.getString("category"),
                            variantsString = obj.getString("variantsString"),
                            includedExtras = obj.optString("includedExtras", ""),
                            imageBytes = imgBytes
                        )
                    )
                }

                repository.importBackup(importedItems, importedConfigs)

                // Solo se toca la agenda si el respaldo la trae. Así, restaurar
                // un respaldo antiguo (anterior a esta versión) no borra las
                // citas firmadas que ya haya en el equipo.
                if (root.has("appointments")) {
                    val apptArray = root.getJSONArray("appointments")
                    val importedAppointments = mutableListOf<AppointmentEntity>()
                    for (i in 0 until apptArray.length()) {
                        val obj = apptArray.getJSONObject(i)
                        val firmaB64 = obj.optString("firmaBytes", "")
                        val firma = if (firmaB64.isNotEmpty()) {
                            try {
                                Base64.decode(firmaB64, Base64.NO_WRAP)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                        fun foto(clave: String): ByteArray? {
                            val b64 = obj.optString(clave, "")
                            return if (b64.isEmpty()) null else try {
                                Base64.decode(b64, Base64.NO_WRAP)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        // Un respaldo anterior a esta versión no trae la
                        // segunda foto: queda vacía, no rompe la importación.
                        val fotoCliente = foto("fotoClienteBytes")
                        val fotoCliente2 = foto("fotoCliente2Bytes")
                        importedAppointments.add(
                            AppointmentEntity(
                                fecha = obj.getString("fecha"),
                                hora = obj.getString("hora"),
                                nombreCliente = obj.getString("nombreCliente"),
                                telefono = obj.getString("telefono"),
                                detalleSeleccion = obj.getString("detalleSeleccion"),
                                notas = obj.optString("notas", ""),
                                firmaBytes = firma,
                                fotoClienteBytes = fotoCliente,
                                fotoCliente2Bytes = fotoCliente2,
                                terminosAceptados = obj.optBoolean("terminosAceptados", true),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                montoAcordado = obj.optDouble("montoAcordado", 0.0),
                                anticipoPagado = obj.optDouble("anticipoPagado", 0.0),
                                estado = obj.optString("estado", EstadoCita.RESERVADA)
                            )
                        )
                    }
                    repository.replaceAppointments(importedAppointments)
                }

                loadConfigs()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al procesar el archivo JSON")
            }
        }
    }

    // Image helper: compress & resize
    fun resizeAndCompressImage(bytes: ByteArray, maxWidth: Int = 800, maxHeight: Int = 800): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var inSampleSize = 1
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight

            if (srcWidth > maxWidth || srcHeight > maxHeight) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while (halfWidth / inSampleSize >= maxWidth && halfHeight / inSampleSize >= maxHeight) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            val ratio = Math.min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
            val finalBitmap = if (ratio < 1.0) {
                val dstWidth = (bitmap.width * ratio).toInt()
                val dstHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deja el número listo para WhatsApp: solo dígitos y con el 53 de Cuba
     * delante cuando viene de 8 cifras, como se marca aquí.
     */
    private fun normalizarTelefono(numero: String): String {
        val raw = numero.filter { it.isDigit() }
        return when {
            raw.isEmpty() -> "5355823513"
            raw.length == 8 -> "53$raw"
            else -> raw
        }
    }

    /** El número del estudio, listo para WhatsApp. */
    private fun telefonoEstudio(): String = normalizarTelefono(whatsappNumber.value)

    /**
     * Número de destino del mensaje. WhatsApp solo abre un chat por vez, así
     * que la copia al cliente se envía en un segundo toque.
     */
    private fun destinoWhatsApp(telefonoCliente: String): String =
        if (telefonoCliente.isNotBlank()) normalizarTelefono(telefonoCliente) else telefonoEstudio()

    // Build the WhatsApp message for Shopping Cart Orders with Studio Info & Signed Terms
    fun generateWhatsAppUri(
        clientName: String = "",
        clientPhone: String = "",
        // Vacío = va al estudio. Con número = va al chat del cliente.
        enviarACliente: Boolean = false
    ): String {
        val phoneFiltered = if (enviarACliente) destinoWhatsApp(clientPhone) else telefonoEstudio()

        val sb = StringBuilder()
        if (enviarACliente) {
            sb.append("¡Gracias por elegir FXestudio! 💙\n")
            sb.append("Esta es su copia del pedido:\n\n")
        }
        sb.append("📸 *FXESTUDIO — Pedido y Cotización de Sesión*\n")
        sb.append("📍 _Bayamo, Granma, Cuba_\n")
        sb.append("-------------------------------------------\n")
        if (clientName.isNotBlank()) {
            sb.append("👤 *Cliente:* $clientName\n")
        }
        if (clientPhone.isNotBlank()) {
            sb.append("📞 *Teléfono:* $clientPhone\n")
        }
        sb.append("-------------------------------------------\n")
        sb.append("📋 *Detalle del Pedido:*\n\n")

        cart.value.forEach { item ->
            val codeStr = if (item.item.code.isNotBlank()) "[${item.item.code}] " else ""
            sb.append("• *$codeStr${item.item.name}*\n")
            sb.append("  Categoría: ${item.item.category}\n")
            sb.append("  Variante/Formato: ${item.variant.name}\n")
            if (item.item.includedExtras.isNotBlank()) {
                sb.append("  Incluye: ${item.item.includedExtras}\n")
            }
            sb.append("  Cantidad: ${item.quantity}  |  Precio unitario: $${String.format("%.2f", item.variant.price)}\n")
            sb.append("  *Subtotal:* $${String.format("%.2f", item.subtotal)}\n\n")
        }

        sb.append("-------------------------------------------\n")
        if (discount.value > 0.0) {
            sb.append("Subtotal: $${String.format("%.2f", cartSubtotal.value)} USD\n")
            sb.append("🏷️ *Descuento aplicado: -$${String.format("%.2f", discount.value)} USD*\n")
        }
        sb.append("💰 *TOTAL GENERAL: $${String.format("%.2f", cartTotal.value)} USD*\n")
        equivalenciasLinea(cartTotal.value)?.let { sb.append("💱 *$it*\n") }
        sb.append("💵 _Se acepta Zelle y CUP al cambio del día._\n")
        sb.append("-------------------------------------------\n")
        sb.append("✍️ *Contrato de Sesión Fotográfica:* ✅ FIRMADO Y ACEPTADO\n")
        sb.append("📌 *Estudio:* Edificio 29, Apt 7, Jesús Menéndez, frente a la Calesa, Bayamo.\n")
        sb.append("🕒 *Horario:* Lun - Sáb, 9:00 AM – 5:00 PM\n")
        sb.append("📞 *Contacto:* 55823513 / 56826099")

        val encodedText = try {
            URLEncoder.encode(sb.toString(), "UTF-8")
        } catch (e: Exception) {
            sb.toString()
        }

        return "https://api.whatsapp.com/send?phone=$phoneFiltered&text=$encodedText"
    }

    /**
     * Recordatorio corto de cobro, al chat del cliente. Se manda desde la
     * pestaña "Dinero": no repite el contrato entero, solo lo que hace falta
     * para que la persona sepa cuánto debe y por qué.
     */
    fun generateCobroWhatsAppUri(appointment: AppointmentEntity): String {
        val phoneFiltered = destinoWhatsApp(appointment.telefono)

        val sb = StringBuilder()
        sb.append("Hola ${appointment.nombreCliente} 👋\n")
        sb.append("Le escribimos de *FXestudio* (Bayamo).\n\n")
        sb.append("📸 *Su sesión:* ${appointment.detalleSeleccion}\n")
        sb.append("📆 *Fecha:* ${appointment.fecha} — ${appointment.hora}\n")
        sb.append("🔧 *Estado del trabajo:* ${appointment.estado}\n")
        sb.append("-------------------------------------------\n")
        sb.append("💰 *Total acordado:* $${String.format("%.2f", appointment.montoAcordado)} USD\n")
        sb.append("✅ *Ya pagado:* $${String.format("%.2f", appointment.anticipoPagado)} USD\n")
        sb.append("🔸 *Le queda por pagar:* $${String.format("%.2f", appointment.saldoPendiente)} USD")
        equivalenciasLinea(appointment.saldoPendiente)?.let { sb.append("  ($it)") }
        sb.append("\n-------------------------------------------\n")
        sb.append("Puede pagar en CUP al cambio del día, por Zelle o transferencia.\n")
        sb.append("📌 Edificio 29, Apt 7, Jesús Menéndez, frente a la Calesa, Bayamo.\n")
        sb.append("🕒 Lun - Sáb, 9:00 AM – 5:00 PM\n")
        sb.append("¡Gracias por confiar en nosotros! 💙")

        val encodedText = try {
            URLEncoder.encode(sb.toString(), "UTF-8")
        } catch (e: Exception) {
            sb.toString()
        }

        return "https://api.whatsapp.com/send?phone=$phoneFiltered&text=$encodedText"
    }

    // Build WhatsApp message for Appointments
    fun generateAppointmentWhatsAppUri(
        appointment: AppointmentEntity,
        // true = se abre el chat del cliente con su copia de la reservación.
        enviarACliente: Boolean = false
    ): String {
        val phoneFiltered =
            if (enviarACliente) destinoWhatsApp(appointment.telefono) else telefonoEstudio()

        val sb = StringBuilder()
        if (enviarACliente) {
            sb.append("¡Gracias por reservar con FXestudio! 💙\n")
            sb.append("Esta es su copia de la reservación:\n\n")
        }
        sb.append("📅 *FXESTUDIO — Reservación de Cita / Sesión*\n")
        sb.append("📍 _Bayamo, Granma, Cuba_\n")
        sb.append("-------------------------------------------\n")
        sb.append("👤 *Cliente:* ${appointment.nombreCliente}\n")
        sb.append("📞 *Teléfono:* ${appointment.telefono}\n")
        sb.append("📆 *Fecha elegida:* ${appointment.fecha}\n")
        sb.append("⏰ *Hora:* ${appointment.hora}\n")
        sb.append("-------------------------------------------\n")
        sb.append("📸 *Opción o Selección:*\n")
        sb.append("${appointment.detalleSeleccion}\n\n")
        if (appointment.notas.isNotBlank()) {
            sb.append("📝 *Notas adicionales:* ${appointment.notas}\n")
        }
        if (appointment.montoAcordado > 0.0) {
            sb.append("-------------------------------------------\n")
            sb.append("💰 *Monto acordado:* $${String.format("%.2f", appointment.montoAcordado)} USD")
            equivalenciasLinea(appointment.montoAcordado)?.let { sb.append("  ($it)") }
            sb.append("\n")
            sb.append("✅ *Anticipo pagado:* $${String.format("%.2f", appointment.anticipoPagado)} USD\n")
            sb.append("🔸 *Saldo pendiente:* $${String.format("%.2f", appointment.saldoPendiente)} USD")
            equivalenciasLinea(appointment.saldoPendiente)?.let { sb.append("  ($it)") }
            sb.append("\n")
        }
        sb.append("-------------------------------------------\n")
        sb.append("✍️ *Contrato de Sesión Fotográfica:* ✅ FIRMADO Y ACEPTADO\n")
        sb.append("📌 *Dirección:* Edificio 29, Apt 7, Jesús Menéndez, frente a la Calesa, Bayamo.\n")
        sb.append("🕒 *Horario del Estudio:* Lun - Sáb, 9:00 AM – 5:00 PM\n")
        sb.append("📞 *Contacto:* 55823513 / 56826099\n")
        sb.append("💵 *Precios en USD* (Zelle o CUP al cambio del día)")

        val encodedText = try {
            URLEncoder.encode(sb.toString(), "UTF-8")
        } catch (e: Exception) {
            sb.toString()
        }

        return "https://api.whatsapp.com/send?phone=$phoneFiltered&text=$encodedText"
    }
}
