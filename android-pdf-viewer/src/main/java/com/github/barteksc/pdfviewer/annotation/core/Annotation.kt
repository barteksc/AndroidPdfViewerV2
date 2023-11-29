package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.PointF

// todo : use enum
data class Annotation(val type: String, val rectCorners: List<PointF>) {
}

fun Annotation.toRectangleShape(pageHeight: Int): Rectangle {

    // rect's corners mapped to image space
    val mappedPoints = listOf<PointF>(
        rectCorners[0].convertCoordinatesFrom(pageHeight),
        rectCorners[1].convertCoordinatesFrom(pageHeight),
        rectCorners[2].convertCoordinatesFrom(pageHeight),
        rectCorners[3].convertCoordinatesFrom(pageHeight)
    )

    // rectangle shape's corner points
    val rectangleShapePoints = mappedPoints.map { it.toPoint() }

    // rectangle shape's edges
    val rectangleShapeEdges = rectangleShapePoints.generateRectangleEdges()

    return Rectangle(corners = rectangleShapePoints, edges = rectangleShapeEdges)
}
