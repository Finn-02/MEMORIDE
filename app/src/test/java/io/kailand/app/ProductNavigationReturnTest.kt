package io.kailand.app

import io.kailand.core.contract.MutationResult
import io.kailand.core.contract.RequestResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProductNavigationReturnTest {
    @Test fun ordinaryColdStartStillRequiresCharacterSelection() {
        val navigation = ProductNavigation()
        navigation.destinationObserved("destination:1")
        assertEquals(ProductRoute.CHARACTER, navigation.state.value.route)
    }

    @Test fun departureRecordsReturnBeforeExternalLaunchAndSurvivesBackgrounding() = runBlocking {
        var savedReturn = false
        var foreground = true
        val navigation = ProductNavigation(onNavigationReturnChanged = { savedReturn = it })
        navigation.selectCharacter(navigation.state.value, { true }) { MutationResult.Success }
        val result = navigation.depart(navigation.state.value, { foreground }) {
            assertTrue(savedReturn)
            foreground = false // The external Activity pauses MEMORIDE immediately.
            RequestResult.Accepted
        }
        assertEquals(RequestResult.Accepted, result)
        assertTrue(savedReturn)
        val restored = ProductNavigation(resumeNavigationReturn = savedReturn,
            onNavigationReturnChanged = { savedReturn = it })
        assertEquals(ProductRoute.ISLAND, restored.state.value.route)
        restored.destinationObserved("destination:1")
        assertEquals(ProductRoute.ISLAND, restored.state.value.route)
        assertFalse(savedReturn)
    }

    @Test fun failedLaunchDoesNotRestoreAnExternalReturn() = runBlocking {
        var savedReturn = false
        val navigation = ProductNavigation(onNavigationReturnChanged = { savedReturn = it })
        navigation.selectCharacter(navigation.state.value, { true }) { MutationResult.Success }
        navigation.depart(navigation.state.value, { true }) { RequestResult.Failed("not installed") }
        assertFalse(savedReturn)
        assertEquals(ProductRoute.NAVIGATION_FAILURE, navigation.state.value.route)
        assertEquals(ProductRoute.CHARACTER, ProductNavigation(savedReturn).state.value.route)
    }

    @Test fun explicitCharacterChoiceClearsContinuationWithoutBypassingSelection() = runBlocking {
        var savedReturn = false
        val navigation = ProductNavigation(onNavigationReturnChanged = { savedReturn = it })
        navigation.selectCharacter(navigation.state.value, { true }) { MutationResult.Success }
        navigation.depart(navigation.state.value, { true }) { RequestResult.Accepted }
        assertTrue(savedReturn)
        assertTrue(navigation.open(navigation.state.value, ProductRoute.CHARACTER))
        assertFalse(savedReturn)
        navigation.destinationObserved("destination:2")
        assertEquals(ProductRoute.CHARACTER, navigation.state.value.route)
    }

    @Test fun receiptAndDemoResetClearRestoredReturn() {
        var savedReturn = true
        val navigation = ProductNavigation(true, { savedReturn = it })
        navigation.presentReceipt("original-receipt")
        assertEquals(ProductRoute.BRIEFING, navigation.state.value.route)
        assertFalse(savedReturn)
        savedReturn = true
        val reset = ProductNavigation(true, { savedReturn = it })
        reset.resetAfterDemo()
        assertFalse(savedReturn)
        assertEquals(ProductRoute.CHARACTER, reset.state.value.route)
    }
}
