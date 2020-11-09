package at.overflow.flowy.Fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.overflow.flowy.Adapter.AdapterFlowyMenu
import at.overflow.flowy.DTO.FlowyMenuData
import at.overflow.flowy.MainActivity
import at.overflow.flowy.R
import at.overflow.flowy.Util.THIS_CONTEXT

class FragmentMenu : Fragment(){

    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var completeBtn: Button
    private lateinit var flowyLogoImgVIew: ImageView

    fun newInstance(): FragmentMenu {
        return FragmentMenu()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        THIS_CONTEXT = context

        initId(view)

        val flowyMenuAdapter = AdapterFlowyMenu(
            THIS_CONTEXT!!,
            loadFlowyMenuData()
        )

        menuRecyclerView.layoutManager = LinearLayoutManager(THIS_CONTEXT)
        menuRecyclerView.adapter = flowyMenuAdapter

        flowyMenuAdapter.setOnClick(object : AdapterFlowyMenu.OnItemClicked{
            override fun onItemClick(position: Int) {

                when(position){
                    // 대비 메뉴
                    0 -> (activity as MainActivity).replaceFragment("add", FragmentMenuContrast()
                        .newInstance())

                    // 자습서
                    1 -> (activity as MainActivity).replaceFragment("add", FragmentDescription()
                        .newInstance())

                    // 정보
                    2 -> (activity as MainActivity).replaceFragment("add", FragmentMenuInfo()
                        .newInstance())
                }
            }
        })

        flowyLogoImgVIew.setOnClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://flowy.kr/")))
        }

        completeBtn.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
        }
    }

    private fun initId(view: View) {
        menuRecyclerView = view.findViewById(R.id.menuRecyclerView)
        completeBtn = view.findViewById(R.id.completeBtn)
        flowyLogoImgVIew = view.findViewById(R.id.flowyLogoImgVIew)
    }

    private fun loadFlowyMenuData(): Array<FlowyMenuData> {

        return arrayOf(
//            FlowyMenuData(R.drawable.menu_theme, resources.getString(R.string.menu_theme_text)),
            FlowyMenuData(
                R.drawable.menu_contrast,
                resources.getString(R.string.menu_contrast_text)
            ),
            FlowyMenuData(
                R.drawable.menu_tutorial,
                resources.getString(R.string.menu_tutorial_text)
            ),
            FlowyMenuData(
                R.drawable.menu_info,
                resources.getString(R.string.menu_info_text)
            )
        )
    }

    override fun onResume() {
        (activity as MainActivity).enableSoftKey()
        super.onResume()
    }

    override fun onAttach(context: Context) {
        (activity as MainActivity).enableSoftKey()
        super.onAttach(context)
    }

    override fun onDestroyView() {
        (activity as MainActivity).disableSoftKey()
        super.onDestroyView()
    }
}