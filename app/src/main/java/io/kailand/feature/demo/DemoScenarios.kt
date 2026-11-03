package io.kailand.feature.demo

import io.kailand.core.contract.DataSource
import io.kailand.core.contract.JourneyKind
import kotlinx.serialization.json.*

/** C-local input plan, not an ArrivalCommand. Fixture UUID/revision/evidence are never forwarded. */
data class DemoScenario(
    val id: String, val title: String, val resetBefore: Boolean,
    val dependsOn: String?, val replayOf: String?, val kind: JourneyKind, val regionId: String?,
    val completedAtEpochMs: Long, val elapsedSeconds: Long?, val distanceMeters: Double?,
    val expectedGrowth: Int, val expectedItems: Set<String>, val expectedDiscovery: String?,
)

object DemoScenarios {
    /** Read fixtures/scenarios.json on A's IO/loading boundary; never replace live game state. */
    fun fromJson(text: String): List<DemoScenario> {
        fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
        fun JsonObject.optional(key: String) = get(key)?.jsonPrimitive?.contentOrNull
        val root = Json.parseToJsonElement(text).jsonObject
        require(root.getValue("schemaVersion").jsonPrimitive.int == 1)
        val plans = root.getValue("scenarios").jsonArray.map { raw ->
            val s = raw.jsonObject
            val a = s.getValue("arrival").jsonObject
            val e = s.getValue("expected").jsonObject
            require(a.text("source") == DataSource.DEMO.name)
            val plan = DemoScenario(
                s.text("id"), s.text("title"), s["resetBefore"]?.jsonPrimitive?.boolean ?: false,
                s.optional("dependsOn"), s.optional("replayOf"), JourneyKind.valueOf(a.text("kind")), a.optional("regionId"),
                a.getValue("completedAtEpochMs").jsonPrimitive.long,
                a.getValue("elapsedSeconds").jsonPrimitive.longOrNull,
                a.getValue("distanceMeters").jsonPrimitive.doubleOrNull,
                e.getValue("growthDelta").jsonPrimitive.int,
                e.getValue("grantedItemIds").jsonArray.map { it.jsonPrimitive.content }.toSet(),
                e.optional("discoveredRegionId"),
            )
            require(plan.id.isNotBlank() && plan.title.isNotBlank() && plan.completedAtEpochMs >= 0)
            require(plan.elapsedSeconds == null || plan.elapsedSeconds >= 0)
            require(plan.distanceMeters == null || (plan.distanceMeters.isFinite() && plan.distanceMeters >= 0))
            require(plan.expectedGrowth >= 0)
            plan
        }
        require(plans.map { it.id }.distinct().size == plans.size)
        val seen = mutableSetOf<String>()
        for (plan in plans) {
            require(plan.dependsOn == null || plan.dependsOn in seen)
            require(plan.replayOf == null || (plan.replayOf in seen && plan.dependsOn == plan.replayOf))
            if (plan.replayOf != null) {
                val original = plans.single { it.id == plan.replayOf }
                require(plan.kind == original.kind && plan.regionId == original.regionId)
                require(plan.expectedGrowth == 0 && plan.expectedItems.isEmpty() && plan.expectedDiscovery == null)
            }
            seen.add(plan.id)
        }
        return plans
    }
}
