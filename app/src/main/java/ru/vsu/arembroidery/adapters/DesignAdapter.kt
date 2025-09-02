package ru.vsu.arembroidery.adapters

import android.graphics.drawable.DrawableWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.vsu.arembroidery.BuildConfig
import ru.vsu.arembroidery.R
import ru.vsu.arembroidery.databinding.DesignItemBinding
import ru.vsu.arembroidery.models.DesignItem

class DesignAdapter : PagingDataAdapter<DesignItem, DesignAdapter.DesignViewHolder>(DesignItemDiffCallback()) {
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
            binding.item = designItem

            if (designItem.fileId > 0) {
                Glide.with(binding.root)
                    .load("${BuildConfig.BASE_URL}api/v1/files/image/${designItem.fileId}")
                    .into(binding.designImage)
                    .onLoadFailed(ContextCompat.getDrawable(binding.root.context, R.drawable.example_texture))
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