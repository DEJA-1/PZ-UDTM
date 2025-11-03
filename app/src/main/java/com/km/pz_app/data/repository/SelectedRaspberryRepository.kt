package com.km.pz_app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "raspberry_prefs")

@Singleton
class SelectedRaspberryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_SELECTED = intPreferencesKey("selected_raspberry_index")

    suspend fun getSelectedIndex(): Int {
        return context.dataStore.data.first()[KEY_SELECTED] ?: 1
    }

    suspend fun setSelectedIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED] = index
        }
    }
}