package com.overflow.flowy.Util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry


/** 카메라 x의 수명주기에 바인딩 할때 사용하는, 라이프 사이클 관리 클래스이다. */

class CustomLifecycle : LifecycleOwner {
    private val mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    fun doOnResume() {
        mLifecycleRegistry.markState(Lifecycle.State.RESUMED)
    }

    fun doOnStarted() {
        mLifecycleRegistry.markState(Lifecycle.State.STARTED)
    }
    fun doOnDestroy(){
        mLifecycleRegistry.markState(Lifecycle.State.DESTROYED)
    }
    fun doOnCreated(){
        mLifecycleRegistry.markState(Lifecycle.State.CREATED)
    }
    fun doOnInitialized(){
        mLifecycleRegistry.markState(Lifecycle.State.INITIALIZED)
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    fun currentState(): Lifecycle.State {
        return mLifecycleRegistry.currentState
    }

    init {
        mLifecycleRegistry.markState(Lifecycle.State.CREATED)
    }
}