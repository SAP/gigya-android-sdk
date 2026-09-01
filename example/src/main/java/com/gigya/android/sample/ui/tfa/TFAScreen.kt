package com.gigya.android.sample.ui.tfa

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gigya.android.sample.ui.common.InputField
import com.gigya.android.sample.ui.common.PrimaryButton
import com.gigya.android.sample.ui.common.SectionTitle
import com.gigya.android.sample.ui.common.StatusRow
import com.gigya.android.sample.ui.common.TestTags
import com.gigya.android.sample.ui.theme.SampleTheme

/**
 * Two-Factor Authentication screen.
 *
 * Handles both TFA registration (new authenticator setup) and TFA verification
 * (existing authenticator challenge). The [TFAViewModel] drives the multi-step
 * resolver flow — this screen is a pure function of [TFAUiState].
 *
 * @param viewModel The [TFAViewModel] holding the resolver state.
 * @param onSuccess Called when TFA completes — the original login flow resumes
 *   via its callback and navigates to [AccountScreen].
 */
@Composable
fun TFAScreen(
    viewModel: TFAViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState is TFAUiState.Success) {
            viewModel.onNavigated()
            onSuccess()
        }
    }

    TFAScreenContent(
        uiState = viewModel.uiState,
        onProviderSelected = { provider, isRegistration ->
            viewModel.onProviderSelected(provider, isRegistration)
        },
        onRegisterPhone = viewModel::registerPhone,
        onSendEmailCode = viewModel::sendEmailCode,
        onVerifyCode = { code, isTOTP ->
            if (isTOTP) viewModel.verifyTotpCode(code) else viewModel.verifyPhoneCode(code)
        },
        modifier = modifier,
    )
}

/**
 * Stateless content composable for [TFAScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TFAScreenContent(
    uiState: TFAUiState,
    onProviderSelected: (com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel, Boolean) -> Unit,
    onRegisterPhone: (String) -> Unit,
    onSendEmailCode: (com.gigya.android.sdk.tfa.models.EmailModel) -> Unit,
    onVerifyCode: (code: String, isTOTP: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var phoneNumber by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }
    var providerExpanded by remember { mutableStateOf(false) }
    var isTOTP by remember { mutableStateOf(false) }

    val statusMessage = (uiState as? TFAUiState.Error)?.message ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Two-Factor Authentication", style = MaterialTheme.typography.titleMedium) })
        },
        modifier = modifier,
    ) { innerPadding ->

        if (uiState is TFAUiState.Loading) {
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

            // Provider selector — shown when providers are available
            if (uiState is TFAUiState.ProviderSelection) {
                SectionTitle("Select Authenticator")
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it },
                ) {
                    OutlinedTextField(
                        value = if (uiState.providers.isEmpty()) "" else uiState.providers[0].name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag(TestTags.DROPDOWN_TFA_PROVIDER),
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false },
                    ) {
                        uiState.providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.name) },
                                onClick = {
                                    providerExpanded = false
                                    isTOTP = provider.name == com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.TOTP
                                    onProviderSelected(provider, true)
                                },
                            )
                        }
                    }
                }
            }

            // TOTP QR code
            if (uiState is TFAUiState.TOTPQRCode) {
                SectionTitle("Scan QR Code")
                val bitmap = remember(uiState.qrCode) {
                    runCatching {
                        val bytes = Base64.decode(uiState.qrCode, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "TOTP QR code",
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                            .testTag(TestTags.IMAGE_QR_CODE),
                    )
                }
                Spacer(Modifier.height(8.dp))
                SectionTitle("Enter Code")
                InputField(
                    value = verifyCode,
                    onValueChange = { verifyCode = it },
                    label = "Verification Code",
                    tag = TestTags.INPUT_TFA_CODE,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PrimaryButton(
                    text = "Verify",
                    onClick = { onVerifyCode(verifyCode, true) },
                    tag = TestTags.BTN_TFA_VERIFY,
                    enabled = verifyCode.isNotBlank(),
                )
            }

            // Phone registration entry
            if (uiState is TFAUiState.PhoneEntry) {
                SectionTitle("Register Phone")
                InputField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "Phone Number",
                    tag = TestTags.INPUT_PHONE_NUMBER,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                PrimaryButton(
                    text = "Send Code",
                    onClick = { onRegisterPhone(phoneNumber) },
                    tag = TestTags.BTN_REGISTER_PHONE,
                    enabled = phoneNumber.isNotBlank(),
                )
            }

            // Email selection — shown when registered emails are loaded
            if (uiState is TFAUiState.EmailSelection) {
                SectionTitle("Select Email")
                Text(
                    "Select a registered email to receive your verification code:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                uiState.emails.forEach { email ->
                    PrimaryButton(
                        text = email.obfuscated ?: email.id ?: "Email",
                        onClick = { onSendEmailCode(email) },
                        tag = TestTags.TEXT_EMAIL_TFA_HINT,
                    )
                }
            }

            // Code verification entry (phone)
            if (uiState is TFAUiState.CodeEntry) {
                SectionTitle("Enter Verification Code")
                InputField(
                    value = verifyCode,
                    onValueChange = { verifyCode = it },
                    label = "Verification Code",
                    tag = TestTags.INPUT_TFA_CODE,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PrimaryButton(
                    text = "Verify",
                    onClick = { onVerifyCode(verifyCode, isTOTP) },
                    tag = TestTags.BTN_TFA_VERIFY,
                    enabled = verifyCode.isNotBlank(),
                )
            }

            Spacer(Modifier.height(8.dp))
            StatusRow(message = statusMessage, isError = uiState is TFAUiState.Error)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "TFAScreen — Idle")
@Composable
private fun TFAScreenIdlePreview() {
    SampleTheme {
        TFAScreenContent(
            uiState = TFAUiState.Idle,
            onProviderSelected = { _, _ -> },
            onRegisterPhone = {},
            onSendEmailCode = {},
            onVerifyCode = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "TFAScreen — Code Entry")
@Composable
private fun TFAScreenCodeEntryPreview() {
    SampleTheme {
        TFAScreenContent(
            uiState = TFAUiState.CodeEntry,
            onProviderSelected = { _, _ -> },
            onRegisterPhone = {},
            onSendEmailCode = {},
            onVerifyCode = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "TFAScreen — Phone Entry")
@Composable
private fun TFAScreenPhoneEntryPreview() {
    SampleTheme {
        TFAScreenContent(
            uiState = TFAUiState.PhoneEntry,
            onProviderSelected = { _, _ -> },
            onRegisterPhone = {},
            onSendEmailCode = {},
            onVerifyCode = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "TFAScreen — Error")
@Composable
private fun TFAScreenErrorPreview() {
    SampleTheme {
        TFAScreenContent(
            uiState = TFAUiState.Error("Invalid verification code"),
            onProviderSelected = { _, _ -> },
            onRegisterPhone = {},
            onSendEmailCode = {},
            onVerifyCode = { _, _ -> },
        )
    }
}
