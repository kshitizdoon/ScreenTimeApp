package com.example.ScreenLess

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.content.Intent

class ScreenLessAccessibilityService : AccessibilityService() {
    private val sessionCheckIntervalMs = 10_000L

    private lateinit var reclinedDetector:
            ReclinedUseDetector

    private var activeSessionStartedAt: Long = 0L

    private var postureInterventionShown =
        false
    private var sessionMonitorRunnable: Runnable? = null

    private var lastInterventionPackage: String? = null
    private var lastInterventionTime: Long = 0L

    private val interventionCooldownMs = 2000L
    private var lastOpeningPackage: String? = null

    private var lastOpeningInterventionTime: Long = 0L

    private val openingDebounceMs = 2000L
    private var currentForegroundPackage: String? = null
    private var allowedSessionPackage: String? = null
    private var possibleExitPackage: String? = null
    private var possibleExitTime: Long = 0L

    private val realExitThresholdMs = 1500L
    private val handler =
        android.os.Handler(
            android.os.Looper.getMainLooper()
        )

    private var exitRunnable: Runnable? = null

    private fun scheduleSessionExit(
        candidatePackage: String
    ) {

        exitRunnable?.let {
            handler.removeCallbacks(it)
        }

        val sessionPackage =
            allowedSessionPackage ?: return

        val runnable = Runnable {

            if (
                allowedSessionPackage == sessionPackage &&
                possibleExitPackage == candidatePackage
            ) {

                android.util.Log.d(
                    "ScreenLessAccessibility",
                    "Confirmed exit: $sessionPackage -> $candidatePackage"
                )
                stopSessionMonitor()

                allowedSessionPackage = null
                possibleExitPackage = null
                possibleExitTime = 0L

                getSharedPreferences(
                    "screenless_preferences",
                    MODE_PRIVATE
                )
                    .edit()
                    .remove("active_session_package")
                    .apply()

                currentForegroundPackage =
                    candidatePackage
            }
        }

        exitRunnable = runnable

        handler.postDelayed(
            runnable,
            realExitThresholdMs
        )
    }

    private fun cancelPendingSessionExit() {

        exitRunnable?.let {
            handler.removeCallbacks(it)
        }

        exitRunnable = null
        possibleExitPackage = null
        possibleExitTime = 0L
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        if (
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val packageName =
            event.packageName?.toString() ?: return

        val now = System.currentTimeMillis()


        // --------------------------------------------------
        // Ignore transient overlays
        // --------------------------------------------------

        if (isTransientPackage(packageName)) {

            android.util.Log.d(
                "ScreenLessAccessibility",
                "Transient -> $packageName"
            )

            return
        }


        // --------------------------------------------------
        // Synchronise deliberate session from preferences
        // --------------------------------------------------

        val preferences =
            getSharedPreferences(
                "screenless_preferences",
                MODE_PRIVATE
            )

        val storedSession =
            preferences.getString(
                "active_session_package",
                null
            )

        if (storedSession != null) {
            allowedSessionPackage = storedSession
        }


        // --------------------------------------------------
        // Are we back inside the already-approved app?
        // --------------------------------------------------

        if (
            allowedSessionPackage != null &&
            packageName == allowedSessionPackage
        ) {

            cancelPendingSessionExit()

            currentForegroundPackage =
                packageName

            android.util.Log.d(
                "ScreenLessAccessibility",
                "Approved session continues -> $packageName"
            )

            if (sessionMonitorRunnable == null) {
                startSessionMonitor(packageName)
            }

            return
        }


        // --------------------------------------------------
        // We're seeing something other than approved app
        // --------------------------------------------------

        if (allowedSessionPackage != null) {

            /*
             * Don't immediately destroy the approved session.
             *
             * Android may briefly report launcher/system
             * windows during fullscreen/PiP transitions.
             */

            if (possibleExitPackage != packageName) {

                possibleExitPackage = packageName
                possibleExitTime = now
                scheduleSessionExit(packageName)

                android.util.Log.d(
                    "ScreenLessAccessibility",
                    "Possible exit from $allowedSessionPackage -> $packageName"
                )

                return
            }


            /*
             * The other package has remained/reappeared for
             * long enough. Treat this as a genuine exit.
             */

            if (
                now - possibleExitTime >=
                realExitThresholdMs
            ) {

                android.util.Log.d(
                    "ScreenLessAccessibility",
                    "Session ended: $allowedSessionPackage -> $packageName"
                )

                allowedSessionPackage = null
                possibleExitPackage = null
                possibleExitTime = 0L

                preferences.edit()
                    .remove("active_session_package")
                    .apply()

            } else {

                return
            }
        }


        // --------------------------------------------------
        // Normal foreground transition
        // --------------------------------------------------

        if (packageName == currentForegroundPackage) {
            return
        }

        val previousPackage =
            currentForegroundPackage

        currentForegroundPackage =
            packageName

        android.util.Log.d(
            "ScreenLessAccessibility",
            "REAL APP TRANSITION: $previousPackage -> $packageName"
        )


        // ScreenLess itself never gets checked.
        if (packageName == this.packageName) {
            return
        }


        checkLimit(packageName)
    }

    private fun isTransientPackage(
        packageName: String
    ): Boolean {

        return packageName in setOf(

            // Android / Samsung system UI
            "com.android.systemui",

            // Samsung keyboard
            "com.samsung.android.honeyboard",

            // Google's keyboard
            "com.google.android.inputmethod.latin",

            // Android permission dialogs
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )
    }

    override fun onInterrupt() {
        Log.d(
            "ScreenLessAccessibility",
            "Accessibility service interrupted"
        )
    }

    private fun calculateWaitSeconds(
        usageMinutes: Long,
        limitMinutes: Int,
        graduated: Boolean
    ): Int {

        if (!graduated) {
            return 1
        }

        // If no limit exists yet, still create
        // a tiny amount of friction.
        if (limitMinutes <= 0) {
            return 1
        }

        val percentage =
            usageMinutes.toDouble() /
                    limitMinutes.toDouble()

        return when {

            percentage < 0.50 -> 1
            percentage < 0.75 -> 3
            percentage < 0.90 -> 5
            else -> 10
        }
    }

    private fun showOpeningIntervention(
        packageName: String,
        usageMinutes: Long,
        limitMinutes: Int,
        graduated: Boolean
    ) {

        val now =
            System.currentTimeMillis()


        // =====================================================
        // PREVENT DUPLICATE INTERVENTION SCREENS
        // =====================================================

        if (
            packageName == lastOpeningPackage &&
            now - lastOpeningInterventionTime <
            openingDebounceMs
        ) {

            android.util.Log.d(
                "ScreenLessAccessibility",
                "Duplicate opening intervention ignored -> $packageName"
            )

            return
        }


        // =====================================================
        // CHECK WHETHER USER JUST PRESSED "CONTINUE"
        // =====================================================

        val preferences =
            getSharedPreferences(
                "screenless_preferences",
                MODE_PRIVATE
            )

        val bypassUntil =
            preferences.getLong(
                "opening_bypass_$packageName",
                0L
            )

        if (now < bypassUntil) {

            android.util.Log.d(
                "ScreenLessAccessibility",
                "Opening bypass active -> $packageName"
            )

            return
        }


        // Only mark it as intercepted AFTER checking bypass.
        lastOpeningPackage =
            packageName

        lastOpeningInterventionTime =
            now


        // =====================================================
        // GET HUMAN-READABLE APP NAME
        // =====================================================

        val appName =
            UsageRepository.appLabel(
                this,
                packageName
            )


        // =====================================================
        // CALCULATE GRADUATED FRICTION
        // =====================================================

        val waitSeconds =
            calculateWaitSeconds(
                usageMinutes = usageMinutes,
                limitMinutes = limitMinutes,
                graduated = graduated
            )


        android.util.Log.d(
            "ScreenLessAccessibility",
            "Showing opening intervention: " +
                    "$appName, wait=${waitSeconds}s"
        )


        // =====================================================
        // OPEN INTERVENTION SCREEN
        // =====================================================

        val intent =
            Intent(
                this,
                OpeningInterventionActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP


                putExtra(
                    "packageName",
                    packageName
                )

                putExtra(
                    "appName",
                    appName
                )

                putExtra(
                    "usageMinutes",
                    usageMinutes
                )

                putExtra(
                    "limitMinutes",
                    limitMinutes
                )

                putExtra(
                    "waitSeconds",
                    waitSeconds
                )
            }
        performGlobalAction(
            GLOBAL_ACTION_HOME
        )

        startActivity(intent)
    }



    private fun categoryUsageMinutes(
        usageByPackage: Map<String, Long>,
        store: CategoryStore,
        category: AppCategory
    ): Long =
        usageByPackage
            .filterKeys { otherPackage ->
                category in store.getCategories(otherPackage)
            }
            .values
            .sum()

    private fun checkLimit(packageName: String) {

        // Never intervene against ScreenLess itself
        if (packageName == this.packageName) {
            return
        }

        val store = CategoryStore(this)

        android.util.Log.d(
            "ScreenLessAccessibility",
            "CONFIG CHECK -> " +
                    "package=$packageName, " +
                    "category=${store.getCategory(packageName)}, " +
                    "appLimit=${store.getAppLimit(packageName)}, " +
                    "hasCategory=${store.hasCategory(packageName)}"
        )

        val packageEnabled =
            try {

                val applicationInfo =
                    packageManager.getApplicationInfo(
                        packageName,
                        0
                    )

                applicationInfo.enabled

            } catch (e: Exception) {

                false
            }


        android.util.Log.d(
            "ScreenLessAccessibility",
            "PACKAGE ENABLED -> $packageName = $packageEnabled"
        )

        // Get today's usage for every package
        val usageByPackage =
            UsageRepository.todayUsageMinutes(this)

        // Usage of the app that was just opened
        val appUsage =
            usageByPackage[packageName] ?: 0L

        // Individual limit for this app
        val appLimit =
            store.getAppLimit(packageName)


        // =====================================================
        // 1. CHECK INDIVIDUAL APP LIMIT
        // =====================================================

        if (
            appLimit > 0 &&
            appUsage >= appLimit
        ) {

            android.util.Log.e(
                "ScreenLessAccessibility",
                "APP LIMIT REACHED -> $packageName"
            )

            showLimitScreen(
                packageName = packageName,
                usageMinutes = appUsage,
                limitMinutes = appLimit,
                reason = "app",
                limitName = UsageRepository.appLabel(
                    this,
                    packageName
                )
            )

            return
        }


        // =====================================================
        // 2. FIND THE APP'S CATEGORY
        // =====================================================

        val categories = store.getCategories(packageName)

        // Apps that aren't categorised don't get category rules.
        if (categories.isEmpty()) {
            return
        }

        // =====================================================
        // 3. CHECK EVERY ASSIGNED CATEGORY LIMIT
        // =====================================================

        val reachedCategory = categories.firstOrNull { category ->
            val limit = store.getCategoryLimit(category)
            limit > 0 &&
                    categoryUsageMinutes(usageByPackage, store, category) >= limit
        }

        if (reachedCategory != null) {

            val categoryUsage =
                categoryUsageMinutes(usageByPackage, store, reachedCategory)
            val categoryLimit = store.getCategoryLimit(reachedCategory)

            android.util.Log.e(
                "ScreenLessAccessibility",
                "CATEGORY LIMIT REACHED -> ${reachedCategory.label}"
            )

            showLimitScreen(
                packageName = packageName,
                usageMinutes = categoryUsage,
                limitMinutes = categoryLimit,
                reason = "category",
                limitName = reachedCategory.label
            )

            return
        }


        // =====================================================
        // 4. EVERY-OPEN INTERVENTION
        // =====================================================
        //
        // For now we're targeting every app you've placed
        // inside Video & Reels.
        //
        // This means YouTube / Instagram / Reddit etc.
        // can receive an intervention BEFORE their limit
        // has been reached.
        //

        val interventionCategory = categories.firstOrNull { category ->
            store.isOpeningIntentionEnabled(category) ||
                    store.isPipWorkflowEnabled(category)
        }

        if (interventionCategory != null) {

            /*
             * If Video & Reels has a category limit,
             * use the category's combined usage.
             *
             * Example:
             *
             * YouTube       20m
             * Instagram     10m
             *
             * Category      30 / 60m
             *
             * Therefore the intervention uses:
             *
             * usage = 30
             * limit = 60
             *
             * rather than YouTube's individual usage.
             */
            val relevantUsage: Long
            val relevantLimit: Int

            val categoryLimit = store.getCategoryLimit(interventionCategory)
            val categoryUsage = categoryUsageMinutes(
                usageByPackage,
                store,
                interventionCategory
            )

            if (categoryLimit > 0) {

                relevantUsage =
                    categoryUsage

                relevantLimit =
                    categoryLimit

            } else {

                // No category limit exists.
                // Fall back to this app's individual limit.

                relevantUsage =
                    appUsage

                relevantLimit =
                    appLimit
            }


            android.util.Log.d(
                "ScreenLessAccessibility",
                "OPENING INTERVENTION -> " +
                        "$packageName, " +
                        "$relevantUsage / $relevantLimit min"
            )


            showOpeningIntervention(
                packageName = packageName,
                usageMinutes = relevantUsage,
                limitMinutes = relevantLimit,
                graduated = store.isGraduatedFrictionEnabled(interventionCategory)
            )
        }
    }
    private fun showLimitScreen(
        packageName: String,
        usageMinutes: Long,
        limitMinutes: Int,
        reason: String,
        limitName: String
    ) {
        val now = System.currentTimeMillis()

        if (
            packageName == lastInterventionPackage &&
            now - lastInterventionTime < interventionCooldownMs
        ) {
            Log.d(
                "ScreenLessAccessibility",
                "Duplicate intervention ignored -> $packageName"
            )
            return
        }

        lastInterventionPackage = packageName
        lastInterventionTime = now

        // Don't intervene if the user recently chose
        // "Continue anyway".
        val preferences =
            getSharedPreferences(
                "screenless_preferences",
                MODE_PRIVATE
            )

        val bypassUntil =
            preferences.getLong(
                "bypass_$packageName",
                0L
            )

        if (System.currentTimeMillis() < bypassUntil) {

            Log.d(
                "ScreenLessAccessibility",
                "Temporary bypass active -> $packageName"
            )

            return
        }

        val appName = try {

            val applicationInfo =
                packageManager.getApplicationInfo(
                    packageName,
                    0
                )

            packageManager
                .getApplicationLabel(applicationInfo)
                .toString()

        } catch (e: Exception) {

            packageName
        }

        val intent =
            Intent(
                this,
                LimitReachedActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                putExtra("appName", appName)
                putExtra("packageName", packageName)
                putExtra("usageMinutes", usageMinutes)
                putExtra("limitMinutes", limitMinutes)
                putExtra("reason", reason)
                putExtra("limitName", limitName)
            }
        performGlobalAction(
            GLOBAL_ACTION_HOME
        )
        startActivity(intent)
    }

    private fun startSessionMonitor(
        packageName: String
    ) {

        stopSessionMonitor()

        activeSessionStartedAt =
            System.currentTimeMillis()

        postureInterventionShown =
            false

        reclinedDetector.start()

        val runnable = object : Runnable {

            override fun run() {

                // Has this approved session ended?
                if (
                    allowedSessionPackage != packageName
                ) {
                    stopSessionMonitor()
                    return
                }

                android.util.Log.d(
                    "ScreenLessAccessibility",
                    "SESSION CHECK -> $packageName"
                )

                checkActiveSessionLimit(
                    packageName
                )

                checkReclinedUse(
                    packageName
                )

                handler.postDelayed(
                    this,
                    sessionCheckIntervalMs
                )
            }
        }

        sessionMonitorRunnable = runnable

        handler.postDelayed(
            runnable,
            sessionCheckIntervalMs
        )
    }

    override fun onServiceConnected() {

        super.onServiceConnected()

        reclinedDetector =
            ReclinedUseDetector(this)
    }

    private fun stopSessionMonitor() {

        sessionMonitorRunnable?.let {
            handler.removeCallbacks(it)
        }

        sessionMonitorRunnable = null

        if (
            ::reclinedDetector.isInitialized
        ) {
            reclinedDetector.stop()
        }
    }


    private fun checkActiveSessionLimit(
        packageName: String
    ) {

        val store =
            CategoryStore(this)

        val usageByPackage =
            UsageRepository.todayUsageMinutes(this)

        val appUsage =
            usageByPackage[packageName] ?: 0L

        val appLimit =
            store.getAppLimit(packageName)


        // -----------------------------------
        // INDIVIDUAL APP LIMIT
        // -----------------------------------

        if (
            appLimit > 0 &&
            appUsage >= appLimit
        ) {

            android.util.Log.e(
                "ScreenLessAccessibility",
                "ACTIVE APP LIMIT REACHED -> $packageName"
            )

            stopSessionMonitor()

            clearActiveSession()

            showLimitScreen(
                packageName = packageName,
                usageMinutes = appUsage,
                limitMinutes = appLimit,
                reason = "app",
                limitName =
                    UsageRepository.appLabel(
                        this,
                        packageName
                    )
            )

            return
        }


        // -----------------------------------
        // CATEGORY LIMIT
        // -----------------------------------

        val category =
            store.getCategory(packageName)

        if (
            category ==
            AppCategory.UNCATEGORIZED
        ) {
            return
        }


        val categoryLimit =
            store.getCategoryLimit(category)

        if (categoryLimit <= 0) {
            return
        }


        val categoryUsage =
            usageByPackage
                .filterKeys { otherPackage ->

                    store.getCategory(
                        otherPackage
                    ) == category
                }
                .values
                .sum()


        if (
            categoryUsage >= categoryLimit
        ) {

            android.util.Log.e(
                "ScreenLessAccessibility",
                "ACTIVE CATEGORY LIMIT REACHED -> ${category.label}"
            )

            stopSessionMonitor()

            clearActiveSession()

            showLimitScreen(
                packageName = packageName,
                usageMinutes = categoryUsage,
                limitMinutes = categoryLimit,
                reason = "category",
                limitName = category.label
            )
        }
    }

    private fun checkReclinedUse(
        packageName: String
    ) {

        // Only warn once during this app session.
        if (postureInterventionShown) {
            return
        }

        val store =
            CategoryStore(this)

        val category =
            store.getCategory(packageName)

        if (
            !store.isLyingCheckEnabled(
                category
            )
        ) {
            return
        }


        val sessionDuration =
            System.currentTimeMillis() -
                    activeSessionStartedAt


        // Five minutes
        if (
            sessionDuration <
            20_000L
        ) {
            return
        }


        if (
            !reclinedDetector
                .isLikelyReclined()
        ) {
            return
        }


        postureInterventionShown =
            true


        android.util.Log.d(
            "ScreenLessAccessibility",
            "LIKELY RECLINED USE -> $packageName " +
                    "score=${reclinedDetector.reclinedScore()}"
        )


        showPostureIntervention(
            packageName
        )
    }

    private fun showPostureIntervention(
        packageName: String
    ) {

        val appName =
            UsageRepository.appLabel(
                this,
                packageName
            )


        performGlobalAction(
            GLOBAL_ACTION_HOME
        )


        val intent =
            android.content.Intent(
                this,
                PostureInterventionActivity::class.java
            ).apply {

                flags =
                    android.content.Intent
                        .FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent
                                .FLAG_ACTIVITY_SINGLE_TOP

                putExtra(
                    "appName",
                    appName
                )
            }


        startActivity(intent)
    }

    private fun clearActiveSession() {

        allowedSessionPackage = null

        getSharedPreferences(
            "screenless_preferences",
            MODE_PRIVATE
        )
            .edit()
            .remove(
                "active_session_package"
            )
            .apply()
    }

}
