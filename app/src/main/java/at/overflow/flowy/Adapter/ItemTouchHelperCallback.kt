package at.overflow.flowy.Adapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import at.overflow.flowy.Interface.ItemTouchHelperListener

/**
 * ItemTouchHelperCallback:
 * 사용되는 위치 : FragmentMenuContrast.kt
 *
 * 아이템을 드래그 앤 드랍하여 아이템간 순서를 변경할때 사용한다.
 * */

class ItemTouchHelperCallback(listener: ItemTouchHelperListener): ItemTouchHelper.Callback() {
    private var listener: ItemTouchHelperListener = listener

    /** 각 view에서 어떤 user action이 가능한지 정의 */
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder:RecyclerView.ViewHolder):Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
//        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, 0)
    }
    /** 사용자가 item을 drag할 때, ItemTouchHelper가 onMove()를 호출 */
    override fun onMove(recyclerView:RecyclerView, source:RecyclerView.ViewHolder, target:RecyclerView.ViewHolder):Boolean {
        return listener.onItemMove(source.adapterPosition, target.adapterPosition)
    }

    /** 사용자가 item을 swipe할 때, ItemTouchHelper가 onSwiped()를 호출 */
    override fun onSwiped(viewHolder:RecyclerView.ViewHolder, direction:Int) {
//        listener.onItemRemove(viewHolder.adapterPosition) // 원한다면 아이템을 왼쪽에서 오른쪽으로 밀어 없애는 기능을 만들수도 있다.
    }
}