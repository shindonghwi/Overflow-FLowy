package at.overflow.flowy.Fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import at.overflow.flowy.MainActivity
import at.overflow.flowy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class FragmentMenuLicense : Fragment() {

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

    override fun onAttach(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            (activity as MainActivity).enableSoftKey()
        }
        super.onAttach(context)
    }
}