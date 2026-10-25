package com.orphybel.bacine.ui.detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The bridge add-on splits its query on `&` and `=`, so a title carrying
 * either — or spaces, accents, a colon — must arrive fully encoded.
 */
class KodiLauncherTest {

    @Test
    fun `plain title is encoded with percent-20 spaces`() {
        assertEquals(
            "plugin://plugin.video.bacine/?action=search&title=Le%20Comte%20de%20Monte-Cristo",
            bridgeSearchUrl("Le Comte de Monte-Cristo"),
        )
    }

    @Test
    fun `reserved characters and accents never leak into the query`() {
        val url = bridgeSearchUrl("Dune : Deuxième partie & co = ?")
        val title = url.substringAfter("title=")

        assertEquals("Dune%20%3A%20Deuxi%C3%A8me%20partie%20%26%20co%20%3D%20%3F", title)
        assertFalse('+' in title)
    }

    @Test
    fun `surrounding whitespace is dropped`() {
        assertEquals(bridgeSearchUrl("Alien"), bridgeSearchUrl("  Alien "))
    }
}
