# YeAvoidOnResult
避免使用onActivityResult，以提高代码可读性,解决其业务逻辑代码日益壮大的问题.


## 优雅的使用onActivityResult

需要和 BaseFragmentAvoidOnResult 保持一致

### 使用代码

```

startActivityForResult(new Intent(mContext, testActivity.class), new OnActivityResultListener()
{
public void onActivityResult(final int resultCode, final Intent data)
{
if (resultCode != RESULT_OK)
return;
//do someting
}
});

```

### 优化后方案:
使用onSaveInstanceState 和 onRestoreInstanceState 来恢复回调函数来实现此功能.

#### 核心代码


```

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

```

### 网上的方案不成熟

当A被销毁后,从B返回时,A会被重建,此时回调函数无法自动创建导致无效!

遇到activity进入后台被系统回收的情况，就没办法接收到回调了，这点可以开启开发者选项->不保留活动 来验证

### 参考资料:

1.  [java - onDestroy() while "waiting" for onActivityResult() - Stack Overflow](https://stackoverflow.com/questions/10319330/ondestroy-while-waiting-for-onactivityresult)
2.  [Activity被回收后Callback不会回调 · Issue #4 · AnotherJack/AvoidOnResult](https://github.com/AnotherJack/AvoidOnResult/issues/4)
3.  [EasyAndroid基础集成组件库之：EasyActivityResult 拒绝臃肿的onActivityResult代码 - 掘金](https://juejin.im/post/5b21d019e51d4506d93701ba)

@author AsionTang   
@version 180912.01.01.001 
