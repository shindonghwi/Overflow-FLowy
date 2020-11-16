package at.overflow.flowy.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceFlag
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceIndex
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceToggleBtn
import at.overflow.flowy.Fragment.FragmentCamera.Companion.userContrastData
import at.overflow.flowy.R
import at.overflow.flowy.Util.THIS_CONTEXT
import at.overflow.flowy.Util.fragmentType

/** AdapterFlowyDescription :
 * 이 어댑터는 gif 이미지 경로를 받아서 생성이되고,
 * 화면에 보이는 아이템의 포지션이 바뀔때마다 캐치하고 gif 이미지를 보여주는 기능을 한다.*/

class AdapterBrightShadeControl(
    val context: Context
) :
    RecyclerView.Adapter<AdapterBrightShadeControl.brightShadeViewHolder>() {
    var holderHashSet: LinkedHashSet<AdapterBrightShadeControl.brightShadeViewHolder> = LinkedHashSet<AdapterBrightShadeControl.brightShadeViewHolder>()

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

    override fun onBindViewHolder(holder: brightShadeViewHolder, position: Int) {

        val itr = holderHashSet.iterator()
        var itrIdx = 0
        while (itr.hasNext()){
            if (itrIdx != luminanceIndex - 1) itr.next().brightShadeItemViewLayout.background = null
            else itr.next().brightShadeItemViewLayout.background = THIS_CONTEXT!!.getDrawable(R.drawable.bright_shade_border)
            itrIdx += 1
        }

        holder.contrastLeftImage.setBackgroundColor(
            THIS_CONTEXT!!.resources.getColor(
                userContrastData[position].contrastLeftImage!!
            )
        )
        holder.contrastRightImage.setBackgroundColor(
            THIS_CONTEXT!!.resources.getColor(
                userContrastData[position].contrastRightImage!!
            )
        )

        holder.brightShadeItemViewLayout.setOnClickListener {

            val itr = holderHashSet.iterator()
            var itrIdx = 0
            while (itr.hasNext()){
                if (itrIdx != position) itr.next().brightShadeItemViewLayout.background = null
                else {
                    itr.next().brightShadeItemViewLayout.background = THIS_CONTEXT!!.getDrawable(R.drawable.bright_shade_border)
                    luminanceIndex = holder.adapterPosition + 1
                    luminanceFlag = true
                    fragmentType = "luminance"
                    luminanceToggleBtn.isChecked = true
                }
                itrIdx += 1
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