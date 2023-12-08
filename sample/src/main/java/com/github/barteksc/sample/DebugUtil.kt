package com.github.barteksc.sample

import android.content.Context
import android.util.Log
import android.widget.Toast


internal fun Context.toast(text:String) {
    Toast.makeText(this, text,Toast.LENGTH_LONG ).show()
}

internal fun logDebug(tag:String = "" , text:String){
    Log.d(tag, text)
}

internal fun logInfo(tag:String = "" , text:String){
    Log.i(tag, text)
}

internal fun logError(tag:String = "" , text:String){
    Log.e(tag, text)
}