package com.xdevl.wallpaper.nexus

import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class NexusModelTest {

    private val preferences = NexusPreferences(ApplicationProvider.getApplicationContext())

    @Test
    fun testRect90Rotation() {
        val rect = RectF(0f, 0f, 20f, 10f)

        val rotated = rect.rotate(Rotation(90f))

        assertThat(rotated.left).isEqualTo(-10)
        assertThat(rotated.top).isZero()
        assertThat(rotated.right).isZero()
        assertThat(rotated.bottom).isEqualTo(20f)
    }

    @Test
    fun testRect180Rotation() {
        val rect = RectF(0f, 0f, 20f, 10f)

        val rotated = rect.rotate(Rotation(180f))

        assertThat(rotated.left).isEqualTo(-20f)
        assertThat(rotated.top).isEqualTo(-10f)
        assertThat(rotated.right).isZero()
        assertThat(rotated.bottom).isZero()
    }

    @Test
    fun testRect270Rotation() {
        val rect = RectF(0f, 0f, 20f, 10f)

        val rotated = rect.rotate(Rotation(270f))

        assertThat(rotated.left).isZero()
        assertThat(rotated.top).isEqualTo(-20f)
        assertThat(rotated.right).isEqualTo(10f)
        assertThat(rotated.bottom).isZero()
    }

    @Test
    fun testAllPulseAreVisible() {
        val model = NexusModel(1000, 400, preferences.nexusSettings)
        val existingPulses = model.pulses.toSet()

        model.update(1000)

        assertThat(model.pulses.toSet()).isEqualTo(existingPulses)
    }

    @Test
    fun testAllPulseAreTransparentWhenDimensionsArentSet() {
        val model = NexusModel(0, 0, preferences.nexusSettings)

        model.update(1000)

        assertThat(model.pulses.find { it.color != 0 }).isNull()
    }

    @Test
    fun testLeftToRightPulse() {
        val model = NexusModel(1000, 400, preferences.nexusSettings)
        val pulse = Pulse(400, 600, 0xFF00FF, 1f, Rotation(0f)).apply {
            setPosition(model.rect.left - width + 1, 0f)
        }
        model.pulses[0] = pulse

        val startPosition = pulse.rect.left
        for (i in 1 until (model.rect.width + pulse.rect.width).roundToInt()) {
            model.update(1000)
            pulse.assertPulsePosition(startPosition + i, 0f)
        }

        model.update(1)
        assertThat(model.pulses[0]).isNotSameInstanceAs(pulse)
    }

    @Test
    fun testRightToLeftPulse() {
        val model = NexusModel(1000, 400, preferences.nexusSettings)
        val pulse = Pulse(400, 600, 0xFF00FF, 1f, Rotation(180f)).apply {
            setPosition(model.rect.right - 1, 0f)
        }
        model.pulses[0] = pulse

        val startPosition = pulse.rect.left
        for (i in 1 until (model.rect.width + pulse.rect.width).roundToInt()) {
            model.update(1000)
            pulse.assertPulsePosition(startPosition - i, 0f)
        }

        model.update(1)
        assertThat(model.pulses[0]).isNotSameInstanceAs(pulse)
    }

    @Test
    fun testTopToBottomPulse() {
        val model = NexusModel(1000, 400, preferences.nexusSettings)
        val pulse = Pulse(400, 600, 0xFF00FF, 1f, Rotation(90f)).apply {
            setPosition(0f, model.rect.top - width + 1)
        }

        model.pulses[0] = pulse

        val startPosition = pulse.rect.top
        for (i in 1 until (model.rect.height + pulse.rect.height).roundToInt()) {
            model.update(1000)
            pulse.assertPulsePosition(0f, startPosition + i)
        }

        model.update(1)
        assertThat(model.pulses[0]).isNotSameInstanceAs(pulse)
    }

    @Test
    fun testBottomToTopPulse() {
        val model = NexusModel(1000, 400, preferences.nexusSettings)
        val pulse = Pulse(400, 600, 0xFF00FF, 1f, Rotation(270f)).apply {
            setPosition(0f, model.rect.bottom - 1)
        }
        model.pulses[0] = pulse

        val startPosition = pulse.rect.top
        for (i in 1 until (model.rect.height + pulse.rect.height).roundToInt()) {
            model.update(1000)
            pulse.assertPulsePosition(0f, startPosition - i)
        }

        model.update(1)
        assertThat(model.pulses[0]).isNotSameInstanceAs(pulse)
    }

    private fun Pulse.assertPulsePosition(x: Float, y: Float) {
        // TODO: round to first decimal here rather than truncating to Int
        assertThat(rect.left.roundToInt()).isEqualTo(x.roundToInt())
        assertThat(rect.top.roundToInt()).isEqualTo(y.roundToInt())
    }

}