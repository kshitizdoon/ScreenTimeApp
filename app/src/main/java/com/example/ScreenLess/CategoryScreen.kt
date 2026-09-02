package com.example.ScreenLess

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.core.graphics.drawable.toBitmap
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)


fun getLaunchableApps(
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
                        .toString(),

                icon =
                    runCatching {
                        packageManager.getApplicationIcon(app)
                    }.getOrNull()
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

    var refreshKey by remember { mutableIntStateOf(0) }

    val usageByPackage =
        remember(refreshKey) {
            UsageRepository.todayUsageMinutes(context)
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
            ,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            Text(
                text =
                    "Review daily app usage and limits.",
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
            ,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
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

            val appLimit =
                store.getAppLimit(
                    app.packageName
                )

            val usageMinutes =
                usageByPackage[app.packageName]
                    ?: 0L

            val progress =
                if (appLimit > 0) {
                    (usageMinutes.toFloat() / appLimit)
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }

            val iconPainter =
                remember(app.icon) {
                    app.icon?.let { icon ->
                        BitmapPainter(
                            icon.toBitmap().asImageBitmap()
                        )
                    }
                }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    if (iconPainter != null) {
                        Image(
                            painter = iconPainter,
                            contentDescription = "${app.label} icon",
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = app.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = {
                            limitApp = app
                        }
                    ) {

                        Text(
                            text =
                                if (appLimit > 0) {
                                    formatMinutes(appLimit.toLong())
                                } else {
                                    "Unlimited"
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color =
                        if (appLimit > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text =
                        if (appLimit > 0) {
                            "${formatMinutes(usageMinutes)} / ${formatMinutes(appLimit.toLong())}"
                        } else {
                            "${formatMinutes(usageMinutes)} / Unlimited"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )


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

                refreshKey++

                limitApp = null
            },

            onRemoveLimit = {
                store.setAppLimit(
                    packageName = app.packageName,
                    minutes = 0
                )

                refreshKey++

                limitApp = null
            }
        )
    }
}
@Composable
fun SimpleAppLimitDialog(
    appName: String,
    onDismiss: () -> Unit,
    onLimitSelected: (Int) -> Unit,
    onRemoveLimit: () -> Unit
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


        confirmButton = {
            TextButton(
                onClick = onRemoveLimit
            ) {
                Text("Remove time limit")
            }
        },


        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}
