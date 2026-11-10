package io.kailand.feature.demo

import io.kailand.core.contract.*
import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class UxDemoPresetsTest {
    private val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "content/catalog.json").isFile }
    private val data = Json.parseToJsonElement(File(root, "content/catalog.json").readText()).jsonObject
    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
    private val catalog = Catalog("test", "test", emptyList(), emptyMap(), emptyMap(),
        data.getValue("regions").jsonArray.map { v -> v.jsonObject.let { r ->
            RegionDefinition(r.text("id"), r.text("name"), r.text("theme"), r.getValue("latitude").jsonPrimitive.double,
                r.getValue("longitude").jsonPrimitive.double, r.getValue("itemIds").jsonArray.map { it.jsonPrimitive.content })
        } }, data.getValue("items").jsonArray.map { v -> v.jsonObject.let { i ->
            ItemDefinition(i.text("id"), i.text("regionId"), ItemType.valueOf(i.text("type")), i.text("name"), i.text("assetKey"))
        } }, GameRules(3, 1, "Asia/Seoul", emptyList()))
    private val items = catalog.regions.single { it.id == "wonju" }.itemIds.toSet()
    private val fresh = GameSnapshot(AvatarPreview("tori", null), 0, emptySet(), emptySet(), emptyMap(), null)
    private fun request(preset: UxDemoPreset) = UxDemoRequest(preset, "control-request-not-journey-id")
    private fun result(preset: UxDemoPreset = UxDemoPreset.NEW_REGION): UxDemoEvidence {
        val revisit = preset == UxDemoPreset.REVISIT
        val before = if (revisit) fresh.copy(dailyGrowth = 1, ownedItemIds = items, discoveredRegionIds = setOf("wonju")) else fresh
        val receipt = ArrivalReceipt("A-original-runtime-id", DataSource.DEMO, JourneyKind.REGION, "wonju", 1,
            null, null, before.dailyGrowth, 1, if (revisit) null else "wonju", if (revisit) emptyList() else items.toList())
        val after = before.copy(dailyGrowth = 1, ownedItemIds = items, discoveredRegionIds = setOf("wonju"))
        val original = ArrivalCommand(receipt.journeyId, DataSource.DEMO, JourneyKind.REGION, "wonju", 0,
            ArrivalEvidence.DEMO_FINAL_DESTINATION, receipt.completedAtEpochMs)
        return UxDemoEvidence(request(preset), "A-isolated-session", true, DataSource.DEMO, before,
            if (revisit) 2 else 0, revisit, before, after, if (revisit) 3 else 1, receipt,
            saveFailure = if (preset == UxDemoPreset.SAVE_RETRY) UxDemoSaveFailure(original, original, before) else null,
            finished = true)
    }
    private fun check(e: UxDemoEvidence) = checkUxDemoResult(e.request, e, catalog).verdict

    @Test fun exactlyFourNewPresetsAndNormalNewRegionCanPass() {
        assertEquals(4, UxDemoPreset.entries.size)
        assertEquals(UxDemoVerdict.PASS, check(result()))
    }

    @Test fun acceptedPreparationOrPendingSaveIsNeverASuccessfulDemo() {
        val e = result()
        assertEquals(UxDemoVerdict.NOT_RUN, checkUxDemoResult(e.request, null, catalog).verdict)
        assertEquals(UxDemoVerdict.NOT_RUN, check(e.copy(finished = false, receipt = null)))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(receipt = null)))
    }

    @Test fun noLiveEnvironmentOldRequestOrAmbiguousCatalogMayPass() {
        val e = result()
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(isolatedEnvironment = false)))
        assertEquals(UxDemoVerdict.FAIL, checkUxDemoResult(e.request.copy(requestToken = "old"), e, catalog).verdict)
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(sessionId = "")))
        assertEquals(UxDemoVerdict.FAIL, checkUxDemoResult(e.request, e,
            catalog.copy(regions = catalog.regions + catalog.regions.single { it.id == "wonju" })).verdict)
    }

    @Test fun sameDayRevisitIncrementsOnlyVisitCountAndRequiresSameDayPreparation() {
        val e = result(UxDemoPreset.REVISIT)
        assertEquals(UxDemoVerdict.PASS, check(e))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(revisitGrowthAlreadyUsedToday = false)))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(receipt = e.receipt!!.copy(growthAfter = 2))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(receipt = e.receipt!!.copy(grantedItemIds = items.toList()))))
    }

    @Test fun nullCountsDuplicateGiftsAndUnrelatedProgressNeverPass() {
        val e = result()
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(visitsBefore = null)))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(visitsAfter = null)))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(receipt = e.receipt!!.copy(grantedItemIds = items.toList() + items.first()))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(beforeAttempt = fresh.copy(dailyGrowth = 2))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(afterAttempt = e.afterAttempt!!.copy(ownedItemIds = items + "extra"))))
    }

    @Test fun characterSelectionBeforeAttemptIsAllowedAndSourceIsNotRewritten() {
        val e = result()
        assertEquals(UxDemoVerdict.PASS, check(e.copy(beforeAttempt = fresh.copy(avatar = AvatarPreview("momo", null)))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(receipt = e.receipt!!.copy(source = DataSource.PLEOS))))
        val confirmed = e.copy(source = DataSource.USER_CONFIRMED, receipt = e.receipt!!.copy(source = DataSource.USER_CONFIRMED))
        assertEquals(UxDemoVerdict.PASS, check(confirmed))
        assertEquals(UxDemoVerdict.FAIL, check(confirmed.copy(receipt = confirmed.receipt!!.copy(distanceMeters = 0.0))))
    }

    @Test fun saveFailureRetryRequiresSameOriginalIdAndUnchangedFailureSnapshot() {
        val e = result(UxDemoPreset.SAVE_RETRY)
        assertEquals(UxDemoVerdict.PASS, check(e))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(saveFailure = null)))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(saveFailure = e.saveFailure!!.copy(
            retriedCommand = e.saveFailure!!.originalCommand.copy(journeyId = "new-id")))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(saveFailure = e.saveFailure!!.copy(
            retriedCommand = e.saveFailure!!.originalCommand.copy(completedAtEpochMs = 2)))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(saveFailure = e.saveFailure!!.copy(
            retriedCommand = e.saveFailure!!.originalCommand.copy(routeRevision = 1)))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(saveFailure = e.saveFailure!!.copy(snapshotAtConfirmedFailure = e.afterAttempt!!))))
    }

    private fun navigationFailure(): UxDemoEvidence = result(UxDemoPreset.NAVIGATION_FAILURE).copy(
        afterAttempt = fresh, receipt = null, visitsAfter = 0,
        navigationFailure = UxDemoNavigationFailure(fresh, false, false, null),
        navigationRecovery = UxDemoNavigationRecovery.RETURNED_TO_ISLAND)

    @Test fun navigationFailureIslandReturnHasNoTravelArrivalOrReward() {
        val e = navigationFailure()
        assertEquals(UxDemoVerdict.PASS, check(e))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(navigationFailure = e.navigationFailure!!.copy(travelStarted = true))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(navigationFailure = e.navigationFailure!!.copy(arrivalSubmitted = true))))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(afterAttempt = fresh.copy(dailyGrowth = 1))))
    }

    @Test fun navigationRetryMustBeObservedAndFollowedByACommittedReceipt() {
        val e = navigationFailure()
        assertEquals(UxDemoVerdict.NOT_RUN, check(e.copy(navigationRecovery = UxDemoNavigationRecovery.NONE)))
        assertEquals(UxDemoVerdict.FAIL, check(e.copy(navigationRecovery = UxDemoNavigationRecovery.RETRIED_SUCCESSFULLY)))
        val completed = result(UxDemoPreset.NAVIGATION_FAILURE)
        assertEquals(UxDemoVerdict.PASS, check(completed.copy(navigationFailure = e.navigationFailure,
            navigationRecovery = UxDemoNavigationRecovery.RETRIED_SUCCESSFULLY)))
    }
}
