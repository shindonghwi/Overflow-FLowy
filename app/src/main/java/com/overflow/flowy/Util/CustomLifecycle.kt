package com.overflow.flowy.Util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

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