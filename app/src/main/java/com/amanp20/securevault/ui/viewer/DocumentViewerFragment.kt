package com.amanp20.securevault.ui.viewer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.amanp20.securevault.R
import com.amanp20.securevault.databinding.FragmentDocumentViewerBinding
import com.amanp20.securevault.utils.AppContainer
import com.amanp20.securevault.viewmodel.SecureVaultViewModel
import com.amanp20.securevault.viewmodel.SecureVaultViewModelFactory
import java.io.File

class DocumentViewerFragment :
    Fragment(R.layout.fragment_document_viewer) {

    private var _binding: FragmentDocumentViewerBinding? = null
    private val binding get() = _binding!!

    private val args: DocumentViewerFragmentArgs by navArgs()

    private val viewModel: SecureVaultViewModel by viewModels {
        SecureVaultViewModelFactory(
            AppContainer.repository
        )
    }

    private var decryptedFile: File? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding =
            FragmentDocumentViewerBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        loadDocument()
    }

    private fun loadDocument() {

        binding.loadingProgress.visibility =
            View.VISIBLE

        binding.imageViewer.visibility =
            View.GONE

        binding.textScrollView.visibility =
            View.GONE

        binding.unsupportedView.visibility =
            View.GONE

        viewModel.getDocumentById(
            id = args.documentId,

            onResult = { document ->

                if (!isAdded) {
                    return@getDocumentById
                }

                if (document == null) {
                    showUnsupported()
                    return@getDocumentById
                }

                binding.toolbar.title =
                    document.originalName

                viewModel.openDocument(
                    document = document,

                    onResult = { file ->

                        if (!isAdded) {
                            file.delete()
                            return@openDocument
                        }

                        decryptedFile = file

                        displayFile(
                            file,
                            document.mimeType
                        )
                    },

                    onError = {
                        if (isAdded) {
                            showUnsupported()
                        }
                    }
                )
            },

            onError = {
                if (isAdded) {
                    showUnsupported()
                }
            }
        )
    }

    private fun displayFile(
        file: File,
        mimeType: String
    ) {

        binding.loadingProgress.visibility =
            View.GONE

        when {

            mimeType.startsWith("image/") -> {
                displayImage(file)
            }

            mimeType.startsWith("text/") ||
                mimeType == "application/json" ||
                mimeType == "application/xml" -> {
                displayText(file)
            }

            else -> {
                showUnsupported()
            }
        }
    }

    private fun displayImage(
        file: File
    ) {

        val bitmap =
            BitmapFactory.decodeFile(
                file.absolutePath
            )

        if (bitmap == null) {
            showUnsupported()
            return
        }

        binding.imageViewer.setImageBitmap(
            bitmap
        )

        binding.imageViewer.visibility =
            View.VISIBLE
    }

    private fun displayText(
        file: File
    ) {

        val text = try {
            file.readText()
        } catch (_: Exception) {
            null
        }

        if (text == null) {
            showUnsupported()
            return
        }

        binding.textViewer.text = text

        binding.textScrollView.visibility =
            View.VISIBLE
    }

    private fun showUnsupported() {

        binding.loadingProgress.visibility =
            View.GONE

        binding.imageViewer.visibility =
            View.GONE

        binding.textScrollView.visibility =
            View.GONE

        binding.unsupportedView.visibility =
            View.VISIBLE
    }

    override fun onDestroyView() {
        decryptedFile?.delete()
        decryptedFile = null

        _binding = null

        super.onDestroyView()
    }
}