package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object BlackScholesEngine {

    private val INV_SQRT_2PI = 1.0 / sqrt(2.0 * PI)

    fun normalPdf(x: Double): Double = INV_SQRT_2PI * exp(-0.5 * x * x)

    /**
     * Abramowitz & Stegun 26.2.17 cumulative normal distribution approximation.
     * Max absolute error < 7.5e-8.
     */
    fun normalCdf(x: Double): Double {
        if (x < -10.0) return 0.0
        if (x > 10.0) return 1.0
        val sign = if (x < 0.0) -1.0 else 1.0
        val absX = abs(x)
        val t = 1.0 / (1.0 + 0.2316419 * absX)
        val poly = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        val cdfPos = 1.0 - normalPdf(absX) * poly
        return if (sign >= 0.0) cdfPos else 1.0 - cdfPos
    }

    data class PriceAndGreeks(
        val price: Double,
        val delta: Double,
        val gamma: Double,
        val thetaPerDay: Double,
        val vegaPerPct: Double
    )

    fun priceInstrument(
        type: InstrumentType,
        spot: Double,
        strike: Double,
        timeYears: Double,
        rateDecimal: Double,
        ivDecimal: Double
    ): PriceAndGreeks {
        val safeSpot = max(0.01, spot)
        if (type == InstrumentType.FUT_EQ) {
            return PriceAndGreeks(
                price = safeSpot,
                delta = 1.0,
                gamma = 0.0,
                thetaPerDay = 0.0,
                vegaPerPct = 0.0
            )
        }

        val intrinsic = when (type) {
            InstrumentType.CE -> max(0.0, safeSpot - strike)
            InstrumentType.PE -> max(0.0, strike - safeSpot)
            InstrumentType.FUT_EQ -> safeSpot
        }

        if (timeYears <= 1e-6 || strike <= 0.0) {
            val delta = when (type) {
                InstrumentType.CE -> if (safeSpot > strike) 1.0 else 0.0
                InstrumentType.PE -> if (safeSpot < strike) -1.0 else 0.0
                InstrumentType.FUT_EQ -> 1.0
            }
            return PriceAndGreeks(
                price = intrinsic,
                delta = delta,
                gamma = 0.0,
                thetaPerDay = 0.0,
                vegaPerPct = 0.0
            )
        }

        val sigma = max(0.01, ivDecimal)
        val sqrtT = sqrt(timeYears)
        val sigmaSqrtT = sigma * sqrtT
        val d1 = (ln(safeSpot / strike) + (rateDecimal + 0.5 * sigma * sigma) * timeYears) / sigmaSqrtT
        val d2 = d1 - sigmaSqrtT

        val nd1 = normalCdf(d1)
        val nd2 = normalCdf(d2)
        val pdfD1 = normalPdf(d1)
        val discStrike = strike * exp(-rateDecimal * timeYears)

        val price: Double
        val delta: Double
        val thetaAnnual: Double

        when (type) {
            InstrumentType.CE -> {
                price = max(0.0, safeSpot * nd1 - discStrike * nd2)
                delta = nd1
                thetaAnnual = -(safeSpot * pdfD1 * sigma) / (2.0 * sqrtT) - rateDecimal * discStrike * nd2
            }
            InstrumentType.PE -> {
                val nMinusD1 = 1.0 - nd1
                val nMinusD2 = 1.0 - nd2
                price = max(0.0, discStrike * nMinusD2 - safeSpot * nMinusD1)
                delta = nd1 - 1.0
                thetaAnnual = -(safeSpot * pdfD1 * sigma) / (2.0 * sqrtT) + rateDecimal * discStrike * nMinusD2
            }
            InstrumentType.FUT_EQ -> {
                price = safeSpot
                delta = 1.0
                thetaAnnual = 0.0
            }
        }

        val gamma = pdfD1 / (safeSpot * sigmaSqrtT)
        val vegaPerPct = safeSpot * pdfD1 * sqrtT * 0.01
        val thetaPerDay = thetaAnnual / 365.0

        return PriceAndGreeks(
            price = price,
            delta = delta,
            gamma = gamma,
            thetaPerDay = thetaPerDay,
            vegaPerPct = vegaPerPct
        )
    }

    fun fairEntryPrice(
        type: InstrumentType,
        spot: Double,
        strike: Double,
        remainingDte: Int,
        ratePct: Double,
        ivPct: Double
    ): Double {
        if (type == InstrumentType.FUT_EQ) return spot
        val t = max(0, remainingDte) / 365.0
        val res = priceInstrument(
            type = type,
            spot = spot,
            strike = strike,
            timeYears = t,
            rateDecimal = ratePct / 100.0,
            ivDecimal = max(1.0, ivPct) / 100.0
        )
        return kotlin.math.round(res.price * 20.0) / 20.0 // round to 0.05 tick
    }

    fun analyzePortfolio(
        legs: List<OptionLeg>,
        params: SimulationParams
    ): PayoffAnalysis {
        val activeLegs = legs.filter { it.enabled && it.lots > 0 && it.lotSize > 0 }
        if (activeLegs.isEmpty()) {
            val minS = max(10.0, params.simulatedSpot * 0.8)
            val maxS = params.simulatedSpot * 1.2
            return PayoffAnalysis.EMPTY.copy(
                nearestExpiryDte = params.nearExpiryDte,
                minChartSpot = minS,
                maxChartSpot = maxS
            )
        }

        val nearestExpiryDte = activeLegs.minOfOrNull { it.expiryDte } ?: params.nearExpiryDte
        val effectiveSimDay = params.simulationDayOffset.coerceIn(0, max(0, nearestExpiryDte))
        val rateDec = params.riskFreeRatePct / 100.0

        // Calculate leg valuations at current simulatedSpot & effectiveSimDay
        val legValuations = legs.map { leg ->
            val remDte = max(0, leg.expiryDte - effectiveSimDay)
            val tYears = remDte / 365.0
            val legIvDec = max(1.0, leg.entryIvPct + params.ivShiftPct) / 100.0
            val valuation = priceInstrument(
                type = leg.type,
                spot = params.simulatedSpot,
                strike = leg.strike,
                timeYears = tYears,
                rateDecimal = rateDec,
                ivDecimal = legIvDec
            )
            val qty = leg.side.sign * leg.lots * leg.lotSize
            val pnl = (valuation.price - leg.entryPrice) * qty
            val scaledGreeks = if (leg.enabled) {
                Greeks(
                    delta = valuation.delta * qty,
                    gamma = valuation.gamma * qty,
                    theta = valuation.thetaPerDay * qty,
                    vega = valuation.vegaPerPct * qty
                )
            } else {
                Greeks()
            }
            LegValuation(
                leg = leg,
                remainingDte = remDte,
                simulatedPrice = valuation.price,
                pnl = pnl,
                greeks = scaledGreeks
            )
        }

        val activeValuations = legValuations.filter { it.leg.enabled }
        val currentSimMtmPnl = activeValuations.sumOf { it.pnl }
        val netGreeks = Greeks(
            delta = activeValuations.sumOf { it.greeks.delta },
            gamma = activeValuations.sumOf { it.greeks.gamma },
            theta = activeValuations.sumOf { it.greeks.theta },
            vega = activeValuations.sumOf { it.greeks.vega }
        )

        val netEntryCashflow = activeLegs.sumOf { leg ->
            -leg.side.sign * leg.entryPrice * leg.lots * leg.lotSize
        }

        // Determine chart spot range covering strikes and simulated spot cleanly
        val optionStrikes = activeLegs.filter { it.type != InstrumentType.FUT_EQ }.map { it.strike }
        val allRefSpots = optionStrikes + params.simulatedSpot + params.baseSpot
        val minRef = allRefSpots.minOrNull() ?: params.simulatedSpot
        val maxRef = allRefSpots.maxOrNull() ?: params.simulatedSpot
        val span = max(maxRef - minRef, params.simulatedSpot * 0.18)
        val padding = max(span * 0.45, params.strikeStep * 4.0)
        val minChartSpot = max(params.strikeStep, minRef - padding)
        val maxChartSpot = maxRef + padding

        val steps = 180
        val stepSize = (maxChartSpot - minChartSpot) / steps

        // Collect exact sample spots including grid + all active strikes + simulatedSpot
        val sampleSpotSet = sortedSetOf<Double>()
        for (i in 0..steps) {
            sampleSpotSet.add(minChartSpot + i * stepSize)
        }
        optionStrikes.forEach { k ->
            if (k in minChartSpot..maxChartSpot) sampleSpotSet.add(k)
        }
        if (params.simulatedSpot in minChartSpot..maxChartSpot) {
            sampleSpotSet.add(params.simulatedSpot)
        }

        fun evaluateAtSpot(spot: Double, dayOffset: Int): Pair<Double, Map<String, Double>> {
            var totalPnl = 0.0
            val groupPnls = LinkedHashMap<String, Double>()
            for (leg in activeLegs) {
                val remDte = max(0, leg.expiryDte - dayOffset)
                val tYears = remDte / 365.0
                val legIvDec = max(1.0, leg.entryIvPct + params.ivShiftPct) / 100.0
                val p = priceInstrument(
                    type = leg.type,
                    spot = spot,
                    strike = leg.strike,
                    timeYears = tYears,
                    rateDecimal = rateDec,
                    ivDecimal = legIvDec
                ).price
                val legPnl = (p - leg.entryPrice) * leg.side.sign * leg.lots * leg.lotSize
                totalPnl += legPnl
                groupPnls[leg.strategyGroupId] = (groupPnls[leg.strategyGroupId] ?: 0.0) + legPnl
            }
            return totalPnl to groupPnls
        }

        fun evaluateLegTargetPnls(spot: Double, dayOffset: Int): Map<String, Double> {
            val map = LinkedHashMap<String, Double>(activeLegs.size)
            for (leg in activeLegs) {
                val remDte = max(0, leg.expiryDte - dayOffset)
                val tYears = remDte / 365.0
                val legIvDec = max(1.0, leg.entryIvPct + params.ivShiftPct) / 100.0
                val p = priceInstrument(
                    type = leg.type,
                    spot = spot,
                    strike = leg.strike,
                    timeYears = tYears,
                    rateDecimal = rateDec,
                    ivDecimal = legIvDec
                ).price
                map[leg.id] = (p - leg.entryPrice) * leg.side.sign * leg.lots * leg.lotSize
            }
            return map
        }

        val curve = sampleSpotSet.map { spot ->
            val (expiryPnl, groupExp) = evaluateAtSpot(spot, nearestExpiryDte)
            val (targetPnl, groupTgt) = evaluateAtSpot(spot, effectiveSimDay)
            val legTgt = evaluateLegTargetPnls(spot, effectiveSimDay)
            PayoffPoint(
                spot = spot,
                expiryPnl = expiryPnl,
                targetDatePnl = targetPnl,
                strategyExpiryPnls = groupExp,
                strategyTargetPnls = groupTgt,
                legTargetPnls = legTgt
            )
        }

        val (currentExpiryPnlAtSpot, _) = evaluateAtSpot(params.simulatedSpot, nearestExpiryDte)

        // Breakeven detection (zero crossings on expiry curve)
        val breakevens = mutableListOf<Double>()
        for (i in 0 until curve.size - 1) {
            val p1 = curve[i]
            val p2 = curve[i + 1]
            if (abs(p1.expiryPnl) < 1e-3) {
                if (breakevens.lastOrNull()?.let { abs(it - p1.spot) > 0.5 } != false) {
                    breakevens.add(p1.spot)
                }
            } else if ((p1.expiryPnl < 0.0 && p2.expiryPnl > 0.0) || (p1.expiryPnl > 0.0 && p2.expiryPnl < 0.0)) {
                val ratio = abs(p1.expiryPnl) / (abs(p1.expiryPnl) + abs(p2.expiryPnl))
                val be = p1.spot + ratio * (p2.spot - p1.spot)
                if (breakevens.lastOrNull()?.let { abs(it - be) > 0.5 } != false) {
                    breakevens.add(be)
                }
            }
        }

        // Upside unlimited detection via net delta as S -> infinity (Calls + Futures)
        // Note: For calendar spreads on same strike with equal lots, netUpsideUnitQty == 0
        val netUpsideUnitQty = activeLegs.sumOf { leg ->
            when (leg.type) {
                InstrumentType.CE, InstrumentType.FUT_EQ -> leg.side.sign * leg.lots * leg.lotSize
                InstrumentType.PE -> 0
            }
        }
        val isMaxProfitUnlimited = netUpsideUnitQty > 0
        val isMaxLossUnlimited = netUpsideUnitQty < 0

        // Wide tail evaluation including S = 0.01 and S = maxRef * 2.5 to capture exact max profit/loss
        val wideSpots = mutableListOf(0.01, minChartSpot * 0.5, maxChartSpot * 1.5, maxRef * 2.5)
        wideSpots.addAll(curve.map { it.spot })
        val widePnls = wideSpots.map { evaluateAtSpot(it, nearestExpiryDte).first }
        val maxProfit = widePnls.maxOrNull() ?: 0.0
        val maxLoss = widePnls.minOrNull() ?: 0.0

        // Approximate Probability of Profit (PoP %) using log-normal distribution over nearest expiry
        val avgIvDec = activeLegs.map { max(1.0, it.entryIvPct + params.ivShiftPct) / 100.0 }.average()
        val horizonYears = max(1, nearestExpiryDte) / 365.0
        val probOfProfitPct = computeProbabilityOfProfit(
            curve = curve,
            spot = params.simulatedSpot,
            rateDec = rateDec,
            ivDec = avgIvDec,
            timeYears = horizonYears
        )

        return PayoffAnalysis(
            curve = curve,
            currentSimMtmPnl = currentSimMtmPnl,
            currentExpiryPnlAtSpot = currentExpiryPnlAtSpot,
            netEntryCashflow = netEntryCashflow,
            maxProfit = maxProfit,
            isMaxProfitUnlimited = isMaxProfitUnlimited,
            maxLoss = maxLoss,
            isMaxLossUnlimited = isMaxLossUnlimited,
            breakevens = breakevens,
            probOfProfitPct = probOfProfitPct,
            netGreeks = netGreeks,
            legValuations = legValuations,
            nearestExpiryDte = nearestExpiryDte,
            minChartSpot = minChartSpot,
            maxChartSpot = maxChartSpot
        )
    }

    private fun computeProbabilityOfProfit(
        curve: List<PayoffPoint>,
        spot: Double,
        rateDec: Double,
        ivDec: Double,
        timeYears: Double
    ): Double {
        if (curve.size < 2 || spot <= 0.0 || ivDec <= 0.0 || timeYears <= 0.0) return 0.0
        val sigmaSqrtT = ivDec * sqrt(timeYears)
        val drift = (rateDec - 0.5 * ivDec * ivDec) * timeYears

        fun cdfSpot(s: Double): Double {
            if (s <= 0.0) return 0.0
            val z = (ln(s / spot) - drift) / sigmaSqrtT
            return normalCdf(z)
        }

        var pop = 0.0
        for (i in 0 until curve.size - 1) {
            val a = curve[i]
            val b = curve[i + 1]
            val midPnl = 0.5 * (a.expiryPnl + b.expiryPnl)
            if (midPnl > 0.0) {
                pop += (cdfSpot(b.spot) - cdfSpot(a.spot))
            }
        }
        // Check right tail beyond maxChartSpot
        if (curve.last().expiryPnl > 0.0) {
            pop += (1.0 - cdfSpot(curve.last().spot))
        }
        // Check left tail below minChartSpot
        if (curve.first().expiryPnl > 0.0) {
            pop += cdfSpot(curve.first().spot)
        }
        return (pop * 100.0).coerceIn(0.0, 99.9)
    }
}
