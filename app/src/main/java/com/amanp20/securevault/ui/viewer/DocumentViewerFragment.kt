package com.amanp20.securevault.ui.viewer

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.amanp20.securevault.R
import com.amanp20.securevault.SecureVaultApplication
import com.amanp20.securevault.databinding.FragmentDocumentViewerBinding
import com.amanp20.securevault.viewmodel.SecureVaultViewModel
import com.amanp20.securevault.viewmodel.SecureVaultViewModelFactory
import java.io.File

class DocumentViewerFragment :
    Fragment(R.layout.fragment_document_viewer) {

    private var _binding:
        FragmentDocumentViewerBinding? = null

    private val binding
        get() = _binding!!

    private val args:
        DocumentViewerFragmentArgs by navArgs()

    private val viewModel:
        SecureVaultViewModel by viewModels {

            val application =
                requireActivity()
                    .application
                        as SecureVaultApplication

            SecureVaultViewModelFactory(
                application
                    .appContainer
                    .secureVaultRepository
            )
        }

    private var decryptedFile:
        File? = null

    private var player:
        ExoPlayer? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        _binding =
            FragmentDocumentViewerBinding.bind(
                view
            )

        binding.toolbar
            .setNavigationOnClickListener {
                findNavController()
                    .navigateUp()
            }

        loadDocument()
    }

    private fun loadDocument() {

        showLoading()

        viewModel.getDocumentById(
            args.documentId,

            onResult = { document ->

                if (!isAdded || _binding == null) {
                    return@getDocumentById
                }

                if (document == null) {
                    showUnsupported()
                    return@getDocumentById
                }

                binding.toolbar.title =
                    document.originalName

                viewModel.openDocument(
                    document,

                    onResult = { file ->

                        if (!isAdded || _binding == null) {

                            file.delete()

                            return@openDocument
                        }

                        decryptedFile =
                            file

                        displayFile(
                            file,
                            document.mimeType
                        )
                    },

                    onError = {

                        if (
                            isAdded &&
                            _binding != null
                        ) {
                            showUnsupported()
                        }
                    }
                )
            },

            onError = {

                if (
                    isAdded &&
                    _binding != null
                ) {
                    showUnsupported()
                }
            }
        )
    }

    private fun displayFile(
        file: File,
        mimeType: String
    ) {

        if (!isAdded || _binding == null) {
            file.delete()
            return
        }

        when {

            mimeType.startsWith(
                "image/",
                ignoreCase = true
            ) -> {
                displayImage(file)
            }

            mimeType.startsWith(
                "video/",
                ignoreCase = true
            ) -> {
                displayVideo(file)
            }

            mimeType.startsWith(
                "text/",
                ignoreCase = true
            ) ||
                mimeType.equals(
                    "application/json",
                    ignoreCase = true
                ) ||
                mimeType.equals(
                    "application/xml",
                    ignoreCase = true
                ) -> {
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

        binding.loadingProgress.visibility =
            View.GONE

        binding.imageViewer.visibility =
            View.VISIBLE

        binding.videoViewer.visibility =
            View.GONE

        binding.textScrollView.visibility =
            View.GONE

        binding.unsupportedView.visibility =
            View.GONE

        val bitmap =
            runCatching {
                BitmapFactory.decodeFile(
                    file.absolutePath
                )
            }.getOrNull()

        if (bitmap == null) {

            showUnsupported()

            return
        }

        binding.imageViewer.setImageBitmap(
            bitmap
        )
    }

    private fun displayVideo(
        file: File
    ) {

        binding.loadingProgress.visibility =
            View.GONE

        binding.imageViewer.visibility =
            View.GONE

        binding.videoViewer.visibility =
            View.VISIBLE

        binding.textScrollView.visibility =
            View.GONE

        binding.unsupportedView.visibility =
            View.GONE

        releasePlayer()

        val exoPlayer =
            ExoPlayer.Builder(
                requireContext()
            ).build()

        player =
            exoPlayer

        binding.videoViewer.player =
            exoPlayer

        exoPlayer.addListener(
            object : Player.Listener {

                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    if (
                        isAdded &&
                        _binding != null
                    ) {
                        showUnsupported()
                    }
                }
            }
        )

        exoPlayer.setMediaItem(
            MediaItem.fromUri(
                android.net.Uri.fromFile(file)
            )
        )

        exoPlayer.prepare()

        exoPlayer.playWhenReady = true
    }

    private fun displayText(
        file: File
    ) {

        binding.loadingProgress.visibility =
            View.GONE

        binding.imageViewer.visibility =
            View.GONE

        binding.videoViewer.visibility =
            View.GONE

        binding.textScrollView.visibility =
            View.VISIBLE

        binding.unsupportedView.visibility =
            View.GONE

        val text =
            runCatching {
                file.readText()
            }.getOrNull()

        if (text == null) {

            showUnsupported()

            return
        }

        binding.textViewer.text =
            text
    }

    private fun showLoading() {

        binding.loadingProgress.visibility =
            View.VISIBLE

        binding.imageViewer.visibility =
            View.GONE

        binding.videoViewer.visibility =
            View.GONE

        binding.textScrollView.visibility =
            View.GONE

        binding.unsupportedView.visibility =
            View.GONE
    }

    private fun showUnsupported() {

        releasePlayer()

        binding.loadingProgress.visibility =
            View.GONE

        binding.imageViewer.visibility =
            View.GONE

        binding.videoViewer.visibility =
            View.GONE

        binding.textScrollView.visibility =
            View.GONE

        binding.unsupportedView.visibility =
            View.VISIBLE
    }

    private fun releasePlayer() {

        player?.let { exoPlayer ->

            binding.videoViewer.player =
                null

            exoPlayer.stop()
            exoPlayer.release()
        }

        player = null
    }

    override fun onStop() {
        super.onStop()

        /*
         * Pause video when the viewer is no longer visible.
         * The decrypted temporary file remains until the
         * viewer itself is destroyed.
         */
        player?.playWhenReady = false
    }

    override fun onDestroyView() {

        releasePlayer()

        binding.imageViewer.setImageDrawable(
            null
        )

        decryptedFile?.delete()

        decryptedFile = null

        _binding = null

        super.onDestroyView()
    }
}