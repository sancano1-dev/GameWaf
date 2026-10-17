package com.woofbattle.engine

/**
 * Small deterministic RNG (SplitMix64). Its whole state is one [Long], so it can be stored
 * inside [GameState] and survive serialization, which kotlin.random.Random cannot.
 */
class SeededRng(state: Long) {
    var state: Long = state
        private set

    fun nextLong(): Long {
        state += -0x61C8864680B583EBL // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** Uniform-enough int in [0, bound). Modulo bias is negligible for our tiny bounds. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** In-place Fisher–Yates shuffle. */
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}
