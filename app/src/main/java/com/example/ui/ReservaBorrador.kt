package com.example.ui

/**
 * La reserva que el cliente está armando, con lo elegido hasta ahora.
 *
 * Existe porque los dos caminos de la app llevan al mismo sitio: se puede
 * empezar por el paquete y acabar poniendo la fecha, o empezar por el
 * calendario, salir a escoger el paquete y volver. En el segundo caso la
 * pantalla del calendario se destruye al salir, así que el día y la hora ya
 * elegidos tienen que estar guardados fuera de ella.
 */
data class ReservaBorrador(
    /** Día elegido, en "dd/MM/yyyy". Vacío = todavía sin fecha. */
    val fecha: String = "",
    /** Día de la semana del día elegido, con lunes = 0. -1 = ninguno. */
    val diaSemana: Int = -1,
    val hora: String = "10:00 AM",
    val nombre: String = "",
    val telefono: String = "",
    val paquete: String = "",
    val notas: String = "",
    val monto: String = "",
    val anticipo: String = "",
    /**
     * true cuando el cliente salió del calendario a escoger un paquete. Al
     * terminar de escoger se vuelve al calendario, que ya tiene la fecha
     * puesta, en vez de pedírsela otra vez.
     */
    val vinoDelCalendario: Boolean = false
) {
    val tieneFecha: Boolean get() = fecha.isNotBlank()
}
