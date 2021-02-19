package at.overflow.flowy

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import at.overflow.flowy.Fragment.FragmentCamera
import at.overflow.flowy.Fragment.FragmentDescription
import at.overflow.flowy.Renderer.FlowyRenderer.Companion.cameraLifecycle
import at.overflow.flowy.Util.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    /** 요청할 권한들을 작성해준다. */
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private lateinit var appUpdateManager: AppUpdateManager

    /** 화면 소프트키 없애기 */
    private var decorView: View? = null
    private var uiOption = 0

    private var backKeyClickTime: Long = 0L

    private val fragmentCameraInstance: FragmentCamera = FragmentCamera()
        .newInstance()

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inAppUpdate()

        // user UUID 가져오기 or UUID 생성
        getUserUUID()

        Log.d("mainLifeCycle", "onCreate")

        pref = this.getSharedPreferences("flowyDescription", Context.MODE_PRIVATE)
        prefEditor = pref.edit()

        val descriptionCheck = pref.getBoolean("flowyDescriptionCheck", false)

        // 만약 플로위 설명을 본적이 없다면, 플로위 설명 화면을 띄워주고, 본적이 있다고 알린다.
        if (!descriptionCheck) {
            replaceFragment(
                "replace", FragmentDescription()
                    .newInstance()
            )
            prefEditor.putBoolean("flowyDescriptionCheck", true)
            prefEditor.commit()
        }
        // 만약 플로위 설명을 본적이 있다면, 권한을 확인하고 카메라 화면으로 넘어간다.
        else {
            requestPermission()
            flowyModeInit() // 앱 시작시 플로위 모드 초기화 상태로 시작
        }
    }

    private fun inAppUpdate() {

        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { // appUpdateManager이 추가되는데 성공하면 발생하는 이벤트
                appUpdateInfo ->
            if ((appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE // UpdateAvailability.UPDATE_AVAILABLE == 2 이면 앱 true
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
            ) { // 허용된 타입의 앱 업데이트이면 실행 (AppUpdateType.IMMEDIATE || AppUpdateType.FLEXIBLE)
                // 업데이트가 가능하고, 상위 버전 코드의 앱이 존재하면 업데이트를 실행한다.
                requestUpdate(appUpdateInfo)
            }
        }
    }

    // 업데이트 요청
    private fun requestUpdate(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                // 'getAppUpdateInfo()' 에 의해 리턴된 인텐트
                appUpdateInfo,
                // 'AppUpdateType.FLEXIBLE': 사용자에게 업데이트 여부를 물은 후 업데이트 실행 가능
                // 'AppUpdateType.IMMEDIATE': 사용자가 수락해야만 하는 업데이트 창을 보여줌
                AppUpdateType.IMMEDIATE,
                // 현재 업데이트 요청을 만든 액티비티, 여기선 MainActivity.
                this,
                // onActivityResult 에서 사용될 REQUEST_CODE.
                APP_UPDATE_PERMISSION_CODE
            )
        } catch (e: Exception) {
            e.printStackTrace()
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

        appUpdateCheck()

        /** 화면 하단에 소프트 키 없애는 코드 */
        disableSoftKey()

        /** 카메라 수명주기가 끝났다면, 다시 카메라를 실행하는 코드 */
        try {
            if (cameraLifecycle.currentState() == Lifecycle.State.DESTROYED) {
                clearStack()
                replaceFragment(
                    "replace", FragmentCamera()
                        .newInstance()
                )
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

    private var permissionCheckFlag: Boolean = false

    /** 권한 요청 결과 */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        Log.d("mainLifeCycle", "onRequestPermissionsResult")

        var toastPermission: ArrayList<String> = ArrayList<String>()

        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {

                        // 카메라는 필수 기능이기에 허용을 안하면 앱을 종료한다.
                        if (permission == "android.permission.CAMERA" && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            permissionCheckFlag = false
                            toastPermission.add("카메라")
                        } else {
                            permissionCheckFlag = true
                        }

                        if (permission == "android.permission.WRITE_EXTERNAL_STORAGE" && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            permissionCheckFlag = false
                            toastPermission.add("저장공간")
                        } else {
                            permissionCheckFlag = true
                        }

                        if (permission == "android.permission.ACCESS_FINE_LOCATION" && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            permissionCheckFlag = false
                            toastPermission.add("위치정보")
                       } else {
                            permissionCheckFlag = true
                        }
                    }
                }
            }
        }
        if (permissionCheckFlag) {
            Log.d("permissionLog", "이동 : 거절된 퍼미션 없음 카메라 화면으로 이동")
            replaceFragment(
                "replace", FragmentCamera()
                    .newInstance()
            )
        } else {
            val toastStr = toastPermission.toString()
            Toast.makeText(
                this,
                "$toastStr\n권한 요청에 동의 해주셔야 이용가능합니다. \n설정에서 권한을 허용해주세요",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == APP_UPDATE_PERMISSION_CODE) {
            // 업데이트가 성공적으로 끝나지 않은 경우
            if (resultCode != RESULT_OK) {
                // 업데이트가 취소되거나 실패하면 업데이트를 다시 요청할 수 있다.,
                // 업데이트 타입을 선택한다 (IMMEDIATE || FLEXIBLE).
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                    if ((appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                                // flexible한 업데이트를 위해서는 AppUpdateType.FLEXIBLE을 사용한다.
                                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE))
                    ) {
                        // 업데이트를 다시 요청한다.
                        requestUpdate(appUpdateInfo)
                    }
                }
            }
        }
    }

    private fun appUpdateCheck() {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if ((appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)) {
                    // If an in-app update is already running, resume the update.
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            APP_UPDATE_PERMISSION_CODE
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
            R.anim.slide_in_left,
            R.anim.slide_out_left,
            R.anim.slide_out_right,
            R.anim.slide_in_right
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

    override fun onDestroy() {
        freezeMode = false
        super.onDestroy()
    }

    private fun flowyModeInit() {
        val flowyInitPref = getSharedPreferences("flowyToggleBtnStatus", Context.MODE_PRIVATE)
        val flowyPrefEditor = flowyInitPref.edit()
        cameraMode = "default"
        cameraSubMode = "default"
        SharedPreferenceUtil().saveBooleanData(flowyInitPref, "flowyZoomToggleBtn", false)
        flowyPrefEditor.apply()
    }

    companion object {
        lateinit var pref: SharedPreferences
        lateinit var prefEditor: SharedPreferences.Editor
    }

}
