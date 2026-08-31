package com.example.ScreenLess

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler

@Composable
fun ManageCategoriesScreen(
    onBack: () -> Unit,
    initialCategory: AppCategory? = null
) {

    val context =
        LocalContext.current

    val store =
        remember {
            CategoryStore(context)
        }


    // Null means we're looking at the list of categories.
    // Non-null means we're looking at one category.
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory)
    }

    val openedDirectly =
        initialCategory != null

    BackHandler {

        if (
            selectedCategory != null &&
            !openedDirectly
        ) {

            // We entered through the category list,
            // so return to the category list.
            selectedCategory = null

        } else {

            // We either:
            //
            // 1. entered directly from a dashboard card, or
            // 2. are already at the category list.
            //
            // Return to dashboard.
            onBack()
        }
    }


    if (selectedCategory == null) {

        CategoryListScreen(
            onBack = onBack,

            onCategorySelected = {
                selectedCategory = it
            }
        )

    } else {

        CategoryDetailScreen(

            category =
                selectedCategory!!,

            onBack = {

                if (openedDirectly) {

                    onBack()

                } else {

                    selectedCategory =
                        null
                }
            },

            store =
                store,

            openedDirectly =
                openedDirectly
        )
    }
}

@Composable
private fun CategoryListScreen(
    onBack: () -> Unit,
    onCategorySelected: (AppCategory) -> Unit
) {

    val context =
        LocalContext.current

    val store =
        remember {
            CategoryStore(context)
        }

    val categoryUsage =
        remember {
            UsageRepository.todayCategoryMinutes(
                context
            )
        }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp)
    ) {

        TextButton(
            onClick = onBack
        ) {
            Text("← Back")
        }


        Text(
            text = "Manage Categories",
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        Text(
            text =
                "Set rules for groups of apps.",
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        AppCategory.assignable
            .forEach { category ->

                val usage =
                    categoryUsage[category]
                        ?: 0L

                val limit =
                    store.getCategoryLimit(
                        category
                    )


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCategorySelected(
                                category
                            )
                        }
                        .padding(
                            vertical = 14.dp
                        )
                ) {

                    Text(
                        text = category.label,
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )


                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )


                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                formatMinutes(
                                    usage
                                ),

                            modifier =
                                Modifier.weight(1f),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )


                        Text(
                            text =
                                if (limit > 0) {

                                    "Limit: ${
                                        formatMinutes(
                                            limit.toLong()
                                        )
                                    }"

                                } else {

                                    "No limit"
                                },

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }


                HorizontalDivider()
            }
    }
}

@Composable
private fun CategoryDetailScreen(
    category: AppCategory,
    onBack: () -> Unit,
    store: CategoryStore,
    openedDirectly: Boolean
) {

    val context =
        LocalContext.current


    // =====================================================
    // REFRESH STATE
    // =====================================================

    var refreshKey by remember {
        mutableIntStateOf(0)
    }


    // =====================================================
    // CATEGORY DATA
    // =====================================================

    val categoryUsage =
        remember(
            category,
            refreshKey
        ) {

            UsageRepository
                .todayCategoryMinutes(
                    context
                )[category]
                ?: 0L
        }


    val categoryLimit =
        remember(
            category,
            refreshKey
        ) {

            store.getCategoryLimit(
                category
            )
        }


    val apps =
        remember(
            category,
            refreshKey
        ) {

            UsageRepository.appsInCategory(
                context,
                category
            )
        }


    // =====================================================
    // DIALOG STATE
    // =====================================================

    var showLimitDialog by remember {
        mutableStateOf(false)
    }


    // =====================================================
    // INTERVENTION SETTINGS
    // =====================================================

    var openingIntentionEnabled by remember(
        category
    ) {

        mutableStateOf(
            store.isOpeningIntentionEnabled(
                category
            )
        )
    }


    var lyingCheckEnabled by remember(
        category
    ) {

        mutableStateOf(
            store.isLyingCheckEnabled(
                category
            )
        )
    }


    var graduatedFrictionEnabled by remember(
        category
    ) {

        mutableStateOf(
            store.isGraduatedFrictionEnabled(
                category
            )
        )
    }


    // =====================================================
    // MAIN SCREEN
    // =====================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp)
    ) {


        // =================================================
        // BACK
        // =================================================

        TextButton(
            onClick = onBack
        ) {

            Text(
                if (openedDirectly) {
                    "← Dashboard"
                } else {
                    "← Categories"
                }
            )
        }


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        // =================================================
        // CATEGORY TITLE
        // =================================================

        Text(
            text = category.label,

            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        // =================================================
        // TODAY
        // =================================================

        Text(
            text = "TODAY",

            style =
                MaterialTheme
                    .typography
                    .labelLarge,

            color =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Usage",

                modifier =
                    Modifier.weight(1f)
            )


            Text(
                text =
                    formatMinutes(
                        categoryUsage
                    ),

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // =================================================
        // DAILY LIMIT
        // =================================================

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {

                        showLimitDialog =
                            true
                    }
                    .padding(
                        vertical = 8.dp
                    )
        ) {

            Text(
                text = "Daily limit",

                modifier =
                    Modifier.weight(1f)
            )


            Text(
                text =
                    if (
                        categoryLimit > 0
                    ) {

                        formatMinutes(
                            categoryLimit.toLong()
                        )

                    } else {

                        "Set limit"
                    },

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        HorizontalDivider()


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        // =================================================
        // INTERVENTIONS
        // =================================================

        Text(
            text = "INTERVENTIONS",

            style =
                MaterialTheme
                    .typography
                    .labelLarge,

            color =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        // -------------------------------------------------
        // OPENING INTENTION
        // -------------------------------------------------

        CategorySettingSwitch(

            title =
                "Opening intention",

            description =
                "Pause before opening apps in this category.",

            checked =
                openingIntentionEnabled,

            onCheckedChange = {
                    enabled ->


                openingIntentionEnabled =
                    enabled


                store.setOpeningIntentionEnabled(
                    category,
                    enabled
                )
            }
        )


        // -------------------------------------------------
        // LYING USE CHECK
        // -------------------------------------------------

        CategorySettingSwitch(

            title =
                "Lying-use check",

            description =
                "Intervene during sustained reclined use.",

            checked =
                lyingCheckEnabled,

            onCheckedChange = {
                    enabled ->


                lyingCheckEnabled =
                    enabled


                store.setLyingCheckEnabled(
                    category,
                    enabled
                )
            }
        )


        // -------------------------------------------------
        // GRADUATED FRICTION
        // -------------------------------------------------

        CategorySettingSwitch(

            title =
                "Graduated friction",

            description =
                "Increase waiting time as usage approaches the limit.",

            checked =
                graduatedFrictionEnabled,

            onCheckedChange = {
                    enabled ->


                graduatedFrictionEnabled =
                    enabled


                store.setGraduatedFrictionEnabled(
                    category,
                    enabled
                )
            }
        )


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        HorizontalDivider()


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        // =================================================
        // APPS
        // =================================================

        Text(
            text = "APPS",

            style =
                MaterialTheme
                    .typography
                    .labelLarge,

            color =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        if (apps.isEmpty()) {


            Text(
                text =
                    "No apps have been assigned to this category.",

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


        } else {


            apps.forEach {
                    (packageName, minutes) ->


                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 12.dp
                            )
                ) {


                    Text(
                        text =
                            UsageRepository
                                .appLabel(
                                    context,
                                    packageName
                                ),

                        modifier =
                            Modifier.weight(1f),

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )


                    Text(
                        text =
                            formatMinutes(
                                minutes
                            ),

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }


                HorizontalDivider()
            }
        }


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )
    }


    // =====================================================
    // CATEGORY LIMIT DIALOG
    // =====================================================

    if (showLimitDialog) {


        CategoryLimitDialog(

            category =
                category,

            currentUsage =
                categoryUsage,

            onDismiss = {

                showLimitDialog =
                    false
            },

            onLimitSelected = {
                    minutes ->


                store.setCategoryLimit(

                    category =
                        category,

                    minutes =
                        minutes
                )


                // Forces this screen to reread
                // category usage, limit and apps.
                refreshKey++


                showLimitDialog =
                    false
            }
        )
    }
}

@Composable
private fun CategorySettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment =
            androidx.compose.ui.Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleSmall
            )

            Spacer(
                Modifier.height(2.dp)
            )

            Text(
                text = description,
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }


        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}