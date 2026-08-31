package com.example.ScreenLess

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ScreenLess.ui.theme.MyApplicationTheme


class PostureInterventionActivity :
    ComponentActivity() {


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        val appName =
            intent.getStringExtra(
                "appName"
            ) ?: "this app"


        setContent {

            MyApplicationTheme {

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {


                    Text(
                        text =
                            "YOU'VE BEEN HERE A WHILE",

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
                        Modifier.height(20.dp)
                    )


                    Text(
                        text =
                            "Still using $appName?",

                        style =
                            MaterialTheme
                                .typography
                                .headlineLarge,

                        textAlign =
                            TextAlign.Center
                    )


                    Spacer(
                        Modifier.height(20.dp)
                    )


                    Text(
                        text =
                            "Your phone's motion and orientation suggest " +
                                    "you may be using it while lying down.",

                        textAlign =
                            TextAlign.Center,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(
                        Modifier.height(32.dp)
                    )


                    Button(
                        onClick = {
                            goHome()
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Put the phone down"
                        )
                    }


                    Spacer(
                        Modifier.height(10.dp)
                    )


                    TextButton(
                        onClick = {
                            finish()
                        }
                    ) {

                        Text(
                            "I'm not lying down"
                        )
                    }
                }
            }
        }
    }


    private fun goHome() {

        val intent =
            Intent(
                Intent.ACTION_MAIN
            ).apply {

                addCategory(
                    Intent.CATEGORY_HOME
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK
            }


        startActivity(intent)

        finish()
    }
}