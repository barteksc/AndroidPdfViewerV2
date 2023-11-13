package com.github.barteksc.pdfviewer.annotation.core

import android.content.Context
import android.net.Uri
import com.github.barteksc.pdfviewer.annotation.ocg.OCGParser
import com.github.barteksc.pdfviewer.annotation.ocg.OCGRemover
import com.github.barteksc.pdfviewer.util.UriUtils
import com.github.barteksc.pdfviewer.util.logDebug
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfDictionary
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

object PdfUtil {

    private val TAG: String = AnnotationManager.javaClass.simpleName

    @Throws(IOException::class)
    @JvmStatic
    fun getAnnotationInfo(filePath: String) {

        if (filePath.isEmpty()) throw java.lang.Exception("Input file is empty")
        val file = File(filePath)
        if (!file.exists()) throw java.lang.Exception("Input file does not exists")

        // input stream from file
        val inputStream: InputStream = FileInputStream(file)

        // we create a reader for a certain document
        val reader = PdfReader(inputStream)

        val n = reader.numberOfPages

        var page: PdfDictionary
        for (i in 1..n) {
            page = reader.getPageN(i)
            val annots: PdfArray = page.getAsArray(PdfName.ANNOTS)
            if (annots == null) {
                logDebug(TAG, "Annotations array for page $i is null")

            } else {
                logDebug(TAG, "Annotations array for page $i: $annots")
            }
        }
    }
}