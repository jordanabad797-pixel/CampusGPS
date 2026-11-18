package com.tecsup.campusgps.data

import com.tecsup.campusgps.model.Posicion
import kotlinx.coroutines.flow.Flow

interface UbicacionRepository {
    suspend fun obtenerPosicion(): Result<Posicion>
    fun seguirPosicion(): Flow<Posicion>
    fun playServicesDisponible(): Boolean
}

class UbicacionNoDisponibleException : Exception("No se pudo obtener la ubicación")
