/*
 * Copyright (C) 2023 XdevL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.xdevl.wallpaper.nexus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.service.wallpaper.WallpaperService
import android.util.SizeF
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

class NexusWallpaper : WallpaperService() {

    enum class Background(val resId: Int) {
        ORIGINAL(R.drawable.original_background), ALTERNATIVE(R.drawable.alternative_background)
    }

    private val refreshRateMillis = 40L
    private lateinit var model: NexusModel
    private lateinit var holder: SurfaceHolder
    private lateinit var background: Bitmap

    @Override
    override fun onCreateEngine(): Engine = object : Engine() {

        lateinit var renderingScope: CoroutineScope

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            Timber.d("onCreate()")
            val preferences = PreferenceManager.getDefaultSharedPreferences(this@NexusWallpaper)
            background = Background.valueOf(preferences.getString(getString(R.string.key_background), getString(R.string.background_value_original))!!).let {
                BitmapFactory.decodeResource(resources, it.resId)
            }

            val colors = (preferences.getStringSet(getString(R.string.key_colors), null) ?: setOf(*resources.getStringArray(R.array.default_colors))).map {
                Color.parseColor(it).withAlpha(0x88)
            }

            model = NexusModel(0, 0, colors)

            surfaceHolder.setSizeFromLayout()
            holder = surfaceHolder
        }

        override fun onDestroy() {
            Timber.d("onDestroy()")
            renderingScope.cancel()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            Timber.d("onVisibilityChanged(visible = $visible)")
            if (visible) {
                renderingScope = CoroutineScope(Dispatchers.Default).apply {
                    launch {
                        while (isActive) {
                            renderFrame(holder)
                            delay(refreshRateMillis)
                        }
                    }
                }
            } else {
                renderingScope.cancel()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            Timber.d("onSurfaceChanged(format = $format, width = $width, height = $height)")
            model.width = width
            model.height = height
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            Timber.d("onOffsetsChanged(xOffset = $xOffset, yOffset = $yOffset, xOffsetStep = $xOffsetStep, yOffsetStep = $yOffsetStep, xPixelOffset = $xPixelOffset, yPixelOffset = $yPixelOffset)")
        }
    }

    private fun renderFrame(surfaceHolder: SurfaceHolder) {
        surfaceHolder.lockCanvas().apply {
            drawBitmap(background, Rect(0, 0, background.width, background.height), Rect(0, 0, width, height), null)

            model.update(refreshRateMillis)

            model.pulses.forEach {
                save()
                rotate(it.rotation.degrees, model.rect.width / 2f, model.rect.height / 2f)

                val rect = it.normalizedRect
                drawParticle(
                    model.rect.width / 2 + rect.left,
                    model.rect.height / 2 + rect.top,
                    SizeF(rect.width, rect.height),
                    it.color
                )
                restore()
            }
            surfaceHolder.unlockCanvasAndPost(this)
        }
    }

    private fun Canvas.drawParticle(x: Float, y: Float, size: SizeF, color: Int) {
        val glowRadius = size.height / 2
        // We want the trace to fill the scaled down version of the inner square of the glow
        val traceHeight = sqrt((glowRadius * 2).pow(2f) / 2f) * 0.50f

        drawLinearGradient(
            x,
            y + glowRadius - traceHeight / 2,
            SizeF(size.width - (glowRadius - traceHeight / 2), traceHeight),
            color
        )

        drawRadialGradient(
            x + size.width - glowRadius * 2,
            y,
            glowRadius * 2,
            color
        )
    }

    private fun Canvas.drawRadialGradient(x: Float, y: Float, size: Float, color: Int) {
        val radius = size / 2f
        drawCircle(x + radius, y + radius, radius, Paint().apply {
            isDither = true
            shader = RadialGradient(
                x + radius, y + radius, radius, color, 0, Shader.TileMode.CLAMP
            )
        })
    }

    private fun Canvas.drawLinearGradient(x: Float, y: Float, size: SizeF, color: Int) {
        drawRect(x, y, x + size.width, y + size.height, Paint().apply {
            isDither = true
            shader = LinearGradient(
                x, y, x + size.width, y + size.height, intArrayOf(0, color), null, Shader.TileMode.CLAMP
            )
        })
    }

    private fun Int.withAlpha(alpha: Int): Int = and(0xFFFFFF).or(alpha.shl(24))
}






