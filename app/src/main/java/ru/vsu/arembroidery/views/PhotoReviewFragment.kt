package ru.vsu.arembroidery.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.databinding.FragmentPhotoReviewBinding
import ru.vsu.arembroidery.di.GlideApp
import ru.vsu.arembroidery.viewmodels.PhotoReviewFragmentVM
import androidx.core.net.toUri

class PhotoReviewFragment : Fragment() {
    private lateinit var binding : FragmentPhotoReviewBinding

    private val args: PhotoReviewFragmentArgs by navArgs()

    private val viewModel by viewModel<PhotoReviewFragmentVM>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotoReviewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoUri = args.photoUri.toUri()

        GlideApp.with(this)
            .load(photoUri)
            .into(binding.photoImageView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteEvent.collect {
                findNavController().navigateUp()
            }
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    viewModel.deletePhoto(requireContext().contentResolver, photoUri)
                    true
                }
                else -> false
            }
        }

        binding.shareFab.setOnClickListener {
            sharePhoto(photoUri)
        }
    }

    private fun sharePhoto(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Photo"))
    }
}