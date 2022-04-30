package com.example.core.hooks

import android.app.Application
import com.example.core.Plugin
import com.example.core.PluginApplicationClient
import com.example.core.PluginManager
import com.example.hookwrapper.HookUtil
import java.lang.RuntimeException

class ApplicationHook: HookComponent() {
    override fun doHook() {
    }

    override fun onPerformLaunchActivity(parameters: HookUtil.CallParameters) {
        val activityClientRecord = Class.forName("android.app.ActivityThread\$ActivityClientRecord")
        parameters.args.forEach { arg ->
            if (activityClientRecord.isInstance(arg)) {
                val packageInfoField = arg::class.java.getDeclaredField("packageInfo")
                packageInfoField.isAccessible = true
                val packageInfo = packageInfoField.get(arg)
                var application:Application? = null
                var plugin: Plugin? = null
                try {
                    plugin = PluginManager.getPlugin(getClassLoader(arg))
                    application =
                        plugin.pluginApplicationClient.pluginApplication
                } catch (e: RuntimeException) {
                    return
                }

                val applicationField = packageInfo::class.java.getDeclaredField("mApplication")
                applicationField.isAccessible = true
                applicationField.set(packageInfo, application)

                plugin.pluginApplicationClient.callAttachBaseContext(plugin.context)
                plugin.pluginApplicationClient.callOnCreate()

            }
        }

    }

    override fun onRegisterListener(hm: HookManager) {
        hm.registerPerformLaunchActivityListener(this)
    }
}