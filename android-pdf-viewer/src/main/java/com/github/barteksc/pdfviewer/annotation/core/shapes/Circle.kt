package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.Annotation

data class Circle(
    override val type: String = "CIRCLE",
    override val points: List<PointF> = emptyList(),
) : Shape(type, points)

fun getMockedCircle(): List<Circle> {
    val corners = listOf(
        PointF(160.606155F, 222.65048F),
        PointF(260.60616F, 322.65048F),
    )

    val rectangle = Circle("CIRCLE", corners)

    return listOf(rectangle)
}