package com.example.ScreenLess

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun DashboardCategoryCard(
    category: AppCategory,
    usageMinutes: Long,
    totalUsageMinutes: Long,
    onClick: () -> Unit
) {

    val percentage =
        if (totalUsageMinutes > 0) {

            (
                    usageMinutes.toDouble() /
                            totalUsageMinutes.toDouble() *
                            100
                    ).toInt()

        } else {

            0
        }


    val progress =
        if (totalUsageMinutes > 0) {

            (
                    usageMinutes.toFloat() /
                            totalUsageMinutes.toFloat()
                    ).coerceIn(0f, 1f)

        } else {

            0f
        }


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

            Text(
                text = category.label,
                style =
                    MaterialTheme.typography.titleSmall
            )


            Spacer(
                Modifier.height(8.dp)
            )


            Text(
                text =
                    formatMinutes(
                        usageMinutes
                    ),

                style =
                    MaterialTheme.typography.titleLarge
            )


            Spacer(
                Modifier.height(8.dp)
            )


            LinearProgressIndicator(
                progress = {
                    progress
                },
                modifier =
                    Modifier.fillMaxWidth()
            )


            Spacer(
                Modifier.height(5.dp)
            )


            Text(
                text =
                    "$percentage% of today's categorized use",

                style =
                    MaterialTheme.typography.bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}