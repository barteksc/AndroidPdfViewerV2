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


import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.annotation.WorkerThread
import java.io.FileNotFoundException
import java.io.FileOutputStream

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

    @JvmStatic
    @WorkerThread
    @Throws(IOException::class)
    fun convertPdfToPngFiles(
        pdfPath: String, outputDirectory: String
    ): List<File> {
        val pngFiles = mutableListOf<File>()

        // create a new renderer
        val renderer = PdfRenderer(getSeekableFileDescriptor(pdfPath))

        renderer.use { renderer ->
            // render all pages
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)

                // create a bitmap
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                // ensure white background
                bitmap.eraseColor(Color.WHITE)

                // render the page on the bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pdfName = extractFileNameFromPath(pdfPath)

                // save the bitmap as a PNG file
                val pngFile = saveBitmapAsPng(bitmap, outputDirectory, "PdfToImage-$pdfName-page-${i+1}.png")
                pngFiles.add(pngFile)

                // close the page
                page.close()
            }
        }

        return pngFiles
    }

    private fun getSeekableFileDescriptor(pdfPath: String): ParcelFileDescriptor {
        var fd: ParcelFileDescriptor? = null
        try {
            fd = ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return fd!!
    }

    private fun saveBitmapAsPng(bitmap: Bitmap, directory: String, fileName: String): File {
        val file = File(directory, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    private fun extractFileNameFromPath(filePath: String): String {
        val file = File(filePath)
        return file.nameWithoutExtension
    }

}