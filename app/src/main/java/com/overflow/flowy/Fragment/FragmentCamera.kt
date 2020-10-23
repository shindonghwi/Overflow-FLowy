package com.overflow.flowy.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import com.overflow.flowy.R
import com.overflow.flowy.Util.THIS_CONTEXT
import com.overflow.flowy.Util.cameraMode
import com.overflow.flowy.Util.cameraSubMode
import com.overflow.flowy.View.FlowyGLSurfaceView

class FragmentCamera : Fragment(), View.OnClickListener {

    private lateinit var glSurfaceView: FlowyGLSurfaceView // 카메라 미리보기가 나올 화면
    private lateinit var flowyZoomBtn: Button // 플로위 줌 버튼 활성/비활성화 버튼
    private var flowyZoomLongClickEvent: Boolean = false // 롱클릭 이벤트 콜백을 위한 변수, 이벤트 발생시 플로위 줌 시작

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
        flowyZoomBtn = view.findViewById(R.id.flowyZoomBtn)

        setClickListener() // 클릭 리스너 설정
        screenTouchListener() // 터치 리스너 설정

    }

    /** 사용자가 터치한 좌표값 설정 - FLowy Zoom 기능에 활용 */
    fun setTouchPoint(x: Double, y: Double) {
        touchPointX = x
        touchPointY = y
        Log.d("touch", "onViewCreated: $touchPointX , $touchPointY")
    }

    /** 사용자가 처음 터치한 좌표값 설정 - 화면 여백을 클릭하는걸 방지하기 위함 */
    fun setFirstTouchPoint(x: Double, y: Double) {
        touchFirstX = x
        touchFirstY = y
    }

    /** camera mode 가 default 이고, 더블탭했을때 좌표값 설정 */
    fun setDoubleTapTouchPoint(x: Double, y: Double) {
        doubleTapPointX = x
        doubleTapPointY = y
    }

    /** 클릭 리스너 관리 */
    private fun setClickListener() {
        flowyZoomBtn.setOnClickListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun screenTouchListener() {

        glSurfaceView.setOnTouchListener(object : View.OnTouchListener {
            private val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {

                        if (cameraSubMode == "longClick") {
                            cameraSubMode = "flowyDoubleTap"
                            setDoubleTapTouchPoint(e.x.toDouble(), e.y.toDouble())
                            Log.d("doubleTapPoint", "${e.x} : ${e.y} ")
                        } else {
                            cameraSubMode = "longClick"
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

                // 롱클릭 이벤트를 받아, 플로위 모드를 들어갔을때
                if (flowyZoomLongClickEvent && cameraMode == "flowy" && event.action == MotionEvent.ACTION_MOVE) {
                    // 첫번째로 터치한 좌표를 받아 firstTouchX , Y 에 할당한다. 화면 여백을 클릭하는걸 방지 하기 위함.
                    if (touchFirstX == 0.0 && touchFirstY == 0.0)
                        setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 터치한 포인트를 기록한다.
                    setTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 현재 상태를 터치중으로 변경한다.
                    isTouching = true
                }
                // 사용자가 화면에서 손을 땠을때
                if (event.action == MotionEvent.ACTION_UP) {
                    setTouchPoint(0.0, 0.0) // 터치한 포인트를 0,0 으로 초기화한다.
                    setFirstTouchPoint(0.0, 0.0) // 첫번째로 터치한 포인트를 0,0으로 초기화한다.
                    Log.d("ActionUP", "onTouch: up")
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
            // 플로위 줌을 사용 여부를 변경한다.
            R.id.flowyZoomBtn -> {

                // 플로위 줌 버튼을 눌렀을때 카메라 모드가 기본값이면, 카메라 모드는 flowy로 카메라 서브 모드는 longClick으로 변경한다.
                if (cameraMode == "default") {
                    cameraMode = "flowy"
                    cameraSubMode = "longClick"
                    flowyZoomBtn.text = "Zoom ON"
                }

                // 카메라 모드가 플로위 모드라면, 카메라모드를 기본값으로, 카메라 서브값도 기본값으로 변경한다.
                else if (cameraMode == "flowy") {
                    cameraMode = "default"
                    cameraSubMode = "longClick"
                    flowyZoomBtn.text = "Zoom OFF"
                }
            }
        }
    }

    companion object {

        /** 사용자가 터치한 좌표 값 ( 지속적으로 바뀜 ) */
        var touchPointX: Double = 0.0
        var touchPointY: Double = 0.0

        /** 사용자가 터치중인지 여부 판단함 */
        var isTouching: Boolean = false

        /** 사용자가 처음으로 터치한 좌표 값 ( 화면 여백을 클릭하는걸 방지하기 위함 ) */
        var touchFirstX: Double = 0.0
        var touchFirstY: Double = 0.0

        /** 사용자가 더블 탭한 위치의 좌표 값 */
        var doubleTapPointX: Double = 0.0
        var doubleTapPointY: Double = 0.0
    }
}