package com.example.core.hooks

import android.content.Intent
import android.content.pm.ActivityInfo
import com.example.core.CHANGE_CLASS_LOADER
import com.example.core.PLUGIN_NAME
import com.example.core.PluginManager
import com.example.core.classloader.CustomClassLoader
import com.example.core.classloader.CustomHostClassLoader
import com.example.hookwrapper.HookUtil
import dalvik.system.DexClassLoader
import java.lang.RuntimeException
import java.lang.reflect.Field

fun getClassLoader(activityClientRecord: Any):ClassLoader {
    val activityClientRecordField = Class.forName("android.app.ActivityThread\$ActivityClientRecord")
    if(!activityClientRecordField.isInstance(activityClientRecord)) throw RuntimeException("parameter is not ActivityClientRecord")
    //获取loaded apk
    val packageInfoField = activityClientRecordField.getDeclaredField("packageInfo")
    packageInfoField.isAccessible = true
    val loadedApk = packageInfoField.get(activityClientRecord)

    //修改插件classloader
    //对于一个插件只需要修改一次classloader就可以，系统根据包名来读取loadedapk的缓存
    val mClassLoaderField = loadedApk.javaClass.getDeclaredField("mClassLoader")
    mClassLoaderField.isAccessible = true
    return mClassLoaderField.get(loadedApk) as ClassLoader
}
class ClassloaderHook : HookComponent() {
    override fun doHook() {
    }

    override fun onPerformLaunchActivity(parameters: HookUtil.CallParameters) {
        val activityClientRecord = Class.forName("android.app.ActivityThread\$ActivityClientRecord")
        parameters.args.forEach { arg ->
            if (activityClientRecord.isInstance(arg)) {
                val targetIntentField = activityClientRecord.getDeclaredField("intent")
                        targetIntentField.isAccessible = true
                val targetIntent = targetIntentField.get(arg) as Intent

                //修改packageName
                // 根据 getPackageInfo 根据这个 包名获取 LoadedApk的信息; 因此这里我们需要手动填上, 从而能够命中缓存
                val activityInfoField: Field = activityClientRecord.getDeclaredField("activityInfo")
                activityInfoField.isAccessible = true
                val activityInfo = activityInfoField.get(arg) as ActivityInfo
                activityInfo.applicationInfo.packageName =
                    if (targetIntent.getPackage() == null) targetIntent.component?.packageName else targetIntent.getPackage()


                //获取loaded apk
                val packageInfoField = activityClientRecord.getDeclaredField("packageInfo")
                packageInfoField.isAccessible = true
                val loadedApk = packageInfoField.get(arg)

                //修改插件classloader
                //对于一个插件只需要修改一次classloader就可以，系统根据包名来读取loadedapk的缓存
                val mClassLoaderField = loadedApk.javaClass.getDeclaredField("mClassLoader")
                mClassLoaderField.isAccessible = true
                val mClassloader = mClassLoaderField.get(loadedApk)
                if (targetIntent.getBooleanExtra(CHANGE_CLASS_LOADER, false)) {
                    targetIntent.getStringExtra(PLUGIN_NAME)?.let {
                        mClassLoaderField.set(loadedApk, PluginManager.getPlugin(it).classloader)
                    }
                } else if(mClassloader !is CustomClassLoader || mClassloader !is CustomHostClassLoader) {
                    //创建宿主类的classloader，native search path 使用application info中的
//                    mClassLoaderField.set(loadedApk, PluginManager.getPlugin(it).classloader)
                }

            }
        }

    }

    override fun onRegisterListener(hm: HookManager) {
        hm.registerPerformLaunchActivityListener(this)
    }
}