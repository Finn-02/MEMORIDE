package io.kailand.app.journey

import android.app.Activity
import android.content.pm.PackageManager

/** Verified Pleos system navigation package; no arbitrary implicit handler or overlay. */
fun createNavigationLauncher(activity: Activity): NavigationLauncher = NavigationLauncher {
    if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) null
    else activity.packageManager.getLaunchIntentForPackage("ai.umos.maps.android.navigation.app")?.let { intent ->
        { activity.startActivity(intent) }
    }
}
