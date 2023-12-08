package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.PointF

data class Point(val x: Float, val y: Float, val z: Float? = null)

fun Point.toPointF() = PointF(x, y)