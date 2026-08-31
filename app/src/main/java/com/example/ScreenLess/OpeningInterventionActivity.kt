package com.example.ScreenLess

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ScreenLess.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay


class OpeningInterventionActivity :
    ComponentActivity() {


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        val appName =
            intent.getStringExtra("appName")
                ?: "This app"


        val packageName =
            intent.getStringExtra("packageName")
                ?: ""


        val usageMinutes =
            intent.getLongExtra(
                "usageMinutes",
                0L
            )


        val limitMinutes =
            intent.getIntExtra(
                "limitMinutes",
                0
            )


        val waitSeconds =
            intent.getIntExtra(
                "waitSeconds",
                1
            )


        setContent {

            MyApplicationTheme {

                var secondsRemaining by remember {
                    mutableIntStateOf(waitSeconds)
                }


                // Countdown
                LaunchedEffect(Unit) {

                    while (
                        secondsRemaining > 0
                    ) {

                        delay(1000)

                        secondsRemaining--
                    }
                }


                Surface(
                    modifier =
                        Modifier.fillMaxSize(),
                    color =
                        MaterialTheme
                            .colorScheme
                            .background
                ) {


                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = 28.dp,
                                    vertical = 40.dp
                                ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {


                        Spacer(
                            modifier =
                                Modifier.weight(1f)
                        )


                        Text(
                            text =
                                "OPEN WITH INTENTION",

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
                                Modifier.height(16.dp)
                        )


                        Text(
                            text = appName,

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineLarge,

                            textAlign =
                                TextAlign.Center
                        )


                        Spacer(
                            modifier =
                                Modifier.height(28.dp)
                        )


                        Text(
                            text =
                                "Do you really want to open $appName?",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            textAlign =
                                TextAlign.Center
                        )


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        if (limitMinutes > 0) {

                            Text(
                                text =
                                    "${formatMinutes(usageMinutes)} used " +
                                            "of ${formatMinutes(limitMinutes.toLong())}",

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyLarge,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.weight(1f)
                        )


                        // This option is ALWAYS available.
                        Button(
                            onClick = {
                                goHome()
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                        ) {

                            Text(
                                text = "Don't open"
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )


                        // Continue is deliberately delayed.
                        TextButton(
                            onClick = {

                                continueToApp(
                                    packageName
                                )
                            },

                            enabled =
                                secondsRemaining == 0,

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {


                            if (
                                secondsRemaining > 0
                            ) {

                                Text(
                                    text =
                                        "Continue in ${secondsRemaining}s"
                                )

                            } else {

                                Text(
                                    text =
                                        "Continue anyway"
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun goHome() {

        val homeIntent =
            Intent(
                Intent.ACTION_MAIN
            ).apply {

                addCategory(
                    Intent.CATEGORY_HOME
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK
            }


        startActivity(homeIntent)

        finish()
    }


    private fun continueToApp(
        packageName: String
    ) {

        if (packageName.isBlank()) {

            finish()

            return
        }


        val preferences =
            getSharedPreferences(
                "screenless_preferences",
                MODE_PRIVATE
            )


        /*
         * Give the app 10 seconds to open without
         * ScreenLess intercepting the launch again.
         *
         * This is NOT the existing five-minute
         * post-limit bypass.
         */
        preferences.edit()
            .putString(
                "active_session_package",
                packageName
            )
            .putLong(
                "opening_bypass_$packageName",
                System.currentTimeMillis() + 10_000
            )
            .apply()


        val launchIntent =
            packageManager
                .getLaunchIntentForPackage(
                    packageName
                )


        if (launchIntent != null) {

            startActivity(
                launchIntent
            )
        }


        finish()
    }
}