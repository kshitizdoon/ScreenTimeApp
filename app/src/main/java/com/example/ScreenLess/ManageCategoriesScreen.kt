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
            ,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
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
                        ,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
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

    val websites =
        remember(category, refreshKey) {
            store.websitesInCategory(category)
                .sorted()
        }


    // =====================================================
    // DIALOG STATE
    // =====================================================

    var showLimitDialog by remember {
        mutableStateOf(false)
    }

    var showAddAppsDialog by remember {
        mutableStateOf(false)
    }

    var showAddWebsiteDialog by remember {
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

    var pipWorkflowEnabled by remember(category) {
        mutableStateOf(store.isPipWorkflowEnabled(category))
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
            ,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
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

        CategorySettingSwitch(
            title = "PiP workflow",
            description = "Use App → Home → ScreenLess → App before opening.",
            checked = pipWorkflowEnabled,
            onCheckedChange = { enabled ->
                pipWorkflowEnabled = enabled
                store.setPipWorkflowEnabled(category, enabled)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { showAddAppsDialog = true }) {
                Text("Add apps")
            }

            OutlinedButton(onClick = { showAddWebsiteDialog = true }) {
                Text("Add websites")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))


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
                        ,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
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

                    TextButton(
                        onClick = {
                            store.removeFromCategory(packageName, category)
                            refreshKey++
                        }
                    ) {
                        Text("Remove")
                    }
                }


                HorizontalDivider()
            }
        }

        if (websites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "WEBSITES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            websites.forEach { domain ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = domain,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            store.removeWebsiteFromCategory(category, domain)
                            refreshKey++
                        }
                    ) {
                        Text("Remove")
                    }
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
            },

            onRemoveLimit = {
                store.setCategoryLimit(category, 0)
                refreshKey++
                showLimitDialog = false
            }
        )
    }

    if (showAddAppsDialog) {
        AddAppsToCategoryDialog(
            category = category,
            store = store,
            onDismiss = { showAddAppsDialog = false },
            onSaved = {
                refreshKey++
                showAddAppsDialog = false
            }
        )
    }

    if (showAddWebsiteDialog) {
        AddWebsiteDialog(
            onDismiss = { showAddWebsiteDialog = false },
            onAdd = { domain ->
                store.addWebsiteToCategory(category, domain)
                refreshKey++
                showAddWebsiteDialog = false
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

@Composable
private fun AddAppsToCategoryDialog(
    category: AppCategory,
    store: CategoryStore,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val apps = remember { getLaunchableApps(context) }
    val selectedPackages = remember {
        mutableStateMapOf<String, Boolean>().apply {
            apps.forEach { app ->
                put(
                    app.packageName,
                    category in store.getCategories(app.packageName)
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add apps to ${category.label}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                apps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPackages[app.packageName] =
                                    !(selectedPackages[app.packageName] ?: false)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedPackages[app.packageName] ?: false,
                            onCheckedChange = { checked ->
                                selectedPackages[app.packageName] = checked
                            }
                        )
                        Text(
                            text = app.label,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    apps.forEach { app ->
                        if (selectedPackages[app.packageName] == true) {
                            store.addToCategory(app.packageName, category)
                        } else {
                            store.removeFromCategory(app.packageName, category)
                        }
                    }
                    onSaved()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddWebsiteDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var domain by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add website") },
        text = {
            TextField(
                value = domain,
                onValueChange = { domain = it },
                singleLine = true,
                label = { Text("Domain, e.g. youtube.com") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(domain) },
                enabled = domain.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
