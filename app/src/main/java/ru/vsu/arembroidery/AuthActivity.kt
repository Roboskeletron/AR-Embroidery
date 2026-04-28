package ru.vsu.arembroidery

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import org.koin.android.ext.android.inject
import ru.vsu.arembroidery.databinding.ActivityAuthBinding
import ru.vsu.arembroidery.utils.AuthManager

class AuthActivity : AppCompatActivity() {
    private val authManager by inject<AuthManager>()

    private val launchAuthActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let {
            handleAuthResponse(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            authManager.restoreAuthState()


            if (authManager.isAuthorized()) {
                navigateToMain()
                return@launch
            }
            val authorizationRequest = authManager.getAuthRequest()

            val authIntent = authManager.getAuthorizationIntent(authorizationRequest)

            launchAuthActivity.launch(authIntent)

        }

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthResponse(intent)
    }

    private fun handleAuthResponse(intent: Intent) {
        lifecycleScope.launch {
            try {
                val success = authManager.handleAuthorizationResponse(intent)
                if (success) {
                    navigateToMain()
                } else {
                    Toast.makeText(this@AuthActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AuthActivity", "Error handling response", e)
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@AuthActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}