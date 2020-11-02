package com.overflow.flowy.Util

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics

class DeviceCheck {

    fun isTabletDevice(activityContext : Context): Boolean {

        val deviceLarge = ((activityContext.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)
        val metrics = activityContext.resources.displayMetrics

        //Tablet
        if (deviceLarge) {
            if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT){
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM){
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_TV){
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_HIGH){
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_280){
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) {
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_400) {
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_XXHIGH) {
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_560) {
                return true
            }else if(metrics.densityDpi == DisplayMetrics.DENSITY_XXXHIGH) {
                return true
            }
        }
        else{
            //Mobile
        }
        return false;
    }

}