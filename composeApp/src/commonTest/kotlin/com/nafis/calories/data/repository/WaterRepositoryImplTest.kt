package com.nafis.calories.data.repository

import com.nafis.calories.data.sync.SyncTrigger
import com.nafis.calories.testing.FakeKeyValueStoreFactory
import com.nafis.calories.testing.TestClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class WaterRepositoryImplTest {

    private val storeFactory = FakeKeyValueStoreFactory()
    private val clock = TestClock()

    private fun repository() = WaterRepositoryImpl(storeFactory, clock, SyncTrigger())

    @Test
    fun addingGlassesQueuesOneEventPerGlass() {
        val repository = repository()

        repository.addGlasses(2)

        assertEquals(2, repository.waterGlasses.value)
        assertEquals(2, repository.pendingEvents().map { it.clientId }.toSet().size)
    }

    @Test
    fun newDayStartsAtZeroButKeepsPendingEvents() {
        val repository = repository()
        repository.addGlasses(3)

        clock.advanceBy(1.days)
        repository.refresh()

        assertEquals(0, repository.waterGlasses.value)
        assertEquals(3, repository.pendingEvents().size)
    }

    @Test
    fun legacyTimestampOnlyEventsGetStableIds() {
        storeFactory.store("water_tracker").values["pending_events"] = "1764000000000,1764000060000"
        val repository = repository()

        val first = repository.pendingEvents()
        val second = repository.pendingEvents()

        assertEquals(listOf("water-1764000000000-0", "water-1764000060000-1"), first.map { it.clientId })
        assertEquals(first, second)
    }

    @Test
    fun serverTotalIsIgnoredWhileEventsArePending() {
        val repository = repository()
        repository.addGlasses(1)

        repository.applyServerToday(date = null, totalGlasses = 6)

        assertEquals(1, repository.waterGlasses.value)
    }
}