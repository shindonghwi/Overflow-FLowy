package at.overflow.flowy.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import at.overflow.flowy.DTO.FlowyMenuData
import at.overflow.flowy.R

/**
 * AdapterFlowyMenu:
 *
 * 사용되는 위치 : AdapterFlowyMenu.kt / 화면 왼쪽 하단에 메뉴 버튼을 눌러서 나오는 화면이다.
 *
 * 현재는 [ 대비, 자습서, 정보에 대한 기능밖에없다.]
 * 차후에 기능이 추가 될 수 도 있기에 recyclerview 로 제작함.
 * 각 항목을 누르면 다른 화면으로 이동 할 수 있게 onItemClicked interface 를 구현하였다.
 *
 * */

class AdapterFlowyMenu(val context: Context, private val menuData: Array<FlowyMenuData>) :
    RecyclerView.Adapter<AdapterFlowyMenu.menuViewHolder>() {

    private lateinit var onClick: OnItemClicked

    interface OnItemClicked {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterFlowyMenu.menuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.menu_item_view,
            parent, false
        )
        return menuViewHolder(view)
    }

    override fun getItemCount(): Int = menuData.size

    override fun onBindViewHolder(holder: AdapterFlowyMenu.menuViewHolder, position: Int) {
        Glide.with(context).load(menuData[position].menuImage).into(holder.imageItem)
        holder.textItem.text = menuData[position].menuText
    }

    inner class menuViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imageItem: ImageView = itemView.findViewById(R.id.menuImageItem)
        val textItem: TextView = itemView.findViewById(R.id.menuTextItem)

        /** 각 항목을 눌렀을때 다른 화면으로 이동시켜주기 위하여 항목마다 클릭이벤트를 달아주었다. */
        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                onClick.onItemClick(pos)
            }
        }
    }

    fun setOnClick(click: OnItemClicked) {
        this.onClick = click
    }

}