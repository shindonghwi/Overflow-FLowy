package com.overflow.flowy.Fragment

import android.R.attr.button
import android.annotation.SuppressLint
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import com.overflow.flowy.DTO.LuminanceData
import com.overflow.flowy.R
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.adjustHeight
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.camera
import com.overflow.flowy.Util.*
import com.overflow.flowy.View.FlowyGLSurfaceView


class FragmentCamera : Fragment(), View.OnClickListener {

    private lateinit var glSurfaceView: FlowyGLSurfaceView // 카메라 미리보기가 나올 화면
    private var flowyZoomLongClickEvent: Boolean = false // 롱클릭 이벤트 콜백을 위한 변수, 이벤트 발생시 플로위 줌 시작

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

    private lateinit var alertToast: Toast
    private lateinit var pinchZoomSeekbar: SeekBar
    private lateinit var pinchZoomMinusImgBtn: ImageButton
    private lateinit var pinchZoomPlusImgBtn: ImageButton

    private var pinchZoomFinishCallback: Boolean = false
    private var deviceSensorDirection = 0f

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
        glSurfaceView = view.findViewById(R.id.glSurfaceView)

        alertToast = Toast(context)

        idInit(view = view)

        setClickListener() // 클릭 리스너 설정
        screenTouchListener() // 터치 리스너 설정
        pinchZoomListener() // 핀치줌 리스너 설정
    }

    /** layout id 초기화하는 공간 */
    private fun idInit(view: View) {
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

        pinchZoomSeekbar = view.findViewById(R.id.pinchZoomSeekbar)
        pinchZoomMinusImgBtn = view.findViewById(R.id.pinchZoomMinusImgBtn)
        pinchZoomPlusImgBtn = view.findViewById(R.id.pinchZoomPlusImgBtn)

        /** 고대비 기본 색상 초기화 */
        luminanceDataInit()
    }

    /** 고대비 기본 색상 초기화 */
    private fun luminanceDataInit() {
        luminanceArrayData = ArrayList<LuminanceData>()
        luminanceArrayData.add(LuminanceData(R.color.black, R.color.white))
        luminanceArrayData.add(LuminanceData(R.color.black, R.color.yellow))
        luminanceArrayData.add(LuminanceData(R.color.blue, R.color.white))
        luminanceArrayData.add(LuminanceData(R.color.blue, R.color.yellow))
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
        val x = glSurfaceView.width * touchFocusPointY / adjustHeight
        val y = adjustHeight * touchFocusPointX / glSurfaceView.width

        touchFocusPointX = x
        touchFocusPointY = glSurfaceView.height - y

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
    }

    private var pinchZoomFlag: Boolean = true

    private fun pinchZoomListener() {
        pinchZoomSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                // 핀치줌을 사용하지 않을때 카메라 확대
                if (!pinchZoomFlag) {
                    camera!!.cameraControl.setLinearZoom(progress / 100.toFloat())
                    Log.d("setLinearZoom", (progress).toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                pinchZoomFlag = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                pinchZoomFlag = true
            }

        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun screenTouchListener() {

        glSurfaceView.setOnTouchListener(object : View.OnTouchListener {
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

                    override fun onLongPress(e: MotionEvent?) {
                        super.onLongPress(e)
                        flowyZoomLongClickEvent = true // 롱클릭 이벤트가 발생했을때, 플로위 줌을 시작한다.
                    }
                })

            private val pinchZoomGesture =
                ScaleGestureDetector(
                    context,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector?): Boolean {
                            Log.d("onScale", "onScale")

                            // zoomRatio의 범위는 1~8배 까지이다.
                            var currentZoomRatio: Float =
                                camera!!.cameraInfo.zoomState.value?.zoomRatio ?: 0F
                            var currentZoomLinear: Float =
                                camera!!.cameraInfo.zoomState.value?.linearZoom ?: 0F
                            val delta = detector!!.scaleFactor
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
                            return true
                        }

                        // 핀치 줌이 끝나면 오토 포커스 모드로 들어간다.
                        override fun onScaleEnd(detector: ScaleGestureDetector?) {
                            CameraUtil().cameraAutoFocus(glSurfaceView)
                            pinchZoomFinishCallback = false
                            focusToggleBtn.isChecked = false
                            super.onScaleEnd(detector)
                        }

                        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                            pinchZoomFinishCallback = true
                            return super.onScaleBegin(detector)
                        }
                    })


            override fun onTouch(v: View, event: MotionEvent): Boolean {
                Log.d("onTouch", "${event.x} : ${event.y} ")

                // 사용자가 찎은 좌표를 항상 기록한다.
                setAlwaysTouchPoint(event.x.toDouble(), event.y.toDouble())

                // 롱클릭 이벤트를 받아, 플로위 롱클릭 모드에서 움직이는 경우
                if (flowyZoomLongClickEvent && cameraMode == "flowy" && event.action == MotionEvent.ACTION_MOVE) {
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
                    CameraUtil().cameraTapFocus(glSurfaceView)
                }

                gestureDetector.onTouchEvent(event)
                pinchZoomGesture.onTouchEvent(event)
                return true
            }
        })
    }

    /** 클릭 이벤트 처리 */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.focusToggleBtn -> {
                // 포커스 기능 버튼
                autoFocusMode = !focusToggleBtn.isChecked

                if (autoFocusMode) CameraUtil().cameraAutoFocus(glSurfaceView)
                else CameraUtil().cameraTapFocus(glSurfaceView) // 포커스 기능을 누르면 오토 포커스 잡힌 곳으로 이동해야하는데 안됨.

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
                CameraUtil().cameraAutoFocus(glSurfaceView)

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
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context, "메뉴 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT)
                alertToast.show()
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
            }
            /** 고대비 기능 완료 */
            R.id.luminanceToggleBtn -> {

                // fragment Shader에서 프로그램을 한번만 만들기 위한 플래그
                // 프로그램을 여러번 만들면 메모리릭이 발생한다.
                luminanceFlag = true

                // 고대비를 활성화 시킨다.
                // arraylist에 등록된 고대비 색상의 갯수보다 내가 선택한 횟수가 많다면 기본색상을 보여줘야한다.
                if (luminanceIndex >= luminanceArrayData.size) {
                    luminanceIndex = 0
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


        }
    }

    override fun onResume() {
        super.onResume()
        /** 화면 방향 체크 */
        deviceRotationCheck()
    }

    /** 기기의 방향 체크 - 카메라 프래그먼트에서 화면 방향에 따라서 UI 버튼도 회전이 되어야한다. */
    private fun deviceRotationCheck(){
        val orientationEventListener = object:OrientationEventListener(THIS_CONTEXT, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation:Int) {
                deviceRotationValue = orientation
                Log.d("orientation", "orientation = $deviceRotationValue")
                uiDirectionChange()
            }
        }
        orientationEventListener.enable()
    }

    /** 메뉴 버튼 방향 변경 */
    private fun uiDirectionChange(){
        // 정방향 이미지
        if ( deviceRotationValue > 315 || deviceRotationValue <= 45){
            deviceSensorDirection = 0f
            focusToggleBtn.animate().rotation(0f).interpolator = AccelerateDecelerateInterpolator()
            seekBarAnimation(pinchZoomSeekbar, 0f)
        }
        // 버튼 왼쪽으로 90도 회전
        else if (deviceRotationValue in 46..135){
            deviceSensorDirection = -90f
            seekBarAnimation(pinchZoomSeekbar, 180f)
        }
        // 역방향 이미지
        else if (deviceRotationValue in 136..225){
            deviceSensorDirection = -180f
            seekBarAnimation(pinchZoomSeekbar, 180f)
        }

        // 버튼 오른쪽으로 90도 회전
        else if (deviceRotationValue in 226..315){
            deviceSensorDirection = 90f
            seekBarAnimation(pinchZoomSeekbar, 0f)
        }
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

    private fun menuButtonAnimation(toggleBtn : ToggleButton, rotation : Float){
        toggleBtn.animate().apply { this.duration = 100; this.rotation(rotation) }.start()
    }
    private fun seekBarAnimation(seekBar: SeekBar , rotation : Float){
        seekBar.animate().apply { this.duration = 0; this.rotation(rotation) }.start()
        if (rotation == 180f){
            pinchZoomMinusImgBtn.setImageResource(R.drawable.pinchzoom_plus)
            pinchZoomPlusImgBtn.setImageResource(R.drawable.pinchzoom_minus)
            pinchZoomMinusImgBtn.animate().apply { this.rotation(180f) }.start()
            pinchZoomPlusImgBtn.animate().apply { this.rotation(180f) }.start()
        }
        else{
            pinchZoomMinusImgBtn.setImageResource(R.drawable.pinchzoom_minus)
            pinchZoomPlusImgBtn.setImageResource(R.drawable.pinchzoom_plus)
            pinchZoomMinusImgBtn.animate().apply {  this.duration = 0; this.rotation(0f) }.start()
            pinchZoomPlusImgBtn.animate().apply { this.duration = 0;  this.rotation(0f) }.start()
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
        var touchPointX: Double = 0.0
        var touchPointY: Double = 0.0

        /** 사용자가 플로위 롱 클릭 모드에서 처음으로 터치한 좌표 값 ( 화면 여백을 클릭하는걸 방지하기 위함 ) */
        var touchFirstX: Double = 0.0
        var touchFirstY: Double = 0.0

        /** --------------------- 플로위 더블 탭 모드 --------------------- */

        /** 사용자가 더블 탭모드에서 찍은 위치의 좌표 값 */
        var doubleTapPointX: Double = 0.0
        var doubleTapPointY: Double = 0.0


        /** --------------------- 고대비 기능 모드 --------------------- */

        /** 고대비 색상과 인덱스 */
        lateinit var luminanceArrayData: ArrayList<LuminanceData>
        var luminanceIndex: Int = 0
        var luminanceFlag: Boolean = false

        /** --------------------- 화면 전환 기능 --------------------- */
        var lensChangeFlag = false


        /** 포커스 기능 */
        var touchFocusPointX: Float = 0f
        var touchFocusPointY: Float = 0f

    }

}