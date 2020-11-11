package at.overflow.flowy.Util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class LayoutInflateUtil {

    /** xml. layout을 동적으로 추가할때 사용하는 함수 */
    fun layoutViewCreate(context: Context, parentViewId : ViewGroup, addLayout : Int): View {
        // 인플레이션 수행
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // inflate()메소드의 파라미터로 R.layout.button과 contentsLayout객체를 전달하고 앋.
        val view = inflater.inflate(addLayout, parentViewId, false)
        parentViewId.addView(view)
        return view
    }

    /** xml. layout을 동적으로 추가된 view를 제거할때 사용하는 함수 */
    fun layoutViewRemove(parentViewId: ViewGroup, removeView : View){
        parentViewId.removeView(removeView)
    }

}