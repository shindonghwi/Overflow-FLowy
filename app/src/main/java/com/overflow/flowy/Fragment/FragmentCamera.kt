package com.overflow.flowy.Fragment

import android.R.attr.x
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import com.overflow.flowy.DTO.LuminanceData
import com.overflow.flowy.R
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.camera
import com.overflow.flowy.Util.*
import com.overflow.flowy.View.FlowyGLSurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception


class FragmentCamera : Fragment(), View.OnClickListener {

    private lateinit var glSurfaceView: FlowyGLSurfaceView // 카메라 미리보기가 나올 화면
    private var flowyZoomLongClickEvent: Boolean = false // 롱클릭 이벤트 콜백을 위한 변수, 이벤트 발생시 플로위 줌 시작

    /** 메뉴바 상단에 있는 버튼 */
    private lateinit var focusToggleBtn : ToggleButton
    private lateinit var flashToggleBtn : ToggleButton
    private lateinit var lensChangeToggleBtn : ToggleButton
    private lateinit var flowyZoomToggleBtn : ToggleButton
    private lateinit var mirroringToggleBtn : ToggleButton

    /** 메뉴바 하단에 있는 버튼 */
    private lateinit var menuToggleBtn : ToggleButton
    private lateinit var flowyCastToggleBtn : ToggleButton
    private lateinit var freezeToggleBtn : ToggleButton
    private lateinit var luminanceToggleBtn : ToggleButton
    private lateinit var controlToggleBtn : ToggleButton

    private lateinit var alertToast : Toast

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

    }

    /** layout id 초기화하는 공간 */
    private fun idInit(view : View){
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

        /** 고대비 기본 색상 초기화 */
        luminanceDataInit()
    }

    /** 고대비 기본 색상 초기화 */
    fun luminanceDataInit(){
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

    @SuppressLint("ClickableViewAccessibility")
    private fun screenTouchListener() {

        glSurfaceView.setOnTouchListener(object : View.OnTouchListener {
            private val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
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
                            setDoubleTapTouchPoint(0.0,0.0)
                            isDoubleTapFirstTouched = false
                        }
                        return super.onDoubleTap(e)
                    }

                    override fun onLongPress(e: MotionEvent?) {
                        super.onLongPress(e)
                        flowyZoomLongClickEvent = true // 롱클릭 이벤트가 발생했을때, 플로위 줌을 시작한다.
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
                }
                else if (cameraSubMode == "flowyDoubleTap"){

                    if (touchFirstX == 0.0 && touchFirstY == 0.0 && event.action == MotionEvent.ACTION_DOWN){
                        setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())
                    }
                    else{
                        setTouchPoint(event.x.toDouble(), event.y.toDouble())
                    }
                }

                // 사용자가 화면에서 손을 땠을때
                if (event.action == MotionEvent.ACTION_UP) {
                    setTouchPoint(0.0, 0.0) // 터치한 포인트를 0,0 으로 초기화한다.
                    setFirstTouchPoint(0.0, 0.0) // 첫번째로 터치한 포인트를 0,0으로 초기화한다.
                    Log.d("ClickEvent", "action up")
                    isTouching = false // 현재 상태를 터치중 아님으로 변경한다.
                    flowyZoomLongClickEvent = false // 플로위줌을 사용하기 다시 사용하기 위해 롱클릭 이벤트를 false로 만듦
                }

                gestureDetector.onTouchEvent(event)
                return true
            }
        })
    }

    /** 클릭 이벤트 처리 */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.focusToggleBtn ->{
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context,"포커스 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT )
                alertToast.show()
            }
            /** 플래시 기능 */
            R.id.flashToggleBtn ->{

                try {
                    if ( flashToggleBtn.isChecked ){
                        camera.cameraControl.enableTorch(true)
                    }
                    else{
                        camera.cameraControl.enableTorch(false)
                    }
                }
                catch (e : Exception){
                    Toast.makeText(context, "잠시후에 실행해주세요",Toast.LENGTH_SHORT).show()
                }

            }
            /** 화면 전환 기능 (전면, 후면) */
            R.id.lensChangeToggleBtn ->{

                // 0 : 전면
                // 1 : 후면

                lensChangeFlag = true
                cameraLensMode = if (cameraLensMode == 0) 1 else 0

                if (flashToggleBtn.isChecked){
                    flashToggleBtn.isChecked = false
                    camera.cameraControl.enableTorch(false)
                }

                flashToggleBtn.isEnabled = cameraLensMode != 0
            }

            /** 플로위줌 기능 완료 */
            R.id.flowyZoomToggleBtn -> {
                // 플로위 줌 버튼을 눌렀을때 카메라 모드가 기본값이면, 카메라 모드는 flowy로 카메라 서브 모드는 longClick으로 변경한다.
                if (flowyZoomToggleBtn.isChecked) {
                    cameraMode = "flowy"
                    cameraSubMode = "longClick"
                }

                // 카메라 모드가 플로위 모드라면, 카메라모드를 기본값으로, 카메라 서브값도 기본값으로 변경한다.
                else{
                    cameraMode = "default"
                    cameraSubMode = "default"
                }
            }
            R.id.mirroringToggleBtn ->{
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context,"화면공유 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT )
                alertToast.show()
            }
            R.id.menuToggleBtn ->{
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context,"메뉴 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT )
                alertToast.show()
            }
            R.id.flowyCastToggleBtn ->{
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context,"플로위 캐스트 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT )
                alertToast.show()
            }
            R.id.freezeToggleBtn ->{
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context,"화면 멈춤 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT )
                alertToast.show()
            }
            /** 고대비 기능 완료 */
            R.id.luminanceToggleBtn ->{

                // fragment Shader에서 프로그램을 한번만 만들기 위한 플래그
                // 프로그램을 여러번 만들면 메모리릭이 발생한다.
                luminanceFlag = true

                // 고대비를 활성화 시킨다.
                // arraylist에 등록된 고대비 색상의 갯수보다 내가 선택한 횟수가 많다면 기본색상을 보여줘야한다.
                if (luminanceIndex >= luminanceArrayData.size) {
                    luminanceIndex = 0
                }
                else{
                    luminanceIndex += 1
                }

                if (luminanceIndex == 0) {
                    fragmentType = "default"
                    luminanceToggleBtn.isChecked = false
                }
                else {
                    fragmentType = "luminance"
                    luminanceToggleBtn.isChecked = true
                }
            }
            R.id.controlToggleBtn ->{
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context,"밝기, 대비 조절 기능은 서비스 구현 예정입니다.", Toast.LENGTH_SHORT )
                alertToast.show()
            }


        }
    }

    /** 메뉴 활성화된 버튼 해제 */
//    private fun menuCheckedOff(clickToggleButton: ToggleButton) {
//
//        // 사용자가 누른 버튼이 눌렀을때 꺼질 상태라면, 해당 버튼을 비활성화 한다.
//        if(!clickToggleButton.isChecked){
//            clickToggleButton.isChecked = false
//        }
//        // 사용자가 누른 버튼이 눌렀을때 켜질 상태라면, 다른버튼은 비활성화하고 선택한 버튼만 활성화 한다.
//        else{
//            focusToggleBtn.isChecked = false
//            flashToggleBtn.isChecked = false
//            lensChangeToggleBtn.isChecked = false
//            flowyZoomToggleBtn.isChecked = false
//            mirroringToggleBtn.isChecked = false
//            menuToggleBtn.isChecked = false
//            flowyCastToggleBtn.isChecked = false
//            freezeToggleBtn.isChecked = false
//            contrastToggleBtn.isChecked = false
//            controlToggleBtn.isChecked = false
//            clickToggleButton.isChecked = true
//        }
//    }

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
        lateinit var luminanceArrayData : ArrayList<LuminanceData>
        var luminanceIndex : Int = 0
        var luminanceFlag : Boolean = false

        /** --------------------- 화면 전환 기능 --------------------- */
        var lensChangeFlag = false


    }

}