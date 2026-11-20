package com.example.domain

enum class Side(val label: String, val shortLabel: String, val sign: Int) {
    BUY("BUY", "B", 1),
    SELL("SELL", "S", -1)
}

enum class InstrumentType(val label: String) {
    CE("CE"),
    PE("PE"),
    FUT_EQ("EQ/FUT")
}

enum class StrategyCategory(val label: String) {
    ALL("All"),
    DIRECTIONAL("Directional & Spreads"),
    NEUTRAL_VOL("Neutral & Vol"),
    CALENDAR_DIAGONAL("Calendar & Diagonal"),
    SYNTHETIC("Synthetic & Adv")
}

enum class MarketOutlook(val label: String) {
    BULLISH("Bullish"),
    BEARISH("Bearish"),
    NEUTRAL("Neutral"),
    VOLATILE("Volatile"),
    HEDGED("Time / Hedged")
}

data class UnderlyingConfig(
    val symbol: String,
    val displayName: String,
    val defaultSpot: Double,
    val strikeStep: Double,
    val defaultLotSize: Int,
    val defaultIvPct: Double
)

data class OptionLeg(
    val id: String,
    val strategyGroupId: String,
    val strategyGroupName: String,
    val side: Side,
    val type: InstrumentType,
    val strike: Double,
    val lots: Int,
    val lotSize: Int,
    val expiryDte: Int,
    val entryPrice: Double,
    val entryIvPct: Double,
    val enabled: Boolean = true,
    val addedAtSpot: Double,
    val addedAtDay: Int = 0
)

data class LegTemplateSpec(
    val side: Side,
    val type: InstrumentType,
    val strikeOffsetSteps: Int,
    val lots: Int = 1,
    val isFarExpiry: Boolean = false
)

data class StrategyTemplate(
    val id: String,
    val name: String,
    val category: StrategyCategory,
    val outlook: MarketOutlook,
    val description: String,
    val legs: List<LegTemplateSpec>
)

data class SimulationParams(
    val underlyingSymbol: String = "TCS",
    val baseSpot: Double = 2000.0,
    val simulatedSpot: Double = 2000.0,
    val strikeStep: Double = 50.0,
    val lotSize: Int = 175,
    val baseIvPct: Double = 22.0,
    val ivShiftPct: Double = 0.0,
    val riskFreeRatePct: Double = 6.5,
    val simulationDayOffset: Int = 0,
    val nearExpiryDte: Int = 7,
    val farExpiryDte: Int = 35
)

data class Greeks(
    val delta: Double = 0.0,
    val gamma: Double = 0.0,
    val theta: Double = 0.0,
    val vega: Double = 0.0
)

data class LegValuation(
    val leg: OptionLeg,
    val remainingDte: Int,
    val simulatedPrice: Double,
    val pnl: Double,
    val greeks: Greeks
)

data class PayoffPoint(
    val spot: Double,
    val expiryPnl: Double,
    val targetDatePnl: Double,
    val strategyExpiryPnls: Map<String, Double>,
    val strategyTargetPnls: Map<String, Double>,
    val legTargetPnls: Map<String, Double>
)

data class PayoffAnalysis(
    val curve: List<PayoffPoint>,
    val currentSimMtmPnl: Double,
    val currentExpiryPnlAtSpot: Double,
    val netEntryCashflow: Double,
    val maxProfit: Double,
    val isMaxProfitUnlimited: Boolean,
    val maxLoss: Double,
    val isMaxLossUnlimited: Boolean,
    val breakevens: List<Double>,
    val probOfProfitPct: Double,
    val netGreeks: Greeks,
    val legValuations: List<LegValuation>,
    val nearestExpiryDte: Int,
    val minChartSpot: Double,
    val maxChartSpot: Double
) {
    companion object {
        val EMPTY = PayoffAnalysis(
            curve = emptyList(),
            currentSimMtmPnl = 0.0,
            currentExpiryPnlAtSpot = 0.0,
            netEntryCashflow = 0.0,
            maxProfit = 0.0,
            isMaxProfitUnlimited = false,
            maxLoss = 0.0,
            isMaxLossUnlimited = false,
            breakevens = emptyList(),
            probOfProfitPct = 0.0,
            netGreeks = Greeks(),
            legValuations = emptyList(),
            nearestExpiryDte = 7,
            minChartSpot = 1600.0,
            maxChartSpot = 2400.0
        )
    }
}
