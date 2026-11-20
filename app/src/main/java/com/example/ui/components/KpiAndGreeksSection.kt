package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.PayoffAnalysis
import com.example.domain.SimulationParams
import com.example.ui.Formatters
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoCurve
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TabularNumFontFamily
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun KpiAndGreeksSection(
    analysis: PayoffAnalysis,
    params: SimulationParams,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("kpi_greeks_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Sim MTM P&L | Net Credit/Debit | Prob of Profit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                label = "SIM MTM P&L (T+${params.simulationDayOffset})",
                value = Formatters.formatInr(analysis.currentSimMtmPnl, showSign = true),
                valueColor = if (analysis.currentSimMtmPnl >= 0) ProfitGreen else LossRed,
                subtext = "At Spot ₹${Formatters.formatStrike(params.simulatedSpot)}",
                modifier = Modifier.weight(1.2f)
            )
            val isCredit = analysis.netEntryCashflow >= 0
            KpiCard(
                label = if (isCredit) "NET CREDIT" else "NET DEBIT",
                value = Formatters.formatCompactInr(kotlin.math.abs(analysis.netEntryCashflow)),
                valueColor = if (isCredit) ProfitGreen else CyanAccent,
                subtext = if (isCredit) "Credit received" else "Debit paid",
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "PROB. OF PROFIT",
                value = "${Formatters.formatDecimal(analysis.probOfProfitPct, 1)}%",
                valueColor = CyanAccent,
                subtext = "${analysis.nearestExpiryDte}D horizon",
                modifier = Modifier.weight(0.95f)
            )
        }

        // Row 2: Max Profit | Max Loss | Breakevens
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                label = "MAX PROFIT",
                value = if (analysis.isMaxProfitUnlimited) "Unlimited" else Formatters.formatCompactInr(analysis.maxProfit, showSign = true),
                valueColor = ProfitGreen,
                subtext = "At nearest expiry",
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = "MAX LOSS",
                value = if (analysis.isMaxLossUnlimited) "Unlimited" else Formatters.formatCompactInr(analysis.maxLoss, showSign = true),
                valueColor = LossRed,
                subtext = "At nearest expiry",
                modifier = Modifier.weight(1f)
            )
            val beText = if (analysis.breakevens.isEmpty()) {
                "None"
            } else {
                analysis.breakevens.take(2).joinToString(" / ") { "₹${Formatters.formatStrike(it)}" }
            }
            KpiCard(
                label = "BREAKEVEN(S)",
                value = beText,
                valueColor = AmberAccent,
                subtext = if (analysis.breakevens.size > 2) "+${analysis.breakevens.size - 2} more" else "Expiry zero-cross",
                modifier = Modifier.weight(1.2f)
            )
        }

        // Net Position Greeks Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = TerminalSurface,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = "NET PORTFOLIO GREEKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GreekCell(
                        symbol = "Δ DELTA",
                        value = Formatters.formatGreek(analysis.netGreeks.delta, 1),
                        color = if (analysis.netGreeks.delta >= 0) ProfitGreen else LossRed,
                        modifier = Modifier.weight(1f)
                    )
                    GreekCell(
                        symbol = "Γ GAMMA",
                        value = Formatters.formatGreek(analysis.netGreeks.gamma, 2),
                        color = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    GreekCell(
                        symbol = "Θ THETA / DAY",
                        value = Formatters.formatInr(analysis.netGreeks.theta, showSign = true),
                        color = if (analysis.netGreeks.theta >= 0) ProfitGreen else LossRed,
                        modifier = Modifier.weight(1.1f)
                    )
                    GreekCell(
                        symbol = "ν VEGA / 1%",
                        value = Formatters.formatInr(analysis.netGreeks.vega, showSign = true),
                        color = IndigoCurve,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    valueColor: Color,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = TerminalSurface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontFamily = TabularNumFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GreekCell(
    symbol: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontFamily = TabularNumFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            style = TextStyle(fontFeatureSettings = "tnum"),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
