package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.Annotation

data class Rectangle(
    override val type: String = "RECTANGLE",
    override val points: List<PointF>,
    val edges: List<Edge>
) : Shape(type, points)

fun getMockedRectangle(): List<Rectangle> {
    val corners = listOf(
        PointF(0.606155F, 2.65048F),
        PointF(60.60616F, 2.65048F),
        PointF(60.60616F, 62.65048F),
        PointF(0.606155F, 62.65048F)
    )

    val edges = listOf(
        Edge(corners[0], corners[1]),
        Edge(corners[1], corners[2]),
        Edge(corners[2], corners[3]),
        Edge(corners[3], corners[0])
    )

    val rectangle = Rectangle("RECTANGLE", corners, edges)

    return listOf(rectangle)
}

data class Edge (val start : PointF, val end : PointF)