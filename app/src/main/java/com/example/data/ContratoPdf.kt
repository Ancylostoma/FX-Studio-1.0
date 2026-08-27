package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Arma el contrato de una reservación como PDF, usando solo lo que trae
 * Android ([PdfDocument]), sin librerías añadidas.
 *
 * La primera página es el resumen operativo (datos, pagos, firma y foto de
 * confirmación) y a continuación va el texto completo del contrato. Es el
 * documento que se le entrega al cliente.
 */
object ContratoPdf {

    // A4 a 72 puntos por pulgada.
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 42f

    private val anchoUtil = ANCHO - MARGEN * 2

    private fun pintor(
        tam: Float,
        negrita: Boolean = false,
        color: Int = Color.BLACK
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tam
        this.color = color
        typeface = Typeface.create(Typeface.SERIF, if (negrita) Typeface.BOLD else Typeface.NORMAL)
    }

    /**
     * Parte el texto en líneas que quepan en [ancho]. Se hace a mano para
     * controlar exactamente dónde cae cada salto y poder paginar después.
     */
    internal fun envolver(texto: String, paint: Paint, ancho: Float): List<String> {
        val lineas = mutableListOf<String>()
        for (parrafo in texto.split("\n")) {
            if (parrafo.isBlank()) {
                lineas.add("")
                continue
            }
            var actual = ""
            for (palabra in parrafo.split(" ").filter { it.isNotEmpty() }) {
                val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (paint.measureText(prueba) <= ancho) {
                    actual = prueba
                } else {
                    if (actual.isNotEmpty()) lineas.add(actual)
                    if (paint.measureText(palabra) > ancho) {
                        // Una sola palabra más ancha que la línea: se parte.
                        var resto = palabra
                        while (paint.measureText(resto) > ancho && resto.length > 1) {
                            var corte = resto.length
                            while (corte > 1 && paint.measureText(resto.substring(0, corte)) > ancho) {
                                corte--
                            }
                            lineas.add(resto.substring(0, corte))
                            resto = resto.substring(corte)
                        }
                        actual = resto
                    } else {
                        actual = palabra
                    }
                }
            }
            lineas.add(actual)
        }
        return lineas
    }

    /** Lleva la cuenta de la página abierta y del alto ya ocupado. */
    private class Lienzo(private val doc: PdfDocument) {
        var pagina: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f
        private var numero = 0

        fun nueva() {
            cerrar()
            numero++
            val info = PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create()
            val p = doc.startPage(info)
            pagina = p
            canvas = p.canvas
            y = MARGEN
        }

        fun cerrar() {
            pagina?.let { doc.finishPage(it) }
            pagina = null
            canvas = null
        }

        /** Abre página nueva si lo que viene no cabe en la actual. */
        fun asegurar(alto: Float) {
            if (canvas == null) nueva()
            if (y + alto > ALTO - MARGEN) nueva()
        }
    }

    private fun escribirLineas(
        lienzo: Lienzo,
        lineas: List<String>,
        paint: Paint,
        interlineado: Float
    ) {
        for (linea in lineas) {
            lienzo.asegurar(interlineado)
            lienzo.canvas?.drawText(linea, MARGEN, lienzo.y + paint.textSize, paint)
            lienzo.y += interlineado
        }
    }

    private fun escribir(
        lienzo: Lienzo,
        texto: String,
        paint: Paint,
        interlineado: Float = paint.textSize * 1.45f
    ) {
        escribirLineas(lienzo, envolver(texto, paint, anchoUtil), paint, interlineado)
    }

    private fun separador(lienzo: Lienzo, grosor: Float = 0.8f) {
        lienzo.asegurar(14f)
        val p = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = grosor
        }
        lienzo.canvas?.drawLine(MARGEN, lienzo.y + 6f, ANCHO - MARGEN, lienzo.y + 6f, p)
        lienzo.y += 14f
    }

    /** Etiqueta en negrita y valor a continuación, en la misma línea lógica. */
    private fun dato(lienzo: Lienzo, etiqueta: String, valor: String) {
        if (valor.isBlank()) return
        val pEtiqueta = pintor(10.5f, negrita = true)
        val pValor = pintor(10.5f)
        val anchoEtiqueta = pEtiqueta.measureText("$etiqueta  ")
        val lineas = envolver(valor, pValor, anchoUtil - anchoEtiqueta)

        lineas.forEachIndexed { i, linea ->
            lienzo.asegurar(15f)
            val base = lienzo.y + pValor.textSize
            if (i == 0) {
                lienzo.canvas?.drawText(etiqueta, MARGEN, base, pEtiqueta)
            }
            lienzo.canvas?.drawText(linea, MARGEN + anchoEtiqueta, base, pValor)
            lienzo.y += 15f
        }
    }

    private fun dibujarImagen(
        lienzo: Lienzo,
        bytes: ByteArray?,
        titulo: String,
        altoMax: Float,
        fondoBlanco: Boolean
    ) {
        val bmp: Bitmap = try {
            bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } ?: return
        } catch (e: Exception) {
            return
        }

        val ratio = bmp.width.toFloat() / bmp.height.toFloat()
        var ancho = anchoUtil
        var alto = ancho / ratio
        if (alto > altoMax) {
            alto = altoMax
            ancho = alto * ratio
        }

        lienzo.asegurar(alto + 26f)
        val pTitulo = pintor(9.5f, negrita = true, color = Color.DKGRAY)
        lienzo.canvas?.drawText(titulo, MARGEN, lienzo.y + pTitulo.textSize, pTitulo)
        lienzo.y += 16f

        val destino = RectF(MARGEN, lienzo.y, MARGEN + ancho, lienzo.y + alto)
        if (fondoBlanco) {
            lienzo.canvas?.drawRect(destino, Paint().apply { color = Color.WHITE })
        }
        lienzo.canvas?.drawBitmap(bmp, null, destino, Paint(Paint.FILTER_BITMAP_FLAG))
        lienzo.canvas?.drawRect(
            destino,
            Paint().apply {
                style = Paint.Style.STROKE
                color = Color.LTGRAY
                strokeWidth = 0.8f
            }
        )
        lienzo.y += alto + 10f
    }

    /**
     * Escribe el contrato de [cita] en [destino] y devuelve el archivo.
     *
     * [textoContrato] es el texto vigente en Ajustes; [config] aporta el
     * nombre del estudio y su ficha de contacto.
     */
    fun generar(
        destino: File,
        cita: AppointmentEntity,
        config: StudioConfig,
        textoContrato: String,
        equivalencias: (Double) -> String?
    ): File {
        destino.parentFile?.mkdirs()
        val doc = PdfDocument()
        val lienzo = Lienzo(doc)
        lienzo.nueva()

        // --- Encabezado -------------------------------------------------
        val pTitulo = pintor(20f, negrita = true)
        lienzo.canvas?.drawText(config.titulo, MARGEN, lienzo.y + pTitulo.textSize, pTitulo)
        lienzo.y += pTitulo.textSize + 6f
        escribir(lienzo, config.lema, pintor(10f, color = Color.DKGRAY))
        escribir(lienzo, "${config.direccion}  •  ${config.telefonos}", pintor(9f, color = Color.DKGRAY))
        separador(lienzo, 1.5f)

        val pSeccion = pintor(14f, negrita = true)
        lienzo.asegurar(24f)
        lienzo.canvas?.drawText(
            "CONTRATO DE SESIÓN FOTOGRÁFICA",
            MARGEN,
            lienzo.y + pSeccion.textSize,
            pSeccion
        )
        lienzo.y += pSeccion.textSize + 12f

        // --- Datos de la reservación ------------------------------------
        dato(lienzo, "Cliente:", cita.nombreCliente)
        dato(lienzo, "Teléfono:", cita.telefono)
        dato(lienzo, "Fecha de la sesión:", "${cita.fecha}   Hora: ${cita.hora}")
        dato(lienzo, "Opción o paquete:", cita.detalleSeleccion)
        if (cita.notas.isNotBlank()) dato(lienzo, "Notas:", cita.notas)
        dato(lienzo, "Estado del trabajo:", cita.estado)

        if (cita.montoAcordado > 0.0) {
            lienzo.y += 4f
            dato(
                lienzo,
                "Monto acordado:",
                "$${String.format(Locale.US, "%.2f", cita.montoAcordado)} USD" +
                    (equivalencias(cita.montoAcordado)?.let { "   ($it)" } ?: "")
            )
            dato(
                lienzo,
                "Anticipo pagado:",
                "$${String.format(Locale.US, "%.2f", cita.anticipoPagado)} USD"
            )
            dato(
                lienzo,
                "Saldo pendiente:",
                "$${String.format(Locale.US, "%.2f", cita.saldoPendiente)} USD" +
                    (equivalencias(cita.saldoPendiente)?.let { "   ($it)" } ?: "")
            )
        }

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        dato(lienzo, "Reservación registrada:", formato.format(Date(cita.createdAt)))

        separador(lienzo)

        // --- Firma y foto -----------------------------------------------
        dibujarImagen(
            lienzo,
            cita.firmaBytes,
            "Firma del cliente (mayor de edad), conforme con este contrato:",
            140f,
            fondoBlanco = true
        )
        dibujarImagen(
            lienzo,
            cita.fotoClienteBytes,
            "Foto tomada al cliente en el momento de firmar:",
            260f,
            fondoBlanco = false
        )

        if (cita.firmaBytes == null) {
            escribir(
                lienzo,
                "Esta reservación no tiene firma digital registrada.",
                pintor(10f, color = Color.RED)
            )
        }

        // --- Texto del contrato -----------------------------------------
        lienzo.nueva()
        val pCabecera = pintor(13f, negrita = true)
        lienzo.canvas?.drawText(
            "Términos y condiciones",
            MARGEN,
            lienzo.y + pCabecera.textSize,
            pCabecera
        )
        lienzo.y += pCabecera.textSize + 10f
        escribir(lienzo, textoContrato, pintor(9.5f), interlineado = 13.5f)

        lienzo.cerrar()
        FileOutputStream(destino).use { doc.writeTo(it) }
        doc.close()
        return destino
    }

    /** Nombre de archivo con el cliente y la fecha, sin caracteres raros. */
    fun nombreArchivo(cita: AppointmentEntity): String {
        val limpio = cita.nombreCliente
            .replace(Regex("""[^\p{L}\p{N}]+"""), "_")
            .trim('_')
            .take(30)
            .ifBlank { "cliente" }
        val fecha = cita.fecha.replace("/", "-")
        return "Contrato_${limpio}_$fecha.pdf"
    }
}
