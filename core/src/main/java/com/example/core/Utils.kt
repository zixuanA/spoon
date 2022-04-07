package com.example.core

import android.content.Context
import java.io.Closeable
import java.io.File

object Utils {
    private lateinit var sBaseDir: File

    fun init(context: Context) {
        sBaseDir = context.getFileStreamPath("plugin")
        enforceDirExists(sBaseDir)
    }

    /**
     * 待加载插件经过opt优化之后存放odex得路径
     */
    fun getPluginOptDexDir(packageName: String): File {
        return enforceDirExists(
            File(
                getPluginBaseDirInternal(
                    packageName
                ), "odex"
            )
        )
    }

    /**
     * 插件得lib库路径
     */
    fun getPluginLibDir(packageName: String): File {
        return enforceDirExists(
            File(
                getPluginBaseDirInternal(
                    packageName
                ), "lib"
            )
        )
    }

    // --------------------------------------------------------------------------
    private fun closeSilently(closeable: Closeable?) {
        if (closeable == null) {
            return
        }
        try {
            closeable.close()
        } catch (e: Throwable) {
            // ignore
        }
    }


    // 需要加载得插件得基本目录 /data/data/<package>/files/plugin/
    private fun getPluginBaseDirInternal(packageName: String): File {
        return enforceDirExists(
            File(
                sBaseDir,
                packageName
            )
        )
    }
    fun getPluginBaseDir(packageName: String): File {
        return getPluginBaseDirInternal(packageName)
    }
    fun getApkFile(packageName: String, apkName: String): File {
        return enforceDirExists(File(getPluginBaseDirInternal(packageName), apkName))
    }

    fun getPluginBasicDir(): File {
        return sBaseDir
    }

    fun getPluginApkDir(packageName: String): File {
        return enforceDirExists(File(getPluginBaseDirInternal(packageName), "apk"))
    }

    @Synchronized
    private fun enforceDirExists(sBaseDir: File): File {
        if (!sBaseDir.exists()) {
            val ret = sBaseDir.mkdir()
            if (!ret) {
                throw RuntimeException("create dir " + sBaseDir + "failed")
            }
        }
        return sBaseDir
    }
}