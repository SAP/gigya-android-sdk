package com.gigya.android.sample.ui.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Phone OTP login screen.
 *
 * Two-phase UI: enter phone number → send code, then enter verification code → verify.
 * The verify section is revealed only after [OTPUiState.PendingVerification].
 *
 * @param viewModel The [OTPViewModel] driving the flow.
 * @param onSuccess Called when OTP login succeeds — navigate to [AccountScreen].
 */
@Composable
fun OTPScreen(
    viewModel: OTPViewModel,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState is OTPUiState.Success) {
            viewModel.onNavigated()
            onSuccess()
        }
    }

    OTPScreenContent(
        uiState = viewModel.uiState,
        onSendCode = viewModel::sendCode,
        onVerifyCode = viewModel::verifyCode,
        modifier = modifier,
    )
}

/**
 * Stateless content composable for [OTPScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPScreenContent(
    uiState: OTPUiState,
    onSendCode: (phoneNumber: String) -> Unit,
    onVerifyCode: (code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val showVerify = uiState is OTPUiState.PendingVerification || uiState is OTPUiState.Loading
    val statusMessage = (uiState as? OTPUiState.Error)?.message ?: ""

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OTP Login", style = MaterialTheme.typography.titleMedium) })
        },
        modifier = modifier,
    ) { innerPadding ->

        if (uiState is OTPUiState.Loading) {
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

            SectionTitle("Phone Number")
            InputField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "Phone Number",
                tag = TestTags.INPUT_OTP_PHONE,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !showVerify,
            )
            PrimaryButton(
                text = "Send Code",
                onClick = { onSendCode(phoneNumber) },
                tag = TestTags.BTN_OTP_SEND,
                enabled = phoneNumber.isNotBlank() && !showVerify,
            )

            if (showVerify) {
                SectionTitle("Verification Code")
                Text(
                    "A verification code was sent to $phoneNumber",
                    style = MaterialTheme.typography.bodyMedium,
                )
                InputField(
                    value = code,
                    onValueChange = { code = it },
                    label = "Verification Code",
                    tag = TestTags.INPUT_OTP_CODE,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PrimaryButton(
                    text = "Verify",
                    onClick = { onVerifyCode(code) },
                    tag = TestTags.BTN_OTP_VERIFY,
                    enabled = code.isNotBlank(),
                )
            }

            Spacer(Modifier.height(8.dp))
            StatusRow(message = statusMessage, isError = uiState is OTPUiState.Error)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "OTPScreen — Idle")
@Composable
private fun OTPScreenIdlePreview() {
    SampleTheme {
        OTPScreenContent(uiState = OTPUiState.Idle, onSendCode = {}, onVerifyCode = {})
    }
}

@Preview(showBackground = true, name = "OTPScreen — Pending Verification")
@Composable
private fun OTPScreenPendingPreview() {
    SampleTheme {
        OTPScreenContent(uiState = OTPUiState.PendingVerification, onSendCode = {}, onVerifyCode = {})
    }
}

@Preview(showBackground = true, name = "OTPScreen — Error")
@Composable
private fun OTPScreenErrorPreview() {
    SampleTheme {
        OTPScreenContent(uiState = OTPUiState.Error("Invalid phone number"), onSendCode = {}, onVerifyCode = {})
    }
}
