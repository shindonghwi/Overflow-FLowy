package at.overflow.flowy.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import at.overflow.flowy.Fragment.FragmentCamera.Companion.userContrastData
import at.overflow.flowy.Interface.ItemTouchHelperListener
import at.overflow.flowy.R
import at.overflow.flowy.Util.THIS_CONTEXT

/** AdapterFlowyDescription :
 * 이 어댑터는 gif 이미지 경로를 받아서 생성이되고,
 * 화면에 보이는 아이템의 포지션이 바뀔때마다 캐치하고 gif 이미지를 보여주는 기능을 한다.*/

class AdapterFlowyMenuContrast(
    val context: Context
) :
    RecyclerView.Adapter<AdapterFlowyMenuContrast.menuContrastViewHolder>(),
    ItemTouchHelperListener {

    private lateinit var onClick: OnItemClicked
    private lateinit var onDrag: ItemTouchHelperListener

    interface OnItemClicked {
        fun onRemoveItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterFlowyMenuContrast.menuContrastViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.menu_contrast_item_view,
            parent, false
        )
        return menuContrastViewHolder(view)
    }

    override fun getItemCount(): Int = userContrastData.size

    override fun onBindViewHolder(holder: AdapterFlowyMenuContrast.menuContrastViewHolder, position: Int) {
        holder.contrastTextInfo.text = userContrastData[position].contrastTextInfo
        holder.contrastLeftImage.setBackgroundColor(THIS_CONTEXT!!.resources.getColor(userContrastData[position].contrastLeftImage!!))
        holder.contrastRightImage.setBackgroundColor(THIS_CONTEXT!!.resources.getColor(userContrastData[position].contrastRightImage!!))
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
                onClick.onRemoveItemClick(pos)
            }
        }
    }

    fun setOnClick(click: OnItemClicked) {
        this.onClick = click
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || fromPosition >= userContrastData.size || toPosition < 0 || toPosition >= userContrastData.size)
        {
            return false
        }
        val fromItem = userContrastData[fromPosition]
        userContrastData.removeAt(fromPosition)
        userContrastData.add(toPosition, fromItem)
        Log.d("movePosition", "$fromPosition :  $toPosition")
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

//    override fun onItemRemove(position: Int) {
//        menuContrastData.removeAt(position)
//        notifyItemRemoved(position)
//    }


}