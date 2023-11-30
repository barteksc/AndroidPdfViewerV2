package com.github.barteksc.pdfviewer.annotation.core

data class Rectangle(
    override val type: String = "RECTANGLE",
    override val corners: List<Point>,
    val edges: List<Edge>
) : Shape(type, corners)