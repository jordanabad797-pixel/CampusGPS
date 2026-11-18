package com.tecsup.campusgps.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.campusgps.data.LugaresRepository
import com.tecsup.campusgps.data.PreferenciasRepository
import com.tecsup.campusgps.data.UbicacionNoDisponibleException
import com.tecsup.campusgps.data.UbicacionRepository
import com.tecsup.campusgps.model.Lugar
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.model.Posicion
import com.tecsup.campusgps.model.TipoError
import com.tecsup.campusgps.model.UbicacionUi
import com.tecsup.campusgps.util.distanciaMetros
import com.tecsup.campusgps.util.formatearDistancia
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapaViewModel(
    private val ubicacionRepo: UbicacionRepository,
    private val lugaresRepo: LugaresRepository,
    private val preferenciasRepo: PreferenciasRepository
) : ViewModel() {

    private val _estado = MutableStateFlow<UbicacionUi>(UbicacionUi.Inicial)
    val estado: StateFlow<UbicacionUi> = _estado.asStateFlow()

    private val _lugares = MutableStateFlow(
        lugaresRepo.obtenerLugares().map { LugarConDistancia(it, null) }
    )
    val lugares: StateFlow<List<LugarConDistancia>> = _lugares.asStateFlow()

    private val _seleccionado = MutableStateFlow<Int?>(null)
    val seleccionado: StateFlow<Int?> = _seleccionado.asStateFlow()

    private val _temaOscuro = MutableStateFlow<Boolean?>(null)
    val temaOscuro: StateFlow<Boolean?> = _temaOscuro.asStateFlow()

    val categorias: List<String> = lugaresRepo.obtenerLugares().map { it.categoria }.distinct()

    private val _categoria = MutableStateFlow<String?>(null)
    val categoria: StateFlow<String?> = _categoria.asStateFlow()

    val lugaresVisibles: StateFlow<List<LugarConDistancia>> = combine(_lugares, _categoria) { lista, cat ->
        if (cat == null) lista else lista.filter { it.lugar.categoria == cat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _lugares.value)

    val ultimoVisitado: StateFlow<Lugar?> = preferenciasRepo.ultimoLugar
        .map { id -> lugaresRepo.obtenerLugares().firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val radios: List<Int> = listOf(100, 200, 500)

    private val _radio = MutableStateFlow(200)
    val radio: StateFlow<Int> = _radio.asStateFlow()

    val dentroDelRadio: StateFlow<Set<Int>> = combine(lugaresVisibles, _radio) { lista, metrosRadio ->
        lista.filter { item ->
            val metros = item.metros
            metros != null && metros <= metrosRadio
        }.map { it.lugar.id }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val masCercano: StateFlow<Lugar?> = lugaresVisibles
        .map { lista -> lista.firstOrNull { it.metros != null }?.lugar }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _siguiendo = MutableStateFlow(false)
    val siguiendo: StateFlow<Boolean> = _siguiendo.asStateFlow()

    private var trabajoSeguimiento: Job? = null

    init {
        viewModelScope.launch {
            val id = preferenciasRepo.ultimoLugar.first()
            if (id != null && _seleccionado.value == null) {
                _seleccionado.value = id
            }
        }
    }

    fun buscar() {
        if (!ubicacionRepo.playServicesDisponible()) {
            _estado.value = UbicacionUi.Error(
                "Este dispositivo no tiene Google Play Services",
                TipoError.SIN_PLAY_SERVICES
            )
            return
        }
        _estado.value = UbicacionUi.Buscando
        viewModelScope.launch {
            ubicacionRepo.obtenerPosicion()
                .onSuccess {
                    _estado.value = UbicacionUi.Lista(it)
                    _seleccionado.value = null
                    actualizarDistancias(it)
                }
                .onFailure { _estado.value = errorDe(it) }
        }
    }

    fun iniciarSeguimiento() {
        if (trabajoSeguimiento != null) return
        if (!ubicacionRepo.playServicesDisponible()) {
            _estado.value = UbicacionUi.Error(
                "Este dispositivo no tiene Google Play Services",
                TipoError.SIN_PLAY_SERVICES
            )
            return
        }
        _siguiendo.value = true
        trabajoSeguimiento = viewModelScope.launch {
            ubicacionRepo.seguirPosicion()
                .catch { e ->
                    _estado.value = errorDe(e)
                    _siguiendo.value = false
                    trabajoSeguimiento = null
                }
                .collect { posicion ->
                    _estado.value = UbicacionUi.Lista(posicion)
                    actualizarDistancias(posicion)
                }
        }
    }

    fun detenerSeguimiento() {
        trabajoSeguimiento?.cancel()
        trabajoSeguimiento = null
        _siguiendo.value = false
    }

    fun permisoDenegado() {
        detenerSeguimiento()
        _estado.value = UbicacionUi.Error(
            "Sin permiso de ubicación no puedo mostrar dónde estás",
            TipoError.SIN_PERMISO
        )
    }

    fun cambiarTema(oscuro: Boolean) {
        _temaOscuro.value = oscuro
    }

    fun cambiarRadio(metros: Int) {
        _radio.value = metros
    }

    fun filtrar(categoria: String?) {
        _categoria.value = if (_categoria.value == categoria) null else categoria
        val actual = _seleccionado.value
        val visible = _lugares.value.any {
            it.lugar.id == actual && (_categoria.value == null || it.lugar.categoria == _categoria.value)
        }
        if (!visible) {
            _seleccionado.value = null
        }
    }

    fun seleccionar(id: Int?) {
        _seleccionado.value = id
        if (id != null) {
            viewModelScope.launch { preferenciasRepo.guardarUltimoLugar(id) }
        }
    }

    private fun actualizarDistancias(pos: Posicion) {
        val ordenados = lugaresRepo.obtenerLugares()
            .map { LugarConDistancia(it, distanciaMetros(pos, it)) }
            .sortedBy { it.metros }
        _lugares.value = ordenados
        ordenados.forEach {
            Log.d("CampusGPS", "${it.lugar.nombre}: ${formatearDistancia(it.metros ?: 0f)}")
        }
    }

    private fun errorDe(e: Throwable): UbicacionUi.Error = when (e) {
        is SecurityException -> UbicacionUi.Error(
            "Falta el permiso de ubicación",
            TipoError.SIN_PERMISO
        )
        is UbicacionNoDisponibleException -> UbicacionUi.Error(
            "No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo",
            TipoError.GPS_APAGADO
        )
        else -> UbicacionUi.Error(
            e.message ?: "No se pudo obtener la ubicación",
            TipoError.OTRO
        )
    }
}
