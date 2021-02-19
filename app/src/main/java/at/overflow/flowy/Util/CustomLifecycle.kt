package at.overflow.flowy.Util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/** 카메라 x의 수명주기에 바인딩 할때 사용하는, 라이프 사이클 관리 클래스이다. */

class CustomLifecycle : LifecycleOwner {
    private val mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    fun doOnResume() {
        mLifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun doOnStarted() {
        mLifecycleRegistry.currentState = Lifecycle.State.STARTED
    }
    fun doOnDestroy(){
        mLifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
    fun doOnCreated(){
        mLifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    fun doOnInitialized(){
        mLifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    fun currentState(): Lifecycle.State {
        return mLifecycleRegistry.currentState
    }

    init {
        /** 라이프 사이클 생성시 현재 상태를 CREATED로 만든다. */
        mLifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
}