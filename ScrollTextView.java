package com.zhudai.view;/*
 *  Android Wheel Control.
 *  https://code.google.com/p/android-wheel/
 *  
 *  Copyright 2011 Yuri Kanivets
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.Scroller;


import com.zhudai.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The text that can scroll and pick
 * 调用模块：日期、时间对话框，闹钟等应用
 * 
 * @author xiaohongzhi
 */
public class ScrollTextView extends View {

    private static String TAG = "ScrollTextView";

    private static final int DEF_YSCROLL_END = 0x7FFFFFFF;

    /* 默认可视项个数 */
    private static final int DEF_VISIBLE_ITEMS = 3;

    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 当前项值
    private int mCurrentItem = 0;

    // 可视项个数 Count of visible items
    private int mVisibleItems = DEF_VISIBLE_ITEMS;

    // Item height
    private float mSelectItemHeight = 0;// 选中项高度
    private float mNormalItemHeight = 0;// 正常项高度

    private int mSelectTextColor;// 选中项颜色
    private int mNormalTextColor;// 正常项颜色

    private float mNormalTextSize;// 正常项字体大小
    private float mSelectTextSize;// 选中项字体大小

    // Scrolling
    private ScrollTextViewScroller mWheelScroller;
    private boolean isScrollingPerformed;
    private int mScrollingOffset;

    // 显示的Y偏移，第一行开始位置的偏移
    private int mOffsetY;
    private int mOffsetX;

    private VisibleItemsRange mRange;
    // Cyclic
    boolean isCyclic = false;

    // The number of first item in layout
    private int mFirstItem;

    // View adapter
    private ScrollTextViewAdapter mViewAdapter;
    private IDataAdapter mDataInterface;

    // 上下阴影
    private Shader mFadingShader;
    private Matrix mFadingMatrix;
    private Paint mFadingPaint;
    private float mFadingHeight;
    private boolean mIsDrawFading = true;

    private float mSelectFontMetricsCenterY;
    private float mNormalFontMetricsCenterY;
    private float mFontMetricsCenterY;

    // flyme6需求添加，flyme6需要可以配置每一项的字体大小，字体颜色
    private List<Float> mNormalTextSizes;
    private List<Integer> mNormalTextColors;

    private boolean mParentRequestDisallowInterceptTouchEvent = true;

//    private SoudPoolHelper mSoundPoolHelper;

    /**
     * 数据适配器
     * 
     * @author xiaohongzhi
     *
     */
    public interface IDataAdapter {
        /**
         * 获取指定位置需显示text
         * @param position 
         * @return 返回需显示的text
         */
        public String getItemText(int position);

        /**
         * 选择项改变时的回调接口
         * @param view
         * @param fromOld 改变前的选中位置
         * @param toNew 改变后的选中位置
         */
        public void onChanged(View view, int fromOld, int toNew);

    }

    /**
     * 选中项改变事件监听
     * @author xiaohongzhi
     *
     */
    public interface OnScrollTextViewChangedListener {
        /**
         * Callback method to be invoked when current item changed
         * @param view the ScrollTextView whose state has changed
         * @param oldValue the old value of current item
         * @param newValue the new value of current item
         */
        void onChanged(ScrollTextView view, int oldValue, int newValue);
    }
    
    /**
     * 点击事件监听
     * @author xiaohongzhi
     *
     */
    public interface OnScrollTextViewClickedListener {
        /**
         * Callback method to be invoked when current item clicked
         * @param view the ScrollTextView
         * @param itemIndex the index of clicked item
         */
        void onItemClicked(ScrollTextView view, int itemIndex);
    }

    /**
     * 滑动事件监听
     * @author xiaohongzhi
     *
     */
    public interface OnScrollTextViewScrollListener {
        /**
         * Callback method to be invoked when scrolling started.
         * @param view the ScrollTextView whose state has changed.
         */
        void onScrollingStarted(ScrollTextView view);
        
        /**
         * Callback method to be invoked when scrolling ended.
         * @param view the ScrollTextView whose state has changed.
         */
        void onScrollingFinished(ScrollTextView view);
    }

    // Listeners
    private List<OnScrollTextViewChangedListener> mChangingListeners = new LinkedList<OnScrollTextViewChangedListener>();
    private List<OnScrollTextViewScrollListener> mScrollingListeners = new LinkedList<OnScrollTextViewScrollListener>();
    private List<OnScrollTextViewClickedListener> mClickingListeners = new LinkedList<OnScrollTextViewClickedListener>();

    private Paint mBitmapPaint;
    private Context mContext;
    public ScrollTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initData(context);
    }

    public ScrollTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollTextView(Context context) {
        super(context);
        initData(context);
    }

    /*
     * Initializes class data
     * @param context the context
     */
    private void initData(Context context) {
        mContext = context;

        mNormalTextSizes = new ArrayList<>();

        mWheelScroller = new ScrollTextViewScroller(getContext(),
                mScrollingListener);

        mSelectTextSize = context.getResources().getDimension(
                R.dimen.mc_picker_selected_number_size);
        mNormalTextSize = context.getResources().getDimension(
                R.dimen.mc_picker_normal_number_size);
        mSelectItemHeight = context.getResources().getDimension(
                R.dimen.mc_picker_select_item_height);
        mNormalItemHeight = context.getResources().getDimension(
                R.dimen.mc_picker_normal_item_height);

        mSelectTextColor = context.getResources().getColor(
                R.color.mc_picker_selected_color);
        mNormalTextColor = context.getResources().getColor(
                R.color.mc_picker_unselected_color);

        setTextPreference(mSelectTextSize, mNormalTextSize, mSelectItemHeight, mNormalItemHeight);
        setTextColor(mSelectTextColor, mNormalTextColor);

        mViewAdapter = new ScrollTextViewAdapter();

        mFadingMatrix = new Matrix();
        mFadingShader = new LinearGradient(0, 0, 0, 1, 0xFF000000, 0,
                Shader.TileMode.CLAMP);

        mFadingPaint = new Paint();
        mFadingPaint
                .setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mFadingPaint.setShader(mFadingShader);
        mFadingHeight = context.getResources().getDimension(
                R.dimen.mc_picker_fading_height);

        mRange = new VisibleItemsRange();

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setColor(Color.LTGRAY);

        mTextPaint.setTextAlign(Paint.Align.CENTER);
        computeFontMetrics();

//        mSoundPoolHelper = new SoudPoolHelper();
//        this.addChangingListener(new PlaySoundScrollTextViewChangedListener());
    }
    
    /**
     * 刷新可选项个数
     * @param count 可选项个数
     */
    public void refreshCount(int count) {
        refreshData(count, mCurrentItem, 0, count - 1);
    }

    /**
     * 刷新当前选择项，无动画
     * @param current 当前选择项id
     */
    public void refreshCurrent(int current) {
        refreshData(mViewAdapter.getItemsCount(), current,
                mViewAdapter.getValidStart(), mViewAdapter.getValidEnd());
    }

    /**
     * 刷新可选项个数和当前选择项，无动画
     * @param count 可选项个数
     * @param current 当前选择项id
     */
    public void refreshCountAndCurrent(int count, int current) {
        refreshData(count, current, 0, count - 1);
    }

    /**
     * 刷新scrolltextview的各项数据
     * @param count 可选项个数
     * @param currentItem 当前选择项id
     * @param validStart 有效开始项
     * @param validEnd 有效结束项
     */
    public void refreshData(int count, int currentItem, int validStart, int validEnd) {
        stopScrolling();

        if (count < 0)
            return;

        setViewAdapter(mViewAdapter.update(count, validStart, validEnd));

        int oldCurrentItem = mCurrentItem;

        if (currentItem != mCurrentItem) {
            mCurrentItem = Math.max(currentItem, validStart);

            if (mCurrentItem > validEnd || mCurrentItem >= count) {
                mCurrentItem = Math.min(validEnd, count);
            }
        }

        if (oldCurrentItem != mCurrentItem) {
            if (mDataInterface != null) {
                mDataInterface.onChanged(this, oldCurrentItem,
                        mCurrentItem);
            }
        }

        invalidate();
    }

    /**
     * 设置scrolltextview的各项数据
     * @param dataAdapter 数据适配器
     * @param currentItem 当前选择项id
     * @param count 可选项个数
     * @param oneScreenCount 可视项个数
     */
    public void setData(IDataAdapter dataAdapter, int currentItem, int count,
            int oneScreenCount) {
        setData(dataAdapter, -1, currentItem, count, oneScreenCount, 0,
                count - 1, true);
    }

    /**
     * 设置scrolltextview的各项数据
     * @param dataAdapter 数据适配器
     * @param lineOffset 第一项的y方向偏移量
     * @param currentItem 当前选择项id
     * @param count 可选项个数
     * @param oneScreenCount 可视项个数
     * @param validStart 有效开始项
     * @param validEnd 有效结束项
     * @param cycleEnabled 是否可循环选择
     */
    public void setData(IDataAdapter dataAdapter, float lineOffset,
            int currentItem, int count, int oneScreenCount, int validStart,
            int validEnd, boolean cycleEnabled) {
        setIDataAdapter(dataAdapter);
        mVisibleItems = oneScreenCount;
        isCyclic = cycleEnabled;

        if (lineOffset == -1) {
            mOffsetY = getResources().getDimensionPixelSize(
                    R.dimen.mc_picker_offset_y);
        } else {
            final DisplayMetrics metrics = getContext().getResources()
                    .getDisplayMetrics();
            final float density = metrics.density;
            mOffsetY = (int) (lineOffset * density);
        }

        if (count < mVisibleItems || validEnd + 1 < count || validStart > 0) {
            // 不使用循环
            isCyclic = false;
        }

        // inflater view into
        refreshData(count, currentItem, validStart, validEnd);
    }

    // Scrolling listener
    private ScrollingListener mScrollingListener = new ScrollingListener() {
        public void onStarted() {
            isScrollingPerformed = true;
            notifyScrollingListenersAboutStart();
        }

        public void onScroll(int distance) {
            doScroll(distance);

            int height = getHeight();
            if (mScrollingOffset > height) {
                mScrollingOffset = height;
                mWheelScroller.stopScrolling();
            } else if (mScrollingOffset < -height) {
                mScrollingOffset = -height;
                mWheelScroller.stopScrolling();
            }
        }

        public void onFinished() {
            if (isScrollingPerformed) {
                notifyScrollingListenersAboutEnd();
                isScrollingPerformed = false;
            }

            mScrollingOffset = 0;
            invalidate();
        }

        public void onJustify() {
            if (!isCyclic && getCurrentItem() < mViewAdapter.getValidStart()) {
                scroll(mViewAdapter.getValidStart() - getCurrentItem(), 0);
            } else if (!isCyclic && getCurrentItem() > mViewAdapter.getValidEnd()) {
                scroll(mViewAdapter.getValidEnd() - getCurrentItem(), 0);
            } else if (Math.abs(mScrollingOffset) > ScrollTextViewScroller.MIN_DELTA_FOR_SCROLLING) {
                mWheelScroller.scroll(mScrollingOffset, 0);
            }
        }
    };

    /**
     * Set the the specified scrolling interpolator
     * 设置滑动时的插值器
     * @param interpolator the interpolator
     */
    public void setInterpolator(Interpolator interpolator) {
        mWheelScroller.setInterpolator(interpolator);
    }

    /**
     * Gets count of visible items
     * 获得可视项的个数
     * @return the count of visible items
     */
    public int getVisibleItems() {
        return mVisibleItems;
    }

    /**
     * Sets the desired count of visible items. Actual amount of visible items
     * depends on scrolltextview layout parameters. To apply changes and rebuild view
     * call measure().
     * 设置可视项的个数
     * @param count the desired count for visible items
     */
    public void setVisibleItems(int count) {
        mVisibleItems = count;
    }

    /**
     * Gets view adapter
     * 获得视图适配器
     * @return the view adapter
     */
    public ScrollTextViewAdapter getViewAdapter() {
        return mViewAdapter;
    }

    /**
     * Sets view adapter. Usually new adapters contain different views, so it
     * needs to rebuild view by calling measure().
     * 
     * @param viewAdapter the view adapter
     */
    private void setViewAdapter(ScrollTextViewAdapter viewAdapter) {
        mViewAdapter = viewAdapter;

        invalidate();
    }

    /**
     * Adds scrolltextview changing listener
     * 添加ScrollTextView内容改变时的监听
     * @param listener the listener
     */
    public void addChangingListener(OnScrollTextViewChangedListener listener) {
        mChangingListeners.add(listener);
    }

    /**
     * Removes scrolltextview changing listener
     * 删除ScrollTextView内容改变时的监听
     * @param listener the listener
     */
    public void removeChangingListener(OnScrollTextViewChangedListener listener) {
        mChangingListeners.remove(listener);
    }

    /**
     * Notifies changing listeners
     * 
     * @param oldValue the old item id value
     * @param newValue the new item id value
     */
    protected void notifyChangingListeners(int oldValue, int newValue) {
        for (OnScrollTextViewChangedListener listener : mChangingListeners) {
            listener.onChanged(this, oldValue, newValue);
        }
    }

    /**
     * Adds scrolltextview scrolling listener
     * 添加ScrollTextView滑动的监听
     * @param listener the listener
     */
    public void addScrollingListener(OnScrollTextViewScrollListener listener) {
        mScrollingListeners.add(listener);
    }

    /**
     * Removes scrolltextview scrolling listener
     * 删除ScrollTextView滑动的监听
     * @param listener the listener
     */
    public void removeScrollingListener(OnScrollTextViewScrollListener listener) {
        mScrollingListeners.remove(listener);
    }

    /**
     * Notifies listeners about starting scrolling
     */
    protected void notifyScrollingListenersAboutStart() {
        for (OnScrollTextViewScrollListener listener : mScrollingListeners) {
            listener.onScrollingStarted(this);
        }
    }

    /**
     * Notifies listeners about ending scrolling
     */
    protected void notifyScrollingListenersAboutEnd() {
        if (mDataInterface != null) {
            mDataInterface.onChanged(this, 0, getCurrentItem());
        }
        for (OnScrollTextViewScrollListener listener : mScrollingListeners) {
            listener.onScrollingFinished(this);
        }
    }

    /**
     * Adds scrolltextview clicking listener
     * 添加ScrollTextView点击事件的监听
     * @param listener the listener
     */
    public void addClickingListener(OnScrollTextViewClickedListener listener) {
        mClickingListeners.add(listener);
    }

    /**
     * Removes scrolltextview clicking listener
     * 删除ScrollTextView点击事件的监听
     * @param listener the listener
     */
    public void removeClickingListener(OnScrollTextViewClickedListener listener) {
        mClickingListeners.remove(listener);
    }

    /**
     * Notifies listeners about clicking
     */
    protected void notifyClickListenersAboutClick(int item) {
        setCurrentItem(item, true);

        for (OnScrollTextViewClickedListener listener : mClickingListeners) {
            listener.onItemClicked(this, item);
        }
    }

    /**
     * 获取当前选中item的id
     * @return the current value
     */
    public int getCurrentItem() {
        return mCurrentItem;
    }

    /**
     * Sets the current item. Does nothing when index is wrong.
     * @param index the item index
     * @param animated the animation flag
     */
    public void setCurrentItem(int index, boolean animated) {
        if (mViewAdapter == null || mViewAdapter.getItemsCount() == 0) {
            return; // throw?
        }

        int itemCount = mViewAdapter.getItemsCount();
        if (index < 0 || index >= itemCount) {// check index
            if (isCyclic) {
                while (index < 0) {
                    index += itemCount;
                }
                index %= itemCount;
            } else {
                return; // throw?
            }
        }

        if (index != mCurrentItem) {
            if (animated) {
                int itemsToScroll = index - mCurrentItem;
                if (isCyclic) {
                    int scroll = itemCount + Math.min(index, mCurrentItem)
                            - Math.max(index, mCurrentItem);
                    if (scroll < Math.abs(itemsToScroll)) {
                        itemsToScroll = itemsToScroll < 0 ? scroll : -scroll;
                    }
                }
                scroll(itemsToScroll, 0);
            } else {
                mScrollingOffset = 0;

                int old = mCurrentItem;
                mCurrentItem = index;

                notifyChangingListeners(old, mCurrentItem);

                invalidate();
            }
        }
    }

    /**
     * Sets the current item w/o animation. Does nothing when index is wrong.
     * 设置当前选中项
     * @param index the item index
     */
    public void setCurrentItem(int index) {
        setCurrentItem(index, false);
    }

    /**
     * Tests if scrolltextview is cyclic. That means before the 1st item there is shown
     * the last one
     * 判断ScrollTextView的内容是否可以循环滑动
     * @return true if scrolltextview is cyclic
     */
    public boolean isCyclic() {
        return isCyclic;
    }

    /**
     * Set scrolltextview cyclic flag
     * 设置ScrollTextView的内容是否可以循环滑动
     * @param isCyclic the flag to set
     */
    public void setCyclic(boolean isCyclic) {
        this.isCyclic = isCyclic;
        invalidate();
    }

    /**
     * Returns height of normal item
     * @return the item height
     */
    private int getItemHeight() {
        return (int) mNormalItemHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) ((mVisibleItems - 1) * mNormalItemHeight + mSelectItemHeight);
        setMeasuredDimension(widthMeasureSpec, height);
    }

    /**
     * set the text horizontal offset in ScrollTextView
     *  在一定时间内滑动到指定的item
     * @param offset
     */
    public void setHorizontalOffset(int offset) {
        mOffsetX = offset;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = getBitmap(getWidth(), getHeight());
        if (mIsBitmapChanged) {
            mTmpCanvas.setBitmap(bitmap);
        }

        if (mViewAdapter != null && mViewAdapter.getItemsCount() > 0) {
            rebuildItems();
            drawItems(mTmpCanvas);
        }

        if (mIsDrawFading) {
            drawVerticalFading(mTmpCanvas);
        }

        canvas.drawBitmap(bitmap, 0, 0, mBitmapPaint);
    }

    private Canvas mTmpCanvas = new Canvas();
    private Bitmap mTmpBitmap;
    private boolean mIsBitmapChanged = false;
    private Bitmap getBitmap(int width, int height) {
        if (mTmpBitmap == null) {
            mTmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mIsBitmapChanged = true;
        } else if (mTmpBitmap.getWidth() != width || mTmpBitmap.getHeight() != height) {
            mTmpBitmap.recycle();
            mTmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mIsBitmapChanged = true;
        } else {
            mIsBitmapChanged = false;
        }

        mTmpBitmap.eraseColor(Color.TRANSPARENT);

        return mTmpBitmap;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if(View.INVISIBLE == visibility && mTmpBitmap != null) {
            mTmpBitmap = null;
        }
    }

    private void drawVerticalFading(Canvas canvas) {
        mFadingMatrix.setScale(1, mFadingHeight);
        mFadingShader.setLocalMatrix(mFadingMatrix);
        canvas.drawRect(0, 0, getWidth(), mFadingHeight, mFadingPaint);

        mFadingMatrix.setScale(1, mFadingHeight);
        mFadingMatrix.postRotate(180);
        mFadingMatrix.postTranslate(0, getHeight());
        mFadingShader.setLocalMatrix(mFadingMatrix);
        canvas.drawRect(0, getHeight() - mFadingHeight, getWidth(), getHeight(), mFadingPaint);
    }

    /*
     * 画items
     * @param canvas 画布
     */
    private void drawItems(Canvas canvas) {
        float yoff = 0;
        int top = (mCurrentItem - mFirstItem) * getItemHeight()
                + ((int) mSelectItemHeight - getHeight()) / 2;

        float dy = -top + mScrollingOffset - getItemHeight();
        canvas.translate(mOffsetX, dy);

        int scrolloff = mScrollingOffset > 0 ? mScrollingOffset
                : getItemHeight() + mScrollingOffset;
        float k = (scrolloff * 1.0f) / getItemHeight();

        yoff = dy;
        dy = 0;
        for (int i = 0; i < mRange.getCount(); i++) {
            dy = configTextView(i, k);
            canvas.translate(0, dy);

            yoff += dy;

            String text = getItemText(i);

            float baseline = mNormalItemHeight / 2 - mFontMetricsCenterY;
            canvas.drawText(text, getWidth()/2, baseline,
                    mTextPaint);

        }

        canvas.translate(-mOffsetX, -yoff);
    }

    /*
     * 获取指定的time的字符
     * @param i
     * @return
     */
    private String getItemText(int i) {
        int t = i + mFirstItem;

        String s = mViewAdapter.getItemText(t);
        if (t < 0) {
            t = mViewAdapter.getItemsCount() + t;
            s = isCyclic ? mViewAdapter.getItemText(t) : "";
        } else if (t >= mViewAdapter.getItemsCount()) {
            t = t - mViewAdapter.getItemsCount();
            s = isCyclic ? mViewAdapter.getItemText(t) : "";
        }

        if (s == null) {
            s = "";
        }

        return s;
    }

    /*
     * 配置item的字体颜色/大小/间距
     * @param index 指定item的id
     * @param scale 滑动距离和item高度的比例值
     * @return
     */
    private float configTextView(int index, float scale) {
        float dy = getItemHeight();
        int gap = (int) (mSelectItemHeight - mNormalItemHeight);
        int selectItemId = mVisibleItems / 2;

        float k = 0;
        if (index < selectItemId) {
            k = scale;
        } else if (index == selectItemId) {
            dy += gap * scale / 2;
            k = scale;
        } else if (index == selectItemId + 1) {
            dy += gap / 2;
            k = 1 - scale;
        } else if (index == selectItemId + 2) {
            dy += gap * (1 - scale) / 2;
            k = 1 - scale;
        } else {
            k = 1 - scale;
        }

        computeTextSizeAndColor(index, k);

        return dy;
    }

    private void computeTextSizeAndColor(int index, float scale) {
        int selectItemId = mVisibleItems / 2;
        int selectColor = mSelectTextColor;
        int normalColor = mNormalTextColor;
        float selectTextSize = mSelectTextSize;
        float normalTextSize = mNormalTextSize;

        if (index >= selectItemId && index <= selectItemId + 1) {
            mFontMetricsCenterY = (mNormalFontMetricsCenterY + (mSelectFontMetricsCenterY - mNormalFontMetricsCenterY) * scale);
        } else {
            mFontMetricsCenterY = mNormalFontMetricsCenterY;
        }

        if (index > selectItemId) {
            index = mVisibleItems - index;
        }
        if (index > selectItemId) {
            index = selectItemId;
        }
        if (index < 0) {
            index = 0;
        }
        if (index == 0) {
            selectColor = mNormalTextColors.get(index);
            normalColor = mNormalTextColors.get(index);
            selectTextSize = mNormalTextSizes.get(index);
            normalTextSize = mNormalTextSizes.get(index);
            scale = 0.0f;
        } else if(index < selectItemId) {
            selectColor = mNormalTextColors.get(index);
            normalColor = mNormalTextColors.get(index - 1);
            selectTextSize = mNormalTextSizes.get(index);
            normalTextSize = mNormalTextSizes.get(index - 1);
        } else {
            selectColor = mSelectTextColor;
            normalColor = mNormalTextColors.get(index - 1);
            selectTextSize = mSelectTextSize;
            normalTextSize = mNormalTextSizes.get(index - 1);
        }
        int selectalpha = Color.alpha(selectColor);
        int slecetred = Color.red(selectColor);
        int slecetgreen = Color.green(selectColor);
        int slecetblue = Color.blue(selectColor);

        int unselectalpha = Color.alpha(normalColor);
        int unslecetred = Color.red(normalColor);
        int unslecetgreen = Color.green(normalColor);
        int unslecetblue = Color.blue(normalColor);

        int a = (int) (unselectalpha + (int) ((selectalpha - unselectalpha) * scale));
        int r = unslecetred + (int) ((slecetred - unslecetred) * scale);
        int g = unslecetgreen + (int) ((slecetgreen - unslecetgreen) * scale);
        int b = unslecetblue + (int) ((slecetblue - unslecetblue) * scale);
        int color = Color.argb(a, r, g, b);

        mTextPaint.setColor(color);
        mTextPaint.setTextSize(normalTextSize + (selectTextSize - normalTextSize) * scale);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || getViewAdapter() == null) {
            return true;
        }

        switch (event.getAction()) {
        case MotionEvent.ACTION_MOVE:
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(mParentRequestDisallowInterceptTouchEvent);
            }
            break;

        case MotionEvent.ACTION_UP:
            if (!isScrollingPerformed) {
                int distance = (int) event.getY() - getHeight() / 2;
                if (distance < 0) {
                    distance += (mSelectItemHeight / 2 - getItemHeight());
                } else {
                    distance -= (mSelectItemHeight / 2 - getItemHeight());
                }
                int items = distance / getItemHeight();

                if (items != 0 && isValidItemIndex(mCurrentItem + items)) {
                    notifyClickListenersAboutClick(mCurrentItem + items);
                }
            }
            break;
        }

        return mWheelScroller.onTouchEvent(event);
    }

    private int getYScrollEnd() {
        int end = 0;

        if (isCyclic) {
            end = DEF_YSCROLL_END;
        } else {
            end = mScrollingOffset
                    + (int) ((getScrollEndItem() - getCurrentItem()) * mNormalItemHeight);
        }

        return end;
    }

    private int getYScrollStart() {
        int start = 0;

        if (isCyclic) {
            start = -DEF_YSCROLL_END;
        } else {
            start = mScrollingOffset
                    + (int) ((getScrollStartItem() - getCurrentItem()) * mNormalItemHeight);
        }

        return start;
    }

    private int getScrollEndItem() {
        int index = 0;
        int itemCount = mViewAdapter.getItemsCount();// item总数

        if (isCyclic)
            return index;

        if (itemCount <= mVisibleItems) {
            index = itemCount - 1;
        } else {
            //index = itemCount - 1 - mVisibleItems / 2;
            // 修复不循环滑动时，无法选择最后一个日期问题
            index = itemCount - 1 ;
        }

        return index;
    }

    private int getScrollStartItem() {
        int index = 0;
        int itemCount = mViewAdapter.getItemsCount();// item总数

        if (isCyclic)
            return index;

        if (itemCount <= mVisibleItems) {
            index = 0;
        } else {
            //index = mVisibleItems / 2;
            // 修复不循环滑动时，无法选择第一个日期问题
            index = 0;
        }

        return index;
    }

    /*
     * Scrolls the textview
     * @param delta the scrolling value
     */
    private void doScroll(int delta) {

        int itemCount = mViewAdapter.getItemsCount();// item总数
        if (itemCount == 1) {
            mScrollingOffset = 0;
        } else {
            mScrollingOffset += delta;
        }

        int itemHeight = getItemHeight();
        int count = mScrollingOffset / itemHeight;// 滑动几个item

        int pos = mCurrentItem - count;// 滑动后的位置

        int fixPos = mScrollingOffset % itemHeight;// 调整距离
        if (Math.abs(fixPos) <= itemHeight / 2) {
            fixPos = 0;
        }

        if (isCyclic && itemCount > 0) {
            if (fixPos > 0) {
                pos--;
                count++;
            } else if (fixPos < 0) {
                pos++;
                count--;
            }
            // fix position by rotating
            while (pos < 0) {
                pos += itemCount;
            }
            pos %= itemCount;
        } else {
            if (pos < getScrollStartItem()) {// item小于0时不允许向下滑动
                count = mCurrentItem - getScrollStartItem();
                pos = getScrollStartItem();
                mScrollingOffset = 0;
            } else if (pos > getScrollEndItem()) {// item大于itemCount - 1时不允许向上滑动
                count = mCurrentItem - getScrollEndItem();// 滑动个数
                pos = getScrollEndItem();// 最后位置
                mScrollingOffset = 0;
            } else if (pos > getScrollStartItem() && fixPos > 0) {
                pos--;
                count++;
            } else if (pos < getScrollEndItem() && fixPos < 0) {
                pos++;
                count--;
            } else if (pos == getScrollEndItem()) {
                if (mScrollingOffset < 0) {// item等于itemCount - 1时不允许向上滑动
                    mScrollingOffset = 0;
                }
            } else if (pos == getScrollStartItem()) {// item等于0时不允许向下滑动
                if (mScrollingOffset > 0) {
                    mScrollingOffset = 0;
                }
            }
        }

        int offset = mScrollingOffset;
        if (pos != mCurrentItem) {
            setCurrentItem(pos, false);
        } else {
            invalidate();
        }

        // update offset
        mScrollingOffset = offset - count * itemHeight;
        if (mScrollingOffset > getHeight() && getHeight() != 0) {
            mScrollingOffset = mScrollingOffset % getHeight() + getHeight();
        }
    }

    /**
     * Scroll the item
     * 在一定时间内滑动到指定的item
     * @param itemsToScroll items to scroll
     * @param time scrolling duration
     */
    public void scroll(int itemsToScroll, int time) {
        int distance = itemsToScroll * getItemHeight() + mScrollingOffset;
        mWheelScroller.scroll(distance, time);
    }

    /*
     * Calculates range for scrolltextview items
     * 
     * @return the items range
     */
    private VisibleItemsRange getItemsRange() {
        if (getItemHeight() == 0) {
            return null;
        }

        int first = mCurrentItem;
        int count = 1;

        while ((count + 2) * getItemHeight() < getHeight()) {
            first--;
            count += 2; // top + bottom items
        }

        if (mScrollingOffset != 0) {
            if (mScrollingOffset > 0) {
                first--;
            }
            count++;

            // process empty items above the first or below the second
            int emptyItems = mScrollingOffset / getItemHeight();
            first -= emptyItems;
            count += Math.asin(emptyItems);
        }

        return mRange.update(first, count);
    }

    /*
     * Rebuilds items if necessary. Caches all unused items.
     * @return true if items are rebuilt
     */
    private boolean rebuildItems() {
        boolean updated = false;
        mRange = getItemsRange();

        if (mFirstItem > mRange.getFirst() && mFirstItem <= mRange.getLast()) {
            for (int i = mFirstItem - 1; i >= mRange.getFirst(); i--) {
                mFirstItem = i;
            }
        } else {
            mFirstItem = mRange.getFirst();
        }

        return updated;
    }

    private void computeFontMetrics() {
        mTextPaint.setTextSize(mSelectTextSize);
        FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
        mSelectFontMetricsCenterY = (fontMetrics.bottom + fontMetrics.top) / 2;

        mTextPaint.setTextSize(mNormalTextSize);
        fontMetrics = mTextPaint.getFontMetricsInt();
        mNormalFontMetricsCenterY = (fontMetrics.bottom + fontMetrics.top) / 2;
    }

    /*
     * Checks whether intem index is valid
     * 
     * @param index the item index
     * @return true if item index is not out of bounds or the scrolltextview is cyclic
     */
    private boolean isValidItemIndex(int index) {
        return mViewAdapter != null
                && mViewAdapter.getItemsCount() > 0
                && (isCyclic || index >= 0
                        && index < mViewAdapter.getItemsCount());
    }

    /**
     * Stops scrolling
     */
    public void stopScrolling() {
        if (mWheelScroller != null) {
            mWheelScroller.stopScrolling();
        }
    }

    /**
     * set the text color of scrolling textView ps:if don't using method
     * setTextColor to set the color, using the default text color; the color
     * must be the color after using method getColor and is't the resource id
     * 
     * @param selectedColor text color of selected textView
     * @param normalColor text color of unselected textView
     * @hide
     **/
    public void setTextColor(int selectedColor, int normalColor) {
        mSelectTextColor = selectedColor;
        mNormalTextColor = normalColor;

        mNormalTextColors = new ArrayList<>();
        int normalItemCount = mVisibleItems - 1;
        for (int i = 0; i < normalItemCount/2; i++) {
            mNormalTextColors.add(new Integer(mNormalTextColor));
        }

        invalidate();
    }

    public void setTextColor(int selectedColor, List<Integer> normalTextColors) {
        mSelectTextColor = selectedColor;
        if (normalTextColors != null && mNormalTextColors != normalTextColors) {
            mNormalTextColor = normalTextColors.get(0);
            mNormalTextColors = new ArrayList<>();
            int normalItemCount = mVisibleItems - 1;
            for (int i = 0; i < normalItemCount/2; i++) {
                int size = normalTextColors.size();
                if (i < size) {
                    mNormalTextColors.add(new Integer(normalTextColors.get(i).intValue()));
                } else {
                    mNormalTextColors.add(new Integer(normalTextColors.get(size - 1).intValue()));
                }
            }
        }

        invalidate();
    }

    public void setSelectTextColor(int selectedColor) {
        if (mSelectTextColor == selectedColor)
            return;
        setTextColor(selectedColor, mNormalTextColors);
    }

    public void setNormalTextColor(int normalTextColor) {
        if (mNormalTextColor == normalTextColor)
            return;
        setTextColor(mSelectTextColor, normalTextColor);
    }

    public void setNormalTextColor(List<Integer> normalTextColors) {
        if (normalTextColors != null)
            return;
        setTextColor(mSelectTextColor, normalTextColors);
    }

    /**
     * set the text size and the height of scrolling textView
     * 设置ScrollTextView内容的字体大小、选项高度
     * @param selectedSize text size of selected textView
     * @param normalSize text size of unselected textView
     * @param selectHeight height of selected textView
     * @param normalHeight height of unselected textView
     **/
    public void setTextPreference(float selectedSize, float normalSize, float selectHeight, float normalHeight) {

        mSelectItemHeight = selectHeight;
        mNormalItemHeight = normalHeight;
        mSelectTextSize = selectedSize;
        mNormalTextSize = normalSize;

        mNormalTextSizes = new ArrayList<>();
        int normalItemCount = mVisibleItems - 1;
        for (int i = 0; i < normalItemCount/2; i++) {
            mNormalTextSizes.add(new Float(mNormalTextSize));
        }

        computeFontMetrics();
        invalidate();
    }

    /**
     * 设置字体大小
     * 
     * @param selectedSize 选中字体大小
     * @param normalSize 非选中字体大小
     */
    public void setTextSize(float selectedSize, float normalSize) {
        setTextPreference(selectedSize, normalSize, mSelectItemHeight, mNormalItemHeight);
    }

    /**
     * 设置非选中字体大小
     *
     * @param normalSize 非选中字体大小
     */
    public void setNormalTextSize(float normalSize) {
        setTextPreference(mSelectTextSize, normalSize, mSelectItemHeight, mNormalItemHeight);
    }

    /**
     * 设置选中字体大小
     *
     * @param selectedSize 选中字体大小
     */
    public void setSelectTextSize(float selectedSize) {
        setTextPreference(selectedSize, mNormalTextSize, mSelectItemHeight, mNormalItemHeight);
    }

    /**
     * 设置选项高度
     * @param selectHeight 选中项高度
     * @param normalHeight 非选中项高度
     */
    public void setItemHeight(float selectHeight, float normalHeight) {
        setTextPreference(mSelectTextSize, mNormalTextSize, selectHeight, normalHeight);
    }

    /**
     * 设置选中项高度
     * @param selectHeight 选中项高度
     */
    public void setSelectItemHeight(float selectHeight) {
        setTextPreference(mSelectTextSize, mNormalTextSize, selectHeight, mNormalItemHeight);
    }

    /**
     * 设置非选中项高度
     * @param normalHeight 非选中项高度
     */
    public void setNormalItemHeight(float normalHeight) {
        setTextPreference(mSelectTextSize, mNormalTextSize, mSelectItemHeight, normalHeight);
    }

    /**
     * 设置数据适配器
     * @param adapter
     */
    public void setIDataAdapter(IDataAdapter adapter) {
        mDataInterface = adapter;
    }

    /**
     * 获取数据适配器
     * @return
     */
    public IDataAdapter getIDataAdapter() {
        return mDataInterface;
    }

    public int getItemsCount() {
        return mViewAdapter.getItemsCount();
    }

    /**
     * 设置字体类型
     * @param font
     */
    public void setTypeface(Typeface font) {
        mTextPaint.setTypeface(font);
        computeFontMetrics();
        invalidate();
    }

    /**
     * 设置是否绘制边界阴影
     * @param isDrawFading
     */
    public void setIsDrawFading(boolean isDrawFading) {
        mIsDrawFading = isDrawFading;
    }

    /*
     * 可视item的范围
     */
    private class VisibleItemsRange {
        // 第一个需显示的item的id
        private int first;

        // 需显示的Item个数
        private int count;

        /*
         * 默认构造函数，创建一个空的range
         */
        public VisibleItemsRange() {
            this(0, 0);
        }

        /*
         * Constructor
         * @param first 第一个可视的item的id
         * @param count 可视的item的个数
         */
        public VisibleItemsRange(int first, int count) {
            this.first = first;
            this.count = count;
        }

        /*
         * Gets number of first item
         * @return the number of the first item
         */
        public int getFirst() {
            return first;
        }

        /*
         * 获取最后可显示的item的id
         * @return 最后可显示的item的id
         */
        public int getLast() {
            return getFirst() + getCount() - 1;
        }

        /*
         * 获取可视item个数
         * @return 可视item个数
         */
        public int getCount() {
            return count;
        }

        /*
         * 更新可视item数据
         * @param first 第一个可视的item的id
         * @param count 可视的item的个数
         */
        public VisibleItemsRange update(int first, int count) {
            this.first = first;
            this.count = count;
            
            return this;
        }
    }

    private class ScrollTextViewAdapter {

        /* The default min value */
        public static final int DEFAULT_MAX_VALUE = 9;

        /* The default max value */
        private static final int DEFAULT_MIN_VALUE = 0;

        // 有效可选行的第一行id
        private int validStart = 0;
        // 有效可选行的最后一行id
        private int validEnd = 0;
        // item的总数目
        private int count = 0;

        /**
         * Constructor
         */
        public ScrollTextViewAdapter() {
            this(DEFAULT_MAX_VALUE - DEFAULT_MIN_VALUE + 1, DEFAULT_MIN_VALUE,
                    DEFAULT_MAX_VALUE);
        }

        /*
         * Constructor
         * @param validStart 可选中item的有效起始位置
         * @param validEnd 可选中item的
         */
        public ScrollTextViewAdapter(int validStart, int validEnd) {
            this(validEnd - validStart + 1, validStart, validEnd);
        }

        /*
         * Constructor
         * @param count item总数
         * @param validStart 可选中item的有效起始位置
         * @param validEnd 可选中item的
         */
        public ScrollTextViewAdapter(int count, int validStart, int validEnd) {
            update(count, validStart, validEnd);
        }

        /*
         * Returns text for specified item
         * 
         * @param index the item index
         * @return the text of specified items
         */
        public String getItemText(int index) {
            if (index >= 0 && index < getItemsCount() && mDataInterface != null) {
                return mDataInterface.getItemText(index);
            }

            return null;
        }

        public void setItemCount(int count) {
            this.count = count;
        }

        public int getItemsCount() {
            return count;
        }

        public ScrollTextViewAdapter update(int count, int validStart, int validEnd) {
            this.validStart = validStart;
            this.validEnd = validEnd;
            this.count = count;

            return this;
        }
        
        public int getValidStart() {
            return validStart;
        }
        
        public int getValidEnd() {
            return validEnd;
        }

    }

    /*
     * Scrolling listener interface
     */
    private interface ScrollingListener {
        /*
         * Scrolling callback called when scrolling is performed.
         * @param distance the distance to scroll
         */
        void onScroll(int distance);

        /*
         * Starting callback called when scrolling is started
         */
        void onStarted();
        
        /*
         * Finishing callback called after justifying
         */
        void onFinished();
        
        /*
         * Justifying callback called to justify a view when scrolling is ended
         */
        void onJustify();
    }

    /*
     * 该类用来处理滑动事件 
     */
    private class ScrollTextViewScroller {
        /* Scrolling duration */
        private static final int SCROLLING_DURATION = 400;

        /* Minimum delta for scrolling */
        public static final int MIN_DELTA_FOR_SCROLLING = 1;

        // Listener
        private ScrollingListener listener;

        // Context
        private Context context;

        // Scrolling
        private GestureDetector gestureDetector;
        private Scroller scroller;
        private int lastScrollY;
        private float lastTouchedY;
        private boolean isScrollingPerformed;

        /*
         * Constructor
         * @param context the current context
         * @param listener the scrolling listener
         * @param scrollTextView 
         */
        public ScrollTextViewScroller(Context context, ScrollingListener listener) {
            gestureDetector = new GestureDetector(context, gestureListener);
            gestureDetector.setIsLongpressEnabled(false);

            scroller = new Scroller(context);

            this.listener = listener;
            this.context = context;
        }

        /*
         * Set the the specified scrolling interpolator
         * @param interpolator the interpolator
         */
        public void setInterpolator(Interpolator interpolator) {
            scroller.forceFinished(true);
            scroller = new Scroller(context, interpolator);
        }

        /*
         * Scroll the scrolltextview
         * @param distance the scrolling distance
         * @param time the scrolling duration
         */
        public void scroll(int distance, int time) {
            scroller.forceFinished(true);

            lastScrollY = 0;
            scroller.startScroll(0, 0, 0, distance, time != 0 ? time : SCROLLING_DURATION);
            setNextMessage(MESSAGE_SCROLL);

            startScrolling();
        }

        /*
         * Stops scrolling
         */
        public void stopScrolling() {
            scroller.forceFinished(true);
        }

        /*
         * Handles Touch event 
         * @param event the motion event
         * @return
         */
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchedY = event.getY();
                    scroller.forceFinished(true);
                    clearMessages();
                    break;
        
                case MotionEvent.ACTION_MOVE:
                    // perform scrolling
                    int distanceY = (int)(event.getY() - lastTouchedY);
                    if (distanceY != 0) {
                        startScrolling();
                        listener.onScroll(distanceY);
                        lastTouchedY = event.getY();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    justify();
                    break;
            }

            if (!gestureDetector.onTouchEvent(event) && event.getAction() == MotionEvent.ACTION_UP) {
                justify();
            }

            return true;
        }

        // gesture listener
        private SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Do scrolling in onTouchEvent() since onScroll() are not call immediately
                //  when user touch and move the scrolltextview
                return true;
            }

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                lastScrollY = 0;
                final int maxY = getYScrollEnd();
                final int minY = getYScrollStart();

                scroller.fling(0, lastScrollY, 0, (int) -velocityY, 0, 0, minY, maxY);
                setNextMessage(MESSAGE_SCROLL);
                return true;
            }
        };

        // Messages
        private final int MESSAGE_SCROLL = 0;
        private final int MESSAGE_JUSTIFY = 1;

        /*
         * Set next message to queue. Clears queue before.
         * @param message the message to set
         */
        private void setNextMessage(int message) {
            clearMessages();
            animationHandler.sendEmptyMessage(message);
        }

        /*
         * Clears messages from queue
         */
        private void clearMessages() {
            animationHandler.removeMessages(MESSAGE_SCROLL);
            animationHandler.removeMessages(MESSAGE_JUSTIFY);
        }

        // animation handler
        private Handler animationHandler = new AnimationHandler(this);

        /*
         * Justifies item position
         */
        private void justify() {
            listener.onJustify();
            setNextMessage(MESSAGE_JUSTIFY);
        }

        /*
         * Starts scrolling
         */
        private void startScrolling() {
            if (!isScrollingPerformed) {
                isScrollingPerformed = true;
                listener.onStarted();
            }
        }

        /*
         * Finishes scrolling
         */
        void finishScrolling() {
            if (isScrollingPerformed) {
                listener.onFinished();
                isScrollingPerformed = false;
            }
        }
    }

    private static class AnimationHandler extends Handler{
        private final WeakReference<ScrollTextViewScroller> mScrollTextViewScroller;

        public AnimationHandler(ScrollTextViewScroller scrollTextViewScroller) {
            mScrollTextViewScroller = new WeakReference<ScrollTextViewScroller>(scrollTextViewScroller);
        }

        public void handleMessage(Message msg) {
            ScrollTextViewScroller scrollTextViewScroller = mScrollTextViewScroller.get();
            if (scrollTextViewScroller != null) {
                scrollTextViewScroller.scroller.computeScrollOffset();// 计算滑动位置
                int currY = scrollTextViewScroller.scroller.getCurrY();
                int delta = scrollTextViewScroller.lastScrollY - currY;
                scrollTextViewScroller.lastScrollY = currY;
                if (delta != 0) {
                    scrollTextViewScroller.listener.onScroll(delta);
                }

                // scrolling is not finished when it comes to final Y
                // so, finish it manually
                if (Math.abs(currY - scrollTextViewScroller.scroller.getFinalY()) < ScrollTextViewScroller.MIN_DELTA_FOR_SCROLLING) {
                    currY = scrollTextViewScroller.scroller.getFinalY();
                    scrollTextViewScroller.scroller.forceFinished(true);
                }
                if (!scrollTextViewScroller.scroller.isFinished()) {
                    scrollTextViewScroller.animationHandler.sendEmptyMessage(msg.what);
                } else if (msg.what == scrollTextViewScroller.MESSAGE_SCROLL) {
                    scrollTextViewScroller.justify();
                } else {
                    scrollTextViewScroller.finishScrolling();
                }
            }
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ScrollTextView.class.getName());
    }


    public void setNormalTextSize(List<Float> normalTextSizes) {
        if (normalTextSizes != null) {
            setTextPreference(mSelectTextSize, normalTextSizes, mSelectItemHeight, mNormalItemHeight);
        }
    }

    public void setTextSize(float selectTextSize, List<Float> normalTextSizes) {
        if (selectTextSize == mSelectTextSize && normalTextSizes == null) {
            return;
        }
        setTextPreference(selectTextSize, normalTextSizes, mSelectItemHeight, mNormalItemHeight);
    }

    /**
     * set the text size and the height of scrolling textView
     * 设置ScrollTextView内容的字体大小、选项高度
     * @param selectedSize text size of selected textView
     * @param normalTextSizes text size of unselected textView
     * @param selectHeight height of selected textView
     * @param normalItemHeight height of unselected textView
     **/
    public void setTextPreference(float selectedSize, List<Float> normalTextSizes, float selectHeight, float normalItemHeight) {
        if (mSelectItemHeight == selectHeight
                && mNormalItemHeight == normalItemHeight
                && mSelectTextSize == selectedSize
                && normalTextSizes == null)
            return;

        mSelectItemHeight = selectHeight;
        mSelectTextSize = selectedSize;
        mNormalItemHeight = normalItemHeight;

        if (normalTextSizes != null && mNormalTextSizes != normalTextSizes) {
            mNormalTextSize = normalTextSizes.get(0);
            mNormalTextSizes = new ArrayList<>();
            int normalItemCount = mVisibleItems - 1;
            for (int i = 0; i < normalItemCount/2; i++) {
                int size = normalTextSizes.size();
                if (i < size) {
                    mNormalTextSizes.add(new Float(normalTextSizes.get(i).floatValue()));
                } else {
                    mNormalTextSizes.add(new Float(normalTextSizes.get(size - 1).floatValue()));
                }
            }
        }

        computeFontMetrics();
        invalidate();
    }

    /**
     * 设置阴影上下大小
     * @param fadingHeight
     */
    public void setFadingHeight(float fadingHeight) {
        if (this.mFadingHeight != fadingHeight) {
            this.mFadingHeight = fadingHeight;
            invalidate();
        }
    }

    /**
     * 设置是否不让父控件拦截事件
     * @param parentRequestDisallowInterceptTouchEvent
     */
    public void setParentRequestDisallowInterceptTouchEvent(boolean parentRequestDisallowInterceptTouchEvent) {
        mParentRequestDisallowInterceptTouchEvent = parentRequestDisallowInterceptTouchEvent;
    }

//    private static class SoudPoolHelper {
//        public SoundPool mSoundPool;
//        public boolean mIsFinishedLoad;
//        public int mVoiceID;
//        public Context mContext;
//
//        public int initSoundPool(Context context) {
//            mContext = context.getApplicationContext();
//            mIsFinishedLoad = false;
//            /**
//             * 21版本后，SoundPool的创建发生很大改变
//             */
//            //判断系统sdk版本，如果版本超过21，调用第一种
//            if (Build.VERSION.SDK_INT >= 21) {
//                SoundPool.Builder builder = new SoundPool.Builder();
//                builder.setMaxStreams(1);//传入音频数量
//                //AudioAttributes是一个封装音频各种属性的方法
//                AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
//                attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);//设置音频流的合适的属性
//                builder.setAudioAttributes(attrBuilder.build());//加载一个AudioAttributes
//                mSoundPool = builder.build();
//            } else {
//                mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
//            }
//
//            mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
//
//                // 判断声音是否加载完成（加载完成后会调用onLoadComplete方法）
//                @Override
//                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
//                    // TODO Auto-generated method stub
//                    mIsFinishedLoad = true;
//                }
//            });
//
//            //load的返回值是一个int类的值：音频的id，在SoundPool的play()方法中加入这个id就能播放这个音频
//            return mSoundPool.load(mContext, R.raw.mc_picker_scrolled, 1);
//        }
//
//        public void play() {
//            if (mIsFinishedLoad && mSoundPool != null) {
//                mSoundPool.play(mVoiceID, 0.5f, 0.5f, 0, 0, 1);
//            }
//        }
//
//        public void release() {
//            if (mIsFinishedLoad) {
//                mSoundPool.unload(mVoiceID);
//                mSoundPool.release();
//                mIsFinishedLoad = false;
//                mContext = null;
//            }
//        }
//
//        public void onAttachedToWindow(Context context) {
//            if (!mIsFinishedLoad) {
//                mVoiceID = initSoundPool(context);
//            }
//        }
//    }
//
//    @Override
//    protected void onAttachedToWindow() {
//        super.onAttachedToWindow();
//        if (mSoundPoolHelper != null) {
//            mSoundPoolHelper.onAttachedToWindow(mContext);
//        }
//    }
//
//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//        if (mSoundPoolHelper != null) {
//            mSoundPoolHelper.release();
//        }
//    }
//
//    private void playSelectedSound() {
//        if (mSoundPoolHelper != null) {
//            mSoundPoolHelper.play();
//        }
//    }

//    private class PlaySoundScrollTextViewChangedListener implements ScrollTextView.OnScrollTextViewChangedListener {
//        @Override
//        public void onChanged(ScrollTextView view, int oldValue, int newValue) {
//            playSelectedSound();
//        }
//    }
}
