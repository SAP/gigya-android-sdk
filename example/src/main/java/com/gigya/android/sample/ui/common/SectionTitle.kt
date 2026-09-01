package com.gigya.android.sample.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * A labelled section divider used to group related controls on a screen.
 *
 * Renders the [title] in [MaterialTheme.typography.titleMedium] above a
 * full-width [HorizontalDivider], providing a consistent visual boundary
 * between functional sections without requiring additional spacing composables
 * at every call site.
 *
 * @param title Human-readable section label.
 * @param modifier Optional modifier chain.
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
    )
    HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
private fun SectionTitlePreview() {
    SampleTheme {
        SectionTitle(title = "Credentials")
    }
}
