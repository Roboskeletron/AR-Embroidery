package ru.vsu.arembroidery.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import ru.vsu.arembroidery.data.UserRepository

class AuthManager(
    private val dataStore: DataStore<Preferences>,
    private val userRepository: UserRepository
) {
    companion object {
        private val USER_ID = stringPreferencesKey("USER_ID")
    }

    suspend fun signIn(email: String, password: String) : Result<Int> = runCatching {
        val signInResult = userRepository.loginUser(email, password).getOrThrow()

        dataStore.edit { prefs ->
            prefs[USER_ID] = signInResult.id.toString()
        }

       signInResult.id
    }

    suspend fun getCurrentUserId() : Int? = dataStore.data.firstOrNull()?.get(USER_ID)?.toInt()

    suspend fun signUp(
        username: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ) : Result<Int> = runCatching {
        // TODO add support for other roles, use user role for now
        val userId = userRepository.registerUser(
            username,
            firstName,
            lastName,
            phoneNumber,
            email,
            password,
            passwordConfirmation,
            3
        ).getOrThrow()

        dataStore.edit { prefs ->
            prefs[USER_ID] = userId.toString()
        }

        userId
    }
}