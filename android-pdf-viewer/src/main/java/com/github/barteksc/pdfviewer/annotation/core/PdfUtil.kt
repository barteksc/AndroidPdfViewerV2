package com.github.barteksc.pdfviewer.annotation.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.github.barteksc.pdfviewer.annotation.core.shapes.Documentation
import com.github.barteksc.pdfviewer.annotation.core.shapes.Rectangle
import com.github.barteksc.pdfviewer.annotation.core.shapes.RectangleTypeAdapter
import com.github.barteksc.pdfviewer.annotation.core.shapes.Relations
import com.github.barteksc.pdfviewer.annotation.core.shapes.Shape
import com.github.barteksc.pdfviewer.annotation.core.shapes.generateRectangleCoordinates
import com.github.barteksc.pdfviewer.annotation.core.shapes.mapJsonStringToPdfShapes
import com.github.barteksc.pdfviewer.annotation.core.shapes.mapPdfShapesToJsonString
import com.github.barteksc.pdfviewer.annotation.core.shapes.toAnnotation
import com.github.barteksc.pdfviewer.util.logDebug
import com.github.salomonbrys.kotson.jsonNull
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lowagie.text.pdf.PdfArray
import com.lowagie.text.pdf.PdfDictionary
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfNumber
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfString
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.properties.Delegates

object PdfUtil {

    private val TAG: String = PdfUtil.javaClass.simpleName

    /** Extract PDF extras from the annotations for the given PDF file path */
    @Throws(IOException::class)
    @JvmStatic
    private fun getAnnotationsExtra(filePath: String) {
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

    /** Uses the passed PDF file to create a PNG image from the first page,
     *  maps the PDF annotations to shapes that will be saved as json string
     *  */
    @JvmStatic
    fun getPdfToImageResultData(
        pdfFilePath: String,
        outputDirectory: String
    ): PdfToImageResultData {
        // (PDF, annotations) -> (PNG, shapes)
        val resultData = convertPdfAnnotationsToPngShapes(pdfFilePath, outputDirectory)
        // Remove annotations from the PDF so we can draw shapes from MeasureLib on the same PDF
        AnnotationManager.removeAnnotationsFromPdf(pdfFilePath)
        return resultData
    }


    /** Maps shapes to annotations and draws them to the given PDF */
    @JvmStatic
    fun getResultPdf(pdfFile: File, pdfPageHeight: Int, jsonShapes: String) {
        val shapes = mapJsonStringToPdfShapes(jsonShapes)
        drawPngShapesToPdf(pdfFile, pdfPageHeight, shapes)
    }

    /** Extract the annotations for the given PDF file path and page number */
    @Throws(IOException::class)
    @JvmStatic
    private fun getAnnotationsFrom(filePath: String, pageNum: Int): List<Annotation> {
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
        val page: PdfDictionary = reader.getPageN(pageNum)
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

                        // Extract relations
                        val documentations = mutableListOf<Documentation>()
                        val relationsArray: PdfArray? = annotation.getAsArray(PdfName("relations"))
                        if (relationsArray != null) {
                            for (j in 0 until relationsArray.size()) {
                                val documentationDict: PdfDictionary = relationsArray.getAsDict(j)
                                val schemaId: PdfNumber? =
                                    documentationDict.getAsNumber(PdfName("schemaId"))
                                val documentId: PdfString? =
                                    documentationDict.getAsString(PdfName("documentId"))

                                if (schemaId != null && documentId != null) {
                                    logDebug(TAG, "Annotation $i - Relations $j:")
                                    logDebug(TAG, "  schemaId: $schemaId")
                                    logDebug(TAG, "  documentId: $documentId")

                                    documentations.add(
                                        Documentation(
                                            schemaId.intValue().toLong(),
                                            documentId.toString()
                                        )
                                    )
                                }

                            }
                        }
                        val squareAnnotation = Annotation(
                            "RECTANGLE",
                            squareAnnotationPoints,
                            relations = Relations(documentations)
                        )
                        annotationsList.add(squareAnnotation)

                        logDebug(TAG, "Annotation is square")
                        logDebug(TAG, "Annotation $i on page $pageNum - points:")
                        logDebug(TAG, "squareAnnotationPoints:$squareAnnotationPoints")

                    } else if (subtype == PdfName.CIRCLE) {
                        // bottom left
                        val xBottomLeftPoint = llx
                        val yBottomLeftPoint = lly
                        val bottomLeftPoint = PointF(xBottomLeftPoint, yBottomLeftPoint)

                        // top right
                        val xTopRightPoint = urx
                        val yTopRightPoint = ury
                        val topRightPoint = PointF(xTopRightPoint, yTopRightPoint)

                        val circleAnnotationPoints = listOf(bottomLeftPoint, topRightPoint)
                        val circleAnnotation = Annotation("CIRCLE", circleAnnotationPoints)
                        annotationsList.add(circleAnnotation)

                        logDebug(TAG, "Annotation is circle")
                        logDebug(TAG, "Annotation $i on page $pageNum - points:")
                        logDebug(TAG, "circleAnnotationPoints:$circleAnnotationPoints")
                    } else {
                        logDebug(TAG, "Annotation is not recognised")
                    }
                }
            }
        }

        return annotationsList
    }

    /** Map annotations to shapes,
     *  using the page height when converting between PDF space and image space*/
    private fun getShapesFor(
        pdfAnnotations: List<Annotation>,
        pageHeight: Int
    ): List<Shape> {
        // convert annotation to shape
        val shapes = pdfAnnotations.map { annotation ->
            when (annotation.type) {
                "RECTANGLE" -> return@map annotation.toRectangleShape(pageHeight)
                "CIRCLE" -> return@map annotation.toCircleShape(pageHeight)
                else -> throw Exception("Annotation is not recognised")
            }
            // todo: adjust for all shapes
        }
        return shapes
    }

    /** Use the passed PDF file path to map the PDF page to PNG image and map PDF annotations to image shapes,
     *  save the PNG image to the given output directory
     */
    @JvmStatic
    @Throws(IOException::class)
    private fun convertPdfAnnotationsToPngShapes(
        pdfPath: String, outputDirectory: String
    ): PdfToImageResultData {
        // Saving result data here
        var shapes: List<Shape>
        lateinit var pngFile: File
        var pageHeight by Delegates.notNull<Int>()
        lateinit var jsonShapes: String

        // Create a new renderer
        val renderer = PdfRenderer(getSeekableFileDescriptor(pdfPath))

        renderer.use { renderer ->
            // Assuming the pdf will have only 1 page (for now)
            val pageNum = 0
            val page = renderer.openPage(pageNum)

            // Create a bitmap
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            // Ensure white background
            bitmap.eraseColor(Color.WHITE)

            // Render the page on the bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val pdfName = extractFileNameFromPath(pdfPath)

            // Save the bitmap as a PNG file
            pngFile = saveBitmapAsPng(
                bitmap,
                outputDirectory,
                "PdfToImage-$pdfName-page-${pageNum + 1}.png"
            )

            // Save page height
            pageHeight = page.height

            // In OpenPdf lib, pages start from 1
            val pdfAnnotations = getAnnotationsFrom(pdfPath, pageNum = pageNum + 1)

            shapes = getShapesFor(pdfAnnotations, page.height)

            // Map to JSON string so we can pass them to MeasureLib
            jsonShapes = mapPdfShapesToJsonString(shapes as List<Rectangle>)

            // Close the page
            page.close()
        }

        return PdfToImageResultData(File(pdfPath), pngFile, pageHeight, jsonShapes)
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

    @JvmStatic
    private fun convertPngShapesToPdfAnnotations(
        shapes: List<Shape>,
        pageHeight: Int,
    ): List<Annotation> = shapes.map { it.toAnnotation(pageHeight) }

    @JvmStatic
    private fun drawPngShapesToPdf(
        pdfFile: File, pageHeight: Int,
        shapes: List<Shape>,
    ) {
        val annotations = convertPngShapesToPdfAnnotations(shapes, pageHeight)
        annotations.forEach { annotation ->
            when (annotation.type) {
                "SQUARE" -> AnnotationManager.addRectAnnotation(
                    annotation.points,
                    pdfFile,
                    annotation.relations
                )

                "CIRCLE" -> AnnotationManager.addCircleAnnotation(annotation.points, pdfFile)
                else -> throw Exception("Annotation is not recognised")
            }
        }

    }
}