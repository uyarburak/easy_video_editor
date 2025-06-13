package com.example.easy_video_editor.handler

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.example.easy_video_editor.command.Command
import com.example.easy_video_editor.utils.OperationManager
import com.example.easy_video_editor.utils.VideoUtils
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MaxFpsCommand(private val context: Context) : Command {
    @UnstableApi
    override fun execute(call: MethodCall, result: MethodChannel.Result) {
        val videoPath = call.argument<String>("videoPath")
        val maxFps = call.argument<Number>("maxFps")?.toInt()

        if (videoPath == null || maxFps == null) {
            result.error(
                "INVALID_ARGUMENTS",
                "Missing required arguments: videoPath or maxFps",
                null
            )
            return
        }

        // Create a new scope that's tied only to this method call
        val methodScope = CoroutineScope(Dispatchers.Main + Job())

        // Register with operation manager for cancellation support
        val operationId = OperationManager.generateOperationId()
        OperationManager.registerOperation(operationId, methodScope)
        
        methodScope.launch {
            try {
                val outputPath = VideoUtils.setMaxFps(
                    context = context,
                    videoPath = videoPath,
                    maxFps = maxFps
                )
                result.success(outputPath)
            } catch (e: Exception) {
                result.error("MAX_FPS_ERROR", e.message, null)
            } finally {
                OperationManager.cancelOperation(operationId)
                methodScope.cancel()
            }
        }
    }
} 