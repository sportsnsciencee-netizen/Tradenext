package com.example.ui

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val inrLocale: Locale = Locale.forLanguageTag("en-IN")

    fun formatDecimal(value: Double, decimals: Int = 1): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    fun formatInr(value: Double, showSign: Boolean = false, decimals: Int = 0): String {
        val absVal = abs(value)
        val nf = NumberFormat.getNumberInstance(inrLocale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }
        val formatted = nf.format(absVal)
        return when {
            value < -0.005 -> "-₹$formatted"
            showSign && value > 0.005 -> "+₹$formatted"
            else -> "₹$formatted"
        }
    }

    fun formatCompactInr(value: Double, showSign: Boolean = false): String {
        val absVal = abs(value)
        val sign = when {
            value < -0.005 -> "-"
            showSign && value > 0.005 -> "+"
            else -> ""
        }
        return when {
            absVal >= 10_000_000.0 -> "${sign}₹${String.format(Locale.US, "%.2f", absVal / 10_000_000.0)}Cr"
            absVal >= 100_000.0 -> "${sign}₹${String.format(Locale.US, "%.2f", absVal / 100_000.0)}L"
            absVal >= 10_000.0 -> "${sign}₹${String.format(Locale.US, "%.1f", absVal / 1_000.0)}K"
            else -> formatInr(value, showSign = showSign, decimals = 0)
        }
    }

    fun formatPrice(value: Double): String {
        val nf = NumberFormat.getNumberInstance(inrLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "₹${nf.format(value)}"
    }

    fun formatStrike(value: Double): String {
        val nf = NumberFormat.getNumberInstance(inrLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        return nf.format(value)
    }

    fun formatGreek(value: Double, decimals: Int = 2): String {
        val pattern = "%.${decimals}f"
        val formatted = String.format(Locale.US, pattern, abs(value))
        return when {
            value < -1e-6 -> "-$formatted"
            value > 1e-6 -> "+$formatted"
            else -> formatted
        }
    }
}
