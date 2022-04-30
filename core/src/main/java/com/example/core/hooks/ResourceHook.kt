package com.example.core.hooks

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import com.example.core.PluginManager
import com.example.core.Utils
import com.example.core.classloader.CustomClassLoader
import com.example.hookwrapper.HookUtil
import java.io.File
import java.lang.reflect.Field

class ResourceHook : HookComponent() {
    override fun doHook() {
    }

    @SuppressLint("ResourceType")
    override fun onPerformLaunchActivity(parameters: HookUtil.CallParameters) {
        val activityClientRecord = Class.forName("android.app.ActivityThread\$ActivityClientRecord")

        parameters.args.forEach { arg ->
            if (activityClientRecord.isInstance(arg)) {

                //修改mResDir 加载插件apk的res
                val packageInfoField = activityClientRecord.getDeclaredField("packageInfo")
                packageInfoField.isAccessible = true
                val loadedApk = packageInfoField[arg]
                val mResDirField = loadedApk.javaClass.getDeclaredField("mResDir")
                mResDirField.isAccessible = true

                val mClassLoaderField = loadedApk.javaClass.getDeclaredField("mClassLoader")
                mClassLoaderField.isAccessible = true
                val classloader = mClassLoaderField.get(loadedApk) as ClassLoader
                if (classloader !is CustomClassLoader) {
                    //对宿主不执行修改res和theme的操作
                    return
                }
                val plugin = PluginManager.getPlugin(classloader)
                mResDirField.set(loadedApk, Utils.getApkFile(plugin.name, plugin.nameWithExtension).path)
//                //手动设置resources ⚠️插件中使用glide加载本地图片会出错，疑似glide不使用插件context
                val resourcesField = loadedApk.javaClass.getDeclaredField("mResources")
                resourcesField.isAccessible = true
                resourcesField.set(loadedApk, plugin.resources)

                //修改插件activity的theme
                val activityInfoField: Field = activityClientRecord.getDeclaredField("activityInfo")
                activityInfoField.isAccessible = true
                val activityInfo = activityInfoField.get(arg) as ActivityInfo
                val targetIntentField = activityClientRecord.getDeclaredField("intent")
                targetIntentField.isAccessible = true
                val targetIntent = targetIntentField.get(arg) as Intent
                val packageParser = Class.forName("android.content.pm.PackageParser").newInstance()
                val parsePackage = packageParser.javaClass.getDeclaredMethod(
                    "parsePackage",
                    File::class.java,
                    Int::class.javaPrimitiveType
                )
                val pkg = parsePackage.invoke(
                    packageParser,
                    Utils.getApkFile(plugin.name, plugin.nameWithExtension),
                    1
                )
                val activities =
                    pkg.javaClass.getDeclaredField("activities").get(pkg) as ArrayList<Any>
                val applicationInfo = pkg::class.java.getDeclaredField("applicationInfo").get(pkg) as ApplicationInfo
                for (j in activities.indices) {
                    val realInfo =
                        activities[j].javaClass.getDeclaredField("info")
                            .get(activities[j]) as ActivityInfo
                    val activityName = realInfo.name
                    if (targetIntent.getComponent()?.getClassName() == activityName) {
                        activityInfoField.set(arg, realInfo)
//                        activityInfo.theme = if(realInfo.theme != 0) realInfo.theme else applicationInfo.theme
                    }
                }
            }
        }
    }

    override fun onRegisterListener(hm: HookManager) {
        hm.registerPerformLaunchActivityListener(this)
    }
}