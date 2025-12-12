package com.guzcar.app.ui.evidencias

data class EvidenciaUi(
    val id: Int?,              // null mientras está "subiendo"
    val url: String?,
    val tipo: String,          // "imagen" o "video"
    var observacion: String?,
    var isUploading: Boolean = false,
    var isSelected: Boolean = false
)
