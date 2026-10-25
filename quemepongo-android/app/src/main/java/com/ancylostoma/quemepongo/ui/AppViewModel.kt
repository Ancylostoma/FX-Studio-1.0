package com.ancylostoma.quemepongo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ancylostoma.quemepongo.data.Almacen
import com.ancylostoma.quemepongo.data.CatalogoTendencias
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Clima
import com.ancylostoma.quemepongo.data.Datos
import com.ancylostoma.quemepongo.data.Fotos
import com.ancylostoma.quemepongo.data.Look
import com.ancylostoma.quemepongo.data.LookGuardado
import com.ancylostoma.quemepongo.data.Momento
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Perfil
import com.ancylostoma.quemepongo.data.Preferencias
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.ServicioClima
import com.ancylostoma.quemepongo.data.ServicioTendencias
import com.ancylostoma.quemepongo.data.Tendencia
import com.ancylostoma.quemepongo.motor.Contexto
import com.ancylostoma.quemepongo.motor.IdeaCompra
import com.ancylostoma.quemepongo.motor.IdeasCompra
import com.ancylostoma.quemepongo.motor.Motor
import com.ancylostoma.quemepongo.motor.Tendencias
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Estado(
  val cargado: Boolean = false,
  val datos: Datos = Datos(),
  val tendencias: CatalogoTendencias = CatalogoTendencias.VACIO,
  val clima: Clima? = null,
  val climaCargando: Boolean = false,
  val climaError: String? = null,
  val ocasion: Ocasion = Ocasion.CASUAL,
  val momento: Momento = Momento.deHora(LocalTime.now().hour),
  val sugerencias: List<Look> = emptyList(),
  val indice: Int = 0,
  /** Sube cada vez que se arma una tanda nueva de looks (para volver al primero). */
  val tanda: Int = 0,
  /** Lo que lleva puesto el maniquí del probador, en el orden en que se puso. */
  val probador: List<Prenda> = emptyList(),
  val mensaje: String? = null,
) {
  val lookActual: Look? get() = sugerencias.getOrNull(indice)

  fun estaGuardado(look: Look) = datos.looks.any { it.id == look.id }
  val tendenciasActivas: List<Tendencia>
    get() = Tendencias.activas(tendencias.tendencias, LocalDate.now(), datos.perfil.latitud)
}

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
  private val almacen = Almacen(app)
  private val servicioTendencias = ServicioTendencias(app)
  private val _estado = MutableStateFlow(Estado())
  val estado: StateFlow<Estado> = _estado
  private var semilla = System.currentTimeMillis()

  init {
    viewModelScope.launch {
      val (datos, tendencias) = withContext(Dispatchers.IO) { almacen.cargar() to servicioTendencias.local() }
      _estado.update { it.copy(cargado = true, datos = datos, tendencias = tendencias) }
      recalcular()
      if (datos.perfil.latitud != null) cargarClima()
      if (servicioTendencias.tocaActualizar()) {
        servicioTendencias.actualizar()?.let { nuevo ->
          _estado.update { it.copy(tendencias = nuevo) }
          recalcular()
        }
      }
    }
  }

  val carpetaFotos get() = almacen.carpetaFotos

  fun archivoFoto(nombre: String?) = almacen.foto(nombre)

  // ---- Datos ----

  private fun cambiarDatos(f: (Datos) -> Datos) {
    _estado.update { it.copy(datos = f(it.datos)) }
    val copia = _estado.value.datos
    viewModelScope.launch(Dispatchers.IO) { almacen.guardar(copia) }
  }

  private fun contexto(e: Estado = _estado.value) = Contexto(
    clima = e.clima?.para(e.momento),
    ocasion = e.ocasion,
    perfil = e.datos.perfil,
    preferencias = e.datos.preferencias,
    tendencias = e.tendenciasActivas,
    ahoraMs = System.currentTimeMillis(),
  )

  private fun recalcular(nuevaSemilla: Boolean = false) {
    if (nuevaSemilla) semilla = System.currentTimeMillis()
    val e = _estado.value
    val ctx = contexto(e)
    val s = semilla
    viewModelScope.launch {
      val looks = withContext(Dispatchers.Default) { Motor.sugerir(e.datos.prendas, ctx, cantidad = 6, semilla = s) }
      _estado.update { it.copy(sugerencias = looks, indice = 0, tanda = it.tanda + 1) }
    }
  }

  fun mensajeVisto() = _estado.update { it.copy(mensaje = null) }

  private fun avisar(texto: String) = _estado.update { it.copy(mensaje = texto) }

  // ---- Clima ----

  fun cargarClima() {
    val perfil = _estado.value.datos.perfil
    viewModelScope.launch {
      _estado.update { it.copy(climaCargando = true, climaError = null) }
      try {
        val guardadaLat = perfil.latitud
        val guardadaLon = perfil.longitud
        val lat: Double
        val lon: Double
        val lugar: String
        if (guardadaLat != null && guardadaLon != null) {
          lat = guardadaLat
          lon = guardadaLon
          lugar = perfil.ciudad ?: ""
        } else {
          val l = ServicioClima.ubicacion(app)
          if (l == null) {
            _estado.update { it.copy(climaCargando = false, climaError = "No pude saber dónde estás. Escribe tu ciudad.") }
            return@launch
          }
          lat = l.latitude
          lon = l.longitude
          lugar = ServicioClima.nombreDelLugar(app, lat, lon) ?: "Mi ubicación"
          cambiarDatos { d -> d.copy(perfil = d.perfil.copy(latitud = lat, longitud = lon, ciudad = lugar)) }
        }
        val clima = ServicioClima.pronostico(lat, lon, lugar)
        _estado.update { it.copy(clima = clima, climaCargando = false) }
        recalcular()
      } catch (e: Exception) {
        _estado.update { it.copy(climaCargando = false, climaError = "Sin conexión con el servicio del tiempo.") }
      }
    }
  }

  /** Tras conceder el permiso: olvida la ciudad guardada y usa la ubicación real. */
  fun usarUbicacion() {
    cambiarDatos { d -> d.copy(perfil = d.perfil.copy(latitud = null, longitud = null, ciudad = null)) }
    cargarClima()
  }

  fun buscarCiudad(nombre: String) {
    if (nombre.isBlank()) return
    viewModelScope.launch {
      _estado.update { it.copy(climaCargando = true, climaError = null) }
      val r = ServicioClima.buscarCiudad(app, nombre.trim())
      if (r == null) {
        _estado.update { it.copy(climaCargando = false, climaError = "No encontré \"$nombre\".") }
        return@launch
      }
      cambiarDatos { d -> d.copy(perfil = d.perfil.copy(ciudad = r.first, latitud = r.second, longitud = r.third)) }
      cargarClima()
    }
  }

  // ---- Sugerencias ----

  fun elegirOcasion(o: Ocasion) {
    _estado.update { it.copy(ocasion = o) }
    recalcular()
  }

  fun elegirMomento(m: Momento) {
    _estado.update { it.copy(momento = m) }
    recalcular()
  }

  /** El look que se está viendo en el carrusel. */
  fun verLook(i: Int) = _estado.update { it.copy(indice = i.coerceIn(0, (it.sugerencias.size - 1).coerceAtLeast(0))) }

  fun masLooks() = recalcular(nuevaSemilla = true)

  private fun aprender(look: Look, senal: Double) =
    cambiarDatos { d -> d.copy(preferencias = Motor.aprender(d.preferencias, look.prendas, senal)) }

  /** "No me gusta": se aprende y el look sale del carrusel. */
  fun descartar(look: Look) {
    aprender(look, -1.0)
    val quedan = _estado.value.sugerencias.filterNot { it.id == look.id }
    if (quedan.isEmpty()) {
      recalcular(nuevaSemilla = true)
    } else {
      _estado.update { it.copy(sugerencias = quedan, indice = it.indice.coerceAtMost(quedan.size - 1)) }
    }
  }

  /** El corazón: guarda el look (y cuenta como "me gusta") o lo quita de guardados. */
  fun alternarGuardado(look: Look) {
    if (_estado.value.estaGuardado(look)) {
      borrarLook(look.id)
      return
    }
    guardarLook(look)
  }

  fun guardarLook(look: Look) {
    if (_estado.value.estaGuardado(look)) {
      avisar("Ese look ya está guardado")
      return
    }
    val guardado = LookGuardado(look.id, look.prendas, System.currentTimeMillis(), _estado.value.ocasion)
    cambiarDatos { d -> d.copy(looks = listOf(guardado) + d.looks) }
    aprender(look, 1.5)
    avisar("Guardado en Yo")
  }

  fun meLoPongo(look: Look) {
    val ahora = System.currentTimeMillis()
    val ids = look.prendas.filter { !it.virtual }.map { it.id }.toSet()
    val usado = LookGuardado("$ahora", look.prendas, ahora, _estado.value.ocasion)
    cambiarDatos { d ->
      d.copy(
        prendas = d.prendas.map { if (it.id in ids) it.copy(usos = it.usos + 1, ultimoUso = ahora) else it },
        historial = (listOf(usado) + d.historial).take(365),
      )
    }
    aprender(look, 1.0)
    avisar("¡Listo! Mañana te sugiero otras prendas")
  }

  fun borrarLook(id: String) = cambiarDatos { d -> d.copy(looks = d.looks.filterNot { it.id == id }) }

  fun borrarDelHistorial(id: String) = cambiarDatos { d -> d.copy(historial = d.historial.filterNot { it.id == id }) }

  fun lookDeTendencia(t: Tendencia): Look = Motor.lookDeTendencia(t, _estado.value.datos.prendas, contexto())

  fun ideasDeCompra(): List<IdeaCompra> {
    val e = _estado.value
    return IdeasCompra.sugerir(e.datos.prendas, e.datos.perfil, e.datos.preferencias, e.tendenciasActivas)
  }

  fun enlaceTienda(idea: com.ancylostoma.quemepongo.data.IdeaPrenda) =
    IdeasCompra.enlace(_estado.value.tendencias.tiendaUrl, idea)

  // ---- Armario ----

  fun nuevoId(): String = UUID.randomUUID().toString().take(8)

  /** Copia la foto a la app y adivina el color. Devuelve (archivo, color) en el hilo principal. */
  fun importarFoto(uri: Uri, id: String, alTerminar: (String?, String?) -> Unit) {
    viewModelScope.launch {
      val r = withContext(Dispatchers.IO) {
        runCatching {
          val nombre = Fotos.importar(app, uri, almacen.carpetaFotos, id)
          nombre to nombre?.let { almacen.foto(it) }?.let { Fotos.colorPrincipal(it) }
        }.getOrNull()
      }
      if (r?.first == null) avisar("No pude abrir esa foto")
      alTerminar(r?.first, r?.second)
    }
  }

  fun guardarPrenda(p: Prenda) {
    val anterior = _estado.value.datos.prendas.firstOrNull { it.id == p.id }
    if (anterior?.foto != null && anterior.foto != p.foto) almacen.borrarFoto(anterior.foto)
    cambiarDatos { d ->
      val nueva = if (anterior == null) p.copy(creada = System.currentTimeMillis()) else p
      d.copy(prendas = if (anterior == null) d.prendas + nueva else d.prendas.map { if (it.id == p.id) nueva else it })
    }
    _estado.update { e -> e.copy(probador = e.probador.map { if (it.id == p.id) p else it }) }
    recalcular()
  }

  fun borrarPrenda(p: Prenda) {
    almacen.borrarFoto(p.foto)
    cambiarDatos { d -> d.copy(prendas = d.prendas.filterNot { it.id == p.id }) }
    _estado.update { e -> e.copy(probador = e.probador.filterNot { it.id == p.id }) }
    recalcular()
  }

  /** Descarta una foto que se importó pero no se llegó a guardar. */
  fun descartarFoto(nombre: String?) {
    if (nombre != null && _estado.value.datos.prendas.none { it.foto == nombre }) almacen.borrarFoto(nombre)
  }

  // ---- Probador ----

  fun ponerEnProbador(p: Prenda) = _estado.update { e ->
    val quitar: (Prenda) -> Boolean = { x ->
      x.id == p.id || when (p.categoria) {
        Categoria.SUPERIOR, Categoria.INFERIOR -> x.categoria == p.categoria || x.categoria == Categoria.VESTIDO
        Categoria.VESTIDO -> x.categoria in setOf(Categoria.SUPERIOR, Categoria.INFERIOR, Categoria.VESTIDO)
        Categoria.ACCESORIO -> x.subtipo.forma == p.subtipo.forma
        else -> x.categoria == p.categoria
      }
    }
    e.copy(probador = e.probador.filterNot(quitar) + p)
  }

  fun quitarDelProbador(p: Prenda) = _estado.update { e -> e.copy(probador = e.probador.filterNot { it.id == p.id }) }

  fun vaciarProbador() = _estado.update { it.copy(probador = emptyList()) }

  fun probarLook(prendas: List<Prenda>) = _estado.update { it.copy(probador = prendas) }

  fun puntuarProbador(): Look? {
    val e = _estado.value
    if (e.probador.isEmpty()) return null
    val ctx = contexto(e)
    return Look(
      id = e.probador.map { it.id }.sorted().joinToString("|"),
      prendas = e.probador,
      puntuacion = Motor.puntuar(e.probador, ctx),
      razones = Motor.explicar(e.probador, ctx),
    )
  }

  // ---- Perfil ----

  fun cambiarPerfil(f: (Perfil) -> Perfil) {
    cambiarDatos { d -> d.copy(perfil = f(d.perfil)) }
    recalcular()
  }

  fun reiniciarAprendizaje() {
    cambiarDatos { d -> d.copy(preferencias = Preferencias()) }
    recalcular()
    avisar("Aprendizaje borrado")
  }
}
