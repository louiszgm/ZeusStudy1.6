package jianqiang.com.hostapp;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

import com.example.jianqiang.mypluginlibrary.PluginManager;
import com.example.jianqiang.mypluginlibrary.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import dalvik.system.DexClassLoader;
import jianqiang.com.hostapp.ams_hook.AMSHookHelper;

public class MyApplication extends Application {

    public static HashMap<String, String> pluginActivies = new HashMap<String, String>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        PluginManager.init(this);

        //get json data from server
        mockData();

        try {
            AMSHookHelper.hookAMN();
            AMSHookHelper.hookActivityThread();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    void mockData() {
        pluginActivies.put("jianqiang.com.plugin1.ActivityA", "jianqiang.com.hostapp.SingleTopActivity1");
        pluginActivies.put("jianqiang.com.plugin1.TestActivity1", "jianqiang.com.hostapp.SingleTaskActivity2");
    }
}
