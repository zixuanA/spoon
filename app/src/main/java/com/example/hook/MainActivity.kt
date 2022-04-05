package com.example.hook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.hook.classloder_hook.BaseDexClassLoaderHookHelper
import com.example.hook.databinding.ActivityMainBinding
import com.example.hook.hook.AMSHookHelper
import top.canyie.pine.Pine
import top.canyie.pine.callback.MethodHook
import java.lang.reflect.Method

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
//            hookResources()
            startActivity(Intent().apply {
                component = ComponentName("com.example.tests","com.example.tests.MainActivity")
            })
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        try {
//            Utils.extractAssets(newBase, "test.apk")
//            LoadedApkClassLoaderHookHelper.hookLoadedApkInActivityThread(getFileStreamPath("test.apk"))
            BaseDexClassLoaderHookHelper.patchClassLoader(classLoader,getFileStreamPath("test.apk"),
                Utils.getPluginOptDexDir(applicationInfo.packageName))


            AMSHookHelper.hookActivityManagerNative()
            AMSHookHelper.hookActivityThreadHandler()
        } catch (throwable: Throwable) {
            throw RuntimeException("hook failed", throwable)
        }
    }
    private fun hookResources() {
        val resourcesKey = Class.forName("android.content.res.ResourcesKey")
        val createAssetManager = Class.forName("android.app.ResourcesManager").getDeclaredMethod("createAssetManager", resourcesKey)
        Pine.hook(createAssetManager, object: MethodHook(){
            override fun beforeCall(callFrame: Pine.CallFrame?) {

                val assetManager = AssetManager::class.java.newInstance()
                val addAssetPath: Method = assetManager.javaClass.getMethod(
                    "addAssetPath",
                    String::class.java
                )
                addAssetPath.invoke(assetManager, getFileStreamPath("test.apk").path)
                callFrame?.result = assetManager
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}