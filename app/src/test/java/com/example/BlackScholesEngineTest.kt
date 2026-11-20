package com.example

import com.example.domain.BlackScholesEngine
import com.example.domain.InstrumentType
import com.example.domain.Side
import com.example.domain.StrategyCatalog
import com.example.ui.StrikeDeskViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackScholesEngineTest {

    @Test
    fun `put call parity holds closely`() {
        val spot = 2000.0
        val strike = 2000.0
        val t = 30.0 / 365.0
        val r = 0.065
        val iv = 0.22

        val call = BlackScholesEngine.priceInstrument(InstrumentType.CE, spot, strike, t, r, iv)
        val put = BlackScholesEngine.priceInstrument(InstrumentType.PE, spot, strike, t, r, iv)
        val rhs = spot - strike * kotlin.math.exp(-r * t)
        assertEquals(rhs, call.price - put.price, 1e-4)
        assertEquals(1.0, call.delta - put.delta, 1e-4)
    }

    @Test
    fun `tcs preset loads calendar plus double calendar and preserves entry prices on spot jump and add leg`() {
        val vm = StrikeDeskViewModel()
        val initial = vm.uiState.value
        assertEquals("TCS", initial.params.underlyingSymbol)
        assertEquals(2000.0, initial.params.simulatedSpot, 0.01)
        assertEquals(6, initial.legs.size) // 2 legs Call Calendar + 4 legs Double Calendar

        val firstLegOriginalEntry = initial.legs.first().entryPrice

        // Step 2: Spot Jump to 2500 + 1 Day
        vm.runTcsStep2SpotJumpAndNextDay()
        val afterJump = vm.uiState.value
        assertEquals(2500.0, afterJump.params.simulatedSpot, 0.01)
        assertEquals(1, afterJump.params.simulationDayOffset)
        assertEquals(firstLegOriginalEntry, afterJump.legs.first().entryPrice, 1e-6)

        // Step 3: Add 2500 CE Leg while preserving original legs
        vm.runTcsStep3AddAdjustmentLeg()
        val afterAdd = vm.uiState.value
        assertEquals(7, afterAdd.legs.size)
        assertEquals(firstLegOriginalEntry, afterAdd.legs.first().entryPrice, 1e-6)
        assertEquals(2500.0, afterAdd.legs.last().strike, 0.01)
        assertEquals(1, afterAdd.legs.last().addedAtDay)
    }

    @Test
    fun `adding multiple strategies appends without wiping existing basket`() {
        val vm = StrikeDeskViewModel()
        vm.clearAllLegs()
        assertEquals(0, vm.uiState.value.legs.size)

        val ironCondor = StrategyCatalog.templates.first { it.id == "iron_condor" }
        val calendar = StrategyCatalog.templates.first { it.id == "call_calendar_spread" }

        vm.addStrategyToBasket(ironCondor, replaceExisting = false)
        assertEquals(4, vm.uiState.value.legs.size)
        assertFalse(vm.uiState.value.analysis.isMaxProfitUnlimited)
        assertFalse(vm.uiState.value.analysis.isMaxLossUnlimited)

        vm.addStrategyToBasket(calendar, replaceExisting = false)
        assertEquals(6, vm.uiState.value.legs.size)
        assertTrue(vm.uiState.value.analysis.curve.isNotEmpty())
    }

    @Test
    fun `long call detects unlimited max profit`() {
        val vm = StrikeDeskViewModel()
        vm.clearAllLegs()
        val longCall = StrategyCatalog.templates.first { it.id == "long_call" }
        vm.addStrategyToBasket(longCall, replaceExisting = true)
        assertTrue(vm.uiState.value.analysis.isMaxProfitUnlimited)
        assertFalse(vm.uiState.value.analysis.isMaxLossUnlimited)
    }
}
