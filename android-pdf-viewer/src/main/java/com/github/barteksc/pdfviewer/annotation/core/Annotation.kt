package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.shapes.Rectangle
import com.github.barteksc.pdfviewer.annotation.core.shapes.convertCoordinatesFrom
import com.github.barteksc.pdfviewer.annotation.core.shapes.generateRectangleEdges

// todo : use enum
data class Annotation(val type: String, val rectCorners: List<PointF>) {
}

fun Annotation.toRectangleShape(pageHeight: Int): Rectangle {

    // rect's corners mapped to image space
    val mappedPoints = listOf(
        rectCorners[0].convertCoordinatesFrom(pageHeight),
        rectCorners[1].convertCoordinatesFrom(pageHeight),
        rectCorners[2].convertCoordinatesFrom(pageHeight),
        rectCorners[3].convertCoordinatesFrom(pageHeight)
    )

    // rectangle shape's edges
    val rectangleShapeEdges = mappedPoints.generateRectangleEdges()

    return Rectangle(corners = mappedPoints, edges = rectangleShapeEdges)
}
