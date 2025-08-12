package ru.vsu.arembroidery.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.vsu.arembroidery.data.MatrixRepository
import ru.vsu.arembroidery.data.UserRepository
import ru.vsu.arembroidery.utils.SessionManager

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ru.vsu.arembroidery.preferences")

val dataModule = module {
    single { MatrixRepository() }
    single { SessionManager(androidContext().dataStore) }
    single { UserRepository(get()) }
}
