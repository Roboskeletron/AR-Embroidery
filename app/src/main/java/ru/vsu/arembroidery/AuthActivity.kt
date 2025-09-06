package ru.vsu.arembroidery

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import ru.vsu.arembroidery.databinding.ActivityAuthBinding
import ru.vsu.arembroidery.utils.AuthManager

class AuthActivity : AppCompatActivity() {
    private val authManager by inject<AuthManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        runBlocking {
            authManager.getCurrentUserId()?.let {
                val intent = Intent(this@AuthActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}