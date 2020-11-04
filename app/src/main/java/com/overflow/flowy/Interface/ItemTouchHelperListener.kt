package com.overflow.flowy.Interface

interface ItemTouchHelperListener {
    fun onItemMove(fromPosition:Int, toPosition:Int):Boolean
//    fun onItemRemove(position:Int)
}