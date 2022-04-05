package com.example.core

import android.content.Context
import android.content.Intent
import java.io.File

object PluginManager {

    private val plugins = mutableSetOf<String>()
    lateinit var pluginDir: String
    init {

    }
    fun setUpPluginDir(dir: String) {
        pluginDir = dir
    }

    fun install(path: String) {

        val source = File(path)
        val target = File(pluginDir, source.name)
        source.copyTo(target, true)
        plugins.add(path)
    }

    fun unInstall(name: String) {
        if (plugins.contains(name)) {
            plugins.remove(name)
            File(pluginDir, name).delete()
        }
    }

    private fun loadInstalledPlugins() {
        File(pluginDir).listFiles()?.forEach {
            plugins.add(it.name)
        }
    }

    fun runPlugin(context: Context, name: String) {
        context.startActivity(Intent().apply {

        })
    }

}