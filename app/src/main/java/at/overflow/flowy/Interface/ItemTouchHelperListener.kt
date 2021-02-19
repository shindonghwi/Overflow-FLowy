package at.overflow.flowy.Interface

/** 고대비 아이템을 드래그 앤 드랍할때 필요한 인터페이스 */
interface ItemTouchHelperListener {
    fun onItemMove(fromPosition:Int, toPosition:Int):Boolean
//    fun onItemRemove(position:Int)
}