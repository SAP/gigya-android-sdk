package com.gigya.android.sample.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigya.android.sample.ui.common.InputField
import com.gigya.android.sample.ui.common.PrimaryButton
import com.gigya.android.sample.ui.common.SectionTitle
import com.gigya.android.sample.ui.common.StatusRow
import com.gigya.android.sample.ui.common.TestTags
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * SDK re-initialisation settings screen.
 *
 * Allows changing the API key, data center, and CNAME at runtime.
 * Warning: re-initialising clears the current session — the user must log in again.
 *
 * @param viewModel The [SettingsViewModel].
 * @param onReinitialized Called after successful re-init so the nav stack can pop back.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onReinitialized: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState is SettingsUiState.Success) {
            viewModel.onNavigated()
            onReinitialized()
        }
    }

    SettingsScreenContent(
        uiState = viewModel.uiState,
        onReinitialize = viewModel::reinitialize,
        modifier = modifier,
    )
}

/**
 * Stateless content composable for [SettingsScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onReinitialize: (apiKey: String, dataCenter: String, cname: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiKey by remember { mutableStateOf("") }
    var dataCenter by remember { mutableStateOf("") }
    var cname by remember { mutableStateOf("") }
    val statusMessage = (uiState as? SettingsUiState.Error)?.message ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SDK Settings", style = MaterialTheme.typography.titleMedium) })
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Re-initialising the SDK will invalidate the current session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            SectionTitle("SDK Configuration")
            InputField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = "API Key",
                tag = TestTags.INPUT_API_KEY,
            )
            InputField(
                value = dataCenter,
                onValueChange = { dataCenter = it },
                label = "Data Center (optional)",
                tag = TestTags.INPUT_DATA_CENTER,
            )
            InputField(
                value = cname,
                onValueChange = { cname = it },
                label = "CNAME (optional)",
                tag = TestTags.INPUT_CNAME,
            )
            PrimaryButton(
                text = "Re-Initialize SDK",
                onClick = { onReinitialize(apiKey, dataCenter, cname) },
                tag = TestTags.BTN_REINIT,
                enabled = apiKey.isNotBlank(),
            )
            Spacer(Modifier.height(8.dp))
            StatusRow(message = statusMessage, isError = uiState is SettingsUiState.Error)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "SettingsScreen — Idle")
@Composable
private fun SettingsScreenIdlePreview() {
    SampleTheme {
        SettingsScreenContent(uiState = SettingsUiState.Idle, onReinitialize = { _, _, _ -> })
    }
}

@Preview(showBackground = true, name = "SettingsScreen — Error")
@Composable
private fun SettingsScreenErrorPreview() {
    SampleTheme {
        SettingsScreenContent(
            uiState = SettingsUiState.Error("Invalid API key"),
            onReinitialize = { _, _, _ -> },
        )
    }
}
