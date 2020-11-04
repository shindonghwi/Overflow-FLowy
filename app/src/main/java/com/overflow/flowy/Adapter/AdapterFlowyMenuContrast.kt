package com.overflow.flowy.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.overflow.flowy.DTO.ContrastData
import com.overflow.flowy.Interface.ItemTouchHelperListener
import com.overflow.flowy.R
import com.overflow.flowy.Util.LuminanceDefaultData
import com.overflow.flowy.Util.THIS_CONTEXT

/** AdapterFlowyDescription :
 * 이 어댑터는 gif 이미지 경로를 받아서 생성이되고,
 * 화면에 보이는 아이템의 포지션이 바뀔때마다 캐치하고 gif 이미지를 보여주는 기능을 한다.*/

class AdapterFlowyMenuContrast(
    val context: Context,
    private val menuContrastData: ArrayList<ContrastData>
) :
    RecyclerView.Adapter<AdapterFlowyMenuContrast.menuContrastViewHolder>(),
    ItemTouchHelperListener {

    private lateinit var onClick: OnItemClicked
    private lateinit var onDrag: ItemTouchHelperListener

    interface OnItemClicked {
        fun onItemClick(position: Int)
    }

    interface ItemTouchHelperListener {
        fun onItemMove(fromPosition:Int, toPosition:Int):Boolean
//        fun onItemRemove(position:Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): menuContrastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.menu_contrast_item_view,
            parent, false
        )
        return menuContrastViewHolder(view)
    }

    override fun getItemCount(): Int = menuContrastData.size

    override fun onBindViewHolder(holder: menuContrastViewHolder, position: Int) {
        holder.contrastTextInfo.text = menuContrastData[position].contrastTextInfo
//        holder.contrastLeftImage.setBackgroundColor((menuContrastData[position].contrastLeftImage!!))
//        holder.contrastRightImage.setBackgroundColor((menuContrastData[position].contrastRightImage!!))

        holder.contrastLeftImage.setBackgroundColor(THIS_CONTEXT!!.resources.getColor(menuContrastData[position].contrastLeftImage!!))
        holder.contrastRightImage.setBackgroundColor(THIS_CONTEXT!!.resources.getColor(menuContrastData[position].contrastRightImage!!))
    }

    inner class menuContrastViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val removeBtnImage: ImageView = itemView.findViewById(R.id.removeBtnImage)
        val contrastLeftImage: ImageView = itemView.findViewById(R.id.contrastLeftImage)
        val contrastRightImage: ImageView = itemView.findViewById(R.id.contrastRightImage)
        val contrastTextInfo: TextView = itemView.findViewById(R.id.contrastTextInfo)
        val dragAndDropImage: ImageView = itemView.findViewById(R.id.dragAndDropImage)

        init {
            removeBtnImage.setOnClickListener {
                val pos = adapterPosition
                onClick.onItemClick(pos)
            }
        }
    }

    fun setOnClick(click: OnItemClicked) {
        this.onClick = click
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || fromPosition >= menuContrastData.size || toPosition < 0 || toPosition >= menuContrastData.size)
        {
            return false
        }
        val fromItem = menuContrastData[fromPosition]
        menuContrastData.removeAt(fromPosition)
        menuContrastData.add(toPosition, fromItem)

        val moveItem = LuminanceDefaultData[fromPosition]
        LuminanceDefaultData.removeAt(fromPosition)
        LuminanceDefaultData.add(toPosition, moveItem)

        notifyItemMoved(fromPosition, toPosition)
        return true
    }

//    override fun onItemRemove(position: Int) {
//        menuContrastData.removeAt(position)
//        notifyItemRemoved(position)
//    }


}