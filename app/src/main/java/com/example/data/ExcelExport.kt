package com.example.data

import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera un archivo .xlsx de verdad, sin librerías externas.
 *
 * Un .xlsx no es más que un ZIP con varios XML dentro, así que se arma a mano
 * con [ZipOutputStream]. Los textos van como "inlineStr" para no tener que
 * mantener la tabla de cadenas compartidas, que es la parte que más suele
 * romperse. Excel, LibreOffice y Google Sheets lo abren igual.
 */
object ExcelExport {

    /** Una hoja: su nombre de pestaña, los encabezados y las filas. */
    data class Hoja(
        val nombre: String,
        val encabezados: List<String>,
        val filas: List<List<Celda>>
    )

    /** El valor de una casilla. Los números entran como número, no como texto. */
    sealed class Celda {
        data class Texto(val valor: String) : Celda()
        data class Numero(val valor: Double) : Celda()
    }

    fun texto(v: String?): Celda = Celda.Texto(v ?: "")
    fun numero(v: Double): Celda = Celda.Numero(v)
    fun siNo(v: Boolean): Celda = Celda.Texto(if (v) "Sí" else "No")

    // -----------------------------------------------------------------------

    /** Escapa lo que XML no admite y quita los caracteres de control. */
    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (c in s) {
            when {
                c == '&' -> sb.append("&amp;")
                c == '<' -> sb.append("&lt;")
                c == '>' -> sb.append("&gt;")
                c == '"' -> sb.append("&quot;")
                c == '\'' -> sb.append("&apos;")
                // XML 1.0 solo tolera tabulador, salto de línea y retorno.
                c.code < 0x20 && c != '\t' && c != '\n' && c != '\r' -> sb.append(' ')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** 0 -> A, 25 -> Z, 26 -> AA … */
    internal fun columna(indice: Int): String {
        var n = indice
        val sb = StringBuilder()
        while (true) {
            sb.insert(0, ('A' + (n % 26)))
            n = n / 26 - 1
            if (n < 0) break
        }
        return sb.toString()
    }

    /**
     * Número tal y como lo espera Excel: sin notación científica, con punto
     * decimal y sin depender del idioma del teléfono. Double.toString() pasa a
     * "1.0E12" con cifras grandes y Excel rechaza esa casilla.
     */
    private fun numeroPlano(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "0"
        return if (v == Math.floor(v) && Math.abs(v) < 1e15) {
            v.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        }
    }

    private fun celdaXml(ref: String, celda: Celda): String = when (celda) {
        is Celda.Numero ->
            """<c r="$ref"><v>${numeroPlano(celda.valor)}</v></c>"""
        is Celda.Texto ->
            if (celda.valor.isEmpty()) ""
            else """<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${esc(celda.valor)}</t></is></c>"""
    }

    private fun filaXml(numero: Int, celdas: List<Celda>): String {
        val sb = StringBuilder()
        sb.append("""<row r="$numero">""")
        celdas.forEachIndexed { i, c ->
            sb.append(celdaXml("${columna(i)}$numero", c))
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun hojaXml(hoja: Hoja): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")
        sb.append(filaXml(1, hoja.encabezados.map { Celda.Texto(it) }))
        hoja.filas.forEachIndexed { i, fila ->
            sb.append(filaXml(i + 2, fila))
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    /** Excel no admite : \ / ? * [ ] en el nombre de una pestaña, ni más de 31 letras. */
    private fun nombreHoja(n: String): String =
        n.replace(Regex("""[:\\/?*\[\]]"""), " ").take(31).ifBlank { "Hoja" }

    private fun contentTypesXml(cuantasHojas: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        sb.append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        sb.append("""<Default Extension="xml" ContentType="application/xml"/>""")
        sb.append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        for (i in 1..cuantasHojas) {
            sb.append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        sb.append("</Types>")
        return sb.toString()
    }

    private fun workbookXml(hojas: List<Hoja>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """)
        sb.append("""xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        hojas.forEachIndexed { i, h ->
            sb.append("""<sheet name="${esc(nombreHoja(h.nombre))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        sb.append("</sheets></workbook>")
        return sb.toString()
    }

    private fun workbookRelsXml(cuantasHojas: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..cuantasHojas) {
            sb.append("""<Relationship Id="rId$i" """)
            sb.append("""Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" """)
            sb.append("""Target="worksheets/sheet$i.xml"/>""")
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private const val RELS_RAIZ =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" """ +
        """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" """ +
        """Target="xl/workbook.xml"/></Relationships>"""

    /** Escribe el libro en [destino] y lo devuelve. */
    fun escribir(destino: File, hojas: List<Hoja>): File {
        destino.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(destino)).use { zip ->
            fun parte(ruta: String, contenido: String) {
                zip.putNextEntry(ZipEntry(ruta))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            parte("[Content_Types].xml", contentTypesXml(hojas.size))
            parte("_rels/.rels", RELS_RAIZ)
            parte("xl/workbook.xml", workbookXml(hojas))
            parte("xl/_rels/workbook.xml.rels", workbookRelsXml(hojas.size))
            hojas.forEachIndexed { i, h ->
                parte("xl/worksheets/sheet${i + 1}.xml", hojaXml(h))
            }
        }
        return destino
    }
}
