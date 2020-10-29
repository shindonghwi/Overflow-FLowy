package com.overflow.flowy

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.overflow.flowy.Fragment.FragmentCamera
import com.overflow.flowy.Util.MY_LOG
import com.overflow.flowy.Util.REQUEST_PERMISSION_CODE
import com.overflow.flowy.Util.THIS_CONTEXT
import com.overflow.flowy.Util.deviceRotationValue


class MainActivity() : AppCompatActivity() {

    /** 요청할 권한들을 작성해준다. */
    private val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA
    )

    private var decorView: View? = null
    private var uiOption = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun requestPermission(){
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        var rejectedPermissionList = ArrayList<String>()

        //필요한 퍼미션들을 하나씩 끄집어내서 현재 권한을 받았는지 체크
        for(permission in requiredPermissions){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                //만약 권한이 없다면 rejected_permission_list 추가
                rejectedPermissionList.add(permission)
            }
        }
        //거절된 퍼미션이 있다면...
        if(rejectedPermissionList.isNotEmpty()){
            Log.d(MY_LOG, "requestPermission: $rejectedPermissionList")
            //권한 요청!
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(this, rejectedPermissionList.toArray(array),
                    REQUEST_PERMISSION_CODE
            )
        }

        // 거절된 퍼미션이 없다면 카메라 실행
        else{
            Log.d("permission","camera execute")
            supportFragmentManager.beginTransaction()
                .replace(R.id.container,
                    FragmentCamera()
                    ,"FlowyCameraFragment"
                )
                .commit()
        }

    }

    // Flowy 권한 요청을 onStart에서 한다.
    // 사용자가 중간에 카메라 권한을 해제 할 수도 있으니, 여기서 항상 체크한다.
    // [ 현재는 카메라 권한만 필요함 ] - 2020-10-06
    override fun onStart() {
        super.onStart()
        requestPermission()
    }

    override fun onResume() {
        Log.d("currentFragment", "onResume")
        super.onResume()
        val myFragment = supportFragmentManager.findFragmentById(R.id.container)
        Log.d("currentFragment", "$myFragment")
        if (myFragment != null) {
            Log.d("currentFragment", "onResume: cameraFragment()")
        }

        /** 화면 하단에 소프트 키 없애는 코드 */
        disableSoftKey()


    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if(grantResults.isNotEmpty()) {
                    for((i, permission) in permissions.withIndex()) {

                        Log.d(MY_LOG, permission.toString())

                        // 카메라는 필수 기능이기에 허용을 안하면 앱을 종료한다.
                        if (permission == "android.permission.CAMERA" && grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                                // 사용자가 거절을 했다면, 앱을 종료시킨다.
                                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                                    finish()
                                }
                                // 다시보지 않기로 거절 했다면, 앱 설정에 들어가서 권한을 허용해주어야 한다. 앱 설정창으로 이동시켜주는 다이얼로그
                                else{
                                    errorDialog()?.show()
                                }
                            }
                        }
                        else{
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.container,
                                    FragmentCamera()
                                    ,"FlowyCameraFragment"
                                )
                                .commit()
                        }
                    }
                }
            }
        }
    }

    // 사용자가 권한을 [다시 권한보지 않기] 로 거절했을때 보여줄 함수
    fun errorDialog(): AlertDialog? {
        val errorAlertDialog = AlertDialog.Builder(this)
                .setTitle("필수권한요청")
                .setMessage("[설정] - [권한] - 카메라 권한을 허용해주세요")
                .setPositiveButton(R.string.settings) { _, _ ->
                    val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                            "package:$packageName"
                    ))
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

        return errorAlertDialog.create()
    }

    /** 화면 하단에 소프트 키 없애는 코드 */
    private fun disableSoftKey() {
        decorView = window.decorView
        uiOption = window.decorView.systemUiVisibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) uiOption =
                uiOption or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) uiOption =
                uiOption or View.SYSTEM_UI_FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) uiOption =
                uiOption or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        decorView!!.systemUiVisibility = uiOption
    }
}
