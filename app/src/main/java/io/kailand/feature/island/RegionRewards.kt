package io.kailand.feature.island

import io.kailand.core.contract.ItemType
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** B's reward and action rules, read together from the packaged Catalog. */
internal class RegionRewards private constructor(
    private val itemIdsByRegion: Map<String, List<String>>,
    val characterIds: Set<String>,
    val itemTypes: Map<String, ItemType>,
    val decorationSlotIds: Set<String>,
    val dailyGrowthCap: Int,
    val timeZone: ZoneId,
) {
    fun contains(regionId: String): Boolean = regionId in itemIdsByRegion

    fun itemsFor(regionId: String): List<String> = itemIdsByRegion.getValue(regionId).toList()

    companion object {
        fun fromJson(text: String): RegionRewards {
            val catalog = Json.parseToJsonElement(text).jsonObject
            val rules = catalog.getValue("rules").jsonObject
            val growthCap = rules.getValue("dailyGrowthCap").jsonPrimitive.int
            check(growthCap in 1..3) { "DAILY cap must fit the existing save schema" }
            check(rules.getValue("dailyGrantLimit").jsonPrimitive.int == 1) { "Only one DAILY grant per date is supported" }
            val timeZone = rules.getValue("timeZone").jsonPrimitive.content
            check(timeZone == "Asia/Seoul") { "DAILY dates must use Asia/Seoul" }
            val slots = rules.getValue("decorationSlotIds").jsonArray.map { it.jsonPrimitive.content }
            check(slots.size == 3 && slots.toSet().size == 3 && slots.none { it.isBlank() })
            val characters = catalog.getValue("characters").jsonArray.map {
                it.jsonObject.getValue("id").jsonPrimitive.content
            }
            check(characters.size == 3 && characters.toSet().size == 3 && characters.none { it.isBlank() })
            val items = catalog.getValue("items").jsonArray.map { it.jsonObject }
            check(items.map { it.getValue("id").jsonPrimitive.content }.distinct().size == items.size)
            val itemsById = items.associateBy { it.getValue("id").jsonPrimitive.content }
            val itemTypes = itemsById.mapValues { (_, item) ->
                ItemType.valueOf(item.getValue("type").jsonPrimitive.content)
            }
            val regions = catalog.getValue("regions").jsonArray.map { it.jsonObject }
            check(regions.map { it.getValue("id").jsonPrimitive.content }.distinct().size == regions.size)
            val rewards = regions.associate { region ->
                val id = region.getValue("id").jsonPrimitive.content
                check(id.isNotBlank())
                val itemIds = region.getValue("itemIds").jsonArray.map { it.jsonPrimitive.content }
                check(itemIds.size == 3 && itemIds.toSet().size == 3)
                val types = itemIds.map { itemId ->
                    val item = itemsById.getValue(itemId)
                    check(item.getValue("regionId").jsonPrimitive.content == id)
                    itemTypes.getValue(itemId)
                }
                check(types.toSet() == ItemType.entries.toSet())
                id to itemIds
            }
            return RegionRewards(rewards, characters.toSet(), itemTypes, slots.toSet(), growthCap, ZoneId.of(timeZone))
        }
    }
}
