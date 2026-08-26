package com.example.data

/**
 * Datos de contacto y horario del estudio, en un único sitio para que la
 * portada, el calendario y los mensajes de WhatsApp digan siempre lo mismo.
 */
object StudioInfo {
    const val NOMBRE = "FXestudio"
    const val LEMA = "Tu estudio fotográfico en Bayamo"
    const val UBICACION = "Bayamo, Granma, Cuba"
    const val DIRECCION = "Edificio 29, Apt 7, Jesús Menéndez, frente a la Calesa, Bayamo"

    const val TELEFONO_1 = "55823513"
    const val TELEFONO_2 = "56826099"

    const val CATALOGO_URL =
        "https://drive.google.com/file/d/1tprJJJfd6MehK61C6SGvYXRmCklpnb7U/view?usp=drive_link"
    const val FACEBOOK_URL =
        "https://www.facebook.com/fotografoescarret86?mibextid=ZbWKwL"

    // Horario real del estudio. El sábado cierra al mediodía.
    const val HORARIO_SEMANA = "Lunes a viernes de 9:00 AM a 5:00 PM"
    const val HORARIO_SABADO = "Sábados de 9:00 AM a 12:00 PM"
    const val HORARIO_DOMINGO = "Domingos cerrado"
    const val HORARIO_RESUMEN = "Lun a Vie 9:00–5:00 • Sáb 9:00–12:00 • Dom cerrado"

    /** Turnos de lunes a viernes. */
    val TURNOS_SEMANA = listOf(
        "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM"
    )

    /** El sábado solo hay mañana. */
    val TURNOS_SABADO = listOf("09:00 AM", "10:00 AM", "11:00 AM")

    /**
     * Turnos disponibles según el día de la semana de la columna del
     * calendario (0 = lunes … 6 = domingo).
     */
    fun turnosPara(diaSemanaLunesCero: Int): List<String> = when (diaSemanaLunesCero) {
        5 -> TURNOS_SABADO   // sábado
        6 -> emptyList()     // domingo, cerrado
        else -> TURNOS_SEMANA
    }
}
