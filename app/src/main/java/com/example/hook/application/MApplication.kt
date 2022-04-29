package com.example.hook.application

import android.app.Application
import android.content.Context
import com.example.core.PluginManager
import com.example.hook.Utils
import com.example.reflection.FreeReflection
import top.canyie.pine.Pine
import top.canyie.pine.Pine.HookMode
import top.canyie.pine.PineConfig


class MApplication: Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Utils.extractAssets(base, "test.apk")
        Utils.extractAssets(base, "test2.apk")
        Pine.setHookMode(HookMode.INLINE)
        PluginManager.init(base!!)

        MApplication.Companion.mContext = base
        FreeReflection.unseal(base)
    }
    companion object{
        private var mContext: Context? = null
        fun getContext(): Context? = mContext

    }

}