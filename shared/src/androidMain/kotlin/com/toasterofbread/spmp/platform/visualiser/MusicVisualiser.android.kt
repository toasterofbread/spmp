package com.toasterofbread.spmp.platform.visualiser

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow

// https://github.com/dzolnai/ExoVisualizer
// Modified for use with Compose

// Taken from: https://en.wikipedia.org/wiki/Preferred_number#Audio_frequencies
private val FREQUENCY_BAND_LIMITS = arrayOf(
    20, 25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
    800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
    12500, 16000, 20000
)

@UnstableApi
actual class MusicVisualiser(
    private val processor: FFTAudioProcessor
): FFTAudioProcessor.FFTListener {

    private val max_const = 25_000 // Reference max value for accum magnitude
    private val bands = FREQUENCY_BAND_LIMITS.size
    private val size = FFTAudioProcessor.SAMPLE_SIZE / 2

    // We average out the values over 3 occurences (plus the current one), so big jumps are smoothed out
    private val smoothing_factor = 2
    private val previous_values = FloatArray(bands * smoothing_factor)

    private val fft: FloatArray = FloatArray(size)

    private var invalidate_state: Int by mutableStateOf(0)
    private fun invalidate() {
        invalidate_state++
    }

    override fun onFFTReady(sampleRateHz: Int, channelCount: Int, fft: FloatArray) {
        synchronized(this.fft) {
            System.arraycopy(fft, 2, this.fft, 0, size)
            invalidate()
        }
    }

    @Composable
    actual fun Visualiser(colour: Color, modifier: Modifier, opacity: Float) {
        DisposableEffect(Unit) {
            processor.listeners.add(this@MusicVisualiser)
            onDispose {
                processor.listeners.remove(this@MusicVisualiser)
            }
        }

        Canvas(modifier) {
            invalidate_state

            if (size.height < 10f) {
                return@Canvas
            }

            // Set up counters and widgets
            var currentFftPosition = 0
            var currentFrequencyBandLimitIndex = 0

            // Iterate over the entire FFT result array
            while (currentFftPosition < this@MusicVisualiser.size) {
                var accum = 0f

                // We divide the bands by frequency.
                // Check until which index we need to stop for the current band
                val nextLimitAtPosition = floor(FREQUENCY_BAND_LIMITS[currentFrequencyBandLimitIndex] / 20_000.toFloat() * this@MusicVisualiser.size).toInt()

                synchronized(fft) {
                    // Here we iterate within this single band
                    for (j in 0 until (nextLimitAtPosition - currentFftPosition) step 2) {
                        // Convert real and imaginary part to get energy
                        val raw = (fft[currentFftPosition + j].toDouble().pow(2.0) +
                            fft[currentFftPosition + j + 1].toDouble().pow(2.0)).toFloat()

                        // Hamming window (by frequency band instead of frequency, otherwise it would prefer 10kHz, which is too high)
                        // The window mutes down the very high and the very low frequencies, usually not hearable by the human ear
                        val m = bands / 2
                        val windowed = raw * (0.54f - 0.46f * cos(2 * Math.PI * currentFrequencyBandLimitIndex / (m + 1))).toFloat()
                        accum += windowed
                    }
                }

                // A window might be empty which would result in a 0 division
                if (nextLimitAtPosition - currentFftPosition != 0) {
                    accum /= (nextLimitAtPosition - currentFftPosition)
                }
                else {
                    accum = 0.0f
                }
                currentFftPosition = nextLimitAtPosition

                // Here we do the smoothing
                // If you increase the smoothing factor, the high shoots will be toned down, but the
                // 'movement' in general will decrease too
                var smoothedAccum = accum
                for (i in 0 until smoothing_factor) {
                    smoothedAccum += previous_values[i * bands + currentFrequencyBandLimitIndex]
                    if (i != smoothing_factor - 1) {
                        previous_values[i * bands + currentFrequencyBandLimitIndex] =
                            previous_values[(i + 1) * bands + currentFrequencyBandLimitIndex]
                    }
                    else {
                        previous_values[i * bands + currentFrequencyBandLimitIndex] = accum
                    }
                }
                smoothedAccum /= (smoothing_factor + 1) // +1 because it also includes the current value

                val x = (size.width * ((2f * currentFrequencyBandLimitIndex) + 1)) / (2f * bands.toFloat())
                val r = x > size.width * 0.5f

                val amplitude = (size.height * (smoothedAccum / max_const.toDouble()).coerceAtMost(1.0).toFloat()) * (if (r) 15f else 1f)
                val bar_height = amplitude.coerceIn(10f, size.height)

                val point = Offset(
                    x,
                    (size.height / 2f) - (bar_height * 0.5f)
                )

                drawRoundRect(
                    colour,
                    point,
                    Size(10f, bar_height),
                    cornerRadius = CornerRadius(20f, 20f),
                    alpha = opacity
                )

                currentFrequencyBandLimitIndex++
            }
        }
    }

    actual companion object {
        actual fun isSupported(): Boolean = true
    }
}
