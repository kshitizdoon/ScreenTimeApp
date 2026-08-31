package com.example.ScreenLess
import android.graphics.drawable.Drawable

data class AppUsage(
    val appName: String,
    val packageName: String,
    val usageMillis: Long,
    val icon: Drawable?,
    val dailyLimitMinutes: Int? = null
) {
    val usageMinutes: Long
        get() = usageMillis / 60_000

    val formattedUsage: String
        get() {
            val totalMinutes = usageMillis / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60

            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }
}