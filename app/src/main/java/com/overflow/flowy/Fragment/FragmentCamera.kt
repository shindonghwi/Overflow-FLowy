package com.overflow.flowy.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.overflow.flowy.DTO.ContrastData
import com.overflow.flowy.Interface.onBackPressedListener
import com.overflow.flowy.MainActivity
import com.overflow.flowy.MainActivity.Companion.pref
import com.overflow.flowy.MainActivity.Companion.prefEditor
import com.overflow.flowy.R
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.adjustHeight
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.camera
import com.overflow.flowy.Util.*
import com.overflow.flowy.View.FlowyGLTextureView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


class FragmentCamera : Fragment(), View.OnClickListener {

    // 프래그먼트의 인스턴스
    public fun newInstance(): FragmentCamera {
        Log.d("newInstance","카메라 인스턴스 생성")
        return FragmentCamera()
    }

    private lateinit var glTextureView: FlowyGLTextureView // 카메라 미리보기가 나올 화면
    private var flowyZoomLongClickEvent: Boolean = false // 롱클릭 이벤트 콜백을 위한 변수, 이벤트 발생시 플로위 줌 시작

    /** 각 요소들의 부모 레이아웃 */
    private lateinit var topMenuLayout: LinearLayout
    private lateinit var bottomMenuLayout: LinearLayout
    private lateinit var pinchZoomLinearLayout: LinearLayout
    private var twoPointClickFlag: Boolean = true

    /** 메뉴바 상단에 있는 버튼 */
    private lateinit var focusToggleBtn: ToggleButton
    private lateinit var flashToggleBtn: ToggleButton
    private lateinit var lensChangeToggleBtn: ToggleButton
    private lateinit var flowyZoomToggleBtn: ToggleButton
    private lateinit var mirroringToggleBtn: ToggleButton

    /** 메뉴바 하단에 있는 버튼 */
    private lateinit var menuToggleBtn: ToggleButton
    private lateinit var flowyCastToggleBtn: ToggleButton
    private lateinit var freezeToggleBtn: ToggleButton
    private lateinit var luminanceToggleBtn: ToggleButton
    private lateinit var controlToggleBtn: ToggleButton

    /** 공유 버튼 */
    private lateinit var shareImgBtn: ImageButton
    private lateinit var shareFrameLayout: FrameLayout

    /** 광고 */
    private lateinit var bannerAdView: AdView
    private lateinit var bannerVersaAD: ImageView


    private lateinit var alertToast: Toast
    private lateinit var pinchZoomSeekbar: SeekBar
    private lateinit var pinchZoomMinusImgBtn: ImageButton
    private lateinit var pinchZoomPlusImgBtn: ImageButton

    private var pinchZoomFinishCallback: Boolean = false
    private var deviceSensorDirection = 0f
    private var pinchZoomFlag: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        THIS_CONTEXT = context

        alertToast = Toast(context)

        idInit(view = view)

        setClickListener() // 클릭 리스너 설정
        screenTouchListener() // 터치 리스너 설정
        pinchZoomListener() // 핀치줌 리스너 설정
        loadLuminanceData() // 사용자의 대비 데이터 가져오기
    }

    /** layout id 초기화하는 공간 */
    private fun idInit(view: View) {

        pref = THIS_CONTEXT!!.getSharedPreferences("flowyToggleBtnStatus", Context.MODE_PRIVATE)
        prefEditor = pref.edit()

        glTextureView = view.findViewById(R.id.glSurfaceView)

        // 각메뉴들의 부모 레이아웃
        topMenuLayout = view.findViewById(R.id.topMenuLayout)
        bottomMenuLayout = view.findViewById(R.id.bottomMenuLayout)
        pinchZoomLinearLayout = view.findViewById(R.id.pinchZoomLinearLayout)

        // 메뉴바 상단의 아이콘
        focusToggleBtn = view.findViewById(R.id.focusToggleBtn)
        flashToggleBtn = view.findViewById(R.id.flashToggleBtn)
        lensChangeToggleBtn = view.findViewById(R.id.lensChangeToggleBtn)
        flowyZoomToggleBtn = view.findViewById(R.id.flowyZoomToggleBtn)
        mirroringToggleBtn = view.findViewById(R.id.mirroringToggleBtn)

        // 메뉴바 하단의 아이콘
        menuToggleBtn = view.findViewById(R.id.menuToggleBtn)
        flowyCastToggleBtn = view.findViewById(R.id.flowyCastToggleBtn)
        freezeToggleBtn = view.findViewById(R.id.freezeToggleBtn)
        luminanceToggleBtn = view.findViewById(R.id.luminanceToggleBtn)
        controlToggleBtn = view.findViewById(R.id.controlToggleBtn)

        // 화면 공유버튼
        shareFrameLayout = view.findViewById(R.id.shareFrameLayout)
        shareImgBtn = view.findViewById(R.id.shareImgBtn)


        pinchZoomSeekbar = view.findViewById(R.id.pinchZoomSeekbar)
        pinchZoomMinusImgBtn = view.findViewById(R.id.pinchZoomMinusImgBtn)
        pinchZoomPlusImgBtn = view.findViewById(R.id.pinchZoomPlusImgBtn)

        // 배너 광고
        bannerAdView = view.findViewById(R.id.bannerAdView)
        bannerVersaAD = view.findViewById(R.id.bannerVersaAD)

        blackScreen = view.findViewById(R.id.blackScreen)
    }

    /** 메뉴바에 있는 토글버튼 상태 가져오기 */
    private fun togBtnStatusCheck() {

        // 플로위줌 버튼 상태 및
        flowyZoomToggleBtn.isChecked = pref.getBoolean("flowyZoomToggleBtn", false)

        luminanceIndex = pref.getInt("luminanceToggleBtn", 0)
        luminanceToggleBtn.isChecked = luminanceIndex != 0

        lensChangeToggleBtn.isChecked = pref.getBoolean("lensChangeToggleBtn", false)
        cameraLensMode = if (lensChangeToggleBtn.isChecked) 0 else 1
        Log.d("sdfsfd","$luminanceIndex")
    }

    /** 메뉴바에 있는 토글버튼 상태 저장하기 */
    private fun togBtnStatusSave() {
        prefEditor.putBoolean("flowyZoomToggleBtn",flowyZoomToggleBtn.isChecked)
        prefEditor.putInt("luminanceToggleBtn",luminanceIndex)
        prefEditor.putBoolean("lensChangeToggleBtn",lensChangeToggleBtn.isChecked)
        prefEditor.commit()
    }

    /** 화면이 생성됨과 동시에 기기방향에 따라 토글 버튼 방향 변경 메서드 추가 */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        uiRelocation()
    }

    /** 사용자의 대비 데이터 가져오기 */
    fun loadLuminanceData(){
        val contrastPref = THIS_CONTEXT!!.getSharedPreferences("userLuminance", Context.MODE_PRIVATE)
        val contrastPrefEditor = contrastPref.edit()

        val userLuminancePref = contrastPref.getBoolean("userLuminanceInitFlag", true)
        // 처음 사용하는 사람이라면, luminance 데이터를 초기화 시켜준다.
        if (userLuminancePref){
            contrastPrefEditor.putBoolean("userLuminanceInitFlag", false)
            contrastPrefEditor.commit()
            SharedPreferenceUtil().saveArrayListData(contrastPref, "userLumincanceData", contrastInitData)
            userContrastData = contrastInitData
            Log.d("lumiinit","처음")
        }
        // 처음 사용하는 사람이 아니라면, 이전에 사용하던 luminance 데이터를 가져온다.
        else{
            val contrastMutableListData = SharedPreferenceUtil().loadArrayListData(contrastPref, "userLumincanceData")
            userContrastData = contrastMutableListData
            Log.d("lumiinit","처음아님")
        }
    }

    /** ui 재배치 ( 태블릿인지, 모바일인지 확인 ) */
    private fun uiRelocation() {

        // 모바일인 경우
        if (!DeviceCheck().isTabletDevice(THIS_CONTEXT!!)) {
            // 현재 버튼의 가로크기를 구해와서, 세로크기를 가로크기와 같게 수정해준다.
            // 그리고 상단, 하단 메뉴레이아웃의 크기를 세로크기의 1.5배로한다.
            topMenuLayout.post {

                // 상단 메뉴바 높이 조정
                val tLayout = RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (focusToggleBtn.height * 1.5).toInt()
                )
                tLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                topMenuLayout.layoutParams = tLayout
                topMenuLayout.requestLayout()

                val marginValue = 20
                val toggleBtnParams = LinearLayout.LayoutParams(0, focusToggleBtn.width, 0.2f)
                toggleBtnParams.topMargin = marginValue
                toggleBtnParams.bottomMargin = marginValue
                toggleBtnParams.leftMargin = marginValue
                toggleBtnParams.rightMargin = marginValue

                focusToggleBtn.layoutParams = toggleBtnParams
                focusToggleBtn.requestLayout()

                flashToggleBtn.layoutParams = toggleBtnParams
                flashToggleBtn.requestLayout()

                lensChangeToggleBtn.layoutParams = toggleBtnParams
                lensChangeToggleBtn.requestLayout()

                flowyZoomToggleBtn.layoutParams = toggleBtnParams
                flowyZoomToggleBtn.requestLayout()

                mirroringToggleBtn.layoutParams = toggleBtnParams
                mirroringToggleBtn.requestLayout()

                // 하단 메뉴바 높이 조정
                val bLayout = RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (focusToggleBtn.height * 1.5).toInt()
                )
                bLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                bottomMenuLayout.layoutParams = bLayout
                bottomMenuLayout.requestLayout()

                menuToggleBtn.layoutParams = toggleBtnParams
                menuToggleBtn.requestLayout()

                flowyCastToggleBtn.layoutParams = toggleBtnParams
                flowyCastToggleBtn.requestLayout()

                freezeToggleBtn.layoutParams = toggleBtnParams
                freezeToggleBtn.requestLayout()

                luminanceToggleBtn.layoutParams = toggleBtnParams
                luminanceToggleBtn.requestLayout()

                controlToggleBtn.layoutParams = toggleBtnParams
                controlToggleBtn.requestLayout()


                // 공유버튼 레이아웃 높이 조정
                val sLayout = RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (focusToggleBtn.height * 1.5).toInt()
                )
                sLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                shareFrameLayout.layoutParams = sLayout
                shareFrameLayout.requestLayout()

                val imgBtnParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    (focusToggleBtn.height * 1.2).toInt()
                )
                imgBtnParams.gravity = Gravity.CENTER
                shareImgBtn.layoutParams = imgBtnParams
                shareImgBtn.requestLayout()

            }
        }

        // 태블릿인경우
        else {
            topMenuLayout.post {

                // 상단 메뉴바 높이 조정
                val tLayout = RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (focusToggleBtn.height * 2.2).toInt()
                )
                tLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                topMenuLayout.layoutParams = tLayout
                topMenuLayout.requestLayout()

                // 하단 메뉴바 높이 조정
                val bLayout = RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (focusToggleBtn.height * 2.2).toInt()
                )
                bLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                bottomMenuLayout.layoutParams = bLayout
                bottomMenuLayout.requestLayout()
            }
        }

    }

    /** 사용자가 화면을 터치했을때 좌표를 항상 기록한다. */
    fun setAlwaysTouchPoint(x: Double, y: Double) {
        touchAlwaysTouchPointX = x
        touchAlwaysTouchPointY = y
    }

    /** ------------ 플로위 - 롱 클릭 모드-------------- */

    /** 사용자가 롱 클릭 모드에서 터치한 좌표값 설정 - FLowy Zoom 기능에 활용 */
    fun setTouchPoint(x: Double, y: Double) {
        touchPointX = x
        touchPointY = y
        Log.d("touch", "onViewCreated: $touchPointX , $touchPointY")
    }

    /** 사용자가 롱 클릭 모드에서 처음 터치한 좌표값 설정 - 화면 여백을 클릭하는걸 방지하기 위함 */
    fun setFirstTouchPoint(x: Double, y: Double) {
        touchFirstX = x
        touchFirstY = y
    }

    /** ------------ 플로위 - 더블탭 모드 -------------- */

    /** camera mode 가 default 이고, 더블탭했을때 좌표값 설정 */
    fun setDoubleTapTouchPoint(x: Double, y: Double) {
        doubleTapPointX = x
        doubleTapPointY = y
    }

    /** ------------ 포커스 기능 -------------- */

    fun setFocusTouchPoint(x: Float, y: Float) {
        touchFocusPointX = x
        touchFocusPointY = y

        // 90도 회전하기때문에 x,y값을 바꾸어서 넣어준다.
        val x = glTextureView.width * touchFocusPointY / adjustHeight
        val y = adjustHeight * touchFocusPointX / glTextureView.width

        touchFocusPointX = x
        touchFocusPointY = glTextureView.height - y

        Log.d("focusPoint", "$touchFocusPointX : $touchFocusPointY")
    }

    /** -------------------------------------------------- */

    /** 클릭 리스너 관리 */
    private fun setClickListener() {
        focusToggleBtn.setOnClickListener(this)
        flashToggleBtn.setOnClickListener(this)
        lensChangeToggleBtn.setOnClickListener(this)
        flowyZoomToggleBtn.setOnClickListener(this)
        mirroringToggleBtn.setOnClickListener(this)
        menuToggleBtn.setOnClickListener(this)
        flowyCastToggleBtn.setOnClickListener(this)
        freezeToggleBtn.setOnClickListener(this)
        luminanceToggleBtn.setOnClickListener(this)
        controlToggleBtn.setOnClickListener(this)
        shareImgBtn.setOnClickListener(this)
        bannerVersaAD.setOnClickListener(this)
    }


    private fun pinchZoomListener() {
        pinchZoomSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                // 핀치줌을 사용하지 않을때 카메라 확대
                if (!pinchZoomFlag && cameraMode != "flowy") {
                    camera!!.cameraControl.setLinearZoom(progress / 100.toFloat())
                    Log.d("setLinearZoom", (progress).toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (cameraMode != "flowy")
                    pinchZoomFlag = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (cameraMode != "flowy")
                    pinchZoomFlag = true
            }

        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun screenTouchListener() {

        glTextureView.setOnTouchListener(object : View.OnTouchListener {

            private val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent?): Boolean {
                        /** 터치를 하면 해당 위치에 포커스가 맞추어 지는 기능 */
                        focusToggleBtn.isChecked = true // 포커스 버튼을 활성화 시킨다.
                        autoFocusMode = false // 자동 포커스 기능을 해제한다.
                        return super.onSingleTapUp(e)
                    }

                    override fun onDoubleTap(e: MotionEvent): Boolean {

                        // 더블 탭 모드로 변경
                        if (cameraSubMode == "longClick") {
                            cameraSubMode = "flowyDoubleTap"
                            setDoubleTapTouchPoint(e.x.toDouble(), e.y.toDouble())
                            Log.d("doubleTapPoint", "${e.x} : ${e.y} ")
                            isDoubleTapFirstTouched = true
                        }

                        // 롱 클릭 모드로 변경
                        else {
                            cameraSubMode = "longClick"
                            setDoubleTapTouchPoint(0.0, 0.0)
                            isDoubleTapFirstTouched = false
                        }
                        return super.onDoubleTap(e)
                    }

                    override fun onShowPress(e: MotionEvent?) {
                        super.onShowPress(e)
                        Log.d("onShowPress", "onShowPress")
                        flowyZoomLongClickEvent = true // 롱클릭 이벤트가 발생했을때, 플로위 줌을 시작한다.
                    }
                })

            /** 핀치줌 기능 : 카메라 모드가 플로위 모드일떄랑 아닐떄랑 나뉜다.
             * 플로위 기능을 사용할떄는 플로위 확대 - 터치한 포인트를 기준으로 확대를 시켜주고
             * 플로위 기능을 사용 안할때는 일반 카메라 확대를 해준다. */
            private val pinchZoomGesture =
                ScaleGestureDetector(
                    context,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            Log.d("onScale", cameraMode)

                            // 일반 확대를 사용하는 경우
                            if (cameraMode != "flowy") {

                                // zoomRatio의 범위는 1~8배 까지이다.
                                var currentZoomRatio: Float =
                                    camera!!.cameraInfo.zoomState.value?.zoomRatio ?: 0F
                                var currentZoomLinear: Float =
                                    camera!!.cameraInfo.zoomState.value?.linearZoom ?: 0F
                                val delta = detector.scaleFactor
                                var scale = currentZoomRatio * delta
                                camera!!.cameraControl.setZoomRatio(scale)
                                Log.d("scaleValue123", "$currentZoomRatio : $currentZoomLinear")

                                scale -= 1
                                if (scale <= 0) scale = 0f
                                else if (scale >= 7) scale = 7f

                                if (currentZoomLinear <= 0) currentZoomLinear = 0f
                                else if (currentZoomLinear >= 1) currentZoomLinear = 1f

                                if (pinchZoomFlag) {
                                    Log.d(
                                        "scaleValue",
                                        " 1: $currentZoomRatio : $scale : ${pinchZoomSeekbar.progress}"
                                    )
                                    pinchZoomSeekbar.progress =
                                        (currentZoomLinear * 100.toDouble()).toInt()
                                    Log.d(
                                        "scaleValue",
                                        " 2: $currentZoomRatio : $scale : ${pinchZoomSeekbar.progress}"
                                    )
//                            Log.d("progress", pinchZoomSeekbar.progress.toString())
                                }
                            }
                            return true
                        }

                        // 핀치 줌이 끝나면 오토 포커스 모드로 들어간다.
                        override fun onScaleEnd(detector: ScaleGestureDetector?) {
                            // 일반 확대를 사용하는 경우
                            if (cameraMode != "flowy") {
                                CameraUtil().cameraAutoFocus(glTextureView)
                                pinchZoomFinishCallback = false
                                focusToggleBtn.isChecked = false
                            }
                            super.onScaleEnd(detector)
                        }

                        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                            // 일반 확대를 사용하는 경우
                            if (cameraMode != "flowy") {
                                pinchZoomFinishCallback = true
                            }
                            return super.onScaleBegin(detector)
                        }
                    })

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                Log.d("onTouch", "${event.x} : ${event.y} ")

                // 사용자가 찎은 좌표를 항상 기록한다.
                setAlwaysTouchPoint(event.x.toDouble(), event.y.toDouble())

                // 롱클릭 이벤트를 받아, 플로위 롱클릭 모드에서 움직이는 경우
                if (flowyZoomLongClickEvent && cameraMode == "flowy" && (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN)) {
                    // 첫번째로 터치한 좌표를 받아 firstTouchX , Y 에 할당한다. 화면 여백을 클릭하는걸 방지 하기 위함.
                    if (touchFirstX == 0.0 && touchFirstY == 0.0)
                        setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 터치한 포인트를 기록한다.
                    setTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 현재 상태를 터치중으로 변경한다.
                    isTouching = true
                } else if (cameraSubMode == "flowyDoubleTap") {

                    if (touchFirstX == 0.0 && touchFirstY == 0.0 && event.action == MotionEvent.ACTION_DOWN) {
                        setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())
                    } else {
                        setTouchPoint(event.x.toDouble(), event.y.toDouble())
                    }
                }

                // 사용자가 화면에서 손을 땠을때
                if (event.action == MotionEvent.ACTION_UP) {
                    setTouchPoint(0.0, 0.0) // 터치한 포인트를 0,0 으로 초기화한다.
                    setFirstTouchPoint(0.0, 0.0) // 첫번째로 터치한 포인트를 0,0으로 초기화한다.
                    setFocusTouchPoint(event.x, event.y)
                    Log.d("ClickEvent", "action up")
                    isTouching = false // 현재 상태를 터치중 아님으로 변경한다.
                    flowyZoomLongClickEvent = false // 플로위줌을 사용하기 다시 사용하기 위해 롱클릭 이벤트를 false로 만듦
                    CameraUtil().cameraTapFocus(glTextureView)
                }

                // 손가락 3개로 터치를 했을때, 메뉴를 보이게 / 안보이게 처리를 한다.
                if (event.action == MotionEvent.ACTION_POINTER_3_DOWN && event.pointerCount == 3) {
                    threePointClick()
                }


                gestureDetector.onTouchEvent(event)
                pinchZoomGesture.onTouchEvent(event)
                return true
            }
        })
    }

    /** 화면을 트리플 터치 했을떄 메뉴가 사라지고 나타나게 만드는 기능 */
    private fun threePointClick() {
        if (twoPointClickFlag) {
            if (topMenuLayout.visibility == View.VISIBLE)
                topMenuLayout.visibility = View.GONE
            else
                shareFrameLayout.visibility = View.GONE
            bottomMenuLayout.visibility = View.GONE
            pinchZoomLinearLayout.visibility = View.GONE

            // 애드몹 배너 광고 위치 조정
            val bannerAdmobViewLayout = RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (bannerAdView.height)
            )
            bannerAdmobViewLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            bannerAdmobViewLayout.addRule(RelativeLayout.CENTER_HORIZONTAL)
            bannerAdView.layoutParams = bannerAdmobViewLayout
            bannerAdView.requestLayout()

            // 회사 배너 광고 위치 조정
            val bannerVersaViewLayout = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                (bannerVersaAD.height)
            )
            bannerVersaViewLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            bannerVersaViewLayout.addRule(RelativeLayout.CENTER_HORIZONTAL)
            bannerVersaAD.layoutParams = bannerVersaViewLayout
            bannerVersaAD.requestLayout()

        } else {
            if (shareFrameLayout.visibility == View.GONE)
                shareFrameLayout.visibility = View.VISIBLE
            else
                topMenuLayout.visibility = View.VISIBLE
            bottomMenuLayout.visibility = View.VISIBLE
            pinchZoomLinearLayout.visibility = View.VISIBLE

            // 애드몹 광고 위치 조정
            val bannerAdmobViewLayout = RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (bannerAdView.height)
            )
            bannerAdmobViewLayout.addRule(RelativeLayout.ABOVE, R.id.pinchZoomLinearLayout)
            bannerAdmobViewLayout.addRule(RelativeLayout.CENTER_HORIZONTAL)
            bannerAdView.layoutParams = bannerAdmobViewLayout
            bannerAdView.requestLayout()

            // 회사 배너 광고 위치 조정
            val bannerVersaViewLayout = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                (bannerVersaAD.height)
            )
            bannerVersaViewLayout.addRule(RelativeLayout.ABOVE, R.id.pinchZoomLinearLayout)
            bannerVersaViewLayout.addRule(RelativeLayout.CENTER_HORIZONTAL)
            bannerVersaAD.layoutParams = bannerVersaViewLayout
            bannerVersaAD.requestLayout()
        }
        twoPointClickFlag = !twoPointClickFlag
    }

    /** 클릭 이벤트 처리 */
    override fun onClick(v: View) {

        when (v.id) {
            R.id.focusToggleBtn -> {
                // 포커스 기능 버튼
                autoFocusMode = !focusToggleBtn.isChecked

                if (autoFocusMode) CameraUtil().cameraAutoFocus(glTextureView)
                else CameraUtil().cameraTapFocus(glTextureView) // 포커스 기능을 누르면 오토 포커스 잡힌 곳으로 이동해야하는데 안됨.

            }
            /** 플래시 기능 */
            R.id.flashToggleBtn -> {

                try {
                    if (camera != null) {
                        if (flashToggleBtn.isChecked) {
                            camera!!.cameraControl.enableTorch(true)
                        } else {
                            camera!!.cameraControl.enableTorch(false)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "잠시후에 실행해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            /** 화면 전환 기능 (전면, 후면) */
            R.id.lensChangeToggleBtn -> {

                CoroutineScope(Dispatchers.Main).launch {
                    blackScreen.visibility = View.VISIBLE
                }

                // 0 : 전면
                // 1 : 후면

                // 화면 렌즈 모드바꾸기 전면 , 후면
                lensChangeFlag = true
                cameraLensMode = if (cameraLensMode == 0) 1 else 0

                // 화면이 전면으로 바뀌게 되면 플래시 기능을 비활성화 시킨다.
                if (camera != null) {
                    if (flashToggleBtn.isChecked) {
                        flashToggleBtn.isChecked = false
                        camera!!.cameraControl.enableTorch(false)
                    }
                }
                flashToggleBtn.isEnabled = cameraLensMode != 0

                // 화면이 전환 되면 포커스 기능을 끄고 자동포커스 키능을 켠다..
                focusToggleBtn.isChecked = false
                CameraUtil().cameraAutoFocus(glTextureView)

            }

            /** 플로위줌 기능 완료 */
            R.id.flowyZoomToggleBtn -> {
                // 플로위 줌 버튼을 눌렀을때 카메라 모드가 기본값이면, 카메라 모드는 flowy로 카메라 서브 모드는 longClick으로 변경한다.
                if (flowyZoomToggleBtn.isChecked) {
                    cameraMode = "flowy"
                    cameraSubMode = "longClick"
                }

                // 카메라 모드가 플로위 모드라면, 카메라모드를 기본값으로, 카메라 서브값도 기본값으로 변경한다.
                else {
                    cameraMode = "default"
                    cameraSubMode = "default"
                }
            }
            R.id.mirroringToggleBtn -> {
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context, "화면공유 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT)
                alertToast.show()
            }
            R.id.menuToggleBtn -> {
                (activity as MainActivity).replaceFragment("add", FragmentMenu().newInstance())
            }
            R.id.flowyCastToggleBtn -> {
                if (alertToast != null) alertToast.cancel()
                alertToast =
                    Toast.makeText(context, "플로위 캐스트 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT)
                alertToast.show()
            }
            /** 화면 멈춤 기능 완료 */
            R.id.freezeToggleBtn -> {
                // 눌렀는데 체크가 되어있다면
                freezeMode = freezeToggleBtn.isChecked

                CoroutineScope(Dispatchers.Main).launch {
                    if (freezeMode) {
                        topMenuLayout.visibility = View.INVISIBLE
                        shareFrameLayout.visibility = View.VISIBLE
                    } else {
                        topMenuLayout.visibility = View.VISIBLE
                        shareFrameLayout.visibility = View.INVISIBLE
                    }
                }
            }
            /** 고대비 기능 완료 */
            R.id.luminanceToggleBtn -> {

                // fragment Shader에서 프로그램을 한번만 만들기 위한 플래그
                // 프로그램을 여러번 만들면 메모리릭이 발생한다.
                luminanceFlag = true

                // 고대비를 활성화 시킨다.
                // arraylist에 등록된 고대비 색상의 갯수보다 내가 선택한 횟수가 많다면 기본색상을 보여줘야한다.
                if (luminanceIndex >= userContrastData.size) {
                    luminanceIndex = 0
                    if (userContrastData.size == 0){
                        if (alertToast != null) alertToast.cancel()
                        alertToast = Toast.makeText(context, "[메뉴 - 대비] 값을 설정해주세요", Toast.LENGTH_SHORT)
                        alertToast.show()
                    }
                } else {
                    luminanceIndex += 1
                }

                if (luminanceIndex == 0) {
                    fragmentType = "default"
                    luminanceToggleBtn.isChecked = false
                } else {
                    fragmentType = "luminance"
                    luminanceToggleBtn.isChecked = true
                }
            }
            R.id.controlToggleBtn -> {
                if (alertToast != null) alertToast.cancel()
                alertToast =
                    Toast.makeText(context, "밝기, 대비 조절 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT)
                alertToast.show()
            }

            /** 공유하기 버튼 */
            R.id.shareImgBtn -> {

                val filePath = Environment.getExternalStorageDirectory().toString()
                val folderName = "Flowy"
                val fileName = "shareImage.jpeg"
                val dirs = File(filePath, folderName)

                // Flowy 폴더가 없으면 만든다.
                if (!dirs.exists()) {
                    dirs.mkdirs()
                }

                try {
                    CoroutineScope(Dispatchers.Default).launch {
//                        delay(1000)
                        val b = glTextureView.bitmap
                        b!!.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            FileOutputStream(
                                "$filePath/$folderName/$fileName"
                            )
                        )
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }

                val shareFile = File(filePath, "$folderName/$fileName")

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/*"
                intent.flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                val uri = FileProvider.getUriForFile(
                    THIS_CONTEXT!!,
                    "com.overflow.flowy.fileprovider",
                    shareFile
                )
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(intent)
            }

            /** 회사 광고를 누르면 회사페이지로 ~ */
            R.id.bannerVersaAD -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://atoverflow.com/")))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("life","camera resume")

        togBtnStatusCheck() // 버튼 상태 불러오기

        /** 화면 방향 체크 */
        deviceRotationCheck()

        /** 광고 불러오기 */
        loadAdMob()
    }

    override fun onPause() {
        Log.d("lifeCycle","onPause")
        super.onPause()
        togBtnStatusSave() // 버튼의 상태 저장

        if (freezeMode) freezeMode = false
    }

    override fun onStop() {
        Log.d("lifeCycle","onStop")
        super.onStop()
    }

    /** 기기의 방향 체크 - 카메라 프래그먼트에서 화면 방향에 따라서 UI 버튼도 회전이 되어야한다. */
    private fun deviceRotationCheck() {
        val orientationEventListener =
            object : OrientationEventListener(THIS_CONTEXT, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    deviceRotationValue = orientation
                    Log.d("orientation", "orientation = $deviceRotationValue")
                    uiDirectionChange()
                }
            }
        orientationEventListener.enable()
    }

    /** 메뉴 버튼 방향 변경 */
    private fun uiDirectionChange() {
        // 정방향 이미지
        if (deviceRotationValue > 315 || deviceRotationValue <= 45) {
            deviceSensorDirection = 0f
            seekBarAnimation(pinchZoomSeekbar, 0f)
        }
        // 버튼 왼쪽으로 90도 회전
        else if (deviceRotationValue in 46..135) {
            deviceSensorDirection = -90f
            seekBarAnimation(pinchZoomSeekbar, 180f)
        }
        // 역방향 이미지
        else if (deviceRotationValue in 136..225) {
            deviceSensorDirection = -180f
            seekBarAnimation(pinchZoomSeekbar, 180f)
        }

        // 버튼 오른쪽으로 90도 회전
        else if (deviceRotationValue in 226..315) {
            deviceSensorDirection = 90f
            seekBarAnimation(pinchZoomSeekbar, 0f)
        }
        imgButtonAnimation(shareImgBtn, deviceSensorDirection)
        menuButtonAnimation(focusToggleBtn, deviceSensorDirection)
        menuButtonAnimation(flashToggleBtn, deviceSensorDirection)
        menuButtonAnimation(lensChangeToggleBtn, deviceSensorDirection)
        menuButtonAnimation(flowyZoomToggleBtn, deviceSensorDirection)
        menuButtonAnimation(mirroringToggleBtn, deviceSensorDirection)
        menuButtonAnimation(menuToggleBtn, deviceSensorDirection)
        menuButtonAnimation(flowyCastToggleBtn, deviceSensorDirection)
        menuButtonAnimation(freezeToggleBtn, deviceSensorDirection)
        menuButtonAnimation(freezeToggleBtn, deviceSensorDirection)
        menuButtonAnimation(luminanceToggleBtn, deviceSensorDirection)
        menuButtonAnimation(controlToggleBtn, deviceSensorDirection)
    }

    private fun imgButtonAnimation(imgBtn: ImageButton, rotation: Float) {
        imgBtn.animate().apply { this.duration = 100; this.rotation(rotation) }.start()
    }

    private fun menuButtonAnimation(toggleBtn: ToggleButton, rotation: Float) {
        toggleBtn.animate().apply { this.duration = 100; this.rotation(rotation) }.start()
    }

    private fun seekBarAnimation(seekBar: SeekBar, rotation: Float) {
        seekBar.animate().apply { this.duration = 0; this.rotation(rotation) }.start()
        if (rotation == 180f) {
            pinchZoomMinusImgBtn.setImageResource(R.drawable.pinchzoom_plus)
            pinchZoomPlusImgBtn.setImageResource(R.drawable.pinchzoom_minus)
            pinchZoomMinusImgBtn.animate().apply { this.rotation(180f) }.start()
            pinchZoomPlusImgBtn.animate().apply { this.rotation(180f) }.start()
        } else {
            pinchZoomMinusImgBtn.setImageResource(R.drawable.pinchzoom_minus)
            pinchZoomPlusImgBtn.setImageResource(R.drawable.pinchzoom_plus)
            pinchZoomMinusImgBtn.animate().apply { this.duration = 0; this.rotation(0f) }.start()
            pinchZoomPlusImgBtn.animate().apply { this.duration = 0; this.rotation(0f) }.start()
        }

    }

    /** 광고 로드 */
    private fun loadAdMob() {
        MobileAds.initialize(THIS_CONTEXT, getString(R.string.admob_app_id))
        val adRequest = AdRequest.Builder().build()
        bannerAdView.loadAd(adRequest)
        bannerAdView.bringToFront()

        bannerAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                bannerAdView.visibility = View.VISIBLE
                bannerVersaAD.visibility = View.GONE

                // 광고가 문제 없이 로드시 출력
                Log.d("@@@", "onAdLoaded")
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                // 광고 로드에 문제가 있을시에 플로위 광고를 보여준다.
                Log.d("@@@", "onAdFailedToLoad $errorCode")
                if (errorCode == 3) {
                    Glide.with(THIS_CONTEXT!!)
                        .asGif()
                        .load(R.raw.versa_banner)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(bannerVersaAD)
                    bannerVersaAD.bringToFront()
                    bannerAdView.visibility = View.INVISIBLE
                }
            }

            override fun onAdOpened() {
            }

            override fun onAdClicked() {
            }

            override fun onAdLeftApplication() {
            }

            override fun onAdClosed() {
            }
        }
    }

    companion object {

        /** 사용자가 찍은 좌표값 (항상 갱신됨) */
        var touchAlwaysTouchPointX = 0.0
        var touchAlwaysTouchPointY = 0.0

        /** --------------------- 플로위 롱 클릭 모드 --------------------- */

        /** 사용자가 터치중인지 여부 판단함 */
        var isTouching: Boolean = false
        var isDoubleTapFirstTouched: Boolean = false // 더블탭을 처음 클릭했는가?

        /** 사용자가 플로위 롱 클릭 모드에서 터치한 좌표 값 ( 지속적으로 바뀜 ) */
        var touchPointX: Double = 0.1
        var touchPointY: Double = 0.1

        /** 사용자가 플로위 롱 클릭 모드에서 처음으로 터치한 좌표 값 ( 화면 여백을 클릭하는걸 방지하기 위함 ) */
        var touchFirstX: Double = 0.1
        var touchFirstY: Double = 0.1

        /** --------------------- 플로위 더블 탭 모드 --------------------- */

        /** 사용자가 더블 탭모드에서 찍은 위치의 좌표 값 */
        var doubleTapPointX: Double = 0.0
        var doubleTapPointY: Double = 0.0


        /** --------------------- 고대비 기능 모드 --------------------- */

        /** 고대비 색상과 인덱스 */
        var luminanceIndex: Int = 0
        var luminanceFlag: Boolean = false

        /** --------------------- 화면 전환 기능 --------------------- */
        var lensChangeFlag = false


        /** 포커스 기능 */
        var touchFocusPointX: Float = 0f
        var touchFocusPointY: Float = 0f

        lateinit var blackScreen: ImageView

        var userContrastData = arrayListOf<ContrastData>()

    }

}