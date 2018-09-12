package cn.asiontang.framework;

import cn.asiontang.framework.exception.LogEx;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.lang.reflect.Constructor;

/**
 * 需要保持一致.
 *
 * @author AsionTang
 * @version 180912.01.01.001
 * @see BaseActivityAvoidOnResult
 */
public abstract class BaseFragmentAvoidOnResult extends android.support.v4.app.Fragment
{
    private static int sRequestCodeCounter = 0;
    @Nullable
    private SparseArray<OnActivityResultListener> mActivityResultListenerArray;
    @Nullable
    private Bundle mActivityResultListenerClassArray;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
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
    public void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        try
        {
            if (savedInstanceState == null)
                return;
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
    public void onSaveInstanceState(final Bundle outState)
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
    public void startActivityForResult(Intent intent, OnActivityResultListener listener)
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
