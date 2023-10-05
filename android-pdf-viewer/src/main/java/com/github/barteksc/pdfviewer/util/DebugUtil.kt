package com.github.barteksc.pdfviewer.util

import android.content.Context
import android.util.Log
import android.widget.Toast


fun Context.toast(text:String) {
    Toast.makeText(this, text,Toast.LENGTH_LONG ).show()
}

fun logDebug(tag:String = "" , text:String){
    Log.d(tag, text)
}

fun logError(tag:String = "" , text:String){
    Log.e(tag, text)
}