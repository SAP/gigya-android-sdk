package com.gigya.android.sample.ui.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigya.android.sample.ui.common.InputField
import com.gigya.android.sample.ui.common.PrimaryButton
import com.gigya.android.sample.ui.common.SectionTitle
import com.gigya.android.sample.ui.common.StatusRow
import com.gigya.android.sample.ui.common.TestTags
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * Account linking screen.
 *
 * Shown when a login attempt returns a conflicting-accounts interruption.
 * Allows the user to link the new credentials to an existing site or social account.
 *
 * @param viewModel The [LinkAccountViewModel] holding the resolver.
 * @param onSuccess Called after the link action is initiated — the original
 *   login flow resumes via the resolver callback and navigates to [AccountScreen].
 */
@Composable
fun LinkAccountScreen(
    viewModel: LinkAccountViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LinkAccountScreenContent(
        uiState = viewModel.uiState,
        onLinkToSite = viewModel::linkToSite,
        onLinkToSocial = viewModel::linkToSocial,
        modifier = modifier,
    )
}

/**
 * Stateless content composable for [LinkAccountScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkAccountScreenContent(
    uiState: LinkUiState,
    onLinkToSite: (loginId: String, password: String) -> Unit,
    onLinkToSocial: (provider: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var providerExpanded by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf("") }

    val providers = (uiState as? LinkUiState.Ready)?.providers ?: emptyList()
    val statusMessage = (uiState as? LinkUiState.Error)?.message ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Link Account", style = MaterialTheme.typography.titleMedium) })
        },
        modifier = modifier,
    ) { innerPadding ->

        if (uiState is LinkUiState.Loading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

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
                "An account already exists. Link your accounts to continue.",
                style = MaterialTheme.typography.bodyMedium,
            )

            // Social provider picker
            if (providers.isNotEmpty()) {
                SectionTitle("Link via Social")
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedProvider.ifEmpty { providers.firstOrNull() ?: "" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag(TestTags.DROPDOWN_LINK_PROVIDER),
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false },
                    ) {
                        providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider) },
                                onClick = {
                                    selectedProvider = provider
                                    providerExpanded = false
                                },
                            )
                        }
                    }
                }
                PrimaryButton(
                    text = "Link Social Account",
                    onClick = { onLinkToSocial(selectedProvider.ifEmpty { providers.firstOrNull() ?: "" }) },
                    tag = TestTags.BTN_LINK,
                )
            }

            // Site credentials link
            SectionTitle("Link via Password")
            InputField(
                value = loginId,
                onValueChange = { loginId = it },
                label = "Email",
                tag = TestTags.INPUT_EMAIL,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            InputField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                tag = TestTags.INPUT_LINK_PASSWORD,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
            )
            PrimaryButton(
                text = "Link Site Account",
                onClick = { onLinkToSite(loginId, password) },
                tag = TestTags.BTN_LINK,
                enabled = loginId.isNotBlank() && password.isNotBlank(),
            )

            Spacer(Modifier.height(8.dp))
            StatusRow(message = statusMessage, isError = uiState is LinkUiState.Error)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "LinkAccountScreen — Ready")
@Composable
private fun LinkAccountScreenReadyPreview() {
    SampleTheme {
        LinkAccountScreenContent(
            uiState = LinkUiState.Ready(providers = listOf("facebook", "google")),
            onLinkToSite = { _, _ -> },
            onLinkToSocial = {},
        )
    }
}

@Preview(showBackground = true, name = "LinkAccountScreen — Idle")
@Composable
private fun LinkAccountScreenIdlePreview() {
    SampleTheme {
        LinkAccountScreenContent(
            uiState = LinkUiState.Idle,
            onLinkToSite = { _, _ -> },
            onLinkToSocial = {},
        )
    }
}
