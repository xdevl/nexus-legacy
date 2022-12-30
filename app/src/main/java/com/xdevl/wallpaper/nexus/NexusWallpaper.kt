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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.service.wallpaper.WallpaperService
import android.util.SizeF
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NexusWallpaper : WallpaperService() {

    private val refreshRateMillis = 40L
    private lateinit var model: NexusModel

    @Override
    override fun onCreateEngine(): Engine = object : Engine() {

        lateinit var renderingScope: CoroutineScope

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            surfaceHolder.setSizeFromLayout()
        }

        override fun onDestroy() {
            super.onDestroy()
            renderingScope.cancel()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            model = NexusModel(holder.surfaceFrame.width(), holder.surfaceFrame.height())
            renderingScope = CoroutineScope(Dispatchers.Default).apply {
                launch {
                    while (isActive) {
                        renderFrame(holder)
                        delay(refreshRateMillis)
                    }
                }
            }
        }
    }

    private fun renderFrame(surfaceHolder: SurfaceHolder) {
        surfaceHolder.lockCanvas().apply {
            drawColor(Color.BLACK)
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
        val traceHeight = size.height * 0.25f

        drawLinearGradient(
            x,
            y + glowRadius - traceHeight / 2,
            SizeF(size.width - glowRadius, traceHeight),
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






