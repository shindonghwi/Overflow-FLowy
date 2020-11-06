package com.overflow.flowy.Fragment

import android.content.Context
import android.content.Intent
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

class FragmentMenuInfo : Fragment(), View.OnClickListener{

    private lateinit var inquireBtn: Button
    private lateinit var shareBtn: Button
    private lateinit var evaluateBtn: Button
    private lateinit var openSourceBtn: Button

    fun newInstance(): FragmentMenuInfo {
        return FragmentMenuInfo()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        THIS_CONTEXT = context

        initId(view)

        clickListener()
    }


    private fun initId(view: View) {
        inquireBtn = view.findViewById(R.id.inquireBtn)
        shareBtn = view.findViewById(R.id.shareBtn)
        evaluateBtn = view.findViewById(R.id.evaluateBtn)
        openSourceBtn = view.findViewById(R.id.openSourceBtn)
    }

    private fun clickListener(){
        inquireBtn.setOnClickListener(this)
        shareBtn.setOnClickListener(this)
        evaluateBtn.setOnClickListener(this)
        openSourceBtn.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.inquireBtn -> {
                val email = Intent(Intent.ACTION_SEND)
                email.type = "plain/text"
                val address = arrayOf<String>(resources.getString(R.string.overflow_email))
                email.putExtra(Intent.EXTRA_EMAIL, address)
                email.putExtra(Intent.EXTRA_SUBJECT, "")
                email.putExtra(Intent.EXTRA_TEXT, "-- Overflow에 문의하기 -- \n")
                startActivity(email)
            }
            R.id.shareBtn -> {
                val msg = Intent(Intent.ACTION_SEND)
                msg.addCategory(Intent.CATEGORY_DEFAULT)
                msg.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=패키지명")
                msg.putExtra(Intent.EXTRA_TITLE, "제목")
                msg.setType("text/plain")
                startActivity(Intent.createChooser(msg, "앱을 선택해 주세요"))
            }
            R.id.evaluateBtn -> {}
            R.id.openSourceBtn -> { (activity as MainActivity).replaceFragment("add", FragmentMenuLicense().newInstance()) }
        }
    }
}