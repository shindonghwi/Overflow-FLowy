package com.overflow.flowy.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.overflow.flowy.R

/** AdapterFlowyDescription :
 * 이 어댑터는 gif 이미지 경로를 받아서 생성이되고,
 * 화면에 보이는 아이템의 포지션이 바뀔때마다 캐치하고 gif 이미지를 보여주는 기능을 한다.*/

class AdapterFlowyDescription(val context: Context, private val gif_images_path: Array<Int>): RecyclerView.Adapter<AdapterFlowyDescription.viewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder =
        viewHolder(
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.flowy_description_gif_view,
                parent,
                false
            )
        )

    override fun getItemCount(): Int = gif_images_path.size

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        Glide.with(context).load(gif_images_path.get(position)).into(holder.gif_image)
    }

    class viewHolder(view: View): RecyclerView.ViewHolder(view) {
        val gif_image: PhotoView = view.findViewById(R.id.photo_view)
    }

}