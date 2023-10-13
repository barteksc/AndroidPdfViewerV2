package com.github.barteksc.pdfviewer.annotation.core

import android.content.Context
import android.graphics.PointF
import android.net.Uri
import android.view.MotionEvent
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.R
import com.github.barteksc.pdfviewer.annotation.ocg.OCGRemover
import com.github.barteksc.pdfviewer.util.PublicFunction.Companion.getByteFromDrawable
import com.github.barteksc.pdfviewer.util.PublicValue
import com.github.barteksc.pdfviewer.util.UriUtils
import com.github.barteksc.pdfviewer.util.logInfo
import com.lowagie.text.Annotation
import com.lowagie.text.Image
import com.lowagie.text.pdf.PdfGState
import com.lowagie.text.pdf.PdfImage
import com.lowagie.text.pdf.PdfLayer
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

object AnnotationManager {
    val TAG = AnnotationManager.javaClass.simpleName

    @Throws(IOException::class)
    @JvmStatic
    fun addAnnotation(context: Context, e: MotionEvent?, currUri: Uri, pdfView: PDFView): Boolean {

        // Generate reference hash
        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        // Get image marker
        val OCGCover = getByteFromDrawable(context, R.drawable.annotation_circle)
        val filePath = UriUtils.getPathFromUri(context, currUri)
        val pointF: PointF = pdfView.convertScreenPintsToPdfCoordinates(e)

        var isAdded = false
        try {
            isAdded =
                addOCG(
                    pointF,
                    filePath,
                    pdfView.getCurrentPage(),
                    referenceHash,
                    OCGCover,
                    0f,
                    0f
                )
            logInfo(TAG, "addAnnotation: isAdded = $isAdded")
        } catch (e1: Exception) {
            e1.printStackTrace()
        }
        return isAdded
    }

    @Throws(IOException::class)
    @JvmStatic
    fun removeAnnotation(context: Context, currUri: Uri, referenceHash: String?): Boolean {
        var isRemoved = false
        try {
            val filePath = UriUtils.getPathFromUri(context, currUri)
            isRemoved = removeOCG(filePath, referenceHash)
            logInfo(TAG, "removeAnnotation: isRemoved = $isRemoved")
        } catch (e1: java.lang.Exception) {
            e1.printStackTrace()
        }
        return isRemoved
    }

    @Throws(java.lang.Exception::class)
    fun addOCG(
        pointF: PointF,
        filePath: String?,
        currPage: Int,
        referenceHash: String?,
        OCGCover: ByteArray?,
        OCGWidth: Float,
        OCGHeight: Float
    ): Boolean {

        // Hint: OCG -> optional content group
        // Hint: Page Starts From --> 1 In OpenPdf Core
        var currPage = currPage

        var OCGWidth = OCGWidth
        var OCGHeight = OCGHeight
        currPage++

        // OCG width & height
        if (OCGWidth == 0f || OCGHeight == 0f) {
            OCGWidth = PublicValue.DEFAULT_OCG_WIDTH
            OCGHeight = PublicValue.DEFAULT_OCG_HEIGHT
        }

        // get file and FileOutputStream
        if (filePath.isNullOrEmpty()) throw java.lang.Exception("Input file is empty")
        val file = File(filePath)
        if (!file.exists()) throw java.lang.Exception("Input file does not exists")
        return try {

            // inout stream from file
            val inputStream: InputStream = FileInputStream(file)

            // we create a reader for a certain document
            val reader = PdfReader(inputStream)

            // we create a stamper that will copy the document to a new file
            val stamp = PdfStamper(reader, FileOutputStream(file))

            // get watermark icon
            val img = Image.getInstance(OCGCover)
            img.annotation = Annotation(0f, 0f, 0f, 0f, referenceHash)
            img.scaleAbsolute(OCGWidth, OCGHeight)
            img.setAbsolutePosition(pointF.x, pointF.y)
            val stream = PdfImage(img, referenceHash, null)
            stream.put(PdfName(PublicValue.KEY_SPECIAL_ID), PdfName(referenceHash))
            val ref = stamp.writer.addToBody(stream)
            img.directReference = ref.indirectReference

            // add as layer
            val wmLayer = PdfLayer(referenceHash, stamp.writer)

            // prepare transparency
            val transparent = PdfGState()
            transparent.setAlphaIsShape(false)

            // get page file number count
            if (reader.numberOfPages < currPage) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("Page index is out of pdf file page numbers")
            }

            // add annotation into target page
            val over = stamp.getOverContent(currPage)
            if (over == null) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("GetUnderContent() is null")
            }

            // add as layer
            over.beginLayer(wmLayer)
            over.setGState(transparent) // set block transparency properties
            over.addImage(img)
            over.endLayer()

            // closing PdfStamper will generate the new PDF file
            stamp.close()

            // close reader
            reader.close()

            // finish method
            true
        } catch (ex: java.lang.Exception) {
            throw java.lang.Exception(ex.message)
        }
    }

    @Throws(java.lang.Exception::class)
    fun removeOCG(filePath: String?, annotationHash: String?): Boolean {

        // get file and FileOutputStream
        if (filePath == null || filePath.isEmpty()) throw java.lang.Exception("Input file is empty")
        val file = File(filePath)
        if (!file.exists()) throw java.lang.Exception("Input file does not exists")
        return try {

            // inout stream from file
            val inputStream: InputStream = FileInputStream(file)

            // we create a reader for a certain document
            val pdfReader = PdfReader(inputStream)

            // we create a stamper that will copy the document to a new file
            val pdfStamper = PdfStamper(pdfReader, FileOutputStream(file))

            // remove target object
            val ocgRemover = OCGRemover()
            ocgRemover.removeLayers(pdfReader, annotationHash)

            // closing PdfStamper will generate the new PDF file
            pdfStamper.close()

            // close reader
            pdfReader.close()

            // finish method
            true
        } catch (e: java.lang.Exception) {
            throw java.lang.Exception(e.message)
        }
    }
}