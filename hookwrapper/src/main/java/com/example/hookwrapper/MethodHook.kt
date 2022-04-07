package com.example.hookwrapper

import java.lang.reflect.Member

abstract class MethodHook {
    open fun beforeCall(parameters : HookUtil.CallParameters){}
    open fun afterCall(parameters : HookUtil.CallParameters){}

    class Hooked(private val hookRecord: HookUtil.HookRecord) {
        fun getTarget() = hookRecord.target
        fun getCallback() = hookRecord.getCallbacks()

//        fun unhook() {
//            //todo
//            HookUtil.getHookHandler().handleUnhook(hookRecord, this)
//        }


    }

}