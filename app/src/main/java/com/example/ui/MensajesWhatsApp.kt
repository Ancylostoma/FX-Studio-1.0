package com.example.ui

import com.example.data.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Los mensajes de WhatsApp que arma la app: el pedido (al estudio y la copia
 * al cliente), el recordatorio de cobro y el aviso de la reservación.
 *
 * Son funciones de extensión del ViewModel: se llaman igual que antes
 * (viewModel.generateWhatsAppUri(...)) y solo leen lo que el ViewModel
 * enseña hacia fuera.
 */

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
private fun StudioViewModel.telefonoEstudio(): String = normalizarTelefono(whatsappNumber.value)

/**
 * Número de destino del mensaje. WhatsApp solo abre un chat por vez, así
 * que la copia al cliente se envía en un segundo toque.
 */
private fun StudioViewModel.destinoWhatsApp(telefonoCliente: String): String =
    if (telefonoCliente.isNotBlank()) normalizarTelefono(telefonoCliente) else telefonoEstudio()

// Build the WhatsApp message for Shopping Cart Orders with Studio Info & Signed Terms
fun StudioViewModel.generateWhatsAppUri(
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
        // Lo que llevan todas las ofertas (el transporte) va solo con los
        // paquetes del catálogo, no con los servicios sueltos.
        val incluye = item.item.getExtrasList() +
            (if (item.item.id != 0) studioConfig.value.incluidoSiempre else emptyList())
        if (incluye.isNotEmpty()) {
            sb.append("  Incluye: ${incluye.distinct().joinToString(", ")}\n")
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
fun StudioViewModel.generateCobroWhatsAppUri(appointment: AppointmentEntity): String {
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
fun StudioViewModel.generateAppointmentWhatsAppUri(
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
