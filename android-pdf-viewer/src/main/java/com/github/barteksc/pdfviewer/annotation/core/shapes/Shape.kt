package com.github.barteksc.pdfviewer.annotation.core.shapes

import android.graphics.PointF
import com.github.barteksc.pdfviewer.annotation.core.Annotation
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import kotlin.reflect.KClass

@ShapeType(
    property = "type",
    subtypes = [
        ShapeSubtype(clazz = Circle::class, name = "CIRCLE"),
        ShapeSubtype(clazz = Rectangle::class, name = "RECTANGLE")
    ]
)
abstract class Shape(
    @Transient open val type: String = "",
    @Transient open val points: List<PointF> = emptyList()
)

fun Shape.toAnnotation(pageHeight: Int): Annotation {
    return when (this) {
        is Rectangle -> toRectangleAnnotation(pageHeight)
        is Circle -> toCircleAnnotation(pageHeight)
        else -> throw Exception("")
    }
}

fun Shape.toRectangleAnnotation(pageHeight: Int): Annotation {
    val points = points.map { it.convertCoordinatesFrom(pageHeight) }
    return Annotation(
        type = "SQUARE",
        points = points
    )
}

fun Shape.toCircleAnnotation(pageHeight: Int): Annotation {
    val points = points.map { it.convertCoordinatesFrom(pageHeight) }
    return Annotation(type = "CIRCLE", points = points)
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ShapeType(val property: String, val subtypes: Array<ShapeSubtype>)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ShapeSubtype(val clazz: KClass<out Shape>, val name: String)

class ShapeDeserializer : JsonDeserializer<Shape> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Shape {
        val jsonObject = json.asJsonObject
        val shapeType = jsonObject.get("type").asString

        return when (shapeType) {
            "CIRCLE" -> context.deserialize(json, Circle::class.java)
            "RECTANGLE" -> context.deserialize(json, Rectangle::class.java)
            else -> throw JsonParseException("Unknown shape type: $shapeType")
        }
    }
}
