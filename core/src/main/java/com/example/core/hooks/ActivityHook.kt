package com.example.core.hooks

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.example.core.StubActivity
import com.example.hookwrapper.HookUtil
import com.example.hookwrapper.MethodHook
import java.lang.reflect.Field

const val EXTRA_TARGET_INTENT = "extra_target_intent"

class ActivityHook : HookComponent() {

    override fun doHook() {
        // 暂时只兼容Android 10
        if (Build.VERSION.SDK_INT >= 29) {
            val startActivityForResult = Activity::class.java.getDeclaredMethod(
                "startActivityForResult",
                Intent::class.java, Int::class.javaPrimitiveType, Bundle::class.java
            )
            startActivityForResult.isAccessible = true
            HookUtil.Companion.hook(
                startActivityForResult,
                object : MethodHook() {
                    override fun beforeCall(parameters: HookUtil.CallParameters) {
                        val args = parameters.args
                        val raw: Intent
                        var index = 0
                        for (i in 0 until (args.size ?: 0)) {
                            if (args[i] is Intent) {
                                index = i
                                break
                            }
                        }
                        raw = args[index] as Intent
                        val newIntent = Intent()

                        // 替身Activity的包名, 也就是我们自己的包名
                        val stubPackage = "com.example.activityhook"

                        // 这里我们把启动的Activity临时替换为 StubActivity
                        val componentName = ComponentName(
                            stubPackage,
                            StubActivity::class.java.getName()
                        )
                        newIntent.component = componentName

                        // 把我们原始要启动的TargetActivity先存起来
                        newIntent.putExtra(EXTRA_TARGET_INTENT, raw)

                        // 替换掉Intent, 达到欺骗AMS的目的
                        args[index] = newIntent
                    }

                }

//                    override fun afterCall(parameters: CallParameters?) {}
            )
            return
        }
//        var gDefaultField: Field? = null
//        gDefaultField = if (Build.VERSION.SDK_INT >= 26) {
//            val activityManager = Class.forName("android.app.ActivityManager")
//            activityManager.getDeclaredField("IActivityManagerSingleton")
//        } else {
//            val activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative")
//            activityManagerNativeClass.getDeclaredField("gDefault")
//        }
//        gDefaultField.isAccessible = true
//        gDefaultField.isAccessible = true
//
//        val gDefault = gDefaultField[null]
//
//        // gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
//
//        // gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
//        val singleton = Class.forName("android.util.Singleton")
//        val mInstanceField = singleton.getDeclaredField("mInstance")
//        mInstanceField.isAccessible = true
//
//        // ActivityManagerNative 的gDefault对象里面原始的 IActivityManager对象
//
//        // ActivityManagerNative 的gDefault对象里面原始的 IActivityManager对象
//        val rawIActivityManager = mInstanceField[gDefault]
//
//        // 创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活
//
//        // 创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活
//        val iActivityManagerInterface = Class.forName("android.app.IActivityManager")
//        val proxy = Proxy.newProxyInstance(
//            Thread.currentThread().contextClassLoader,
//            arrayOf(iActivityManagerInterface),
//            IActivityManagerHandler(rawIActivityManager)
//        )
//        mInstanceField[gDefault] = proxy
    }

    override fun onPerformLaunchActivity(parameters: HookUtil.CallParameters) {
        var old: Intent
        var targetIntent: Intent?
        val activityClientRecord = Class.forName("android.app.ActivityThread\$ActivityClientRecord")

        parameters.args.forEach { arg ->
            if (activityClientRecord.isInstance(arg)) {

                //修改intent
                val intentField: Field = activityClientRecord.getDeclaredField("intent")
                intentField.isAccessible = true
                old = intentField[arg] as Intent
                targetIntent = old.getParcelableExtra(EXTRA_TARGET_INTENT)
                if (targetIntent == null) return
                intentField.set(arg, targetIntent)
            }
        }
    }

    override fun onRegisterListener(hm: HookManager) {
        hm.registerPerformLaunchActivityListener(this)
    }
}