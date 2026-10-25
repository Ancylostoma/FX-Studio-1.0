package com.ancylostoma.quemepongo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ancylostoma.quemepongo.ui.Anuncios
import com.ancylostoma.quemepongo.ui.App
import com.ancylostoma.quemepongo.ui.TemaQueMePongo

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    Anuncios.preparar(this)
    setContent { TemaQueMePongo { App() } }
  }
}
