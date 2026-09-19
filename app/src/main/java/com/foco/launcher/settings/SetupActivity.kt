package com.foco.launcher.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foco.launcher.FocoApp
import com.foco.launcher.core.FocoTheme
import com.foco.launcher.core.LaunchController
import com.foco.launcher.core.LauncherActivity

class SetupActivity : ComponentActivity() {
    private val vm: SetupViewModel by viewModels { SetupViewModel.factory(application as FocoApp) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocoTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(state.finished) {
                    if (state.finished) {
                        startActivity(
                            Intent(this@SetupActivity, LauncherActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        )
                        finish()
                    }
                }
                SetupScreen(
                    state = state,
                    onStart = vm::onWelcomeContinue,
                    onToggle = vm::toggle,
                    onContinueApps = vm::persistSelectionAndContinue,
                    onChooseDefault = {
                        // Persist setup before the OEM picker so HOME cannot bounce back into S0.
                        vm.finishSetup()
                        LaunchController.openHomePicker(this)
                    },
                    onSkipDefault = vm::finishSetup,
                    onBackToApps = vm::backToApps,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume(LaunchController.isDefaultHome(this))
        (application as FocoApp).registry.refreshIfPackagesChanged()
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, SetupActivity::class.java)
    }
}
