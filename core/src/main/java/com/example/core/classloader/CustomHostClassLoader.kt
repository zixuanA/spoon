package com.example.core.classloader

import dalvik.system.DexClassLoader

//optimizedDirectory this parameter is deprecated and has no effect since API level 26.
class CustomHostClassLoader(
    dexPath: String?, optimizedDirectory: String?,
    librarySearchPath: String?, parent: ClassLoader?
) : DexClassLoader(
    dexPath,
    optimizedDirectory, librarySearchPath, parent
) {
}