package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.annotations.Annotation
import com.github.barteksc.pdfviewer.annotation.core.annotations.AnnotationType
import com.github.salomonbrys.kotson.jsonNull
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

open class Shape(
    @Transient open val type: String = "",
    @Transient open val points: List<PointF> = emptyList()
)

fun Shape.toAnnotation(pageHeight: Int): Annotation {
    return when (this) {
        is Rectangle -> toRectangleAnnotation(pageHeight)
        is Circle -> toCircleAnnotation(pageHeight)
        else -> throw Exception("Couldn't parse shape to annotation")
    }
}

fun Shape.toRectangleAnnotation(pageHeight: Int): Annotation {
    val points = points.map { it.convertCoordinatesFrom(pageHeight) }
    return Annotation(
        type = AnnotationType.SQUARE.name,
        points = points,
        relations = (this as Rectangle).relations
    )
}

fun Shape.toCircleAnnotation(pageHeight: Int): Annotation {
    val points = points.map { it.convertCoordinatesFrom(pageHeight) }
    return Annotation(type = AnnotationType.CIRCLE.name, points = points)
}

class ShapeDeserializer : JsonDeserializer<Shape> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Shape {
        val jsonObject = json.asJsonObject
        val shapeType = jsonObject.get("type").asString

        return when (shapeType) {
            ShapeType.CIRCLE.name-> context.deserialize(json, Circle::class.java)
            ShapeType.RECTANGLE.name -> context.deserialize(json, Rectangle::class.java)
            else -> throw JsonParseException("Unknown shape type: $shapeType")
        }
    }
}

internal fun mapJsonStringToPdfShapes(jsonShapes: String): List<Rectangle> {
    val listType = object : TypeToken<List<Rectangle>>() {}.type
    val gson =
        Gson().newBuilder().registerTypeAdapter(Rectangle::class.java, RectangleTypeAdapter())
            .create()
    val shapes: List<Rectangle> = gson.fromJson(jsonShapes, listType)
    return shapes
}

internal fun mapPdfShapesToJsonString(rectangles: List<Rectangle>): String {
    val shapesArray = rectangles.map { rectangle ->
        val gson = GsonBuilder()
            .setLenient()
            .create()

        jsonObject(
            "type" to rectangle.type,
            "points" to rectangle.points.map { point ->
                jsonObject(
                    "x" to point.x.toString(),
                    "y" to point.y.toString(),
                    "z" to jsonNull
                )
            }.toJsonArray(),
            "edges" to rectangle.edges.map {
                jsonObject(
                    "start" to jsonObject(
                        "x" to it.start.x.toString(),
                        "y" to it.start.y.toString(),
                        "z" to jsonNull
                    ),
                    "end" to jsonObject(
                        "x" to it.end.x.toString(),
                        "y" to it.end.y.toString(),
                        "z" to jsonNull
                    ),
                    "value" to "0.0",
                    "unit" to "",
                    "distance" to jsonNull,
                    "name" to "",
                    "colorCode" to ""
                )
            }.toJsonArray(),
            "relations" to gson.toJsonTree(rectangle.relations)
        )
    }.toJsonArray()
    return shapesArray.toString()
}