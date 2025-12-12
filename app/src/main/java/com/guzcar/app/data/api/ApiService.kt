package com.guzcar.app.data.api

import com.guzcar.app.data.model.ApiMessageResponse
import com.guzcar.app.data.model.BulkDeleteRequest
import com.guzcar.app.data.model.BulkUpdateRequest
import com.guzcar.app.data.model.EvidenciaListResponse
import com.guzcar.app.data.model.EvidenciaUpdateRequest
import com.guzcar.app.data.model.EvidenciaUpdateResponse
import com.guzcar.app.data.model.LoginRequest
import com.guzcar.app.data.model.LoginResponse
import com.guzcar.app.data.model.Trabajo
import com.guzcar.app.data.model.TrabajosAsignadosResponse
import com.guzcar.app.data.model.TrabajosDisponiblesResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @GET("trabajos/asignados")
    suspend fun getTrabajosAsignados(): TrabajosAsignadosResponse

    @POST("trabajos/{id}/finalizar")
    suspend fun finalizarTrabajo(@Path("id") id: Int): ApiMessageResponse

    @POST("trabajos/{id}/abandonar")
    suspend fun abandonarTrabajo(@Path("id") id: Int): ApiMessageResponse

    @GET("trabajos/disponibles")
    suspend fun getTrabajosDisponibles(): TrabajosDisponiblesResponse

    @POST("trabajos/{id}/asignar")
    suspend fun asignarTrabajo(@Path("id") id: Int): ApiMessageResponse

    @GET("trabajos/{trabajoId}/evidencias")
    suspend fun getEvidencias(
        @Path("trabajoId") trabajoId: Int
    ): EvidenciaListResponse

    @Multipart
    @POST("trabajos/{trabajoId}/evidencias")
    suspend fun uploadEvidencias(
        @Path("trabajoId") trabajoId: Int,
        @Part files: List<MultipartBody.Part>,
        @Part("observacion") observacion: RequestBody?
    ): EvidenciaListResponse

    @PUT("evidencias/{id}")
    suspend fun updateEvidencia(
        @Path("id") id: Int,
        @Body body: EvidenciaUpdateRequest
    ): EvidenciaUpdateResponse

    @DELETE("evidencias/{id}")
    suspend fun deleteEvidencia(
        @Path("id") id: Int
    ): ApiMessageResponse

    @POST("evidencias/bulk-update")
    suspend fun bulkUpdateEvidencias(
        @Body body: BulkUpdateRequest
    ): ApiMessageResponse

    @POST("evidencias/bulk-delete")
    suspend fun bulkDeleteEvidencias(
        @Body body: BulkDeleteRequest
    ): ApiMessageResponse
}

data class TrabajosResponse(
    val message: String,
    val data: List<Trabajo>
)

data class BasicResponse(
    val message: String,
    val trabajo_id: Int? = null
)
