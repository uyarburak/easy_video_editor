package com.example.easy_video_editor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.source.UriDataSource
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@UnstableApi
class VideoUtils {
    companion object {
        /**
         * Gets metadata information about a video file
         * 
         * @param context Android context
         * @param videoPath Path to the video file
         * @return VideoMetadata object containing video information
         */
        suspend fun getVideoMetadata(context: Context, videoPath: String): VideoMetadata {
            return withContext(Dispatchers.IO) {
                val videoFile = File(videoPath)
                require(videoFile.exists()) { "Video file does not exist" }
                
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoPath)
                    
                    // Get basic metadata
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                    
                    // Get title and author (may be null)
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) 
                        ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                    
                    // Get rotation
                    val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0

                    // Get date
                    val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

                    // Get file size
                    val fileSize = videoFile.length()
                    
                    VideoMetadata(
                        duration = duration,
                        width = width,
                        height = height,
                        title = title,
                        author = author,
                        rotation = rotation,
                        fileSize = fileSize,
                        date = date
                    )
                } finally {
                    retriever.release()
                }
            }
        }
        /**
         * Compress a video while maintaining aspect ratio
         * @param context Android context
         * @param videoPath Path to the input video file
         * @param targetHeight Target height for the compressed video (default: 720p)
         * @return Path to the compressed video file
         */
        suspend fun compressVideo(
            context: Context,
            videoPath: String,
            targetHeight: Int = 720, // Default to 720p
        ): String {
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
                require(targetHeight > 0) { "Target height must be positive" }
            }
            
            // Create temp directory if it doesn't exist
            val tempDir: String = context.getExternalFilesDir("easy_video_editor")!!.absolutePath
            val outputFileName = "VID_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())}_${videoPath.hashCode()}.mp4"
            val outputPath = "$tempDir${File.separator}$outputFileName"
            val outputFile = File(outputPath)
            if (outputFile.exists()) outputFile.delete()
            
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    // Define video compression strategy based on targetHeight
                    val videoTrackStrategy = DefaultVideoStrategy.atMost(targetHeight).build()
                    
                    // Configure audio strategy - always include audio for the simple version
                    val audioTrackStrategy = DefaultAudioStrategy.builder()
                        .channels(DefaultAudioStrategy.CHANNELS_AS_INPUT)
                        .sampleRate(DefaultAudioStrategy.SAMPLE_RATE_AS_INPUT)
                        .build()
                    
                    // Create data source (no trimming in the simple version)
                    val dataSource = UriDataSource(context, videoPath.toUri())
                    
                    // Create a variable to store the transcode future for cancellation
                    val transcodeFuture = Transcoder.into(outputPath)
                        .addDataSource(dataSource)
                        .setVideoTrackStrategy(videoTrackStrategy)
                        .setAudioTrackStrategy(audioTrackStrategy)
                        .setListener(object : TranscoderListener {
                            override fun onTranscodeProgress(progress: Double) {
                                // Report progress to ProgressManager (0.0 to 1.0)
                                ProgressManager.getInstance().reportProgress(progress)
                            }
                            
                            override fun onTranscodeCompleted(successCode: Int) {
                                if (continuation.isActive) {
                                    // Mark progress as 100% complete
                                    ProgressManager.getInstance().reportProgress(1.0)
                                    
                                    // Return the output path to the caller
                                    continuation.resume(outputPath)
                                }
                            }
                            
                            override fun onTranscodeCanceled() {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        VideoException("Video compression was canceled")
                                    )
                                }
                                // Clean up output file if canceled
                                outputFile.delete()
                            }
                            
                            override fun onTranscodeFailed(exception: Throwable) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        VideoException(
                                            "Failed to compress video: ${exception.message}",
                                            exception
                                        )
                                    )
                                }
                                // Clean up output file if failed
                                outputFile.delete()
                            }
                        }).transcode()
                    
                    // Set up cancellation handling
                    continuation.invokeOnCancellation {
                        transcodeFuture.cancel(true)
                        outputFile.delete()
                    }
                }
            }
        }
        suspend fun trimVideo(
                context: Context,
                videoPath: String,
                startTimeMs: Long,
                endTimeMs: Long
        ): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(startTimeMs >= 0) { "Start time must be non-negative" }
                require(endTimeMs > startTimeMs) { "End time must be greater than start time" }
                require(File(videoPath).exists()) { "Input video file does not exist" }
            }

            val outputFile =
                    withContext(Dispatchers.IO) {
                        File(context.cacheDir, "trimmed_video_${System.currentTimeMillis()}.mp4")
                                .apply { if (exists()) delete() }
                    }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                            MediaItem.Builder()
                                    .setUri(Uri.fromFile(File(videoPath)))
                                    .setClippingConfiguration(
                                            MediaItem.ClippingConfiguration.Builder()
                                                    .setStartPositionMs(startTimeMs)
                                                    .setEndPositionMs(endTimeMs)
                                                    .build()
                                    )
                                    .build()

                    val transformer =
                            Transformer.Builder(context)
                                    .addListener(
                                            object : Transformer.Listener {
                                                override fun onCompleted(
                                                        composition: Composition,
                                                        exportResult: ExportResult
                                                ) {
                                                    if (continuation.isActive) {
                                                        continuation.resume(outputFile.absolutePath)
                                                    }
                                                }

                                                override fun onError(
                                                        composition: Composition,
                                                        exportResult: ExportResult,
                                                        exportException: ExportException
                                                ) {
                                                    if (continuation.isActive) {
                                                        continuation.resumeWithException(
                                                                VideoException(
                                                                        "Failed to trim video: ${exportException.message}",
                                                                        exportException
                                                                )
                                                        )
                                                    }
                                                    outputFile.delete()
                                                }
                                            }
                                    )
                                    .build()

                    transformer.start(mediaItem, outputFile.absolutePath)
                    
                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }
                                
                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                }
            }
        }

        suspend fun mergeVideos(context: Context, videoPaths: List<String>): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(videoPaths.isNotEmpty()) { "Video paths list cannot be empty" }
                videoPaths.forEachIndexed { index, path ->
                    require(File(path).exists()) {
                        "Video file at index $index does not exist: $path"
                    }
                }
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "merged_video_${System.currentTimeMillis()}.mp4")
                    .apply { if (exists()) delete() }
            }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val editedMediaItems =
                        videoPaths.map { path ->
                            EditedMediaItem.Builder(
                                MediaItem.fromUri(Uri.fromFile(File(path)))
                            )
                            .build()
                        }

                    val sequence = EditedMediaItemSequence(editedMediaItems)

                    val composition = Composition.Builder(listOf(sequence)).build()

                    val transformer =
                        Transformer.Builder(context)
                            .addListener(
                                object : Transformer.Listener {
                                    override fun onCompleted(
                                        composition: Composition,
                                        exportResult: ExportResult
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(
                                                outputFile.absolutePath
                                            )
                                        }
                                    }

                                    override fun onError(
                                        composition: Composition,
                                        exportResult: ExportResult,
                                        exportException: ExportException
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(
                                                VideoException(
                                                    "Failed to merge videos: ${exportException.message}",
                                                    exportException
                                                )
                                            )
                                        }
                                        outputFile.delete()
                                    }
                                }
                            )
                            .build()

                    transformer.start(composition, outputFile.absolutePath)
                    
                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }
                                
                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                    }
                }
            }

        suspend fun extractAudio(context: Context, videoPath: String): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
            }

            val outputFile =
                    withContext(Dispatchers.IO) {
                        File(context.cacheDir, "extracted_audio_${System.currentTimeMillis()}.aac")
                                .apply { if (exists()) delete() }
                    }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                            MediaItem.Builder().setUri(Uri.fromFile(File(videoPath))).build()

                    val editedMediaItem =
                            EditedMediaItem.Builder(mediaItem).setRemoveVideo(true).build()

                    val transformer =
                            Transformer.Builder(context)
                                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                                    .addListener(
                                            object : Transformer.Listener {
                                                override fun onCompleted(
                                                        composition: Composition,
                                                        exportResult: ExportResult
                                                ) {
                                                    if (continuation.isActive) {
                                                        continuation.resume(outputFile.absolutePath)
                                                    }
                                                }

                                                override fun onError(
                                                        composition: Composition,
                                                        exportResult: ExportResult,
                                                        exportException: ExportException
                                                ) {
                                                    if (continuation.isActive) {
                                                        continuation.resumeWithException(
                                                                VideoException(
                                                                        "Failed to extract audio: ${exportException.message}",
                                                                        exportException
                                                                )
                                                        )
                                                    }
                                                    outputFile.delete()
                                                }
                                            }
                                    )
                                    .build()

                    transformer.start(editedMediaItem, outputFile.absolutePath)

                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }
                                
                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                }
            }
        }
        suspend fun adjustVideoSpeed(
            context: Context,
            videoPath: String,
            speedMultiplier: Float
        ): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
                require(speedMultiplier > 0) { "Speed multiplier must be positive" }
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "speed_adjusted_video_${System.currentTimeMillis()}.mp4")
                    .apply { if (exists()) delete() }
            }

            // Check if video has audio
            val hasAudio = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoPath)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)?.toInt() == 1
                } finally {
                    retriever.release()
                }
            }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                        MediaItem.Builder().setUri(Uri.fromFile(File(videoPath))).build()

                    val videoEffect = SpeedChangeEffect(speedMultiplier)
                    
                    // Only create audio processor if video has audio
                    val audioEffects = if (hasAudio) {
                        val audio = SonicAudioProcessor()
                        audio.setSpeed(speedMultiplier)
                        listOf(audio)
                    } else {
                        emptyList()
                    }

                    val effects = Effects(audioEffects, listOf(videoEffect))
                    
                    val editedMediaItem = EditedMediaItem.Builder(mediaItem).setEffects(effects).build()
                    
                    val transformer = Transformer.Builder(context)
                            .addListener(
                                object : Transformer.Listener {
                                    override fun onCompleted(
                                        composition: Composition,
                                        exportResult: ExportResult
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(outputFile.absolutePath)
                                        }
                                    }

                                    override fun onError(
                                        composition: Composition,
                                        exportResult: ExportResult,
                                        exportException: ExportException
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(
                                                VideoException(
                                                    "Failed to adjust video speed: ${exportException.message}",
                                                    exportException
                                                )
                                            )
                                        }
                                        outputFile.delete()
                                    }
                                }
                            )
                            .build()
                    
                    transformer.start(editedMediaItem, outputFile.absolutePath)
                    
                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }
                                
                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )
                    
                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                }
            }
        }

        suspend fun removeAudioFromVideo(context: Context, videoPath: String): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "muted_video_${System.currentTimeMillis()}.mp4")
                    .apply { if (exists()) delete() }
            }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                        MediaItem.Builder().setUri(Uri.fromFile(File(videoPath))).build()

                    val editedMediaItem =
                        EditedMediaItem.Builder(mediaItem)
                            .setRemoveAudio(true) // Remove audio
                            .build()

                    val transformer =
                        Transformer.Builder(context)
                            .addListener(
                                object : Transformer.Listener {
                                    override fun onCompleted(
                                        composition: Composition,
                                        exportResult: ExportResult
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(outputFile.absolutePath)
                                        }
                                    }

                                    override fun onError(
                                        composition: Composition,
                                        exportResult: ExportResult,
                                        exportException: ExportException
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(
                                                VideoException(
                                                    "Failed to remove audio: ${exportException.message}",
                                                    exportException
                                                )
                                            )
                                        }
                                        outputFile.delete()
                                    }
                                }
                            )
                            .build()

                    transformer.start(editedMediaItem, outputFile.absolutePath)

                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }
                                
                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                }
            }
        }

        suspend fun cropVideo(
            context: Context,
            videoPath: String,
            aspectRatio: String
        ): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
                require(aspectRatio.matches(Regex("\\d+:\\d+"))) { "Aspect ratio must be in format 'width:height' (e.g., '16:9')" }
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "cropped_video_${System.currentTimeMillis()}.mp4")
                    .apply { if (exists()) delete() }
            }

            // Get video dimensions
            val retriever = MediaMetadataRetriever()
            val (videoWidth, videoHeight) = withContext(Dispatchers.IO) {
                try {
                    retriever.setDataSource(videoPath)
                    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloat() ?: 0f
                    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloat() ?: 0f
                    width to height
                } finally {
                    retriever.release()
                }
            }

            // Calculate crop dimensions based on aspect ratio
            val (targetWidth, targetHeight) = aspectRatio.split(":").map { it.toFloat() }
            val targetAspectRatio = targetWidth / targetHeight
            val videoAspectRatio = videoWidth / videoHeight

            // Calculate scale factors to achieve the desired aspect ratio through scaling
            val (scaleWidth, scaleHeight) = if (videoAspectRatio > targetAspectRatio) {
                // Video is wider than target, scale height up to crop sides
                val scale = videoAspectRatio / targetAspectRatio
                1f to scale
            } else {
                // Video is taller than target, scale width up to crop top/bottom
                val scale = targetAspectRatio / videoAspectRatio
                scale to 1f
            }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                        MediaItem.Builder().setUri(Uri.fromFile(File(videoPath))).build()

                    val editedMediaItem =
                        EditedMediaItem.Builder(mediaItem)
                            .setEffects(
                                Effects(
                                    emptyList(),
                                    listOf(
                                        ScaleAndRotateTransformation.Builder()
                                            .setScale(scaleWidth, scaleHeight)
                                            .build()
                                    )
                                )
                            )
                            .build()

                    val transformer =
                        Transformer.Builder(context)
                            .addListener(
                                object : Transformer.Listener {
                                    override fun onCompleted(
                                        composition: Composition,
                                        exportResult: ExportResult
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(outputFile.absolutePath)
                                        }
                                    }

                                    override fun onError(
                                        composition: Composition,
                                        exportResult: ExportResult,
                                        exportException: ExportException
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(
                                                VideoException(
                                                    "Failed to crop video: ${exportException.message}",
                                                    exportException
                                                )
                                            )
                                        }
                                        outputFile.delete()
                                    }
                                }
                            )
                            .build()

                    transformer.start(editedMediaItem, outputFile.absolutePath)

                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }
                                
                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                }
            }
        }

        suspend fun rotateVideo(context: Context, videoPath: String, rotationDegrees: Float): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
                require(rotationDegrees % 90 == 0f) { "Rotation must be a multiple of 90 degrees" }
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "rotated_video_${System.currentTimeMillis()}.mp4")
                    .apply { if (exists()) delete() }
            }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                        MediaItem.Builder().setUri(Uri.fromFile(File(videoPath))).build()

                    val effects =
                        Effects(
                            emptyList(),
                            listOf(
                                ScaleAndRotateTransformation.Builder()
                                    .setRotationDegrees(rotationDegrees)
                                    .build()
                            )
                        )

                    val editedMediaItem =
                        EditedMediaItem.Builder(mediaItem).setEffects(effects).build()

                    val transformer =
                        Transformer.Builder(context)
                            .addListener(
                                object : Transformer.Listener {
                                    override fun onCompleted(
                                        composition: Composition,
                                        exportResult: ExportResult
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(outputFile.absolutePath)
                                        }
                                    }

                                    override fun onError(
                                        composition: Composition,
                                        exportResult: ExportResult,
                                        exportException: ExportException
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(
                                                VideoException(
                                                    "Failed to rotate video: ${exportException.message}",
                                                    exportException
                                                )
                                            )
                                        }
                                        outputFile.delete()
                                    }
                                }
                            )
                            .build()

                    transformer.start(editedMediaItem, outputFile.absolutePath)

                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }
                                
                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                }
            }
        }

        suspend fun generateThumbnail(
            context: Context,
            videoPath: String,
            positionMs: Long,
            width: Int? = null,
            height: Int? = null,
            quality: Int = 80
        ): String = withContext(Dispatchers.IO) {
            require(File(videoPath).exists()) { "Video file does not exist" }
            require(positionMs >= 0) { "Position must be non-negative" }
            require(quality in 0..100) { "Quality must be between 0 and 100" }
            width?.let { require(it > 0) { "Width must be positive" } }
            height?.let { require(it > 0) { "Height must be positive" } }

            val retriever = MediaMetadataRetriever()
            return@withContext try {
                retriever.setDataSource(videoPath)

                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    ?: throw VideoException("Could not determine video duration")
                require(positionMs <= durationMs) { "Position exceeds video duration" }

                val bitmap = retriever.getFrameAtTime(
                    positionMs * 1000L, // milliseconds to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: throw VideoException("Failed to generate thumbnail")

                val scaledBitmap = if (width != null && height != null) {
                    val aspectRatio = bitmap.width.toFloat() / bitmap.height
                    val targetRatio = width.toFloat() / height

                    val finalWidth: Int
                    val finalHeight: Int

                    if (aspectRatio > targetRatio) {
                        finalWidth = width
                        finalHeight = (width / aspectRatio).toInt()
                    } else {
                        finalHeight = height
                        finalWidth = (height * aspectRatio).toInt()
                    }

                    Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
                } else {
                    bitmap
                }

                val outputFile = File(context.cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg").apply {
                    if (exists()) delete()
                }

                FileOutputStream(outputFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }

                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                outputFile.absolutePath
            } catch (e: OutOfMemoryError) {
                throw VideoException("Out of memory while generating thumbnail", e)
            } catch (e: Exception) {
                throw VideoException("Error generating thumbnail: ${e.message}", e)
            } finally {
                retriever.release()
            }
        }


        suspend fun flipVideo(context: Context, videoPath: String, flipDirection: String): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
                require(flipDirection.isNotEmpty()) { "Direction must not empty" }
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "flip_video_${System.currentTimeMillis()}.mp4")
                    .apply { if (exists()) delete() }
            }

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                        MediaItem.Builder().setUri(Uri.fromFile(File(videoPath))).build()

                    val flipEffect = when (flipDirection.lowercase()) {
                        "horizontal" -> ScaleAndRotateTransformation.Builder().setScale(-1f, 1f).build()
                        "vertical" -> ScaleAndRotateTransformation.Builder().setScale(1f, -1f).build()
                        else -> ScaleAndRotateTransformation.Builder().setScale(1f, 1f).build()
                    }

                    val effects =
                        Effects(
                            emptyList(),
                            listOf(
                                flipEffect
                            )
                        )

                    val editedMediaItem =
                        EditedMediaItem.Builder(mediaItem).setEffects(effects).build()

                    val transformer =
                        Transformer.Builder(context)
                            .addListener(
                                object : Transformer.Listener {
                                    override fun onCompleted(
                                        composition: Composition,
                                        exportResult: ExportResult
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resume(outputFile.absolutePath)
                                        }
                                    }

                                    override fun onError(
                                        composition: Composition,
                                        exportResult: ExportResult,
                                        exportException: ExportException
                                    ) {
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(
                                                VideoException(
                                                    "Failed to rotate video: ${exportException.message}",
                                                    exportException
                                                )
                                            )
                                        }
                                        outputFile.delete()
                                    }
                                }
                            )
                            .build()

                    transformer.start(editedMediaItem, outputFile.absolutePath)

                    // Set up progress tracking
                    val progressHolder = androidx.media3.transformer.ProgressHolder()
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    mainHandler.post(
                        object : Runnable {
                            override fun run() {
                                val progressState = transformer.getProgress(progressHolder)
                                // Report progress to ProgressManager
                                // Send progress updates more frequently
                                // Always report progress as long as we have a valid progress value
                                if (progressHolder.progress >= 0) {
                                    // Report progress to ProgressManager
                                    ProgressManager.getInstance().reportProgress(progressHolder.progress / 100.0)
                                }

                                // Continue polling if the transformer has started (simplified condition)
                                // The original Media3 example uses this condition, which might be more reliable
                                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                                    mainHandler.postDelayed(this, 200) // Update every 200ms - better balance
                                }
                            }
                        }
                    )

                    continuation.invokeOnCancellation {
                        if (transformer.getProgress(progressHolder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                            transformer.cancel()
                            outputFile.delete()
                        }
                    }
                }
            }
        }

        /**
         * Sets the maximum frames per second (FPS) for a video
         * @param context Android context
         * @param videoPath Path to the input video file
         * @param maxFps Maximum frames per second to set
         * @return Path to the processed video file
         */
        suspend fun setMaxFps(
            context: Context,
            videoPath: String,
            maxFps: Int
        ): String {
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
                require(maxFps > 0) { "Max FPS must be positive" }
            }

            // Get input video's frame rate using MediaMetadataRetriever
            val inputFrameRate = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoPath)
                    // Try to get frame rate from metadata
                    var frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
                    
                    // If frame rate is not available in metadata, try to get it from MediaFormat
                    if (frameRate == null || frameRate <= 0) {
                        val mediaExtractor = MediaExtractor()
                        try {
                            mediaExtractor.setDataSource(videoPath)
                            for (i in 0 until mediaExtractor.trackCount) {
                                val format = mediaExtractor.getTrackFormat(i)
                                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                                    break
                                }
                            }
                        } finally {
                            mediaExtractor.release()
                        }
                    }
                    
                    frameRate ?: 0f
                } finally {
                    retriever.release()
                }
            }

            // If input frame rate is already lower than or equal to maxFps, return original video
            if (inputFrameRate <= maxFps) {
                return videoPath
            }

            // Create temp directory if it doesn't exist
            val tempDir: String = context.getExternalFilesDir("easy_video_editor")!!.absolutePath
            val outputFileName = "VID_${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())}_${videoPath.hashCode()}.mp4"
            val outputPath = "$tempDir${File.separator}$outputFileName"
            val outputFile = File(outputPath)
            if (outputFile.exists()) outputFile.delete()

            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    // Create data source
                    val dataSource = UriDataSource(context, videoPath.toUri())

                    // Configure video strategy with max FPS
                    val videoTrackStrategy = DefaultVideoStrategy.Builder()
                        .frameRate(maxFps)
                        .build()

                    // Configure audio strategy - keep original audio
                    val audioTrackStrategy = DefaultAudioStrategy.builder()
                        .channels(DefaultAudioStrategy.CHANNELS_AS_INPUT)
                        .sampleRate(DefaultAudioStrategy.SAMPLE_RATE_AS_INPUT)
                        .build()

                    // Create a variable to store the transcode future for cancellation
                    val transcodeFuture = Transcoder.into(outputPath)
                        .addDataSource(dataSource)
                        .setVideoTrackStrategy(videoTrackStrategy)
                        .setAudioTrackStrategy(audioTrackStrategy)
                        .setListener(object : TranscoderListener {
                            override fun onTranscodeProgress(progress: Double) {
                                // Report progress to ProgressManager (0.0 to 1.0)
                                ProgressManager.getInstance().reportProgress(progress)
                            }

                            override fun onTranscodeCompleted(successCode: Int) {
                                if (continuation.isActive) {
                                    // Mark progress as 100% complete
                                    ProgressManager.getInstance().reportProgress(1.0)
                                    // Return the output path to the caller
                                    continuation.resume(outputPath)
                                }
                            }

                            override fun onTranscodeCanceled() {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        VideoException("Video FPS adjustment was canceled")
                                    )
                                }
                                // Clean up output file if canceled
                                outputFile.delete()
                            }

                            override fun onTranscodeFailed(exception: Throwable) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        VideoException(
                                            "Failed to adjust video FPS: ${exception.message}",
                                            exception
                                        )
                                    )
                                }
                                // Clean up output file if failed
                                outputFile.delete()
                            }
                        }).transcode()

                    // Set up cancellation handling
                    continuation.invokeOnCancellation {
                        transcodeFuture.cancel(true)
                        outputFile.delete()
                    }
                }
            }
        }

        // --- Constants for Repair Logic ---
        private const val REPAIR_TAG = "VideoRepairUtil"
        private const val IO_TIMEOUT_US = 10000L

        /**
         * Checks a video for odd dimensions and, if found, repairs it by transcoding to
         * even dimensions. If dimensions are already even, it returns the original path.
         * This is the public-facing function to handle potentially problematic videos.
         *
         * @param context The application context.
         * @param videoPath The absolute path to the input video file.
         * @return The absolute path to the compliant video file (either the original or a new, repaired file).
         * @throws IOException if there's a problem with file I/O or media processing during repair.
         */
        suspend fun ensureEvenDimensions(context: Context, videoPath: String): String = withContext(Dispatchers.IO) {
            val inputFile = File(videoPath)
            require(inputFile.exists()) { "Input video file does not exist: $videoPath" }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(inputFile.absolutePath)
                val originalWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val originalHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

                // If dimensions are valid and even, no processing is needed.
                if (originalWidth > 0 && originalHeight > 0 && originalWidth % 2 == 0 && originalHeight % 2 == 0) {
                    Log.d(REPAIR_TAG, "Video dimensions are already even. No processing needed.")
                    return@withContext videoPath
                }

                Log.d(REPAIR_TAG, "Video requires repair. Original dimensions: ${originalWidth}x${originalHeight}")
                // Call the internal repair function.
                return@withContext repairVideoWithEvenDimensions(context, retriever, videoPath, originalWidth, originalHeight)
            } finally {
                retriever.release()
            }
        }

        /**
         * Internal logic to transcode a video to even dimensions.
         * This function manually extracts video frames, crops them to even dimensions, and re-encodes them.
         * The audio track is copied directly without re-encoding.
         */
        @Throws(IOException::class)
        private fun repairVideoWithEvenDimensions(context: Context, retriever: MediaMetadataRetriever, inputPath: String, originalWidth: Int, originalHeight: Int): String {
            val outputFile = File(context.cacheDir, "repaired_video_${System.currentTimeMillis()}.mp4")
            var muxer: MediaMuxer? = null
            var videoEncoder: MediaCodec? = null
            var audioExtractor: MediaExtractor? = null
            var muxerStarted = false

            try {
                // Crop to the nearest even dimensions by subtracting 1 if odd.
                val newWidth = originalWidth - (originalWidth % 2)
                val newHeight = originalHeight - (originalHeight % 2)
                if (newWidth <= 0 || newHeight <= 0) {
                    throw IOException("Invalid video dimensions after cropping: ${newWidth}x${newHeight}")
                }

                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                // --- Video Setup ---
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val totalDurationUs = durationStr!!.toLong() * 1000
                val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull()?.let { count ->
                    if (totalDurationUs > 0) (count * 1_000_000L / totalDurationUs).toInt() else 30
                } ?: 30

                val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, newWidth, newHeight).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, newWidth * newHeight * 5)
                    setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inputSurface = videoEncoder.createInputSurface()
                videoEncoder.start()
                var videoTrackIndex = -1
                val bufferInfo = MediaCodec.BufferInfo()

                // --- Audio Setup ---
                audioExtractor = MediaExtractor().apply { setDataSource(inputPath) }
                val audioTrackIndexInExtractor = findTrackIndex(audioExtractor, "audio/")
                var audioTrackIndexInMuxer = -1
                if (audioTrackIndexInExtractor != -1) {
                    val audioFormat = audioExtractor.getTrackFormat(audioTrackIndexInExtractor)
                    audioTrackIndexInMuxer = muxer.addTrack(audioFormat)
                    audioExtractor.selectTrack(audioTrackIndexInExtractor)
                }

                // --- Main Video Processing Loop ---
                val frameIntervalUs = 1_000_000L / frameRate
                var presentationTimeUs = 0L

                while (presentationTimeUs < totalDurationUs) {
                    val originalBitmap = retriever.getFrameAtTime(presentationTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (originalBitmap == null) {
                        presentationTimeUs += frameIntervalUs
                        continue
                    }
                    
                    inputSurface.lockCanvas(null).also { canvas ->
                        canvas.drawBitmap(originalBitmap, Rect(0, 0, newWidth, newHeight), Rect(0, 0, newWidth, newHeight), null)
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    originalBitmap.recycle()

                    drainEncoder(videoEncoder, muxer, bufferInfo, false)?.let { newVideoTrack ->
                        if(videoTrackIndex == -1) {
                            videoTrackIndex = newVideoTrack
                            // If audio track is also ready, start muxer
                            if(audioTrackIndexInMuxer != -1) {
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                    }
                    presentationTimeUs += frameIntervalUs
                }
                videoEncoder.signalEndOfInputStream()
                drainEncoder(videoEncoder, muxer, bufferInfo, true)

                // --- Audio Passthrough Loop ---
                if (audioTrackIndexInExtractor != -1) {
                    if (!muxerStarted) {
                        muxer.start()
                        muxerStarted = true
                    }
                    val audioBuffer = ByteBuffer.allocate(1024 * 1024)
                    while (true) {
                        val chunkSize = audioExtractor.readSampleData(audioBuffer, 0)
                        if (chunkSize < 0) break
                        muxer.writeSampleData(audioTrackIndexInMuxer, audioBuffer, MediaCodec.BufferInfo().apply {
                            offset = 0; size = chunkSize; presentationTimeUs = audioExtractor.sampleTime; flags = audioExtractor.sampleFlags
                        })
                        audioExtractor.advance()
                    }
                }

                return outputFile.absolutePath

            } catch (e: Exception) {
                Log.e(REPAIR_TAG, "Error during video repair", e)
                outputFile.delete()
                throw IOException("Failed to repair video", e)
            } finally {
                videoEncoder?.stop()
                videoEncoder?.release()
                audioExtractor?.release()
                if (muxerStarted) {
                    try {
                        muxer?.stop()
                    } catch (e: Exception) {
                        Log.e(REPAIR_TAG, "Error stopping muxer", e)
                    }
                }
                muxer?.release()
            }
        }

        private fun drainEncoder(encoder: MediaCodec, muxer: MediaMuxer, bufferInfo: MediaCodec.BufferInfo, endOfStream: Boolean): Int? {
            var videoTrackIndex : Int? = null
            if (endOfStream) encoder.signalEndOfInputStream()
            while (true) {
                when (val status = encoder.dequeueOutputBuffer(bufferInfo, IO_TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) break
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                    else -> {
                        val encodedData = encoder.getOutputBuffer(status) ?: break
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && bufferInfo.size != 0) {
                            muxer.writeSampleData(videoTrackIndex ?: -1, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(status, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
            return videoTrackIndex
        }

        private fun findTrackIndex(extractor: MediaExtractor, mimePrefix: String): Int {
            for (i in 0 until extractor.trackCount) {
                if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith(mimePrefix) == true) return i
            }
            return -1
        }

        // Helper to check if muxer has been started, using reflection as there's no public API.
        private fun isMuxerStarted(muxer: MediaMuxer): Boolean {
            return try {
                val field = muxer.javaClass.getDeclaredField("mState")
                field.isAccessible = true
                field.getInt(muxer) == 1 // MediaMuxer.MUXER_STATE_STARTED is 1
            } catch (e: Exception) { false }
        }
    }
}

class VideoException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

/**
 * Data class representing video metadata
 */
data class VideoMetadata(
    val duration: Long, // Duration in milliseconds
    val width: Int,
    val height: Int,
    val title: String?,
    val author: String?,
    val rotation: Int, // 0, 90, 180, or 270 degrees
    val fileSize: Long, // in bytes
    val date: String?
)
