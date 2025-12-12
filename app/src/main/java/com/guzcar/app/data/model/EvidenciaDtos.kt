package com.guzcar.app.data.model

data class EvidenciaListResponse(
    val message: String,
    val data: List<EvidenciaDto>
)

data class EvidenciaDto(
    val id: Int,
    val url: String,
    val tipo: String,            // "imagen" o "video"
    val observacion: String?
)

data class EvidenciaUpdateRequest(
    val observacion: String?
)

data class EvidenciaUpdateResponse(
    val message: String,
    val data: EvidenciaDto
)

data class BulkUpdateRequest(
    val evidencia_ids: List<Int>,
    val observacion: String?
)

data class BulkDeleteRequest(
    val evidencia_ids: List<Int>
)
