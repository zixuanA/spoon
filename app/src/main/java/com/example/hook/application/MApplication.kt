package com.example.hook.application

import android.app.Application
import android.content.Context
import com.example.hook.Utils
import com.example.reflection.FreeReflection
import top.canyie.pine.Pine
import top.canyie.pine.Pine.HookMode
import top.canyie.pine.PineConfig


class MApplication: Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Utils.extractAssets(base, "test.apk")

        MApplication.Companion.mContext = base
        FreeReflection.unseal(base)
        Pine.setHookMode(HookMode.INLINE)
        PineConfig.debug = true
    }
    companion object{
        var mContext: Context? = null
        fun getContext(): Context? = mContext

    }

}