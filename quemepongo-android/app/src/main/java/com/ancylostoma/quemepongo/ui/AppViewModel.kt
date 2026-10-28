package com.ancylostoma.quemepongo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ancylostoma.quemepongo.R
import com.ancylostoma.quemepongo.data.Almacen
import com.ancylostoma.quemepongo.data.AvisoDiario
import com.ancylostoma.quemepongo.data.Vista
import com.ancylostoma.quemepongo.data.CatalogoTendencias
import com.ancylostoma.quemepongo.data.Categoria
import com.ancylostoma.quemepongo.data.Clima
import com.ancylostoma.quemepongo.data.Datos
import com.ancylostoma.quemepongo.data.Facturacion
import com.ancylostoma.quemepongo.data.PlanPremium
import com.ancylostoma.quemepongo.data.Viaje
import com.ancylostoma.quemepongo.data.Fotos
import com.ancylostoma.quemepongo.data.Look
import com.ancylostoma.quemepongo.data.Lugar
import com.ancylostoma.quemepongo.data.Lugares
import com.ancylostoma.quemepongo.data.LookGuardado
import com.ancylostoma.quemepongo.data.Momento
import com.ancylostoma.quemepongo.data.Ocasion
import com.ancylostoma.quemepongo.data.Perfil
import com.ancylostoma.quemepongo.data.Preferencias
import com.ancylostoma.quemepongo.data.Prenda
import com.ancylostoma.quemepongo.data.ServicioClima
import com.ancylostoma.quemepongo.data.ServicioTendencias
import com.ancylostoma.quemepongo.data.Tendencia
import com.ancylostoma.quemepongo.motor.Analisis
import com.ancylostoma.quemepongo.motor.Contexto
import com.ancylostoma.quemepongo.motor.DiaPlan
import com.ancylostoma.quemepongo.motor.Estadisticas
import com.ancylostoma.quemepongo.motor.Planificador
import com.ancylostoma.quemepongo.motor.IdeaCompra
import com.ancylostoma.quemepongo.motor.IdeasCompra
import com.ancylostoma.quemepongo.motor.Motor
import com.ancylostoma.quemepongo.motor.Tendencias
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
  /** Ciudades encontradas al buscar, para que el usuario elija la suya. */
  val ciudades: List<Lugar> = emptyList(),
  val ocasion: Ocasion = Ocasion.CASUAL,
  val momento: Momento = Momento.deHora(LocalTime.now().hour),
  val sugerencias: List<Look> = emptyList(),
  val indice: Int = 0,
  /** Sube cada vez que se arma una tanda nueva de looks (para volver al primero). */
  val tanda: Int = 0,
  /** Lo que lleva puesto el maniquí del probador, en el orden en que se puso. */
  val probador: List<Prenda> = emptyList(),
  val mensaje: String? = null,
  /** Premium activo (comprado en Play o, en compilaciones de prueba, activado a mano). */
  val premium: Boolean = false,
  val planes: List<PlanPremium> = emptyList(),
  /** Si no es null, hay que mostrar la pantalla de Premium por este motivo. */
  val pedirPremium: MotivoPremium? = null,
  val maletaCargando: Boolean = false,
  val maletaError: String? = null,
) {
  val lookActual: Look? get() = sugerencias.getOrNull(indice)

  fun estaGuardado(look: Look) = datos.looks.any { it.id == look.id }

  val prendasReales get() = datos.prendas.count { !it.virtual }
  val tendenciasActivas: List<Tendencia>
    get() = Tendencias.activas(tendencias.tendencias, LocalDate.now(), datos.perfil.latitud)
}

/** Por qué se ofrece Premium: cambia el título de la pantalla de pago. */
enum class MotivoPremium { GENERAL, PRENDAS, LOOKS, SEMANA, MALETA, ESTADISTICAS, HORA_AVISO }

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
  companion object {
    const val LIMITE_PRENDAS = 30
    const val LIMITE_LOOKS = 15
  }

  private val almacen = Almacen(app)
  private val facturacion = Facturacion(app)
  private val servicioTendencias = ServicioTendencias(app)
  private val _estado = MutableStateFlow(Estado())
  val estado: StateFlow<Estado> = _estado
  private var semilla = System.currentTimeMillis()

  init {
    facturacion.conectar()
    viewModelScope.launch {
      combine(facturacion.comprado, facturacion.prueba, facturacion.planes) { c, p, pl -> Triple(c, p, pl) }.collect { (c, p, pl) ->
        _estado.update { it.copy(premium = c || p, planes = pl) }
      }
    }
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
            val mensaje = if (!ServicioClima.ubicacionActiva(app)) R.string.msg_ubicacion_apagada else R.string.msg_sin_ubicacion
            _estado.update { it.copy(climaCargando = false, climaError = app.getString(mensaje)) }
            return@launch
          }
          lat = l.latitude
          lon = l.longitude
          lugar = Lugares.nombreDe(app, lat, lon) ?: app.getString(R.string.mi_ubicacion)
          cambiarDatos { d -> d.copy(perfil = d.perfil.copy(latitud = lat, longitud = lon, ciudad = lugar)) }
        }
        val clima = ServicioClima.pronostico(lat, lon, lugar, app.cacheDir)
        _estado.update { it.copy(clima = clima, climaCargando = false) }
        recalcular()
      } catch (e: Exception) {
        _estado.update { it.copy(climaCargando = false, climaError = app.getString(R.string.msg_sin_conexion)) }
      }
    }
  }

  /** Tras conceder el permiso: olvida la ciudad guardada y usa la ubicación real. */
  fun usarUbicacion() {
    _estado.update { it.copy(ciudades = emptyList()) }
    cambiarDatos { d -> d.copy(perfil = d.perfil.copy(latitud = null, longitud = null, ciudad = null)) }
    cargarClima()
  }

  fun buscarCiudad(nombre: String) {
    if (nombre.isBlank()) return
    viewModelScope.launch {
      _estado.update { it.copy(climaCargando = true, climaError = null, ciudades = emptyList()) }
      val r = Lugares.buscar(app, nombre)
      _estado.update {
        if (r.isEmpty()) {
          it.copy(climaCargando = false, climaError = app.getString(R.string.msg_no_encontre, nombre.trim()))
        } else {
          it.copy(climaCargando = false, ciudades = r)
        }
      }
    }
  }

  /** La ciudad que el usuario eligió de la lista de resultados. */
  fun elegirCiudad(l: Lugar) {
    _estado.update { it.copy(ciudades = emptyList()) }
    cambiarDatos { d -> d.copy(perfil = d.perfil.copy(ciudad = l.nombre, latitud = l.latitud, longitud = l.longitud)) }
    cargarClima()
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
    val e = _estado.value
    if (!e.premium && e.datos.looks.size >= LIMITE_LOOKS) {
      pedirPremium(MotivoPremium.LOOKS)
      return
    }
    if (e.estaGuardado(look)) {
      avisar(app.getString(R.string.msg_ya_guardado))
      return
    }
    val guardado = LookGuardado(look.id, look.prendas, System.currentTimeMillis(), _estado.value.ocasion)
    cambiarDatos { d -> d.copy(looks = listOf(guardado) + d.looks) }
    aprender(look, 1.5)
    avisar(app.getString(R.string.msg_guardado))
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
    avisar(app.getString(R.string.msg_me_lo_pongo))
  }

  fun borrarLook(id: String) = cambiarDatos { d -> d.copy(looks = d.looks.filterNot { it.id == id }) }

  fun borrarDelHistorial(id: String) = cambiarDatos { d -> d.copy(historial = d.historial.filterNot { it.id == id }) }

  fun lookDeTendencia(t: Tendencia): Look = Motor.lookDeTendencia(t, _estado.value.datos.prendas, contexto())

  fun ideasDeCompra(max: Int = 8): List<IdeaCompra> {
    val e = _estado.value
    return IdeasCompra.sugerir(e.datos.prendas, e.datos.perfil, e.datos.preferencias, e.tendenciasActivas, max)
  }

  /** Lo que el usuario escribió en el buscador de Descubrir, en la tienda configurada. */
  fun enlaceBusqueda(texto: String) = IdeasCompra.enlace(_estado.value.tendencias.tiendaUrl, texto.trim())

  fun enlaceTienda(idea: com.ancylostoma.quemepongo.data.IdeaPrenda) =
    IdeasCompra.enlace(_estado.value.tendencias.tiendaUrl, app.busqueda(idea))

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
      if (r?.first == null) avisar(app.getString(R.string.msg_foto_error))
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

  // ---- Premium ----

  val modoPrueba get() = Facturacion.MODO_PRUEBA
  val pruebaActiva get() = facturacion.prueba.value

  fun pedirPremium(motivo: MotivoPremium) = _estado.update { it.copy(pedirPremium = motivo) }

  fun premiumMostrado() = _estado.update { it.copy(pedirPremium = null) }

  /** Con el plan gratis, el armario llega hasta [LIMITE_PRENDAS]. */
  fun puedeAgregarPrenda(): Boolean {
    val e = _estado.value
    if (e.premium || e.prendasReales < LIMITE_PRENDAS) return true
    pedirPremium(MotivoPremium.PRENDAS)
    return false
  }

  fun comprar(actividad: android.app.Activity, plan: PlanPremium) = facturacion.comprar(actividad, plan)

  fun restaurarCompras() {
    facturacion.conectar()
    avisar(app.getString(R.string.pw_restaurando))
  }

  fun cambiarPrueba(activo: Boolean) = facturacion.cambiarPrueba(activo)

  fun cambiarHoraAviso(hora: String) {
    cambiarPerfil { it.copy(horaAviso = hora) }
    if (_estado.value.datos.perfil.avisoDiario) runCatching { AvisoDiario.programar(app, true, hora) }
  }

  // ---- Premium: planear la semana ----

  private fun diasDeLaSemana(): List<LocalDate> = (0L until 7L).map { LocalDate.now().plusDays(it) }

  private fun ocasionPorDefecto(f: LocalDate) =
    if (f.dayOfWeek == DayOfWeek.SATURDAY || f.dayOfWeek == DayOfWeek.SUNDAY) Ocasion.CASUAL else Ocasion.TRABAJO

  /** Arma (o rehace) los 7 días. Conserva la ocasión que el usuario eligió para cada día. */
  fun armarSemana(cambiar: Pair<LocalDate, Ocasion>? = null, nuevaSemilla: Boolean = false) {
    val e = _estado.value
    val anteriores = e.datos.plan.associateBy { it.id }
    val dias = diasDeLaSemana().map { f ->
      val id = "plan-$f"
      val ocasion = cambiar?.takeIf { it.first == f }?.second ?: anteriores[id]?.ocasion ?: ocasionPorDefecto(f)
      DiaPlan(f, e.clima?.paraDia(f), ocasion)
    }
    val s = if (nuevaSemilla) System.currentTimeMillis() else semilla
    viewModelScope.launch {
      val looks = withContext(Dispatchers.Default) {
        Planificador.semana(e.datos.prendas, dias, e.datos.perfil, e.datos.preferencias, { f -> Tendencias.activas(e.tendencias.tendencias, f, e.datos.perfil.latitud) }, s)
      }
      val plan = dias.zip(looks).map { (d, l) -> LookGuardado("plan-${d.fecha}", l.prendas, d.fecha.toEpochDay() * 86_400_000L, d.ocasion) }
      cambiarDatos { it.copy(plan = plan) }
    }
  }

  /** El plan guardado, si es de esta semana; si empezó otro día, se rehace. */
  fun revisarSemana() {
    val primero = _estado.value.datos.plan.firstOrNull()?.id
    if (primero != "plan-${LocalDate.now()}") armarSemana()
  }

  // ---- Premium: maleta de viaje ----

  fun armarMaleta(destino: String, desde: LocalDate, dias: Int, ocasion: Ocasion) {
    if (destino.isBlank()) return
    viewModelScope.launch {
      _estado.update { it.copy(maletaCargando = true, maletaError = null) }
      val lugar = Lugares.buscar(app, destino, max = 1).firstOrNull()
      if (lugar == null) {
        _estado.update { it.copy(maletaCargando = false, maletaError = app.getString(R.string.maleta_no_encontre)) }
        return@launch
      }
      val clima = runCatching { ServicioClima.pronostico(lugar.latitud, lugar.longitud, lugar.nombre, app.cacheDir) }.getOrNull()
      val e = _estado.value
      val plan = (0 until dias).map { i ->
        val f = desde.plusDays(i.toLong())
        DiaPlan(f, clima?.paraDia(f), ocasion)
      }
      val m = withContext(Dispatchers.Default) {
        Planificador.maleta(e.datos.prendas, plan, e.datos.perfil, e.datos.preferencias, { f -> Tendencias.activas(e.tendencias.tendencias, f, lugar.latitud) })
      }
      val looks = plan.zip(m.looks).map { (d, l) -> LookGuardado("viaje-${d.fecha}", l.prendas, d.fecha.toEpochDay() * 86_400_000L, d.ocasion) }
      cambiarDatos { it.copy(viaje = Viaje(lugar.nombre, desde.toString(), dias, looks, m.prendas, m.faltan)) }
      _estado.update { it.copy(maletaCargando = false) }
    }
  }

  fun borrarViaje() = cambiarDatos { it.copy(viaje = null) }

  // ---- Premium: estadísticas ----

  fun estadisticas(): Estadisticas = _estado.value.datos.let { Analisis.estadisticas(it.prendas, it.historial, System.currentTimeMillis()) }

  // ---- Perfil ----

  fun cambiarPerfil(f: (Perfil) -> Perfil) {
    cambiarDatos { d -> d.copy(perfil = f(d.perfil)) }
    recalcular()
  }

  fun cambiarAvisoDiario(activo: Boolean) {
    cambiarPerfil { it.copy(avisoDiario = activo) }
    runCatching { AvisoDiario.programar(app, activo, _estado.value.datos.perfil.horaAviso) }
    if (activo) avisar(app.getString(R.string.msg_aviso_activo))
  }

  fun cambiarVista(v: Vista) = cambiarPerfil { it.copy(vista = v) }

  fun reiniciarAprendizaje() {
    cambiarDatos { d -> d.copy(preferencias = Preferencias()) }
    recalcular()
    avisar(app.getString(R.string.msg_aprendizaje))
  }
}
