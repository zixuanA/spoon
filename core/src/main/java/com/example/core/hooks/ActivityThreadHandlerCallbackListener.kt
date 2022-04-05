package com.example.core.hooks

import android.os.Message

interface ActivityThreadHandlerCallbackListener {
    fun onActivityThreadHandler(msg: Message)
    fun registerListener(hm: HookManager)
}