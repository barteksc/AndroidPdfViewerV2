package com.github.barteksc.pdfviewer.annotation.core

import java.io.File

data class PdfToImageResultData(val unmodifiedPdfFile: File, val annotatedPdfFile: File,val imageFile: File, val pageHeight:Int, val shapes: List<Rectangle>)

