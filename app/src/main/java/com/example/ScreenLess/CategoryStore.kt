package com.example.ScreenLess

import android.content.Context

class CategoryStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "screenless_preferences",
            Context.MODE_PRIVATE
        )

    fun getCategory(packageName: String): AppCategory {

        val stored =
            prefs.getString(
                "category_$packageName",
                null
            ) ?: return AppCategory.UNCATEGORIZED

        return runCatching {
            AppCategory.valueOf(stored)
        }.getOrDefault(
            AppCategory.UNCATEGORIZED
        )
    }

    fun hasCategory(packageName: String): Boolean {

        return prefs.contains(
            "category_$packageName"
        )
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

    fun setCategory(
        packageName: String,
        category: AppCategory
    ) {

        prefs.edit()
            .putString(
                "category_$packageName",
                category.name
            )
            .apply()
    }

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