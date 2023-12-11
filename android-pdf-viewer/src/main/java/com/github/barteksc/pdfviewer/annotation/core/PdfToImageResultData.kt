package com.github.barteksc.pdfviewer.annotation.core

import java.io.File

data class PdfToImageResultData(val originalPdfFile: File,val imageFile: File, val pageHeight:Int, val shapes: List<Rectangle>)