package com.tecsup.campusgps.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "preferencias_campus")

class PreferenciasRepositoryDataStore(context: Context) : PreferenciasRepository {

    private val dataStore = context.applicationContext.dataStore
    private val claveUltimoLugar = intPreferencesKey("ultimo_lugar")

    override val ultimoLugar: Flow<Int?> = dataStore.data.map { it[claveUltimoLugar] }

    override suspend fun guardarUltimoLugar(id: Int) {
        dataStore.edit { it[claveUltimoLugar] = id }
    }
}
