package dev.velocity.oneui.rendering

import kotlin.math.roundToInt

/**
 * Frame-cadence statistics computed from Choreographer callback timestamps.
 *
 * IMPORTANT, honest scope: this measures main-thread vsync callback cadence,
 * a proxy for main-thread congestion in the target process. It is NOT the
 * same metric as SurfaceFlinger's frame deadline accounting. For authoritative
 * jank numbers use `dumpsys gfxinfo` and Perfetto traces (see
 * docs/BENCHMARKING.md); this in-module session exists so a quick before/after
 * signal can be captured without a PC.
 */
data class FrameStats(
    val frameCount: Int,
    val durationMs: Long,
    val medianMs: Double,
    val p90Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
    val missedCount: Int,
    val missedPercent: Double,
) {
    fun toJson(): org.json.JSONObject = org.json.JSONObject()
        .put("frameCount", frameCount)
        .put("durationMs", durationMs)
        .put("medianMs", round2(medianMs))
        .put("p90Ms", round2(p90Ms))
        .put("p99Ms", round2(p99Ms))
        .put("maxMs", round2(maxMs))
        .put("missedCount", missedCount)
        .put("missedPercent", round2(missedPercent))

    companion object {
        /**
         * @param timestamps monotonically increasing frame callback times (ns).
         * A frame gap larger than 1.5x the median interval is counted as a
         * missed callback window (main thread was busy past the expected vsync).
         */
        fun from(timestamps: List<Long>, durationMs: Long): FrameStats {
            val deltas = ArrayList<Long>(timestamps.size - 1)
            for (i in 1 until timestamps.size) {
                deltas.add(timestamps[i] - timestamps[i - 1])
            }
            if (deltas.isEmpty()) {
                return FrameStats(0, durationMs, 0.0, 0.0, 0.0, 0.0, 0, 0.0)
            }
            val deltasMs = deltas.map { it / 1_000_000.0 }.sorted()
            val median = percentile(deltasMs, 0.50)
            val missed = deltasMs.count { it > median * 1.5 }
            return FrameStats(
                frameCount = deltas.size,
                durationMs = durationMs,
                medianMs = median,
                p90Ms = percentile(deltasMs, 0.90),
                p99Ms = percentile(deltasMs, 0.99),
                maxMs = deltasMs.last(),
                missedCount = missed,
                missedPercent = 100.0 * missed / deltas.size,
            )
        }

        private fun percentile(sorted: List<Double>, fraction: Double): Double {
            if (sorted.isEmpty()) return 0.0
            val idx = ((sorted.size - 1) * fraction).roundToInt()
            return sorted[idx.coerceIn(0, sorted.size - 1)]
        }

        private fun round2(v: Double): Double = (v * 100).roundToInt() / 100.0
    }
}
