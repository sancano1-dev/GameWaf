package com.woofbattle.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { TAP, FLIP, PLACE, BARK, WIN, SAD, VICTORY }

/**
 * All sounds are synthesized at startup (original, no third-party audio), written once to the
 * app cache as WAV files and played through SoundPool for low latency. Every [SoundTheme] has
 * its own set; switching theme is instant.
 */
class SoundPlayer(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = mutableMapOf<Pair<SoundTheme, Sfx>, Int>()

    @Volatile var enabled = true
    @Volatile var theme = SoundTheme.BIG_DOG

    init {
        val dir = File(context.cacheDir, "sfx-v2").apply { mkdirs() }
        SoundTheme.entries.forEach { theme ->
            Sfx.entries.forEach { sfx ->
                val file = File(dir, "${theme.name.lowercase()}_${sfx.name.lowercase()}.wav")
                if (!file.exists()) file.writeBytes(wav(Synth.render(theme, sfx)))
                ids[theme to sfx] = pool.load(file.path, 1)
            }
        }
    }

    fun play(sfx: Sfx, force: Boolean = false) {
        if (!enabled && !force) return
        ids[theme to sfx]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun release() = pool.release()

    private fun wav(samples: ShortArray): ByteArray {
        val dataLen = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()).putInt(36 + dataLen).put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(Synth.RATE).putInt(Synth.RATE * 2).putShort(2).putShort(16)
        buf.put("data".toByteArray()).putInt(dataLen)
        samples.forEach { buf.putShort(it) }
        return buf.array()
    }
}

internal object Synth {
    const val RATE = 22050
    private val rng = Random(7)

    fun render(theme: SoundTheme, sfx: Sfx): ShortArray = toPcm(
        when (theme) {
            SoundTheme.BIG_DOG -> bigDog(sfx)
            SoundTheme.PUPPY -> puppy(sfx)
            SoundTheme.SILLY -> silly(sfx)
        },
    )

    /** Deep "woof woof", warm low chimes. */
    private fun bigDog(sfx: Sfx) = when (sfx) {
        Sfx.TAP -> tone(660.0, 0.08, decay = 35.0)
        Sfx.FLIP -> swoosh(0.16)
        Sfx.PLACE -> swoosh(0.09, bright = 0.3)
        Sfx.BARK -> concat(bark(0.22, 360.0, noise = 0.45), silence(0.1), bark(0.26, 320.0, noise = 0.45))
        Sfx.WIN -> arpeggio(listOf(440.0, 554.4, 659.3), 0.1, decay = 10.0)
        Sfx.SAD -> concat(tone(330.0, 0.16, decay = 7.0), tone(247.0, 0.26, decay = 5.0))
        Sfx.VICTORY -> concat(
            arpeggio(listOf(392.0, 493.9, 587.3, 784.0), 0.13, decay = 6.0),
            tone(784.0, 0.12, decay = 4.0), tone(987.8, 0.5, decay = 3.0),
        )
    }

    /** High little "yip yip yip", bright sparkly chimes. */
    private fun puppy(sfx: Sfx) = when (sfx) {
        Sfx.TAP -> tone(1320.0, 0.06, decay = 45.0)
        Sfx.FLIP -> swoosh(0.12, bright = 0.35)
        Sfx.PLACE -> swoosh(0.07, bright = 0.5)
        Sfx.BARK -> concat(bark(0.1, 1100.0, noise = 0.2), silence(0.06), bark(0.1, 1200.0, noise = 0.2), silence(0.06), bark(0.13, 1000.0, noise = 0.2))
        Sfx.WIN -> arpeggio(listOf(1046.5, 1318.5, 1568.0, 2093.0), 0.07, decay = 14.0)
        Sfx.SAD -> concat(tone(880.0, 0.12, decay = 9.0), tone(660.0, 0.2, decay = 7.0))
        Sfx.VICTORY -> concat(
            arpeggio(listOf(1046.5, 1318.5, 1568.0, 2093.0, 1568.0, 2093.0), 0.09, decay = 7.0),
            tone(2637.0, 0.45, decay = 4.0),
        )
    }

    /** Cartoon: boing barks, slide whistles, kazoo fanfare. */
    private fun silly(sfx: Sfx) = when (sfx) {
        Sfx.TAP -> pop(0.06)
        Sfx.FLIP -> slide(500.0, 1100.0, 0.12)
        Sfx.PLACE -> slide(700.0, 400.0, 0.08)
        Sfx.BARK -> concat(boing(0.3, 220.0), silence(0.05), boing(0.3, 300.0))
        Sfx.WIN -> slide(400.0, 1600.0, 0.35, vibrato = 0.06)
        Sfx.SAD -> slide(700.0, 180.0, 0.55, vibrato = 0.04)
        Sfx.VICTORY -> concat(
            *listOf(392.0, 392.0, 523.3, 659.3, 784.0).map { kazoo(it, 0.14) }.toTypedArray(),
            kazoo(1046.5, 0.5),
        )
    }

    // ---------- Building blocks ----------

    private fun samples(sec: Double) = DoubleArray((sec * RATE).toInt())

    private fun silence(sec: Double) = samples(sec)

    /** Soft "bell-ish" tone: sine plus a little octave, fast attack, exponential decay. */
    private fun tone(freq: Double, sec: Double, decay: Double): DoubleArray {
        val out = samples(sec)
        for (i in out.indices) {
            val t = i.toDouble() / RATE
            val env = min(1.0, t * 200) * exp(-decay * t)
            out[i] = env * (0.75 * sin(2 * PI * freq * t) + 0.25 * sin(4 * PI * freq * t))
        }
        return out
    }

    private fun arpeggio(freqs: List<Double>, step: Double, decay: Double) =
        concat(*freqs.map { tone(it, step, decay) }.toTypedArray())

    /** Filtered noise burst, like a card sliding. */
    private fun swoosh(sec: Double, bright: Double = 0.15): DoubleArray {
        val out = samples(sec)
        var lp = 0.0
        for (i in out.indices) {
            val p = i.toDouble() / out.size
            lp += bright * ((rng.nextDouble() * 2 - 1) - lp)
            out[i] = lp * sin(PI * p) * 2.2
        }
        return out
    }

    /** Cartoon bark: falling buzzy pitch with harmonics and a breath of noise. */
    private fun bark(sec: Double, startFreq: Double, noise: Double = 0.35): DoubleArray {
        val out = samples(sec)
        var phase = 0.0
        var lp = 0.0
        for (i in out.indices) {
            val p = i.toDouble() / out.size
            val freq = startFreq * (1.0 - 0.45 * p)
            phase += 2 * PI * freq / RATE
            val buzz = sin(phase) + 0.5 * sin(2 * phase) + 0.3 * sin(3 * phase) + 0.15 * sin(5 * phase)
            lp += 0.35 * ((rng.nextDouble() * 2 - 1) - lp)
            val env = min(1.0, p * 25) * exp(-4.5 * p)
            out[i] = env * (0.55 * buzz + noise * lp)
        }
        return out
    }

    /** Springy "boing": pitch wobbles around [base] while decaying. */
    private fun boing(sec: Double, base: Double): DoubleArray {
        val out = samples(sec)
        var phase = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / RATE
            val freq = base * (1.0 + 0.6 * exp(-8 * t) * sin(2 * PI * 14 * t)) + base * 0.8 * exp(-12 * t)
            phase += 2 * PI * freq / RATE
            out[i] = min(1.0, t * 300) * exp(-6 * t) * (sin(phase) + 0.3 * sin(2 * phase))
        }
        return out
    }

    /** Slide whistle from [from] to [to] Hz. */
    private fun slide(from: Double, to: Double, sec: Double, vibrato: Double = 0.0): DoubleArray {
        val out = samples(sec)
        var phase = 0.0
        for (i in out.indices) {
            val p = i.toDouble() / out.size
            val t = i.toDouble() / RATE
            val freq = (from + (to - from) * p) * (1 + vibrato * sin(2 * PI * 7 * t))
            phase += 2 * PI * freq / RATE
            out[i] = sin(PI * p).let { if (it < 0) 0.0 else it } * sin(phase)
        }
        return out
    }

    /** Buzzy kazoo note (soft square wave with vibrato). */
    private fun kazoo(freq: Double, sec: Double): DoubleArray {
        val out = samples(sec)
        var phase = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / RATE
            phase += 2 * PI * freq * (1 + 0.015 * sin(2 * PI * 6 * t)) / RATE
            val sq = sign(sin(phase)) * 0.5 + 0.5 * sin(phase)
            val env = min(1.0, t * 80) * min(1.0, (sec - t) * 40)
            out[i] = env * sq * 0.7
        }
        return out
    }

    /** Short bubbly pop. */
    private fun pop(sec: Double): DoubleArray {
        val out = samples(sec)
        var phase = 0.0
        for (i in out.indices) {
            val p = i.toDouble() / out.size
            phase += 2 * PI * (300.0 + 900.0 * p) / RATE
            out[i] = exp(-5 * p) * sin(phase)
        }
        return out
    }

    private fun concat(vararg parts: DoubleArray): DoubleArray {
        val out = DoubleArray(parts.sumOf { it.size })
        var pos = 0
        parts.forEach { it.copyInto(out, pos); pos += it.size }
        return out
    }

    private fun toPcm(data: DoubleArray): ShortArray {
        val peak = data.maxOfOrNull { abs(it) }?.takeIf { it > 0 } ?: 1.0
        return ShortArray(data.size) { (data[it] / peak * 0.8 * Short.MAX_VALUE).toInt().toShort() }
    }
}
