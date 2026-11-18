package com.tecsup.campusgps.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tecsup.campusgps.model.Posicion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UbicacionRepositoryGoogle(context: Context) : UbicacionRepository {

    private val contexto = context.applicationContext
    private val cliente = LocationServices.getFusedLocationProviderClient(contexto)

    @SuppressLint("MissingPermission")
    override suspend fun obtenerPosicion(): Result<Posicion> = runCatching {
        val ubicacion = cliente.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await() ?: throw UbicacionNoDisponibleException()
        Posicion(ubicacion.latitude, ubicacion.longitude)
    }

    @SuppressLint("MissingPermission")
    override fun seguirPosicion(): Flow<Posicion> = callbackFlow {
        val solicitud = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                resultado.lastLocation?.let {
                    trySend(Posicion(it.latitude, it.longitude))
                }
            }
        }
        cliente.requestLocationUpdates(solicitud, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
        awaitClose { cliente.removeLocationUpdates(callback) }
    }

    override fun playServicesDisponible(): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(contexto) == ConnectionResult.SUCCESS
}
