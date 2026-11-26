package com.nafis.calories.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface WaterRepository {
    val waterGlasses: StateFlow<Int>

    fun addGlasses(amount: Int = 1)

    /** Re-reads the count, e.g. when the app returns to the foreground on a new day. */
    fun refresh()

    companion object {
        const val ML_PER_GLASS = 250
        const val MAX_GLASSES = 20
    }
}
