package net.azyk.framework;

import android.content.Intent;
import android.os.Bundle;

/**
 * @see AvoidOnActivityResult
 */
public abstract class BaseActivityAvoidOnResult extends android.support.v4.app.FragmentActivity
{
    private final AvoidOnActivityResult mAvoidOnActivityResult = new AvoidOnActivityResult(this);

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        mAvoidOnActivityResult.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.size() > 0)
            mAvoidOnActivityResult.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState)
    {
        mAvoidOnActivityResult.onSaveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    /**
     * startActivityForResult 不再需要重载 onActivityResult了.
     *
     * @param listener 返回时触发的回调函数.
     */
    protected void startActivityForResult(final Intent intent, final AvoidOnActivityResultListener listener)
    {
        startActivityForResult(intent, mAvoidOnActivityResult.startActivityForResult(listener));
    }
}