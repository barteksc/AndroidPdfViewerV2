package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.PointF

/** convert image coordinates to pdf coordinates and vice versa, no scale */
fun PointF.convertCoordinatesFrom(pageHeight: Int) = PointF(x, pageHeight - y)

/** from the given diagonal bottom left and top right points, calculate the other 2 points */
fun generateRectangleCoordinates(bottomLeft: PointF, topRight: PointF): List<PointF> {
    val topLeft = PointF(bottomLeft.x, topRight.y)
    val bottomRight = PointF(topRight.x, bottomLeft.y)

    return listOf(topLeft, topRight, bottomRight, bottomLeft)
}