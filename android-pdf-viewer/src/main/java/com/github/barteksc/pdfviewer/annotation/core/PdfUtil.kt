package com.github.barteksc.pdfviewer.annotation.core

import com.github.barteksc.pdfviewer.util.logDebug
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfDictionary
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfString
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

object PdfUtil {

    private val TAG: String = PdfUtil.javaClass.simpleName

    /** Extract PDF extras from the annotations for the given PDF file path */
    @Throws(IOException::class)
    @JvmStatic
    fun getAnnotationsExtra(filePath: String) {
        logDebug(TAG, "file path is: $filePath")

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
            // When using getAsDict(), getAsArray() and others we may get null if there was a problem
            val annots: PdfArray? = page.getAsArray(PdfName.ANNOTS)
            if (annots == null) {
                logDebug(TAG, "Annotations array for page $i is null")

            } else {
                logDebug(TAG, "Annotations array for page $i: $annots")

                for (j in 0 until annots.size()) {
                    val annotation: PdfDictionary = annots.getAsDict(j)

                    // Extract extras
                    val entityLinks: PdfArray? = annotation.getAsArray(PdfName("EntityLinks"))
                    if (entityLinks != null) {
                        for (k in 0 until entityLinks.size()) {
                            val entityLink: PdfDictionary = entityLinks.getAsDict(k)
                            val relKey: PdfString? = entityLink.getAsString(PdfName("relKey"))
                            val relType: PdfString? = entityLink.getAsString(PdfName("relType"))

                            logDebug(TAG, "Annotation $j on page $i - EntityLink $k:")
                            logDebug(TAG, "  relKey: $relKey")
                            logDebug(TAG, "  relType: $relType")
                        }
                    }
                }
            }
        }
    }
}