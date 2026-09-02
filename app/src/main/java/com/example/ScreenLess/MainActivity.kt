package com.example.ScreenLess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ScreenLess.ui.theme.MyApplicationTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class MainActivity : ComponentActivity() {
    private fun getTodayUsage(): List<AppUsage> {

        val usageStatsManager =
            getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager

        val endTime = System.currentTimeMillis()

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis

        val usageEvents =
            usageStatsManager.queryEvents(startTime, endTime)

        val event = android.app.usage.UsageEvents.Event()

        val totalUsage = mutableMapOf<String, Long>()

        var currentPackage: String? = null
        var currentStart: Long? = null

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event)

            when (event.eventType) {

                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {

                    // Another app/activity has taken foreground.
                    // Finish timing whatever we were previously tracking.
                    if (currentPackage != null && currentStart != null) {

                        val duration = event.timeStamp - currentStart!!

                        if (duration > 0) {
                            totalUsage[currentPackage!!] =
                                totalUsage.getOrDefault(currentPackage!!, 0L) +
                                        duration
                        }
                    }

                    currentPackage = event.packageName
                    currentStart = event.timeStamp
                }

                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {

                    // Screen turned off. Stop counting usage.
                    if (currentPackage != null && currentStart != null) {

                        val duration = event.timeStamp - currentStart!!

                        if (duration > 0) {
                            totalUsage[currentPackage!!] =
                                totalUsage.getOrDefault(currentPackage!!, 0L) +
                                        duration
                        }
                    }

                    currentPackage = null
                    currentStart = null
                }
            }
        }

        // The current app may still be open.
        if (currentPackage != null && currentStart != null) {

            val duration = endTime - currentStart!!

            if (duration > 0) {
                totalUsage[currentPackage!!] =
                    totalUsage.getOrDefault(currentPackage!!, 0L) + duration
            }
        }

        val ignoredPackages = setOf(
            "com.sec.android.app.launcher",
            "com.android.systemui"
        )

        return totalUsage
            .filter { it.value >= 60_000 }
            .filter { (packageName, _) ->
                packageName !in ignoredPackages
            }
            .map { (packageName, milliseconds) ->
                AppUsage(
                    appName = getAppName(packageName),
                    packageName = packageName,
                    usageMillis = milliseconds,
                    icon = getAppIcon(packageName),
                    dailyLimitMinutes = getAppLimit(packageName)
                )
            }
            .sortedByDescending { it.usageMillis }
    }
    private fun getAppName(packageName: String): String {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun getAppIcon(packageName: String): android.graphics.drawable.Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
    private fun saveAppLimit(packageName: String, minutes: Int) {
        val preferences =
            getSharedPreferences("screenless_preferences", MODE_PRIVATE)

        preferences.edit()
            .putInt("limit_$packageName", minutes)
            .apply()
    }

    private fun getAppLimit(packageName: String): Int? {
        val preferences =
            getSharedPreferences("screenless_preferences", MODE_PRIVATE)

        val key = "limit_$packageName"

        return if (preferences.contains(key)) {
            preferences.getInt(key, 0)
        } else {
            null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val checker = LimitChecker(this)

        val whatsappUsage =
            checker.getUsageToday("com.whatsapp")

        android.util.Log.d(
            "ScreenLess",
            "WhatsApp usage: ${whatsappUsage / 60_000} minutes"
        )
        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        enableEdgeToEdge()
        setContent {

            MyApplicationTheme {

                // =====================================================
                // NAVIGATION STATE
                // =====================================================

                // Despite the old variable name, this opens Manage Apps.
                var showCategories by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(false)
                }

                // Opens our dedicated Manage Categories screen.
                var showManageCategories by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(false)
                }


                // =====================================================
                // DIALOG STATE
                // =====================================================

                var selectedApp by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf<AppUsage?>(null)
                }

                var dashboardCategory by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf<AppCategory?>(null)
                }

                // =====================================================
                // USAGE STATE
                // =====================================================

                var usage by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(
                        getTodayUsage()
                    )
                }

                var categoryUsage by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(
                        UsageRepository.todayCategoryMinutes(
                            this@MainActivity
                        )
                    )
                }


                // =====================================================
                // PERMISSION STATE
                // =====================================================

                var usageAccessEnabled by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(
                        hasUsageAccess()
                    )
                }

                var accessibilityEnabled by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(
                        hasAccessibilityAccess()
                    )
                }


                // =====================================================
                // REFRESH WHEN SCREENLESS RETURNS TO FOREGROUND
                // =====================================================

                val lifecycleOwner =
                    androidx.lifecycle.compose.LocalLifecycleOwner.current

                androidx.compose.runtime.DisposableEffect(
                    lifecycleOwner
                ) {

                    val observer =
                        androidx.lifecycle.LifecycleEventObserver { _, event ->

                            if (
                                event ==
                                androidx.lifecycle.Lifecycle.Event.ON_RESUME
                            ) {

                                usage =
                                    getTodayUsage()

                                categoryUsage =
                                    UsageRepository.todayCategoryMinutes(
                                        this@MainActivity
                                    )

                                usageAccessEnabled =
                                    hasUsageAccess()

                                accessibilityEnabled =
                                    hasAccessibilityAccess()
                            }
                        }


                    lifecycleOwner.lifecycle.addObserver(
                        observer
                    )


                    onDispose {

                        lifecycleOwner.lifecycle.removeObserver(
                            observer
                        )
                    }
                }


                // =====================================================
                // MANAGE APPS SCREEN
                // =====================================================

                if (showCategories) {

                    CategoryScreen(

                        onBack = {

                            usage =
                                getTodayUsage()

                            categoryUsage =
                                UsageRepository.todayCategoryMinutes(
                                    this@MainActivity
                                )

                            showCategories =
                                false
                        }
                    )


                    // =====================================================
                    // MANAGE CATEGORIES SCREEN
                    // =====================================================

                } else if (showManageCategories) {

                    ManageCategoriesScreen(

                        initialCategory =
                            dashboardCategory,

                        onBack = {

                            categoryUsage =
                                UsageRepository
                                    .todayCategoryMinutes(
                                        this@MainActivity
                                    )

                            dashboardCategory =
                                null

                            showManageCategories =
                                false
                        }
                    )


                    // =====================================================
                    // MAIN DASHBOARD
                    // =====================================================

                } else {

                    androidx.compose.foundation.layout.Column(

                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxSize()
                                .verticalScroll(
                                    androidx.compose.foundation.rememberScrollState()
                                )
                                .padding(20.dp)
                    ) {


                        // =================================================
                        // TITLE
                        // =================================================

                        androidx.compose.material3.Text(

                            text = "ScreenLess",

                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .headlineLarge
                            ,

                            color =
                                androidx.compose.material3.MaterialTheme
                                    .colorScheme
                                    .onBackground
                        )


                        androidx.compose.foundation.layout.Spacer(
                            modifier =
                                androidx.compose.ui.Modifier.height(24.dp)
                        )


                        // Setup is an exception state: only show actions for
                        // permissions that still need the user's attention.
                        if (!usageAccessEnabled || !accessibilityEnabled) {

                        androidx.compose.material3.Text(

                            text = "Setup",

                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .titleLarge
                            ,

                            color =
                                androidx.compose.material3.MaterialTheme
                                    .colorScheme
                                    .onBackground
                        )


                        androidx.compose.foundation.layout.Spacer(
                            modifier =
                                androidx.compose.ui.Modifier.height(8.dp)
                        )


                            if (!usageAccessEnabled) {
                                PermissionStatusRow(
                                    name = "Usage Access",
                                    enabled = false
                                )
                            }


                            if (!accessibilityEnabled) {
                                PermissionStatusRow(
                                    name = "Accessibility",
                                    enabled = false
                                )
                            }


                        // -------------------------------------------------
                        // ACCESSIBILITY SETTINGS BUTTON
                        // -------------------------------------------------

                        if (!accessibilityEnabled) {

                            androidx.compose.material3.TextButton(

                                onClick = {

                                    try {

                                        val componentName =
                                            android.content.ComponentName(
                                                this@MainActivity,
                                                ScreenLessAccessibilityService::class.java
                                            )


                                        val intent =
                                            android.content.Intent(
                                                "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
                                            ).apply {

                                                data =
                                                    android.net.Uri.parse(
                                                        "package:${componentName.packageName}"
                                                    )
                                            }


                                        startActivity(intent)

                                    } catch (e: Exception) {

                                        val fallbackIntent =
                                            android.content.Intent(
                                                android.provider.Settings
                                                    .ACTION_ACCESSIBILITY_SETTINGS
                                            )

                                        startActivity(
                                            fallbackIntent
                                        )
                                    }
                                }
                            ) {

                                androidx.compose.material3.Text(
                                    text = "Open Accessibility Settings"
                                )
                            }
                        }


                        // -------------------------------------------------
                        // USAGE ACCESS SETTINGS BUTTON
                        // -------------------------------------------------

                        if (!usageAccessEnabled) {

                            androidx.compose.material3.TextButton(

                                onClick = {

                                    val intent =
                                        android.content.Intent(
                                            android.provider.Settings
                                                .ACTION_USAGE_ACCESS_SETTINGS
                                        )

                                    startActivity(intent)
                                }
                            ) {

                                androidx.compose.material3.Text(
                                    text = "Enable Usage Access"
                                )
                            }
                        }


                            androidx.compose.foundation.layout.Spacer(
                                modifier =
                                    androidx.compose.ui.Modifier.height(28.dp)
                            )
                        }


                        // =================================================
                        // MOST USED CATEGORIES
                        // =================================================

                        /*
                         * This outer Card creates the border separating
                         * Categories from the rest of the dashboard.
                         */

                        androidx.compose.material3.Card(

                            modifier =
                                androidx.compose.ui.Modifier
                                    .fillMaxWidth(),

                            border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    androidx.compose.material3.MaterialTheme
                                        .colorScheme
                                        .outlineVariant
                                )
                        ) {


                            androidx.compose.foundation.layout.Column(

                                modifier =
                                    androidx.compose.ui.Modifier
                                        .padding(16.dp)
                            ) {


                                androidx.compose.material3.Text(

                                    text = "Most Used Categories",

                                    style =
                                        androidx.compose.material3.MaterialTheme
                                            .typography
                                            .titleLarge
                                )


                                androidx.compose.foundation.layout.Spacer(
                                    modifier =
                                        androidx.compose.ui.Modifier.height(14.dp)
                                )


                                // -----------------------------------------
                                // CALCULATE TOP CATEGORIES
                                // -----------------------------------------

                                val totalCategorizedUsage =
                                    categoryUsage
                                        .filterKeys { category ->

                                            category !=
                                                    AppCategory.UNCATEGORIZED
                                        }
                                        .values
                                        .sum()


                                val topCategories =
                                    AppCategory.assignable
                                        .map { category ->

                                            Pair(
                                                category,
                                                categoryUsage[category]
                                                    ?: 0L
                                            )
                                        }
                                        .filter { (_, minutes) ->

                                            minutes > 0
                                        }
                                        .sortedByDescending {
                                                (_, minutes) ->

                                            minutes
                                        }
                                        .take(4)


                                // -----------------------------------------
                                // TWO CATEGORIES PER ROW
                                // -----------------------------------------

                                topCategories
                                    .chunked(2)
                                    .forEach { rowCategories ->


                                        androidx.compose.foundation.layout.Row(

                                            modifier =
                                                androidx.compose.ui.Modifier
                                                    .fillMaxWidth(),

                                            horizontalArrangement =
                                                androidx.compose.foundation.layout
                                                    .Arrangement
                                                    .spacedBy(12.dp)
                                        ) {


                                            rowCategories.forEach {
                                                    (category, minutes) ->


                                                androidx.compose.foundation.layout.Box(

                                                    modifier =
                                                        androidx.compose.ui.Modifier
                                                            .weight(1f)
                                                ) {


                                                    DashboardCategoryCard(

                                                        category =
                                                            category,

                                                        usageMinutes =
                                                            minutes,

                                                        totalUsageMinutes =
                                                            totalCategorizedUsage,

                                                        onClick = {

                                                            dashboardCategory =
                                                                category

                                                            showManageCategories =
                                                                true
                                                        }
                                                    )
                                                }
                                            }


                                            // Keeps a single final card
                                            // half-width instead of stretching.

                                            if (
                                                rowCategories.size == 1
                                            ) {

                                                androidx.compose.foundation.layout.Spacer(

                                                    modifier =
                                                        androidx.compose.ui.Modifier
                                                            .weight(1f)
                                                )
                                            }
                                        }


                                        androidx.compose.foundation.layout.Spacer(

                                            modifier =
                                                androidx.compose.ui.Modifier
                                                    .height(12.dp)
                                        )
                                    }


                                // -----------------------------------------
                                // MANAGE CATEGORIES
                                // -----------------------------------------

                                androidx.compose.material3.TextButton(

                                    onClick = {

                                        dashboardCategory =
                                            null

                                        showManageCategories =
                                            true
                                    }
                                ) {


                                    androidx.compose.material3.Text(

                                        text =
                                            "Manage categories →",

                                        color =
                                            androidx.compose.material3.MaterialTheme
                                                .colorScheme
                                                .primary,

                                        style =
                                            androidx.compose.material3.MaterialTheme
                                                .typography
                                                .titleMedium
                                                .copy(
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                    )
                                }
                            }
                        }


                        androidx.compose.foundation.layout.Spacer(

                            modifier =
                                androidx.compose.ui.Modifier
                                    .height(24.dp)
                        )


                        // =================================================
                        // MOST USED APPS
                        // =================================================

                        /*
                         * Separate bordered card for apps.
                         */

                        androidx.compose.material3.Card(

                            modifier =
                                androidx.compose.ui.Modifier
                                    .fillMaxWidth(),

                            border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    androidx.compose.material3.MaterialTheme
                                        .colorScheme
                                        .outlineVariant
                                )
                        ) {


                            androidx.compose.foundation.layout.Column(

                                modifier =
                                    androidx.compose.ui.Modifier
                                        .padding(16.dp)
                            ) {


                                androidx.compose.material3.Text(

                                    text = "Most Used Apps",

                                    style =
                                        androidx.compose.material3.MaterialTheme
                                            .typography
                                            .titleLarge
                                )


                                androidx.compose.foundation.layout.Spacer(

                                    modifier =
                                        androidx.compose.ui.Modifier
                                            .height(14.dp)
                                )


                                // -----------------------------------------
                                // TOP FOUR APPS
                                // -----------------------------------------

                                val topApps =
                                    usage.take(4)


                                // -----------------------------------------
                                // TWO APPS PER ROW
                                // -----------------------------------------

                                topApps
                                    .chunked(2)
                                    .forEach { rowApps ->


                                        androidx.compose.foundation.layout.Row(

                                            modifier =
                                                androidx.compose.ui.Modifier
                                                    .fillMaxWidth(),

                                            horizontalArrangement =
                                                androidx.compose.foundation.layout
                                                    .Arrangement
                                                    .spacedBy(12.dp)
                                        ) {


                                            rowApps.forEach { app ->


                                                androidx.compose.foundation.layout.Box(

                                                    modifier =
                                                        androidx.compose.ui.Modifier
                                                            .weight(1f)
                                                ) {


                                                    DashboardAppCard(

                                                        app = app,

                                                        onClick = {

                                                            selectedApp =
                                                                app
                                                        }
                                                    )
                                                }
                                            }


                                            if (
                                                rowApps.size == 1
                                            ) {

                                                androidx.compose.foundation.layout.Spacer(

                                                    modifier =
                                                        androidx.compose.ui.Modifier
                                                            .weight(1f)
                                                )
                                            }
                                        }


                                        androidx.compose.foundation.layout.Spacer(

                                            modifier =
                                                androidx.compose.ui.Modifier
                                                    .height(12.dp)
                                        )
                                    }


                                // -----------------------------------------
                                // MANAGE APPS
                                // -----------------------------------------

                                androidx.compose.material3.TextButton(

                                    onClick = {

                                        showCategories =
                                            true
                                    }
                                ) {


                                    androidx.compose.material3.Text(

                                        text =
                                            "Manage apps →",

                                        color =
                                            androidx.compose.material3.MaterialTheme
                                                .colorScheme
                                                .primary,

                                        style =
                                            androidx.compose.material3.MaterialTheme
                                                .typography
                                                .titleMedium
                                                .copy(
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                    )
                                }
                            }
                        }


                        androidx.compose.foundation.layout.Spacer(

                            modifier =
                                androidx.compose.ui.Modifier
                                    .height(32.dp)
                        )
                    }


                    // =====================================================
                    // INDIVIDUAL APP LIMIT DIALOG
                    // =====================================================

                    selectedApp?.let { app ->


                        LimitDialog(

                            app = app,

                            onDismiss = {

                                selectedApp =
                                    null
                            },

                            onLimitSelected = {
                                    minutes ->


                                saveAppLimit(

                                    packageName =
                                        app.packageName,

                                    minutes =
                                        minutes
                                )


                                usage =
                                    getTodayUsage()


                                selectedApp =
                                    null
                            }
                        )
                    }
                }
            }
        }
    }
    private fun hasUsageAccess(): Boolean {

        val appOpsManager =
            getSystemService(
                android.content.Context.APP_OPS_SERVICE
            ) as android.app.AppOpsManager

        val mode =
            appOpsManager.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )

        return mode ==
                android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun hasAccessibilityAccess(): Boolean {

        val accessibilityManager =
            getSystemService(
                android.content.Context.ACCESSIBILITY_SERVICE
            ) as android.view.accessibility.AccessibilityManager

        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

        return enabledServices.any { serviceInfo ->

            val service =
                serviceInfo.resolveInfo.serviceInfo

            service.packageName == packageName &&
                    service.name ==
                    ScreenLessAccessibilityService::class.java.name
        }
    }
}





fun formatMinutes(totalMinutes: Long): String {

    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {

        hours > 0 && minutes > 0 ->
            "${hours}h ${minutes}m"

        hours > 0 ->
            "${hours}h"

        else ->
            "${minutes}m"
    }
}

fun android.graphics.drawable.Drawable.toBitmap(): android.graphics.Bitmap {
    if (this is BitmapDrawable) {
        return bitmap
    }

    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)

    val bitmap = android.graphics.Bitmap.createBitmap(
        width,
        height,
        android.graphics.Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)

    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)

    return bitmap
}
@Composable
fun AppUsageRow(
    app: AppUsage,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {

        // App icon
        app.icon?.let { drawable ->

            Image(
                bitmap = drawable.toBitmap().asImageBitmap(),
                contentDescription = "${app.appName} icon",
                modifier = Modifier.size(42.dp)
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )
        }

        // App name
        Text(
            text = app.appName,
            modifier = Modifier.weight(1f),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )

        // Usage
        Text(
            text = if (app.dailyLimitMinutes != null) {
                "${app.usageMinutes} / ${app.dailyLimitMinutes} min"
            } else {
                app.formattedUsage
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CategoryLimitDialog(
    category: AppCategory,
    currentUsage: Long,
    onDismiss: () -> Unit,
    onLimitSelected: (Int) -> Unit,
    onRemoveLimit: () -> Unit
) {

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            androidx.compose.material3.Text(
                text = "Limit ${category.label}"
            )
        },

        text = {

            androidx.compose.foundation.layout.Column {

                androidx.compose.material3.Text(
                    text = "Used today: ${formatMinutes(currentUsage)}"
                )

                androidx.compose.foundation.layout.Spacer(
                    modifier = androidx.compose.ui.Modifier.height(16.dp)
                )

                androidx.compose.material3.Text(
                    text = "Choose a combined daily limit"
                )

                androidx.compose.foundation.layout.Spacer(
                    modifier = androidx.compose.ui.Modifier.height(8.dp)
                )

                listOf(1, 15, 30, 45, 60, 90, 120).forEach { minutes ->

                    androidx.compose.material3.Text(
                        text = formatMinutes(minutes.toLong()),

                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLimitSelected(minutes)
                            }
                            .padding(12.dp)
                    )
                }
            }
        },

        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onRemoveLimit
            ) {
                androidx.compose.material3.Text("Remove time limit")
            }
        },

        dismissButton = {

            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                androidx.compose.material3.Text("Cancel")
            }
        }
    )
}
@Composable
fun LimitDialog(
    app: AppUsage,
    onDismiss: () -> Unit,
    onLimitSelected: (Int) -> Unit
) {

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Limit ${app.appName}")
        },

        text = {

            androidx.compose.foundation.layout.Column {

                Text("Choose a daily limit")

                Spacer(modifier = Modifier.height(16.dp))

                listOf(1, 10, 15, 30, 45, 60, 90).forEach { minutes ->

                    Text(
                        text = if (minutes >= 60) {
                            "${minutes / 60}h ${minutes % 60}m"
                        } else {
                            "$minutes minutes"
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLimitSelected(minutes)
                            }
                            .padding(12.dp)
                    )
                }
            }
        },

        confirmButton = {},

        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PermissionStatusRow(
    name: String,
    enabled: Boolean
) {

    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        androidx.compose.material3.Text(
            text = name,
            modifier =
                androidx.compose.ui.Modifier.weight(1f),
            color =
                androidx.compose.material3.MaterialTheme
                    .colorScheme
                    .onSurface
        )

        androidx.compose.material3.Text(
            text =
                if (enabled) {
                    "✓ Enabled"
                } else {
                    "✕ Required"
                },

            color =
                if (enabled) {
                    androidx.compose.material3.MaterialTheme
                        .colorScheme.primary
                } else {
                    androidx.compose.material3.MaterialTheme
                        .colorScheme.error
                }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}
