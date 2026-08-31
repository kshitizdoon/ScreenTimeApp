package com.example.ScreenLess

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp


@Composable
fun DashboardAppCard(
    app: AppUsage,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {


            app.icon?.let { drawable ->

                Image(
                    bitmap =
                        drawable
                            .toBitmap()
                            .asImageBitmap(),

                    contentDescription =
                        "${app.appName} icon",

                    modifier =
                        Modifier.size(36.dp)
                )


                Spacer(
                    Modifier.height(10.dp)
                )
            }


            Text(
                text = app.appName,

                style =
                    MaterialTheme
                        .typography
                        .titleSmall
            )


            Spacer(
                Modifier.height(4.dp)
            )


            Text(
                text =
                    app.formattedUsage,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )
        }
    }
}