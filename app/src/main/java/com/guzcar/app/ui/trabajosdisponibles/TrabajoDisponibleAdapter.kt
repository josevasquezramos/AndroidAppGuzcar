package com.guzcar.app.ui.trabajosdisponibles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.guzcar.app.data.model.TrabajoAsignadoDto
import com.guzcar.app.databinding.ItemTrabajoDisponibleBinding

class TrabajoDisponibleAdapter(
    private val onAsignarClick: (TrabajoAsignadoDto) -> Unit
) : RecyclerView.Adapter<TrabajoDisponibleAdapter.TrabajoDisponibleViewHolder>() {

    private val items = mutableListOf<TrabajoAsignadoDto>()

    fun submitList(list: List<TrabajoAsignadoDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class TrabajoDisponibleViewHolder(
        private val binding: ItemTrabajoDisponibleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TrabajoAsignadoDto) {
            val v = item.vehiculo
            val detalle = listOfNotNull(v.tipo, v.marca, v.modelo, v.color)
                .joinToString(" ")

            binding.txtPlacaDisp.text = v.placa ?: "SIN PLACA"
            binding.txtDetalleVehiculoDisp.text =
                if (detalle.isNotBlank()) detalle else "Sin datos de vehículo"
            binding.txtDescripcionDisp.text =
                item.descripcion_servicio ?: "Sin descripción"

            binding.btnAsignar.setOnClickListener {
                onAsignarClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrabajoDisponibleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTrabajoDisponibleBinding.inflate(inflater, parent, false)
        return TrabajoDisponibleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrabajoDisponibleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
