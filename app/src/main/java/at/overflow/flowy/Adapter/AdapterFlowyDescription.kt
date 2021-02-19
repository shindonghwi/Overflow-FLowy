package at.overflow.flowy.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import at.overflow.flowy.R

/** AdapterFlowyDescription :
 *
 * 사용되는 위치 : FragmentDescription.kt
 *
 * 이 어댑터는 Flowy APP을 설치하고 실행을 하게 되면 Flowy 기능에 대한 설명 GIF 가 나오게된다.
 * 화면에는 GIF 이미지가 보여지는데, 기능이 몇개가 추가될지 몰라서 recyclerview 로 만들었다.
 * 각 아이템(GIF) 들은 Glide Library 를 사용하여 화면에 보여지게된다.
 * */

class AdapterFlowyDescription(val context: Context, private val gifImagesPath: Array<Int>) :
    RecyclerView.Adapter<AdapterFlowyDescription.viewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolder =
        viewHolder(
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.flowy_description_gif_view,
                parent,
                false
            )
        )

    override fun getItemCount(): Int = gifImagesPath.size

    override fun onBindViewHolder(
        holder: viewHolder,
        position: Int
    ) {
        Glide.with(context).load(gifImagesPath[position]).into(holder.gifImage)
    }

    class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gifImage: PhotoView = view.findViewById(R.id.photo_view)
    }
}