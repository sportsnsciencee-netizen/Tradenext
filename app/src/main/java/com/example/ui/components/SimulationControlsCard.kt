package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.SimulationParams
import com.example.ui.Formatters
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoCurve
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenMuted
import com.example.ui.theme.TabularNumFontFamily
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulationControlsCard(
    params: SimulationParams,
    nearestExpiryDte: Int,
    tcsScenarioStep: Int,
    onSpotChange: (Double) -> Unit,
    onStepSpot: (Double) -> Unit,
    onDayChange: (Int) -> Unit,
    onIvShiftChange: (Double) -> Unit,
    onRiskFreeRateChange: (Double) -> Unit,
    onLotSizeChange: (Int) -> Unit,
    onResetSimulation: () -> Unit,
    onTcsStep1: () -> Unit,
    onTcsStep2: () -> Unit,
    onTcsStep3: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isSpotFieldFocused by remember { mutableStateOf(false) }
    var spotInputText by remember {
        mutableStateOf(params.simulatedSpot.roundToInt().toString())
    }
    androidx.compose.runtime.LaunchedEffect(params.simulatedSpot, isSpotFieldFocused) {
        if (!isSpotFieldFocused) {
            spotInputText = params.simulatedSpot.roundToInt().toString()
        }
    }
    var showAdvancedModelParams by remember { mutableStateOf(false) }

    val minSliderSpot = (params.baseSpot * 0.65).coerceAtLeast(params.strikeStep)
    val maxSliderSpot = (params.baseSpot * 1.45).coerceAtLeast(params.simulatedSpot * 1.05)
    val maxDays = nearestExpiryDte.coerceAtLeast(params.nearExpiryDte).coerceAtLeast(1)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("simulation_controls_card"),
        shape = RoundedCornerShape(12.dp),
        color = TerminalSurface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, TerminalBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "What-If Market Simulation",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { showAdvancedModelParams = !showAdvancedModelParams },
                        shape = RoundedCornerShape(8.dp),
                        color = if (showAdvancedModelParams) TerminalSurfaceHighlight else TerminalSurfaceElevated,
                        border = BorderStroke(1.dp, if (showAdvancedModelParams) CyanAccent else TerminalBorder)
                    ) {
                        Text(
                            text = "Lot ${params.lotSize} • r ${Formatters.formatDecimal(params.riskFreeRatePct, 1)}%",
                            fontFamily = TabularNumFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            color = if (showAdvancedModelParams) CyanAccent else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(
                        onClick = onResetSimulation,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("reset_simulation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Simulation",
                            tint = TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spot Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SIMULATED SPOT (₹)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    val pctChange = if (params.baseSpot > 0) {
                        ((params.simulatedSpot - params.baseSpot) / params.baseSpot) * 100.0
                    } else 0.0
                    Text(
                        text = "${Formatters.formatGreek(pctChange, 1)}% vs ₹${Formatters.formatStrike(params.baseSpot)}",
                        fontFamily = TabularNumFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        color = when {
                            pctChange > 0.05 -> ProfitGreen
                            pctChange < -0.05 -> LossRed
                            else -> TextSecondary
                        }
                    )
                }

                IconButton(
                    onClick = { onStepSpot(-params.strikeStep) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                        .testTag("spot_minus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease Spot",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedTextField(
                    value = spotInputText,
                    onValueChange = { raw ->
                        spotInputText = raw.filter { it.isDigit() || it == '.' }
                        spotInputText.toDoubleOrNull()?.let { parsed ->
                            if (parsed >= 10.0) onSpotChange(parsed)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFeatureSettings = "tnum",
                        color = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            spotInputText.toDoubleOrNull()?.let { onSpotChange(it) }
                            focusManager.clearFocus()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = TerminalBorder,
                        focusedContainerColor = TerminalSurface,
                        unfocusedContainerColor = TerminalSurfaceElevated
                    ),
                    modifier = Modifier
                        .width(96.dp)
                        .onFocusChanged { isSpotFieldFocused = it.isFocused }
                        .testTag("spot_input_field")
                )

                IconButton(
                    onClick = { onStepSpot(params.strikeStep) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                        .testTag("spot_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase Spot",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Slider(
                value = params.simulatedSpot.toFloat().coerceIn(minSliderSpot.toFloat(), maxSliderSpot.toFloat()),
                onValueChange = { onSpotChange(it.roundToInt().toDouble()) },
                valueRange = minSliderSpot.toFloat()..maxSliderSpot.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = CyanAccent,
                    activeTrackColor = CyanAccent,
                    inactiveTrackColor = TerminalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spot_slider")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(-10, -5, 0, 10, 25).forEach { pct ->
                    val target = ((params.baseSpot * (1.0 + pct / 100.0)) / params.strikeStep).roundToInt() * params.strikeStep
                    val isSelected = kotlin.math.abs(params.simulatedSpot - target) < 1.0
                    QuickChip(
                        label = if (pct == 0) "Base ₹${Formatters.formatStrike(target)}" else "${if (pct > 0) "+" else ""}$pct% (₹${Formatters.formatStrike(target)})",
                        selected = isSelected,
                        onClick = { onSpotChange(target) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Target Date / Time Decay Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET DATE (TIME DECAY)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = if (params.simulationDayOffset == 0) {
                        "Today (T+0) • ${maxDays}D to Expiry"
                    } else {
                        "T+${params.simulationDayOffset} Day(s) • ${(maxDays - params.simulationDayOffset).coerceAtLeast(0)}D left"
                    },
                    fontFamily = TabularNumFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = IndigoCurve
                )
            }

            Slider(
                value = params.simulationDayOffset.toFloat().coerceIn(0f, maxDays.toFloat()),
                onValueChange = { onDayChange(it.roundToInt()) },
                valueRange = 0f..maxDays.toFloat(),
                steps = (maxDays - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = IndigoCurve,
                    activeTrackColor = IndigoCurve,
                    inactiveTrackColor = TerminalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("day_slider")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickChip(label = "Today (T+0)", selected = params.simulationDayOffset == 0) { onDayChange(0) }
                QuickChip(label = "+1 Day (T+1)", selected = params.simulationDayOffset == 1) { onDayChange(1) }
                if (maxDays >= 3) {
                    QuickChip(label = "+3 Days", selected = params.simulationDayOffset == 3) { onDayChange(3) }
                }
                QuickChip(label = "Near Exp (T+$maxDays)", selected = params.simulationDayOffset == maxDays) { onDayChange(maxDays) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // IV % Shift Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IMPLIED VOLATILITY SHIFT (IV)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                val effIv = (params.baseIvPct + params.ivShiftPct).coerceAtLeast(1.0)
                Text(
                    text = "${Formatters.formatDecimal(effIv, 1)}% (${Formatters.formatGreek(params.ivShiftPct, 1)}%)",
                    fontFamily = TabularNumFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    color = TextPrimary
                )
            }

            Slider(
                value = params.ivShiftPct.toFloat().coerceIn(-15f, 30f),
                onValueChange = { onIvShiftChange((it * 2f).roundToInt() / 2.0) },
                valueRange = -15f..30f,
                colors = SliderDefaults.colors(
                    thumbColor = ProfitGreen,
                    activeTrackColor = ProfitGreen,
                    inactiveTrackColor = TerminalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("iv_slider")
            )

            if (showAdvancedModelParams) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TerminalSurfaceElevated)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var lotInput by remember(params.lotSize) { mutableStateOf(params.lotSize.toString()) }
                    var rateInput by remember(params.riskFreeRatePct) { mutableStateOf(Formatters.formatDecimal(params.riskFreeRatePct, 1)) }

                    OutlinedTextField(
                        value = lotInput,
                        onValueChange = { txt ->
                            lotInput = txt.filter { it.isDigit() }
                            lotInput.toIntOrNull()?.let { if (it > 0) onLotSizeChange(it) }
                        },
                        label = { Text("Lot Size", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { txt ->
                            rateInput = txt.filter { it.isDigit() || it == '.' }
                            rateInput.toDoubleOrNull()?.let { onRiskFreeRateChange(it) }
                        },
                        label = { Text("Risk-Free Rate %", fontSize = 10.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // TCS Interactive Walkthrough Strip
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TerminalSurfaceHighlight)
                    .border(1.dp, CyanAccent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "TCS SCENARIO WALKTHROUGH (CALENDAR + DOUBLE CALENDAR)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StepActionButton(
                        label = "1. Load TCS ₹2,000",
                        active = tcsScenarioStep == 1,
                        onClick = onTcsStep1,
                        testTag = "tcs_step1_btn"
                    )
                    StepActionButton(
                        label = "2. Jump ₹2,500 +1D",
                        active = tcsScenarioStep == 2,
                        onClick = onTcsStep2,
                        testTag = "tcs_step2_btn"
                    )
                    StepActionButton(
                        label = "3. + Add 2500 CE Leg",
                        active = tcsScenarioStep == 3,
                        onClick = onTcsStep3,
                        testTag = "tcs_step3_btn"
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        color = if (selected) TerminalSurfaceHighlight else TerminalSurfaceElevated,
        border = BorderStroke(1.dp, if (selected) CyanAccent else TerminalBorder)
    ) {
        Text(
            text = label,
            fontFamily = TabularNumFontFamily,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = TextStyle(fontFeatureSettings = "tnum"),
            color = if (selected) CyanAccent else TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StepActionButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, if (active) ProfitGreen else TerminalBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) ProfitGreenMuted else TerminalSurface,
            contentColor = if (active) ProfitGreen else TextPrimary
        ),
        modifier = Modifier.testTag(testTag)
    ) {
        Text(
            text = label,
            fontFamily = TabularNumFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}
