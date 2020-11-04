package com.overflow.flowy.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.overflow.flowy.DTO.FlowyMenuData
import com.overflow.flowy.Fragment.FragmentCamera
import com.overflow.flowy.MainActivity
import com.overflow.flowy.R

/** AdapterFlowyDescription :
 * 이 어댑터는 gif 이미지 경로를 받아서 생성이되고,
 * 화면에 보이는 아이템의 포지션이 바뀔때마다 캐치하고 gif 이미지를 보여주는 기능을 한다.*/

class AdapterFlowyMenu(val context: Context, private val menuData: Array<FlowyMenuData>) :
    RecyclerView.Adapter<AdapterFlowyMenu.menuViewHolder>() {

    private lateinit var onClick: OnItemClicked

    interface OnItemClicked {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): menuViewHolder {
        Log.d("asdsadsadasd", menuData.toString())
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.menu_item_view,
            parent, false
        )
        return menuViewHolder(view)
    }

    override fun getItemCount(): Int = menuData.size

    override fun onBindViewHolder(holder: menuViewHolder, position: Int) {
        Glide.with(context).load(menuData[position].menuImage).into(holder.imageItem)
        holder.textItem.text = menuData[position].menuText
    }

    inner class menuViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imageItem: ImageView = itemView.findViewById(R.id.menuImageItem)
        val textItem: TextView = itemView.findViewById(R.id.menuTextItem)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                onClick.onItemClick(pos)
            }
        }
    }

    fun setOnClick(click: AdapterFlowyMenu.OnItemClicked) {
        this.onClick = click
    }

}