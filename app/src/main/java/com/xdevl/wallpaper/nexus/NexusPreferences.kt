package com.xdevl.wallpaper.nexus
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import androidx.preference.PreferenceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NexusPreferences(private val context: Context) {

    data class NexusSettings(
        val background: String,
        val colors: Collection<Int>,
        val particlePixelWidthRange: IntRange,
        val particlePixelHeightRange: IntRange,
        val particlePixelSpeedRange: IntRange
    )

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val backgroundKey: String get() = context.getString(R.string.key_background)
    private val background: String get() = prefs.getString(backgroundKey, context.getString(R.string.background_value_original))!!

    private val colorsKey: String get() = context.getString(R.string.key_colors)
    private val colors: Collection<Int> get() = (prefs.getStringSet(colorsKey, null) ?: setOf(*context.resources.getStringArray(R.array.default_colors))).map {
        Color.parseColor(it).withAlpha(0x88)
    }

    val nexusSettings: NexusSettings get() = NexusSettings(background, colors,
        context.resources.getDimensionPixelSize(R.dimen.min_particle_width)..context.resources.getDimensionPixelSize(R.dimen.max_particle_width),
        context.resources.getDimensionPixelSize(R.dimen.min_particle_height)..context.resources.getDimensionPixelSize(R.dimen.max_particle_height),
        context.resources.getDimensionPixelSize(R.dimen.min_particle_speed)..context.resources.getDimensionPixelSize(R.dimen.max_particle_speed)
    )

    val nexusSettingsFlow: Flow<NexusSettings> get() = callbackFlow {
        val listener = OnSharedPreferenceChangeListener { _, _ -> trySendBlocking(nexusSettings) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySendBlocking(nexusSettings)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    private fun Int.withAlpha(alpha: Int): Int = and(0xFFFFFF).or(alpha.shl(24))
}





