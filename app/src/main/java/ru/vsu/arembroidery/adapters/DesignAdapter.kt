package ru.vsu.arembroidery.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.vsu.arembroidery.BuildConfig
import ru.vsu.arembroidery.databinding.DesignItemBinding
import ru.vsu.arembroidery.models.DesignItem
import ru.vsu.arembroidery.usecases.SelectEmbroideryUseCase

class DesignAdapter(
    private val selectEmbroideryUseCase: SelectEmbroideryUseCase
) : PagingDataAdapter<DesignItem, DesignAdapter.DesignViewHolder>(DesignItemDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DesignViewHolder {
        val binding = DesignItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DesignViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DesignViewHolder,
        position: Int
    ) {
        val item = getItem(position)

        item?.let {
            holder.bind(it)
        }
    }

    inner class DesignViewHolder(
        private val binding: DesignItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(designItem: DesignItem){
            binding.apply {
                item = designItem
                if (designItem.fileId > 0) {
                    Glide.with(root)
                        .load("${BuildConfig.BASE_URL}api/v1/files/image/${designItem.fileId}")
                        .into(designImage)

                    root.apply {
                        designImage.setOnClickListener {
                            selectEmbroideryUseCase.invoke(context, designImage.drawable.toBitmap(), designItem.id)
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    class DesignItemDiffCallback : DiffUtil.ItemCallback<DesignItem>() {
        override fun areItemsTheSame(
            oldItem: DesignItem,
            newItem: DesignItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DesignItem,
            newItem: DesignItem
        ): Boolean {
            return oldItem == newItem
        }

    }
}