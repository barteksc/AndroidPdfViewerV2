package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF

abstract class Shape(@Transient open val type:String, @Transient open val corners: List<PointF>)