package io.kailand.app.journey

import io.kailand.core.contract.RequestResult
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class NavigationLauncherTest {
    @Test fun unknownOrBackgroundNeverResolvesOrOpensExternalApp() {
        var resolves = 0
        val launcher = NavigationLauncher { resolves++; { fail("must not open") } }
        assertTrue(launcher.launch { false } is RequestResult.Failed)
        assertEquals(0, resolves)
    }
    @Test fun changedGateBetweenResolutionAndLaunchBlocksSideEffect() {
        var allowed = true; var opens = 0
        val launcher = NavigationLauncher { allowed = false; { opens++ } }
        assertTrue(launcher.launch { allowed } is RequestResult.Failed)
        assertEquals(0, opens)
    }
    @Test fun missingAppAndRejectedLaunchCanRetryWithoutInventingArrival() {
        var present = false; var fails = true; var opens = 0
        val launcher = NavigationLauncher {
            if (!present) null else { { if (fails) throw SecurityException(); opens++ } }
        }
        assertTrue(launcher.launch { true } is RequestResult.Failed)
        present = true
        assertTrue(launcher.launch { true } is RequestResult.Failed)
        fails = false
        assertEquals(RequestResult.Accepted, launcher.launch { true })
        assertEquals(1, opens)
    }
    @Test fun cancellationIsNotConvertedToFailureOrRetried() {
        val launcher = NavigationLauncher { { throw CancellationException() } }
        try { launcher.launch { true }; fail("expected cancellation") } catch (_: CancellationException) { }
    }
}
