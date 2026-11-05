package io.kailand.feature.island

import android.content.res.AssetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.AssetDefinition
import io.kailand.core.contract.AvatarPreview
import io.kailand.core.contract.Catalog
import io.kailand.core.contract.CharacterDefinition
import io.kailand.core.contract.GameRules
import io.kailand.core.contract.ItemDefinition
import io.kailand.core.contract.ItemType
import io.kailand.core.contract.RegionDefinition
import io.kailand.core.ui.MemorideTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Use Run Preview on Android to exercise the packaged assets and production AvatarView. */
@Preview(name = "B04-2A Avatar runtime", showBackground = true)
@Composable
fun AvatarRuntimePreview() {
    AvatarRuntimeGalleryScreen(onBack = {})
}

/** Debug gallery for the host activity. The host owns navigation via [onBack]. */
@Composable
fun AvatarRuntimeGalleryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MemorideTheme {
        Surface(modifier.fillMaxSize().systemBarsPadding()) {
            Column {
                TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Back") }
                if (LocalInspectionMode.current) {
                    Text("B04-2A · Run Preview → MEMORIDE_API37_Test", Modifier.padding(24.dp))
                } else {
                    val assets = LocalContext.current.applicationContext.assets
                    val loaded by produceState<Result<Catalog>?>(null, assets) {
                        value = withContext(Dispatchers.IO) { runCatching { readPreviewCatalog(assets) } }
                    }
                    val result = loaded
                    when {
                        result == null -> Text("Catalog loading…", Modifier.padding(24.dp))
                        result.isFailure -> Text("Catalog error · ${result.exceptionOrNull()?.javaClass?.simpleName}", Modifier.padding(24.dp))
                        else -> AvatarRuntimeGallery(result.getOrThrow())
                    }
                }
            }
        }
    }
}

private val bodies = listOf("tori", "momo", "lulu")
private val expressions = listOf("idle", "happy", "eat")
private val outfits = listOf(null, "daejeon_hat", "wonju_crown", "sokcho_scarf")
private const val FALLBACK_PAGE = 9
private const val MISSING_PNG = "assets/starter/b04_2a_missing.png"

private data class PreviewCase(
    val label: String,
    val avatar: AvatarPreview,
    val expression: String,
    val catalog: Catalog,
)

@Composable
private fun AvatarRuntimeGallery(catalog: Catalog) {
    var page by remember { mutableIntStateOf(0) }
    var rotation by remember { mutableIntStateOf(0) }
    var switching by remember { mutableStateOf(false) }
    LaunchedEffect(switching) {
        if (switching) {
            repeat(18) { step ->
                page = step % FALLBACK_PAGE
                rotation = step % outfits.size
                delay(45)
            }
            page = 8
            rotation = 0
            switching = false
        }
    }
    val fallbacks = remember(catalog) {
        val outfitKey = catalog.items.single { it.id == "daejeon_hat" }.assetKey
        val missingOutfit = catalog.copy(assets = catalog.assets +
            (outfitKey to catalog.assets.getValue(outfitKey).copy(path = MISSING_PNG)))
        val faceKey = catalog.expressions.getValue("happy")
        val missingFace = catalog.copy(assets = catalog.assets +
            (faceKey to catalog.assets.getValue(faceKey).copy(path = MISSING_PNG)))
        val missingBody = catalog.copy(assets = catalog.assets - catalog.characters.single { it.id == "tori" }.assetKey)
        listOf(
            PreviewCase("unknown body", AvatarPreview("unknown", "daejeon_hat"), "idle", catalog),
            PreviewCase("unknown outfit", AvatarPreview("tori", "unknown"), "idle", catalog),
            PreviewCase("unknown expression", AvatarPreview("tori", null), "unknown", catalog),
            PreviewCase("missing body ref", AvatarPreview("tori", null), "idle", missingBody),
            PreviewCase("missing outfit PNG", AvatarPreview("tori", "daejeon_hat"), "idle", missingOutfit),
            PreviewCase("missing face PNG", AvatarPreview("tori", null), "happy", missingFace),
        )
    }
    val cases = if (page == FALLBACK_PAGE) fallbacks else {
        val body = bodies[page / expressions.size]
        val expression = expressions[page % expressions.size]
        outfits.indices.map { index ->
            val outfit = outfits[(index + rotation) % outfits.size]
            PreviewCase(outfit ?: "no outfit", AvatarPreview(body, outfit), expression, catalog)
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("B04-2A · Avatar runtime", style = MaterialTheme.typography.titleLarge)
        Text(if (page == FALLBACK_PAGE) "10/10 · fallback" else
            "${page + 1}/10 · ${bodies[page / expressions.size]} · ${expressions[page % expressions.size]}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { page-- }, enabled = !switching && page > 0) { Text("Prev") }
            Button(onClick = { page++ }, enabled = !switching && page < FALLBACK_PAGE) { Text("Next") }
            Button(onClick = { switching = true }, enabled = !switching) { Text("Switch fast") }
        }
        cases.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sample ->
                    Card(Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            AvatarView(sample.avatar, sample.catalog, sample.expression, Modifier.size(96.dp))
                            Text(sample.label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// Debug-only DTO adapter. The source of truth is the same Catalog bundled with the app.
private fun readPreviewCatalog(assets: AssetManager): Catalog {
    val root = assets.open("content/catalog.json").bufferedReader(Charsets.UTF_8).use {
        Json.parseToJsonElement(it.readText()).jsonObject
    }
    val rules = root.getValue("rules").jsonObject
    return Catalog(
        contentVersion = root.string("contentVersion"),
        description = root.string("description"),
        characters = root.objects("characters").map {
            CharacterDefinition(it.string("id"), it.string("name"), it.string("assetKey"))
        },
        expressions = root.getValue("expressions").jsonObject.mapValues { it.value.jsonPrimitive.content },
        assets = root.getValue("assets").jsonObject.mapValues { (_, value) ->
            val entry = value.jsonObject
            AssetDefinition(entry.string("path"), entry.string("kind"))
        },
        regions = root.objects("regions").map {
            RegionDefinition(it.string("id"), it.string("name"), it.string("theme"),
                it.getValue("latitude").jsonPrimitive.double, it.getValue("longitude").jsonPrimitive.double,
                it.strings("itemIds"))
        },
        items = root.objects("items").map {
            ItemDefinition(it.string("id"), it.string("regionId"), ItemType.valueOf(it.string("type")),
                it.string("name"), it.string("assetKey"))
        },
        rules = GameRules(rules.getValue("dailyGrowthCap").jsonPrimitive.int,
            rules.getValue("dailyGrantLimit").jsonPrimitive.int, rules.string("timeZone"),
            rules.strings("decorationSlotIds")),
    )
}

private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.objects(key: String) = getValue(key).jsonArray.map { it.jsonObject }
private fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }
