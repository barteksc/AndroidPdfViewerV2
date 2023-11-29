package com.github.barteksc.pdfviewer.annotation.core

// todo: inherit from shape
data class Rectangle(val type: String = "RECTANGLE", val corners: List<Point>, val edges: List<Edge>)
