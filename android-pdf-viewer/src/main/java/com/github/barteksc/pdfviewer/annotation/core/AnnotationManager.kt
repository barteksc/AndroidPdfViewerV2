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
import com.lowagie.text.Element
import com.lowagie.text.Image
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfAction
import com.lowagie.text.pdf.PdfAnnotation
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfGState
import com.lowagie.text.pdf.PdfImage
import com.lowagie.text.pdf.PdfLayer
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import java.awt.Color
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

    /** Draws a layer with a text annotation and a link annotation to the PDF document
     * WIP - text needs to be centered to the touched point,
     * the link annotation rect needs to be extended around the text  */
    @Throws(FileNotFoundException::class, IOException::class)
    @JvmStatic
    fun addTextAnnotation(
        context: Context,
        e: MotionEvent,
        currUri: Uri,
        pdfView: PDFView
    ): Boolean {
        // Page Starts From 1 In OpenPDF Core
        var page = pdfView.currentPage
        page++

        val filePath = UriUtils.getPathFromUri(context, currUri)

        if (filePath.isNullOrEmpty()) throw FileNotFoundException()
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException()

        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        var isAdded = false
        try {
            val inputStream: InputStream = FileInputStream(file)
            val reader = PdfReader(inputStream)
            val stamp = PdfStamper(reader, FileOutputStream(file))

            val pointF: PointF = pdfView.convertScreenPintsToPdfCoordinates(e)
            // The space to extend to
            val border = 30F

            // Create a layer for the annotations
            val annotationLayer = PdfLayer(referenceHash, stamp.writer)

            // text annotation over target page
            val over = stamp.getOverContent(page)
            if (over == null) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("GetUnderContent() is null")
            }

            val textAnnotation = PdfAnnotation.createFreeText(
                stamp.writer,
                Rectangle(
                    pointF.x - border,
                    pointF.y - border,
                    pointF.x + border,
                    pointF.y + border
                ),
                referenceHash, PdfContentByte(stamp.writer)
            )
            textAnnotation.apply {
                put(PdfName.OC, annotationLayer)
                put(PdfName.TYPE, PdfName.XOBJECT)
            }

            val linkAnnotation = PdfAnnotation(
                stamp.writer, pointF.x - 3 * border,
                pointF.y - 2 * border,
                pointF.x + 3 * border,
                pointF.y + border, PdfAction(referenceHash)
            )
            linkAnnotation.apply {
                put(PdfName.OC, annotationLayer)
                put(PdfName.TYPE, PdfName.XOBJECT)
            }

            // Add the annotations to the layer
            over.beginLayer(annotationLayer)
            over.apply {
                beginText()
                // Setting blue as default
                setRGBColorFill(0, 0, 255)
                setTextMatrix(30f, 30f)
                // create base font for text
                val bf =
                    BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED)
                setFontAndSize(bf, 32f)
                showTextAligned(
                    Element.ALIGN_LEFT,
                    "Hello VDS",
                    pointF.x - 2 * border,
                    pointF.y - border / 2,
                    0f
                )
                endText()
            }
            stamp.addAnnotation(textAnnotation, page)
            stamp.addAnnotation(linkAnnotation, page)
            over.endLayer()

            // Close the PdfStamper
            stamp.close()
            reader.close()

            isAdded = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return isAdded
    }

    /** Draws a layer with a circle annotation and a link annotation to the PDF document */
    @Throws(FileNotFoundException::class, IOException::class)
    @JvmStatic
    fun addCircleAnnotation(
        context: Context,
        e: MotionEvent,
        currUri: Uri,
        pdfView: PDFView
    ): Boolean {
        // Page Starts From 1 In OpenPDF Core
        var page = pdfView.currentPage
        page++

        val filePath = UriUtils.getPathFromUri(context, currUri)

        if (filePath.isNullOrEmpty()) throw FileNotFoundException()
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException()

        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        var isAdded = false
        try {
            val inputStream: InputStream = FileInputStream(file)
            val reader = PdfReader(inputStream)
            val stamp = PdfStamper(reader, FileOutputStream(file))

            val pointF: PointF = pdfView.convertScreenPintsToPdfCoordinates(e)
            val circleRadius = 30F

            // Create a layer for the annotations
            val annotationLayer = PdfLayer(referenceHash, stamp.writer)

            val circleAnnotation = PdfAnnotation.createSquareCircle(
                stamp.writer,
                Rectangle(
                    pointF.x - circleRadius,
                    pointF.y - circleRadius,
                    pointF.x + circleRadius,
                    pointF.y + circleRadius
                ),
                referenceHash,
                false
            )
            circleAnnotation.apply {
                setColor(Color.BLUE)
                put(PdfName.OC, annotationLayer)
                put(PdfName.TYPE, PdfName.XOBJECT)
            }

            val linkAnnotation = PdfAnnotation(
                stamp.writer, pointF.x - circleRadius,
                pointF.y - circleRadius,
                pointF.x + circleRadius,
                pointF.y + circleRadius, PdfAction(referenceHash)
            )
            linkAnnotation.apply {
                put(PdfName.OC, annotationLayer)
                put(PdfName.TYPE, PdfName.XOBJECT)
            }

            // add annotation into target page
            val over = stamp.getOverContent(page)
            if (over == null) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("GetUnderContent() is null")
            }

            // Add the annotations to the layer
            over.beginLayer(annotationLayer)
            stamp.addAnnotation(circleAnnotation, page)
            stamp.addAnnotation(linkAnnotation, page)
            over.endLayer()

            // Close the PdfStamper
            stamp.close()
            reader.close()

            isAdded = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return isAdded
    }

    /** Draws a layer with a rectangle annotation and a link annotation to the PDF document */
    @Throws(FileNotFoundException::class, IOException::class)
    @JvmStatic
    fun addRectAnnotation(
        context: Context,
        e: MotionEvent,
        currUri: Uri,
        pdfView: PDFView
    ): Boolean {
        // Page Starts From 1 In OpenPDF Core
        var page = pdfView.currentPage
        page++

        val filePath = UriUtils.getPathFromUri(context, currUri)

        if (filePath.isNullOrEmpty()) throw FileNotFoundException()
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException()

        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        var isAdded = false
        try {
            val inputStream: InputStream = FileInputStream(file)
            val reader = PdfReader(inputStream)
            val stamp = PdfStamper(reader, FileOutputStream(file))

            val pointF: PointF = pdfView.convertScreenPintsToPdfCoordinates(e)
            // The space to extend to
            val border = 30F

            // Create a layer for the annotations
            val annotationLayer = PdfLayer(referenceHash, stamp.writer)

            val rectAnnotation = PdfAnnotation.createSquareCircle(
                stamp.writer,
                Rectangle(
                    pointF.x - border,
                    pointF.y - border,
                    pointF.x + border,
                    pointF.y + border
                ),
                referenceHash,
                true
            )
            rectAnnotation.apply {
                setColor(Color.BLUE)
                put(PdfName.OC, annotationLayer)
            }

            val linkAnnotation = PdfAnnotation(
                stamp.writer, pointF.x - border,
                pointF.y - border,
                pointF.x + border,
                pointF.y + border, PdfAction(referenceHash)
            )
            linkAnnotation.apply {
                put(PdfName.OC, annotationLayer)
                put(PdfName.TYPE, PdfName.XOBJECT)
            }

            // add annotation into target page
            val over = stamp.getOverContent(page)
            if (over == null) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("GetUnderContent() is null")
            }

            // Add the annotations to the layer
            over.beginLayer(annotationLayer)
            stamp.addAnnotation(rectAnnotation, page)
            stamp.addAnnotation(linkAnnotation, page)
            over.endLayer()


            // Close the PdfStamper
            stamp.close()
            reader.close()


            isAdded = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return isAdded
    }

    /** Draws a layer with a rectangle annotation to a PDF document with 1 page */
    @Throws(FileNotFoundException::class, IOException::class)
    @JvmStatic
    fun addRectAnnotation(
        rectCorners: List<PointF>,
        file:File,
    ): Boolean {
        // PDFs wil1 have 1 page (for now)
        val page = 1

        if (!file.exists()) throw FileNotFoundException()

        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        var isAdded = false
        try {
            val inputStream: InputStream = FileInputStream(file)
            val reader = PdfReader(inputStream)
            val stamp = PdfStamper(reader, FileOutputStream(file))

            // Create a layer for the annotation
            val annotationLayer = PdfLayer(referenceHash, stamp.writer)

            // bottom left corner
            val lowerLeftX = rectCorners[3].x
            val lowerLeftY = rectCorners[3].y

            // topRightCorner
            val upperRightX = rectCorners[1].x
            val upperRightY = rectCorners[1].y

            val rectAnnotation = PdfAnnotation.createSquareCircle(
                stamp.writer,
                Rectangle(
                  lowerLeftX,
                     lowerLeftY,
                    upperRightX,
                    upperRightY
                ),
                referenceHash,
                true
            )
            rectAnnotation.apply {
                setColor(Color.BLUE)
                put(PdfName.OC, annotationLayer)
            }

            // add annotation into target page
            val over = stamp.getOverContent(page)
            if (over == null) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("GetUnderContent() is null")
            }

            // Add the annotation to the layer
            over.beginLayer(annotationLayer)
            stamp.addAnnotation(rectAnnotation, page)
            over.endLayer()

            // Close the PdfStamper
            stamp.close()
            reader.close()

            isAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isAdded
    }


    /** Draws a layer with a line and a link annotation to the PDF document */
    @Throws(FileNotFoundException::class, IOException::class)
    @JvmStatic
    fun addLineAnnotation(
        context: Context,
        e: MotionEvent,
        currUri: Uri,
        pdfView: PDFView
    ): Boolean {
        // Page Starts From 1 In OpenPDF Core
        var page = pdfView.currentPage
        page++

        val filePath = UriUtils.getPathFromUri(context, currUri)

        if (filePath.isNullOrEmpty()) throw FileNotFoundException()
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException()

        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        var isAdded = false
        try {
            val inputStream: InputStream = FileInputStream(file)
            val reader = PdfReader(inputStream)
            val stamp = PdfStamper(reader, FileOutputStream(file))

            val pointF: PointF = pdfView.convertScreenPintsToPdfCoordinates(e)
            // The space to extend to
            val border = 30F

            // Create a layer for the annotations
            val annotationLayer = PdfLayer(referenceHash, stamp.writer)

            val linkAnnotation = PdfAnnotation(
                stamp.writer, pointF.x - border,
                pointF.y - border,
                pointF.x + border,
                pointF.y + border, PdfAction(referenceHash)
            )
            linkAnnotation.apply {
                put(PdfName.OC, annotationLayer)
                put(PdfName.TYPE, PdfName.XOBJECT)
            }

            // add annotation into target page
            val over = stamp.getOverContent(page)
            if (over == null) {
                stamp.close()
                reader.close()
                throw java.lang.Exception("GetUnderContent() is null")
            }

            // Add the annotations to the layer
            over.beginLayer(annotationLayer)
            over.apply {
                // Setting blue as default
                setRGBColorStroke(0, 0, 255)
                setLineWidth(1f)

                // line coordinates
                val l1_x1 = pointF.x - border
                val l1_y1 = pointF.y
                val l1_x2 = pointF.x + border
                val l1_y2 = pointF.y

                // prepare line
                moveTo(l1_x1, l1_y1)
                lineTo(l1_x2, l1_y2)

                //draw lines
                over.stroke()
            }

            stamp.addAnnotation(linkAnnotation, page)
            over.endLayer()

            // Close the PdfStamper
            stamp.close()
            reader.close()

            isAdded = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return isAdded
    }

    @JvmStatic
    fun addLines(
        context: Context,
        currUri: Uri,
        pdfView: PDFView,
    ): Boolean {
        // Hint: Page Starts From --> 1 In OpenPdf Core
        var page = pdfView.currentPage
        page++

        val filePath = UriUtils.getPathFromUri(context, currUri)

        // get file and FileOutputStream
        if (filePath.isNullOrEmpty()) throw FileNotFoundException()
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException()

        var isAdded = false
        try {
            // input stream from file
            val inputStream: InputStream = FileInputStream(file)

            // we create a reader for a certain document
            val reader = PdfReader(inputStream)

            // we create a stamper that will copy the document to a new file
            val stamp = PdfStamper(reader, FileOutputStream(file))

            // content over the existing page
            val over: PdfContentByte = stamp.getOverContent(page)

            // default blue
            over.setRGBColorStroke(0, 0, 255)

            over.setLineWidth(1f)


            // drawing lines on the provided "PDF-Test1.pdf"
            // line 1 coordinates - line above letter B
            val l1_x1 = 512f
            val l1_y1 = 105f
            val l1_x2 = 512f
            val l1_y2 = 380f

            // prepare line 1
            over.moveTo(l1_x1, l1_y1)
            over.lineTo(l1_x2, l1_y2)

            // line 2 coordinates - line from D to B
            val l2_x1 = 84f
            val l2_y1 = 98f
            val l2_x2 = 504f
            val l2_y2 = 98f

            // prepare line 2
            over.moveTo(l2_x1, l2_y1)
            over.lineTo(l2_x2, l2_y2)

            // line 3 coordinates - from D to above B
            val l3_x1 = 84f
            val l3_y1 = 104f
            val l3_x2 = 508f
            val l3_y2 = 380f

            // prepare line 3
            over.moveTo(l3_x1, l3_y1)
            over.lineTo(l3_x2, l3_y2)

            //draw lines
            over.stroke()

            // todo: Add reference hash
            // add as layer
            val wmLayer = PdfLayer("lines-referenceHash", stamp.writer)
            over.beginLayer(wmLayer)
            over.endLayer()

            // closing PdfStamper will generate the new PDF file
            stamp.close()
            over.sanityCheck()

            isAdded = true
        } catch (de: java.lang.Exception) {
            de.printStackTrace()
        }
        return isAdded
    }

    /** Removes annotations from the PDF document */
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
    fun removeOCG(filePath: String?, annotationHash: String?): Boolean {
        if (filePath.isNullOrEmpty()) throw java.lang.Exception("Input file is empty")
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

    /** Adds default image marker to the PDF document */
    @Throws(IOException::class)
    @JvmStatic
    fun addImageAnnotation(
        context: Context,
        e: MotionEvent?,
        currUri: Uri,
        pdfView: PDFView
    ): Boolean {
        // Generate reference hash
        val referenceHash = StringBuilder()
            .append(PublicValue.KEY_REFERENCE_HASH)
            .append(UUID.randomUUID().toString())
            .toString()

        // Get image marker
        val OCGCover = getByteFromDrawable(context, R.drawable.annotation_marker)
        val filePath = UriUtils.getPathFromUri(context, currUri)
        val pointF: PointF = pdfView.convertScreenPintsToPdfCoordinates(e)

        var isAdded = false
        try {
            isAdded =
                addOCG(
                    pointF,
                    filePath,
                    pdfView.currentPage,
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

    /** Adds image OCG */
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
            img.transparency = intArrayOf(0x00, 0x10)
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
}