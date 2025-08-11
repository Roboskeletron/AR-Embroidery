package ru.vsu.arembroidery.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignUpFragmentVM : ViewModel() {
    val username = MutableLiveData<String?>()
    val firstName = MutableLiveData<String?>()
    val lastName = MutableLiveData<String?>()
    val email = MutableLiveData<String?>()
    val phoneNumber = MutableLiveData<String?>()
    val password = MutableLiveData<String?>()
    val confirmPassword = MutableLiveData<String?>()

    fun signUp() {

    }
}