package cn.asiontang.framework;

import cn.asiontang.framework.exception.LogEx;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.lang.reflect.Constructor;

/**
 * <h2>优雅的使用onActivityResult</h2>
 *
 * <p>需要和 {@link BaseFragmentAvoidOnResult} 保持一致</p>
 *
 * <h3>测试代码</h3>
 * <pre><code>
 * startActivityForResult(new Intent(mContext, testActivity.class), new OnActivityResultListener()
 * {
 * public void onActivityResult(final int resultCode, final Intent data)
 * {
 * if (resultCode != RESULT_OK)
 * return;
 * //do someting
 * }
 * });
 * </code></pre>
 *
 * <p><b>优化后方案:</b>使用onSaveInstanceState 和 onRestoreInstanceState 来恢复回调函数来实现此功能.</p>
 *
 * <p><b>网上的方案<span color='red'>不成熟</span>,当A被销毁后,从B返回时,A会被重建,此时回调函数无法自动创建导致无效!</b></p>
 * <p>遇到activity进入后台被系统回收的情况，就没办法接收到回调了，这点可以开启开发者选项->不保留活动 来验证</p>
 *
 * <p>参考资料:</p>
 * <ol>
 * <li><a href="https://stackoverflow.com/questions/10319330/ondestroy-while-waiting-for-onactivityresult">java - onDestroy() while "waiting" for onActivityResult() - Stack Overflow</a></li>
 * <li><a href="https://github.com/AnotherJack/AvoidOnResult/issues/4">Activity被回收后Callback不会回调 · Issue #4 · AnotherJack/AvoidOnResult</a></li>
 * <li><a href="https://juejin.im/post/5b21d019e51d4506d93701ba">EasyAndroid基础集成组件库之：EasyActivityResult 拒绝臃肿的onActivityResult代码 - 掘金</a></li>
 * </ol>
 *
 * @author AsionTang
 * @version 180912.01.01.001
 * @see BaseFragmentAvoidOnResult
 */
public abstract class BaseActivityAvoidOnResult extends android.support.v4.app.FragmentActivity
{
    private static int sRequestCodeCounter = 0;
    @Nullable
    private SparseArray<OnActivityResultListener> mActivityResultListenerArray;
    @Nullable
    private Bundle mActivityResultListenerClassArray;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        try
        {
            if (mActivityResultListenerClassArray != null && mActivityResultListenerClassArray.size() > 0)
                mActivityResultListenerClassArray.remove(String.valueOf(requestCode));

            if (mActivityResultListenerArray != null && mActivityResultListenerArray.size() > 0)
            {
                final OnActivityResultListener listener = mActivityResultListenerArray.get(requestCode);
                if (listener != null)
                    listener.onActivityResult(resultCode, data);

                mActivityResultListenerArray.remove(requestCode);
            }
        }
        catch (Throwable e)
        {
            LogEx.e("BaseActivityAvoidOnResult", mActivityResultListenerClassArray, mActivityResultListenerArray, e);
        }
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        try
        {
            mActivityResultListenerClassArray = savedInstanceState.getBundle("mActivityResultListenerClassArray");
            if (mActivityResultListenerClassArray != null)
            {
                if (mActivityResultListenerArray == null)
                    mActivityResultListenerArray = new SparseArray<>();
                for (String requestCode : mActivityResultListenerClassArray.keySet())
                {
                    try
                    {
                        final String className = mActivityResultListenerClassArray.getString(requestCode);
                        final Constructor<?> constructor = Class.forName(className).getDeclaredConstructors()[0];
                        constructor.setAccessible(true);
                        final OnActivityResultListener c = (OnActivityResultListener) constructor.newInstance(this);
                        mActivityResultListenerArray.put(Integer.parseInt(requestCode), c);
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (Throwable e)
        {
            LogEx.e("BaseActivityAvoidOnResult", mActivityResultListenerClassArray, mActivityResultListenerArray, e);
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState)
    {
        if (mActivityResultListenerClassArray != null && mActivityResultListenerClassArray.size() > 0)
            outState.putBundle("mActivityResultListenerClassArray", mActivityResultListenerClassArray);

        super.onSaveInstanceState(outState);
    }

    /**
     * startActivityForResult 不再需要重载 onActivityResult了.
     *
     * @param listener 返回时触发的回调函数.
     */
    protected void startActivityForResult(Intent intent, OnActivityResultListener listener)
    {
        sRequestCodeCounter++;

        if (mActivityResultListenerArray == null)
            mActivityResultListenerArray = new SparseArray<>();
        if (mActivityResultListenerClassArray == null)
            mActivityResultListenerClassArray = new Bundle();

        mActivityResultListenerArray.put(sRequestCodeCounter, listener);
        mActivityResultListenerClassArray.putString(String.valueOf(sRequestCodeCounter), listener.getClass().getName());

        startActivityForResult(intent, sRequestCodeCounter);
    }

    public interface OnActivityResultListener
    {
        void onActivityResult(int resultCode, Intent data);
    }
}