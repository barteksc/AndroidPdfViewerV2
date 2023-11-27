package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.PointF

/** convert image coordinates to pdf coordinates and vice versa, no scale */
fun PointF.convertCoordinatesFrom(pageWidth: Int, pageHeight: Int) = PointF(x, pageHeight - y)