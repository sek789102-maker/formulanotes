package com.sek789102.jotwai.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import com.sek789102.jotwai.util.NoteImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Small in-memory cache so scrolling the note list does not decode photos again and again. */
object ThumbCache {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, b: Bitmap) { cache.put(key, b) }
}

@Composable
fun NoteImage(
    fileName: String,
    maxPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val key = "$fileName@$maxPx"
    val bitmap: ImageBitmap? by produceState(ThumbCache.get(key)?.asImageBitmap(), key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                NoteImageStorage.decodeSampledBitmap(NoteImageStorage.resolveImageFile(context, fileName), maxPx)
                    ?.also { ThumbCache.put(key, it) }
            }?.asImageBitmap()
        }
    }
    val b = bitmap
    if (b != null) {
        Image(bitmap = b, contentDescription = contentDescription, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
