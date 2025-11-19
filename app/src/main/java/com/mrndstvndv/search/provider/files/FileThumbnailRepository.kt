package com.mrndstvndv.search.provider.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class FileThumbnailRepository private constructor(private val context: Context) {

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_ITEMS) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    suspend fun loadThumbnail(
        uriString: String,
        lastModified: Long,
        type: ThumbnailType
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uriString#$lastModified#${type.name}"
        cache.get(key)?.let { return@withContext it }
        val uri = Uri.parse(uriString)
        val decoded = when (type) {
            ThumbnailType.IMAGE -> decodeImage(uri)
            ThumbnailType.VIDEO -> decodeVideo(uri)
            ThumbnailType.AUDIO -> decodeAudioArt(uri)
        }
        if (decoded != null) {
            cache.put(key, decoded)
        }
        decoded
    }

    private fun decodeImage(uri: Uri): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val (sampleWidth, sampleHeight) = options.outWidth to options.outHeight
            if (sampleWidth <= 0 || sampleHeight <= 0) return null
            val sampleSize = calculateSampleSize(sampleWidth, sampleHeight, TARGET_SIZE)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
            bitmap?.let { scaleToTarget(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeVideo(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.frameAtTime
            retriever.release()
            frame?.let { scaleToTarget(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeAudioArt(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val picture = retriever.embeddedPicture
            retriever.release()
            if (picture != null) {
                BitmapFactory.decodeByteArray(picture, 0, picture.size)?.let { scaleToTarget(it) }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        if (height > target || width > target) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / sample) >= target && (halfWidth / sample) >= target) {
                sample *= 2
            }
        }
        return sample
    }

    private fun scaleToTarget(bitmap: Bitmap): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height).takeIf { it > 0 } ?: return bitmap
        if (maxSide <= TARGET_SIZE) return bitmap
        val scale = TARGET_SIZE.toFloat() / maxSide
        val width = max(1, (bitmap.width * scale).toInt())
        val height = max(1, (bitmap.height * scale).toInt())
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    companion object {
        private const val TARGET_SIZE = 128
        private const val MAX_CACHE_ITEMS = 1024 // measured in KB

        @Volatile
        private var INSTANCE: FileThumbnailRepository? = null

        fun getInstance(context: Context): FileThumbnailRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FileThumbnailRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

enum class ThumbnailType {
    IMAGE,
    VIDEO,
    AUDIO
}
