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
import java.net.URLEncoder

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

    // Configurations
    private val _whatsappNumber = MutableStateFlow("")
    val whatsappNumber: StateFlow<String> = _whatsappNumber.asStateFlow()

    private val _adminPin = MutableStateFlow("1234")
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

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
    val cartTotal: StateFlow<Double> = _cart.map { list ->
        list.sumOf { it.subtotal }
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
        _whatsappNumber.value = repository.getWhatsAppNumber()
        _adminPin.value = repository.getAdminPin()
    }

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
        val index = currentList.indexOfFirst { it.item.id == item.id && it.variant.name == variant.name }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(CartItem(item, variant, quantity))
        }
        _cart.value = currentList
    }

    fun removeFromCart(cartItem: CartItem) {
        val currentList = _cart.value.filterNot { it.item.id == cartItem.item.id && it.variant.name == cartItem.variant.name }
        _cart.value = currentList
    }

    fun updateCartQuantity(cartItem: CartItem, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(cartItem)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.item.id == cartItem.item.id && it.variant.name == cartItem.variant.name }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = newQty)
        }
        _cart.value = currentList
    }

    fun clearCart() {
        _cart.value = emptyList()
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
        name: String,
        description: String,
        category: String,
        variants: List<CatalogVariant>,
        imageBytes: ByteArray?
    ) {
        viewModelScope.launch {
            // Compress image if provided
            val finalImageBytes = imageBytes?.let { resizeAndCompressImage(it) }
            val item = CatalogItem(
                id = id,
                name = name,
                description = description,
                category = category,
                variantsString = CatalogItem.createVariantsString(variants),
                imageBytes = finalImageBytes ?: (if (id != 0) repository.getItemById(id)?.imageBytes else null)
            )
            repository.insertItem(item)
        }
    }

    fun deleteCatalogItem(id: Int) {
        viewModelScope.launch {
            repository.deleteItemById(id)
            // Clean cart if deleted item was inside
            _cart.value = _cart.value.filterNot { it.item.id == id }
        }
    }

    // Verify PIN
    fun verifyPin(entered: String): Boolean {
        return entered == _adminPin.value
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
            obj.put("name", item.name)
            obj.put("description", item.description)
            obj.put("category", item.category)
            obj.put("variantsString", item.variantsString)
            val base64Img = item.imageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
            obj.put("imageBytes", base64Img)
            itemsArray.put(obj)
        }
        root.put("items", itemsArray)

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
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            category = obj.getString("category"),
                            variantsString = obj.getString("variantsString"),
                            imageBytes = imgBytes
                        )
                    )
                }

                repository.importBackup(importedItems, importedConfigs)
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

    // Build the WhatsApp message and return deep-link URI
    fun generateWhatsAppUri(): String {
        val phoneFiltered = whatsappNumber.value.filter { it.isDigit() }
        val sb = StringBuilder()
        sb.append("📸 *Presupuesto - Estudio de Fotos*\n")
        sb.append("-------------------------------------------\n")
        sb.append("Cliente solicita cotización para:\n\n")

        cart.value.forEach { item ->
            sb.append("• *${item.item.name}*\n")
            sb.append("  Var: ${item.variant.name}\n")
            sb.append("  Cant: ${item.quantity} | Unit: $${String.format("%.2f", item.variant.price)}\n")
            sb.append("  Subtotal: $${String.format("%.2f", item.subtotal)}\n\n")
        }

        sb.append("-------------------------------------------\n")
        sb.append("*TOTAL GENERAL: $${String.format("%.2f", cartTotal.value)}*\n")
        sb.append("-------------------------------------------\n")
        sb.append("_Pedido generado localmente desde la tablet del estudio._")

        val encodedText = try {
            URLEncoder.encode(sb.toString(), "UTF-8")
        } catch (e: Exception) {
            sb.toString()
        }

        return "https://api.whatsapp.com/send?phone=$phoneFiltered&text=$encodedText"
    }
}
