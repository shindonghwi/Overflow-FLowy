package com.overflow.flowy

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.cameraLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    fun backgroundTimeCheckStart(){
        backgroundFlag = true
        CoroutineScope(Dispatchers.Default).launch {
            while (backgroundFlag){
                delay(1000)
                backgroundTimeCheck += 1L

                if (backgroundTimeCheck >= 55L){
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

}