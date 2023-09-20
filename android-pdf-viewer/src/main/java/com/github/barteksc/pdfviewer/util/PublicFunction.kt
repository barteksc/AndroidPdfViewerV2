package com.github.barteksc.pdfviewer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

class PublicFunction {
    companion object {
        fun getByteFromDrawable(context: Context, resDrawable: Int): ByteArray {
            val bitmap = BitmapFactory.decodeResource(context.resources, resDrawable)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }
    }
}