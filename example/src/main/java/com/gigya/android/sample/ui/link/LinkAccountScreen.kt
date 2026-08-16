package com.gigya.android.sample.ui.link

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * Account-linking screen — stub placeholder.
 *
 * Full implementation delivered in task p3-flow-login.
 */
@Composable
fun LinkAccountScreen(
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text("Link Account Screen — coming soon")
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkAccountScreenPreview() {
    SampleTheme {
        LinkAccountScreen(onSuccess = {})
    }
}
