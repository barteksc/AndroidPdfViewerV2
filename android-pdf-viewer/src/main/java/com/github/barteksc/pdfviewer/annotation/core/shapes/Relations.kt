package com.github.barteksc.pdfviewer.annotation.core.shapes

data class Relations(
    var documentation: List<Documentation> = emptyList()
)

data class Documentation(
    val schemaId: Long,
    val documentId: String
)