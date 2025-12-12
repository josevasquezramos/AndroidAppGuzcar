package com.guzcar.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.guzcar.app.data.model.TrabajoAsignadoDto
import com.guzcar.app.databinding.ItemTrabajoBinding

class TrabajoAdapter(
    private val onOpcionesClick: (TrabajoAsignadoDto) -> Unit,
    private val onFinalizarClick: (TrabajoAsignadoDto) -> Unit,
    private val onAbandonarClick: (TrabajoAsignadoDto) -> Unit,
) : RecyclerView.Adapter<TrabajoAdapter.TrabajoViewHolder>() {

    private val items = mutableListOf<TrabajoAsignadoDto>()

    fun submitList(list: List<TrabajoAsignadoDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class TrabajoViewHolder(
        private val binding: ItemTrabajoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrabajoAsignadoDto) {
            val v = item.vehiculo
            val detalle = listOfNotNull(v.tipo, v.marca, v.modelo, v.color)
                .joinToString(" ")

            binding.txtPlaca.text = v.placa ?: "SIN PLACA"
            binding.txtDetalleVehiculo.text =
                if (detalle.isNotBlank()) detalle else "Sin datos de vehículo"
            binding.txtDescripcion.text =
                item.descripcion_servicio ?: "Sin descripción"

            binding.btnOpciones.setOnClickListener { onOpcionesClick(item) }
            binding.btnFinalizar.setOnClickListener { onFinalizarClick(item) }
            binding.btnAbandonar.setOnClickListener { onAbandonarClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrabajoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTrabajoBinding.inflate(inflater, parent, false)
        return TrabajoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrabajoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
