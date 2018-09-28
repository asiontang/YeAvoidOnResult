package net.azyk.framework;

import net.azyk.framework.exception.LogEx;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * <h2>优雅的使用onActivityResult</h2>
 *
 * <p>需要和 {@link BaseFragmentAvoidOnResult} 保持一致</p>
 *
 * <h3>测试代码</h3>
 * <p>正确的和错误的示范代码单击<a href="https://github.com/asiontang/YeAvoidOnResult">asiontang/YeAvoidOnResult:Github</a>查看</p>
 * <pre><code>
 * startActivityForResult(new Intent(mContext, testActivity.class), new AvoidOnActivityResultListener()
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
 * @version 180912.03.01.004
 */
class AvoidOnActivityResult
{
    private static final String TAG = "AvoidOnActivityResult";
    private static int sRequestCodeCounter = 0;
    private final Object mContainer;
    @Nullable
    private SparseArray<AvoidOnActivityResultListener> mActivityResultListenerArray;
    @Nullable
    private Bundle mActivityResultListenerClassArray;

    AvoidOnActivityResult(final Object container)
    {
        mContainer = container;
    }

    /**
     * 判断一个Class是否是当前的子类.
     */
    private boolean checkIsChildClass(final Class<?> aClass)
    {
        if (aClass == null)
            return false;
        if (aClass.getName().equals(mContainer.getClass().getName()))
            return true;
        return checkIsChildClass(aClass.getSuperclass());
    }

    @Nullable
    private AvoidOnActivityResultListener getOnActivityResultListener(final String className) throws Exception
    {
        final Constructor<?> constructor = Class.forName(className).getDeclaredConstructors()[0];

        //当不是按照推荐的方式使用时,会导致匿名类不是当前子类,而是类似OnClick事件匿名类.
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length != 1)
        {
            LogEx.e(TAG, "startActivityForResult调用方式有误!", "parameterTypes=", parameterTypes);
            if (BuildConfig.DEBUG)
                throw new RuntimeException();
            return null;
        }
        final Class<?> parameterType = parameterTypes[0];

        //判断是否为子类
        if (!checkIsChildClass(parameterType))
        {
            //支持大部分"简单情况下"的匿名函数里调用startActivityForResult，如常见控件的onClick事件等等.
            final AvoidOnActivityResultListener listener = getOnActivityResultListenerWhenOnNestedMode(constructor, parameterType);
            if (listener != null)
                return listener;

            LogEx.e(TAG, "startActivityForResult调用方式有误!", "回调的匿名函数不是在子类的直属范围内创建的!"
                    , "parameterTypes=", parameterTypes
                    , "getDeclaredConstructors=", parameterType.getDeclaredConstructors()
                    , "getInterfaces=", parameterType.getInterfaces()
                    , "getGenericInterfaces=", parameterType.getGenericInterfaces()
                    , "getSuperclass=", parameterType.getSuperclass()
                    , "getGenericSuperclass=", parameterType.getGenericSuperclass()
            );
            if (BuildConfig.DEBUG)
                throw new RuntimeException();
            return null;
        }

        constructor.setAccessible(true);

        return (AvoidOnActivityResultListener) constructor.newInstance(mContainer);
    }

    private AvoidOnActivityResultListener getOnActivityResultListenerWhenOnNestedMode(final Constructor<?> constructor, final Class<?> parameterType) throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        final Constructor<?> uplevelConstructor = parameterType.getDeclaredConstructors()[0];
        final Class<?>[] uplevelParameterTypes = uplevelConstructor.getParameterTypes();
        if (uplevelParameterTypes == null || uplevelParameterTypes.length != 1)
        {
            LogEx.e(TAG, "startActivityForResult调用方式有误!", "理论上不存在", "uplevelParameterTypes=", uplevelParameterTypes);
            if (BuildConfig.DEBUG)
                throw new RuntimeException();
            return null;
        }
        final Class<?> uplevelParameterType = uplevelParameterTypes[0];

        //判断是否为子类
        if (!checkIsChildClass(uplevelParameterType))
        {
            LogEx.e(TAG, "startActivityForResult调用方式有误!", "不支持的匿名回调嵌套调用的方式(最多支持嵌套2级别)!"
                    , "parameterTypes=", uplevelParameterType
                    , "getDeclaredConstructors=", uplevelParameterType.getDeclaredConstructors()
                    , "getInterfaces=", uplevelParameterType.getInterfaces()
                    , "getGenericInterfaces=", uplevelParameterType.getGenericInterfaces()
                    , "getSuperclass=", uplevelParameterType.getSuperclass()
                    , "getGenericSuperclass=", uplevelParameterType.getGenericSuperclass()
            );
            if (BuildConfig.DEBUG)
                throw new RuntimeException();
            return null;
        }

        //判断上级嵌套的的匿名类必须足够简单!否则可能导致其类变量数据状态不一致的问题.
        final Field[] declaredFields = parameterType.getDeclaredFields();
        if (declaredFields != null && declaredFields.length > 1)//因为匿名函数至少有一个变量名为this$0的持有其所在的类的引用.
        {
            LogEx.e(TAG, "startActivityForResult调用方式有误!"
                    , "不支持的匿名回调嵌套调用的方式(最多支持嵌套2级别)!"
                    , "不支持复杂的,匿名类存在类变量的匿名回调嵌套调用的方式!,担心反射实例化后导致其类变量数据状态不一致!"
                    , "declaredFields=", declaredFields
                    , "parameterTypes=", uplevelParameterType
                    , "getDeclaredConstructors=", uplevelParameterType.getDeclaredConstructors()
                    , "getInterfaces=", uplevelParameterType.getInterfaces()
                    , "getGenericInterfaces=", uplevelParameterType.getGenericInterfaces()
                    , "getSuperclass=", uplevelParameterType.getSuperclass()
                    , "getGenericSuperclass=", uplevelParameterType.getGenericSuperclass()
            );
            if (BuildConfig.DEBUG)
                throw new RuntimeException();
            return null;
        }

        constructor.setAccessible(true);

        uplevelConstructor.setAccessible(true);

        return (AvoidOnActivityResultListener) constructor.newInstance(uplevelConstructor.newInstance(mContainer));
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        try
        {
            if (mActivityResultListenerClassArray != null && mActivityResultListenerClassArray.size() > 0)
                mActivityResultListenerClassArray.remove(String.valueOf(requestCode));

            if (mActivityResultListenerArray != null && mActivityResultListenerArray.size() > 0)
            {
                final AvoidOnActivityResultListener listener = mActivityResultListenerArray.get(requestCode);
                if (listener != null)
                    listener.onActivityResult(resultCode, data);

                mActivityResultListenerArray.remove(requestCode);
            }
        }
        catch (final Throwable e)
        {
            LogEx.e(TAG, mActivityResultListenerClassArray, mActivityResultListenerArray, e);
        }
    }

    public void onRestoreInstanceState(final Bundle savedInstanceState)
    {
        try
        {
            mActivityResultListenerClassArray = savedInstanceState.getBundle("mActivityResultListenerClassArray");

            if (mActivityResultListenerClassArray == null || mActivityResultListenerClassArray.isEmpty())
                return;

            if (mActivityResultListenerArray == null)
                mActivityResultListenerArray = new SparseArray<>();

            for (final String requestCode : mActivityResultListenerClassArray.keySet())
                try
                {
                    final String className = mActivityResultListenerClassArray.getString(requestCode);

                    final AvoidOnActivityResultListener c = getOnActivityResultListener(className);
                    if (c == null)
                        continue;
                    final int code = Integer.parseInt(requestCode);

                    //跳过"生"前的号段，以防万一而已。
                    sRequestCodeCounter += code * 10;

                    mActivityResultListenerArray.put(code, c);
                }
                catch (final Throwable e)
                {
                    LogEx.e(TAG, mActivityResultListenerClassArray, mActivityResultListenerArray, e);
                }
        }
        catch (final Throwable e)
        {
            LogEx.e(TAG, mActivityResultListenerClassArray, mActivityResultListenerArray, e);
        }
    }

    public void onSaveInstanceState(final Bundle outState)
    {
        if (mActivityResultListenerClassArray != null && mActivityResultListenerClassArray.size() > 0)
            outState.putBundle("mActivityResultListenerClassArray", mActivityResultListenerClassArray);
    }

    /**
     * startActivityForResult 不再需要重载 onActivityResult了.
     *
     * @param listener 返回时触发的回调函数.
     */
    public int startActivityForResult(final AvoidOnActivityResultListener listener)
    {
        sRequestCodeCounter++;

        if (mActivityResultListenerArray == null)
            mActivityResultListenerArray = new SparseArray<>();
        if (mActivityResultListenerClassArray == null)
            mActivityResultListenerClassArray = new Bundle();

        mActivityResultListenerArray.put(sRequestCodeCounter, listener);
        mActivityResultListenerClassArray.putString(String.valueOf(sRequestCodeCounter), listener.getClass().getName());

        if (BuildConfig.DEBUG)
        {
            //提前检测:以便预判当恢复时,是否能正常反射实例化,不行的话则直接崩溃出错,让调用者立马有问题.
            try
            {
                final AvoidOnActivityResultListener c = getOnActivityResultListener(listener.getClass().getName());
                if (c == null)
                    return 0;
            }
            catch (final Throwable e)
            {
                throw new RuntimeException(e);
            }
        }
        return sRequestCodeCounter;
    }
}
