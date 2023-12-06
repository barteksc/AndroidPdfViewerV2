package com.github.barteksc.pdfviewer.annotation.core

abstract class Shape(@Transient open val type:String, @Transient open val corners: List<Point>)