package com.github.barteksc.pdfviewer.annotation.core


import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.github.barteksc.pdfviewer.util.logDebug
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfDictionary
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfString
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
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

    /** Extract the annotations for the given PDF file path and page number */
    @Throws(IOException::class)
    @JvmStatic
    fun getAnnotationsFrom(filePath: String, pageNum: Int): List<Annotation> {
        logDebug(TAG, "file path is: $filePath")

        if (filePath.isEmpty()) throw Exception("Input file is empty")
        val file = File(filePath)
        if (!file.exists()) throw Exception("Input file does not exist")

        // a list of the annotations
        val annotationsList = mutableListOf<Annotation>()

        // input stream from file
        val inputStream: InputStream = FileInputStream(file)

        // we create a reader for a certain document
        val reader = PdfReader(inputStream)

        // read annotations for the given page
        var page: PdfDictionary = reader.getPageN(pageNum)
        val annots: PdfArray? = page.getAsArray(PdfName.ANNOTS)
        if (annots == null) {
            logDebug(TAG, "Annotations array for page $pageNum is null")
        } else {
            logDebug(TAG, "Annotations array for page $pageNum: $annots")

            for (i in 0 until annots.size()) {
                val annotation: PdfDictionary = annots.getAsDict(i)

                // Extract extras
                // coordinates of 2 corners of the rectangle of the annotation
                val rectArray: PdfArray? = annotation.getAsArray(PdfName.RECT)
                // type of annotation
                val subtype: PdfName? = annotation.getAsName(PdfName.SUBTYPE)

                if (rectArray != null && rectArray.size() == 4) {
                    // bottom left corner's coordinates
                    val llx: Float = rectArray.getAsNumber(0).floatValue()
                    val lly: Float = rectArray.getAsNumber(1).floatValue()
                    // top right corner's coordinates
                    val urx: Float = rectArray.getAsNumber(2).floatValue()
                    val ury: Float = rectArray.getAsNumber(3).floatValue()

                    // from the extracted coordinates, calculate the rest, based on the annotation type
                    if (subtype == PdfName.SQUARE) {
                        // bottom left
                        val xBottomLeftPoint = llx
                        val yBottomLeftPoint = lly
                        val bottomLeftPoint = PointF(xBottomLeftPoint, yBottomLeftPoint)

                        // top right
                        val xTopRightPoint = urx
                        val yTopRightPoint = ury
                        val topRightPoint = PointF(xTopRightPoint, yTopRightPoint)

                        val squareAnnotationPoints =
                            generateRectangleCoordinates(bottomLeftPoint, topRightPoint)

                        val squareAnnotation = Annotation("RECTANGLE", squareAnnotationPoints)
                        annotationsList.add(squareAnnotation)

                        logDebug(TAG, "Annotation is square")
                        logDebug(TAG, "Annotation $i on page $pageNum- corner points:")
                        logDebug(TAG, "squareAnnotationPoints:$squareAnnotationPoints")

                    } else if (subtype == PdfName.CIRCLE) {
                        // todo: check circle's rect
                        logDebug(TAG, "Annotation is circle")

                    }

                }
            }
        }

        return annotationsList
    }

    private fun getShapesFor(
        pdfAnnotations: List<Annotation>,
        pageHeight: Int
    ): List<Shape> {
        // convert annotation to shape
        val shapes = pdfAnnotations.map { annotation ->
            return@map annotation.toRectangleShape(pageHeight)
            // todo: adjust for all shapes
        }
        return shapes
    }


    //  may need  @WorkerThread ?
    @JvmStatic
    @Throws(IOException::class)
    fun convertPdfAnnotationsToPngShapes(
        pdfPath: String, outputDirectory: String
    ): PdfToImageResultData {
        //saving result data here
        var shapes: List<Shape>
        lateinit var pngFile: File

        // create a new renderer
        val renderer = PdfRenderer(getSeekableFileDescriptor(pdfPath))

        renderer.use { renderer ->
            // assuming the pdf will have only 1 page (for now)
            val pageNum = 0
            val page = renderer.openPage(pageNum)

            // create a bitmap
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            // ensure white background
            bitmap.eraseColor(Color.WHITE)

            // render the page on the bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val pdfName = extractFileNameFromPath(pdfPath)

            // save the bitmap as a PNG file
            pngFile = saveBitmapAsPng(
                bitmap,
                outputDirectory,
                "PdfToImage-$pdfName-page-${pageNum + 1}.png"
            )

            // in OpenPdf lib, pages start from 1
            val pdfAnnotations = getAnnotationsFrom(pdfPath, pageNum = pageNum + 1)

            shapes = getShapesFor(pdfAnnotations, page.height)

            // close the page
            page.close()

        }

        return PdfToImageResultData(pngFile, shapes)
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