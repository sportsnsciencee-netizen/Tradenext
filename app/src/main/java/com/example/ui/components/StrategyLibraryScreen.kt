package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BlackScholesEngine
import com.example.domain.InstrumentType
import com.example.domain.MarketOutlook
import com.example.domain.Side
import com.example.domain.SimulationParams
import com.example.domain.StrategyCatalog
import com.example.domain.StrategyCategory
import com.example.domain.StrategyTemplate
import com.example.ui.Formatters
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedMuted
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenMuted
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.TabularNumFontFamily
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent

enum class BuilderSegment(val label: String) {
    READY_STRATEGIES("Ready Strategies"),
    CUSTOM_LEG("Custom Leg")
}

enum class StrategyOutlookFilter(val label: String) {
    ALL("All"),
    BULLISH("Bullish"),
    BEARISH("Bearish"),
    NEUTRAL("Neutral"),
    CALENDAR_VOL("Calendar / Vol")
}

@Composable
fun StrategyLibraryScreen(
    params: SimulationParams,
    activeLegCount: Int,
    selectedSegment: BuilderSegment,
    onSegmentChange: (BuilderSegment) -> Unit,
    onAddStrategyToBasket: (StrategyTemplate) -> Unit,
    onReplaceWithStrategy: (StrategyTemplate) -> Unit,
    onAddCustomLeg: (
        side: Side,
        type: InstrumentType,
        strike: Double,
        lots: Int,
        expiryDte: Int,
        entryPrice: Double,
        entryIvPct: Double
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(StrategyOutlookFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTemplates = remember(selectedFilter, searchQuery) {
        StrategyCatalog.templates.filter { template ->
            val groupMatch = when (selectedFilter) {
                StrategyOutlookFilter.ALL -> true
                StrategyOutlookFilter.BULLISH -> template.outlook == MarketOutlook.BULLISH
                StrategyOutlookFilter.BEARISH -> template.outlook == MarketOutlook.BEARISH
                StrategyOutlookFilter.NEUTRAL -> template.outlook == MarketOutlook.NEUTRAL
                StrategyOutlookFilter.CALENDAR_VOL ->
                    template.category == StrategyCategory.CALENDAR_DIAGONAL ||
                        template.outlook == MarketOutlook.VOLATILE ||
                        template.outlook == MarketOutlook.HEDGED
            }
            val q = searchQuery.trim().lowercase()
            val queryMatch = q.isEmpty() ||
                template.name.lowercase().contains(q) ||
                template.description.lowercase().contains(q) ||
                template.outlook.label.lowercase().contains(q)
            groupMatch && queryMatch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("strategy_library_list"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Segmented Control: Ready Strategies | Custom Leg
        item {
            BuilderSegmentedControl(
                selectedSegment = selectedSegment,
                onSelect = onSegmentChange
            )
        }

        if (selectedSegment == BuilderSegment.CUSTOM_LEG) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = TerminalSurface,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, TerminalBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Custom Leg Builder",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add a custom leg at Spot ₹${Formatters.formatStrike(params.simulatedSpot)} (T+${params.simulationDayOffset}) while preserving existing basket legs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CustomLegFormContent(
                            params = params,
                            editingLeg = null,
                            submitButtonLabel = "+ Add Leg to Running Trade",
                            onSubmit = onAddCustomLeg
                        )
                    }
                }
            }
        } else {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search 36 strategies (e.g. Double Calendar, Iron Condor)...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = TerminalBorder,
                        focusedContainerColor = TerminalSurface,
                        unfocusedContainerColor = TerminalSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("strategy_search_input")
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(StrategyOutlookFilter.entries) { filter ->
                        val selected = selectedFilter == filter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) CyanAccent else TerminalSurface,
                            border = BorderStroke(1.dp, if (selected) CyanAccent else TerminalBorder)
                        ) {
                            Text(
                                text = filter.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            items(filteredTemplates, key = { it.id }) { template ->
                StrategyTemplateCard(
                    template = template,
                    params = params,
                    hasExistingPositions = activeLegCount > 0,
                    onAddToBasket = { onAddStrategyToBasket(template) },
                    onReplaceBasket = { onReplaceWithStrategy(template) }
                )
            }
        }
    }
}

@Composable
fun BuilderSegmentedControl(
    selectedSegment: BuilderSegment,
    onSelect: (BuilderSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE2E8F0).copy(alpha = 0.6f))
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BuilderSegment.entries.forEach { segment ->
            val selected = selectedSegment == segment
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) TerminalSurface else Color.Transparent)
                    .clickable { onSelect(segment) }
                    .padding(vertical = 8.dp, horizontal = 10.dp)
                    .testTag("segment_${segment.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = segment.label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) RoyalBlueDark else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrategyTemplateCard(
    template: StrategyTemplate,
    params: SimulationParams,
    hasExistingPositions: Boolean,
    onAddToBasket: () -> Unit,
    onReplaceBasket: () -> Unit,
    compact: Boolean = false
) {
    val atm = StrategyCatalog.atmStrike(params.simulatedSpot, params.strikeStep)

    val sparklinePoints = remember(template.id, params.simulatedSpot, params.strikeStep, params.baseIvPct) {
        val sampleLegs = StrategyCatalog.instantiateTemplate(
            template = template,
            params = params,
            groupId = "preview",
            idGenerator = { java.util.UUID.randomUUID().toString() }
        )
        val minS = (params.simulatedSpot * 0.85).coerceAtLeast(params.strikeStep)
        val maxS = params.simulatedSpot * 1.15
        val nearestDte = sampleLegs.minOfOrNull { it.expiryDte } ?: params.nearExpiryDte
        val steps = 26
        (0..steps).map { i ->
            val s = minS + (maxS - minS) * (i.toDouble() / steps)
            sampleLegs.sumOf { leg ->
                val remDte = (leg.expiryDte - nearestDte).coerceAtLeast(0)
                val p = BlackScholesEngine.priceInstrument(
                    type = leg.type,
                    spot = s,
                    strike = leg.strike,
                    timeYears = remDte / 365.0,
                    rateDecimal = params.riskFreeRatePct / 100.0,
                    ivDecimal = leg.entryIvPct / 100.0
                ).price
                (p - leg.entryPrice) * leg.side.sign * leg.lots
            }
        }
    }

    val (outlookColor, outlookBg) = when (template.outlook) {
        MarketOutlook.BULLISH -> ProfitGreen to ProfitGreenMuted
        MarketOutlook.BEARISH -> LossRed to LossRedMuted
        MarketOutlook.NEUTRAL -> CyanAccent to TerminalSurfaceHighlight
        MarketOutlook.VOLATILE -> Color(0xFFD97706) to Color(0xFFFFFBEB)
        MarketOutlook.HEDGED -> VioletAccent to Color(0xFFF5F3FF)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("strategy_card_${template.id}"),
        shape = RoundedCornerShape(10.dp),
        color = TerminalSurface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(if (compact) 10.dp else 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = template.name,
                            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = outlookBg,
                            border = BorderStroke(0.6.dp, outlookColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = template.outlook.label,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = outlookColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MiniPayoffSparkline(
                    points = sparklinePoints,
                    modifier = Modifier.size(width = if (compact) 64.dp else 76.dp, height = if (compact) 38.dp else 44.dp)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                template.legs.forEach { spec ->
                    val k = if (spec.type == InstrumentType.FUT_EQ) {
                        params.simulatedSpot
                    } else {
                        (atm + spec.strikeOffsetSteps * params.strikeStep).coerceAtLeast(params.strikeStep)
                    }
                    val dte = if (spec.isFarExpiry) params.farExpiryDte else params.nearExpiryDte
                    val isBuy = spec.side.sign > 0
                    val sideColor = if (isBuy) ProfitGreen else LossRed
                    val sideBg = if (isBuy) ProfitGreenMuted else LossRedMuted
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = sideBg,
                        border = BorderStroke(0.5.dp, sideColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${spec.side.shortLabel} ${spec.lots}× ${if (spec.type == InstrumentType.FUT_EQ) "EQ" else "${Formatters.formatStrike(k)}${spec.type.label}"} (${dte}D)",
                            fontFamily = TabularNumFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            color = sideColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddToBasket,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_strategy_${template.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasExistingPositions) "Add to Basket" else "Build Strategy",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasExistingPositions) {
                    OutlinedButton(
                        onClick = onReplaceBasket,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TerminalBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TerminalSurfaceElevated,
                            contentColor = TextSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                        modifier = Modifier.testTag("replace_strategy_${template.id}")
                    ) {
                        Text("Load Fresh", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
