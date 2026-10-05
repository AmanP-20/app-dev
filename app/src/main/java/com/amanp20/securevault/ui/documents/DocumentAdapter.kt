package com.amanp20.securevault.ui.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amanp20.securevault.R
import com.amanp20.securevault.data.model.SecureDocument
import com.amanp20.securevault.databinding.ItemDocumentBinding
import java.text.DateFormat
import java.util.Date

class DocumentAdapter(
    private val onClick: (SecureDocument) -> Unit,
    private val onMenu: (SecureDocument) -> Unit
) : ListAdapter<SecureDocument, DocumentAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        return ViewHolder(
            ItemDocumentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            document: SecureDocument
        ) {

            binding.nameText.text =
                document.originalName

            binding.detailsText.text =
                binding.root.context.getString(
                    R.string.document_details,
                    document.category,
                    formatSize(document.fileSize)
                )

            binding.dateText.text =
                binding.root.context.getString(
                    R.string.document_added,
                    DateFormat
                        .getDateInstance(
                            DateFormat.MEDIUM
                        )
                        .format(
                            Date(document.dateAdded)
                        )
                )

            binding.lockIcon.visibility =
                View.VISIBLE

            binding.root.setOnClickListener {
                onClick(document)
            }

            binding.moreButton.setOnClickListener {
                onMenu(document)
            }
        }

        private fun formatSize(
            bytes: Long
        ): String {

            return when {

                bytes < 1024 ->
                    "$bytes B"

                bytes < 1024 * 1024 ->
                    "${bytes / 1024} KB"

                bytes < 1024 * 1024 * 1024 ->
                    String.format(
                        "%.1f MB",
                        bytes / (1024f * 1024f)
                    )

                else ->
                    String.format(
                        "%.1f GB",
                        bytes / (1024f * 1024f * 1024f)
                    )
            }
        }
    }

    companion object {

        private val DIFF =
            object :
                DiffUtil.ItemCallback<SecureDocument>() {

                override fun areItemsTheSame(
                    oldItem: SecureDocument,
                    newItem: SecureDocument
                ): Boolean {

                    return oldItem.id ==
                        newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: SecureDocument,
                    newItem: SecureDocument
                ): Boolean {

                    return oldItem ==
                        newItem
                }
            }
    }
}