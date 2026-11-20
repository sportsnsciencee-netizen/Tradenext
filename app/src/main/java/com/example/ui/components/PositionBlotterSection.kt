package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.InstrumentType
import com.example.domain.LegValuation
import com.example.domain.OptionLeg
import com.example.domain.PayoffAnalysis
import com.example.domain.Side
import com.example.domain.SimulationParams
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

@Composable
fun PositionBlotterSection(
    analysis: PayoffAnalysis,
    params: SimulationParams,
    onAddStrategyClick: (() -> Unit)? = null,
    onAddLegClick: () -> Unit,
    onEditLegClick: (OptionLeg) -> Unit,
    onToggleLeg: (String) -> Unit,
    onToggleGroup: (String, Boolean) -> Unit,
    onChangeLots: (String, Int) -> Unit,
    onDuplicateLeg: (String) -> Unit,
    onRemoveLeg: (String) -> Unit,
    onRemoveGroup: (String) -> Unit,
    onClearAll: () -> Unit,
    compactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val valuations = analysis.legValuations
    val grouped = valuations.groupBy { it.leg.strategyGroupId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("position_blotter_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active Strategy Basket (${valuations.count { it.leg.enabled }}/${valuations.size} Legs)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Existing legs retain entry prices; new legs price at ₹${Formatters.formatStrike(params.simulatedSpot)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (valuations.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAll,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    border = BorderStroke(1.dp, LossRed.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = LossRedMuted,
                        contentColor = LossRed
                    ),
                    modifier = Modifier.testTag("clear_basket_button")
                ) {
                    Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Prominent + Add Strategy & + Add Leg to Running Trade Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onAddStrategyClick != null) {
                OutlinedButton(
                    onClick = onAddStrategyClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = TerminalSurfaceHighlight,
                        contentColor = RoyalBlueDark
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_strategy_cta_button")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("+ Add Strategy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Button(
                onClick = onAddLegClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .testTag("add_leg_running_trade_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (compactMode) "+ Add Leg to Trade" else "+ Add Leg to Running Trade",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (grouped.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = TerminalSurface,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, TerminalBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No active legs in Strategy Basket",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a Ready Strategy, load the TCS Preset, or add a custom leg.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            grouped.forEach { (groupId, groupVals) ->
                val groupName = groupVals.first().leg.strategyGroupName
                val allEnabled = groupVals.all { it.leg.enabled }
                val groupPnl = groupVals.filter { it.leg.enabled }.sumOf { it.pnl }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalSurface,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, TerminalBorder)
                ) {
                    Column {
                        // Strategy Group Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TerminalSurfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Switch(
                                    checked = allEnabled,
                                    onCheckedChange = { onToggleGroup(groupId, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CyanAccent,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = TerminalBorder
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${groupVals.size} leg(s) • ${groupVals.sumOf { it.leg.lots }} lots",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Formatters.formatInr(groupPnl, showSign = true),
                                    fontFamily = TabularNumFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                    color = if (groupPnl >= 0) ProfitGreen else LossRed
                                )
                                IconButton(
                                    onClick = { onRemoveGroup(groupId) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Strategy Group",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

                        groupVals.forEachIndexed { idx, legVal ->
                            if (idx > 0) {
                                HorizontalDivider(color = TerminalBorder, thickness = 0.7.dp)
                            }
                            LegBlotterRow(
                                legVal = legVal,
                                onToggle = { onToggleLeg(legVal.leg.id) },
                                onChangeLots = { d -> onChangeLots(legVal.leg.id, d) },
                                onEdit = { onEditLegClick(legVal.leg) },
                                onDuplicate = { onDuplicateLeg(legVal.leg.id) },
                                onRemove = { onRemoveLeg(legVal.leg.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegBlotterRow(
    legVal: LegValuation,
    onToggle: () -> Unit,
    onChangeLots: (Int) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit
) {
    val leg = legVal.leg
    val isBuy = leg.side == Side.BUY
    val rowAlpha = if (leg.enabled) 1f else 0.45f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Row 1: B/S pill + Strike & Type + Expiry + Live P&L
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isBuy) ProfitGreenMuted else LossRedMuted)
                        .border(
                            0.8.dp,
                            if (isBuy) ProfitGreen.copy(alpha = 0.35f) else LossRed.copy(alpha = 0.35f),
                            RoundedCornerShape(5.dp)
                        )
                        .clickable { onToggle() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = leg.side.shortLabel,
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isBuy) ProfitGreen else LossRed
                    )
                }

                Text(
                    text = if (leg.type == InstrumentType.FUT_EQ) {
                        "EQ/FUT"
                    } else {
                        "${Formatters.formatStrike(leg.strike)} ${leg.type.label}"
                    },
                    fontFamily = TabularNumFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = TextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TerminalSurfaceHighlight
                ) {
                    Text(
                        text = "${leg.expiryDte}D (${legVal.remainingDte}D)",
                        fontFamily = TabularNumFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = Formatters.formatInr(legVal.pnl, showSign = true),
                fontFamily = TabularNumFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = if (legVal.pnl >= 0) ProfitGreen else LossRed
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Row 2: Entry vs Simulated Price + Greeks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Entry ${Formatters.formatPrice(leg.entryPrice)} → Sim ${Formatters.formatPrice(legVal.simulatedPrice)}",
                    fontFamily = TabularNumFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = TextPrimary
                )
                Text(
                    text = "IV ${Formatters.formatDecimal(leg.entryIvPct, 1)}% • Added @Spot ₹${Formatters.formatStrike(leg.addedAtSpot)} (T+${leg.addedAtDay})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Δ ${Formatters.formatGreek(legVal.greeks.delta, 1)}  Θ ${Formatters.formatInr(legVal.greeks.theta, showSign = true)}",
                fontFamily = TabularNumFontFamily,
                fontSize = 10.5.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Row 3: Lots Stepper + Quick Toggle + Edit / Duplicate / Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { onChangeLots(-1) },
                    shape = RoundedCornerShape(6.dp),
                    color = TerminalSurfaceElevated,
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Lots",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${leg.lots}L (${leg.lots * leg.lotSize})",
                    fontFamily = TabularNumFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    onClick = { onChangeLots(1) },
                    shape = RoundedCornerShape(6.dp),
                    color = TerminalSurfaceElevated,
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Lots",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    onClick = onToggle,
                    shape = RoundedCornerShape(6.dp),
                    color = if (leg.enabled) ProfitGreenMuted else TerminalSurfaceElevated,
                    border = BorderStroke(1.dp, if (leg.enabled) ProfitGreen.copy(alpha = 0.4f) else TerminalBorder)
                ) {
                    Text(
                        text = if (leg.enabled) "ON" else "OFF",
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (leg.enabled) ProfitGreen else TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Leg",
                        tint = CyanAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onDuplicate, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate Leg",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Leg",
                        tint = LossRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
