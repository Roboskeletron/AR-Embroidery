package ru.vsu.arembroidery.viewmodels

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.passay.PasswordData
import org.passay.PasswordValidator
import ru.vsu.arembroidery.utils.AuthManager

class SignInFragmentVM(
    private val authManager: AuthManager,
    private val passwordValidator: PasswordValidator
) : ViewModel() {

    companion object{
        private const val  TAG = "SignInFragmentVM"
    }

    private val _emailError = MutableLiveData<String?>()
    private val _passwordError = MutableLiveData<String?>()
    private val _signInError = MutableLiveData<String?>()
    private val _signInSuccessful = MutableLiveData<Boolean>()

    val email = MutableLiveData<String?>()
    val password = MutableLiveData<String?>()

    val emailError: LiveData<String?> = _emailError
    val passwordError: LiveData<String?> = _passwordError
    val signInError: LiveData<String?> = _signInError
    val signInSuccessful: LiveData<Boolean> = _signInSuccessful

    fun signIn() {
        _emailError.value = "Invalid email provide".takeUnless { Patterns.EMAIL_ADDRESS.matcher(email.value ?: "").matches() }

        val passwordValidationResult = passwordValidator.validate(PasswordData(password.value))
        _passwordError.value = passwordValidator.getMessages(passwordValidationResult).joinToString(", ").takeUnless { passwordValidationResult.isValid }

        if (emailError.value?.isNotBlank() == true || passwordError.value?.isNotBlank() == true) {
            return
        }

        viewModelScope.launch {
            authManager.signIn(email.value!!, password.value!!)
                .onSuccess {
                    _signInSuccessful.postValue(true)
                }
                .onFailure {
                    _signInError.postValue("Invalid email or password")
                    Log.e(TAG, "Failed to sign in", it)
                }
        }
    }
}
