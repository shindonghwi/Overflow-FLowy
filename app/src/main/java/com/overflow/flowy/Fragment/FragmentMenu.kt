package com.overflow.flowy.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overflow.flowy.Adapter.AdapterFlowyMenu
import com.overflow.flowy.DTO.FlowyMenuData
import com.overflow.flowy.Interface.onBackPressedListener
import com.overflow.flowy.MainActivity
import com.overflow.flowy.R
import com.overflow.flowy.Util.THIS_CONTEXT

class FragmentMenu : Fragment(), View.OnClickListener, onBackPressedListener {

    private lateinit var menuCompleteBtn: Button
    private lateinit var menuRecyclerView: RecyclerView

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
        clickListener()

        val flowyMenuAdapter = AdapterFlowyMenu(THIS_CONTEXT!!, loadFlowyMenuData())

        menuRecyclerView.layoutManager = LinearLayoutManager(THIS_CONTEXT)
        menuRecyclerView.adapter = flowyMenuAdapter

        flowyMenuAdapter.setOnClick(object : AdapterFlowyMenu.OnItemClicked{
            override fun onItemClick(position: Int) {

                // 대비 메뉴 클릭
                if (position == 0){
                    (activity as MainActivity).replaceFragment(FragmentMenuContrast().newInstance())
                }
            }

        })
    }

    private fun initId(view: View) {
        menuCompleteBtn = view.findViewById(R.id.menuCompleteBtn)
        menuRecyclerView = view.findViewById(R.id.menuRecyclerView)
    }

    private fun clickListener() {
        menuCompleteBtn.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {

            R.id.menuCompleteBtn -> {
                (activity as MainActivity).replaceFragment(FragmentCamera().newInstance())
            }

        }
    }

    private fun loadFlowyMenuData(): Array<FlowyMenuData> {

        return arrayOf(
//            FlowyMenuData(R.drawable.menu_theme, resources.getString(R.string.menu_theme_text)),
            FlowyMenuData(R.drawable.menu_contrast,resources.getString(R.string.menu_contrast_text)),
            FlowyMenuData(R.drawable.menu_tutorial,resources.getString(R.string.menu_tutorial_text)),
            FlowyMenuData(R.drawable.menu_info,resources.getString(R.string.menu_info_text))
        )
    }

    override fun onBackPressed() {
        (activity as MainActivity).replaceFragment(FragmentCamera().newInstance())
    }
}