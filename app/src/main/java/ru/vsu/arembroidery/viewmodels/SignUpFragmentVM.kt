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

class SignUpFragmentVM(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val passwordValidator: PasswordValidator
) : ViewModel() {
    private val _emailError = MutableLiveData<String?>()
    private val _passwordError = MutableLiveData<String?>()
    private val _usernameError = MutableLiveData<String?>()
    private val _firstNameError = MutableLiveData<String?>()
    private val _lastNameError = MutableLiveData<String?>()
    private val _phoneNumberError = MutableLiveData<String?>()
    private val _confirmPasswordError = MutableLiveData<String?>()
    private val _signUpSuccessful = MutableLiveData(false)

    val email = MutableLiveData<String?>()
    val password = MutableLiveData<String?>()
    val username = MutableLiveData<String?>()
    val firstName = MutableLiveData<String?>()
    val lastName = MutableLiveData<String?>()
    val phoneNumber = MutableLiveData<String?>()
    val confirmPassword = MutableLiveData<String?>()

    val emailError: LiveData<String?> = _emailError
    val passwordError: LiveData<String?> = _passwordError
    val usernameError: LiveData<String?> = _usernameError
    val firstNameError: LiveData<String?> = _firstNameError
    val lastNameError: LiveData<String?> = _lastNameError
    val phoneNumberError: LiveData<String?> = _phoneNumberError
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError
    val signUpSuccessful: LiveData<Boolean> = _signUpSuccessful

    fun signUp() {
        var hasError = false

        _usernameError.value = "Username is required".takeIf { username.value.isNullOrBlank() }
        if (_usernameError.value != null) hasError = true

        _firstNameError.value = "First name is required".takeIf { firstName.value.isNullOrBlank() }
        if (_firstNameError.value != null) hasError = true

        _lastNameError.value = "Last name is required".takeIf { lastName.value.isNullOrBlank() }
        if (_lastNameError.value != null) hasError = true

        _phoneNumberError.value = "Phone number is required".takeIf { phoneNumber.value.isNullOrBlank() }
        if (_phoneNumberError.value != null) hasError = true

        _emailError.value = "Invalid email provided".takeUnless { Patterns.EMAIL_ADDRESS.matcher(email.value ?: "").matches() }
        if (_emailError.value != null) hasError = true

        val passwordValidationResult = passwordValidator.validate(PasswordData(password.value))
        _passwordError.value = passwordValidator.getMessages(passwordValidationResult).joinToString(", ").takeUnless { passwordValidationResult.isValid }
        if (_passwordError.value != null) hasError = true

        _confirmPasswordError.value = "Passwords do not match".takeUnless { password.value == confirmPassword.value }
        if (_confirmPasswordError.value != null) hasError = true

        if (hasError) {
            return
        }

        viewModelScope.launch {
            userRepository.registerUser(
                username = username.value!!,
                firstName = firstName.value!!,
                lastName = lastName.value!!,
                phoneNumber = phoneNumber.value!!,
                email = email.value!!,
                password = password.value!!,
                roleId = 1 // Assuming a default roleId for now
            ).onSuccess { user ->
                sessionManager.saveUserSession(user)
                _signUpSuccessful.postValue(true)
            }
        }
    }
}
