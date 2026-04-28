package ru.vsu.arembroidery.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import org.json.JSONException
import ru.vsu.arembroidery.BuildConfig
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val AUTH_STATE_KEY = stringPreferencesKey("AUTH_STATE")
        private val USER_ID = stringPreferencesKey("USER_ID")
        private const val TAG = "AuthManager"
    }

    private val currentAuthState = AtomicReference<AuthState>()
    private val authService = AuthorizationService(context)

    suspend fun restoreAuthState(){
        val json = dataStore.data.firstOrNull()?.get(AUTH_STATE_KEY)
        if (json != null) {
            try {
                currentAuthState.set(AuthState.jsonDeserialize(json))
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to deserialize auth state", e)
            }
        }
    }

    suspend fun getAuthRequest(): AuthorizationRequest = suspendCancellableCoroutine { continuation ->
        val issuerUri = BuildConfig.KEYCLOAK_ISSUER_URI.toUri()
        AuthorizationServiceConfiguration.fetchFromIssuer(issuerUri) { config, ex ->
            if (ex != null) {
                continuation.resumeWithException(ex)
                return@fetchFromIssuer
            }

            if (config != null) {
                val builder = AuthorizationRequest.Builder(
                    config,
                    BuildConfig.CLIENT_ID,
                    ResponseTypeValues.CODE,
                    "${BuildConfig.APPLICATION_ID}://oauth".toUri()
                )
                builder.setScope("openid profile email offline_access")
                continuation.resume(builder.build())
            } else {
                continuation.resumeWithException(Exception("Config is null"))
            }
        }
    }

    fun getAuthorizationIntent(request: AuthorizationRequest): Intent {
        return authService.getAuthorizationRequestIntent(request)
    }

    suspend fun handleAuthorizationResponse(intent: Intent): Boolean {
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        if (response != null) {
            val authState = AuthState(response, ex)
            currentAuthState.set(authState)

            return try {
                val tokenResponse = suspendCancellableCoroutine { continuation ->
                    authService.performTokenRequest(response.createTokenExchangeRequest()) { tr, exception ->
                        if (exception != null) {
                            continuation.resumeWithException(exception)
                        } else {
                            continuation.resume(tr)
                        }
                    }
                }

                if (tokenResponse != null) {
                    authState.update(tokenResponse, null)
                    persistAuthState(authState)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token exchange failed", e)
                false
            }
        } else {
            return false
        }
    }

    fun getAccessToken(): String? {
        val authState = currentAuthState.get() ?: return null

        var accessToken: String? = null
        val lock = java.util.concurrent.CountDownLatch(1)

        authState.performActionWithFreshTokens(authService) { token, _, ex ->
            accessToken = token
            if (ex != null) {
                Log.e(TAG, "Failed to get fresh token", ex)
            }

            runBlocking(Dispatchers.IO) {
                persistAuthState(authState)
            }
            lock.countDown()
        }

        try {
            lock.await()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted waiting for token", e)
        }

        return accessToken
    }

    fun isAuthorized(): Boolean {
        return currentAuthState.get()?.isAuthorized == true
    }

    suspend fun logout() {
        currentAuthState.set(null)
        dataStore.edit { it.remove(AUTH_STATE_KEY) }
    }

    suspend fun persistAuthState(authState: AuthState){
        dataStore.edit { prefs ->
            prefs[AUTH_STATE_KEY] = authState.jsonSerializeString()
        }

        currentAuthState.set(authState)
    }
}