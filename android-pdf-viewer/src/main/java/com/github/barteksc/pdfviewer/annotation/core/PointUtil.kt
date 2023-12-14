package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.PointF

/** Convert image coordinates to pdf coordinates and vice versa, no scale */
fun PointF.convertCoordinatesFrom(pageHeight: Int) = PointF(x, pageHeight - y)

/** From the given diagonal bottom left and top right points, calculate the other 2 points */
fun generateRectangleCoordinates(bottomLeft: PointF, topRight: PointF): List<PointF> {
    val topLeft = PointF(bottomLeft.x, topRight.y)
    val bottomRight = PointF(topRight.x, bottomLeft.y)

    return listOf(topLeft, topRight, bottomRight, bottomLeft)
}

fun List<PointF>.generateRectangleEdges() : List<Edge> {
    val edgeTopHorizontal = Edge (this[0], this[1])
    val edgeRightVertical = Edge (this[1], this[2])
    val edgeBottomHorizontal = Edge (this[2], this[3])
    val edgeLeftVertical = Edge (this[3], this[0])

    return listOf(edgeTopHorizontal, edgeRightVertical, edgeBottomHorizontal, edgeLeftVertical)
}
