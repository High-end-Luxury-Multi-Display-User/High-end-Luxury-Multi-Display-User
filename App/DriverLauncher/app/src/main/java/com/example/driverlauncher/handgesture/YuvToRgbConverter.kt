package com.example.driverlauncher.handgesture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import android.util.Log
import android.graphics.BitmapFactory

object YuvToRgbConverter {

    fun yuvToRgb(context: Context, image: Image): Bitmap? {
        val nv21 = yuv420ToNv21(image) ?: return null
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        try {
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val jpegBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            Log.e("YuvToRgb", "Error compressing YUV to JPEG", e)
            return null
        } finally {
            out.close()
        }
    }

    private fun yuv420ToNv21(image: Image): ByteArray? {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)

        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        // Check if buffers are accessible
        if (!yBuffer.hasRemaining() || !uBuffer.hasRemaining() || !vBuffer.hasRemaining()) {
            Log.e("YuvToRgb", "Buffer inaccessible")
            return null
        }

        try {
            yBuffer.get(nv21, 0, ySize)
            val rowStride = image.planes[2].rowStride
            val pixelStride = image.planes[2].pixelStride
            var offset = ySize
            for (row in 0 until height / 2) {
                for (col in 0 until width / 2) {
                    val vuPos = row * rowStride + col * pixelStride
                    if (vBuffer.hasRemaining() && uBuffer.hasRemaining()) {
                        nv21[offset++] = vBuffer.get(vuPos)
                        nv21[offset++] = uBuffer.get(vuPos)
                    } else {
                        Log.e("YuvToRgb", "Buffer position out of bounds")
                        return null
                    }
                }
            }
            return nv21
        } catch (e: Exception) {
            Log.e("YuvToRgb", "Error converting YUV to NV21", e)
            return null
        }
    }
}