package com.example.ScreenLess

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun CategoryUsageRow(
    category: AppCategory,
    usageMinutes: Long,
    limitMinutes: Int,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(vertical = 12.dp)
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
                        usageMinutes
                    ),

                modifier =
                    Modifier.weight(1f),

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )


            Text(
                text =
                    if (limitMinutes > 0) {

                        "Limit: ${
                            formatMinutes(
                                limitMinutes.toLong()
                            )
                        }"

                    } else {

                        "No limit"
                    },

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        HorizontalDivider()
    }
}