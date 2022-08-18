#include <jni.h>
#include <dlfcn.h>
#include "fake_dlfcn.h"
#include "art.h"
#include <jni.h>
#include <jni.h>
#include <jni.h>
#include <jni.h>
#include <jni.h>
#include <jni.h>
#include <jni.h>


// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("hook");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("hook")
//      }
//    }
struct Offset {
    int ART_QUICK_CODE_OFFSET = 0;
};
#undef NDEBUG
#ifdef NDEBUG
#define LOGV(...)  ((void)__android_log_print(ANDROID_LOG_INFO, "epic.Native", __VA_ARGS__))
#else
#define LOGV(...)
#endif
static int api_level;
jobject (*addWeakGloablReference)(JavaVM *, void *, void *) = nullptr;

void* (*jit_load_)(bool*) = nullptr;
bool (*jit_compile_method_)(void*, void*, void*, bool) = nullptr;
void* jit_compiler_handle_ = nullptr;
void* (*JitCodeCache_GetCurrentRegion)(void*) = nullptr;

class ScopedSuspendAll {};

void (*suspendAll)(ScopedSuspendAll*, char*) = nullptr;
void (*resumeAll)(ScopedSuspendAll*) = nullptr;

class ScopedJitSuspend {};
void (*startJit)(ScopedJitSuspend*) = nullptr;
void (*stopJit)(ScopedJitSuspend*) = nullptr;

Offset offset;


typedef bool (*JIT_COMPILE_METHOD1)(void *, void *, void *, bool);
typedef bool (*JIT_COMPILE_METHOD2)(void *, void *, void *, bool, bool); // Android Q
typedef bool (*JIT_COMPILE_METHOD3)(void *, void *, void *, void *, bool, bool); // Android R
typedef bool (*JIT_COMPILE_METHOD4)(void *, void *, void *, void *, int); // Android S

void* (*JniIdManager_DecodeMethodId_)(void*, jlong) = nullptr;
void* (*ClassLinker_MakeInitializedClassesVisiblyInitialized_)(void*, void*, bool) = nullptr;

void initOffset(int apiLevel);

void init(JNIEnv *env) {
    char api_level_str[5];
    __system_property_get("ro.build.version.sdk", api_level_str);
    api_level = atoi(api_level_str);

    ArtHelper::init(env, api_level);

    if (api_level < 23) {
        // Android L, art::JavaVMExt::AddWeakGlobalReference(art::Thread*, art::mirror::Object*)
        void *handle = dlopen("libart.so", RTLD_LAZY | RTLD_GLOBAL);
        addWeakGloablReference = (jobject (*)(JavaVM *, void *, void *)) dlsym(handle,
                                                                               "_ZN3art9JavaVMExt22AddWeakGlobalReferenceEPNS_6ThreadEPNS_6mirror6ObjectE");
    } else if (api_level < 24) {
        // Android M, art::JavaVMExt::AddWeakGlobalRef(art::Thread*, art::mirror::Object*)
        void *handle = dlopen("libart.so", RTLD_LAZY | RTLD_GLOBAL);
        addWeakGloablReference = (jobject (*)(JavaVM *, void *, void *)) dlsym(handle,
                                                                               "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadEPNS_6mirror6ObjectE");
    } else {
        // Android N and O, Google disallow us use dlsym;
        void *handle = dlopen_ex("libart.so", RTLD_NOW);
        void *jit_lib = dlopen_ex("libart-compiler.so", RTLD_NOW);
        LOGV("fake dlopen install: %p", handle);
        const char *addWeakGloablReferenceSymbol = api_level <= 25
                                                   ? "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadEPNS_6mirror6ObjectE"
                                                   : "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE";
        addWeakGloablReference = (jobject (*)(JavaVM *, void *, void *)) dlsym_ex(handle, addWeakGloablReferenceSymbol);

        jit_compile_method_ = (bool (*)(void *, void *, void *, bool)) dlsym_ex(jit_lib, "jit_compile_method");
        jit_load_ = reinterpret_cast<void* (*)(bool*)>(dlsym_ex(jit_lib, "jit_load"));
        bool generate_debug_info = false;
        jit_compiler_handle_ = (jit_load_)(&generate_debug_info);
        LOGV("jit compile_method: %p", jit_compile_method_);

        suspendAll = reinterpret_cast<void (*)(ScopedSuspendAll*, char*)>(dlsym_ex(handle, "_ZN3art16ScopedSuspendAllC1EPKcb"));
        resumeAll = reinterpret_cast<void (*)(ScopedSuspendAll*)>(dlsym_ex(handle, "_ZN3art16ScopedSuspendAllD1Ev"));

        if (api_level >= 30) {
            // Android R would not directly return ArtMethod address but an internal id
            ClassLinker_MakeInitializedClassesVisiblyInitialized_ = reinterpret_cast<void* (*)(void*, void*, bool)>(dlsym_ex(handle, "_ZN3art11ClassLinker40MakeInitializedClassesVisiblyInitializedEPNS_6ThreadEb"));
            JniIdManager_DecodeMethodId_ = reinterpret_cast<void* (*)(void*, jlong)>(dlsym_ex(handle, "_ZN3art3jni12JniIdManager14DecodeMethodIdEP10_jmethodID"));
            if (api_level >= 31) {
                // Android S CompileMethod accepts a CompilationKind enum instead of two booleans
                // source: https://android.googlesource.com/platform/art/+/refs/heads/android12-release/compiler/jit/jit_compiler.cc
                jit_compile_method_ = (bool (*)(void *, void *, void *, bool)) dlsym_ex(jit_lib, "_ZN3art3jit11JitCompiler13CompileMethodEPNS_6ThreadEPNS0_15JitMemoryRegionEPNS_9ArtMethodENS_15CompilationKindE");
            } else {
                jit_compile_method_ = (bool (*)(void *, void *, void *, bool)) dlsym_ex(jit_lib, "_ZN3art3jit11JitCompiler13CompileMethodEPNS_6ThreadEPNS0_15JitMemoryRegionEPNS_9ArtMethodEbb");
            }
            JitCodeCache_GetCurrentRegion = (void* (*)(void*)) dlsym_ex(handle, "_ZN3art3jit12JitCodeCache16GetCurrentRegionEv");
        }
        // Disable this now.
        // startJit = reinterpret_cast<void(*)(ScopedJitSuspend*)>(dlsym_ex(handle, "_ZN3art3jit16ScopedJitSuspendD1Ev"));
        // stopJit = reinterpret_cast<void(*)(ScopedJitSuspend*)>(dlsym_ex(handle, "_ZN3art3jit16ScopedJitSuspendC1Ev"));

        // DisableMovingGc = reinterpret_cast<void(*)(void*)>(dlsym_ex(handle, "_ZN3art2gc4Heap15DisableMovingGcEv"));
    }
    initOffset(api_level);
}

void initOffset(int apiLevel) {
    switch (apiLevel) {
        case 32:
            offset.ART_QUICK_CODE_OFFSET = 24;
            break;
        case 31:
            // source: https://android.googlesource.com/platform/art/+/refs/heads/android12-release/runtime/art_method.h
            offset.ART_QUICK_CODE_OFFSET = 24;
            break;
        case 30:
        case 29:
        case 28:
            offset.ART_QUICK_CODE_OFFSET = 32;
            break;
        case 27:
        case 26:
            offset.ART_QUICK_CODE_OFFSET = 40;
            break;
        default:
            throw ("API LEVEL:  is not supported now : (");
    }
}
bool compileMethod(JNIEnv *env, jobject method, jlong self) {
    jlong art_method = (jlong) env->FromReflectedMethod(method);
    if (art_method % 2 == 1) {
        art_method = reinterpret_cast<jlong>(JniIdManager_DecodeMethodId_(ArtHelper::getJniIdManager(), art_method));
    }
    bool ret;
    if (api_level >= 30) {
        void* current_region = JitCodeCache_GetCurrentRegion(ArtHelper::getJitCodeCache());
        if (api_level >= 31) {
            ret = ((JIT_COMPILE_METHOD4)jit_compile_method_)(jit_compiler_handle_, reinterpret_cast<void*>(self),
                                                             reinterpret_cast<void*>(current_region),
                                                             reinterpret_cast<void*>(art_method), 1);
        } else {
            ret = ((JIT_COMPILE_METHOD3)jit_compile_method_)(jit_compiler_handle_, reinterpret_cast<void*>(self),
                                                             reinterpret_cast<void*>(current_region),
                                                             reinterpret_cast<void*>(art_method), false, false);
        }
    } else if (api_level >= 29) {
        ret = ((JIT_COMPILE_METHOD2) jit_compile_method_)(jit_compiler_handle_,
                                                          reinterpret_cast<void *>(art_method),
                                                          reinterpret_cast<void *>(self), false, false);
    } else {
        ret = ((JIT_COMPILE_METHOD1) jit_compile_method_)(jit_compiler_handle_,
                                                          reinterpret_cast<void *>(art_method),
                                                          reinterpret_cast<void *>(self), false);
    }
    return (jboolean)ret;
}
extern "C" JNIEXPORT jboolean
Java_com_x_rehook_Rehook_compileMethod(JNIEnv *env, jobject thiz, jobject method, jlong self) {
    return compileMethod(env, method, self);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    init(env);
    return JNI_VERSION_1_6;
}

// 通过Method对象获取真实ArtMethod地址
long getMethodAddress(JNIEnv *env, jobject method) {
    long art_method = (long) env->FromReflectedMethod(method);
    if (art_method % 2 == 1) {
        art_method = reinterpret_cast<long>(JniIdManager_DecodeMethodId_(ArtHelper::getJniIdManager(), art_method));
    }
    return art_method;
}

void changeArtMethodArtQuickCodeAddress(long sourceAddress, long targetAddress) {
    long mem = 0;
    memcpy(&mem, (void*)(sourceAddress + offset.ART_QUICK_CODE_OFFSET), sizeof mem);
    memcpy((void*)(sourceAddress + offset.ART_QUICK_CODE_OFFSET), (void*)(targetAddress + offset.ART_QUICK_CODE_OFFSET), 8);
}

extern "C" JNIEXPORT void JNICALL Java_com_x_rehook_Rehook_nativeHookMethod(JNIEnv *env, jobject thiz, jobject origin_method,
                                  jobject target_method) {
    changeArtMethodArtQuickCodeAddress(getMethodAddress(env, origin_method), getMethodAddress(env, target_method));
}
