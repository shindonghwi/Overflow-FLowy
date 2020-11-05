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
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.overflow.flowy.Fragment.FragmentCamera
import com.overflow.flowy.Fragment.FragmentDescription
import com.overflow.flowy.Fragment.FragmentMenu
import com.overflow.flowy.Interface.onBackPressedListener
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.cameraLifecycle
import com.overflow.flowy.Util.MY_LOG
import com.overflow.flowy.Util.REQUEST_PERMISSION_CODE
import java.io.File
import kotlin.system.exitProcess


class MainActivity() : AppCompatActivity() {

    /** 요청할 권한들을 작성해준다. */
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /** 화면 소프트키 없애기 */
    private var decorView: View? = null
    private var uiOption = 0

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("mainLifeCycle", "onCreate")

        pref = this.getSharedPreferences("flowyDescription", Context.MODE_PRIVATE)
        prefEditor = pref.edit()

        val descriptionCheck = pref.getBoolean("flowyDescriptionCheck", false)

        // 만약 플로위 설명을 본적이 없다면, 플로위 설명 화면을 띄워주고, 본적이 있다고 알린다.
        if (!descriptionCheck) {
            replaceFragment("add", FragmentDescription())
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
            Log.d("permissionLog", "거절된 퍼미션 없음 카메라 화면으로 이동")
            replaceFragment("add", FragmentCamera())
        }
    }

    override fun onResume() {
        Log.d("mainLifeCycle", "onResume")
        super.onResume()

        /** 화면 하단에 소프트 키 없애는 코드 */
        disableSoftKey()

        try {
            Log.d("countFragment", supportFragmentManager.backStackEntryCount.toString())
            Log.d("cameraLifecycle", cameraLifecycle.currentState().toString())

            // 만일 카메라 수명주기가 해제된 상태라면 프래그먼트 카메라를 다시 살린다.
            if (cameraLifecycle.currentState().toString() == "DESTROYED") {
                Log.d("permissionLog", "수명주기 해제 후 다시 실행 카메라화면으로이동")
                replaceFragment("add", FragmentCamera())
            }
        } catch (e: Exception) {

        }
    }

    override fun onPause() {
        Log.d("mainLifeCycle", "onPause")
        super.onPause()

        // 현재 보여지는 프래그먼트가 플로위 카메라 프래그먼트라면 수명주기를 해제한다. 그리고 onResume시에 다시 수명주기를 붙인다.
        val topFragment = getVisibleFragment()
        Log.d("topfragment", topFragment.toString())
        if (topFragment != null) {
            if (topFragment.tag!!.contains("FragmentCamera")) {
                cameraLifecycle.doOnDestroy() // 카메라 수명주기 off
                Log.d("currentLifeCycle", cameraLifecycle.currentState().toString())
            }
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

    /** 권한 요청 결과 */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        Log.d("mainLifeCycle", "onRequestPermissionsResult")

        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {

                        Log.d(MY_LOG, permission.toString())

                        // 카메라는 필수 기능이기에 허용을 안하면 앱을 종료한다.
                        if (permission == "android.permission.CAMERA" && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                                // 사용자가 거절을 했다면, 앱을 종료시킨다.
                                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                                    finish()
                                }
                                // 다시보지 않기로 거절 했다면, 앱 설정에 들어가서 권한을 허용해주어야 한다. 앱 설정창으로 이동시켜주는 다이얼로그
                                else {
                                    errorDialog()?.show()
                                }
                            }
                        } else if (permission == "android.permission.WRITE_EXTERNAL_STORAGE" && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // 사용자가 거절을 했다면, 앱을 종료시킨다.
                                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                    finish()
                                }
                                // 다시보지 않기로 거절 했다면, 앱 설정에 들어가서 권한을 허용해주어야 한다. 앱 설정창으로 이동시켜주는 다이얼로그
                                else {
                                    errorDialog()?.show()
                                }
                            }
                        }

                        // 권한을 허용한 경우에는 다음 화면으로 넘어간다.
                        else {
                            Log.d("permissionLog", "권한허용되있음 카메라화면으로이동")
                            replaceFragment("add", FragmentCamera())
                        }
                    }
                }
            }
        }
    }

    // 사용자가 권한을 [다시 권한보지 않기] 로 거절했을때 보여줄 함수
    fun errorDialog(): AlertDialog? {
        Log.d("mainLifeCycle", "errorDialog")
        val errorAlertDialog = AlertDialog.Builder(this)
            .setTitle("필수권한요청")
            .setMessage("[설정] - [권한] - 카메라 권한을 허용해주세요")
            .setPositiveButton(R.string.settings) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                        "package:$packageName"
                    )
                )
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

        return errorAlertDialog.create()
    }

    /** 화면 하단에 소프트 키 없애는 코드 */
    private fun disableSoftKey() {
        Log.d("mainLifeCycle", "disableSoftKey")
        decorView = window.decorView
        uiOption = window.decorView.systemUiVisibility
        uiOption = uiOption or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        uiOption = uiOption or View.SYSTEM_UI_FLAG_FULLSCREEN
        uiOption = uiOption or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        decorView!!.systemUiVisibility = uiOption
    }

    fun replaceFragment(type: String, fragment: Fragment) {
        Log.d("replaceFragment", "$fragment")
        Log.d("mainLifeCycle", "replaceFragment")

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(null)
        if (type == "replace") {
            fragmentTransaction.replace(R.id.container, fragment, fragment.toString()).commit()
        } else if (type == "add") {
            fragmentTransaction.add(R.id.container, fragment, fragment.toString()).commit()
        }
    }

    override fun onDestroy() {
        Log.d("mainLifeCycle", "onDestroy")
        removeToggleBtnStatus()
        super.onDestroy()
    }

    /** 화면을 닫을시 토글버튼 상태 초기화 */
    private fun removeToggleBtnStatus() {
        try {
            val f = File("/data/data/com.overflow.flowy/shared_prefs", "flowyToggleBtnStatus.xml")
            f.delete()
        } catch (e: Exception) {

        }
    }

    private var backKeyClickTime : Long = 0L


    override fun onBackPressed() {
        val count: Int = supportFragmentManager.backStackEntryCount

        Log.d("countCount","$count")

        if (count == 1) {
            if (System.currentTimeMillis() > backKeyClickTime + 2000)
            {
                backKeyClickTime = System.currentTimeMillis()
                Toast.makeText(this, "뒤로 가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                return
            }
            if (System.currentTimeMillis() <= backKeyClickTime + 2000)
            {
                clearStack()
                finish()
            }

        } else {
            supportFragmentManager.popBackStack()
        }
    }

    /** 앱을 종료할떄 살아 있는 프래그먼트를 모두 지워준다. */
    private fun clearStack()
    {
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

    companion object {
        lateinit var pref: SharedPreferences
        lateinit var prefEditor: SharedPreferences.Editor
    }

}
