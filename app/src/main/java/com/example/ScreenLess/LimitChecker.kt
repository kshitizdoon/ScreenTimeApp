package com.example.ScreenLess

import android.content.Context

class LimitChecker(
    private val context: Context
) {

    fun getUsageToday(packageName: String): Long {

        val minutes =
            UsageRepository.todayUsageMinutes(context)[packageName] ?: 0L

        // Existing callers expect milliseconds.
        return minutes * 60_000
    }
}