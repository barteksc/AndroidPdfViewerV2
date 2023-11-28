package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.PointF

// todo : use enum
data class Annotation(val type: String, val rectCorners: List<PointF>) {
}

fun Annotation.toShape(pageHeight: Int): Shape {

    // rect's corners mapped to image space
    val mappedPoints = listOf<PointF>(
        rectCorners[0].convertCoordinatesFrom(pageHeight),
        rectCorners[1].convertCoordinatesFrom(pageHeight),
        rectCorners[2].convertCoordinatesFrom(pageHeight),
        rectCorners[3].convertCoordinatesFrom(pageHeight)
    )

    // shape's corner points
    val shapePoints = listOf<Point>(
        Point(x = mappedPoints[0].x, y = mappedPoints[0].y),
        Point(x = mappedPoints[1].x, y = mappedPoints[1].y),
        Point(x = mappedPoints[2].x, y = mappedPoints[2].y),
        Point(x = mappedPoints[3].x, y = mappedPoints[3].y),
        )

    return Shape(
        type,
        shapePoints
    )
}
