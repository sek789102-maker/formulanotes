package com.sek789102.formulabook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object NoteImageStorage {

    private const val DIR_NOTE_IMAGES = "note_images"
    private const val DIR_CAMERA_TEMP = "camera_temp"
    private const val MAX_DIMENSION_PX = 1600
    private const val MAX_BACKUP_IMAGE_BYTES = 15L * 1024L * 1024L
    private const val JPEG_QUALITY = 82

    fun imagesDir(context: Context): File {
        return File(context.filesDir, DIR_NOTE_IMAGES).apply { if (!exists()) mkdirs() }
    }

    fun sanitizeFileName(fileName: String): String? {
        val base = File(fileName.trim()).name
        if (base.isEmpty() || base == "." || base == "..") return null
        return base
    }

    fun resolveImageFile(context: Context, fileName: String): File {
        val safeName = sanitizeFileName(fileName) ?: "invalid_image"
        return File(imagesDir(context), safeName)
    }

    fun createCameraTempUri(context: Context): Pair<File, Uri> {
        val tempDir = File(context.cacheDir, DIR_CAMERA_TEMP).apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDir, "cam_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        return tempFile to uri
    }

    fun decodeSampledBitmap(file: File, maxDimPx: Int): Bitmap? {
        return try {
            if (!file.exists() || !file.isFile || file.length() <= 0L) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var inSampleSize = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / inSampleSize > maxDimPx) {
                inSampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
        } catch (_: Throwable) {
            null
        }
    }

    fun saveCompressedImageFromUri(context: Context, sourceUri: Uri): String? {
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var inSampleSize = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / inSampleSize > MAX_DIMENSION_PX * 2) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            decoded = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return null

            val rotationDegrees = readExifRotationDegrees(context, sourceUri)
            oriented = rotateIfNeeded(decoded, rotationDegrees)
            if (oriented !== decoded) {
                decoded.recycle()
                decoded = null
            }

            scaled = scaleDownIfNeeded(oriented, MAX_DIMENSION_PX)
            if (scaled !== oriented) {
                oriented.recycle()
                oriented = null
            }

            val fileName = "note_${UUID.randomUUID()}.jpg"
            val outFile = resolveImageFile(context, fileName)
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }
            fileName
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { if (scaled != null && !scaled.isRecycled) scaled.recycle() }
            runCatching { if (oriented != null && oriented !== scaled && !oriented.isRecycled) oriented.recycle() }
            runCatching { if (decoded != null && decoded !== oriented && decoded !== scaled && !decoded.isRecycled) decoded.recycle() }
        }
    }

    fun saveCompressedImageFromFile(context: Context, sourceFile: File): String? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return null
        val saved = saveCompressedImageFromUri(context, Uri.fromFile(sourceFile))
        runCatching { sourceFile.delete() }
        return saved
    }

    fun encodeImageBase64(context: Context, fileName: String): String? {
        return try {
            if (sanitizeFileName(fileName) == null) return null
            val file = resolveImageFile(context, fileName)
            if (!file.exists() || !file.isFile || file.length() > MAX_BACKUP_IMAGE_BYTES) return null
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    fun restoreImageFromBase64(context: Context, fileName: String, base64Data: String): Boolean {
        return try {
            if (sanitizeFileName(fileName) == null) return false
            if (base64Data.length > MAX_BACKUP_IMAGE_BYTES * 2) return false
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (bytes.isEmpty() || bytes.size > MAX_BACKUP_IMAGE_BYTES) return false
            val outFile = resolveImageFile(context, fileName)
            if (!outFile.exists()) {
                outFile.writeBytes(bytes)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun deleteImageFiles(context: Context, fileNames: List<String>) {
        fileNames.forEach { name ->
            if (sanitizeFileName(name) == null) return@forEach
            runCatching {
                val file = resolveImageFile(context, name)
                if (file.exists()) file.delete()
            }
        }
    }

    private fun readExifRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun rotateIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest.toFloat()
        val targetW = max(1, (bitmap.width * ratio).roundToInt())
        val targetH = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
