package at.overflow.flowy.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceFlag
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceIndex
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceToggleBtn
import at.overflow.flowy.Fragment.FragmentCamera.Companion.userContrastData
import at.overflow.flowy.R
import at.overflow.flowy.Util.THIS_CONTEXT
import at.overflow.flowy.Util.fragmentType

/** AdapterBrightShadeControl :
 *
 *  사용되는 위치 : FragmentCamera.kt / 화면 하단 메뉴바에서 가장 오른쪽 버튼을 누르면 밝기/대비 조절 창에서 가장 상단에 보이는 고대비 색상값에 사용
 *
 *  밝기/대비 조절 뷰를 생성 후 뷰 안에서 고대비 색상값을 바꾸기 위해서 필요한 어댑터
 *  차후에는 사용자가 원하는 색상을 넣을 수 있도록 구현을 하면 어떻게냐는 얘기도 나왔었음.
 *  */

class AdapterBrightShadeControl(
    val context: Context
) :
    RecyclerView.Adapter<AdapterBrightShadeControl.brightShadeViewHolder>() {
    var holderHashSet: LinkedHashSet<AdapterBrightShadeControl.brightShadeViewHolder> = LinkedHashSet<AdapterBrightShadeControl.brightShadeViewHolder>()
    val TAG : String by lazy { "AdapterBrightShadeControl" }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): brightShadeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.bright_shade_item_view,
            parent, false
        )

        val holder = brightShadeViewHolder(view)
        holderHashSet.add(holder)
        return holder
    }

    override fun getItemCount(): Int = userContrastData.size

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: brightShadeViewHolder, position: Int) {

        Log.d(TAG, "onBindViewHolder: $userContrastData")

        /** luminanceIndex는 고대비 모드 순서이다. [ 기본값, 흑/백, 흑/황, 청/백, 청/황 ] 순으로 되어있다.
         *  현재 선택된 모드만 활성화 시키고 나머지는 비활성화 시킨다.
         * */
        val itr = holderHashSet.iterator()
        var itrIdx = 0
        while (itr.hasNext()){
            if (itrIdx != luminanceIndex - 1) itr.next().brightShadeItemViewLayout.background = null
            else itr.next().brightShadeItemViewLayout.background = context.getDrawable(R.drawable.bright_shade_border)
            itrIdx += 1
        }

        /** 각 아이템의 배경을 지정한다 [ 흑/백, 흑/황, 청/백, 청/황 ] */
        holder.contrastLeftImage.setBackgroundColor(ContextCompat.getColor(context, userContrastData[position].contrastLeftImage!!))
        holder.contrastRightImage.setBackgroundColor(ContextCompat.getColor(context, userContrastData[position].contrastRightImage!!))

        /** 각 아이템마다 클릭 이벤트를 달아준다. 각 버튼을 클릭하면 해당 색상으로 고대비 모드가 변경된다. */
        holder.brightShadeItemViewLayout.setOnClickListener {
            val itra = holderHashSet.iterator()
            var itraIdx = 0
            while (itra.hasNext()){
                if (itraIdx != position) itra.next().brightShadeItemViewLayout.background = null
                else {
                    itra.next().brightShadeItemViewLayout.background = context.getDrawable(R.drawable.bright_shade_border)
                    luminanceIndex = holder.adapterPosition + 1
                    luminanceFlag = true // 고대비 모드 활성화
                    fragmentType = "luminance" // 고대비 모드 활성화
                    luminanceToggleBtn.isChecked = true
                }
                itraIdx += 1
            }
        }
    }

    class brightShadeViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val brightShadeItemViewLayout: LinearLayout =
            itemView.findViewById(R.id.brightShadeItemViewLayout)
        val contrastLeftImage: ImageView = itemView.findViewById(R.id.contrastLeftImage)
        val contrastRightImage: ImageView = itemView.findViewById(R.id.contrastRightImage)
    }
}