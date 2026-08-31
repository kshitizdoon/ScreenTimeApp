package com.example.ScreenLess

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler

private data class InstalledApp(
    val packageName: String,
    val label: String
)


private fun getLaunchableApps(
    context: Context
): List<InstalledApp> {

    val packageManager =
        context.packageManager

    return packageManager
        .getInstalledApplications(0)
        .filter { app ->

            packageManager
                .getLaunchIntentForPackage(
                    app.packageName
                ) != null
        }
        .map { app ->

            InstalledApp(
                packageName = app.packageName,

                label =
                    packageManager
                        .getApplicationLabel(app)
                        .toString()
            )
        }
        .sortedBy {
            it.label.lowercase()
        }
}


@Composable
fun CategoryScreen(
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    val context =
        LocalContext.current

    val store =
        remember {
            CategoryStore(context)
        }

    val apps =
        remember {
            getLaunchableApps(context)
        }

    val assignments =
        remember {

            mutableStateMapOf<String, AppCategory>()
                .apply {

                    apps.forEach { app ->

                        put(
                            app.packageName,
                            store.getCategory(
                                app.packageName
                            )
                        )
                    }
                }
        }


    // Which app's limit dialog is open?
    var limitApp by remember {
        mutableStateOf<InstalledApp?>(null)
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {

            TextButton(
                onClick = onBack
            ) {
                Text("← Back")
            }


            Text(
                text = "Manage apps",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            Text(
                text =
                    "Set categories and individual limits.",
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )
        }


        items(
            items = apps,
            key = { it.packageName }
        ) { app ->

            var expanded by remember {
                mutableStateOf(false)
            }


            val category =
                assignments[app.packageName]
                    ?: AppCategory.UNCATEGORIZED


            val appLimit =
                store.getAppLimit(
                    app.packageName
                )


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {


                // APP NAME
                Text(
                    text = app.label,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    // -------------------------
                    // CATEGORY
                    // -------------------------

                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        TextButton(
                            onClick = {
                                expanded = true
                            }
                        ) {

                            Text(
                                text = category.label
                            )
                        }


                        DropdownMenu(
                            expanded = expanded,

                            onDismissRequest = {
                                expanded = false
                            }
                        ) {


                            AppCategory.assignable
                                .forEach { newCategory ->


                                    DropdownMenuItem(

                                        text = {
                                            Text(
                                                newCategory.label
                                            )
                                        },

                                        onClick = {

                                            store.setCategory(
                                                app.packageName,
                                                newCategory
                                            )

                                            assignments[
                                                app.packageName
                                            ] = newCategory

                                            expanded = false
                                        }
                                    )
                                }
                        }
                    }


                    // -------------------------
                    // APP LIMIT
                    // -------------------------

                    TextButton(
                        onClick = {
                            limitApp = app
                        }
                    ) {

                        Text(
                            text =
                                if (appLimit > 0) {
                                    "Limit: ${formatMinutes(appLimit.toLong())}"
                                } else {
                                    "Set limit"
                                }
                        )
                    }
                }


                HorizontalDivider()
            }
        }
    }


    // --------------------------------------------------
    // LIMIT PICKER
    // --------------------------------------------------

    limitApp?.let { app ->

        SimpleAppLimitDialog(

            appName = app.label,

            onDismiss = {
                limitApp = null
            },

            onLimitSelected = { minutes ->

                store.setAppLimit(
                    packageName =
                        app.packageName,

                    minutes =
                        minutes
                )

                limitApp = null
            }
        )
    }
}
@Composable
fun SimpleAppLimitDialog(
    appName: String,
    onDismiss: () -> Unit,
    onLimitSelected: (Int) -> Unit
) {

    AlertDialog(

        onDismissRequest =
            onDismiss,


        title = {

            Text(
                text = "Limit $appName"
            )
        },


        text = {

            Column {

                Text(
                    text =
                        "Choose a daily limit"
                )


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                listOf(
                    5,
                    15,
                    30,
                    45,
                    60,
                    90,
                    120
                ).forEach { minutes ->


                    Text(
                        text =
                            formatMinutes(
                                minutes.toLong()
                            ),

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    onLimitSelected(
                                        minutes
                                    )
                                }
                                .padding(12.dp)
                    )
                }
            }
        },


        confirmButton = {},


        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}