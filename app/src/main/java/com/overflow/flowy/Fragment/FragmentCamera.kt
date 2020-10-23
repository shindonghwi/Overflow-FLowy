package com.overflow.flowy.Fragment

import android.annotation.SuppressLint
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.overflow.flowy.MainActivity
import com.overflow.flowy.R
import com.overflow.flowy.Renderer.FlowyRenderer
import com.overflow.flowy.Util.THIS_CONTEXT
import com.overflow.flowy.Util.cameraMode
import com.overflow.flowy.Util.flowyMode
import com.overflow.flowy.View.FlowyGLSurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlowyCameraFragment : Fragment(), View.OnClickListener {

    private lateinit var glSurfaceView: FlowyGLSurfaceView
    private lateinit var flowyZoomBtn: Button
    private lateinit var doubleTapListener : GestureDetector

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
        setDoubleTapListener() // 더블 탭 리스너 설정
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
        glSurfaceView.setOnClickListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun screenTouchListener() {

        glSurfaceView.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                Log.d("ASdd", "onTouch: ")

                // 플로위 모드일때
                if (flowyMode && event.action == MotionEvent.ACTION_MOVE) {

                    // 첫번째로 터치한 좌표를 받아 firstTouchX , Y 에 할당한다. 화면 여백을 클릭하는걸 방지 하기 위함.
                    if ( touchFirstX == 0.0 && touchFirstY == 0.0 )
                        setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())

                    setTouchPoint(event.x.toDouble(), event.y.toDouble()) // 터치한 포인트를 기록한다.
                    isTouching = true // 현재 상태를 터치중으로 변경한다.
                }

                // 플로위 모드가 아닐때
                else {
                    setTouchPoint(0.0, 0.0) // 터치한 포인트를 0,0 으로 초기화한다.
                    setFirstTouchPoint(0.0, 0.0) // 첫번째로 터치한 포인트를 0,0으로 초기화한다.
                    isTouching = false // 현재 상태를 터치중 아님으로 변경한다.
                }

                return doubleTapListener.onTouchEvent(event)
            }
        })
    }

    /** camera mode가 default 이고, 사용자가 더블 탭을 클릭했을때, 더블 탭한 좌표를 셋팅하고 camera mode를 doubleTap 모드로 변경 */
    private fun setDoubleTapListener(){
        doubleTapListener = GestureDetector(object:GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e:MotionEvent):Boolean {

                if (cameraMode == "default"){
                    cameraMode = "doubleTap"
                    setDoubleTapTouchPoint(e.x.toDouble(), e.y.toDouble())
                    Log.d("doubleTapPoint", "${e.x} : ${e.y} ")
                }
                return true
            }
        })
    }

    /** 클릭 이벤트 처리 */
    override fun onClick(v: View) {

        when (v.id) {
            // 플로위 줌을 사용 여부를 변경한다.
            R.id.flowyZoomBtn -> {
                if (!flowyMode) {
                    flowyMode = !flowyMode
                    cameraMode = "flowy"
                } else {
                    flowyMode = !flowyMode
                    cameraMode = "default"
                }
            }
            R.id.glSurfaceView -> {
                Log.d("asddssa","click")
            }
        }
    }

    companion object {
        var touchPointX: Double = 0.0
        var touchPointY: Double = 0.0
        var isTouching: Boolean = false
        var touchFirstX : Double = 0.0
        var touchFirstY : Double = 0.0
        var doubleTapPointX : Double = 0.0
        var doubleTapPointY : Double = 0.0

    }
}