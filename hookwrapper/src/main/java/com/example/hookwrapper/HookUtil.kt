package com.example.hookwrapper

import top.canyie.pine.Pine
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.HashSet

fun Pine.CallFrame.toCallParameters(): HookUtil.CallParameters {
    return HookUtil.CallParameters(this.method, thisObject, args,result, throwable)
}
class HookUtil {

    data class CallParameters(
        val method: Member,
        var thisObject: Any,
        var args: Array<Any>?,
        private var result: Any? = null,
        private val throwable: Throwable? = null,
//        /* package */
//        var returnEarly: Boolean = false,
//        private val hookRecord: HookRecord? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CallParameters

            if (method != other.method) return false
            if (thisObject != other.thisObject) return false
            if (!args.contentEquals(other.args)) return false
            if (result != other.result) return false
            if (throwable != other.throwable) return false
//            if (returnEarly != other.returnEarly) return false
//            if (hookRecord != other.hookRecord) return false

            return true
        }

        override fun hashCode(): Int {
            var result1 = method?.hashCode() ?: 0
            result1 = 31 * result1 + (thisObject?.hashCode() ?: 0)
            result1 = 31 * result1 + args.contentHashCode()
            result1 = 31 * result1 + (result?.hashCode() ?: 0)
            result1 = 31 * result1 + (throwable?.hashCode() ?: 0)
//            result1 = 31 * result1 + returnEarly.hashCode()
//            result1 = 31 * result1 + (hookRecord?.hashCode() ?: 0)
            return result1
        }
    }


    class HookRecord(val target: Member, val artMethod: Long) {
        var backup: Method? = null
        var isStatic = false
        var paramNumber = 0
        var paramTypes: Array<Class<*>> = emptyArray()

        private val callbacks: MutableSet<MethodHook> = HashSet<MethodHook>()

        @Synchronized
        fun addCallback(callback: MethodHook) {
            callbacks.add(callback)
        }

        @Synchronized
        fun removeCallback(callback: MethodHook) {
            callbacks.remove(callback)
        }

        @Synchronized
        fun emptyCallbacks(): Boolean {
            return callbacks.isEmpty()
        }

        @Synchronized
        fun getCallbacks(): Array<MethodHook> {
            return callbacks.toTypedArray()
        }

//        val isPending: Boolean
//            get() = backup == null

    }
    companion object{
        public fun hook(method: Member, methodHook: MethodHook) {
            Pine.hook(method, object : top.canyie.pine.callback.MethodHook(){
                override fun beforeCall(callFrame: Pine.CallFrame?) {
                    methodHook.beforeCall(callFrame?.toCallParameters())
                }

                override fun afterCall(callFrame: Pine.CallFrame?) {
                    methodHook.afterCall(callFrame?.toCallParameters())
                }
            })
        }
    }
}