package com.orphybel.bacine

import android.app.Application

class BaCineApp : Application() {
    /**
     * Coil picks up its OkHttp network fetcher automatically through
     * `META-INF/services`, so there is no image loader to configure here.
     */
    val container: AppContainer by lazy { AppContainer(this) }
}
