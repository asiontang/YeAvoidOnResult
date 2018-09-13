# YeAvoidOnResult
避免使用onActivityResult，以提高代码可读性,解决其业务逻辑代码日益壮大的问题.


## 优雅的使用onActivityResult

需要和 BaseFragmentAvoidOnResult 保持一致

### 优化后方案:
使用onSaveInstanceState 和 onRestoreInstanceState 来恢复回调函数来实现此功能.

#### 优势:

1. 实现的方式有参考性(有别于网上已知的方案)
1. 实现了"非匿名回调"里 调用 时,能解决上层 Activity 被 **Destroy** 后,返回时正常触发回调.(参考"局限性"一节的错误示范代码)
1. 假如不考虑 "上层 Activity 被 **Destroy** 后能正常回调错误示范场景下"的情况,那么功能应该和网上已知方案是一致的.

#### 核心代码

```java
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
#### 局限性:(不支持的使用场景)

按照 (@AnotherJack)[https://github.com/AnotherJack/AvoidOnResult/issues/4#issuecomment-420620079] 提出来的思路,写了测试代码实际验证后发现的确存在问题.
> 创建listener的代码是在点击事件中，在OnClickListener中，是不是这个构造器的参数类型就不是外部Activity而是OnClickListener了

##### 不支持的使用场景 - 示范代码1:
```java
        this.btnLogin.setOnClickListener(new OnClickListener()
        {
            int testCode = 0;

            @Override
            public void onClick(final View v)
            {
                startActivityForResult(new Intent(getApplicationContext(), ServerSettingActivity.class), new OnActivityResultListener()
                {
                    @Override
                    public void onActivityResult(final int resultCode, final Intent data)
                    {
                        testCode++;
                        ToastEx.makeTextAndShowLong("testCode=" + testCode + " resultCode=" + resultCode);
                    }
                });
            }
        });
```

##### 不支持的使用场景 - 示范代码2:
```java
        this.btnLogin.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                startActivityForResult(new Intent(getApplicationContext(), ServerSettingActivity.class), new OnActivityResultListener()
                {
                    @Override
                    public void onActivityResult(final int resultCode, final Intent data)
                    {
                        ToastEx.makeTextAndShowLong(" resultCode=" + resultCode);
                    }
                });
            }
        });
```

##### 错误提示:
```java
System.err: java.lang.IllegalArgumentException: argument 1 should have type LoginActivity$5, got LoginActivity
        at java.lang.reflect.Constructor.constructNative(Native Method)
        at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
        at cn.asiontang.BaseActivityAvoidOnResult.onRestoreInstanceState(BaseActivityAvoidOnResult.java:96)
```

##### 原因初解:
> 在 `new OnActivityResultListener` 里调用了 `new OnClickListener` 的变量,导致实际生成的`OnActivityResultListener`匿名类名字为 `LoginActivity$5$1` ,因此其默认的构造函数具有唯一的一个参数类型必须为`LoginActivity$5`.

##### 局限性小结:

>必须保证 `OnActivityResultListener的匿名回调类` 的 `new`生成 是在 **其子类直属 范围内** 即可.  
>不能在匿名函数里再 `new OnActivityResultListener` 出来调用.  


### 使用代码

#### 正确的使用方式 - 代码示范1:
```java
public class LoginActivity extends BaseActivity implements OnClickListener
{
    @Override
    public void onCreate(final android.os.Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login);

        //other code

        this.btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v)
    {
        switch (v.getId())
        {
            case R.id.btnLogin:
                startActivityForResult(new Intent(getApplicationContext(), ServerSettingActivity.class), new OnActivityResultListener()
                {
                    @Override
                    public void onActivityResult(final int resultCode, final Intent data)
                    {
                        ToastEx.makeTextAndShowLong(" resultCode=" + resultCode);
                    }
                });
                break;
        }
    }
}
```

#### 正确的使用方式 - 代码示范2:
```java
public class LoginActivity extends BaseActivity
{
    @Override
    public void onCreate(final android.os.Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login);

        //other code

        this.btnLogin.setOnClickListener(setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
            	onLoginClick();
            }
        });
    }

    public void onLoginClick()
    {
        startActivityForResult(new Intent(getApplicationContext(), ServerSettingActivity.class), new OnActivityResultListener()
        {
            @Override
            public void onActivityResult(final int resultCode, final Intent data)
            {
                ToastEx.makeTextAndShowLong(" resultCode=" + resultCode);
            }
        });
    }
}
```

#### 正确的使用方式 - 代码示范3:
```java
public class LoginActivity extends BaseActivity implements OnClickListener
{
    @Override
    public void onCreate(final android.os.Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login);

        //other code
        
        final OnActivityResultListener mListener = new OnActivityResultListener()
        {
            @Override
            public void onActivityResult(final int resultCode, final Intent data)
            {
                ToastEx.makeTextAndShowLong(" resultCode=" + resultCode);
            }
        };
        this.btnLogin.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                startActivityForResult(new Intent(getApplicationContext(), ServerSettingActivity.class), mListener);
            }
        });
    }
}
```

### 网上的方案

当A被销毁后,从B返回时,A会被重建,此时回调函数无法自动创建导致无效!

遇到activity进入后台被系统回收的情况，就没办法接收到回调了，这点可以开启开发者选项->不保留活动 来验证

### 参考资料:

1.  [java - onDestroy() while "waiting" for onActivityResult() - Stack Overflow](https://stackoverflow.com/questions/10319330/ondestroy-while-waiting-for-onactivityresult)
2.  [Activity被回收后Callback不会回调 · Issue #4 · AnotherJack/AvoidOnResult](https://github.com/AnotherJack/AvoidOnResult/issues/4)
3.  [EasyAndroid基础集成组件库之：EasyActivityResult 拒绝臃肿的onActivityResult代码 - 掘金](https://juejin.im/post/5b21d019e51d4506d93701ba)

@author AsionTang   
@version 180912.01.01.001 
