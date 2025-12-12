package com.guzcar.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guzcar.app.data.api.RetrofitClient
import com.guzcar.app.data.model.TrabajoAsignadoDto
import com.guzcar.app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.core.widget.addTextChangedListener
import com.guzcar.app.ui.evidencias.EvidenciasActivity
import com.guzcar.app.ui.trabajosdisponibles.TrabajosDisponiblesActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TrabajoAdapter
    private var allTrabajos: List<TrabajoAsignadoDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupSearchListener()
        setupFab()
        loadTrabajos()
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que vuelves a Main, recarga lista (por si asignaste algún trabajo)
        loadTrabajos()
    }

    private fun setupFab() {
        binding.fabAsignarTrabajo.setOnClickListener {
            startActivity(
                Intent(this, TrabajosDisponiblesActivity::class.java)
            )
        }
    }

    private fun setupRecycler() {
        adapter = TrabajoAdapter(
            onOpcionesClick = { trabajo ->
                mostrarOpciones(trabajo)
            },
            onFinalizarClick = { trabajo ->
                confirmarFinalizar(trabajo)
            },
            onAbandonarClick = { trabajo ->
                confirmarAbandonar(trabajo)
            }
        )

        binding.recyclerTrabajos.layoutManager = LinearLayoutManager(this)
        binding.recyclerTrabajos.adapter = adapter
    }

    private fun loadTrabajos() {
        binding.emptyView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getTrabajosAsignados()
                val lista = response.data

                withContext(Dispatchers.Main) {
                    allTrabajos = lista
                    setupSearchListener() // asegurar que el search funciona
                    applyFilter(binding.searchEditText.text?.toString().orEmpty())
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.mainRoot, "Error al cargar trabajos", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener { text ->
            applyFilter(text?.toString().orEmpty())
        }
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()

        val filtered = if (q.isBlank()) {
            allTrabajos
        } else {
            val tokens = q.split("\\s+".toRegex())
                .filter { it.isNotBlank() }

            allTrabajos.filter { trabajo ->
                val v = trabajo.vehiculo
                val texto = listOfNotNull(
                    v.placa,
                    v.tipo,
                    v.marca,
                    v.modelo,
                    v.color,
                    trabajo.descripcion_servicio
                )
                    .joinToString(" ")
                    .lowercase()

                // TODOS los tokens deben estar contenidos en el texto
                tokens.all { token ->
                    texto.contains(token)
                }
            }
        }

        if (filtered.isEmpty()) {
            binding.recyclerTrabajos.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text =
                if (allTrabajos.isEmpty())
                    "No tienes trabajos asignados."
                else
                    "No se encontraron trabajos para \"$query\"."
        } else {
            binding.recyclerTrabajos.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            adapter.submitList(filtered)
        }
    }


    private fun mostrarOpciones(trabajo: TrabajoAsignadoDto) {
        val opciones = arrayOf("Evidencias")

        MaterialAlertDialogBuilder(this)
            .setTitle("Opciones del trabajo")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        val titulo = buildString {
                            append(trabajo.vehiculo.placa ?: "SIN PLACA")
                            val extras = listOfNotNull(
                                trabajo.vehiculo.marca,
                                trabajo.vehiculo.modelo
                            )
                            if (extras.isNotEmpty()) {
                                append(" ")
                                append(extras.joinToString(" "))
                            }
                        }

                        val intent = Intent(this, EvidenciasActivity::class.java).apply {
                            putExtra(EvidenciasActivity.EXTRA_TRABAJO_ID, trabajo.id)
                            putExtra(EvidenciasActivity.EXTRA_TITULO, titulo)
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun confirmarFinalizar(trabajo: TrabajoAsignadoDto) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Finalizar trabajo")
            .setMessage("¿Seguro que deseas marcar este trabajo como finalizado?")
            .setPositiveButton("Sí") { _, _ ->
                finalizarTrabajo(trabajo.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarAbandonar(trabajo: TrabajoAsignadoDto) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Abandonar trabajo")
            .setMessage("¿Seguro que deseas abandonar este trabajo?")
            .setPositiveButton("Sí") { _, _ ->
                abandonarTrabajo(trabajo.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarTrabajo(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.finalizarTrabajo(id)

                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.mainRoot, response.message, Snackbar.LENGTH_LONG).show()
                    loadTrabajos()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.mainRoot, "Error al finalizar trabajo", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun abandonarTrabajo(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.abandonarTrabajo(id)

                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.mainRoot, response.message, Snackbar.LENGTH_LONG).show()
                    loadTrabajos()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.mainRoot, "Error al abandonar trabajo", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}
