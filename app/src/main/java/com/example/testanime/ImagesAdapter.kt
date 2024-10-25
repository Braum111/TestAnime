package com.example.testanime
import android.annotation.SuppressLint
import android.view.View
import android.content.Context
import android.net.Uri
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.testanime.databinding.ItemImageBinding

class ImagesAdapter(private val imagesList: List<Uri>) :
    RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(uri: Uri) {
            Glide.with(binding.imageViewItem.context)
                .load(uri)
                .into(binding.imageViewItem)

            val gestureDetector = GestureDetector(binding.root.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Реализуйте копирование изображения по вашему усмотрению
                    showCopyMenu(binding.root.context, uri)
                    return true
                }
            })

            binding.root.setOnTouchListener { view: View, event: MotionEvent ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
    }

    private fun showCopyMenu(context: Context, uri: Uri) {
        // Реализуйте копирование изображения
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun getItemCount(): Int = imagesList.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imagesList[position])
    }
}
