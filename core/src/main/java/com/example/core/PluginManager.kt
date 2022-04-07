package com.example.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.core.classloader.CustomClassLoader
import com.example.core.hooks.HookManager
import java.io.File
import java.lang.RuntimeException
import java.util.ArrayList

const val CHANGE_CLASS_LOADER = "flag_change_class_loader"
const val PLUGIN_NAME = "flag_plugin_name"
object PluginManager {

    private val plugins = mutableMapOf<String, Plugin>()
//    private lateinit var pluginBaseDir: String
    private val hookManager = HookManager()

    fun init(context: Context) {
        Utils.init(context)
        hookManager.doHook()
    }

//    fun setUpPluginDir(dir: String) {
//        pluginBaseDir = dir
//    }

    fun install(path: String) {
        val source = File(path)
        val pluginName = source.nameWithoutExtension
        val pluginNameWithExtension = source.name
        val target = Utils.getApkFile(pluginName, pluginNameWithExtension)
        if (target.exists()) {
            target.delete()
        }
        source.copyTo(target, true)
        plugins[pluginName] = createPlugin(pluginName, pluginNameWithExtension)

    }

    fun unInstall(name: String) {
        if (plugins.contains(name)) {
            plugins.remove(name)
            Utils.getPluginBaseDir(name).delete()
        }
    }

    private fun createPlugin(pluginName: String, pluginNameWithExtension: String): Plugin {

        val packageParser = Class.forName("android.content.pm.PackageParser").newInstance()
        val parsePackage = packageParser.javaClass.getDeclaredMethod(
            "parsePackage",
            File::class.java,
            Int::class.javaPrimitiveType
        )
        val pkg = parsePackage.invoke(packageParser, Utils.getApkFile(pluginName, pluginNameWithExtension), 1)

        val mainActivityName = getPluginMainActivity(pkg)

        //提前创建classloader
        val odexPath: String = Utils.getPluginOptDexDir(pluginName).path
        val libDir: String = Utils.getPluginLibDir(pluginName).path

        val classLoader: ClassLoader = CustomClassLoader(
            Utils.getApkFile(pluginName, pluginNameWithExtension).path,
            odexPath,
            libDir,
            ClassLoader.getSystemClassLoader()
        )

        return Plugin(pluginName, pluginNameWithExtension, classLoader, mainActivityName)
    }
    private fun getPluginMainActivity(pkg: Any):String {
        try {
            val activities = pkg::class.java.getDeclaredField("activities").get(pkg) as ArrayList<*>
            activities.forEach { activity->
//                val info = activity::class.java.getDeclaredField("info").get(activity)
                val className = activity::class.java.getField("className").get(activity) as String
                val intents = activity::class.java.getField("intents").get(activity) as ArrayList<*>
                intents.forEach { intent->
//                    val fields = intent::class.java.fields
//                    val superfie = intent::class.java.superclass.superclass.declaredFields
                    val mActionsField = intent::class.java.superclass.superclass.getDeclaredField("mActions")
                    mActionsField.isAccessible = true
                    val mActions = mActionsField.get(intent) as ArrayList<String>
//                    val mActions = intent::class.java.getField("mActions").get(intent) as ArrayList<*>
//                    val mActions = intent::class.java.getField("mActions").get(intent) as ArrayList<*>
                    mActions.forEach {
                        if (it == Intent.ACTION_MAIN)
                            return className
                    }
                }
                throw RuntimeException("$className has`t a main activity")
            }
            throw RuntimeException("this plugin has`t a main activity")
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
            throw RuntimeException("can`t find fields")
        }

    }

    private fun loadInstalledPlugins() {
        Utils.getPluginBasicDir().listFiles()?.forEach {
            plugins[it.nameWithoutExtension] = createPlugin(it.nameWithoutExtension, it.name)
        }
    }

    fun runPlugin(context: Context, name: String) {
        runPlugin(context, plugins[name]?:throw RuntimeException("uninstalled plugin $name"))
    }

    fun runPlugin(context: Context, plugin: Plugin) {
        context.startActivity(Intent().apply {
            component = ComponentName(context, plugin.mainActivityName)
            putExtra(CHANGE_CLASS_LOADER, true)
            putExtra(PLUGIN_NAME, plugin.name)
        })
    }

    fun getPlugin(name:String):Plugin {
        plugins.filter {
            it.key == name
        }.firstNotNullOfOrNull { return it.value}
        throw RuntimeException("can`t find plugin $name")
    }

    fun getPlugin(classLoader: ClassLoader):Plugin {
        plugins.filter { it.value.classloader === classLoader }.firstNotNullOfOrNull {
            return it.value
        }
        throw RuntimeException("can`t find plugin through ${classLoader.javaClass.name}")
    }

}