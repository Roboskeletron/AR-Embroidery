package ru.vsu.arembroidery.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.lifecycle.withCreated
import androidx.lifecycle.withStarted
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.vsu.arembroidery.adapters.DesignAdapter
import ru.vsu.arembroidery.databinding.FragmentDesignsBinding
import ru.vsu.arembroidery.viewmodels.DesignsFragmentVM

class DesignsFragment : Fragment() {
    private lateinit var binding: FragmentDesignsBinding
    private val viewModel by viewModel<DesignsFragmentVM>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDesignsBinding.inflate(inflater, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.toolbar.setupWithNavController(findNavController())

        val designAdapter = DesignAdapter()

        binding.designsGrid.adapter = designAdapter

        with(viewLifecycleOwner.lifecycleScope) {
            launch {
                whenStarted {
                    viewModel.designsFlow.collectLatest {
                        designAdapter.submitData(it)
                    }
                }
            }
        }

        return binding.root
    }
}