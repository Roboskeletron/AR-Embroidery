package ru.vsu.arembroidery.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.vsu.arembroidery.MainActivity
import ru.vsu.arembroidery.databinding.FragmentSignInBinding
import ru.vsu.arembroidery.viewmodels.SignInFragmentVM

class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding

    private val viewModel by viewModel<SignInFragmentVM>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSignInBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.signUpBtn.setOnClickListener {
            findNavController().navigate(SignInFragmentDirections.actionSignInFragmentToSignUpFragment())
        }

        viewModel.signInSuccessful.observe(viewLifecycleOwner) {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return binding.root
    }
}