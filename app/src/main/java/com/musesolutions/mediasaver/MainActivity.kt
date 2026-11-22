package com.musesolutions.mediasaver

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var results: TextView
    private lateinit var shareButton: Button
    private var latestOutput: File? = null
    private var totalSavedBytes: Long = 0
    private val moneyPerGb = 10.0 / 3.0 // user's historical $10 for 3 GB baseline
    private val decimal = DecimalFormat("#,##0.00")

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) optimizeImage(uri)
    }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) optimizeVideo(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 24)
            setBackgroundColor(0xFFF5F7F5.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "DATA SAVER MEDIA"
            textSize = 27f
            setTextColor(0xFF143B28.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        content.addView(title)
        content.addView(label("Stop paying big corporations with big files.", 17f, true))
        content.addView(label("Optimize photos and videos before you share them. Originals are never overwritten.", 14f, false))
        content.addView(space(20))
        content.addView(actionButton("Choose a photo", "#16834A") { pickImage.launch("image/*") })
        content.addView(space(10))
        content.addView(actionButton("Choose a video", "#245B88") { pickVideo.launch("video/*") })
        content.addView(space(18))
        status = label("Choose a photo or video to begin.", 15f, true)
        content.addView(status)
        results = label("Your results will appear here.", 14f, false)
        content.addView(results)
        content.addView(space(12))
        shareButton = actionButton("Share optimized copy", "#16834A") { shareLatest() }
        shareButton.isEnabled = false
        content.addView(shareButton)
        content.addView(space(22))
        content.addView(label("Savings estimate", 19f, true))
        content.addView(label("Baseline: $10 for 3 GB = $3.33 per GB. The dollar figure is an allocated estimate, not a guaranteed reduction in your phone bill.", 13f, false))
        content.addView(space(8))
        content.addView(label("Total data saved in this session", 14f, false))
        content.addView(label("0.00 Mb  •  $0.00 estimated value", 17f, true).also { totalView = it })
        content.addView(space(20))
        content.addView(label("Pilot limitations: this prototype optimizes files you select here and shares the optimized copy. It does not yet intercept every app's upload or appear as a universal gallery source.", 12f, false))
        scroll.addView(content)
        root.addView(scroll, ViewGroup.LayoutParams(-1, -1))
        setContentView(root)
    }

    private lateinit var totalView: TextView

    private fun label(text: String, size: Float, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(if (bold) 0xFF1C3025.toInt() else 0xFF526057.toInt())
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(0, 7, 0, 7)
    }

    private fun space(height: Int) = TextView(this).apply { this.height = height }

    private fun actionButton(text: String, color: String, click: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextColor(0xFFFFFFFF.toInt())
        backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
        setOnClickListener { click() }
    }

    private fun copyUriToCache(uri: Uri, suffix: String): File {
        val out = File(cacheDir, "input_${System.currentTimeMillis()}$suffix")
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected media" }.copyTo(out.outputStream())
        }
        return out
    }

    private fun optimizeImage(uri: Uri) {
        status.text = "Optimizing photo…"
        shareButton.isEnabled = false
        Thread {
            try {
                val original = copyUriToCache(uri, ".source")
                val originalBytes = original.length()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(original.absolutePath, bounds)
                val maxSide = 1600
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = BitmapFactory.decodeFile(original.absolutePath, opts) ?: error("Could not decode this image format")
                val output = File(cacheDir, "optimized_${System.currentTimeMillis()}.jpg")
                FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
                bitmap.recycle()
                original.delete()
                showResult(output, originalBytes, output.length(), "Photo")
            } catch (e: Exception) { runOnUiThread { status.text = "Photo optimization failed: ${e.message}" } }
        }.start()
    }

    private fun optimizeVideo(uri: Uri) {
        status.text = "Preparing video optimization…"
        shareButton.isEnabled = false
        Thread {
            try {
                val input = copyUriToCache(uri, ".video")
                val originalBytes = input.length()
                val output = File(cacheDir, "optimized_${System.currentTimeMillis()}.mp4")
                val encoderSettings = VideoEncoderSettings.Builder().setBitrate(1_500_000).build()
                val encoderFactory = DefaultEncoderFactory.Builder(this)
                    .setRequestedVideoEncoderSettings(encoderSettings)
                    .build()
                val transformer = Transformer.Builder(this)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoderFactory)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult) {
                            input.delete()
                            showResult(output, originalBytes, output.length(), "Video")
                        }
                        override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException) {
                            runOnUiThread { status.text = "Video optimization failed: ${exportException.message}" }
                            input.delete()
                        }
                    }).build()
                runOnUiThread { status.text = "Compressing video on your phone…" }
                val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input))).build()
                transformer.start(editedItem, output.absolutePath)
            } catch (e: Exception) { runOnUiThread { status.text = "Video optimization failed: ${e.message}" } }
        }.start()
    }

    private fun showResult(output: File, original: Long, optimized: Long, kind: String) {
        val saved = (original - optimized).coerceAtLeast(0)
        val savedMb = saved * 8.0 / 1_000_000.0
        val originalMb = original * 8.0 / 1_000_000.0
        val optimizedMb = optimized * 8.0 / 1_000_000.0
        val percent = if (original > 0) saved * 100.0 / original else 0.0
        val dollars = saved.toDouble() / 1_000_000_000.0 * moneyPerGb
        runOnUiThread {
            latestOutput = output
            totalSavedBytes += saved
            status.text = "$kind optimized successfully"
            results.text = "Original: ${formatBytes(original)} (${decimal.format(originalMb)} Mb)\n" +
                "Optimized: ${formatBytes(optimized)} (${decimal.format(optimizedMb)} Mb)\n" +
                "Data saved: ${decimal.format(savedMb)} Mb\n" +
                "Reduction: ${decimal.format(percent)}%\n" +
                "Estimated value: $${decimal.format(dollars)}"
            val totalMb = totalSavedBytes * 8.0 / 1_000_000.0
            val totalDollars = totalSavedBytes.toDouble() / 1_000_000_000.0 * moneyPerGb
            totalView.text = "${decimal.format(totalMb)} Mb  •  $${decimal.format(totalDollars)} estimated value"
            shareButton.isEnabled = true
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "${decimal.format(bytes / 1_000_000_000.0)} GB"
        bytes >= 1_000_000 -> "${decimal.format(bytes / 1_000_000.0)} MB"
        bytes >= 1_000 -> "${decimal.format(bytes / 1_000.0)} KB"
        else -> "$bytes bytes"
    }

    private fun shareLatest() {
        val file = latestOutput ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val mime = if (file.extension.equals("mp4", true)) "video/mp4" else "image/jpeg"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share optimized media"))
    }
}
