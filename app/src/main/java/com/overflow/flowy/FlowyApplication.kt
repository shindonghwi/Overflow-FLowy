package com.overflow.flowy

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.cameraLifecycle
import com.overflow.flowy.Util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class FlowyApplication : Application(), LifecycleObserver {

    private var backgroundTimeCheck : Long = 0L
    private var backgroundFlag: Boolean = false

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        backgroundTimeCheckStart()
        Log.d("LifecycleAPp","onAppBackgrounded")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        backgroundTimeCheckEnd()
        Log.d("LifecycleAPp","onAppForegrounded")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onAppCreated() {
        Log.d("LifecycleAPp","onAppCreated")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAppResumed() {
        Log.d("LifecycleAPp","onAppResumed")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppDestroyed() {
        removeToggleBtnStatus()
        Log.d("LifecycleAPp","onAppDestroyed")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppPaused() {
        Log.d("LifecycleAPp","onAppPaused")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    fun onAppAny() {
        Log.d("LifecycleAPp","onAppAny")
    }

    /** 백그라운드에서 시간을 체크한다. 카메라 수명주기 관리때문, 1분이 되면 error가 발생해서 55초에 카메라 수명주기를 닫는 메서드임 */
    private fun backgroundTimeCheckStart(){
        backgroundFlag = true
        CoroutineScope(Dispatchers.Default).launch {
            while (backgroundFlag){
                delay(1000)
                backgroundTimeCheck += 1L

                if (backgroundTimeCheck >= 55L){
                    removeToggleBtnStatus()
                    modeInit()
                    cameraLifecycle.doOnDestroy()
                    break
                }

                Log.d("LifecycleAPp","time : $backgroundTimeCheck")
            }
        }
    }
    fun backgroundTimeCheckEnd(){
        backgroundFlag = false
        backgroundTimeCheck = 0L
    }

    /** 화면을 닫을시 토글버튼 상태 초기화 */
    private fun removeToggleBtnStatus() {
        try {
            val f = File("/data/data/com.overflow.flowy/shared_prefs", "flowyToggleBtnStatus.xml")
            f.delete()
        } catch (e: Exception) {

        }
    }

    private fun modeInit(){
        cameraMode = "default"
        cameraSubMode = "longClick"
        fragmentType = "default"
        vertexType = "default"
        freezeMode = false
        autoFocusMode = true
    }
}