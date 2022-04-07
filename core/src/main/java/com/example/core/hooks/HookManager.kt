package com.example.core.hooks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.hookwrapper.HookUtil
import com.example.hookwrapper.MethodHook
import java.lang.reflect.Field
import java.lang.reflect.Method

class HookManager {
    private val hookComponents = mutableListOf<HookComponent>(ActivityHook(), ClassloaderHook(), ResourceHook())
//    private val activityThreadHandlerCallbackListener =
//        mutableMapOf<Int, MutableList<HookComponent>>()
    private val performLaunchActivityListener = mutableListOf<HookComponent>()

    fun doHook() {
        hookComponents.forEach { hookComponent ->
            hookComponent.doHook()
            hookComponent.onRegisterListener(this)
        }
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentActivityThreadField: Field =
            activityThreadClass.getDeclaredField("sCurrentActivityThread")
        currentActivityThreadField.isAccessible = true
        val currentActivityThread = currentActivityThreadField[null]
        val activityClientRecord = Class.forName("android.app.ActivityThread\$ActivityClientRecord")
        val performLaunchActivity: Method = currentActivityThread.javaClass.getDeclaredMethod(
            "performLaunchActivity", activityClientRecord, Intent::class.java
        )
//        val startActivityForResult = Activity::class.java.getDeclaredMethod(
//            "startActivityForResult",
//            Intent::class.java, Int::class.javaPrimitiveType, Bundle::class.java
//        )
        HookUtil.hook(performLaunchActivity,object : MethodHook(){
            override fun beforeCall(parameters: HookUtil.CallParameters) {
                performLaunchActivityListener.forEach {
                    it.onPerformLaunchActivity(parameters)
                }
            }
        })

    }

    fun registerPerformLaunchActivityListener(hookComponent: HookComponent) {
        performLaunchActivityListener.add(hookComponent)
    }
//    /**
//     * @param action is msg.what
//     */
//    fun registerActivityThreadHandlerCallbackListener(action: Int, hookComponent: HookComponent) {
//        if (activityThreadHandlerCallbackListener[action] == null) {
//            activityThreadHandlerCallbackListener[action] = mutableListOf(hookComponent)
//        } else {
//            activityThreadHandlerCallbackListener[action] =
//                activityThreadHandlerCallbackListener[action]!!.apply {
//                    add(hookComponent)
//                }
//        }
//    }

}