package com.orphybel.bacine.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.orphybel.bacine.AppContainer

/**
 * The dependency graph, handed to composables without a DI framework.
 *
 * Provided once in [com.orphybel.bacine.MainActivity]; reading it outside that
 * tree is a programming error, hence the hard failure rather than a null.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer was not provided")
}

/**
 * Builds a [ViewModel] from the container.
 *
 * Keyed so that, for example, two detail screens for different movies get their
 * own instance instead of sharing one.
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline build: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        key = key,
        viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner available"
        },
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                build(container) as T
        },
    )
}
