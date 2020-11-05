package com.overflow.flowy.Fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overflow.flowy.Adapter.AdapterFlowyMenuContrast
import com.overflow.flowy.Adapter.ItemTouchHelperCallback
import com.overflow.flowy.DTO.ContrastData
import com.overflow.flowy.Fragment.FragmentCamera.Companion.userContrastData
import com.overflow.flowy.Interface.onBackPressedListener
import com.overflow.flowy.MainActivity
import com.overflow.flowy.R
import com.overflow.flowy.Util.SharedPreferenceUtil
import com.overflow.flowy.Util.THIS_CONTEXT
import com.overflow.flowy.Util.contrastInitData

class FragmentMenuContrast : Fragment(){

    private lateinit var menuContrastRecyclerView: RecyclerView
    private lateinit var initBackupBtn: Button

    private lateinit var flowyMenuContrastAdapter: AdapterFlowyMenuContrast

    fun newInstance(): FragmentMenuContrast {
        return FragmentMenuContrast()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu_contrast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        THIS_CONTEXT = context

        initId(view)

        flowyMenuContrastAdapter = AdapterFlowyMenuContrast(THIS_CONTEXT!!)

        menuContrastRecyclerView.layoutManager = LinearLayoutManager(THIS_CONTEXT)
        menuContrastRecyclerView.adapter = flowyMenuContrastAdapter

        itemClick()
        itemDragAndDrop()
        clickListener()
    }

    private fun itemDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(flowyMenuContrastAdapter))
        itemTouchHelper.attachToRecyclerView(menuContrastRecyclerView)
    }

    private fun initId(view: View) {
        menuContrastRecyclerView = view.findViewById(R.id.menuContrastRecyclerView)
        initBackupBtn = view.findViewById(R.id.initBackupBtn)
    }

    private fun clickListener(){
        initBackupBtn.setOnClickListener{
            userContrastData.clear()
            userContrastData.addAll(contrastInitData)
            flowyMenuContrastAdapter.notifyDataSetChanged()
        }
    }

    private fun itemClick() {
        flowyMenuContrastAdapter.setOnClick(object : AdapterFlowyMenuContrast.OnItemClicked {
            override fun onItemClick(position: Int) {
                userContrastData.removeAt(position)
                flowyMenuContrastAdapter.notifyDataSetChanged()
            }
        })
    }

    override fun onDestroyView() {
        val contrastPref = THIS_CONTEXT!!.getSharedPreferences("userLuminance", Context.MODE_PRIVATE)
        SharedPreferenceUtil().saveArrayListData(contrastPref, "userLumincanceData", userContrastData)
        super.onDestroyView()
    }
}