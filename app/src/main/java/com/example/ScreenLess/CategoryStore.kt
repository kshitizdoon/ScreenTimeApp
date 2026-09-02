package com.example.ScreenLess

import android.content.Context

class CategoryStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "screenless_preferences",
            Context.MODE_PRIVATE
        )

    fun getCategory(packageName: String): AppCategory {

        return getCategories(packageName)
            .firstOrNull()
            ?: AppCategory.UNCATEGORIZED
    }

    fun getCategories(packageName: String): Set<AppCategory> {

        val memberships =
            AppCategory.assignable
                .filter { category ->
                    prefs.getBoolean(
                        membershipKey(category, packageName),
                        false
                    )
                }
                .toSet()

        if (memberships.isNotEmpty() || isMembershipMigrated(packageName)) {
            return memberships
        }

        val stored =
            prefs.getString(
                "category_$packageName",
                null
            ) ?: return emptySet()

        return runCatching {
            AppCategory.valueOf(stored)
        }.getOrNull()
            ?.takeIf { it in AppCategory.assignable }
            ?.let { setOf(it) }
            ?: emptySet()
    }

    fun hasCategory(packageName: String): Boolean {

        return getCategories(packageName).isNotEmpty()
    }
    fun isOpeningIntentionEnabled(
        category: AppCategory
    ): Boolean {

        return prefs.getBoolean(
            "opening_intention_${category.name}",
            false
        )
    }


    fun setOpeningIntentionEnabled(
        category: AppCategory,
        enabled: Boolean
    ) {

        prefs.edit()
            .putBoolean(
                "opening_intention_${category.name}",
                enabled
            )
            .apply()
    }


    fun isLyingCheckEnabled(
        category: AppCategory
    ): Boolean {

        return prefs.getBoolean(
            "lying_check_${category.name}",
            false
        )
    }


    fun setLyingCheckEnabled(
        category: AppCategory,
        enabled: Boolean
    ) {

        prefs.edit()
            .putBoolean(
                "lying_check_${category.name}",
                enabled
            )
            .apply()
    }


    fun isGraduatedFrictionEnabled(
        category: AppCategory
    ): Boolean {

        return prefs.getBoolean(
            "graduated_friction_${category.name}",
            false
        )
    }


    fun setGraduatedFrictionEnabled(
        category: AppCategory,
        enabled: Boolean
    ) {

        prefs.edit()
            .putBoolean(
                "graduated_friction_${category.name}",
                enabled
            )
            .apply()
    }

    fun isPipWorkflowEnabled(category: AppCategory): Boolean =
        prefs.getBoolean("pip_workflow_${category.name}", false)

    fun setPipWorkflowEnabled(category: AppCategory, enabled: Boolean) {
        prefs.edit()
            .putBoolean("pip_workflow_${category.name}", enabled)
            .apply()
    }

    fun setCategory(
        packageName: String,
        category: AppCategory
    ) {

        val editor = prefs.edit()

        AppCategory.assignable.forEach { assignableCategory ->
            editor.putBoolean(
                membershipKey(assignableCategory, packageName),
                assignableCategory == category
            )
        }

        editor
            .putBoolean(migrationKey(packageName), true)
            .apply()
    }

    fun addToCategory(packageName: String, category: AppCategory) {
        migrateLegacyMembership(packageName)

        prefs.edit()
            .putBoolean(membershipKey(category, packageName), true)
            .apply()
    }

    fun removeFromCategory(packageName: String, category: AppCategory) {
        migrateLegacyMembership(packageName)

        prefs.edit()
            .remove(membershipKey(category, packageName))
            .apply()
    }

    fun websitesInCategory(category: AppCategory): Set<String> =
        prefs.getStringSet(websiteKey(category), emptySet())
            ?.toSet()
            ?: emptySet()

    fun addWebsiteToCategory(category: AppCategory, domain: String) {
        val normalizedDomain =
            domain.trim()
                .lowercase()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore('/')

        if (normalizedDomain.isBlank()) return

        prefs.edit()
            .putStringSet(
                websiteKey(category),
                websitesInCategory(category) + normalizedDomain
            )
            .apply()
    }

    fun removeWebsiteFromCategory(category: AppCategory, domain: String) {
        prefs.edit()
            .putStringSet(
                websiteKey(category),
                websitesInCategory(category) - domain
            )
            .apply()
    }

    private fun migrateLegacyMembership(packageName: String) {
        if (isMembershipMigrated(packageName)) return

        val legacyCategory =
            prefs.getString("category_$packageName", null)
                ?.let { stored ->
                    runCatching { AppCategory.valueOf(stored) }.getOrNull()
                }

        prefs.edit().apply {
            legacyCategory
                ?.takeIf { it in AppCategory.assignable }
                ?.let { category ->
                    putBoolean(
                        membershipKey(category, packageName),
                        true
                    )
                }
            putBoolean(migrationKey(packageName), true)
            apply()
        }
    }

    private fun isMembershipMigrated(packageName: String): Boolean =
        prefs.getBoolean(migrationKey(packageName), false)

    private fun membershipKey(category: AppCategory, packageName: String) =
        "category_membership_${category.name}_$packageName"

    private fun migrationKey(packageName: String) =
        "category_membership_migrated_$packageName"

    private fun websiteKey(category: AppCategory) =
        "category_websites_${category.name}"

    fun getCategoryLimit(
        category: AppCategory
    ): Int {

        return prefs.getInt(
            "limit_category_${category.name}",
            0
        )
    }

    fun setCategoryLimit(
        category: AppCategory,
        minutes: Int
    ) {

        prefs.edit()
            .putInt(
                "limit_category_${category.name}",
                minutes
            )
            .apply()
    }

    fun getAppLimit(
        packageName: String
    ): Int {

        return prefs.getInt(
            "limit_$packageName",
            0
        )
    }

    fun setAppLimit(
        packageName: String,
        minutes: Int
    ) {

        prefs.edit()
            .putInt(
                "limit_$packageName",
                minutes
            )
            .apply()
    }
}
