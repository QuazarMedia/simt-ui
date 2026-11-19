package app.simt.ui.features.qr

import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeFullscreenOverlayTest {
    @Test
    fun `landscape width is bounded by screen height`() {
        assertEquals(360, qrOverlayMaxWidthDp(screenWidthDp = 800, screenHeightDp = 360))
    }

    @Test
    fun `portrait width is bounded by screen width`() {
        assertEquals(360, qrOverlayMaxWidthDp(screenWidthDp = 360, screenHeightDp = 800))
    }
}
