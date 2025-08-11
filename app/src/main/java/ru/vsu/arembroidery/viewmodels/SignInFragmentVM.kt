package ru.vsu.arembroidery.viewmodels

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.passay.PasswordData
import org.passay.PasswordValidator
import ru.vsu.arembroidery.data.UserRepository
import ru.vsu.arembroidery.utils.SessionManager

class SignInFragmentVM(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val passwordValidator: PasswordValidator
) : ViewModel() {
    private val _emailError = MutableLiveData<String?>()
    private val _passwordError = MutableLiveData<String?>()

    val email = MutableLiveData<String?>()
    val password = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError
    val passwordError: LiveData<String?> = _passwordError

    fun signIn() {
        _emailError.value = "Invalid email provide".takeUnless { Patterns.EMAIL_ADDRESS.matcher(email.value ?: "").matches() }

        val passwordValidationResult = passwordValidator.validate(PasswordData(password.value))
        _passwordError.value = passwordValidator.getMessages(passwordValidationResult).joinToString(", ").takeUnless { passwordValidationResult.isValid }

        if (emailError.value?.isNotBlank() == true || passwordError.value?.isNotBlank() == true) {
            return
        }

        viewModelScope.launch {
            val r = userRepository.loginUser(email.value!!, password.value!!)

        }
    }
}
