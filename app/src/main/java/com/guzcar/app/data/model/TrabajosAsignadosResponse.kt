package com.guzcar.app.data.model

data class TrabajosAsignadosResponse(
    val message: String,
    val data: List<TrabajoAsignadoDto>
)

data class TrabajoAsignadoDto(
    val id: Int,
    val vehiculo: VehiculoResumenDto,
    val descripcion_servicio: String?,
    val fecha_ingreso: String?,
    val fecha_salida: String?,
    val estado: EstadoTrabajoDto
)

data class VehiculoResumenDto(
    val placa: String?,
    val tipo: String?,
    val marca: String?,
    val modelo: String?,
    val color: String?
)

data class EstadoTrabajoDto(
    val asignado_al_tecnico: Boolean,
    val finalizado: Boolean
)
