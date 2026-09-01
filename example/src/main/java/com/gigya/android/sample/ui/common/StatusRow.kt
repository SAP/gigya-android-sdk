package com.gigya.android.sample.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * A single-line status row that displays an operation result or error message.
 *
 * Renders in the error colour when [isError] is true, otherwise uses the
 * primary colour. Intentionally minimal — its role is to surface SDK
 * response text so that both users and automated tests can read the outcome.
 *
 * The [TestTags.TEXT_STATUS] tag is always applied so tests can assert
 * the displayed text regardless of which screen hosts this component.
 *
 * @param message Text to display. Pass an empty string to hide the row.
 * @param isError When true, renders the message in the error colour.
 * @param modifier Optional modifier chain.
 */
@Composable
fun StatusRow(
    message: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (message.isEmpty()) return

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(TestTags.TEXT_STATUS),
    )
}

@Preview(showBackground = true)
@Composable
private fun StatusRowSuccessPreview() {
    SampleTheme {
        StatusRow(message = "Login successful — UID: abc123")
    }
}

@Preview(showBackground = true, name = "StatusRow — error")
@Composable
private fun StatusRowErrorPreview() {
    SampleTheme {
        StatusRow(message = "Error 403006: Invalid login credentials", isError = true)
    }
}
