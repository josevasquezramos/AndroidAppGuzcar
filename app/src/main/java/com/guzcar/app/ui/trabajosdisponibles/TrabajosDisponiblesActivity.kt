package com.guzcar.app.ui.trabajosdisponibles

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guzcar.app.data.api.RetrofitClient
import com.guzcar.app.data.model.TrabajoAsignadoDto
import com.guzcar.app.databinding.ActivityTrabajosDisponiblesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrabajosDisponiblesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrabajosDisponiblesBinding
    private lateinit var adapter: TrabajoDisponibleAdapter
    private var allTrabajosDisponibles: List<TrabajoAsignadoDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrabajosDisponiblesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecycler()
        setupSearch()
        loadTrabajosDisponibles()
    }

    private fun setupToolbar() {
        val toolbar = binding.topAppBarDisponibles
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecycler() {
        adapter = TrabajoDisponibleAdapter { trabajo ->
            confirmarAsignar(trabajo)
        }
        binding.recyclerTrabajosDisponibles.layoutManager = LinearLayoutManager(this)
        binding.recyclerTrabajosDisponibles.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEditTextDisponibles.addTextChangedListener { text ->
            applyFilter(text?.toString().orEmpty())
        }
    }

    private fun loadTrabajosDisponibles() {
        binding.emptyViewDisponibles.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getTrabajosDisponibles()
                val lista = response.data

                withContext(Dispatchers.Main) {
                    allTrabajosDisponibles = lista
                    applyFilter(binding.searchEditTextDisponibles.text?.toString().orEmpty())
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.disponiblesRoot,
                        "Error al cargar trabajos disponibles",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()

        val filtered = if (q.isBlank()) {
            allTrabajosDisponibles
        } else {
            val tokens = q.split("\\s+".toRegex())
                .filter { it.isNotBlank() }

            allTrabajosDisponibles.filter { trabajo ->
                val v = trabajo.vehiculo
                val texto = listOfNotNull(
                    v.placa,
                    v.tipo,
                    v.marca,
                    v.modelo,
                    v.color,
                    trabajo.descripcion_servicio
                ).joinToString(" ").lowercase()

                tokens.all { token ->
                    texto.contains(token)
                }
            }
        }

        if (filtered.isEmpty()) {
            binding.recyclerTrabajosDisponibles.visibility = View.GONE
            binding.emptyViewDisponibles.visibility = View.VISIBLE
            binding.emptyViewDisponibles.text =
                if (allTrabajosDisponibles.isEmpty())
                    "No hay trabajos disponibles."
                else
                    "No se encontraron trabajos para \"$query\"."
        } else {
            binding.recyclerTrabajosDisponibles.visibility = View.VISIBLE
            binding.emptyViewDisponibles.visibility = View.GONE
            adapter.submitList(filtered)
        }
    }

    private fun confirmarAsignar(trabajo: TrabajoAsignadoDto) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Asignar trabajo")
            .setMessage("¿Deseas asignarte este trabajo?")
            .setPositiveButton("Sí") { _, _ ->
                asignarTrabajo(trabajo.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun asignarTrabajo(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.asignarTrabajo(id)

                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.disponiblesRoot,
                        response.message,
                        Snackbar.LENGTH_LONG
                    ).show()

                    // Volver al Main para ver el trabajo ya asignado
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.disponiblesRoot,
                        "Error al asignar trabajo",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
