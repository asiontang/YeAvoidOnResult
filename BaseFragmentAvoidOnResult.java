package net.azyk.framework;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * @see AvoidOnActivityResult
 */
public abstract class BaseFragmentAvoidOnResult extends android.support.v4.app.Fragment
{
    private final AvoidOnActivityResult mAvoidOnActivityResult = new AvoidOnActivityResult(this);

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        mAvoidOnActivityResult.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.size() > 0)
            mAvoidOnActivityResult.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState)
    {
        mAvoidOnActivityResult.onSaveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    /**
     * startActivityForResult 不再需要重载 onActivityResult了.
     *
     * @param listener 返回时触发的回调函数.
     */
    public void startActivityForResult(final Intent intent, final AvoidOnActivityResultListener listener)
    {
        startActivityForResult(intent, mAvoidOnActivityResult.startActivityForResult(listener));
    }
}
