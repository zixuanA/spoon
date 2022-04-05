package com.example.hook.hook;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.example.hook.StubActivity;
import com.example.hookwrapper.HookUtil;
import com.example.hookwrapper.MethodHook;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author weishu
 * @date 16/3/21
 */
public class AMSHookHelper {

    public static final String EXTRA_TARGET_INTENT = "extra_target_intent";

    /**
     * Hook AMS
     * <p/>
     * 主要完成的操作是  "把真正要启动的Activity临时替换为在AndroidManifest.xml中声明的替身Activity"
     * <p/>
     * 进而骗过AMS
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public static void hookActivityManagerNative() throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, NoSuchFieldException {

        //        17package android.util;
        //        18
        //        19/**
        //         20 * Singleton helper class for lazily initialization.
        //         21 *
        //         22 * Modeled after frameworks/base/include/utils/Singleton.h
        //         23 *
        //         24 * @hide
        //         25 */
        //        26public abstract class Singleton<T> {
        //            27    private T mInstance;
        //            28
        //                    29    protected abstract T create();
        //            30
        //                    31    public final T get() {
        //                32        synchronized (this) {
        //                    33            if (mInstance == null) {
        //                        34                mInstance = create();
        //                        35            }
        //                    36            return mInstance;
        //                    37        }
        //                38    }
        //            39}
        //        40

        if (Build.VERSION.SDK_INT >= 29) {
            Method startActivityForResult = Activity.class.getDeclaredMethod(
                    "startActivityForResult",
                    Intent.class, int.class, Bundle.class
            );
            startActivityForResult.setAccessible(true);
            HookUtil.Companion.hook(startActivityForResult, new MethodHook() {
                @Override
                public void beforeCall(@androidx.annotation.Nullable HookUtil.CallParameters parameters) {
                    Object[] args = parameters.getArgs();

                    Intent raw;
                    int index = 0;

                    for (int i = 0; i < (args == null ?0:args.length); i++) {
                        if (args[i] instanceof Intent) {
                            index = i;
                            break;
                        }
                    }
                    raw = (Intent) args[index];

                    Intent newIntent = new Intent();

                    // 替身Activity的包名, 也就是我们自己的包名
                    String stubPackage = "com.example.activityhook";

                    // 这里我们把启动的Activity临时替换为 StubActivity
                    ComponentName componentName = new ComponentName(stubPackage, StubActivity.class.getName());
                    newIntent.setComponent(componentName);

                    // 把我们原始要启动的TargetActivity先存起来
                    newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, raw);

                    // 替换掉Intent, 达到欺骗AMS的目的
                    args[index] = newIntent;
                }

                @Override
                public void afterCall(@androidx.annotation.Nullable HookUtil.CallParameters parameters) {

                }
            });

        }
        Field gDefaultField = null;
        if (Build.VERSION.SDK_INT >= 26) {
            Class<?> activityManager = Class.forName("android.app.ActivityManager");
            gDefaultField = activityManager.getDeclaredField("IActivityManagerSingleton");
        } else {
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
        }
        gDefaultField.setAccessible(true);
        gDefaultField.setAccessible(true);

        Object gDefault = gDefaultField.get(null);

        // gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
        Class<?> singleton = Class.forName("android.util.Singleton");
        Field mInstanceField = singleton.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);

        // ActivityManagerNative 的gDefault对象里面原始的 IActivityManager对象
        Object rawIActivityManager = mInstanceField.get(gDefault);

        // 创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活
        Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iActivityManagerInterface}, new IActivityManagerHandler(rawIActivityManager));
        mInstanceField.set(gDefault, proxy);

    }

    /**
     * 由于之前我们用替身欺骗了AMS; 现在我们要换回我们真正需要启动的Activity
     * <p/>
     * 不然就真的启动替身了, 狸猫换太子...
     * <p/>
     * 到最终要启动Activity的时候,会交给ActivityThread 的一个内部类叫做 H 来完成
     * H 会完成这个消息转发; 最终调用它的callback
     */
    public static void hookActivityThreadHandler() throws Exception {

        // 先获取到当前的ActivityThread对象
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Field currentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
        currentActivityThreadField.setAccessible(true);
        Object currentActivityThread = currentActivityThreadField.get(null);

        // 由于ActivityThread一个进程只有一个,我们获取这个对象的mH
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(currentActivityThread);

        // 设置它的回调, 根据源码:
        // 我们自己给他设置一个回调,就会替代之前的回调;

        //        public void dispatchMessage(Message msg) {
        //            if (msg.callback != null) {
        //                handleCallback(msg);
        //            } else {
        //                if (mCallback != null) {
        //                    if (mCallback.handleMessage(msg)) {
        //                        return;
        //                    }
        //                }
        //                handleMessage(msg);
        //            }
        //        }

        Field mCallBackField = Handler.class.getDeclaredField("mCallback");
        mCallBackField.setAccessible(true);

        mCallBackField.set(mH, new ActivityThreadHandlerCallback(mH));

    }
}
