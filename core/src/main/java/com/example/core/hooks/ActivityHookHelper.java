package com.example.core.hooks;

import static com.example.core.hooks.ActivityHookKt.EXTRA_TARGET_INTENT;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import com.example.core.StubActivity;

public class ActivityHookHelper {
    void aopStartActivityForResult(Intent intent, int requestCode, Bundle options) {
        Intent raw = intent;

        Intent newIntent = new Intent();

        // 替身Activity的包名, 也就是我们自己的包名
        String stubPackage = "com.example.activityhook";

        // 这里我们把启动的Activity临时替换为 StubActivity
        ComponentName componentName = new ComponentName(
                stubPackage,
                StubActivity.class.getName()
        );
        newIntent.setComponent(componentName);

        // 把我们原始要启动的TargetActivity先存起来
        newIntent.putExtra(EXTRA_TARGET_INTENT, raw);

        intent = newIntent;
//        Origin.callVoid();
    }
}
