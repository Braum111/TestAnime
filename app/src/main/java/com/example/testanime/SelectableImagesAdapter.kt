package com.example.testanime

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.testanime.databinding.ItemSelectableImageBinding

class SelectableImagesAdapter(private val imagesList: List<Uri>) :
    RecyclerView.Adapter<SelectableImagesAdapter.SelectableImageViewHolder>() {

    private val selectedPositions = mutableSetOf<Int>()

    inner class SelectableImageViewHolder(private val binding: ItemSelectableImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, position: Int) {
            Glide.with(binding.imageViewSelectable.context)
                .load(uri)
                .into(binding.imageViewSelectable)

            binding.checkbox.isChecked = selectedPositions.contains(position)

            binding.root.setOnClickListener {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position)
                    binding.checkbox.isChecked = false
                } else {
                    selectedPositions.add(position)
                    binding.checkbox.isChecked = true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableImageViewHolder {
        val binding = ItemSelectableImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectableImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectableImageViewHolder, position: Int) {
        holder.bind(imagesList[position], position)
    }

    override fun getItemCount(): Int = imagesList.size

    fun getSelectedImages(): List<Uri> {
        return selectedPositions.map { imagesList[it] }
    }
}
