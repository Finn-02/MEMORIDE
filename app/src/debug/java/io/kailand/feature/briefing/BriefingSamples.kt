package io.kailand.feature.briefing

import android.app.UiModeManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.kailand.core.contract.*
import io.kailand.core.platform.developmentGate
import io.kailand.core.ui.MemorideTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Debug/Preview only. Read the packaged original files; never inject these into GameService. */
internal data class BriefingSample(
    val catalog: Catalog,
    val avatar: AvatarPreview,
    val gate: InteractionGate,
    val receipt: ArrivalReceipt?,
)

internal fun readBriefingSample(catalogJson: String, fixtureJson: String, id: String): BriefingSample {
    val c = Json.parseToJsonElement(catalogJson).jsonObject
    val s = Json.parseToJsonElement(fixtureJson).jsonObject.array("samples").map { it.jsonObject }
        .first { it.string("id") == id }
    val g = s.getValue("game").jsonObject
    val a = g.getValue("avatar").jsonObject
    val gate = s.getValue("gate").jsonObject
    val catalog = Catalog(
        c.string("contentVersion"), c.string("description"),
        c.array("characters").map { it.jsonObject.let { x -> CharacterDefinition(x.string("id"), x.string("name"), x.string("assetKey")) } },
        c.getValue("expressions").jsonObject.mapValues { it.value.jsonPrimitive.content },
        c.getValue("assets").jsonObject.mapValues { (_, value) ->
            value.jsonObject.let { AssetDefinition(it.string("path"), it.string("kind")) }
        },
        c.array("regions").map { value -> value.jsonObject.let {
            RegionDefinition(it.string("id"), it.string("name"), it.string("theme"),
                it.getValue("latitude").jsonPrimitive.double, it.getValue("longitude").jsonPrimitive.double, it.strings("itemIds"))
        } },
        c.array("items").map { value -> value.jsonObject.let {
            ItemDefinition(it.string("id"), it.string("regionId"), ItemType.valueOf(it.string("type")), it.string("name"), it.string("assetKey"))
        } },
        c.getValue("rules").jsonObject.let {
            GameRules(it.int("dailyGrowthCap"), it.int("dailyGrantLimit"), it.string("timeZone"), it.strings("decorationSlotIds"))
        },
    )
    val receipt = g["pendingReceipt"]?.takeUnless { it is JsonNull }?.jsonObject?.let {
        ArrivalReceipt(
            it.string("journeyId"), DataSource.valueOf(it.string("source")), JourneyKind.valueOf(it.string("kind")),
            it.nullableString("regionId"), it.getValue("completedAtEpochMs").jsonPrimitive.long,
            it["elapsedSeconds"]?.jsonPrimitive?.longOrNull, it["distanceMeters"]?.jsonPrimitive?.doubleOrNull,
            it.int("growthBefore"), it.int("growthAfter"), it.nullableString("discoveredRegionId"), it.strings("grantedItemIds"),
        )
    }
    return BriefingSample(catalog, AvatarPreview(a.string("characterId"), a.nullableString("outfitItemId")),
        InteractionGate(GateState.valueOf(gate.string("state")), gate.nullableString("reason")), receipt)
}

private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.nullableString(key: String) = get(key)?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String) = getValue(key).jsonPrimitive.int
private fun JsonObject.array(key: String) = getValue(key).jsonArray
private fun JsonObject.strings(key: String) = array(key).map { it.jsonPrimitive.content }

/** Run a Preview on a generic emulator. No app entry point or live state is changed. */
@Composable
internal fun BriefingSampleHost(sampleId: String, variant: String = "") {
    val context = LocalContext.current
    val assets = context.assets
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val uiMode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    val sampleGate = developmentGate(
        debugBuild = true, developmentEntry = true,
        automotiveDevice = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE),
        carUiMode = uiMode == Configuration.UI_MODE_TYPE_CAR ||
            context.getSystemService(UiModeManager::class.java)?.currentModeType == Configuration.UI_MODE_TYPE_CAR,
        foreground = resumed || LocalInspectionMode.current,
    )
    var sample by remember(sampleId, variant) { mutableStateOf<BriefingSample?>(null) }
    var loadError by remember(sampleId, variant) { mutableStateOf(false) }
    var callbackStatus by remember(sampleId, variant) { mutableStateOf("") }
    var attempts by remember(sampleId, variant) { mutableStateOf(0) }
    LaunchedEffect(assets, sampleId, variant) {
        try {
            sample = withContext(Dispatchers.IO) {
                val catalog = assets.open("content/catalog.json").bufferedReader().use { it.readText() }
                val fixtures = assets.open("fixtures/ui_samples.json").bufferedReader().use { it.readText() }
                val original = readBriefingSample(catalog, fixtures, sampleId)
                when (variant) {
                    "unknown" -> original.copy(gate = InteractionGate(GateState.UNKNOWN))
                    "missing_ids" -> original.copy(
                        avatar = AvatarPreview("missing", "missing"),
                        receipt = original.receipt?.copy(regionId = "missing", discoveredRegionId = "missing", grantedItemIds = listOf("missing")),
                    )
                    "revisit" -> original.copy(receipt = original.receipt?.copy(discoveredRegionId = null, grantedItemIds = emptyList()))
                    else -> original
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = true
        }
    }
    val callback: suspend () -> MutationResult = {
        // Give manual device checks time to observe disabled controls and try competing inputs.
        delay(2500)
        attempts++
        if (variant == "retry" && attempts == 1) {
            MutationResult.StorageFailure("Preview only")
        } else {
            callbackStatus = "샘플 콜백 성공 · 실제 저장/이동 없음"
            MutationResult.Success
        }
    }
    MemorideTheme {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("P01 개발 샘플 · 실제 저장/이동 없음", Modifier.padding(12.dp), style = MaterialTheme.typography.labelLarge)
            if (callbackStatus.isNotEmpty()) Text(callbackStatus, Modifier.padding(horizontal = 12.dp))
            val loaded = sample
            val receipt = loaded?.receipt
            when {
                loadError -> Text("샘플을 불러오지 못했어요.")
                loaded == null -> Text("샘플을 불러오고 있어요…")
                receipt == null -> Text("아직 도착 기록이 없어요.", Modifier.padding(24.dp))
                else -> BriefingScreen(receipt, loaded.catalog, loaded.avatar,
                    if (sampleGate.canInteract) loaded.gate else sampleGate,
                    callback, callback, Modifier.weight(1f))
            }
        }
    }
}

@Preview(name = "P01 Regional", showBackground = true, widthDp = 480, heightDp = 960)
@Composable
internal fun RegionalBriefingPreview() = BriefingSampleHost("regional")

@Preview(name = "P01 Daily", showBackground = true, widthDp = 480, heightDp = 960)
@Composable
internal fun DailyBriefingPreview() = BriefingSampleHost("daily")

@Preview(name = "P01 Missing metrics", showBackground = true)
@Composable
internal fun MissingMetricsBriefingPreview() = BriefingSampleHost("missing_metrics")

@Preview(name = "P01 Restricted", showBackground = true)
@Composable
internal fun RestrictedBriefingPreview() = BriefingSampleHost("restricted_pending")

@Preview(name = "P01 Fresh", showBackground = true)
@Composable
internal fun FreshBriefingPreview() = BriefingSampleHost("fresh")

@Preview(name = "P01 Unknown", showBackground = true)
@Composable
internal fun UnknownBriefingPreview() = BriefingSampleHost("regional", "unknown")

@Preview(name = "P01 Revisit", showBackground = true)
@Composable
internal fun RevisitBriefingPreview() = BriefingSampleHost("regional", "revisit")

@Preview(name = "P01 Missing IDs", showBackground = true)
@Composable
internal fun MissingIdsBriefingPreview() = BriefingSampleHost("regional", "missing_ids")

@Preview(name = "P01 Failure and retry", showBackground = true, widthDp = 480, heightDp = 960)
@Composable
internal fun RetryBriefingPreview() = BriefingSampleHost("regional", "retry")
