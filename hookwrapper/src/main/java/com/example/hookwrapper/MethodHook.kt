package com.example.hookwrapper

import java.lang.reflect.Member

abstract class MethodHook {
    open fun beforeCall(parameters : HookUtil.CallParameters){}
    open fun afterCall(parameters : HookUtil.CallParameters){}

}