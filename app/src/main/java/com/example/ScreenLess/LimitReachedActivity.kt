package com.example.ScreenLess

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ScreenLess.ui.theme.MyApplicationTheme

class LimitReachedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName =
            intent.getStringExtra("appName") ?: "This app"

        val packageName =
            intent.getStringExtra("packageName") ?: ""

        val usageMinutes =
            intent.getLongExtra("usageMinutes", 0)

        val limitMinutes =
            intent.getIntExtra("limitMinutes", 0)

        val reason =
            intent.getStringExtra("reason") ?: "app"

        val limitName =
            intent.getStringExtra("limitName") ?: appName

        val isCategoryLimit =
            reason == "category"

        setContent {

            MyApplicationTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Push the main content toward the center
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )

                        // Small heading
                        Text(
                            text = if (isCategoryLimit) {
                                "CATEGORY LIMIT REACHED"
                            } else {
                                "APP LIMIT REACHED"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        // Main app/category name
                        Text(
                            text = limitName,
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(
                            modifier = Modifier.height(32.dp)
                        )

                        // Usage number
                        Text(
                            text = formatMinutes(usageMinutes),
                            style = MaterialTheme.typography.displayMedium
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "used today",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier = Modifier.height(24.dp)
                        )

                        HorizontalDivider()

                        Spacer(
                            modifier = Modifier.height(24.dp)
                        )

                        // Limit information
                        Text(
                            text = if (isCategoryLimit) {
                                "${limitName} limit"
                            } else {
                                "Daily limit"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = formatMinutes(limitMinutes.toLong()),
                            style = MaterialTheme.typography.headlineMedium
                        )

                        // Only necessary for category limits
                        if (isCategoryLimit) {

                            Spacer(
                                modifier = Modifier.height(24.dp)
                            )

                            Text(
                                text = "You opened $appName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Spacer(
                            modifier = Modifier.weight(1f)
                        )

                        // Primary action
                        Button(
                            onClick = {
                                goHome()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text("Go to Home")
                        }

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        // Deliberately less prominent than leaving
                        TextButton(
                            onClick = {
                                continueAnyway(packageName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue anyway")
                        }
                    }
                }
            }
        }
    }

    private fun goHome() {

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        startActivity(homeIntent)
        finish()
    }

    private fun continueAnyway(packageName: String) {

        if (packageName.isBlank()) {
            finish()
            return
        }

        // Allow this app temporarily so our accessibility
        // service doesn't immediately show this screen again.
        val preferences =
            getSharedPreferences(
                "screenless_preferences",
                MODE_PRIVATE
            )

        preferences.edit()
            .putLong(
                "bypass_$packageName",
                System.currentTimeMillis() + 5 * 60_000
            )
            .apply()

        val launchIntent =
            packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            startActivity(launchIntent)
        }

        finish()
    }
}