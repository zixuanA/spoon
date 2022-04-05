package com.example.core.hooks

abstract class HookComponent:ActivityThreadHandlerCallbackListener {
    abstract fun doHook()
}