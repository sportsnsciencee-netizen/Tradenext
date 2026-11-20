package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.OptionLeg
import com.example.domain.PayoffAnalysis
import com.example.domain.PayoffPoint
import com.example.domain.SimulationParams
import com.example.ui.Formatters
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoCurve
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedMuted
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenMuted
import com.example.ui.theme.TabularNumFontFamily
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayoffChartCard(
    analysis: PayoffAnalysis,
    params: SimulationParams,
    legs: List<OptionLeg>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 260.dp
) {
    var showFullscreen by remember { mutableStateOf(false) }
    var scrubbedIndex by remember(analysis.curve) { mutableStateOf<Int?>(null) }

    val strategyNames = remember(legs) {
        legs.filter { it.enabled }.associate { it.strategyGroupId to it.strategyGroupName }
    }
    val activeLegsMap = remember(legs) {
        legs.filter { it.enabled }.associateBy { it.id }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payoff_chart_card"),
        shape = RoundedCornerShape(12.dp),
        color = TerminalSurface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${params.underlyingSymbol} Payoff Analyzer",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        LegendItem(color = ProfitGreen, label = "On Expiry (${analysis.nearestExpiryDte}D)", dashed = false)
                        LegendItem(color = IndigoCurve, label = "On T+${params.simulationDayOffset} Target", dashed = true)
                        LegendItem(color = AmberAccent, label = "Breakeven", isDot = true)
                    }
                }
                IconButton(
                    onClick = { showFullscreen = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("expand_chart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand Payoff Chart",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (analysis.curve.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                            contentDescription = "Empty Payoff Chart",
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add a strategy or custom leg to plot payoff",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                val selectedPoint = scrubbedIndex?.let { analysis.curve.getOrNull(it) }
                ScrubTooltipHud(
                    point = selectedPoint,
                    params = params,
                    analysis = analysis,
                    strategyNames = strategyNames,
                    activeLegsMap = activeLegsMap
                )

                Spacer(modifier = Modifier.height(8.dp))

                InteractivePayoffCanvas(
                    analysis = analysis,
                    params = params,
                    scrubbedIndex = scrubbedIndex,
                    onScrubIndexChange = { scrubbedIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                )

                Text(
                    text = "Drag or tap across the payoff chart to inspect exact Spot, T+${params.simulationDayOffset} MTM & per-leg P&L",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    if (showFullscreen && analysis.curve.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = RoundedCornerShape(14.dp),
                color = TerminalSurface,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, TerminalBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${params.underlyingSymbol} Payoff Lab • T+${params.simulationDayOffset} vs Expiry (${analysis.nearestExpiryDte}D)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = { showFullscreen = false },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Fullscreen Chart",
                                tint = TextPrimary
                            )
                        }
                    }
                    val selectedPoint = scrubbedIndex?.let { analysis.curve.getOrNull(it) }
                    ScrubTooltipHud(
                        point = selectedPoint,
                        params = params,
                        analysis = analysis,
                        strategyNames = strategyNames,
                        activeLegsMap = activeLegsMap
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InteractivePayoffCanvas(
                        analysis = analysis,
                        params = params,
                        scrubbedIndex = scrubbedIndex,
                        onScrubIndexChange = { scrubbedIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScrubTooltipHud(
    point: PayoffPoint?,
    params: SimulationParams,
    analysis: PayoffAnalysis,
    strategyNames: Map<String, String>,
    activeLegsMap: Map<String, OptionLeg>
) {
    val displaySpot = point?.spot ?: params.simulatedSpot
    val displayExpiryPnl = point?.expiryPnl ?: analysis.currentExpiryPnlAtSpot
    val displayTargetPnl = point?.targetDatePnl ?: analysis.currentSimMtmPnl
    val isScrubbing = point != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = TerminalSurfaceElevated,
        border = BorderStroke(
            1.dp,
            if (isScrubbing) CyanAccent.copy(alpha = 0.6f) else TerminalBorder
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isScrubbing) "INSPECTED SPOT" else "SIMULATED SPOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isScrubbing) CyanAccent else TextMuted
                    )
                    Text(
                        text = "₹${Formatters.formatStrike(displaySpot)}",
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "T+${params.simulationDayOffset} TARGET P&L",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndigoCurve
                    )
                    Text(
                        text = Formatters.formatInr(displayTargetPnl, showSign = true),
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                        color = if (displayTargetPnl >= 0) ProfitGreen else LossRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPIRY P&L",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = Formatters.formatInr(displayExpiryPnl, showSign = true),
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                        color = if (displayExpiryPnl >= 0) ProfitGreen else LossRed
                    )
                }
            }

            if (point != null && (point.strategyTargetPnls.size > 1 || point.legTargetPnls.isNotEmpty())) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (point.strategyTargetPnls.size > 1) {
                        point.strategyTargetPnls.forEach { (grpId, pnl) ->
                            val label = strategyNames[grpId] ?: grpId
                            MiniBreakdownPill(label = label.take(24), pnl = pnl)
                        }
                    } else {
                        point.legTargetPnls.forEach { (legId, pnl) ->
                            val leg = activeLegsMap[legId]
                            if (leg != null) {
                                val label = "${leg.side.shortLabel} ${Formatters.formatStrike(leg.strike)} ${leg.type.label}"
                                MiniBreakdownPill(label = label, pnl = pnl)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBreakdownPill(label: String, pnl: Double) {
    val isPos = pnl >= 0
    val color = if (isPos) ProfitGreen else LossRed
    val bg = if (isPos) ProfitGreenMuted else LossRedMuted
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.6.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Text(
            text = Formatters.formatCompactInr(pnl, showSign = true),
            fontFamily = TabularNumFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
            color = color
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    dashed: Boolean = false,
    isDot: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isDot) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        } else {
            Canvas(modifier = Modifier.size(width = 14.dp, height = 4.dp)) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.2.dp.toPx(),
                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun InteractivePayoffCanvas(
    analysis: PayoffAnalysis,
    params: SimulationParams,
    scrubbedIndex: Int?,
    onScrubIndexChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val curve = analysis.curve

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFAFBFD))
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(curve) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (curve.isEmpty()) return@detectTapGestures
                            val leftPad = 54.dp.toPx()
                            val rightPad = 14.dp.toPx()
                            val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                            val frac = ((offset.x - leftPad) / plotW).coerceIn(0f, 1f)
                            val idx = (frac * (curve.size - 1)).toInt().coerceIn(0, curve.size - 1)
                            onScrubIndexChange(if (scrubbedIndex == idx) null else idx)
                        }
                    )
                }
                .pointerInput(curve) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (curve.isEmpty()) return@detectHorizontalDragGestures
                            val leftPad = 54.dp.toPx()
                            val rightPad = 14.dp.toPx()
                            val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                            val frac = ((change.position.x - leftPad) / plotW).coerceIn(0f, 1f)
                            val idx = (frac * (curve.size - 1)).toInt().coerceIn(0, curve.size - 1)
                            onScrubIndexChange(idx)
                        }
                    )
                }
        ) {
            if (curve.size < 2) return@Canvas

            val leftPad = 54.dp.toPx()
            val rightPad = 14.dp.toPx()
            val topPad = 26.dp.toPx()
            val bottomPad = 26.dp.toPx()
            val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            val minSpot = curve.first().spot
            val maxSpot = curve.last().spot
            val spotSpan = (maxSpot - minSpot).coerceAtLeast(1.0)

            var minPnl = Double.POSITIVE_INFINITY
            var maxPnl = Double.NEGATIVE_INFINITY
            for (pt in curve) {
                minPnl = min(minPnl, min(pt.expiryPnl, pt.targetDatePnl))
                maxPnl = max(maxPnl, max(pt.expiryPnl, pt.targetDatePnl))
            }
            minPnl = min(minPnl, 0.0)
            maxPnl = max(maxPnl, 0.0)
            val rawSpan = (maxPnl - minPnl).coerceAtLeast(100.0)
            val padPnl = rawSpan * 0.14
            val yMin = minPnl - padPnl
            val yMax = maxPnl + padPnl
            val ySpan = (yMax - yMin).coerceAtLeast(1.0)

            fun xForSpot(s: Double): Float {
                return leftPad + (((s - minSpot) / spotSpan).toFloat() * plotW)
            }

            fun yForPnl(pnl: Double): Float {
                return topPad + ((1.0 - (pnl - yMin) / ySpan).toFloat() * plotH)
            }

            val yZero = yForPnl(0.0).coerceIn(topPad, topPad + plotH)

            val axisTextStyle = TextStyle(
                fontFamily = TabularNumFontFamily,
                fontSize = 9.5.sp,
                fontFeatureSettings = "tnum",
                color = TextMuted
            )

            // Gridlines
            for (i in 0..4) {
                val frac = i / 4f
                val y = topPad + frac * plotH
                val pnlVal = yMax - frac * ySpan
                drawLine(
                    color = TerminalBorder,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + plotW, y),
                    strokeWidth = 0.8.dp.toPx()
                )
                val label = Formatters.formatCompactInr(pnlVal, showSign = true)
                val measured = textMeasurer.measure(label, axisTextStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        (leftPad - measured.size.width - 5.dp.toPx()).coerceAtLeast(2f),
                        (y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height)
                    )
                )
            }

            for (i in 0..3) {
                val frac = i / 3f
                val x = leftPad + frac * plotW
                val spotVal = minSpot + frac * spotSpan
                drawLine(
                    color = TerminalBorder.copy(alpha = 0.7f),
                    start = Offset(x, topPad),
                    end = Offset(x, topPad + plotH),
                    strokeWidth = 0.8.dp.toPx()
                )
                val label = Formatters.formatStrike(spotVal)
                val measured = textMeasurer.measure(label, axisTextStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        (x - measured.size.width / 2f).coerceIn(leftPad, size.width - measured.size.width - 2f),
                        topPad + plotH + 5.dp.toPx()
                    )
                )
            }

            val expiryPath = Path()
            val targetPath = Path()
            val fillPath = Path()

            curve.forEachIndexed { idx, pt ->
                val x = xForSpot(pt.spot)
                val yExp = yForPnl(pt.expiryPnl)
                val yTgt = yForPnl(pt.targetDatePnl)
                if (idx == 0) {
                    expiryPath.moveTo(x, yExp)
                    targetPath.moveTo(x, yTgt)
                    fillPath.moveTo(x, yZero)
                    fillPath.lineTo(x, yExp)
                } else {
                    expiryPath.lineTo(x, yExp)
                    targetPath.lineTo(x, yTgt)
                    fillPath.lineTo(x, yExp)
                }
                if (idx == curve.lastIndex) {
                    fillPath.lineTo(x, yZero)
                    fillPath.close()
                }
            }

            // Soft translucent green profit area fill & bold emerald expiry curve
            clipRect(left = leftPad, top = topPad, right = leftPad + plotW, bottom = yZero) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.24f),
                            Color(0xFF10B981).copy(alpha = 0.04f)
                        ),
                        startY = topPad,
                        endY = yZero
                    )
                )
                drawPath(
                    path = expiryPath,
                    color = ProfitGreen,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Soft translucent red loss area fill & crisp coral red expiry curve
            clipRect(left = leftPad, top = yZero, right = leftPad + plotW, bottom = topPad + plotH) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEF4444).copy(alpha = 0.04f),
                            Color(0xFFEF4444).copy(alpha = 0.24f)
                        ),
                        startY = yZero,
                        endY = topPad + plotH
                    )
                )
                drawPath(
                    path = expiryPath,
                    color = LossRed,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Zero baseline
            drawLine(
                color = TextSecondary.copy(alpha = 0.7f),
                start = Offset(leftPad, yZero),
                end = Offset(leftPad + plotW, yZero),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f))
            )

            // Smooth dashed Indigo T+N target date curve
            clipRect(left = leftPad, top = topPad, right = leftPad + plotW, bottom = topPad + plotH) {
                drawPath(
                    path = targetPath,
                    color = IndigoCurve,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 7f))
                    )
                )
            }

            // Simulated Spot Vertical Marker
            if (params.simulatedSpot in minSpot..maxSpot) {
                val xSpot = xForSpot(params.simulatedSpot)
                drawLine(
                    color = TextPrimary.copy(alpha = 0.65f),
                    start = Offset(xSpot, topPad),
                    end = Offset(xSpot, topPad + plotH),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
                )
                val ySimDot = yForPnl(analysis.currentSimMtmPnl).coerceIn(topPad, topPad + plotH)
                drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = Offset(xSpot, ySimDot))
                drawCircle(color = IndigoCurve, radius = 3.8.dp.toPx(), center = Offset(xSpot, ySimDot))

                val spotBadge = "Spot ₹${Formatters.formatStrike(params.simulatedSpot)}"
                val badgeMeasure = textMeasurer.measure(
                    spotBadge,
                    TextStyle(
                        fontFamily = TabularNumFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        color = Color.White
                    )
                )
                val pillW = badgeMeasure.size.width + 12.dp.toPx()
                val pillH = badgeMeasure.size.height + 4.dp.toPx()
                val pillX = (xSpot - pillW / 2f).coerceIn(leftPad, leftPad + plotW - pillW)
                drawRoundRect(
                    color = TextPrimary,
                    topLeft = Offset(pillX, 4.dp.toPx()),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawText(
                    textLayoutResult = badgeMeasure,
                    topLeft = Offset(pillX + 6.dp.toPx(), 6.dp.toPx())
                )
            }

            // Breakeven Markers
            analysis.breakevens.forEachIndexed { idx, be ->
                if (be in minSpot..maxSpot) {
                    val xBe = xForSpot(be)
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(xBe, yZero))
                    drawCircle(color = AmberAccent, radius = 3.5.dp.toPx(), center = Offset(xBe, yZero))
                    if (idx < 3) {
                        val beText = textMeasurer.measure(
                            "BE ${Formatters.formatStrike(be)}",
                            TextStyle(
                                fontFamily = TabularNumFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum",
                                color = AmberAccent
                            )
                        )
                        val textY = if (yZero > topPad + 20.dp.toPx()) yZero - beText.size.height - 6.dp.toPx() else yZero + 6.dp.toPx()
                        drawText(
                            textLayoutResult = beText,
                            topLeft = Offset(
                                (xBe - beText.size.width / 2f).coerceIn(leftPad, leftPad + plotW - beText.size.width),
                                textY
                            )
                        )
                    }
                }
            }

            // Scrub Crosshair
            val idx = scrubbedIndex
            if (idx != null && idx in curve.indices) {
                val pt = curve[idx]
                val xScrub = xForSpot(pt.spot)
                val yExp = yForPnl(pt.expiryPnl).coerceIn(topPad, topPad + plotH)
                val yTgt = yForPnl(pt.targetDatePnl).coerceIn(topPad, topPad + plotH)

                drawLine(
                    color = CyanAccent.copy(alpha = 0.85f),
                    start = Offset(xScrub, topPad),
                    end = Offset(xScrub, topPad + plotH),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(xScrub, yExp))
                drawCircle(
                    color = if (pt.expiryPnl >= 0) ProfitGreen else LossRed,
                    radius = 4.dp.toPx(),
                    center = Offset(xScrub, yExp)
                )
                drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = Offset(xScrub, yTgt))
                drawCircle(color = IndigoCurve, radius = 3.5.dp.toPx(), center = Offset(xScrub, yTgt))
            }
        }
    }
}

@Composable
fun MiniPayoffSparkline(
    points: List<Double>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurfaceElevated)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(5.dp)
    ) {
        if (points.size < 2) return@Canvas
        val minVal = min(points.min(), 0.0)
        val maxVal = max(points.max(), 0.0)
        val span = (maxVal - minVal).coerceAtLeast(1.0)
        val pad = span * 0.15
        val yMin = minVal - pad
        val yMax = maxVal + pad
        val ySpan = (yMax - yMin).coerceAtLeast(1.0)

        fun yFor(v: Double): Float = ((1.0 - (v - yMin) / ySpan).toFloat() * size.height)
        val yZero = yFor(0.0).coerceIn(0f, size.height)

        drawLine(
            color = TerminalBorder,
            start = Offset(0f, yZero),
            end = Offset(size.width, yZero),
            strokeWidth = 0.9.dp.toPx()
        )

        val path = Path()
        points.forEachIndexed { idx, v ->
            val x = (idx.toFloat() / (points.size - 1)) * size.width
            val y = yFor(v)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        clipRect(0f, 0f, size.width, yZero) {
            drawPath(path, color = ProfitGreen, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        clipRect(0f, yZero, size.width, size.height) {
            drawPath(path, color = LossRed, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
