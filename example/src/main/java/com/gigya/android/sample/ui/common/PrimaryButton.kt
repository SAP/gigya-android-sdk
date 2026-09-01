package com.gigya.android.sample.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * A full-width primary action button.
 *
 * Uses Material3 [Button] with a fixed height for visual consistency across
 * all screens. The [tag] is applied as a semantic test tag so instrumented
 * tests can locate and click it by constant.
 *
 * @param text Label displayed on the button.
 * @param onClick Action invoked on tap.
 * @param tag [TestTags] constant used as the semantic test tag.
 * @param modifier Optional modifier chain.
 * @param enabled Whether the button is interactive.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(tag),
    ) {
        Text(text)
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    SampleTheme {
        PrimaryButton(
            text = "Login",
            onClick = {},
            tag = TestTags.BTN_LOGIN,
        )
    }
}

@Preview(showBackground = true, name = "PrimaryButton — disabled")
@Composable
private fun PrimaryButtonDisabledPreview() {
    SampleTheme {
        PrimaryButton(
            text = "Login",
            onClick = {},
            tag = TestTags.BTN_LOGIN,
            enabled = false,
        )
    }
}
