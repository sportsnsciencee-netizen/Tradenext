package com.example.domain

import kotlin.math.round

object StrategyCatalog {

    val underlyings = listOf(
        UnderlyingConfig("TCS", "TCS Ltd", 2000.0, 50.0, 175, 22.0),
        UnderlyingConfig("NIFTY", "NIFTY 50", 24200.0, 50.0, 75, 14.5),
        UnderlyingConfig("BANKNIFTY", "NIFTY BANK", 51800.0, 100.0, 30, 17.0),
        UnderlyingConfig("RELIANCE", "Reliance Ind", 1280.0, 20.0, 500, 24.0),
        UnderlyingConfig("INFY", "Infosys Ltd", 1850.0, 20.0, 400, 23.5),
        UnderlyingConfig("HDFCBANK", "HDFC Bank", 1720.0, 20.0, 550, 19.0)
    )

    val templates: List<StrategyTemplate> = listOf(
        // --- DIRECTIONAL & SPREADS ---
        StrategyTemplate(
            id = "long_call",
            name = "Long Call",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Buy ATM Call for unlimited upside with defined risk.",
            legs = listOf(LegTemplateSpec(Side.BUY, InstrumentType.CE, 0))
        ),
        StrategyTemplate(
            id = "short_call",
            name = "Short Call",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BEARISH,
            description = "Sell ATM Call to collect premium on bearish/flat view.",
            legs = listOf(LegTemplateSpec(Side.SELL, InstrumentType.CE, 0))
        ),
        StrategyTemplate(
            id = "long_put",
            name = "Long Put",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BEARISH,
            description = "Buy ATM Put to profit from downside moves.",
            legs = listOf(LegTemplateSpec(Side.BUY, InstrumentType.PE, 0))
        ),
        StrategyTemplate(
            id = "short_put",
            name = "Short Put",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Sell ATM Put to collect premium in neutral-to-bullish markets.",
            legs = listOf(LegTemplateSpec(Side.SELL, InstrumentType.PE, 0))
        ),
        StrategyTemplate(
            id = "bull_call_spread",
            name = "Bull Call Spread",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Buy ATM Call, Sell OTM Call (+2 steps) to cap cost.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2)
            )
        ),
        StrategyTemplate(
            id = "bear_call_spread",
            name = "Bear Call Spread",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BEARISH,
            description = "Sell ATM Call, Buy OTM Call (+2 steps) for net credit.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 2)
            )
        ),
        StrategyTemplate(
            id = "bull_put_spread",
            name = "Bull Put Spread",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Sell ATM Put, Buy OTM Put (-2 steps) for credit spread.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.PE, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -2)
            )
        ),
        StrategyTemplate(
            id = "bear_put_spread",
            name = "Bear Put Spread",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BEARISH,
            description = "Buy ATM Put, Sell OTM Put (-2 steps) for defined bearish risk.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 0),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -2)
            )
        ),
        StrategyTemplate(
            id = "call_ratio_spread",
            name = "Call Ratio Spread (1x2)",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Buy 1 ATM Call, Sell 2 OTM Calls (+2 steps).",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0, lots = 1),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2, lots = 2)
            )
        ),
        StrategyTemplate(
            id = "put_ratio_spread",
            name = "Put Ratio Spread (1x2)",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BEARISH,
            description = "Buy 1 ATM Put, Sell 2 OTM Puts (-2 steps).",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 0, lots = 1),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -2, lots = 2)
            )
        ),
        StrategyTemplate(
            id = "call_backspread",
            name = "Call Ratio Backspread",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.VOLATILE,
            description = "Sell 1 ATM Call, Buy 2 OTM Calls (+2 steps) for explosive upside.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 0, lots = 1),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 2, lots = 2)
            )
        ),
        StrategyTemplate(
            id = "put_backspread",
            name = "Put Ratio Backspread",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.VOLATILE,
            description = "Sell 1 ATM Put, Buy 2 OTM Puts (-2 steps) for sharp crash protection.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.PE, 0, lots = 1),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -2, lots = 2)
            )
        ),
        StrategyTemplate(
            id = "jade_lizard",
            name = "Jade Lizard",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Short OTM Put (-1) + Bear Call Spread (+1/+3) with zero upside risk if credit > spread width.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -1),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 1),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 3)
            )
        ),
        StrategyTemplate(
            id = "broken_wing_butterfly",
            name = "Broken Wing Butterfly",
            category = StrategyCategory.DIRECTIONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Asymmetric Call Butterfly (+0/-2/+5) eliminating downside risk.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0, lots = 1),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2, lots = 2),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 5, lots = 1)
            )
        ),

        // --- NEUTRAL & VOLATILITY ---
        StrategyTemplate(
            id = "long_straddle",
            name = "Long Straddle",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.VOLATILE,
            description = "Buy ATM Call + ATM Put to profit from large breakout in either direction.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 0)
            )
        ),
        StrategyTemplate(
            id = "short_straddle",
            name = "Short Straddle",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.NEUTRAL,
            description = "Sell ATM Call + ATM Put to harvest theta decay near spot.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 0),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, 0)
            )
        ),
        StrategyTemplate(
            id = "long_strangle",
            name = "Long Strangle",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.VOLATILE,
            description = "Buy OTM Call (+2) and OTM Put (-2) for cheaper volatility expansion.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 2),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -2)
            )
        ),
        StrategyTemplate(
            id = "short_strangle",
            name = "Short Strangle",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.NEUTRAL,
            description = "Sell OTM Call (+2) and OTM Put (-2) for wide range theta income.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -2)
            )
        ),
        StrategyTemplate(
            id = "iron_condor",
            name = "Iron Condor",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.NEUTRAL,
            description = "Defined-risk neutral credit strategy: Bull Put Spread + Bear Call Spread.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -4),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -2),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 4)
            )
        ),
        StrategyTemplate(
            id = "iron_butterfly",
            name = "Iron Butterfly",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.NEUTRAL,
            description = "Short ATM Straddle hedged with OTM Long Strangle (-3/+3).",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -3),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, 0),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 3)
            )
        ),
        StrategyTemplate(
            id = "long_call_butterfly",
            name = "Long Call Butterfly",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.NEUTRAL,
            description = "Buy -2 CE, Sell 2x ATM CE, Buy +2 CE for pin risk payoff.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, -2, lots = 1),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 0, lots = 2),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 2, lots = 1)
            )
        ),
        StrategyTemplate(
            id = "short_call_butterfly",
            name = "Short Call Butterfly",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.VOLATILE,
            description = "Sell -2 CE, Buy 2x ATM CE, Sell +2 CE for defined volatility breakout.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.CE, -2, lots = 1),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0, lots = 2),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2, lots = 1)
            )
        ),
        StrategyTemplate(
            id = "long_put_butterfly",
            name = "Long Put Butterfly",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.NEUTRAL,
            description = "Buy -2 PE, Sell 2x ATM PE, Buy +2 PE centered at spot.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -2, lots = 1),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, 0, lots = 2),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 2, lots = 1)
            )
        ),
        StrategyTemplate(
            id = "long_call_condor",
            name = "Long Call Condor",
            category = StrategyCategory.NEUTRAL_VOL,
            outlook = MarketOutlook.NEUTRAL,
            description = "Buy -3 CE, Sell -1 CE, Sell +1 CE, Buy +3 CE for wide neutral plateau.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, -3),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, -1),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 1),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 3)
            )
        ),

        // --- CALENDAR & DIAGONAL SPREADS ---
        StrategyTemplate(
            id = "call_calendar_spread",
            name = "Call Calendar Spread",
            category = StrategyCategory.CALENDAR_DIAGONAL,
            outlook = MarketOutlook.HEDGED,
            description = "Sell Near-Expiry ATM Call, Buy Far-Expiry ATM Call to capture differential theta & vega.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 0, isFarExpiry = false),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0, isFarExpiry = true)
            )
        ),
        StrategyTemplate(
            id = "put_calendar_spread",
            name = "Put Calendar Spread",
            category = StrategyCategory.CALENDAR_DIAGONAL,
            outlook = MarketOutlook.HEDGED,
            description = "Sell Near-Expiry ATM Put, Buy Far-Expiry ATM Put.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.PE, 0, isFarExpiry = false),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 0, isFarExpiry = true)
            )
        ),
        StrategyTemplate(
            id = "double_calendar_spread",
            name = "Double Calendar Spread",
            category = StrategyCategory.CALENDAR_DIAGONAL,
            outlook = MarketOutlook.HEDGED,
            description = "OTM Put Calendar (-2 steps) + OTM Call Calendar (+2 steps) spanning a wide range.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -2, isFarExpiry = false),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -2, isFarExpiry = true),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2, isFarExpiry = false),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 2, isFarExpiry = true)
            )
        ),
        StrategyTemplate(
            id = "diagonal_call_spread",
            name = "Diagonal Call Spread",
            category = StrategyCategory.CALENDAR_DIAGONAL,
            outlook = MarketOutlook.BULLISH,
            description = "Buy Far-Expiry ATM Call, Sell Near-Expiry OTM Call (+2 steps).",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0, isFarExpiry = true),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2, isFarExpiry = false)
            )
        ),
        StrategyTemplate(
            id = "diagonal_put_spread",
            name = "Diagonal Put Spread",
            category = StrategyCategory.CALENDAR_DIAGONAL,
            outlook = MarketOutlook.BEARISH,
            description = "Buy Far-Expiry ATM Put, Sell Near-Expiry OTM Put (-2 steps).",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 0, isFarExpiry = true),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -2, isFarExpiry = false)
            )
        ),
        StrategyTemplate(
            id = "double_diagonal_spread",
            name = "Double Diagonal Spread",
            category = StrategyCategory.CALENDAR_DIAGONAL,
            outlook = MarketOutlook.HEDGED,
            description = "Sell Near Strangle (-2/+2) + Buy Far Strangle (-3/+3) for boosted credit.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -2, isFarExpiry = false),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -3, isFarExpiry = true),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2, isFarExpiry = false),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 3, isFarExpiry = true)
            )
        ),

        // --- SYNTHETIC & ADVANCED ---
        StrategyTemplate(
            id = "covered_call",
            name = "Covered Call",
            category = StrategyCategory.SYNTHETIC,
            outlook = MarketOutlook.BULLISH,
            description = "Long Underlying/Futures + Short OTM Call (+2 steps) for yield enhancement.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.FUT_EQ, 0),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2)
            )
        ),
        StrategyTemplate(
            id = "protective_put",
            name = "Protective Put",
            category = StrategyCategory.SYNTHETIC,
            outlook = MarketOutlook.BULLISH,
            description = "Long Underlying/Futures + Long OTM Put (-2 steps) for portfolio floor.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.FUT_EQ, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -2)
            )
        ),
        StrategyTemplate(
            id = "collar",
            name = "Collar",
            category = StrategyCategory.SYNTHETIC,
            outlook = MarketOutlook.HEDGED,
            description = "Long Underlying + Long OTM Put (-2) + Short OTM Call (+2).",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.FUT_EQ, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, -2),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 2)
            )
        ),
        StrategyTemplate(
            id = "box_spread",
            name = "Box Spread",
            category = StrategyCategory.SYNTHETIC,
            outlook = MarketOutlook.NEUTRAL,
            description = "Bull Call Spread + Bear Put Spread across -1/+1 strikes (arbitrage synthetic loan).",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.CE, -1),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 1),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 1),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, -1)
            )
        ),
        StrategyTemplate(
            id = "conversion",
            name = "Conversion",
            category = StrategyCategory.SYNTHETIC,
            outlook = MarketOutlook.NEUTRAL,
            description = "Long Underlying + Long ATM Put + Short ATM Call.",
            legs = listOf(
                LegTemplateSpec(Side.BUY, InstrumentType.FUT_EQ, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.PE, 0),
                LegTemplateSpec(Side.SELL, InstrumentType.CE, 0)
            )
        ),
        StrategyTemplate(
            id = "reversal",
            name = "Reversal",
            category = StrategyCategory.SYNTHETIC,
            outlook = MarketOutlook.NEUTRAL,
            description = "Short Underlying + Short ATM Put + Long ATM Call.",
            legs = listOf(
                LegTemplateSpec(Side.SELL, InstrumentType.FUT_EQ, 0),
                LegTemplateSpec(Side.SELL, InstrumentType.PE, 0),
                LegTemplateSpec(Side.BUY, InstrumentType.CE, 0)
            )
        )
    )

    fun atmStrike(spot: Double, strikeStep: Double): Double {
        if (strikeStep <= 0.0) return round(spot)
        return round(spot / strikeStep) * strikeStep
    }

    fun instantiateTemplate(
        template: StrategyTemplate,
        params: SimulationParams,
        groupId: String,
        groupName: String = template.name,
        idGenerator: () -> String
    ): List<OptionLeg> {
        val atm = atmStrike(params.simulatedSpot, params.strikeStep)
        val currentIv = (params.baseIvPct + params.ivShiftPct).coerceAtLeast(1.0)
        return template.legs.map { spec ->
            val strike = if (spec.type == InstrumentType.FUT_EQ) {
                params.simulatedSpot
            } else {
                (atm + spec.strikeOffsetSteps * params.strikeStep).coerceAtLeast(params.strikeStep)
            }
            val expiryDte = if (spec.isFarExpiry) params.farExpiryDte else params.nearExpiryDte
            val remDte = (expiryDte - params.simulationDayOffset).coerceAtLeast(0)
            val entryPrice = BlackScholesEngine.fairEntryPrice(
                type = spec.type,
                spot = params.simulatedSpot,
                strike = strike,
                remainingDte = remDte,
                ratePct = params.riskFreeRatePct,
                ivPct = currentIv
            )
            OptionLeg(
                id = idGenerator(),
                strategyGroupId = groupId,
                strategyGroupName = groupName,
                side = spec.side,
                type = spec.type,
                strike = strike,
                lots = spec.lots,
                lotSize = params.lotSize,
                expiryDte = expiryDte,
                entryPrice = entryPrice,
                entryIvPct = currentIv,
                enabled = true,
                addedAtSpot = params.simulatedSpot,
                addedAtDay = params.simulationDayOffset
            )
        }
    }
}
