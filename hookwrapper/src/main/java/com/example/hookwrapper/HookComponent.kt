package com.example.hookwrapper

import com.x.rehook.Rehook
import top.canyie.pine.Pine
import java.lang.reflect.Member
import java.lang.reflect.Method

abstract class HookComponent {
    abstract fun doHook(method: Member, methodHook: MethodHook)
}

object PineHookComponent : HookComponent() {
    override fun doHook(method: Member, methodHook: MethodHook) {
        Pine.hook(method, object : top.canyie.pine.callback.MethodHook() {
            override fun beforeCall(callFrame: Pine.CallFrame?) {
                callFrame ?: return
                methodHook.beforeCall(callFrame.toCallParameters())
            }

            override fun afterCall(callFrame: Pine.CallFrame?) {
                callFrame ?: return
                methodHook.afterCall(callFrame.toCallParameters())
            }
        })
    }
}

object ReHookComponent : HookComponent() {
    override fun doHook(method: Member, methodHook: MethodHook) {
//        Rehook.hookMethod()
        // todo 这里应该可以通过asm在编译期找到并创建调用需要用到的替换用Method对象
//        Method
    }
}