package com.ancylostoma.quemepongo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ancylostoma.quemepongo.ui.Anuncios
import com.ancylostoma.quemepongo.ui.App

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val error = RegistroErrores.leer(this)
    if (error != null) {
      mostrarError(error)
      return
    }
    enableEdgeToEdge()
    Anuncios.preparar(this)
    // El tema (claro, oscuro y paleta) lo pone App según lo que eligió el usuario.
    setContent { App() }
  }

  /** Pantalla hecha con vistas simples de Android: tiene que funcionar aunque falle todo lo demás. */
  private fun mostrarError(texto: String) {
    val margen = (16 * resources.displayMetrics.density).toInt()
    val columna = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(margen, margen * 3, margen, margen * 3)
    }
    columna.addView(TextView(this).apply {
      text = getString(R.string.error_titulo)
      textSize = 18f
    })
    columna.addView(Button(this).apply {
      text = getString(R.string.error_copiar)
      setOnClickListener {
        val portapapeles = getSystemService(ClipboardManager::class.java)
        portapapeles.setPrimaryClip(ClipData.newPlainText("Qué Me Pongo", texto))
        Toast.makeText(this@MainActivity, R.string.error_copiado, Toast.LENGTH_SHORT).show()
      }
    })
    columna.addView(Button(this).apply {
      text = getString(R.string.error_compartir)
      setOnClickListener {
        val enviar = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
        startActivity(Intent.createChooser(enviar, getString(R.string.error_compartir)))
      }
    })
    columna.addView(Button(this).apply {
      text = getString(R.string.error_reabrir)
      setOnClickListener {
        RegistroErrores.borrar(this@MainActivity)
        recreate()
      }
    })
    columna.addView(TextView(this).apply {
      text = texto
      typeface = Typeface.MONOSPACE
      textSize = 11f
      setTextIsSelectable(true)
      setPadding(0, margen, 0, 0)
    })
    setContentView(ScrollView(this).apply { addView(columna) })
  }
}
