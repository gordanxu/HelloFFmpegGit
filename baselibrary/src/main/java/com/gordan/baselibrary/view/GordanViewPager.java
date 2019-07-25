package com.gordan.baselibrary.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by gordan on 2018/11/5.
 */

public class GordanViewPager extends ViewPager
{
    private boolean mTouchFlag;

    public GordanViewPager(Context context)
    {
        super(context);
    }

    public GordanViewPager(Context context, AttributeSet attrs)
    {
        super(context,attrs);
    }

    public void setTouchFlag(boolean mTouchFlag) {
        this.mTouchFlag = mTouchFlag;
    }

    /****
     * mTouchFlag标记位默认为False 不向下分发触摸事件
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mTouchFlag && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.mTouchFlag && super.onTouchEvent(ev);

    }
}
