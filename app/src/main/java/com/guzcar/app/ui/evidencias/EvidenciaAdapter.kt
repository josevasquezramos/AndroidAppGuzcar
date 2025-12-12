package com.guzcar.app.ui.evidencias

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.guzcar.app.data.model.EvidenciaDto
import com.guzcar.app.databinding.ItemEvidenciaBinding

class EvidenciaAdapter(
    private val onEditarClick: (EvidenciaUi) -> Unit,
    private val onEliminarClick: (EvidenciaUi) -> Unit,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<EvidenciaAdapter.EvidenciaViewHolder>() {

    private val items = mutableListOf<EvidenciaUi>()

    fun setFromDtoList(list: List<EvidenciaDto>) {
        items.clear()
        items.addAll(
            list.map {
                EvidenciaUi(
                    id = it.id,
                    url = it.url,
                    tipo = it.tipo,
                    observacion = it.observacion,
                    isUploading = false,
                    isSelected = false
                )
            }
        )
        notifyDataSetChanged()
    }

    fun addUploadingItems(list: List<EvidenciaUi>) {
        items.addAll(0, list)
        notifyDataSetChanged()
    }

    fun updateItemObservacion(id: Int, nuevaObs: String?) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx != -1) {
            items[idx].observacion = nuevaObs
            items[idx].isUploading = false
            notifyItemChanged(idx)
        }
    }

    fun getSelectedIds(): List<Int> =
        items.filter { it.isSelected && it.id != null }.map { it.id!! }

    inner class EvidenciaViewHolder(
        private val binding: ItemEvidenciaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EvidenciaUi) {
            binding.checkSelect.setOnCheckedChangeListener(null)
            binding.checkSelect.isChecked = item.isSelected
            binding.checkSelect.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onSelectionChanged()
            }

            binding.txtTipo.text = if (item.tipo == "video") "Video" else "Imagen"
            binding.txtObservacion.text =
                if (item.observacion.isNullOrBlank()) "Sin observación" else item.observacion

            binding.txtEstado.visibility = if (item.isUploading) View.VISIBLE else View.GONE

            binding.iconVideo.visibility =
                if (item.tipo == "video") View.VISIBLE else View.GONE

            if (!item.url.isNullOrBlank()) {
                Glide.with(binding.imgPreview.context)
                    .load(item.url)
                    .centerCrop()
                    .into(binding.imgPreview)
            } else {
                binding.imgPreview.setImageResource(android.R.color.darker_gray)
            }

            binding.btnEditar.setOnClickListener { onEditarClick(item) }
            binding.btnEliminar.setOnClickListener { onEliminarClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvidenciaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEvidenciaBinding.inflate(inflater, parent, false)
        return EvidenciaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EvidenciaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
