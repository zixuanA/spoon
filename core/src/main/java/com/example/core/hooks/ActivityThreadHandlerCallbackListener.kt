package com.example.core.hooks

import android.os.Message
import com.example.hookwrapper.HookUtil

interface ActivityThreadHandlerCallbackListener {
//    fun onActivityThreadHandler(msg: Message)
    fun onPerformLaunchActivity(parameters : HookUtil.CallParameters)
    // 提前注册listener
    fun onRegisterListener(hm: HookManager)
}