package com.overflow.flowy.Fragment

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
import com.overflow.flowy.Interface.onBackPressedListener
import com.overflow.flowy.MainActivity
import com.overflow.flowy.R
import com.overflow.flowy.Util.LuminanceDefaultData
import com.overflow.flowy.Util.LuminanceInitData
import com.overflow.flowy.Util.THIS_CONTEXT
import com.overflow.flowy.Util.contrastInitData

class FragmentMenuContrast : Fragment(), onBackPressedListener {

    private lateinit var menuContrastRecyclerView: RecyclerView
    private var contrastArrayData: ArrayList<ContrastData> = ArrayList<ContrastData>()
    private lateinit var initBackupBtn: Button

    private var leftColor: Int = 0
    private var rightColor: Int = 0
    private lateinit var flowyMenuContrastAdapter: AdapterFlowyMenuContrast
    private lateinit var helper: ItemTouchHelper

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

        for (data in LuminanceDefaultData) {

            var leftColorText = data.colorText.split("/")[0]
            val rightColorText = data.colorText.split("/")[1]

            when (leftColorText) {
                "흑" -> leftColor = (R.color.black)
                "백" -> leftColor = (R.color.white)
                "황" -> leftColor = (R.color.yellow)
                "청" -> leftColor = (R.color.blue)
            }
            when (rightColorText) {
                "흑" -> rightColor = (R.color.black)
                "백" -> rightColor = (R.color.white)
                "황" -> rightColor = (R.color.yellow)
                "청" -> rightColor = (R.color.blue)
            }

            contrastArrayData.add(
                ContrastData(
                    null,
                    leftColor,
                    rightColor,
                    data.colorText,
                    null
                )
            )
        }

        flowyMenuContrastAdapter = AdapterFlowyMenuContrast(THIS_CONTEXT!!, contrastArrayData)

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
            Log.d("asdasd","clcik")

            Log.d("asdasd", LuminanceDefaultData.size.toString())

            contrastArrayData.clear()
            LuminanceDefaultData.clear()
            contrastArrayData.addAll(contrastInitData)
            LuminanceDefaultData.addAll(LuminanceInitData)
            flowyMenuContrastAdapter.notifyDataSetChanged()

            Log.d("asdasd", LuminanceDefaultData.size.toString())

        }
    }

    private fun itemClick() {
        flowyMenuContrastAdapter.setOnClick(object : AdapterFlowyMenuContrast.OnItemClicked {
            override fun onItemClick(position: Int) {

                Log.d("asdasd", "!" + contrastArrayData.size.toString())
                Log.d("asdasd", "@" + LuminanceDefaultData.size.toString())

                contrastArrayData.removeAt(position)
                LuminanceDefaultData.removeAt(position)
                flowyMenuContrastAdapter.notifyDataSetChanged()
            }

        })
    }

    override fun onBackPressed() {
        (activity as MainActivity).replaceFragment(FragmentMenu().newInstance())
    }


}