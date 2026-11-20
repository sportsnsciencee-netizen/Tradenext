package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.domain.BlackScholesEngine
import com.example.domain.InstrumentType
import com.example.domain.OptionLeg
import com.example.domain.PayoffAnalysis
import com.example.domain.Side
import com.example.domain.SimulationParams
import com.example.domain.StrategyCatalog
import com.example.domain.StrategyTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

data class StrikeDeskUiState(
    val params: SimulationParams = SimulationParams(),
    val legs: List<OptionLeg> = emptyList(),
    val analysis: PayoffAnalysis = PayoffAnalysis.EMPTY,
    val tcsScenarioStep: Int = 0, // 0 = custom/none, 1 = Initial (2000), 2 = Jump (2500 +1D), 3 = Added Leg
    val statusBanner: String? = null
)

class StrikeDeskViewModel : ViewModel() {

    private val idCounter = AtomicInteger(100)
    private fun nextId(prefix: String): String = "${prefix}_${idCounter.incrementAndGet()}"

    private val _uiState = MutableStateFlow(StrikeDeskUiState())
    val uiState: StateFlow<StrikeDeskUiState> = _uiState.asStateFlow()

    init {
        loadTcsSampleScenario()
    }

    private fun updateAndAnalyze(
        transform: (StrikeDeskUiState) -> StrikeDeskUiState
    ) {
        _uiState.update { current ->
            val updated = transform(current)
            val analysis = BlackScholesEngine.analyzePortfolio(updated.legs, updated.params)
            updated.copy(analysis = analysis)
        }
    }

    fun selectUnderlying(symbol: String) {
        val config = StrategyCatalog.underlyings.find { it.symbol == symbol } ?: return
        updateAndAnalyze { state ->
            val newParams = state.params.copy(
                underlyingSymbol = config.symbol,
                baseSpot = config.defaultSpot,
                simulatedSpot = config.defaultSpot,
                strikeStep = config.strikeStep,
                lotSize = config.defaultLotSize,
                baseIvPct = config.defaultIvPct,
                ivShiftPct = 0.0,
                simulationDayOffset = 0
            )
            state.copy(
                params = newParams,
                legs = emptyList(),
                tcsScenarioStep = 0,
                statusBanner = "Switched underlying to ${config.symbol} (Spot ₹${Formatters.formatStrike(config.defaultSpot)})"
            )
        }
    }

    fun updateSimulatedSpot(spot: Double) {
        val safeSpot = spot.coerceAtLeast(10.0)
        updateAndAnalyze { state ->
            state.copy(
                params = state.params.copy(simulatedSpot = safeSpot),
                statusBanner = null
            )
        }
    }

    fun stepSpot(delta: Double) {
        updateSimulatedSpot(_uiState.value.params.simulatedSpot + delta)
    }

    fun setSimulationDay(dayOffset: Int) {
        updateAndAnalyze { state ->
            val maxDay = state.analysis.nearestExpiryDte.coerceAtLeast(state.params.nearExpiryDte).coerceAtLeast(1)
            val clamped = dayOffset.coerceIn(0, maxDay)
            state.copy(
                params = state.params.copy(simulationDayOffset = clamped),
                statusBanner = null
            )
        }
    }

    fun updateIvShift(ivShiftPct: Double) {
        val clamped = ivShiftPct.coerceIn(-15.0, 40.0)
        updateAndAnalyze { state ->
            state.copy(
                params = state.params.copy(ivShiftPct = clamped),
                statusBanner = null
            )
        }
    }

    fun updateRiskFreeRate(ratePct: Double) {
        val clamped = ratePct.coerceIn(0.0, 20.0)
        updateAndAnalyze { state ->
            state.copy(
                params = state.params.copy(riskFreeRatePct = clamped),
                statusBanner = null
            )
        }
    }

    fun updateLotSize(lotSize: Int) {
        val safeLotSize = lotSize.coerceIn(1, 10000)
        updateAndAnalyze { state ->
            state.copy(
                params = state.params.copy(lotSize = safeLotSize),
                legs = state.legs.map { it.copy(lotSize = safeLotSize) },
                statusBanner = "Lot size updated to $safeLotSize"
            )
        }
    }

    fun updateStrikeStep(step: Double) {
        val safeStep = step.coerceAtLeast(1.0)
        updateAndAnalyze { state ->
            state.copy(
                params = state.params.copy(strikeStep = safeStep)
            )
        }
    }

    fun resetSimulationControls() {
        updateAndAnalyze { state ->
            state.copy(
                params = state.params.copy(
                    simulatedSpot = state.params.baseSpot,
                    ivShiftPct = 0.0,
                    simulationDayOffset = 0
                ),
                statusBanner = "Simulation reset to Base Spot & T+0"
            )
        }
    }

    fun addStrategyToBasket(template: StrategyTemplate, replaceExisting: Boolean = false) {
        updateAndAnalyze { state ->
            val groupId = nextId("grp_${template.id}")
            val existingCount = state.legs.map { it.strategyGroupId }.distinct().size
            val groupLabel = if (replaceExisting || existingCount == 0) {
                template.name
            } else {
                "${template.name} #${existingCount + 1}"
            }
            val newLegs = StrategyCatalog.instantiateTemplate(
                template = template,
                params = state.params,
                groupId = groupId,
                groupName = groupLabel,
                idGenerator = { nextId("leg") }
            )
            val combined = if (replaceExisting) newLegs else state.legs + newLegs
            state.copy(
                legs = combined,
                tcsScenarioStep = 0,
                statusBanner = if (replaceExisting) {
                    "Loaded ${template.name} (${newLegs.size} legs)"
                } else {
                    "Added ${template.name} (${newLegs.size} legs) to Trade Book"
                }
            )
        }
    }

    fun addCustomLegToRunningTrade(
        side: Side,
        type: InstrumentType,
        strike: Double,
        lots: Int,
        expiryDte: Int,
        entryPrice: Double,
        entryIvPct: Double,
        strategyGroupName: String = "Running Trade Adjustments"
    ) {
        updateAndAnalyze { state ->
            val existingAdjGroup = state.legs.firstOrNull { it.strategyGroupName == strategyGroupName }?.strategyGroupId
            val groupId = existingAdjGroup ?: nextId("grp_adj")
            val leg = OptionLeg(
                id = nextId("leg"),
                strategyGroupId = groupId,
                strategyGroupName = strategyGroupName,
                side = side,
                type = type,
                strike = strike,
                lots = lots.coerceAtLeast(1),
                lotSize = state.params.lotSize,
                expiryDte = expiryDte.coerceAtLeast(1),
                entryPrice = entryPrice.coerceAtLeast(0.05),
                entryIvPct = entryIvPct.coerceAtLeast(1.0),
                enabled = true,
                addedAtSpot = state.params.simulatedSpot,
                addedAtDay = state.params.simulationDayOffset
            )
            state.copy(
                legs = state.legs + leg,
                statusBanner = "Added ${side.label} ${lots}L ${Formatters.formatStrike(strike)} ${type.label} @ ${Formatters.formatPrice(entryPrice)}"
            )
        }
    }

    fun updateExistingLeg(updatedLeg: OptionLeg) {
        updateAndAnalyze { state ->
            state.copy(
                legs = state.legs.map { if (it.id == updatedLeg.id) updatedLeg else it },
                statusBanner = "Updated ${updatedLeg.side.label} ${Formatters.formatStrike(updatedLeg.strike)} ${updatedLeg.type.label}"
            )
        }
    }

    fun toggleLegEnabled(legId: String) {
        updateAndAnalyze { state ->
            state.copy(
                legs = state.legs.map { if (it.id == legId) it.copy(enabled = !it.enabled) else it }
            )
        }
    }

    fun toggleStrategyGroupEnabled(groupId: String, enabled: Boolean) {
        updateAndAnalyze { state ->
            state.copy(
                legs = state.legs.map { if (it.strategyGroupId == groupId) it.copy(enabled = enabled) else it }
            )
        }
    }

    fun changeLegLots(legId: String, delta: Int) {
        updateAndAnalyze { state ->
            state.copy(
                legs = state.legs.map { leg ->
                    if (leg.id == legId) {
                        leg.copy(lots = (leg.lots + delta).coerceIn(1, 500))
                    } else {
                        leg
                    }
                }
            )
        }
    }

    fun duplicateLeg(legId: String) {
        updateAndAnalyze { state ->
            val target = state.legs.find { it.id == legId } ?: return@updateAndAnalyze state
            val copy = target.copy(id = nextId("leg"))
            val idx = state.legs.indexOfFirst { it.id == legId }
            val mutable = state.legs.toMutableList()
            mutable.add(idx + 1, copy)
            state.copy(
                legs = mutable,
                statusBanner = "Duplicated ${target.side.label} ${Formatters.formatStrike(target.strike)} ${target.type.label}"
            )
        }
    }

    fun removeLeg(legId: String) {
        updateAndAnalyze { state ->
            state.copy(
                legs = state.legs.filterNot { it.id == legId }
            )
        }
    }

    fun removeStrategyGroup(groupId: String) {
        updateAndAnalyze { state ->
            state.copy(
                legs = state.legs.filterNot { it.strategyGroupId == groupId }
            )
        }
    }

    fun clearAllLegs() {
        updateAndAnalyze { state ->
            state.copy(
                legs = emptyList(),
                tcsScenarioStep = 0,
                statusBanner = "Trade Book cleared"
            )
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    /**
     * One-click Sample Preset Scenario:
     * TCS (Spot ₹2,000) -> Call Calendar Spread (2000 CE) + Double Calendar Spread (1900 PE / 2100 CE)
     */
    fun loadTcsSampleScenario() {
        val tcs = StrategyCatalog.underlyings.first { it.symbol == "TCS" }
        val baseParams = SimulationParams(
            underlyingSymbol = tcs.symbol,
            baseSpot = 2000.0,
            simulatedSpot = 2000.0,
            strikeStep = 50.0,
            lotSize = tcs.defaultLotSize,
            baseIvPct = 22.0,
            ivShiftPct = 0.0,
            riskFreeRatePct = 6.5,
            simulationDayOffset = 0,
            nearExpiryDte = 7,
            farExpiryDte = 35
        )
        val calTemplate = StrategyCatalog.templates.first { it.id == "call_calendar_spread" }
        val dblCalTemplate = StrategyCatalog.templates.first { it.id == "double_calendar_spread" }

        val calLegs = StrategyCatalog.instantiateTemplate(
            template = calTemplate,
            params = baseParams,
            groupId = "grp_tcs_calendar",
            groupName = "TCS Call Calendar (2000 CE)",
            idGenerator = { nextId("tcs_cal") }
        )
        val dblCalLegs = StrategyCatalog.instantiateTemplate(
            template = dblCalTemplate,
            params = baseParams,
            groupId = "grp_tcs_dbl_calendar",
            groupName = "TCS Double Calendar (1900P / 2100C)",
            idGenerator = { nextId("tcs_dbl") }
        )

        updateAndAnalyze {
            StrikeDeskUiState(
                params = baseParams,
                legs = calLegs + dblCalLegs,
                tcsScenarioStep = 1,
                statusBanner = "Loaded TCS Preset: Call Calendar + Double Calendar @ Spot ₹2,000 (T+0)"
            )
        }
    }

    fun runTcsStep2SpotJumpAndNextDay() {
        updateAndAnalyze { state ->
            val currentLegs = if (state.params.underlyingSymbol != "TCS" || state.legs.isEmpty()) {
                loadTcsSampleScenario()
                _uiState.value.legs
            } else {
                state.legs
            }
            val jumpedParams = state.params.copy(
                underlyingSymbol = "TCS",
                baseSpot = 2000.0,
                simulatedSpot = 2500.0,
                simulationDayOffset = 1
            )
            state.copy(
                params = jumpedParams,
                legs = currentLegs,
                tcsScenarioStep = 2,
                statusBanner = "Simulated Spot Jump ₹2,000 → ₹2,500 & +1 Day Decay (T+1)"
            )
        }
    }

    fun runTcsStep3AddAdjustmentLeg() {
        val current = _uiState.value
        val needsJump = current.params.underlyingSymbol != "TCS" || current.params.simulatedSpot < 2400.0
        if (needsJump) {
            runTcsStep2SpotJumpAndNextDay()
        }
        val stateAfterJump = _uiState.value
        val spot = stateAfterJump.params.simulatedSpot
        val simDay = stateAfterJump.params.simulationDayOffset
        val strike = StrategyCatalog.atmStrike(spot, stateAfterJump.params.strikeStep)
        val expiryDte = stateAfterJump.params.nearExpiryDte
        val remDte = (expiryDte - simDay).coerceAtLeast(1)
        val iv = (stateAfterJump.params.baseIvPct + stateAfterJump.params.ivShiftPct).coerceAtLeast(1.0)
        val price = BlackScholesEngine.fairEntryPrice(
            type = InstrumentType.CE,
            spot = spot,
            strike = strike,
            remainingDte = remDte,
            ratePct = stateAfterJump.params.riskFreeRatePct,
            ivPct = iv
        )
        addCustomLegToRunningTrade(
            side = Side.BUY,
            type = InstrumentType.CE,
            strike = strike,
            lots = 1,
            expiryDte = expiryDte,
            entryPrice = price,
            entryIvPct = iv,
            strategyGroupName = "Adjustment @ ₹${Formatters.formatStrike(spot)}"
        )
        _uiState.update {
            it.copy(
                tcsScenarioStep = 3,
                statusBanner = "Added BUY 1L ${Formatters.formatStrike(strike)} CE @ ${Formatters.formatPrice(price)} while preserving original Calendar legs"
            )
        }
    }
}
