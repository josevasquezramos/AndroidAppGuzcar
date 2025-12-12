package com.guzcar.app.ui.evidencias

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guzcar.app.data.api.RetrofitClient
import com.guzcar.app.data.model.*
import com.guzcar.app.databinding.ActivityEvidenciasBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.guzcar.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EvidenciasActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRABAJO_ID = "trabajo_id"
        const val EXTRA_TITULO = "titulo"
    }

    private lateinit var binding: ActivityEvidenciasBinding
    private lateinit var adapter: EvidenciaAdapter
    private var trabajoId: Int = 0

    private var allEvidencias: List<EvidenciaDto> = emptyList()
    private var isUploading = false

    private var tempPhotoUri: Uri? = null
    private var tempVideoUri: Uri? = null

    // Picker galería (imágenes + videos, múltiples)
    private val pickMultipleMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult
            pedirObservacionYSubir(uris)
        }

    // Cámara foto
    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempPhotoUri != null) {
                pedirObservacionYSubir(listOf(tempPhotoUri!!))
            }
        }

    // Cámara video
    private val recordVideoLauncher =
        registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            if (success && tempVideoUri != null) {
                pedirObservacionYSubir(listOf(tempVideoUri!!))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEvidenciasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        trabajoId = intent.getIntExtra(EXTRA_TRABAJO_ID, 0)
        val tituloExtra = intent.getStringExtra(EXTRA_TITULO)

        setupToolbar(tituloExtra)
        setupRecycler()
        setupBulkActions()
        setupFab()

        loadEvidencias()
    }

    private fun setupToolbar(tituloExtra: String?) {
        val toolbar = binding.topAppBarEvidencias
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        if (!tituloExtra.isNullOrBlank()) {
            toolbar.subtitle = tituloExtra
        }
    }

    private fun setupRecycler() {
        adapter = EvidenciaAdapter(
            onEditarClick = { evidenciaUi ->
                evidenciaUi.id?.let { id ->
                    mostrarDialogoEditar(id, evidenciaUi.observacion)
                }
            },
            onEliminarClick = { evidenciaUi ->
                evidenciaUi.id?.let { id ->
                    confirmarEliminar(id)
                }
            },
            onSelectionChanged = { actualizarBarraBulk() }
        )

        binding.recyclerEvidencias.layoutManager = LinearLayoutManager(this)
        binding.recyclerEvidencias.adapter = adapter
    }

    private fun setupBulkActions() {
        binding.btnBulkEditar.setOnClickListener {
            val seleccionados = adapter.getSelectedIds()
            if (seleccionados.isEmpty()) return@setOnClickListener
            mostrarDialogoBulkEditar(seleccionados)
        }

        binding.btnBulkEliminar.setOnClickListener {
            val seleccionados = adapter.getSelectedIds()
            if (seleccionados.isEmpty()) return@setOnClickListener
            confirmarBulkEliminar(seleccionados)
        }
    }

    private fun setupFab() {
        binding.fabAddEvidencia.setOnClickListener {
            if (isUploading) return@setOnClickListener
            seleccionarArchivos()
        }
    }

    private fun seleccionarArchivos() {
        val opciones = arrayOf("Tomar foto", "Grabar video", "Elegir de galería")

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar evidencia")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirCamaraFoto()
                    1 -> abrirCamaraVideo()
                    2 -> abrirGaleria()
                }
            }
            .show()
    }

    private fun abrirGaleria() {
        pickMultipleMediaLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
            )
        )
    }

    private fun abrirCamaraFoto() {
        val imageFile = createTempMediaFile("foto_", ".jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            imageFile
        )
        tempPhotoUri = uri
        takePhotoLauncher.launch(uri)
    }

    private fun abrirCamaraVideo() {
        val videoFile = createTempMediaFile("video_", ".mp4")
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            videoFile
        )
        tempVideoUri = uri
        recordVideoLauncher.launch(uri)
    }

    private fun createTempMediaFile(prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix, cacheDir)
    }

    // ---------------- CARGA LISTA ---------------- //

    private fun loadEvidencias() {
        binding.emptyViewEvidencias.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getEvidencias(trabajoId)
                allEvidencias = response.data

                withContext(Dispatchers.Main) {
                    if (allEvidencias.isEmpty()) {
                        binding.recyclerEvidencias.visibility = View.GONE
                        binding.emptyViewEvidencias.visibility = View.VISIBLE
                    } else {
                        binding.recyclerEvidencias.visibility = View.VISIBLE
                        binding.emptyViewEvidencias.visibility = View.GONE
                        adapter.setFromDtoList(allEvidencias)
                    }
                    actualizarBarraBulk()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error al cargar evidencias",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------- ESTADO UPLOAD ---------------- //

    private fun setUploadingState(uploading: Boolean) {
        isUploading = uploading
        binding.fabAddEvidencia.isEnabled = !uploading
        binding.progressUpload.visibility = if (uploading) View.VISIBLE else View.GONE
        if (uploading) {
            binding.bulkActionsBar.visibility = View.GONE
        } else {
            actualizarBarraBulk()
        }
    }

    private fun actualizarBarraBulk() {
        val seleccionados = adapter.getSelectedIds()
        binding.bulkActionsBar.visibility =
            if (seleccionados.isEmpty() || isUploading) View.GONE else View.VISIBLE
    }

    // ---------------- EDITAR INDIVIDUAL ---------------- //

    private fun mostrarDialogoEditar(id: Int, observacionActual: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_observacion, null)
        val observacionEdit = dialogView.findViewById<TextInputEditText>(R.id.observacionEdit)

        observacionEdit.setText(observacionActual ?: "")

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Editar observación")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaObs = observacionEdit.text?.toString()?.trim()
                actualizarObservacion(id, nuevaObs)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        // 👇 Esto evita que se cierre al tocar fuera
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
    }

    private fun actualizarObservacion(id: Int, nuevaObs: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.updateEvidencia(
                    id,
                    EvidenciaUpdateRequest(observacion = nuevaObs)
                )

                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        response.message,
                        Snackbar.LENGTH_LONG
                    ).show()
                    adapter.updateItemObservacion(id, nuevaObs)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error al actualizar evidencia",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------- ELIMINAR INDIVIDUAL ---------------- //

    private fun confirmarEliminar(id: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar evidencia")
            .setMessage("¿Deseas eliminar esta evidencia?")
            .setPositiveButton("Sí") { _, _ ->
                eliminarEvidencia(id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEvidencia(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.deleteEvidencia(id)
                val refreshed = RetrofitClient.api.getEvidencias(trabajoId)

                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        response.message,
                        Snackbar.LENGTH_LONG
                    ).show()
                    allEvidencias = refreshed.data
                    adapter.setFromDtoList(allEvidencias)
                    if (allEvidencias.isEmpty()) {
                        binding.recyclerEvidencias.visibility = View.GONE
                        binding.emptyViewEvidencias.visibility = View.VISIBLE
                    } else {
                        binding.recyclerEvidencias.visibility = View.VISIBLE
                        binding.emptyViewEvidencias.visibility = View.GONE
                    }
                    actualizarBarraBulk()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error al eliminar evidencia",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------- BULK UPDATE ---------------- //

    private fun mostrarDialogoBulkEditar(ids: List<Int>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_observacion, null)
        val observacionEdit = dialogView.findViewById<TextInputEditText>(R.id.observacionEdit)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Editar observación")
            .setMessage("Se aplicará la misma observación a ${ids.size} evidencias.")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaObs = observacionEdit.text?.toString()?.trim()
                bulkUpdateObservacion(ids, nuevaObs)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
    }

    private fun bulkUpdateObservacion(ids: List<Int>, nuevaObs: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.bulkUpdateEvidencias(
                    BulkUpdateRequest(
                        evidencia_ids = ids,
                        observacion = nuevaObs
                    )
                )

                val refreshed = RetrofitClient.api.getEvidencias(trabajoId)

                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        response.message,
                        Snackbar.LENGTH_LONG
                    ).show()
                    allEvidencias = refreshed.data
                    adapter.setFromDtoList(allEvidencias)
                    actualizarBarraBulk()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error al actualizar evidencias",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------- BULK DELETE ---------------- //

    private fun confirmarBulkEliminar(ids: List<Int>) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar evidencias")
            .setMessage("¿Eliminar ${ids.size} evidencias seleccionadas?")
            .setPositiveButton("Sí") { _, _ ->
                bulkDelete(ids)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun bulkDelete(ids: List<Int>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.bulkDeleteEvidencias(
                    BulkDeleteRequest(evidencia_ids = ids)
                )

                val refreshed = RetrofitClient.api.getEvidencias(trabajoId)

                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        response.message,
                        Snackbar.LENGTH_LONG
                    ).show()
                    allEvidencias = refreshed.data
                    adapter.setFromDtoList(allEvidencias)
                    if (allEvidencias.isEmpty()) {
                        binding.recyclerEvidencias.visibility = View.GONE
                        binding.emptyViewEvidencias.visibility = View.VISIBLE
                    } else {
                        binding.recyclerEvidencias.visibility = View.VISIBLE
                        binding.emptyViewEvidencias.visibility = View.GONE
                    }
                    actualizarBarraBulk()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error al eliminar evidencias",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ---------------- SUBIDA (ilusión de rapidez) ---------------- //

    private fun pedirObservacionYSubir(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_observacion, null)
        val observacionEdit =
            dialogView.findViewById<TextInputEditText>(R.id.observacionEdit)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar observación")
            .setView(dialogView)
            .setPositiveButton("Subir") { _, _ ->
                val texto = observacionEdit.text?.toString()?.trim().orEmpty()
                subirEvidencias(uris, texto.ifBlank { null })
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.show()
    }

    private fun getMediaTypeFromUri(uri: Uri): String {
        val mime = contentResolver.getType(uri)?.lowercase()
        if (mime != null) {
            return if (mime.startsWith("video/")) "video" else "imagen"
        }
        val path = uri.lastPathSegment?.lowercase() ?: ""
        return when {
            path.endsWith(".mp4") || path.endsWith(".3gp") || path.endsWith(".mkv") || path.endsWith(".mov") -> "video"
            path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".webp") -> "imagen"
            else -> "imagen"
        }
    }

    private fun subirEvidencias(uris: List<Uri>, observacion: String?) {
        // UI optimista: añadir "subiendo..."
        val uploadingItems = uris.map { uri ->
            val tipo = getMediaTypeFromUri(uri)
            EvidenciaUi(
                id = null,
                url = null,
                tipo = tipo,
                observacion = observacion,
                isUploading = true
            )
        }

        adapter.addUploadingItems(uploadingItems)
        binding.recyclerEvidencias.visibility = View.VISIBLE
        binding.emptyViewEvidencias.visibility = View.GONE

        Snackbar.make(
            binding.root,
            "Subiendo ${uris.size} evidencia(s)...",
            Snackbar.LENGTH_SHORT
        ).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    setUploadingState(true)
                }

                val parts = uris.map { uri ->
                    val file = createTempFileFromUri(uri)
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                    val body = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData(
                        name = "files[]",
                        filename = file.name,
                        body = body
                    )
                }

                val obsBody: RequestBody? =
                    if (observacion.isNullOrBlank()) null
                    else RequestBody.create("text/plain".toMediaTypeOrNull(), observacion)

                val response = RetrofitClient.api.uploadEvidencias(
                    trabajoId = trabajoId,
                    files = parts,
                    observacion = obsBody
                )

                val refreshed = RetrofitClient.api.getEvidencias(trabajoId)

                withContext(Dispatchers.Main) {
                    allEvidencias = refreshed.data
                    adapter.setFromDtoList(allEvidencias)
                    setUploadingState(false)
                    Snackbar.make(
                        binding.root,
                        response.message,
                        Snackbar.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setUploadingState(false)
                    Snackbar.make(
                        binding.root,
                        "Error al subir evidencias",
                        Snackbar.LENGTH_LONG
                    ).show()
                    loadEvidencias()
                }
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val mime = contentResolver.getType(uri)?.lowercase() ?: ""
        val ext = when {
            mime.startsWith("image/") -> ".jpg"
            mime == "video/mp4" -> ".mp4"
            mime.startsWith("video/") -> ".mp4"
            else -> ""
        }

        val tempFile = File.createTempFile("evidencia_", ext, cacheDir)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
