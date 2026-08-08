package com.amiro.vpn.core

import android.content.pm.PackageManager

class TunnelManager(private val packageManager: PackageManager) {

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val isAllowed: Boolean
    )

    private val blockedApps = mutableSetOf<String>()

    fun getInstalledApps(): List<AppInfo> {
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != "com.amiro.vpn" }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    appName = packageManager.getApplicationLabel(app).toString(),
                    isAllowed = app.packageName !in blockedApps
                )
            }
            .sortedBy { it.appName }
    }

    fun toggleApp(packageName: String, allow: Boolean) {
        if (allow) blockedApps.remove(packageName)
        else blockedApps.add(packageName)
    }

    fun shouldTunnelApp(packageName: String) = packageName !in blockedApps
}
