package com.example.core.hooks;

import android.app.Activity;
import android.content.Intent;

import com.example.hookwrapper.HookUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class HookManagerHelper {
    private HookManager hookManager;

    HookManagerHelper(HookManager hookManager) {
        this.hookManager = hookManager;
    }

    void doHook() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException {

        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Field currentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
        currentActivityThreadField.setAccessible(true);
        Object currentActivityThread = currentActivityThreadField.get(null);
        Class activityClientRecord = Class.forName("android.app.ActivityThread$ActivityClientRecord");
        // todo android 12 增加了native黑名单校验，🤮
        Method performLaunchActivity= currentActivityThread.getClass().getDeclaredMethod(
                "performLaunchActivity", activityClientRecord, Intent.class
        );
//        hookManager.dispatchPerformLaunchActivity();
    }

//    Activity aopPerformLaunchActivity(Object r, Intent customIntent) {
//        Object[] objects = new Object[2];
//        objects[0] = r;
//        objects[1] = customIntent;
//        hookManager.dispatchPerformLaunchActivity(new HookUtil.CallParameters(This.get(), objects, null, null));
//        return (Activity) (Origin.call());
//    }
}
