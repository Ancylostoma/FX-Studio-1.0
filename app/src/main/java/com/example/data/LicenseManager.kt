package com.example.data

import java.util.Calendar

/**
 * Sistema de licencia mensual, 100% offline (no llama a ningún servidor).
 *
 * Cada mes la app exige un código de 6 dígitos para seguir funcionando.
 * El código correcto se calcula así:
 *
 *   periodo = (año * 100) + mes              -> ej. julio 2026 = 202607
 *   codigo  = ((periodo * MULTIPLICADOR) + DESPLAZAMIENTO) mod 1 000 000
 *
 * Tú calculas el código de cada mes fuera de la app (con una calculadora,
 * Excel, o pidiéndomelo a mí) y se lo entregas al cliente. La fórmula usa dos
 * constantes secretas (MULTIPLIER y OFFSET) que solo tú conoces.
 *
 * También hay una LLAVE MAESTRA fija (MASTER_KEY) que nunca caduca: introducida
 * una vez, desbloquea la app de forma permanente sin importar el mes.
 *
 * IMPORTANTE (léelo antes de confiar en esto): un APK se puede descompilar.
 * Cualquiera con herramientas como jadx puede abrir el .apk, leer estas
 * constantes y la llave maestra en texto, o simplemente eliminar la
 * verificación. Esto detiene a un cliente normal que no paga o se atrasa;
 * no detiene a alguien con conocimientos técnicos que quiera saltárselo.
 */
object LicenseManager {

    // Cambia estos dos números por otros antes de compilar el APK final que
    // entregarás. Son el secreto que te permite calcular el código de cada mes.
    private const val MULTIPLIER = 9973L
    private const val OFFSET = 734911L

    // Cambia este texto por tu propia llave antes de compilar el APK final.
    const val MASTER_KEY = "FE-MASTER-9K7X2Q"

    const val CONFIG_KEY_PERIOD = "license_active_period"
    const val CONFIG_KEY_MASTER = "license_master_unlocked"

    /** Periodo actual en formato "YYYY-MM", ej. "2026-07". */
    fun currentPeriod(calendar: Calendar = Calendar.getInstance()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    /** Etiqueta legible del periodo actual, ej. "Julio 2026". */
    fun currentPeriodLabel(calendar: Calendar = Calendar.getInstance()): String {
        val meses = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        return "${meses[month]} $year"
    }

    /** Código de 6 dígitos esperado para un periodo "YYYY-MM" dado. */
    fun expectedCodeForPeriod(period: String): String {
        val parts = period.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val seed = (year * 100 + month).toLong()
        val raw = (seed * MULTIPLIER + OFFSET) % 1_000_000L
        return raw.toString().padStart(6, '0')
    }

    /** Código de 6 dígitos esperado para el mes actual del sistema. */
    fun expectedCodeForCurrentMonth(calendar: Calendar = Calendar.getInstance()): String {
        return expectedCodeForPeriod(currentPeriod(calendar))
    }
}
