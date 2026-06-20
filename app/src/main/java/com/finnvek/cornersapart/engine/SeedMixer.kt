package com.finnvek.cornersapart.engine

internal object SeedMixer {
    fun index(
        seed: Long,
        salt: Long,
        bound: Int,
    ): Int {
        require(bound > 0) { "Bound must be positive." }
        return mix(seed xor salt).floorMod(bound.toLong()).toInt()
    }

    fun unitInterval(seed: Long): Double = (mix(seed) ushr DOUBLE_FRACTION_SHIFT) * DOUBLE_UNIT

    private fun mix(seed: Long): Long =
        seed
            .let { value -> value xor (value ushr FIRST_MIX_SHIFT) }
            .let { value -> value * FIRST_MIX_MULTIPLIER }
            .let { value -> value xor (value ushr SECOND_MIX_SHIFT) }
            .let { value -> value * SECOND_MIX_MULTIPLIER }
            .let { value -> value xor (value ushr THIRD_MIX_SHIFT) }

    private fun Long.floorMod(divisor: Long): Long = ((this % divisor) + divisor) % divisor

    private const val FIRST_MIX_SHIFT = 33
    private const val SECOND_MIX_SHIFT = 29
    private const val THIRD_MIX_SHIFT = 32
    private const val DOUBLE_FRACTION_SHIFT = 11
    private const val FIRST_MIX_MULTIPLIER = 6_364_136_223_846_793_005L
    private const val SECOND_MIX_MULTIPLIER = 1_442_695_040_888_963_407L
    private const val DOUBLE_UNIT = 1.0 / (1L shl 53)
}
