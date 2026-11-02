package io.kailand.app

import android.content.res.AssetManager
import java.io.InputStream
import org.json.JSONObject

/** Root sources are packaged unchanged. Read from an IO dispatcher, not a Compose body. */
class CatalogAssets(private val assets: AssetManager) {
    fun readCatalogJson(): String = assets.open("content/catalog.json")
        .bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun readFixtureJson(name: String): String {
        require(name in setOf("scenarios", "ui_samples"))
        return assets.open("fixtures/$name.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun openCatalogImage(assetKey: String): InputStream {
        val path = JSONObject(readCatalogJson()).getJSONObject("assets")
            .getJSONObject(assetKey).getString("path")
        require(path.startsWith("assets/starter/") && path.endsWith(".png") && ".." !in path)
        return assets.open(path)
    }
}
