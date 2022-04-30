package com.example.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.AssetManager
import android.content.res.Resources

data class Plugin(
    val name: String,
    val nameWithExtension: String,
    val classloader: ClassLoader,
    val mainActivityName: String,
    val applicationInfo: ApplicationInfo,
    val assetManager: AssetManager
) {
    var resources: Resources? = null

    val pluginApplicationClient:PluginApplicationClient by lazy {
        PluginApplicationClient.create(this)
    }

    //todo delete it
    public var context: Context? = null

}
