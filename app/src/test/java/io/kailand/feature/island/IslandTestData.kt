package io.kailand.feature.island

import io.kailand.core.contract.ArrivalCommand
import io.kailand.core.contract.ArrivalEvidence
import io.kailand.core.contract.DataSource
import io.kailand.core.contract.JourneyKind
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/** Read the PM-owned sources directly; never maintain a second fixture/catalog. */
internal object IslandTestData {
    private val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "content/catalog.json").isFile }
    val rewards = RegionRewards.fromJson(File(root, "content/catalog.json").readText(Charsets.UTF_8))
    private val scenarios = Json.parseToJsonElement(
        File(root, "fixtures/scenarios.json").readText(Charsets.UTF_8),
    ).jsonObject.getValue("scenarios").jsonArray.associate { element ->
        val scenario = element.jsonObject
        scenario.getValue("id").jsonPrimitive.content to scenario
    }

    fun expected(id: String): JsonObject = scenarios.getValue(id).getValue("expected").jsonObject

    fun arrival(id: String): ArrivalCommand {
        val input = scenarios.getValue(id).getValue("arrival").jsonObject
        return ArrivalCommand(
            journeyId = input.getValue("journeyId").jsonPrimitive.content,
            source = DataSource.valueOf(input.getValue("source").jsonPrimitive.content),
            kind = JourneyKind.valueOf(input.getValue("kind").jsonPrimitive.content),
            regionId = input.getValue("regionId").jsonPrimitive.content.takeUnless { it == "null" },
            routeRevision = input.getValue("routeRevision").jsonPrimitive.int,
            evidence = ArrivalEvidence.valueOf(input.getValue("evidence").jsonPrimitive.content),
            completedAtEpochMs = input.getValue("completedAtEpochMs").jsonPrimitive.long,
            elapsedSeconds = input.getValue("elapsedSeconds").jsonPrimitive.longOrNull,
            distanceMeters = input.getValue("distanceMeters").jsonPrimitive.doubleOrNull,
        )
    }
}
