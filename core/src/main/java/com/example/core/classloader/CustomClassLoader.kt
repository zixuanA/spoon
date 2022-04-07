package com.example.core.classloader

import dalvik.system.DexClassLoader

class CustomClassLoader(
    dexPath: String?,
    optimizedDirectory: String?,
    librarySearchPath: String?,
    parent: ClassLoader?
) : DexClassLoader(
    dexPath,
    optimizedDirectory, librarySearchPath, parent
) {

}