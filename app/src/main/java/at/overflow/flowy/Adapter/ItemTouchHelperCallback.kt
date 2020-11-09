package at.overflow.flowy.Adapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import at.overflow.flowy.Interface.ItemTouchHelperListener

class ItemTouchHelperCallback(listener: ItemTouchHelperListener): ItemTouchHelper.Callback() {
    private var listener: ItemTouchHelperListener = listener

    // 각 view에서 어떤 user action이 가능한지 정의
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder:RecyclerView.ViewHolder):Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
//        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, 0)
    }
    // user가 item을 drag할 때, ItemTouchHelper가 onMove()를 호출
    override fun onMove(recyclerView:RecyclerView,
               source:RecyclerView.ViewHolder, target:RecyclerView.ViewHolder):Boolean {
        return listener.onItemMove(source.adapterPosition, target.adapterPosition)
    }

    // user가 item을 swipe할 때, ItemTouchHelper가 onSwiped()를 호출
    override fun onSwiped(viewHolder:RecyclerView.ViewHolder, direction:Int) {
//        listener.onItemRemove(viewHolder.adapterPosition)
    }
}