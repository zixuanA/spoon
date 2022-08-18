package com.example.hook.application

import android.app.Application
import android.content.Context
import com.example.core.PluginManager
import com.example.hook.Utils
import com.example.reflection.FreeReflection


class MApplication: Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Utils.extractAssets(base, "test.apk")
        Utils.extractAssets(base, "test2.apk")
        Utils.extractAssets(base, "玩安卓.apk")

        PluginManager.init(base!!)

        MApplication.Companion.mContext = base
        FreeReflection.unseal(base)
        mContext = base
    }

    override fun onCreate() {
        super.onCreate()
        PluginManager.init(mContext!!)
    }

    companion object{
        private var mContext: Context? = null
        fun getContext(): Context? = mContext

    }

}