package com.github.barteksc.pdfviewer.annotation.core

import java.io.File

data class PdfToImageResultData(val imageFile: File, val shapes: List<Rectangle>) {
}