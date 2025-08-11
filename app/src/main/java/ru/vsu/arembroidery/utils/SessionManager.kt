package ru.vsu.arembroidery.utils


import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import ru.vsu.arembroidery.models.User

class SessionManager(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_IS_LOGGED_IN = stringPreferencesKey("is_logged_in")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_FIRST_NAME = stringPreferencesKey("first_name")
        private val KEY_LAST_NAME = stringPreferencesKey("last_name")
        private val KEY_PHONE_NUMBER = stringPreferencesKey("phone_number")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_ROLE_ID = stringPreferencesKey("role_id")
    }
    
    suspend fun saveUserSession(user: User) {
        dataStore.edit { prefs ->
           prefs[KEY_IS_LOGGED_IN]= true.toString()
           prefs[KEY_USER_ID] = user.id.toString()
           prefs[KEY_USERNAME] = user.username
           prefs[KEY_FIRST_NAME] = user.firstName
           prefs[KEY_LAST_NAME] = user.lastName
           prefs[KEY_PHONE_NUMBER] = user.phoneNumber
           prefs[KEY_EMAIL] = user.email
           prefs[KEY_ROLE_ID] = user.roleId.toString()
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
           return dataStore.data.last()[KEY_IS_LOGGED_IN].toBoolean()
    }
    
    suspend fun getCurrentUser(): User? {
        return if (isLoggedIn()) {

            dataStore.data.map { prefs ->
                User(
                    id = prefs[KEY_USER_ID]?.toInt() ?: 0,
                    username = prefs[KEY_USERNAME] ?: "",
                    firstName = prefs[KEY_FIRST_NAME] ?: "",
                    lastName = prefs[KEY_LAST_NAME] ?: "",
                    phoneNumber = prefs[KEY_PHONE_NUMBER] ?: "",
                    email = prefs[KEY_EMAIL] ?: "",
                    roleId = prefs[KEY_ROLE_ID]?.toInt() ?: 0,
                    password = ""
                )
            }.last()
        } else {
            null
        }
    }
    
    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN]= false.toString()
        }
    }
}