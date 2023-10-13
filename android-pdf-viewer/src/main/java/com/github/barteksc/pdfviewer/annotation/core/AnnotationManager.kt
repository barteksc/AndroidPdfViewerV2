package com.github.barteksc.pdfviewer.annotation.core

import android.content.Context
import android.graphics.PointF
import android.net.Uri
import android.view.MotionEvent
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.annotation.ocg.OCGRemover
import com.github.barteksc.pdfviewer.util.PublicValue
import com.github.barteksc.pdfviewer.util.UriUtils
import com.github.barteksc.pdfviewer.util.logInfo
import com.lowagie.text.Annotation
import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Element
import com.lowagie.text.Image
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfGState
import com.lowagie.text.pdf.PdfImage
import com.lowagie.text.pdf.PdfLayer
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import com.lowagie.text.pdf.PdfWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID


object AnnotationManager {
    val TAG = AnnotationManager.javaClass.simpleName
    // TODO: extract file check in a function

    /** Adds text to the PDF document. Will need reference hash for identification */
    @Throws(FileNotFoundException::class, IOException::class)
    @JvmStatic
    fun addTextAnnotation(
        context: Context,
        e: MotionEvent,
        currUri: Uri,
        pdfView: PDFView,
        page: Int
    ): Boolean {
        // Hint: Page Starts From --> 1 In OpenPdf Core
        var page = page
        page++

        val filePath = UriUtils.getPathFromUri(context, currUri)

        // get file and FileOutputStream
        if (filePath.isNullOrEmpty()) throw FileNotFoundException()
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException()

        var isAdded = false
        try {

            // inout stream from file
            val inputStream: InputStream = FileInputStream(file)

            // we create a reader for a certain document
            val reader = PdfReader(inputStream)

            // we create a stamper that will copy the document to a new file
            val stamp = PdfStamper(reader, FileOutputStream(file))

            // create base font for text
            val bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED)

            // text over the existing page
            val over: PdfContentByte = stamp.getOverContent(page)
            over.beginText()
            over.setFontAndSize(bf, 18f)

            // Setting blue as default
            over.setRGBColorFill(0, 0, 255)
            over.setTextMatrix(30f, 30f)

            over.setFontAndSize(bf, 32f)

            val pointF: PointF = pdfView.convertScreenPintsToPdfCoordinates(e)

            over.showTextAligned(Element.ALIGN_LEFT, "Hello VDS", pointF.x, pointF.y, 0f)
            over.endText()

            // closing PdfStamper will generate the new PDF file
            stamp.close()

            isAdded = true
        } catch (de: java.lang.Exception) {
            de.printStackTrace()
        }
        return isAdded
    }

    /** Adds default image marker to the PDF document */
    @Throws(IOException::class)
    @JvmStatic
    fun addImageAnnotation(
        context: Context,
        e: MotionEvent?,
        currUri: Uri,
        pdfView: PDFView,
        OCGCover: ByteArray
    ): Boolean {

        // Generate reference hash
        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        // Get image marker
//        val OCGCover = getByteFromDrawableJ(context, R.drawable.annotation_circle)
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

    /** Removes annotation from the PDF document. Currently removes only image annotations */
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

    /** Replaces the current document with document that contains a dashed line - WIP
     *  TODO: Add the lines to the original document, instead of replacing it, use MotionEvent's coordinates, add id/reference hash
     *  */

    @Throws(java.lang.Exception::class)
    @JvmStatic
    fun addLineAnnotation(
        context: Context,
        currUri: Uri,
    ): Boolean {
        val filePath = UriUtils.getPathFromUri(context, currUri)

        if (filePath.isNullOrEmpty()) throw java.lang.Exception("Input file is empty")
        val file = File(filePath)
        if (!file.exists()) throw java.lang.Exception("Input file does not exists")
        val fileOutputStream = FileOutputStream(file, true)

        // step 1: creation of a document-object
        val document = Document()
        return try {

            // step 2: creation of the writer
            val writer = PdfWriter.getInstance(document, fileOutputStream)

            // step 3: we open the document
            document.open()

            // step 4: we grab the ContentByte and do some stuff with it
            val cb = writer.directContent
            cb.setRGBColorStroke(0, 0, 255)

            // first we draw some lines to be able to visualize the text alignment functions
            cb.setLineWidth(0f)
            cb.moveTo(250f, 500f)
            cb.lineTo(255f, 500f)
            cb.moveTo(260f, 500f)
            cb.lineTo(265f, 500f)
            cb.moveTo(270f, 500f)
            cb.lineTo(275f, 500f)
            cb.stroke()

            cb.sanityCheck()
            true
        } catch (de: DocumentException) {
            System.err.println(de.message)
            false
        } catch (de: IOException) {
            System.err.println(de.message)
            false
        } finally {
            // step 5: we close the document
            document.close()
        }
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