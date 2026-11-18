package com.tecsup.campusgps.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tecsup.campusgps.model.Lugar
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.model.UbicacionUi
import com.tecsup.campusgps.util.formatearDistancia
import com.tecsup.campusgps.viewmodel.MapaViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMapa(vm: MapaViewModel, temaOscuro: Boolean) {
    val estado by vm.estado.collectAsState()
    val lugares by vm.lugaresVisibles.collectAsState()
    val categoria by vm.categoria.collectAsState()
    val radio by vm.radio.collectAsState()
    val dentroDelRadio by vm.dentroDelRadio.collectAsState()
    val ultimoVisitado by vm.ultimoVisitado.collectAsState()
    val siguiendo by vm.siguiendo.collectAsState()
    val seleccionado by vm.seleccionado.collectAsState()
    val masCercano by vm.masCercano.collectAsState()
    val contexto = LocalContext.current
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) {
            vm.buscar()
        } else {
            vm.permisoDenegado()
        }
    }

    fun tienePermiso(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    val permisoConcedido = tienePermiso()

    fun alPulsar() {
        if (tienePermiso()) {
            vm.buscar()
        } else {
            lanzador.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun alternarSeguimiento() {
        if (siguiendo) {
            vm.detenerSeguimiento()
        } else if (tienePermiso()) {
            vm.iniciarSeguimiento()
        } else {
            lanzador.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun abrirAjustes() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", contexto.packageName, null)
        )
        contexto.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusGPS", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.cambiarTema(!temaOscuro) }) {
                        Text(text = if (temaOscuro) "☀️" else "🌙", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        val error = estado as? UbicacionUi.Error
        if (error != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Button(onClick = { alPulsar() }) {
                    Text("Obtener mi ubicación", fontWeight = FontWeight.Bold)
                }
                MensajeError(
                    error = error,
                    onAbrirAjustes = { abrirAjustes() },
                    onReintentar = { alPulsar() }
                )
            }
        } else if (horizontal) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MapaUbicacion(
                    estado = estado,
                    lugares = lugares,
                    seleccionado = seleccionado,
                    masCercanoId = masCercano?.id,
                    permisoConcedido = permisoConcedido,
                    radio = radio,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Controles(
                        estado = estado,
                        lugares = lugares,
                        masCercano = masCercano,
                        ultimoVisitado = ultimoVisitado,
                        siguiendo = siguiendo,
                        onSeguimiento = { alternarSeguimiento() },
                        onObtener = { alPulsar() },
                        onIrMasCercano = { masCercano?.let { vm.seleccionar(it.id) } }
                    )
                    FiltroCategorias(
                        categorias = vm.categorias,
                        seleccionada = categoria,
                        onFiltrar = { vm.filtrar(it) }
                    )
                    FiltroRadio(
                        radios = vm.radios,
                        seleccionado = radio,
                        onCambiar = { vm.cambiarRadio(it) }
                    )
                    ListaLugares(
                        lugares = lugares,
                        seleccionado = seleccionado,
                        onClick = { vm.seleccionar(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        masCercanoId = masCercano?.id,
                        dentroIds = dentroDelRadio
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MapaUbicacion(
                    estado = estado,
                    lugares = lugares,
                    seleccionado = seleccionado,
                    masCercanoId = masCercano?.id,
                    permisoConcedido = permisoConcedido,
                    radio = radio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Controles(
                    estado = estado,
                    lugares = lugares,
                    masCercano = masCercano,
                    ultimoVisitado = ultimoVisitado,
                    siguiendo = siguiendo,
                    onSeguimiento = { alternarSeguimiento() },
                    onObtener = { alPulsar() },
                    onIrMasCercano = { masCercano?.let { vm.seleccionar(it.id) } }
                )
                FiltroCategorias(
                    categorias = vm.categorias,
                    seleccionada = categoria,
                    onFiltrar = { vm.filtrar(it) }
                )
                FiltroRadio(
                    radios = vm.radios,
                    seleccionado = radio,
                    onCambiar = { vm.cambiarRadio(it) }
                )
                ListaLugares(
                    lugares = lugares,
                    seleccionado = seleccionado,
                    onClick = { vm.seleccionar(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    masCercanoId = masCercano?.id,
                    dentroIds = dentroDelRadio
                )
            }
        }
    }
}

@Composable
private fun Controles(
    estado: UbicacionUi,
    lugares: List<LugarConDistancia>,
    masCercano: Lugar?,
    ultimoVisitado: Lugar?,
    siguiendo: Boolean,
    onSeguimiento: () -> Unit,
    onObtener: () -> Unit,
    onIrMasCercano: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(
            onClick = onObtener,
            enabled = estado !is UbicacionUi.Buscando
        ) {
            Text("Obtener mi ubicación", fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onIrMasCercano,
                enabled = masCercano != null,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(
                    1.dp,
                    if (masCercano != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    }
                )
            ) {
                Text("Ir al más cercano", fontWeight = FontWeight.Bold)
            }
            val colorSeguir = if (siguiendo) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
            OutlinedButton(
                onClick = onSeguimiento,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorSeguir),
                border = BorderStroke(1.dp, colorSeguir)
            ) {
                Text(
                    text = if (siguiendo) "Detener seguimiento" else "Iniciar seguimiento",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val cercano = lugares.firstOrNull { it.lugar.id == masCercano?.id }
        val metrosCercano = cercano?.metros
        if (cercano != null && metrosCercano != null) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Más cercano: ")
                    }
                    append("${cercano.lugar.nombre} (${formatearDistancia(metrosCercano)})")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (ultimoVisitado != null) {
            Text(
                text = "Último lugar visitado: ${ultimoVisitado.nombre}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when (estado) {
            UbicacionUi.Inicial -> Text(
                text = "Pulsa el botón para empezar",
                style = MaterialTheme.typography.bodySmall
            )
            UbicacionUi.Buscando -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            is UbicacionUi.Lista -> Text(
                text = "Latitud: ${String.format(Locale.US, "%.4f", estado.posicion.latitud)} · " +
                    "Longitud: ${String.format(Locale.US, "%.4f", estado.posicion.longitud)}",
                style = MaterialTheme.typography.bodySmall
            )
            is UbicacionUi.Error -> Unit
        }
    }
}

@Composable
private fun FiltroCategorias(
    categorias: List<String>,
    seleccionada: String?,
    onFiltrar: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorias.forEach { cat ->
            FilterChip(
                selected = cat == seleccionada,
                onClick = { onFiltrar(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun FiltroRadio(
    radios: List<Int>,
    seleccionado: Int,
    onCambiar: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Radio:",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        radios.forEach { metros ->
            FilterChip(
                selected = metros == seleccionado,
                onClick = { onCambiar(metros) },
                label = { Text("$metros m") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
