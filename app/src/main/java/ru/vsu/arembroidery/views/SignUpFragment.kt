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
import ru.vsu.arembroidery.databinding.FragmentSignUpBinding
import ru.vsu.arembroidery.viewmodels.SignUpFragmentVM

class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding

    private val viewModel by viewModel<SignUpFragmentVM>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.signInBtn.setOnClickListener {
            findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToSignInFragment())
        }

        viewModel.signUpSuccessful.observe(viewLifecycleOwner) {
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return binding.root
    }
}