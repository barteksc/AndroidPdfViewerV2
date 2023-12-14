package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.Annotation

abstract class Shape(@Transient open val type:String, @Transient open val points: List<PointF>)

fun Shape.toAnnotation(pageHeight: Int) : Annotation{
    return when (this){
        is Rectangle -> toRectangleAnnotation(pageHeight)
        is Circle-> toCircleAnnotation(pageHeight)
        else -> throw Exception ("")
    }
}

fun Shape.toRectangleAnnotation(pageHeight: Int): Annotation {
    val points = points.map { it.convertCoordinatesFrom(pageHeight) }
    return Annotation(
        type = "SQUARE",
        points = points
    )
}

fun Shape.toCircleAnnotation(pageHeight: Int): Annotation {
    val points = points.map { it.convertCoordinatesFrom(pageHeight) }
    return Annotation(type = "CIRCLE", points = points)
}
