package com.mx.common

import android.util.Log

object LogU {
    private val TAG = "Global"
    fun i(txt:String){
        Log.e(TAG, "${Thread.currentThread().name}: $txt")
        println("$txt")
    }
    fun d(txt:String){
        Log.d(TAG,txt)
    }
    fun e(txt:String){
        Log.e(TAG,txt)
    }
    fun w(txt:String){
        Log.w(TAG,txt)
    }
}