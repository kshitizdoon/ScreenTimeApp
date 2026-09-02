package com.example.ScreenLess

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

object UsageRepository {

    fun todayUsageMinutes(context: Context): Map<String, Long> {

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val endTime = System.currentTimeMillis()

        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val usageEvents =
            usageStatsManager.queryEvents(startTime, endTime)

        val event = UsageEvents.Event()

        val totalUsage = mutableMapOf<String, Long>()

        var currentPackage: String? = null
        var currentStart: Long? = null

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event)

            when (event.eventType) {

                UsageEvents.Event.ACTIVITY_RESUMED -> {

                    if (currentPackage != null && currentStart != null) {

                        val duration =
                            event.timeStamp - currentStart!!

                        if (duration > 0) {
                            totalUsage[currentPackage!!] =
                                totalUsage.getOrDefault(
                                    currentPackage!!,
                                    0L
                                ) + duration
                        }
                    }

                    currentPackage = event.packageName
                    currentStart = event.timeStamp
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {

                    if (currentPackage != null && currentStart != null) {

                        val duration =
                            event.timeStamp - currentStart!!

                        if (duration > 0) {
                            totalUsage[currentPackage!!] =
                                totalUsage.getOrDefault(
                                    currentPackage!!,
                                    0L
                                ) + duration
                        }
                    }

                    currentPackage = null
                    currentStart = null
                }
            }
        }

        // Account for the app currently in the foreground.
        if (currentPackage != null && currentStart != null) {

            val duration =
                endTime - currentStart!!

            if (duration > 0) {
                totalUsage[currentPackage!!] =
                    totalUsage.getOrDefault(
                        currentPackage!!,
                        0L
                    ) + duration
            }
        }

        return totalUsage
            .mapValues { (_, milliseconds) ->
                milliseconds / 60_000
            }
            .filterValues { minutes ->
                minutes > 0
            }
    }

    fun todayCategoryMinutes(
        context: Context
    ): Map<AppCategory, Long> {

        val store = CategoryStore(context)

        return todayUsageMinutes(context)
            .flatMap { (packageName, minutes) ->
                store.getCategories(packageName)
                    .map { category -> category to minutes }
            }
            .groupBy { (category, _) -> category }
            .mapValues { (_, entries) ->
                entries.sumOf { (_, minutes) -> minutes }
            }
    }

    fun appsInCategory(
        context: Context,
        category: AppCategory
    ): List<Pair<String, Long>> {

        val store =
            CategoryStore(context)

        val usage =
            todayUsageMinutes(context)

        val packageManager =
            context.packageManager

        return packageManager
            .getInstalledApplications(0)

            // Only normal apps that can be launched
            .filter { app ->

                packageManager
                    .getLaunchIntentForPackage(
                        app.packageName
                    ) != null
            }

            // Only apps assigned to this category
            .filter { app ->

                category in store.getCategories(
                    app.packageName
                )
            }

            // Package name + today's usage
            .map { app ->

                Pair(
                    app.packageName,
                    usage[app.packageName] ?: 0L
                )
            }

            // Most-used apps first
            .sortedByDescending {
                it.second
            }
    }

    fun appLabel(
        context: Context,
        packageName: String
    ): String {

        return try {

            val info =
                context.packageManager.getApplicationInfo(
                    packageName,
                    0
                )

            context.packageManager
                .getApplicationLabel(info)
                .toString()

        } catch (e: Exception) {

            packageName
        }
    }
}
