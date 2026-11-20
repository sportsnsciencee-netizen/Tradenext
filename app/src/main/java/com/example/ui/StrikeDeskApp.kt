package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.domain.OptionLeg
import com.example.domain.StrategyCatalog
import com.example.ui.components.AddEditLegSheet
import com.example.ui.components.BuilderSegment
import com.example.ui.components.KpiAndGreeksSection
import com.example.ui.components.PayoffChartCard
import com.example.ui.components.PositionBlotterSection
import com.example.ui.components.SimulationControlsCard
import com.example.ui.components.StrategyLibraryScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedMuted
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenMuted
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.TabularNumFontFamily
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceElevated
import com.example.ui.theme.TerminalSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class TerminalTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ANALYZER("Payoff Lab", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    STRATEGIES("Strategies", Icons.Filled.Layers, Icons.Outlined.Layers),
    POSITIONS("Positions", Icons.AutoMirrored.Filled.ViewList, Icons.AutoMirrored.Outlined.ViewList)
}

@Composable
fun StrikeDeskApp(
    viewModel: StrikeDeskViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(TerminalTab.ANALYZER) }
    var builderSegment by remember { mutableStateOf(BuilderSegment.READY_STRATEGIES) }
    var showAddLegSheet by remember { mutableStateOf(false) }
    var editingLeg by remember { mutableStateOf<OptionLeg?>(null) }
    var showModelInfoDialog by remember { mutableStateOf(false) }
    var showLotSizeDialog by remember { mutableStateOf(false) }
    var showStrategyBrowserInDesktop by remember { mutableStateOf(false) }

    val activeLegCount = state.legs.count { it.enabled }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        val isWideScreen = maxWidth >= 780.dp

        BackHandler(enabled = !isWideScreen && activeTab != TerminalTab.ANALYZER) {
            activeTab = TerminalTab.ANALYZER
        }
        BackHandler(enabled = isWideScreen && showStrategyBrowserInDesktop) {
            showStrategyBrowserInDesktop = false
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = TerminalBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TradeNextTopBar(
                    state = state,
                    isWideScreen = isWideScreen,
                    onSelectUnderlying = viewModel::selectUnderlying,
                    onLotBadgeClick = { showLotSizeDialog = true },
                    onLoadTcsPreset = viewModel::loadTcsSampleScenario,
                    onClearAll = viewModel::clearAllLegs,
                    onShowInfo = { showModelInfoDialog = true }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    Column {
                        HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
                        NavigationBar(
                            containerColor = TerminalSurface,
                            contentColor = TextPrimary,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            TerminalTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { activeTab = tab },
                                    icon = {
                                        if (tab == TerminalTab.POSITIONS && activeLegCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = CyanAccent,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text("$activeLegCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanAccent,
                                        selectedTextColor = CyanAccent,
                                        indicatorColor = TerminalSurfaceHighlight,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Status Feedback Banner
                AnimatedVisibility(visible = state.statusBanner != null) {
                    state.statusBanner?.let { msg ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = ProfitGreenMuted,
                            border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = ProfitGreen,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = viewModel::dismissBanner,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss Banner",
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    // Responsive 2-Column Pro Trading Workspace (~36% Left Sidebar | ~64% Right Main Panel)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Left Panel (Strategy & Position Manager ~36% width)
                        Surface(
                            modifier = Modifier
                                .weight(0.36f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            color = TerminalSurface,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, TerminalBorder)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Left Panel Top Toggle: Basket vs Ready Strategies / Custom Leg
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(TerminalSurfaceElevated)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showStrategyBrowserInDesktop = false },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (!showStrategyBrowserInDesktop) CyanAccent else TerminalBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (!showStrategyBrowserInDesktop) TerminalSurfaceHighlight else TerminalSurface,
                                            contentColor = if (!showStrategyBrowserInDesktop) RoyalBlueDark else TextSecondary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Active Basket ($activeLegCount)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { showStrategyBrowserInDesktop = true },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (showStrategyBrowserInDesktop) CyanAccent else TerminalBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (showStrategyBrowserInDesktop) TerminalSurfaceHighlight else TerminalSurface,
                                            contentColor = if (showStrategyBrowserInDesktop) RoyalBlueDark else TextSecondary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Builder / Strategies",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

                                if (showStrategyBrowserInDesktop) {
                                    StrategyLibraryScreen(
                                        params = state.params,
                                        activeLegCount = state.legs.size,
                                        selectedSegment = builderSegment,
                                        onSegmentChange = { builderSegment = it },
                                        onAddStrategyToBasket = { template ->
                                            viewModel.addStrategyToBasket(template, replaceExisting = false)
                                            showStrategyBrowserInDesktop = false
                                        },
                                        onReplaceWithStrategy = { template ->
                                            viewModel.addStrategyToBasket(template, replaceExisting = true)
                                            showStrategyBrowserInDesktop = false
                                        },
                                        onAddCustomLeg = { side, type, strike, lots, dte, price, iv ->
                                            viewModel.addCustomLegToRunningTrade(side, type, strike, lots, dte, price, iv)
                                            showStrategyBrowserInDesktop = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        PositionBlotterSection(
                                            analysis = state.analysis,
                                            params = state.params,
                                            onAddStrategyClick = {
                                                builderSegment = BuilderSegment.READY_STRATEGIES
                                                showStrategyBrowserInDesktop = true
                                            },
                                            onAddLegClick = {
                                                editingLeg = null
                                                showAddLegSheet = true
                                            },
                                            onEditLegClick = { leg ->
                                                editingLeg = leg
                                                showAddLegSheet = true
                                            },
                                            onToggleLeg = viewModel::toggleLegEnabled,
                                            onToggleGroup = viewModel::toggleStrategyGroupEnabled,
                                            onChangeLots = viewModel::changeLegLots,
                                            onDuplicateLeg = viewModel::duplicateLeg,
                                            onRemoveLeg = viewModel::removeLeg,
                                            onRemoveGroup = viewModel::removeStrategyGroup,
                                            onClearAll = viewModel::clearAllLegs,
                                            compactMode = true
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Right Main Panel (~64% width: KPI Summary Strip, Payoff Graph, Simulation Controls, Greeks)
                        Column(
                            modifier = Modifier
                                .weight(0.64f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiAndGreeksSection(
                                analysis = state.analysis,
                                params = state.params
                            )
                            PayoffChartCard(
                                analysis = state.analysis,
                                params = state.params,
                                legs = state.legs,
                                chartHeight = 290.dp
                            )
                            SimulationControlsCard(
                                params = state.params,
                                nearestExpiryDte = state.analysis.nearestExpiryDte,
                                tcsScenarioStep = state.tcsScenarioStep,
                                onSpotChange = viewModel::updateSimulatedSpot,
                                onStepSpot = viewModel::stepSpot,
                                onDayChange = viewModel::setSimulationDay,
                                onIvShiftChange = viewModel::updateIvShift,
                                onRiskFreeRateChange = viewModel::updateRiskFreeRate,
                                onLotSizeChange = viewModel::updateLotSize,
                                onResetSimulation = viewModel::resetSimulationControls,
                                onTcsStep1 = viewModel::loadTcsSampleScenario,
                                onTcsStep2 = viewModel::runTcsStep2SpotJumpAndNextDay,
                                onTcsStep3 = viewModel::runTcsStep3AddAdjustmentLeg
                            )
                            DisclaimerFooter()
                        }
                    }
                } else {
                    // Compact Mobile Flow
                    when (activeTab) {
                        TerminalTab.ANALYZER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiAndGreeksSection(
                                    analysis = state.analysis,
                                    params = state.params
                                )
                                PayoffChartCard(
                                    analysis = state.analysis,
                                    params = state.params,
                                    legs = state.legs,
                                    chartHeight = 240.dp
                                )
                                SimulationControlsCard(
                                    params = state.params,
                                    nearestExpiryDte = state.analysis.nearestExpiryDte,
                                    tcsScenarioStep = state.tcsScenarioStep,
                                    onSpotChange = viewModel::updateSimulatedSpot,
                                    onStepSpot = viewModel::stepSpot,
                                    onDayChange = viewModel::setSimulationDay,
                                    onIvShiftChange = viewModel::updateIvShift,
                                    onRiskFreeRateChange = viewModel::updateRiskFreeRate,
                                    onLotSizeChange = viewModel::updateLotSize,
                                    onResetSimulation = viewModel::resetSimulationControls,
                                    onTcsStep1 = viewModel::loadTcsSampleScenario,
                                    onTcsStep2 = viewModel::runTcsStep2SpotJumpAndNextDay,
                                    onTcsStep3 = viewModel::runTcsStep3AddAdjustmentLeg
                                )
                                PositionBlotterSection(
                                    analysis = state.analysis,
                                    params = state.params,
                                    onAddStrategyClick = {
                                        builderSegment = BuilderSegment.READY_STRATEGIES
                                        activeTab = TerminalTab.STRATEGIES
                                    },
                                    onAddLegClick = {
                                        editingLeg = null
                                        showAddLegSheet = true
                                    },
                                    onEditLegClick = { leg ->
                                        editingLeg = leg
                                        showAddLegSheet = true
                                    },
                                    onToggleLeg = viewModel::toggleLegEnabled,
                                    onToggleGroup = viewModel::toggleStrategyGroupEnabled,
                                    onChangeLots = viewModel::changeLegLots,
                                    onDuplicateLeg = viewModel::duplicateLeg,
                                    onRemoveLeg = viewModel::removeLeg,
                                    onRemoveGroup = viewModel::removeStrategyGroup,
                                    onClearAll = viewModel::clearAllLegs,
                                    compactMode = true
                                )
                                DisclaimerFooter()
                            }
                        }

                        TerminalTab.STRATEGIES -> {
                            StrategyLibraryScreen(
                                params = state.params,
                                activeLegCount = state.legs.size,
                                selectedSegment = builderSegment,
                                onSegmentChange = { builderSegment = it },
                                onAddStrategyToBasket = { template ->
                                    viewModel.addStrategyToBasket(template, replaceExisting = false)
                                    activeTab = TerminalTab.ANALYZER
                                },
                                onReplaceWithStrategy = { template ->
                                    viewModel.addStrategyToBasket(template, replaceExisting = true)
                                    activeTab = TerminalTab.ANALYZER
                                },
                                onAddCustomLeg = { side, type, strike, lots, dte, price, iv ->
                                    viewModel.addCustomLegToRunningTrade(side, type, strike, lots, dte, price, iv)
                                    activeTab = TerminalTab.ANALYZER
                                }
                            )
                        }

                        TerminalTab.POSITIONS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiAndGreeksSection(
                                    analysis = state.analysis,
                                    params = state.params
                                )
                                PositionBlotterSection(
                                    analysis = state.analysis,
                                    params = state.params,
                                    onAddStrategyClick = {
                                        builderSegment = BuilderSegment.READY_STRATEGIES
                                        activeTab = TerminalTab.STRATEGIES
                                    },
                                    onAddLegClick = {
                                        editingLeg = null
                                        showAddLegSheet = true
                                    },
                                    onEditLegClick = { leg ->
                                        editingLeg = leg
                                        showAddLegSheet = true
                                    },
                                    onToggleLeg = viewModel::toggleLegEnabled,
                                    onToggleGroup = viewModel::toggleStrategyGroupEnabled,
                                    onChangeLots = viewModel::changeLegLots,
                                    onDuplicateLeg = viewModel::duplicateLeg,
                                    onRemoveLeg = viewModel::removeLeg,
                                    onRemoveGroup = viewModel::removeStrategyGroup,
                                    onClearAll = viewModel::clearAllLegs,
                                    compactMode = true
                                )
                                DisclaimerFooter()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddLegSheet) {
        AddEditLegSheet(
            params = state.params,
            editingLeg = editingLeg,
            onDismiss = {
                showAddLegSheet = false
                editingLeg = null
            },
            onConfirmAdd = { side, type, strike, lots, dte, price, iv ->
                viewModel.addCustomLegToRunningTrade(
                    side = side,
                    type = type,
                    strike = strike,
                    lots = lots,
                    expiryDte = dte,
                    entryPrice = price,
                    entryIvPct = iv
                )
            },
            onConfirmEdit = { updated ->
                viewModel.updateExistingLeg(updated)
            }
        )
    }

    if (showLotSizeDialog) {
        var lotInput by remember(state.params.lotSize) { mutableStateOf(state.params.lotSize.toString()) }
        AlertDialog(
            onDismissRequest = { showLotSizeDialog = false },
            containerColor = TerminalSurface,
            title = { Text("Configure Lot Size (${state.params.underlyingSymbol})", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = lotInput,
                    onValueChange = { lotInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Shares per Lot") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        lotInput.toIntOrNull()?.let { if (it > 0) viewModel.updateLotSize(it) }
                        showLotSizeDialog = false
                    }
                ) {
                    Text("Apply", color = CyanAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLotSizeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showModelInfoDialog) {
        AlertDialog(
            onDismissRequest = { showModelInfoDialog = false },
            containerColor = TerminalSurface,
            title = {
                Text("TradeNext Payoff & Valuation Engine", color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Multi-Expiry Pricing: Near-expiry legs settle at intrinsic value on expiry while far-month legs retain remaining Black-Scholes time value.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Running Trade Adjustments: Existing legs preserve their original entry price, IV, and entry spot when adding new hedge or adjustment legs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = stringResource(R.string.disclaimer_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberAccent
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelInfoDialog = false }) {
                    Text("Close", color = CyanAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TradeNextTopBar(
    state: StrikeDeskUiState,
    isWideScreen: Boolean,
    onSelectUnderlying: (String) -> Unit,
    onLotBadgeClick: () -> Unit,
    onLoadTcsPreset: () -> Unit,
    onClearAll: () -> Unit,
    onShowInfo: () -> Unit
) {
    var tickerMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = TerminalSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: TradeNext Brand Logo + Wordmark + Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Logo + Wordmark + Tagline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .testTag("tradenext_brand_header")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tradenext_logo),
                        contentDescription = "TradeNext Logo",
                        modifier = Modifier.size(30.dp)
                    )
                    Column {
                        Text(
                            text = "TradeNext",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isWideScreen) {
                        TopMarketControls(
                            state = state,
                            tickerMenuExpanded = tickerMenuExpanded,
                            onTickerMenuToggle = { tickerMenuExpanded = it },
                            onSelectUnderlying = onSelectUnderlying,
                            onLotBadgeClick = onLotBadgeClick
                        )
                    }

                    Surface(
                        onClick = onLoadTcsPreset,
                        shape = RoundedCornerShape(8.dp),
                        color = TerminalSurfaceHighlight,
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("tcs_preset_topbar_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "TCS Preset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlueDark
                            )
                        }
                    }

                    Surface(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(8.dp),
                        color = LossRedMuted,
                        border = BorderStroke(1.dp, LossRed.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("topbar_reset_clear_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset / Clear All",
                                tint = LossRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LossRed
                            )
                        }
                    }

                    IconButton(
                        onClick = onShowInfo,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Model & Disclaimer Info",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // On mobile, show compact secondary ticker + spot + lot size strip right inside the white top bar
            if (!isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopMarketControls(
                        state = state,
                        tickerMenuExpanded = tickerMenuExpanded,
                        onTickerMenuToggle = { tickerMenuExpanded = it },
                        onSelectUnderlying = onSelectUnderlying,
                        onLotBadgeClick = onLotBadgeClick
                    )
                }
            }

            HorizontalDivider(color = TerminalBorder, thickness = 1.dp)
        }
    }
}

@Composable
private fun TopMarketControls(
    state: StrikeDeskUiState,
    tickerMenuExpanded: Boolean,
    onTickerMenuToggle: (Boolean) -> Unit,
    onSelectUnderlying: (String) -> Unit,
    onLotBadgeClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            Surface(
                onClick = { onTickerMenuToggle(true) },
                shape = RoundedCornerShape(8.dp),
                color = TerminalSurfaceElevated,
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.testTag("underlying_selector")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = state.params.underlyingSymbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Underlying",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = tickerMenuExpanded,
                onDismissRequest = { onTickerMenuToggle(false) },
                containerColor = TerminalSurface
            ) {
                StrategyCatalog.underlyings.forEach { cfg ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${cfg.symbol} • ${cfg.displayName}", color = TextPrimary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "₹${Formatters.formatStrike(cfg.defaultSpot)}",
                                    fontFamily = TabularNumFontFamily,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyanAccent,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        onClick = {
                            onTickerMenuToggle(false)
                            onSelectUnderlying(cfg.symbol)
                        }
                    )
                }
            }
        }

        // Spot Price Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TerminalSurfaceHighlight,
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.25f))
        ) {
            Text(
                text = "Spot ₹${Formatters.formatStrike(state.params.simulatedSpot)}",
                fontFamily = TabularNumFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = RoyalBlueDark,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }

        // Lot Size Badge (tap to edit)
        Surface(
            onClick = onLotBadgeClick,
            shape = RoundedCornerShape(8.dp),
            color = TerminalSurfaceElevated,
            border = BorderStroke(1.dp, TerminalBorder),
            modifier = Modifier.testTag("lot_size_badge")
        ) {
            Text(
                text = "Lot ${state.params.lotSize}",
                fontFamily = TabularNumFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun DisclaimerFooter() {
    Text(
        text = stringResource(R.string.disclaimer_text),
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
