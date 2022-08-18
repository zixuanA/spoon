package com.example.core.hooks;

import android.app.Activity;
import android.content.Intent;

import com.example.hookwrapper.HookUtil;


public class HookManagerHelper {
    private HookManager hookManager;

    HookManagerHelper(HookManager hookManager) {
        this.hookManager = hookManager;
    }


//    Activity aopPerformLaunchActivity(Object r, Intent customIntent) {
//        Object[] objects = new Object[2];
//        objects[0] = r;
//        objects[1] = customIntent;
//        hookManager.dispatchPerformLaunchActivity(new HookUtil.CallParameters(This.get(), objects, null, null));
//        return (Activity) (Origin.call());
//    }
}
