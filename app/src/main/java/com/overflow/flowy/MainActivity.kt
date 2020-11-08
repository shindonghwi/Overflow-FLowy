package com.overflow.flowy

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.overflow.flowy.Fragment.FragmentCamera
import com.overflow.flowy.Fragment.FragmentDescription
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.cameraLifecycle
import com.overflow.flowy.Util.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    /** 요청할 권한들을 작성해준다. */
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /** 화면 소프트키 없애기 */
    private var decorView: View? = null
    private var uiOption = 0

    private var backKeyClickTime: Long = 0L

    private val fragmentCameraInstance: FragmentCamera = FragmentCamera().newInstance()

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // user UUID 가져오기 or UUID 생성
        getUserUUID()

        Log.d("mainLifeCycle", "onCreate")

        pref = this.getSharedPreferences("flowyDescription", Context.MODE_PRIVATE)
        prefEditor = pref.edit()

        val descriptionCheck = pref.getBoolean("flowyDescriptionCheck", false)

        // 만약 플로위 설명을 본적이 없다면, 플로위 설명 화면을 띄워주고, 본적이 있다고 알린다.
        if (!descriptionCheck) {
            replaceFragment("replace", FragmentDescription().newInstance())
            prefEditor.putBoolean("flowyDescriptionCheck", true)
            prefEditor.commit()
        }
        // 만약 플로위 설명을 본적이 있다면, 권한을 확인하고 카메라 화면으로 넘어간다.
        else {
            requestPermission()
        }

    }

    /** 퍼미션 체크를 하고, 거절된 권한이 없다면 플로위 카메라 화면으로 이동한다. */
    fun requestPermission() {
        Log.d("mainLifeCycle", "requestPermission")
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        var rejectedPermissionList = ArrayList<String>()

        //필요한 퍼미션들을 하나씩 끄집어내서 현재 권한을 받았는지 체크
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //만약 권한이 없다면 rejected_permission_list 추가
                rejectedPermissionList.add(permission)
            }
        }
        //거절된 퍼미션이 있다면...
        if (rejectedPermissionList.isNotEmpty()) {
            Log.d(MY_LOG, "requestPermission: $rejectedPermissionList")
            //권한 요청!
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(
                this, rejectedPermissionList.toArray(array),
                REQUEST_PERMISSION_CODE
            )
        }

        // 거절된 퍼미션이 없다면 카메라 실행
        else {
            if (!getVisibleFragment().toString().contains("FragmentCamera")) {
                Log.d("permissionLog", "거절된 퍼미션 없음 카메라 화면으로 이동")
                replaceFragment("replace", fragmentCameraInstance)
            }
        }
    }

    override fun onResume() {
        Log.d("mainLifeCycle", "onResume")
        super.onResume()

        /** 화면 하단에 소프트 키 없애는 코드 */
        disableSoftKey()

        /** 카메라 수명주기가 끝났다면, 다시 카메라를 실행하는 코드 */
        try {
            if (cameraLifecycle.currentState() == Lifecycle.State.DESTROYED) {
                clearStack()
                replaceFragment("replace", FragmentCamera().newInstance())
                return
            }
        } catch (e: Exception) {

        }

        /** 화면이 닫혔을때, 카메라 수명주기가 다시 살아남. */
        try {
            cameraLifecycle.doOnResume()
            cameraLifecycle.doOnStarted()
        } catch (e: UninitializedPropertyAccessException) {

        }
    }

    override fun onPause() {
        Log.d("mainLifeCycle", "onPause")
        super.onPause()

        /** 화면이 닫혔을때, 카메라 수명주기 죽임. */
        try {
            cameraLifecycle.doOnStarted()
        } catch (e: UninitializedPropertyAccessException) {
        }
    }

    /** 현재 보여지고 있는 프래그먼트를 가져온다. */
    private fun getVisibleFragment(): Fragment? {
        Log.d("mainLifeCycle", "getVisibleFragment")
        for (fragment in supportFragmentManager.fragments)
            if (fragment.isVisible)
                return (fragment as Fragment)
        return null
    }

    private var permissionCheckFlag : Boolean = false

    /** 권한 요청 결과 */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        Log.d("mainLifeCycle", "onRequestPermissionsResult")

        var toastPermission : ArrayList<String> = ArrayList<String>()

        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {

                        // 카메라는 필수 기능이기에 허용을 안하면 앱을 종료한다.
                        if (permission == "android.permission.CAMERA" && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            permissionCheckFlag = false
                            toastPermission.add("카메라")
                        }
                        else { permissionCheckFlag = true }

                        if (permission == "android.permission.WRITE_EXTERNAL_STORAGE" && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            permissionCheckFlag = false
                            toastPermission.add("저장공간")
                        }
                        else{  permissionCheckFlag = true  }
                    }
                }
            }
        }

        if ( permissionCheckFlag ){
            Log.d("permissionLog", "이동 : 거절된 퍼미션 없음 카메라 화면으로 이동")
            replaceFragment("replace", FragmentCamera().newInstance())
        }
        else{
            val toastStr = toastPermission.toString()
            Toast.makeText(this, "$toastStr\n권한 요청에 동의 해주셔야 이용가능합니다. \n설정에서 권한을 허용해주세요",Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /** 화면 하단에 소프트 키 없애는 코드 */
    fun disableSoftKey() {
        Log.d("mainLifeCycle", "disableSoftKey")
        decorView = window.decorView
        uiOption = window.decorView.systemUiVisibility
        uiOption = uiOption or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        uiOption = uiOption or View.SYSTEM_UI_FLAG_FULLSCREEN
        uiOption = uiOption or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView!!.systemUiVisibility = uiOption
    }

    /** 화면 하단에 소프트 키 생성하는 코드 */
    fun enableSoftKey() {
        decorView!!.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    fun replaceFragment(type: String, fragment: Fragment) {
        Log.d("replaceFragment", "$fragment")
        Log.d("mainLifeCycle", "replaceFragment")

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.setCustomAnimations(
            R.anim.slide_in_left, R.anim.slide_out_left,
            R.anim.slide_out_right, R.anim.slide_in_right
        )
        if (type == "replace") {
            fragmentTransaction.replace(R.id.container, fragment).commit()
        } else if (type == "add") {
            fragmentTransaction.add(R.id.container, fragment).commit()
        }
    }

    /** 백버튼 이벤트 : 카메라 프래그먼트일 경우에는 뒤로가기 종료하고, 그렇지 않은 경우에는 프래그먼트 뒤로가기를 한다. */
    override fun onBackPressed() {
        val count: Int = supportFragmentManager.backStackEntryCount

        Log.d("countCount", "$count")

        if (count == 1) {
            if (System.currentTimeMillis() > backKeyClickTime + 2000) {
                backKeyClickTime = System.currentTimeMillis()
                Toast.makeText(this, "뒤로 가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                return
            }
            if (System.currentTimeMillis() <= backKeyClickTime + 2000) {
                clearStack()
                finish()
            }

        } else {
            supportFragmentManager.popBackStack()
        }
    }

    /** 앱을 종료할떄 살아 있는 프래그먼트를 모두 지워준다. */
    private fun clearStack() {
        val backStackEntry = supportFragmentManager.backStackEntryCount
        if (backStackEntry > 0) {
            for (i in 0 until backStackEntry) {
                supportFragmentManager.popBackStackImmediate()
            }
        }
        if (supportFragmentManager.fragments.size > 0) {
            supportFragmentManager.fragments.forEach {
                if (it != null) {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun getUserUUID(): String? {

        val userUUIDPref = this.getSharedPreferences("userUUID", Context.MODE_PRIVATE)

        USER_UUID = SharedPreferenceUtil().loadStringData(userUUIDPref, "uuid")!!

        if (USER_UUID == "") {
            USER_UUID = UUID.randomUUID().toString()
            SharedPreferenceUtil().saveStringData(userUUIDPref, "uuid", USER_UUID)
        }
        return USER_UUID
    }

    companion object {
        lateinit var pref: SharedPreferences
        lateinit var prefEditor: SharedPreferences.Editor
    }

}
