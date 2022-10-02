package com.gigya.android.sample.ui

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.gigya.android.sample.R
import com.gigya.android.sample.databinding.ActivityMainBinding
import com.gigya.android.sample.ui.fragment.LoginFragment
import com.gigya.android.sample.ui.fragment.MyAccountFragment
import com.gigya.android.sdk.Gigya

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: MainViewModel


    // Custom result handler for FIDO sender intents.
    val resultHandler: ActivityResultLauncher<IntentSenderRequest> =
            registerForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult()
            ) { activityResult ->
                val extras =
                        activityResult.data?.extras?.keySet()?.map { "$it: ${intent.extras?.get(it)}" }
                                ?.joinToString { it }
                Gigya.getInstance().WebAuthn().handleFidoResult(activityResult)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (savedInstanceState == null) {
            if (viewModel.isLoggedIn()) {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.container, MyAccountFragment.newInstance())
                        .commitNow()
                return
            }
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, LoginFragment.newInstance())
                    .commitNow()
        }
    }

    fun onLogout() {
        supportFragmentManager.popBackStackImmediate()
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, LoginFragment.newInstance())
                .commitNow()
    }
}