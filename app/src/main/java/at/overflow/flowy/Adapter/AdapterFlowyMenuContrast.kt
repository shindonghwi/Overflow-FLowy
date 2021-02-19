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

/** AdapterFlowyMenuContrast :
 *
 * 사용되는 위치 : FragmentMenuContrast.kt / 화면 왼쪽 하단에 메뉴-대비 버튼을 눌러서 나오는 화면이다.
 *
 * x 버튼을 눌러서 아이템을 지울 수 도 있고, 아이템을 꾹 누른 후 위아래 드래그 앤 드랍으로 아이템간 순서를 변경할 수 도 있다.
 * 불편한것 같고 사용자가 못 알아차릴 것 같은 기능인데 ios에서 이렇게 구현이 되어있다.
 * 차후에 모바일 UI/UX 회의를 할때 변경 할 수 있도록 얘기를 하자
 *
 * */

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
        /** userContrastData 는 사용자가 가지고 있는 고대비 색상 값 4가지 이다. */
        holder.contrastTextInfo.text = userContrastData[position].contrastTextInfo // 색상 값 글자 , ex ) 흑/백
        holder.contrastLeftImage.setBackgroundColor(context.resources.getColor(userContrastData[position].contrastLeftImage!!)) // 색상값 1 , ex) 흑색
        holder.contrastRightImage.setBackgroundColor(context.resources.getColor(userContrastData[position].contrastRightImage!!)) // 색상값 2 , ex) 백색
    }

    inner class menuContrastViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val removeBtnImage: ImageView = itemView.findViewById(R.id.removeBtnImage)
        val contrastLeftImage: ImageView = itemView.findViewById(R.id.contrastLeftImage)
        val contrastRightImage: ImageView = itemView.findViewById(R.id.contrastRightImage)
        val contrastTextInfo: TextView = itemView.findViewById(R.id.contrastTextInfo)
        val dragAndDropImage: ImageView = itemView.findViewById(R.id.dragAndDropImage)

        /** 사용하고 싶지 않은 고대비 색상을 지울 수 있도록 아이템마다 삭제이벤트를 달아주었음 */
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

    /** 고대비 항목 아이템을 꾹 눌러서 순서를 변경 할 수 있는 기능 */
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