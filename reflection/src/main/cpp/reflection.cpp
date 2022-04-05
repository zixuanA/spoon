#include <jni.h>

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("reflection");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("reflection")
//      }
//    }
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_reflection_FreeReflection_unsealNative(JNIEnv *env, jclass clazz,
                                                        jint target_sdk_version) {
    // TODO: implement unsealNative()
}