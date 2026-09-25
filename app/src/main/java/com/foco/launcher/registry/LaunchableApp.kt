package com.foco.launcher.registry

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Bitmap,
    val component: ComponentName?,
)

/** Home tiles are 48dp. 192px covers xxxhdpi without pinning the full adaptive-icon buffer. */
internal const val ICON_BITMAP_MAX_PX = 192

internal fun Drawable.toBitmapCached(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap.downscaledIcon(owned = false)
    }
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val created = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(created)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return created.downscaledIcon(owned = true)
}

private fun Bitmap.downscaledIcon(owned: Boolean): Bitmap {
    val maxEdge = maxOf(width, height)
    if (maxEdge <= ICON_BITMAP_MAX_PX || width <= 0 || height <= 0) return this
    val scale = ICON_BITMAP_MAX_PX.toFloat() / maxEdge.toFloat()
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    val scaled = runCatching { Bitmap.createScaledBitmap(this, w, h, true) }.getOrNull() ?: return this
    if (owned && scaled != this) recycle()
    return scaled
}
