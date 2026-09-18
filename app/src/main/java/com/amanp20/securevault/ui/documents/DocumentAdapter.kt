package com.amanp20.securevault.ui.documents

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.databinding.ItemDocumentBinding
import java.text.DateFormat
import java.util.Date

class DocumentAdapter(
    private val onClick: (SecureDocument) -> Unit,
    private val onMenu: (SecureDocument) -> Unit
) : ListAdapter<SecureDocument, DocumentAdapter.ViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(document: SecureDocument) {
            binding.nameText.text = document.name
            binding.detailsText.text = binding.root.context.getString(
                com.amanp20.securevault.R.string.document_details,
                document.category,
                formatSize(document.fileSize)
            )
            binding.dateText.text = binding.root.context.getString(
                com.amanp20.securevault.R.string.document_added,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(document.dateAdded))
            )
            binding.lockIcon.visibility = if (document.isLocked) android.view.View.VISIBLE else android.view.View.GONE
            binding.root.setOnClickListener { onClick(document) }
            binding.moreButton.setOnClickListener { onMenu(document) }
        }

        private fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024f * 1024f))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SecureDocument>() {
            override fun areItemsTheSame(a: SecureDocument, b: SecureDocument) = a.id == b.id
            override fun areContentsTheSame(a: SecureDocument, b: SecureDocument) = a == b
        }
    }
}
