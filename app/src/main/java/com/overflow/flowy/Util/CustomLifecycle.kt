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

    fun doOnStart() {
        mLifecycleRegistry.markState(Lifecycle.State.STARTED)
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    init {
        mLifecycleRegistry.markState(Lifecycle.State.CREATED)
    }
}