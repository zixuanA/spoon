package com.example.core

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.lang.ref.WeakReference
import java.lang.reflect.Method

class PluginApplicationClient {
    lateinit var pluginApplication: Application

    fun callAttachBaseContext(c: Context?) {
        try {
            sAttachBaseContextMethod?.isAccessible = true // Protected 修饰
            sAttachBaseContextMethod?.invoke(pluginApplication, c)
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }

    fun callOnCreate() {
        pluginApplication.onCreate()
    }

    fun callOnLowMemory() {
        pluginApplication.onLowMemory()
    }

    fun callOnTrimMemory(level: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return
        }
        pluginApplication.onTrimMemory(level)
    }

    fun callOnConfigurationChanged(newConfig: Configuration) {
        pluginApplication.onConfigurationChanged(newConfig)
    }


    companion object{
        val activeClients= mutableListOf<WeakReference<PluginApplicationClient>>()
        private var sAttachBaseContextMethod: Method? = null
        private fun initMethods() {
            sAttachBaseContextMethod =
                Application::class.java.getDeclaredMethod("attach", Context::class.java)
            sAttachBaseContextMethod!!.isAccessible = true // Protected 修饰
        }
        fun notifyOnLowMemory() {
            for (pacw in activeClients) {
                val pac = pacw.get() ?: continue
                pac.callOnLowMemory()
            }
        }

        fun notifyOnTrimMemory(level: Int) {
            for (pacw in activeClients) {
                val pac = pacw.get() ?: continue
                pac.callOnTrimMemory(level)
            }
        }

        fun notifyOnConfigurationChanged(newConfig: Configuration?) {
            for (pacw in activeClients) {
                val pac = pacw.get() ?: continue
                pac.callOnConfigurationChanged(newConfig!!)
            }
        }

        fun create(plugin: Plugin):PluginApplicationClient {
            initMethods()
            val pluginApplicationClient = PluginApplicationClient()

            //插件有自定义application
            if (plugin.applicationInfo.className != null) {
                pluginApplicationClient.pluginApplication = plugin.classloader.loadClass(plugin.applicationInfo.className)
                    .getConstructor().newInstance() as Application

            } else {
                pluginApplicationClient.pluginApplication = Application()
            }
            activeClients.add(WeakReference<PluginApplicationClient>(pluginApplicationClient))
            return pluginApplicationClient
        }
    }

}