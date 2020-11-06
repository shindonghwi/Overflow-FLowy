package com.overflow.flowy.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.overflow.flowy.Adapter.AdapterFlowyMenuContrast
import com.overflow.flowy.Adapter.ItemTouchHelperCallback
import com.overflow.flowy.Fragment.FragmentCamera.Companion.userContrastData
import com.overflow.flowy.R
import com.overflow.flowy.Util.SharedPreferenceUtil
import com.overflow.flowy.Util.THIS_CONTEXT
import com.overflow.flowy.Util.contrastInitData

class FragmentMenuLicense : Fragment(){

    fun newInstance(): FragmentMenuLicense {
        return FragmentMenuLicense()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu_license, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}