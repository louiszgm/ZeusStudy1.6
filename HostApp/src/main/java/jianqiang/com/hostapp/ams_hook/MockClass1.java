package jianqiang.com.hostapp.ams_hook;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jianqiang.com.hostapp.MyApplication;
import jianqiang.com.hostapp.StubActivity;

class MockClass1 implements InvocationHandler {

    private static final String TAG = "MockClass1";
    private static final String stubPackage = "jianqiang.com.hostapp";

    Object mBase;

    public MockClass1(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Log.e("bao", method.getName());

        if ("startActivity".equals(method.getName())) {
            // 只拦截这个方法
            // 替换参数, 任你所为;甚至替换原始Activity启动别的Activity偷梁换柱

            // 找到参数里面的第一个Intent 对象
            Intent raw;
            int index = 0;

            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            raw = (Intent) args[index];

            Intent newIntent = new Intent();

            // 替身Activity的包名, 也就是我们自己的包名

            ComponentName componentName = null;

            String rawClass = raw.getComponent().getClassName();
            if(MyApplication.pluginActivies.containsKey(rawClass)) {
                String activity = MyApplication.pluginActivies.get(rawClass);
                int pos = activity.lastIndexOf(".");
                String pluginPackage = activity.substring(0, pos);
                componentName = new ComponentName(pluginPackage, activity);
            } else {
                componentName = new ComponentName(stubPackage, StubActivity.class.getName());
            }

            newIntent.setComponent(componentName);

            // 把我们原始要启动的TargetActivity先存起来
            newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, raw);

            // 替换掉Intent, 达到欺骗AMS的目的
            args[index] = newIntent;

            Log.d(TAG, "hook success");
            return method.invoke(mBase, args);

        }

        return method.invoke(mBase, args);
    }
}