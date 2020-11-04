package com.overflow.flowy.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.overflow.flowy.Adapter.AdapterFlowyDescription
import com.overflow.flowy.MainActivity
import com.overflow.flowy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.relex.circleindicator.CircleIndicator3

class FragmentDescription : Fragment() {

    private lateinit var view_pager : ViewPager2
    private lateinit var indicator : CircleIndicator3
    private lateinit var page_move_status_btn : Button

    fun newInstance():FragmentDescription {
        return FragmentDescription()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        idInit(view = view)
    }

    private fun idInit(view: View) {
        view_pager = view.findViewById(R.id.view_pager)
        indicator = view.findViewById(R.id.indicator)
        page_move_status_btn = view.findViewById(R.id.page_move_status_btn)
    }

    override fun onResume() {
        super.onResume()

        /** flowy앱을 설명하는 gif 이미지의 경로를 불러와서(총 4개) FLowy 설명 gif를 보여 줄 수 있는 Adapter와 연결시켜준다.
         * 그리고 마지막으로 view_pager에 연결 시켜준다. */
        view_pager.adapter = AdapterFlowyDescription(context!!, gifPathRead())

        /** 화면이 움직일때마다 몇 번째 화면을 보고있는지 알려주기위해서, indicator사용 */
        indicator.setViewPager(view_pager) // view_pager의 페이지가 이동될때마다 indicator를 변경하기위해서, view_page - indicator 연결
        indicator.createIndicators(
            gifPathRead().size,
            0
        ) // Flowy를 설명하는 git가 4장이여서, indicator 또한 4개로 설정함. ( indicator의 개수 설정 )

        /** 화면의 요소에 대한 클릭 리스너
         * 1. 화면하단 : Next, Close 글자를 눌렀을때 화면이동 */
        clickListener()

        /** view_pager의 페이지가 이동되었다는 것을 캐치하기 위함.
         * 페이지 이동시에는 화면하단에 Next, Close 텍스트를 변경한다. */
        viewPagerListener()

        view_pager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    }

    private fun clickListener() {
        // 화면하단에 있는 Next, Close 글자를 눌렀을때 페이지 이동
        page_move_status_btn.setOnClickListener {

            // 마지막 페이지가 아닌경우에 Next 글자가 보이는데, 이때는 Flowy 다음 설명으로 이동 시켜준다.
            if (page_move_status_btn.text == getString(R.string.next)) {
                view_pager.currentItem = view_pager.currentItem + 1
            }

            // 마지막 페이지인 경우에는 Close 글자가 보이는데, 이떄는 페이지를 종료하면서, 카메라가 활성화 되는 Activity로 이동시킨다.
            else if (page_move_status_btn.text == getString(R.string.close)) {
                view_pager.visibility = View.GONE
                indicator.visibility = View.GONE
                page_move_status_btn.visibility = View.GONE
                (activity as MainActivity).replaceFragment(FragmentCamera().newInstance())
            }
        }
    }

    private fun viewPagerListener() {
        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            // 페이지가 이동완료된 경우
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // 화면 하단에 있는 텍스트를 변경한다.
                // 0..2 -> 화면이 마지막 페이지가 아닌경우 : Next 글자를 보여준다.
                // else -> 화면이 마지막 페이지인 경우 : Close 글자를 보여준다.

                CoroutineScope(Dispatchers.Main).launch {
                    if (position < gifPathRead().size - 1) {
                        page_move_status_btn.text = getString(R.string.next)
                        page_move_status_btn.setBackgroundResource(R.drawable.transparent)
                    } else {
                        page_move_status_btn.text = getString(R.string.close)
                        page_move_status_btn.setBackgroundResource(R.drawable.blue_radius_10)
                    }
                }
            }
        })
    }

    // 페이지가 이동될때마다 AdapterFlowyDescription에서 캐치하고, 화면에 보여준다.
    private fun gifPathRead(): Array<Int> {
        return arrayOf(
            R.raw.flowy_description_1,
            R.raw.flowy_description_2
        )
    }
}