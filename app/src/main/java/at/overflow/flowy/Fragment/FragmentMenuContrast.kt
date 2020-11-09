package at.overflow.flowy.Fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.overflow.flowy.Adapter.AdapterFlowyMenuContrast
import at.overflow.flowy.Adapter.ItemTouchHelperCallback
import at.overflow.flowy.Fragment.FragmentCamera.Companion.userContrastData
import at.overflow.flowy.R
import at.overflow.flowy.Util.SharedPreferenceUtil
import at.overflow.flowy.Util.THIS_CONTEXT
import at.overflow.flowy.Util.contrastInitData

/** 메뉴 - 대비 : 기본 대비 색상을 편집 할 수 있는 화면이다. */

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

        // 고대비 어댑터 생성
        flowyMenuContrastAdapter =
            AdapterFlowyMenuContrast(THIS_CONTEXT!!)

        // 리니어 레이아웃 매니저 설정 및 어댑터 연결
        menuContrastRecyclerView.layoutManager = LinearLayoutManager(THIS_CONTEXT)
        menuContrastRecyclerView.adapter = flowyMenuContrastAdapter

        removeItemClick() // X 버튼을 눌러서 아이템 제거
        itemDragAndDrop() // 드래그앤 드랍 버튼을 눌러서 순서 재배치
        contrastInitListener() // 기본값으로 복원 버튼 눌러서 고대비 아이템 초기화
    }

    /** 드래그앤 드랍 버튼을 꾹 눌러서 아이템간의 순서를 재배치 할 수 있는 함수 */
    private fun itemDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(
            ItemTouchHelperCallback(
                flowyMenuContrastAdapter
            )
        )
        itemTouchHelper.attachToRecyclerView(menuContrastRecyclerView)
    }

    /** 뷰 아이디 찾기 */
    private fun initId(view: View) {
        menuContrastRecyclerView = view.findViewById(R.id.menuContrastRecyclerView)
        initBackupBtn = view.findViewById(R.id.initBackupBtn)
    }

    /** 기본값으로 복원 버튼 눌러서 고대비 아이템 초기화 - Util-Constants.kt ->  contrastInitData 변수 참고 */
    private fun contrastInitListener(){
        initBackupBtn.setOnClickListener{
            userContrastData.clear()
            userContrastData.addAll(contrastInitData)
            flowyMenuContrastAdapter.notifyDataSetChanged()
        }
    }

     /** X 표시를 눌러서 기본 대비 색상 제거하는 함수 */
    private fun removeItemClick() {
        flowyMenuContrastAdapter.setOnClick(object : AdapterFlowyMenuContrast.OnItemClicked {
            override fun onRemoveItemClick(position: Int) {
                userContrastData.removeAt(position)
                flowyMenuContrastAdapter.notifyDataSetChanged()
            }
        })
    }

    /** 화면을 나가기전에 편집 내용을 ShardPreference에 저장한다. */
    override fun onDestroyView() {
        val contrastPref = THIS_CONTEXT!!.getSharedPreferences("userLuminance", Context.MODE_PRIVATE)
        SharedPreferenceUtil().saveArrayListData(contrastPref, "userLumincanceData", userContrastData)
        super.onDestroyView()
    }
}