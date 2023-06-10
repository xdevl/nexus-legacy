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
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.SizeF
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class NexusWallpaper : WallpaperService() {

    enum class Background(val resId: Int) {
        ORIGINAL(R.drawable.original_background), ALTERNATIVE(R.drawable.alternative_background)
    }

    // Note: the wallpaper service can create several instances of the Engine
    @Override
    override fun onCreateEngine(): Engine = object : Engine() {

        private val preferences = NexusPreferences(this@NexusWallpaper)
        private val refreshRateMillis = 20L
        private var xOffsetRatio = 0f // 0 is most left, 1 is most right
        private var yOffsetRatio = 0f // 0 is most top, 1 is most bottom
        private lateinit var model: NexusModel
        private lateinit var holder: SurfaceHolder
        private var background: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        private var renderingScope = CoroutineScope(EmptyCoroutineContext)


        override fun onCreate(hodler: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            if (Timber.treeCount == 0) {
                Timber.plant(Timber.DebugTree())
            }

            Timber.d("onCreate(holder = $surfaceHolder)")
            model = NexusModel(0, 0, preferences.nexusSettings)

            surfaceHolder.setSizeFromLayout()
            holder = surfaceHolder
        }

        override fun onDestroy() {
            super.onDestroy()
            Timber.d("onDestroy()")
            renderingScope.cancel()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Timber.d("onVisibilityChanged(visible = $visible)")
            if (visible) {
                renderingScope = CoroutineScope(Dispatchers.Default).apply {
                    launch {
                        preferences.nexusSettingsFlow.collect {
                            background = BitmapFactory.decodeResource(resources, Background.valueOf(it.background).resId)
                            model.settings = it
                        }
                    }
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

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Timber.d("onSurfaceCreated(holder = $holder)")
            this.holder = holder
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Timber.d("onSurfaceChanged(holder = $holder, format = $format, width = $width, height = $height)")
            this.holder = holder
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            Timber.d("onOffsetsChanged(xOffset = $xOffset, yOffset = $yOffset, xOffsetStep = $xOffsetStep, yOffsetStep = $yOffsetStep, xPixelOffset = $xPixelOffset, yPixelOffset = $yPixelOffset)")
            xOffsetRatio = xOffset
            yOffsetRatio = yOffset
        }

        private fun renderFrame(surfaceHolder: SurfaceHolder) {
            surfaceHolder.safeLockHardwareCanvas { canvas ->

                model.width = 2 * canvas.width
                model.height = canvas.height

                model.update(refreshRateMillis)

                val ratio = max(model.width.toFloat() / background.width, model.height.toFloat() / background.height)
                val bgLeftOffset = (background.width * ratio - model.width) / 2
                val bgTopOffset = (background.height * ratio - model.height) / 2

                val xOffset = xOffsetRatio * (model.width - canvas.width)
                val yOffset = yOffsetRatio * (model.height - canvas.height)

                canvas.drawBitmap(
                    background,
                    RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
                        .translate(bgLeftOffset + xOffset, bgTopOffset + yOffset)
                        .scale(1 / ratio)
                        .toRect(),
                    Rect(0, 0, canvas.width, canvas.height), null
                )

                model.pulses.forEach {
                    canvas.save()
                    canvas.translate(-xOffset, -yOffset)
                    canvas.rotate(it.rotation.degrees, model.rect.width / 2f, model.rect.height / 2f)

                    val rect = it.normalizedRect
                    canvas.drawParticle(
                        (model.rect.width / 2 + rect.left),
                        (model.rect.height / 2 + rect.top),
                        SizeF(rect.width, rect.height),
                        it.color
                    )
                    canvas.restore()
                }
            }
        }
    }

    private fun SurfaceHolder.safeLockHardwareCanvas(block: (Canvas) -> Unit) {
        if (surface.isValid) {
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                lockHardwareCanvas()
            } else {
                lockCanvas()
            }
            try {
                block(canvas)
            } finally {
                unlockCanvasAndPost(canvas)
            }
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
}






