package com.x.rehook

import java.lang.reflect.Field
import java.lang.reflect.Method

object Rehook {
    private val nativePeer by lazy {
        Thread.currentThread().let {
            getField(it.javaClass, "nativePeer").apply { isAccessible = true }.getLong(it)
//            it.javaClass.superclass.getField("nativePeer").getLong(it)
        }
    }
    private fun getField(clazz: Class<*>, fieldName: String): Field {
        var clazz = clazz
        try {
            return clazz.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            while (true) {
                clazz = clazz.superclass
                if (clazz == null || clazz == Any::class.java) break
                try {
                    return clazz.getDeclaredField(fieldName)
                } catch (ignored: NoSuchFieldException) {
                }
            }
            throw e
        }
    }

    fun hookMethod(originMethod: Method, targetMethod: Method) {
        makeSureMethodComplied(originMethod)
        makeSureMethodComplied(targetMethod)

        nativeHookMethod(originMethod, targetMethod)
    }

    private fun makeSureMethodComplied(method: Method) {
        compileMethod(method, nativePeer)
    }


    private external fun compileMethod(method: Method, self: Long): Boolean
    private external fun nativeHookMethod(originMethod: Method, targetMethod: Method)

    init {
        System.loadLibrary("hook")
    }
}