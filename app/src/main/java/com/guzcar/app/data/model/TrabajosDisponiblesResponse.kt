package com.guzcar.app.data.model

data class TrabajosDisponiblesResponse(
    val message: String,
    val data: List<TrabajoAsignadoDto> // reutilizamos el mismo DTO
)