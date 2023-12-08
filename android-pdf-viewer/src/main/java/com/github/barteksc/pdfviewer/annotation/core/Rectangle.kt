package com.github.barteksc.pdfviewer.annotation.core

data class Rectangle(
    override val type: String = "RECTANGLE",
    override val corners: List<Point>,
    val edges: List<Edge>
) : Shape(type, corners)

fun Rectangle.toAnnotation(pageHeight: Int): Annotation {
    val points = corners.map { it.toPointF().convertCoordinatesFrom(pageHeight) }
    return Annotation(type = "SQUARE", rectCorners = points)
}

fun getMockedData(): List<Rectangle> {
    val corners = listOf(
        Point(0.606155F, 2.65048F, null),
        Point(60.60616F, 2.65048F, null),
        Point(60.60616F, 62.65048F, null),
        Point(0.606155F, 62.65048F, null)
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