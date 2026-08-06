package com.orphybel.bacine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.orphybel.bacine.ui.LocalAppContainer
import com.orphybel.bacine.ui.components.LoadingState
import com.orphybel.bacine.ui.nav.BaCineNavHost
import com.orphybel.bacine.ui.nav.Routes
import com.orphybel.bacine.ui.theme.BaCineTheme
import com.orphybel.bacine.ui.theme.Ink
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as BaCineApp).container

        setContent {
            BaCineTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    Root()
                }
            }
        }
    }
}

/**
 * Decides where the app opens.
 *
 * Reading the key is a disk hit, so the first frame is a loading state rather
 * than a guess that would flash the wrong screen and then replace it.
 */
@Composable
private fun Root() {
    val container = LocalAppContainer.current

    // Plain state plus an effect rather than produceState: the intent is the
    // same, and it reads more directly for a one-shot read.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(container) {
        val key = container.settings.effectiveApiKey.first()
        startDestination = if (key.isBlank()) Routes.ONBOARDING else Routes.HOME
    }

    val destination = startDestination
    if (destination == null) {
        LoadingState(Modifier.fillMaxSize().background(Ink))
    } else {
        BaCineNavHost(
            startDestination = destination,
            modifier = Modifier.fillMaxSize().background(Ink),
        )
    }
}
