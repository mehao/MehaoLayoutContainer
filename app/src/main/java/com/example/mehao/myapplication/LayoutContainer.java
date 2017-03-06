package com.example.mehao.myapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Scroller;

public class LayoutContainer extends RelativeLayout {
    private View mTopView;
    private View mBottomView;
    private VelocityTracker vt;//用于计算手滑动的速度

    private int mViewHeight;
    private int mViewWidth;
    private boolean isMeasured = false;
    private boolean canPullDown;
    private boolean canPullUp;
    private int mCurrentViewIndex = 0;//记录当前展示的是哪个view，0是topView，1是bottomView
    private float mMoveLen;//手滑动距离，这个是控制布局的主要变量
    private float mLastY, mTempLastY, mLastX;
    private int mEvents;
    private Scroller mScroller;
    private int oldY;
    private float mTempMoveLen;
    private int mScreenH;
    private boolean mIsClick;
    private boolean mCanScroll;
    private int mScrollX, mScrollY;

    public LayoutContainer(Context context) {
        super(context);
        mScroller = new Scroller(context);
        mScreenH = getScreenH(context);
    }

    public LayoutContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
        mScreenH = getScreenH(context);
    }

    public LayoutContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScroller = new Scroller(context);
        mScreenH = getScreenH(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (vt == null) {
                    vt = VelocityTracker.obtain();
                } else {
                    vt.clear();
                }
                mLastY = ev.getY();
                mLastX = ev.getX();
                mTempLastY = ev.getY();
                vt.addMovement(ev);
                mEvents = 0;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                mEvents = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                vt.addMovement(ev);
                if (canPullUp && mCurrentViewIndex == 0 && mEvents == 0) {
                    mMoveLen += (ev.getY() - mLastY);
                    if (mMoveLen > 0) {
                        mMoveLen = 0;
                        mCurrentViewIndex = 0;
                    } else if (mMoveLen < -mViewHeight) {
                        mMoveLen = -mViewHeight;
                        mCurrentViewIndex = 1;
                    }
                    if (mMoveLen < -5) {
                        mCanScroll = true;
                    } else {
                        mCanScroll = false;
                    }
                } else if (canPullDown && mCurrentViewIndex == 1 && mEvents == 0) {
                    mMoveLen += (ev.getY() - mLastY);
                    if (mMoveLen < -mViewHeight) {
                        mMoveLen = -mViewHeight;
                        mCurrentViewIndex = 1;
                    } else if (mMoveLen > 0) {
                        mMoveLen = 0;
                        mCurrentViewIndex = 0;
                    }
                    if (mMoveLen > 5 - mViewHeight) {
                        mCanScroll = true;
                    } else {
                        mCanScroll = false;
                    }
                } else {
                    mCanScroll = false;
                    mEvents++;
                }
                mLastY = ev.getY();
                requestLayout();
                break;
            case MotionEvent.ACTION_UP:
                double deltaX = Math.sqrt((ev.getX() - mLastX) * (ev.getX() - mLastX) + (ev.getY() - mTempLastY) * (ev.getY() - mTempLastY));
                if (deltaX < 10) {
                    mIsClick = true;
                }
                if (mIsClick) {
                    mIsClick = false;
                    break;
                }
                mLastY = ev.getY();
                vt.addMovement(ev);
                vt.computeCurrentVelocity(500);
                int initialVelocity = (int) vt.getYVelocity();
                if (mMoveLen != 0 && mMoveLen != mTempMoveLen) {
                    fling(-initialVelocity);
                }
                mTempMoveLen = mMoveLen;
                try {
                    vt.recycle();
                    vt = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
        super.dispatchTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, -700, 700);
            awakenScrollBars(mScroller.getDuration());
            invalidate();
        }
    }

    /**
     * 当scrollY > 0时，是往下滑动，当scrollY < 0时，是往上滑动
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset() && mMoveLen != (-mScreenH + getStatusBarHeight(getContext()))) {
            int scrollX = mScroller.getCurrX();
            int scrollY = mScroller.getCurrY();
            //往上滑动
            if (scrollY > 0 && oldY < 0) {
                oldY = 0;
            }
            if (scrollY > 0 && scrollY < oldY) {
                oldY = 0;
            }

            //往下滑动
            if (scrollY < 0 && scrollY > oldY) {
                oldY = 0;
            }
            if (scrollY < 0 && oldY > 0) {
                oldY = 0;
            }
            if (mMoveLen != 0 || mMoveLen != (-mScreenH + getStatusBarHeight(getContext()))) {
                scrollTo(scrollX, scrollY);
            }
            if (scrollY > 0) {
                mMoveLen = mMoveLen - scrollY + oldY;
            }
            if (scrollY < 0) {
                mMoveLen = mMoveLen - scrollY + oldY;
            }
            if (mMoveLen > 0) {
                mMoveLen = 0;
            }
            if (mMoveLen <= (-mScreenH + getStatusBarHeight(getContext()))) {
                mMoveLen = -mScreenH + getStatusBarHeight(getContext());
            }
            oldY = scrollY;
            if (mMoveLen != 0 || mMoveLen != (-mScreenH + getStatusBarHeight(getContext()))) {
                requestLayout();
            }
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (mScrollX != x || mScrollY != y) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = x;
            mScrollY = y;
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (!awakenScrollBars()) {
                invalidate();
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mTopView.layout(0, (int) mMoveLen, mViewWidth, mTopView.getMeasuredHeight() + (int) mMoveLen);
        mBottomView.layout(0, mTopView.getMeasuredHeight() + (int) mMoveLen,
                mViewWidth, mTopView.getMeasuredHeight() + (int) mMoveLen + mBottomView.getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!isMeasured) {
            isMeasured = true;
            mViewHeight = getMeasuredHeight();
            mViewWidth = getMeasuredWidth();
            mTopView = getChildAt(0);
            mBottomView = getChildAt(1);
            mBottomView.setOnTouchListener(bottomViewTouchListener);
            mTopView.setOnTouchListener(topViewTouchListener);
        }
    }

    private OnTouchListener topViewTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ScrollView sv = (ScrollView) v;
            if (sv.getScrollY() == (sv.getChildAt(0).getMeasuredHeight() - sv.getMeasuredHeight()) && mCurrentViewIndex == 0) {
                canPullUp = true;
            } else {
                canPullUp = false;
            }
            return mCanScroll;
        }
    };

    private OnTouchListener bottomViewTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ScrollView sv = (ScrollView) v;
            if (sv.getScrollY() == 0 && mCurrentViewIndex == 1) {
                canPullDown = true;
            } else {
                canPullDown = false;
            }
            return mCanScroll;
        }
    };

    /**
     * 获取屏幕高度
     */
    public static int getScreenH(Context aty) {
        DisplayMetrics dm = aty.getResources().getDisplayMetrics();
        int h = dm.heightPixels;
        return h;
    }

    public static int getStatusBarHeight(Context context) {
        // 获得状态栏高度
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return context.getResources().getDimensionPixelSize(resourceId);
    }
}
