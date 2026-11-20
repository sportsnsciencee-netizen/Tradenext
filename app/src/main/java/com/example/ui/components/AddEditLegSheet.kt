package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BlackScholesEngine
import com.example.domain.InstrumentType
import com.example.domain.OptionLeg
import com.example.domain.Side
import com.example.domain.SimulationParams
import com.example.domain.StrategyCatalog
import com.example.ui.Formatters
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedMuted
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditLegSheet(
    params: SimulationParams,
    editingLeg: OptionLeg?,
    onDismiss: () -> Unit,
    onConfirmAdd: (
        side: Side,
        type: InstrumentType,
        strike: Double,
        lots: Int,
        expiryDte: Int,
        entryPrice: Double,
        entryIvPct: Double
    ) -> Unit,
    onConfirmEdit: (OptionLeg) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TerminalSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editingLeg == null) "Add Leg to Running Trade" else "Edit Option Leg",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = if (editingLeg == null) {
                    "Priced at simulated spot ₹${Formatters.formatStrike(params.simulatedSpot)} (Day T+${params.simulationDayOffset}). Existing legs stay untouched."
                } else {
                    "Modify strike, quantity, expiry, entry price, or IV for this leg."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            CustomLegFormContent(
                params = params,
                editingLeg = editingLeg,
                submitButtonLabel = if (editingLeg == null) "Add Leg to Active Basket" else "Save Leg Changes",
                onSubmit = { side, type, strike, lots, dte, price, iv ->
                    if (editingLeg == null) {
                        onConfirmAdd(side, type, strike, lots, dte, price, iv)
                    } else {
                        onConfirmEdit(
                            editingLeg.copy(
                                side = side,
                                type = type,
                                strike = strike,
                                lots = lots,
                                expiryDte = dte,
                                entryPrice = price,
                                entryIvPct = iv
                            )
                        )
                    }
                    onDismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomLegFormContent(
    params: SimulationParams,
    editingLeg: OptionLeg? = null,
    submitButtonLabel: String = "+ Add Leg to Running Trade",
    onSubmit: (
        side: Side,
        type: InstrumentType,
        strike: Double,
        lots: Int,
        expiryDte: Int,
        entryPrice: Double,
        entryIvPct: Double
    ) -> Unit
) {
    val defaultStrike = editingLeg?.strike ?: StrategyCatalog.atmStrike(params.simulatedSpot, params.strikeStep)
    val defaultIv = editingLeg?.entryIvPct ?: (params.baseIvPct + params.ivShiftPct).coerceAtLeast(1.0)
    val defaultExpiry = editingLeg?.expiryDte ?: params.nearExpiryDte

    var side by remember(editingLeg?.id) { mutableStateOf(editingLeg?.side ?: Side.BUY) }
    var type by remember(editingLeg?.id) { mutableStateOf(editingLeg?.type ?: InstrumentType.CE) }
    var strikeText by remember(editingLeg?.id, defaultStrike) {
        mutableStateOf(Formatters.formatStrike(defaultStrike).replace(",", ""))
    }
    var lotsText by remember(editingLeg?.id) { mutableStateOf((editingLeg?.lots ?: 1).toString()) }
    var expiryDteText by remember(editingLeg?.id) { mutableStateOf(defaultExpiry.toString()) }
    var ivText by remember(editingLeg?.id) { mutableStateOf(Formatters.formatDecimal(defaultIv, 1)) }

    val initialPrice = editingLeg?.entryPrice ?: BlackScholesEngine.fairEntryPrice(
        type = type,
        spot = params.simulatedSpot,
        strike = defaultStrike,
        remainingDte = (defaultExpiry - params.simulationDayOffset).coerceAtLeast(1),
        ratePct = params.riskFreeRatePct,
        ivPct = defaultIv
    )
    var priceText by remember(editingLeg?.id, initialPrice) { mutableStateOf(Formatters.formatDecimal(initialPrice, 2)) }

    fun recomputeFairPrice() {
        val k = strikeText.toDoubleOrNull() ?: defaultStrike
        val dte = expiryDteText.toIntOrNull() ?: defaultExpiry
        val iv = ivText.toDoubleOrNull() ?: defaultIv
        val remDte = (dte - params.simulationDayOffset).coerceAtLeast(0)
        val fair = BlackScholesEngine.fairEntryPrice(
            type = type,
            spot = params.simulatedSpot,
            strike = k,
            remainingDte = remDte,
            ratePct = params.riskFreeRatePct,
            ivPct = iv
        )
        priceText = Formatters.formatDecimal(fair, 2)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Side & Instrument Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Side.entries.forEach { s ->
                val selected = side == s
                val accent = if (s == Side.BUY) ProfitGreen else LossRed
                val bg = if (s == Side.BUY) ProfitGreenMuted else LossRedMuted
                Surface(
                    onClick = { side = s },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) bg else TerminalSurfaceElevated,
                    border = BorderStroke(1.dp, if (selected) accent else TerminalBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = s.label,
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = if (selected) accent else TextSecondary,
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InstrumentType.entries.forEach { t ->
                val selected = type == t
                Surface(
                    onClick = {
                        type = t
                        recomputeFairPrice()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) TerminalSurfaceHighlight else TerminalSurfaceElevated,
                    border = BorderStroke(1.dp, if (selected) CyanAccent else TerminalBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = t.label,
                        fontFamily = TabularNumFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selected) CyanAccent else TextSecondary,
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp)
                    )
                }
            }
        }

        if (type != InstrumentType.FUT_EQ) {
            val atm = StrategyCatalog.atmStrike(params.simulatedSpot, params.strikeStep)
            Text(
                text = "QUICK STRIKES (AROUND ₹${Formatters.formatStrike(atm)})",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(-3, -2, -1, 0, 1, 2, 3).forEach { step ->
                    val k = (atm + step * params.strikeStep).coerceAtLeast(params.strikeStep)
                    val selected = strikeText.toDoubleOrNull() == k
                    Surface(
                        onClick = {
                            strikeText = Formatters.formatStrike(k).replace(",", "")
                            recomputeFairPrice()
                        },
                        shape = RoundedCornerShape(7.dp),
                        color = if (selected) TerminalSurfaceHighlight else TerminalSurfaceElevated,
                        border = BorderStroke(1.dp, if (selected) CyanAccent else TerminalBorder)
                    ) {
                        Text(
                            text = Formatters.formatStrike(k),
                            fontFamily = TabularNumFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            color = if (selected) CyanAccent else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = strikeText,
                onValueChange = { strikeText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Strike (₹)") },
                singleLine = true,
                enabled = type != InstrumentType.FUT_EQ,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lotsText,
                onValueChange = { lotsText = it.filter { c -> c.isDigit() } },
                label = { Text("Lots (×${params.lotSize})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Text("EXPIRY (DTE FROM START)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Near (${params.nearExpiryDte}D)" to params.nearExpiryDte,
                "Mid (14D)" to 14,
                "Far (${params.farExpiryDte}D)" to params.farExpiryDte,
                "Next Mo (60D)" to 60
            ).forEach { (label, dte) ->
                val selected = expiryDteText.toIntOrNull() == dte
                Surface(
                    onClick = {
                        expiryDteText = dte.toString()
                        recomputeFairPrice()
                    },
                    shape = RoundedCornerShape(7.dp),
                    color = if (selected) TerminalSurfaceHighlight else TerminalSurfaceElevated,
                    border = BorderStroke(1.dp, if (selected) CyanAccent else TerminalBorder)
                ) {
                    Text(
                        text = label,
                        fontFamily = TabularNumFontFamily,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        color = if (selected) CyanAccent else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expiryDteText,
                onValueChange = { expiryDteText = it.filter { c -> c.isDigit() } },
                label = { Text("Expiry DTE") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ivText,
                onValueChange = { ivText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Entry IV %") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Entry Price (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1.35f)
            )
            OutlinedButton(
                onClick = { recomputeFairPrice() },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyanAccent),
                modifier = Modifier.weight(1f)
            ) {
                Text("Calc BS Fair", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyanAccent)
            }
        }

        Button(
            onClick = {
                val k = strikeText.toDoubleOrNull() ?: defaultStrike
                val l = (lotsText.toIntOrNull() ?: 1).coerceAtLeast(1)
                val dte = (expiryDteText.toIntOrNull() ?: defaultExpiry).coerceAtLeast(1)
                val iv = (ivText.toDoubleOrNull() ?: defaultIv).coerceAtLeast(1.0)
                val pr = (priceText.toDoubleOrNull() ?: initialPrice).coerceAtLeast(0.05)
                onSubmit(side, type, k, l, dte, pr, iv)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("confirm_leg_button")
        ) {
            Text(
                text = submitButtonLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
        }
    }
}
