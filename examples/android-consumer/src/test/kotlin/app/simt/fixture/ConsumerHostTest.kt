package app.simt.fixture

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConsumerHostTest : ConsumerSmokeChecks()
