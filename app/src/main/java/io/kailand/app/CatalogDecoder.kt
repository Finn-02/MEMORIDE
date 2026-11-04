package io.kailand.app

import io.kailand.core.contract.*
import kotlinx.serialization.json.*

/** Decode the packaged Catalog without touching game progress. Caller chooses an IO dispatcher. */
internal fun decodeCatalog(text: String): Catalog {
    val c = Json.parseToJsonElement(text).jsonObject
    fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
    fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }
    return Catalog(
        c.text("contentVersion"), c.text("description"),
        c.getValue("characters").jsonArray.map { value -> value.jsonObject.let {
            CharacterDefinition(it.text("id"), it.text("name"), it.text("assetKey"))
        } },
        c.getValue("expressions").jsonObject.mapValues { it.value.jsonPrimitive.content },
        c.getValue("assets").jsonObject.mapValues { (_, value) -> value.jsonObject.let {
            AssetDefinition(it.text("path"), it.text("kind"))
        } },
        c.getValue("regions").jsonArray.map { value -> value.jsonObject.let {
            RegionDefinition(it.text("id"), it.text("name"), it.text("theme"),
                it.getValue("latitude").jsonPrimitive.double, it.getValue("longitude").jsonPrimitive.double,
                it.strings("itemIds"))
        } },
        c.getValue("items").jsonArray.map { value -> value.jsonObject.let {
            ItemDefinition(it.text("id"), it.text("regionId"), ItemType.valueOf(it.text("type")),
                it.text("name"), it.text("assetKey"))
        } },
        c.getValue("rules").jsonObject.let {
            GameRules(it.getValue("dailyGrowthCap").jsonPrimitive.int,
                it.getValue("dailyGrantLimit").jsonPrimitive.int, it.text("timeZone"), it.strings("decorationSlotIds"))
        },
    )
}
