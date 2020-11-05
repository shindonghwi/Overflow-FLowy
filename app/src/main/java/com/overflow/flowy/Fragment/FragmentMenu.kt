package com.overflow.flowy.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overflow.flowy.Adapter.AdapterFlowyMenu
import com.overflow.flowy.DTO.FlowyMenuData
import com.overflow.flowy.Interface.onBackPressedListener
import com.overflow.flowy.MainActivity
import com.overflow.flowy.R
import com.overflow.flowy.Util.THIS_CONTEXT

class FragmentMenu : Fragment(){

    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var completeBtn: Button

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

        val flowyMenuAdapter = AdapterFlowyMenu(THIS_CONTEXT!!, loadFlowyMenuData())

        menuRecyclerView.layoutManager = LinearLayoutManager(THIS_CONTEXT)
        menuRecyclerView.adapter = flowyMenuAdapter

        flowyMenuAdapter.setOnClick(object : AdapterFlowyMenu.OnItemClicked{
            override fun onItemClick(position: Int) {

                when(position){
                    // 대비 메뉴
                    0 -> (activity as MainActivity).replaceFragment("add", FragmentMenuContrast().newInstance())

                    // 자습서
                    1 -> (activity as MainActivity).replaceFragment("replace", FragmentDescription().newInstance())
                }
            }
        })

        completeBtn.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
        }
    }

    private fun initId(view: View) {
        menuRecyclerView = view.findViewById(R.id.menuRecyclerView)
        completeBtn = view.findViewById(R.id.completeBtn)
    }

    private fun loadFlowyMenuData(): Array<FlowyMenuData> {

        return arrayOf(
//            FlowyMenuData(R.drawable.menu_theme, resources.getString(R.string.menu_theme_text)),
            FlowyMenuData(R.drawable.menu_contrast,resources.getString(R.string.menu_contrast_text)),
            FlowyMenuData(R.drawable.menu_tutorial,resources.getString(R.string.menu_tutorial_text)),
            FlowyMenuData(R.drawable.menu_info,resources.getString(R.string.menu_info_text))
        )
    }

}