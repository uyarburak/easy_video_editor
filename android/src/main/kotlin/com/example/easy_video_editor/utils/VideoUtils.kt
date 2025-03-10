package com.example.easy_video_editor.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
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
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@UnstableApi
class VideoUtils {
    companion object {
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
                        File(context.cacheDir, "extracted_audio_${System.currentTimeMillis()}.m4a")
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

            // Transformer operations on Main thread
            return withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val mediaItem =
                        MediaItem.Builder().setUri(Uri.fromFile(File(videoPath))).build()

                    val videoEffect = SpeedChangeEffect(speedMultiplier)

                    val effects = Effects(emptyList(), listOf(videoEffect))

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
                            .setRemoveAudio(true) // Xoá âm thanh
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

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                        outputFile.delete()
                    }
                }
            }
        }

        suspend fun scaleVideo(
            context: Context,
            videoPath: String,
            scaleX: Float,
            scaleY: Float
        ): String {
            // File operations on IO thread
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Input video file does not exist" }
                require(scaleX > 0 && scaleY > 0) { "Scale values must be greater than 0" }
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "scaled_video_${System.currentTimeMillis()}.mp4")
                    .apply { if (exists()) delete() }
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
                                            .setScale(scaleX, scaleY)
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
                                                    "Failed to scale video: ${exportException.message}",
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
        ): String =
            withContext(Dispatchers.IO) {
                require(File(videoPath).exists()) { "Video file does not exist" }

                val retriever = MediaMetadataRetriever()
                return@withContext try {
                    retriever.setDataSource(videoPath)

                    val bitmap =
                        retriever.getFrameAtTime(
                            positionMs * 1000, // Convert to microseconds
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                            ?: throw VideoException("Failed to generate thumbnail")

                    val scaledBitmap =
                        if (width != null && height != null) {
                            Bitmap.createScaledBitmap(bitmap, width, height, true)
                        } else {
                            bitmap
                        }

                    val outputFile =
                        File(context.cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outputFile).use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    }

                    if (scaledBitmap != bitmap) scaledBitmap.recycle()
                    bitmap.recycle()

                    outputFile.absolutePath
                } catch (e: Exception) {
                    throw VideoException("Error generating thumbnail: ${e.message}", e)
                } finally {
                    retriever.release()
                }
            }
    }
}

class VideoException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
