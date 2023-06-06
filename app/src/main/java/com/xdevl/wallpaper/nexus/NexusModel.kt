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

import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class Rotation(val degrees: Float) {

    private val cos = cos(Math.toRadians(degrees.toDouble())).toFloat()
    private val sin = sin(Math.toRadians(degrees.toDouble())).toFloat()

    fun rotate(x: Float, y: Float): Pair<Float, Float> = (x * cos - y * sin) to (x * sin + y * cos)

    fun negate(): Rotation = Rotation(degrees.unaryMinus())
}

val RectF.width get() = right - left
val RectF.height get() = bottom - top

fun RectF.rotate(rotation: Rotation): RectF {
    val (x1, y1) = rotation.rotate(left, top)
    val (x2, y2) = rotation.rotate(right, bottom)
    return RectF(min(x1, x2), min(y1,y2), max(x1, x2), max(y1, y2))
}

fun RectF.scale(ratio: Float): RectF {
    return RectF(left * ratio, top * ratio, right * ratio, bottom * ratio)
}

fun RectF.translate(x: Float, y: Float): RectF {
    return RectF(x + left, y + top, x + right, y + bottom)
}

fun RectF.toRect(): Rect {
    return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt());
}

data class Pulse(val width: Int, val height: Int, val color: Int, val speed: Float, val rotation: Rotation) {

    private var normalizedX: Float = 0f
    private var normalizedY: Float = 0f

    val normalizedRect: RectF get() = RectF(normalizedX, normalizedY, normalizedX + width, normalizedY + height)

    val rect: RectF get() = normalizedRect.rotate(rotation)

    fun update(elapsedMillis: Long) {
        normalizedX += speed * elapsedMillis / 1000f
    }

    fun intersects(rect: RectF): Boolean {
        return RectF.intersects(this.rect, rect)
    }

    fun setPosition(x: Float, y: Float) {
        RectF(x, y, x + rect.width, y + rect.height).rotate(rotation.negate()).also {
            normalizedX = it.left
            normalizedY = it.top
        }
    }
}

data class NexusModel(var width: Int, var height: Int, var settings: NexusPreferences.NexusSettings) {

    val rect: RectF get() =  RectF(-width / 2f, -height / 2f, width / 2f, height / 2f)

    val pulses = (1..10).map { randomPulse() }.toTypedArray()

    fun update(elapsedMillis: Long) {
        pulses.forEachIndexed { index, pulse ->
            if (!pulse.intersects(rect)) {
                pulses[index] = randomPulse()
            } else {
                pulse.update(elapsedMillis)
            }
        }
    }

    private fun randomX(pulse: Pulse): Float = (rect.left.toInt() ..(rect.right - pulse.rect.width).toInt()).randomOrNull()?.toFloat() ?: rect.left
    private fun randomY(pulse: Pulse): Float = (rect.top.toInt() ..(rect.bottom - pulse.rect.height).toInt()).randomOrNull()?.toFloat() ?: rect.top

    private fun randomPulse(): Pulse {
        val bias = Random.nextFloat()
        return Pulse(
            width = settings.particlePixelWidthRange.valueFromBias(bias),
            height = settings.particlePixelHeightRange.random(),
            // Until we've got dimensions set we make the particles "invisible" to avoid seeing the first ones
            // all starting from the same (0, 0) position, this way particles also start to appear randomly rather
            // than all at once
            color = settings.colors.randomOrNull().takeIf { width != 0 && height != 0 } ?: 0,
            speed = settings.particlePixelSpeedRange.valueFromBias(bias).toFloat(),
            rotation = listOf(Rotation(0f), Rotation(90f), Rotation(180f), Rotation(270f)).random()
        ).also {
            when (it.rotation.degrees) {
                270f -> it.setPosition(randomX(it), rect.bottom - 1)
                180f -> it.setPosition(rect.right - 1, randomY(it))
                90f -> it.setPosition(randomX(it), rect.top + 1 - it.rect.height)
                else -> it.setPosition(rect.left + 1 - it.rect.width, randomY(it))
            }
        }
    }

    private fun IntRange.valueFromBias(bias: Float): Int {
        return (first + (last - first) * bias).toInt()
    }
}