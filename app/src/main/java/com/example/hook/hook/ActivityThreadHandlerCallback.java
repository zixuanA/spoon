package com.example.hook.hook;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;

import com.example.hook.Utils;
import com.example.hook.application.MApplication;
import com.example.hook.classloder_hook.CustomClassLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

class ActivityThreadHandlerCallback implements Handler.Callback {

    Handler mBase;

    public ActivityThreadHandlerCallback(Handler base) {
        mBase = base;
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            // ActivityThread里面 "LAUNCH_ACTIVITY" 这个字段的值是100
            // 本来使用反射的方式获取最好, 这里为了简便直接使用硬编码
            case 100:
                handleLaunchActivity(msg);
                return true;
            case 159:
                handleTransaction(msg);
                break;


        }

        mBase.handleMessage(msg);
        return true;
    }

    private void handleLaunchActivity(Message msg) {
        // 这里简单起见,直接取出TargetActivity;

        Object obj = msg.obj;
        // 根据源码:
        // 这个对象是 ActivityClientRecord 类型
        // 我们修改它的intent字段为我们原来保存的即可.
        // switch (msg.what) {
        //      case LAUNCH_ACTIVITY: {
        //          Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
        //          final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

        //          r.packageInfo = getPackageInfoNoCheck(
        //                  r.activityInfo.applicationInfo, r.compatInfo);
        //         handleLaunchActivity(r, null);


        try {
            // 把替身恢复成真身
            Field intent = obj.getClass().getDeclaredField("intent");
            intent.setAccessible(true);
            Intent raw = (Intent) intent.get(obj);

            Intent target = raw.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
            raw.setComponent(target.getComponent());

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void handleTransaction(Message msg) {
        // 先获取到当前的ActivityThread对象
        Class<?> activityThreadClass = null;
        try {
            activityThreadClass = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
            Object currentActivityThread = currentActivityThreadField.get(null);
            Class<?> activityClientRecord = Class.forName("android.app.ActivityThread$ActivityClientRecord");
            Method performLaunchActivity = currentActivityThread.getClass().getDeclaredMethod(
                    "performLaunchActivity", activityClientRecord, Intent.class
            );
//            Class<?> activityInfo = Class.forName("android.content.pm.ActivityInfo");
//            Class<?> instance = Class.forName("android.app.Activity$NonConfigurationInstances");
//
//            Method startActivityNow = currentActivityThread.getClass().getDeclaredMethod(
//                    "startActivityNow", Activity.class, String.class,
//                    Intent.class, activityInfo, IBinder.class, Bundle.class,
//                    instance, IBinder.class
//            );
            Pine.hook(performLaunchActivity, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                    Intent old, targetIntent;
                    int index;
                    for (int i = 0; i < callFrame.args.length; i++) {
                        if (activityClientRecord.isInstance(callFrame.args[i])) {
                            //修改intent
                            Field intentField = activityClientRecord.getDeclaredField("intent");
                            intentField.setAccessible(true);
                            old = (Intent) intentField.get(callFrame.args[i]);
                            targetIntent = old.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
                            if (targetIntent == null) return;
                            intentField.set(callFrame.args[i], targetIntent);

                            //修改packageName
                            // 根据 getPackageInfo 根据这个 包名获取 LoadedApk的信息; 因此这里我们需要手动填上, 从而能够命中缓存
                            Field activityInfoField = activityClientRecord.getDeclaredField("activityInfo");
                            activityInfoField.setAccessible(true);
                            ActivityInfo activityInfo = (ActivityInfo) activityInfoField.get(callFrame.args[i]);
                            activityInfo.applicationInfo.packageName = targetIntent.getPackage() == null ?
                                    targetIntent.getComponent().getPackageName() : targetIntent.getPackage();

                            //修改mResDir 加载插件apk的res
                            Field packageInfoField = activityClientRecord.getDeclaredField("packageInfo");
                            packageInfoField.setAccessible(true);
                            Object loadedApk = packageInfoField.get(callFrame.args[i]);
                            Field mResDirField = loadedApk.getClass().getDeclaredField("mResDir");
                            mResDirField.setAccessible(true);
                            mResDirField.set(loadedApk, MApplication.Companion.getContext().getFileStreamPath("test.apk").getPath());

                            //修改插件classloader
                            String odexPath = Utils.getPluginOptDexDir(activityInfo.applicationInfo.packageName).getPath();
                            String libDir = Utils.getPluginLibDir(activityInfo.applicationInfo.packageName).getPath();
                            ClassLoader classLoader = new CustomClassLoader(MApplication.Companion.getContext().getFileStreamPath("test.apk").getPath()
                                    , odexPath, libDir, ClassLoader.getSystemClassLoader());
                            Field mClassLoaderField = loadedApk.getClass().getDeclaredField("mClassLoader");
                            mClassLoaderField.setAccessible(true);
                            mClassLoaderField.set(loadedApk, classLoader);

                            //修改插件activity的theme
                            Object packageParser = Class.forName("android.content.pm.PackageParser").newInstance();
                            Method parsePackage = packageParser.getClass().getDeclaredMethod("parsePackage", File.class, int.class);
                            Object pkg = parsePackage.invoke(packageParser, MApplication.Companion.getContext().getFileStreamPath("test.apk") , 1);
                            ArrayList<Object> activities = (ArrayList<Object>)pkg.getClass().getDeclaredField("activities").get(pkg);
                            for (int j = 0 ; j < activities.size() ; j++) {
                                ActivityInfo realInfo = (ActivityInfo) activities.get(j).getClass().getDeclaredField("info").get(activities.get(j));
                                String activityName = realInfo.name;
                                if (targetIntent.getComponent().getClassName().equals(activityName)) {
                                    activityInfo.theme = realInfo.theme;
                                }
                            }

                            hookPackageManager();


                            index = i;
                        }
                    }
//                    targetIntent = old.getEx
//                    callFrame.args
                }
            });
//            Object mTransactionExecutor = currentActivityThread.getClass().getDeclaredField("mTransactionExecutor").get(currentActivityThread);
//            Object mTransactionHandler = mTransactionExecutor.getClass().getDeclaredField("mTransactionHandler").get(mTransactionExecutor);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    private static void hookPackageManager() throws Exception {

        // 这一步是因为 initializeJavaContextClassLoader 这个方法内部无意中检查了这个包是否在系统安装
        // 如果没有安装, 直接抛出异常, 这里需要临时Hook掉 PMS, 绕过这个检查.

        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        // 获取ActivityThread里面原始的 sPackageManager
        Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
        sPackageManagerField.setAccessible(true);
        Object sPackageManager = sPackageManagerField.get(currentActivityThread);

        // 准备好代理对象, 用来替换原始的对象
        Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
        Object proxy = Proxy.newProxyInstance(iPackageManagerInterface.getClassLoader(),
                new Class<?>[]{iPackageManagerInterface},
                new IPackageManagerHookHandler(sPackageManager));

        // 1. 替换掉ActivityThread里面的 sPackageManager 字段
        sPackageManagerField.set(currentActivityThread, proxy);
    }

}
