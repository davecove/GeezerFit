package com.softoolsmith.geezerfit.util

import java.util.Locale
import kotlin.math.roundToInt

private const val KG_PER_LB = 0.45359237f
private const val FT_PER_M = 3.28084f

fun unitLabel(metric: Boolean) = if (metric) "kg" else "lbs"

/** Weights are always STORED in lbs; this converts for display. */
fun lbsToDisplay(lbs: Float, metric: Boolean): Float =
    if (metric) (lbs * KG_PER_LB * 10).roundToInt() / 10f else lbs

fun displayToLbs(value: Float, metric: Boolean): Float =
    if (metric) value / KG_PER_LB else value

fun formatNumber(f: Float): String =
    if (f % 1f == 0f) f.toInt().toString() else String.format(Locale.US, "%.1f", f)

fun formatWeight(lbs: Float, metric: Boolean): String =
    formatNumber(lbsToDisplay(lbs, metric)) + " " + unitLabel(metric)

private val METERS = Regex("""(\d+(?:\.\d+)?)\s?(meters|meter|m)\b""")

/** Plan text stores distances in meters; show feet when Imperial is selected. */
fun localizeDistance(text: String, metric: Boolean): String =
    if (metric) text
    else METERS.replace(text) { m -> "${(m.groupValues[1].toFloat() * FT_PER_M).roundToInt()} ft" }
