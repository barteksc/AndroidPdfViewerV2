package com.github.barteksc.pdfviewer.annotation.core

abstract class Shape(open val type:String, @Transient open val corners: List<Point>)