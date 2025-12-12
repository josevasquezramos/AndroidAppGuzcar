package com.guzcar.app.data.model

data class Trabajo(
    val id: Int,
    val vehiculo: Vehiculo,
    val descripcion_servicio: String?,
    val fecha_ingreso: String?,
    val fecha_salida: String?,
    val estado: EstadoTrabajo
)