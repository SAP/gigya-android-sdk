package com.gigya.android.sample.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * A full-width outlined text input field.
 *
 * Wraps [OutlinedTextField] with consistent styling and an explicit [testTag]
 * so that instrumented tests can locate and interact with it by tag.
 *
 * @param value Current text value.
 * @param onValueChange Callback invoked on every keystroke.
 * @param label Floating label displayed above the field.
 * @param tag [TestTags] constant used as the semantic test tag.
 * @param modifier Optional modifier chain.
 * @param keyboardOptions Keyboard type, IME action, etc.
 * @param visualTransformation Use [androidx.compose.ui.text.input.PasswordVisualTransformation]
 *   for password fields.
 * @param enabled Whether the field accepts input.
 */
@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
    )
}

@Preview(showBackground = true)
@Composable
private fun InputFieldPreview() {
    SampleTheme {
        InputField(
            value = "user@example.com",
            onValueChange = {},
            label = "Email",
            tag = TestTags.INPUT_EMAIL,
        )
    }
}

@Preview(showBackground = true, name = "InputField — empty")
@Composable
private fun InputFieldEmptyPreview() {
    SampleTheme {
        InputField(
            value = "",
            onValueChange = {},
            label = "Email",
            tag = TestTags.INPUT_EMAIL,
        )
    }
}
