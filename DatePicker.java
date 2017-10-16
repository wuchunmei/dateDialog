/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhudai.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.zhudai.R;
import com.zhudai.util.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A view for selecting a month / year / day based on a calendar like layout.
 * <p>日期选择器，继承FrameLayout</p>
 * <p>
 */
public class DatePicker extends FrameLayout {

    private static final int DEFAULT_START_YEAR = 2015;
    private static final int DEFAULT_END_YEAR = 2020;

    // This ignores Undecimber, but we only support real Gregorian calendars.
    private static final int NUMBER_OF_MONTHS = 12;

    private TextView mDayUnit;
    private TextView mMonthUnit;
    private TextView mYearUnit;
    private TextView mLeapUnit;

    /* UI Components */
//    private ScrollTextView mDayPicker;
    private ScrollTextView mMonthPicker;
    private ScrollTextView mYearPicker;

    private boolean isLunar = false;

    /**
     * How we notify users the date has changed.
     */
    private OnDateChangedListener mOnDateChangedListener;

    private int mDay;
    private int mMonth;
    private int mYear;

    private int mStartYear;
    private int mEndYear;

    private Calendar mStartCal;
    private Calendar mEndCal;

    //上一次的年份和上一次的月份，如果年份有变化月份重置
    private int mOldYear;
    private int mOldMonth;

    private String[] mMonths;
    String mOrder;

    private Object mMonthUpdateLock = new Object();
    private volatile Locale mMonthLocale;
    private String[] mShortMonths;
    /*
     * the position of the current focused row,default is -1 means that the row
     * is align center；normal position is [0, 4] if less then 0，that is 0，if
     * greater then 4, that is 4
     */
    private int mOneScreenCount = 3;
    private int mLayoutResId = R.layout.mc_date_picker;
    private int mYearOfMonths;
    private int mMonthOfDays;
    private float mNormalItemHeight;
    private float mSelectItemHeight;

    private int mWordSelectTextSize;
    private List<Float> mWordNormalTextSizes;
    private int mNumberSelectTextSize;
    private List<Float> mNumberNormalTextSizes;

    private LinearLayout mPickerHolder;
    private int mLineOneHeight;
    private int mLineTwoHeight;
    private int mWidthPadding;
    private Paint mLinePaint;
    private boolean mIsDrawLine;
    private boolean mIsAccessibilityEnable = false;

    String[] mLunarMouths;
    String[] mLunardays;
    String mLeap;

    boolean mIsLayoutRtl = false;
    private Typeface mZhTypeface; // 中文使用medium字重
    private Typeface mNumTpyeface; // 其他使用Din-pro-medium字重
    String[] mGregorianDays;

    /**
     * The callback used to indicate the user changes the date.
     */
    public interface OnDateChangedListener {

        /**
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility with
         *                    {@link java.util.Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateChanged(DatePicker view, int year, int monthOfYear,
                           int dayOfMonth);
    }

    private class DateAdapter implements ScrollTextView.IDataAdapter {
        final static int SET_YEAR = 1;
        final static int SET_MONTH = 2;
        final static int SET_DAY = 3;
        int mType = 0;

        DateAdapter(int i) {
            mType = i;
        }

        @Override
        public String getItemText(int position) {
            switch (mType) {
                case SET_YEAR:
                    return String.valueOf(position + mStartYear);

                case SET_MONTH:


                        if (mStartCal != null && mStartCal.get(Calendar.YEAR) == mYear) {
                            position += mStartCal.get(Calendar.MONTH);
                        }
                        if (position < mMonths.length) {
                            return mMonths[position];
                        }

                    break;

                case SET_DAY:

                        if (mStartCal != null && mStartCal.get(Calendar.YEAR) == mYear && mStartCal.get(Calendar.MONTH) == mMonth) {
                            return mGregorianDays[position + mStartCal.get(Calendar.DAY_OF_MONTH)];
                        } else {
                            return mGregorianDays[position + 1];
                        }


                default:
                    break;
            }

            return null;
        }

        @Override
        public void onChanged(View view, int fromOld, int toNew) {
            int maxdays = getMonthDays();
            int maxmonths = getYearMonths();

            switch (mType) {
                case SET_YEAR:
                    mYear = toNew + mStartYear;
                    if (mStartCal != null && mStartCal.get(Calendar.YEAR) == mYear) {
                        if (mMonth < mStartCal.get(Calendar.MONTH)) {
                            mMonth = mStartCal.get(Calendar.MONTH);
                        }
                    }

                    if (maxmonths != getYearMonths() && mMonthPicker != null) {
                        maxmonths = getYearMonths();

                        mYearOfMonths = maxmonths;
                        mMonthPicker.refreshCount(maxmonths);
                        if (maxmonths - 1 < mMonth) {  // 如果mMonth超出了范围，就让其等于最大的月份即可
                            mDay = maxdays;
                            mMonth = maxmonths - 1;
                            mMonthPicker.setCurrentItem(mMonth, true);
                        }

                    }

//                    if (maxdays != getMonthDays() && mDayPicker != null) {
//                        maxdays = getMonthDays();
//
//                        mMonthOfDays = maxdays;
//                        mDayPicker.refreshCount(maxdays);
//                        if (maxdays < mDay) {
//                            mDay = maxdays;
//                            mDayPicker.setCurrentItem(mDay - 1, true);
//                        }
//
//                    }
                    break;

                case SET_MONTH:
                    mMonth = toNew;
                    if (mStartCal != null && mStartCal.get(Calendar.YEAR) == mYear) {
                        mMonth += mStartCal.get(Calendar.MONTH);
                    }
//                    if (maxdays != getMonthDays() && mDayPicker != null) {
//                        maxdays = getMonthDays();
//
//                        mMonthOfDays = maxdays;
//                        mDayPicker.refreshCount(maxdays);
//                        if (maxdays < mDay) {
//                            mDay = maxdays;
//                            mDayPicker.setCurrentItem(mDay - 1, true);
//                        }
//
//                    }
                    break;
                case SET_DAY:
                    mDay = toNew + 1;
                    if (mStartCal != null && mStartCal.get(Calendar.YEAR) == mYear && mStartCal.get(Calendar.MONTH) == mMonth) {
                        mDay = toNew + mStartCal.get(Calendar.DAY_OF_MONTH);
                    }
                    break;
                default:
                    return;
            }

            setDayRange(mMonth);
            setMonthRange(mYear);

            if (mOnDateChangedListener != null) {
                mOnDateChangedListener.onDateChanged(DatePicker.this, mYear,
                        mMonth, mDay);
            }
//            if (mType == SET_MONTH) {
//                setLeapUnitVisibility(mMonth);
//            }
            sendAccessibilityEvent();
        }
    }

    private void setMonthRange(int year) {
        // 判断当前月是否在限制内
        if (mStartCal == null || mEndCal == null) {
            return;
        }
        if (mOldYear == year) {
            return;
        }
        mOldYear = year;
        mOldMonth = -1;

        if (mStartCal.get(Calendar.YEAR) <= year && mEndCal.get(Calendar.YEAR) >= year) {
            // 获取当前月内的日限制,时间限制在同一个月内
            int count = 0, currentItem = (mMonth - mStartCal.get(Calendar.MONTH)) < 0 ? 0 : mMonth - mStartCal.get(Calendar.MONTH);// mStartCal.get(Calendar.MONTH);
            int validStart = 0, validEnd = 0;
            boolean isCyclic = false;
            if (mStartCal.get(Calendar.YEAR) == year && mEndCal.get(Calendar.YEAR) == year) {
                //  等于　startCal  endCal
                count = mEndCal.get(Calendar.MONTH) - mStartCal.get(Calendar.MONTH) + 1;
                validStart = 0;
                validEnd = count;
            } else if (mStartCal.get(Calendar.YEAR) == year) {
                // 等于startCal  小于endCal
                count = mStartCal.getActualMaximum(Calendar.MONTH) + 1 - mStartCal.get(Calendar.MONTH);
                validStart = 0;
                validEnd = count;
            } else if (mEndCal.get(Calendar.YEAR) == year) {
                // 大于startCal 等于endCal
                currentItem = (mEndCal.get(Calendar.MONTH) - mMonth) < 0 ? 0 : mMonth;
                count = 1 + mEndCal.get(Calendar.MONTH);
                validStart = 0;
                validEnd = mEndCal.get(Calendar.MONTH);
            } else {
                // 大于startCal 小于endCal 在这个区间内，并且是循环状态下，就不再重新设置时间
                if(mMonthPicker.isCyclic()){
                    return;
                }
                currentItem = mMonth;
                count = mStartCal.getActualMaximum(Calendar.MONTH) + 1;
                validStart = 0;
                validEnd = mStartCal.getActualMaximum(Calendar.MONTH);
                isCyclic = true;
            }
            // 判断如果时满月状态，让它循环滑动
            if (count == mStartCal.getActualMaximum(Calendar.MONTH) + 1) {
                isCyclic = true;
            }
            mMonthPicker.setCyclic(isCyclic);
            mMonthPicker.refreshData(count, currentItem, validStart, validEnd);
        }
    }


    private void setDayRange(int month) {

        // 判断当前月是否在限制内
        if (mStartCal == null || mEndCal == null) {
            return;
        }
        if (mOldYear == mYear && mOldMonth == month) {
            return;
        }
        mOldMonth = month;
        Calendar currentCalendar = getCurrentCalendar();
        int count = getMonthDays(), currentItem = mDay - 1;
        int validStart = 0;
        int validEnd = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        boolean isCyclic = true;

        /** 不在开始，也不在结束的年份，不受限制: 2011-03-14, 2014-04-13  if当前mYear为2012,则不受任何限制
         * if 在2011,受开始年份限制，if在2014，则结束年份受限制
         */
        if (mStartCal.get(Calendar.YEAR) == mYear && mEndCal.get(Calendar.YEAR) != mYear) {
            // 当前年份和开始年份相等，和结束年份不相等，对开始年的月份进行限制
            if (mStartCal.get(Calendar.MONTH) == month) {
                currentItem = (currentItem - mStartCal.get(Calendar.DAY_OF_MONTH)) < 0 ? 0 : currentItem - mStartCal.get(Calendar.DAY_OF_MONTH) + 1;
                count = mStartCal.getActualMaximum(Calendar.DAY_OF_MONTH) - mStartCal.get(Calendar.DAY_OF_MONTH) + 1;
                validStart = 0;
                validEnd = count;
                if (count == mStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    isCyclic = true;
                } else {
                    isCyclic = false;
                }
            }
        } else if (mStartCal.get(Calendar.YEAR) != mYear && mEndCal.get(Calendar.YEAR) == mYear) {
            //　当前年份和开始年份不想等，和结束年份相等,判断结束年的月份进行限制
            if (mEndCal.get(Calendar.MONTH) == month) {
                currentItem = currentItem >= mEndCal.get(Calendar.DAY_OF_MONTH) ? mEndCal.get(Calendar.DAY_OF_MONTH) - 1 : currentItem;
                count = mEndCal.get(Calendar.DAY_OF_MONTH);
                validStart = 0;
                validEnd = mEndCal.get(Calendar.DAY_OF_MONTH);
                if (count == mEndCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    isCyclic = true;
                } else {
                    isCyclic = false;
                }
            }
        } else if (mStartCal.get(Calendar.YEAR) == mYear && mEndCal.get(Calendar.YEAR) == mYear) {
            //　当前年份和开始年份，结束年份都相等
            if (mStartCal.get(Calendar.MONTH) <= month && mEndCal.get(Calendar.MONTH) >= month) {
                // 获取当前月内的日限制,时间限制在同一个月内
                if (mStartCal.get(Calendar.MONTH) == month && mEndCal.get(Calendar.MONTH) == month) {
                    //  等于　startCal  endCal
                    //判断如果当前时间小于限制时间，自动调节到起始时间
                    currentItem = (currentItem - mStartCal.get(Calendar.DAY_OF_MONTH)) < 0 ? 0 : currentItem - mStartCal.get(Calendar.DAY_OF_MONTH) + 1;
                    count = mEndCal.get(Calendar.DAY_OF_MONTH) - mStartCal.get(Calendar.DAY_OF_MONTH) + 1;
                    validStart = 0;
                    validEnd = count;
                    isCyclic = false;
                    if (count == mEndCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        isCyclic = true;
                    } else {
                        isCyclic = false;
                    }
                } else if (mStartCal.get(Calendar.MONTH) == month) {
                    // 等于startCal  小于endCal
                    currentItem = (currentItem - mStartCal.get(Calendar.DAY_OF_MONTH)) < 0 ? 0 : currentItem - mStartCal.get(Calendar.DAY_OF_MONTH) + 1;
                    count = mStartCal.getActualMaximum(Calendar.DAY_OF_MONTH) - mStartCal.get(Calendar.DAY_OF_MONTH) + 1;
                    validStart = 0;
                    validEnd = count;
                    if (count == mStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        isCyclic = true;
                    } else {
                        isCyclic = false;
                    }
                } else if (mEndCal.get(Calendar.MONTH) == month) {
                    // 大于startCal 等于endCal
                    currentItem = currentItem >= mEndCal.get(Calendar.DAY_OF_MONTH) ? mEndCal.get(Calendar.DAY_OF_MONTH) - 1 : currentItem;
                    count = mEndCal.get(Calendar.DAY_OF_MONTH);
                    validStart = 0;
                    validEnd = mEndCal.get(Calendar.DAY_OF_MONTH);
                    if (count == mEndCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        isCyclic = true;
                    } else {
                        isCyclic = false;
                    }
                }
            }
        }
//        //当已经是循环方式时，不再刷新重设时间
//        if(isCyclic&&mDayPicker.isCyclic()){
//            return;
//        }
//        mDayPicker.setCyclic(isCyclic);
//        mDayPicker.refreshData(count, currentItem, validStart, validEnd - 1);
    }

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // 不使用2D加速
        // setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mZhTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        mNumTpyeface = Typeface.create("DINPro-medium", Typeface.NORMAL);

        mWordNormalTextSizes = new ArrayList<>();
        mWordNormalTextSizes.add(context.getResources().getDimension(R.dimen.mc_picker_normal_word_size_one));
        mWordNormalTextSizes.add(context.getResources().getDimension(R.dimen.mc_picker_normal_word_size_two));
        mWordSelectTextSize = context.getResources().getDimensionPixelOffset(R.dimen.mc_picker_selected_word_size);
        mNumberNormalTextSizes = new ArrayList<>();
        mNumberNormalTextSizes.add(context.getResources().getDimension(R.dimen.mc_picker_normal_number_size_one));
        mNumberNormalTextSizes.add(context.getResources().getDimension(R.dimen.mc_picker_normal_number_size_two));
        mNumberSelectTextSize = context.getResources().getDimensionPixelOffset(R.dimen.mc_picker_selected_number_size);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.DatePicker);
        mStartYear = a.getInt(R.styleable.DatePicker_mcStartYear,
                DEFAULT_START_YEAR);

        mEndYear = a.getInt(R.styleable.DatePicker_mcEndYear, DEFAULT_END_YEAR);
        mLayoutResId = a.getResourceId(R.styleable.DatePicker_mcInternalLayout,
                mLayoutResId);

        mOneScreenCount = a.getInt(R.styleable.DatePicker_mcVisibleRow,
                mOneScreenCount);

        mSelectItemHeight = a.getDimension(R.styleable.DatePicker_mcSelectItemHeight, mSelectItemHeight);

        mNormalItemHeight = a.getDimension(R.styleable.DatePicker_mcNormalItemHeight, mNormalItemHeight);
        a.recycle();

        inflate(getContext(), mLayoutResId, this);

        mLeapUnit = (TextView) findViewById(R.id.mc_leap);
        mMonthUnit = (TextView) findViewById(R.id.mc_scroll1_text);
        if (mMonthUnit != null) {
            mMonthUnit.setText(R.string.mc_date_time_month);
        }

//        mDayUnit = (TextView) findViewById(R.id.mc_scroll2_text);
//        if (mDayUnit != null) {
//            mDayUnit.setText(R.string.mc_date_time_day);
//        }

        mYearUnit = (TextView) findViewById(R.id.mc_scroll3_text);
        if (mYearUnit != null) {
            mYearUnit.setText(R.string.mc_date_time_year);
        }

        // initialize to current date
        Calendar cal = Calendar.getInstance();
        mYear = cal.get(Calendar.YEAR);
        mMonth = cal.get(Calendar.MONTH);
        mDay = cal.get(Calendar.DAY_OF_MONTH);
        mOnDateChangedListener = null;

        int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        mPickerHolder = (LinearLayout) findViewById(R.id.mc_column_parent);

//        mDayPicker = (ScrollTextView) findViewById(R.id.mc_scroll2);
//        mDayPicker.setTypeface(mNumTpyeface);
//
//        if (mSelectItemHeight != 0 && mNormalItemHeight != 0) {
//            mDayPicker.setItemHeight((int) mSelectItemHeight,
//                    (int) mNormalItemHeight);
//        }
//        mDayPicker.setData(new DateAdapter(DateAdapter.SET_DAY), -1, mDay - 1,
//                max, mOneScreenCount, 0, max - 1, true);

        mMonthPicker = (ScrollTextView) findViewById(R.id.mc_scroll1);
        mMonthPicker.setTypeface(mNumTpyeface);
        if (mSelectItemHeight != 0 && mNormalItemHeight != 0) {
            mMonthPicker.setItemHeight((int) mSelectItemHeight,
                    (int) mNormalItemHeight);
        }
        mMonths = getShortMonths();
        mMonthPicker.setData(new DateAdapter(DateAdapter.SET_MONTH), -1,
                mMonth, 12, mOneScreenCount, 0, 11, true);

        mYearPicker = (ScrollTextView) findViewById(R.id.mc_scroll3);
        if (mSelectItemHeight != 0 && mNormalItemHeight != 0) {
            mYearPicker.setItemHeight((int) mSelectItemHeight,
                    (int) mNormalItemHeight);
        }

        refreshTextPreference();

        updateYearPicker();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            mStartCal = Calendar.getInstance();
            mStartCal.setTime(df.parse(mStartYear + "-01-01"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            mEndCal = Calendar.getInstance();
            mEndCal.setTime(df.parse(mEndYear + "-12-31"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // re-order the number pickers to match the current date format
        reorderPickers(mMonths);
        boolean isZh = isZh();
//        mDayUnit.setVisibility(isZh ? VISIBLE : GONE);
        mMonthUnit.setVisibility(isZh ? VISIBLE : GONE);
        mYearUnit.setVisibility(isZh ? VISIBLE : GONE);

        adjustLayout4FocusedPosition();

        // 根据字体大小动态调整年、月、日的PaddingTop
        int textUnitPaddingTop = mYearUnit.getPaddingTop();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.setToDefaults();
        float defaultScaledDensity = displayMetrics.scaledDensity;
        float nowScaledDensity = getResources().getDisplayMetrics().scaledDensity;
        float paddingTopOffset = (mYearUnit.getTextSize() / nowScaledDensity) * (nowScaledDensity - defaultScaledDensity) / 1.3f;
        mYearUnit.setPadding(mYearUnit.getPaddingLeft(), (int) (textUnitPaddingTop - paddingTopOffset),
                mYearUnit.getPaddingRight(), mYearUnit.getPaddingBottom());
        mMonthUnit.setPadding(mMonthUnit.getPaddingLeft(), (int) (textUnitPaddingTop - paddingTopOffset),
                mMonthUnit.getPaddingRight(), mMonthUnit.getPaddingBottom());
//        mDayUnit.setPadding(mDayUnit.getPaddingLeft(), (int) (textUnitPaddingTop - paddingTopOffset),
//                mDayUnit.getPaddingRight(), mDayUnit.getPaddingBottom());

        if (!isEnabled()) {
            setEnabled(false);
        }

        mLineOneHeight = 0;
        mLineTwoHeight = 0;
        mWidthPadding = context.getResources().getDimensionPixelSize(R.dimen.mc_custom_time_picker_line_width_padding);
        mLinePaint = new Paint();


        int lineColor = getResources().getColor(R.color.gregorian_color);
        mLinePaint.setColor(lineColor);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.mc_custom_time_picker_line_stroke_width));
        mIsDrawLine = false;
        setWillNotDraw(false);

        mLunarMouths = getResources().getStringArray(R.array.mc_custom_time_picker_lunar_month);
        mLunardays = getResources().getStringArray(R.array.mc_custom_time_picker_lunar_day);
        mGregorianDays = new String[100];
        for (int i=0; i<100; i++) {
            mGregorianDays[i] = String.valueOf(i);
            if (isZh()) {
                mGregorianDays[i] = String.valueOf(i);
            }
            if (i <= 9) {
                mGregorianDays[i] = "0" + mGregorianDays[i];
            }
        }




        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null) {
            mIsAccessibilityEnable = accessibilityManager.isEnabled();
        }
        if (mIsAccessibilityEnable) {
            setFocusable(true);
        }
    }



    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
//        mDayPicker.setEnabled(enabled);
        mMonthPicker.setEnabled(enabled);
        mYearPicker.setEnabled(enabled);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(DatePicker.class.getName());
    }

    private void reorderPickers(String[] months) {
        /*
         * If the user is in a locale where the medium date format is still
         * numeric (Japanese and Czech, for example), respect the date format
         * order setting. Otherwise, use the order that the locale says is
         * appropriate for a spelled-out date.
         */

        java.text.DateFormat format;
        if (months[0].startsWith("1")) {
            format = DateFormat.getDateFormat(getContext());
        } else {
            format = DateFormat.getMediumDateFormat(getContext());
        }

        if (format instanceof SimpleDateFormat) {
            mOrder = ((SimpleDateFormat) format).toPattern();
        } else {
            // Shouldn't happen, but just in case.
            mOrder = new String(DateFormat.getDateFormatOrder(getContext()));
        }

        /*
         * Remove the 3 pickers from their parent and then add them back in the
         * required order.
         */
        FrameLayout monthLayout = (FrameLayout) findViewById(R.id.mc_column1Layout);
//        FrameLayout dayLayout = (FrameLayout) findViewById(R.id.mc_column2Layout);
        LinearLayout yearLayout = (LinearLayout) findViewById(R.id.mc_column3Layout);
        ImageView divider1 = (ImageView) findViewById(R.id.mc_divider_bar1);
//        ImageView divider2 = (ImageView) findViewById(R.id.mc_divider_bar2);
        LinearLayout parent = (LinearLayout) findViewById(R.id.mc_column_parent);
        parent.removeAllViews();

        boolean quoted = false;
        boolean didDay = false, didMonth = false, didYear = false, didDiv1 = false, didDiv2 = false;

        if (mOrder.contentEquals("dd\u200F/MM\u200F/y") || mOrder.contentEquals("d בMMM y")) {
            mOrder = "yy/M/d";
        }
        for (int i = 0; i < mOrder.length(); i++) {
            char c = mOrder.charAt(i);

            if (c == '\'') {
                quoted = !quoted;
            }

            if (!quoted) {
                boolean need_divider = false;
                if (c == 'd' && !didDay) {
//                    parent.addView(dayLayout);
                    didDay = true;
                    need_divider = true;
                } else if ((c == 'M' || c == 'L') && !didMonth) {
                    parent.addView(monthLayout);
                    didMonth = true;
                    need_divider = true;
                } else if (c == 'y' && !didYear) {
                    parent.addView(yearLayout);
                    didYear = true;
                    need_divider = true;
                }

                if (true == need_divider) {
                    if (didDiv1 == false) {
                        parent.addView(divider1);
                        didDiv1 = true;
                    } else {
                        if (didDiv2 == false) {
//                            parent.addView(divider2);
                            didDiv2 = true;
                        }
                    }
                }
            }
        }

        // Shouldn't happen, but just in case.
        if (!didMonth) {
            parent.addView(monthLayout);
        }
//        if (!didDay) {
//            parent.addView(dayLayout);
//        }
        if (!didYear) {
            parent.addView(yearLayout);
        }

        // 非中文情况重新设置年滑块的左右padding，让日滑块在月和年中间
        if (!isZh()) {
            LinearLayout yearParent = (LinearLayout) findViewById(R.id.mc_column3Layout);
            LinearLayout.LayoutParams yearParentLayoutParams = (LinearLayout.LayoutParams) yearParent.getLayoutParams();
            yearParentLayoutParams.rightMargin = 0;
            yearParentLayoutParams.leftMargin = getResources().getDimensionPixelOffset(R.dimen.mc_picker_year_no_zh_margin_left);
            yearParent.setLayoutParams(yearParentLayoutParams);
        }

        mYearPicker.post(new Runnable() {
            @Override
            public void run() {
                if (mYearPicker != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mYearPicker.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                    mIsLayoutRtl = true;
                } else {
                    mIsLayoutRtl = false;
                }

            }
        });
    }

    /**
     * Updates the current date.
     *
     * @param year
     * @param monthOfYear
     * @param dayOfMonth
     */
    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        updateDate(year, monthOfYear, dayOfMonth, true);
    }

    public void updateDate(int year, int monthOfYear, int dayOfMonth, boolean doAnimate) {
        updateDate(year, monthOfYear, dayOfMonth, doAnimate, true);
    }

    private void updateDate(int year, int monthOfYear, int dayOfMonth, boolean doAnimate, boolean isReorderPickers) {
        mYear = year < mStartYear ? mStartYear : year;
        mYear = year > mEndYear ? mEndYear : year;
        mMonth = monthOfYear;
        mDay = dayOfMonth;
        mMonths = null;
        mMonths = getShortMonths();

        mYearPicker.setCurrentItem(mYear - mStartYear, doAnimate);

        if (mYearOfMonths != getYearMonths()) {
            mYearOfMonths = getYearMonths();
            mMonthPicker.refreshCount(mYearOfMonths);
        }
        mMonthPicker.setCurrentItem(mMonth, doAnimate);

        if (mMonthOfDays != getMonthDays()) {
            mMonthOfDays = getMonthDays();
//            mDayPicker.refreshCount(getMonthDays());
        }
//        mDayPicker.setCurrentItem(mDay - 1, doAnimate);

        if (isReorderPickers) {
            reorderPickers(mMonths);
        }
    }

    private String[] getShortMonths() {
        final Locale currentLocale = Locale.getDefault();
        if (currentLocale.equals(mMonthLocale) && mShortMonths != null) {
            return mShortMonths;
        } else {
            synchronized (mMonthUpdateLock) {
                if (!currentLocale.equals(mMonthLocale)) {
                    mShortMonths = new String[NUMBER_OF_MONTHS];
                    for (int i = 0; i < NUMBER_OF_MONTHS; i++) {
                        mShortMonths[i] = DateUtils.getMonthString(
                                Calendar.JANUARY + i, DateUtils.LENGTH_MEDIUM);
                    }


                    if (mShortMonths[0].startsWith("1")) {
                        for (int i = 0; i < mShortMonths.length; i++) {
                            mShortMonths[i] = String.valueOf(i + 1);
                            if (i < 9) {
                                mShortMonths[i] = "0" + mShortMonths[i];
                            }
                        }
                    }
                    mMonthLocale = currentLocale;
                }
            }

            return mShortMonths;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mIsDrawLine) {
            int width = getWidth();
            int pickerWidth = mPickerHolder.getWidth() - mWidthPadding * 2;
            int paddingWdith = (width - pickerWidth) / 2;
            canvas.drawLine(paddingWdith, mLineOneHeight, paddingWdith + pickerWidth, mLineOneHeight, mLinePaint);
            canvas.drawLine(paddingWdith, mLineTwoHeight, paddingWdith + pickerWidth, mLineTwoHeight, mLinePaint);
        }
    }

    public void setIsDrawLine(boolean isDrawLine) {
        mIsDrawLine = isDrawLine;
    }

    public void setLineHeight(int lineOneHeight, int lineTwoHeight) {
        mLineOneHeight = lineOneHeight;
        mLineTwoHeight = lineTwoHeight;
    }

//    /**
//     * @param isShowDay 是否显示日
//     */
//    public void setShowDayColumn(boolean isShowDay) {
//        FrameLayout dayLayout = (FrameLayout) findViewById(R.id.mc_column2Layout);
//        if (dayLayout != null) {
//            dayLayout.setVisibility(isShowDay ? VISIBLE : GONE);
//        }
//    }

    private static class SavedState extends BaseSavedState {

        private final int mYear;
        private final int mMonth;
        private final int mDay;

        /**
         * Constructor called from {@link DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int year, int month, int day) {
            super(superState);
            mYear = year;
            mMonth = month;
            mDay = day;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mYear = in.readInt();
            mMonth = in.readInt();
            mDay = in.readInt();
        }

        public int getYear() {
            return mYear;
        }

        public int getMonth() {
            return mMonth;
        }

        public int getDay() {
            return mDay;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mYear);
            dest.writeInt(mMonth);
            dest.writeInt(mDay);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Override so we are in complete control of save / restore for this widget.
     */
    @Override
    protected void dispatchRestoreInstanceState(
            SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mYear, mMonth, mDay);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mYear = ss.getYear();
        mMonth = ss.getMonth();
        mDay = ss.getDay();
    }

    /**
     * Initialize the state.
     *
     * @param year                  The initial year.
     * @param monthOfYear           The initial month.
     * @param dayOfMonth            The initial day of the month.
     * @param onDateChangedListener How user is notified date is changed by user, can be null.
     */
    public void init(int year, int monthOfYear, int dayOfMonth,
                     OnDateChangedListener onDateChangedListener, boolean doAnimate) {
        if (mYear != year || mMonth != monthOfYear || mDay != dayOfMonth) {
            updateDate(year, monthOfYear, dayOfMonth, doAnimate);
        }

        mOnDateChangedListener = onDateChangedListener;
        sendAccessibilityEvent();
    }


    /**
     * Initialize the state.
     *
     * @param year                  The initial year.
     * @param monthOfYear           The initial month.
     * @param dayOfMonth            The initial day of the month, from 0 to 11(if it is lunar, may be 12).
     * @param onDateChangedListener How user is notified date is changed by user, can be null.
     * @param isLunar               whether is lunar calendar
     * @param isLeapMonth           whether is leap month, only used when the calendar is lunar
     */
    public void init(int year, int monthOfYear, int dayOfMonth,
                     OnDateChangedListener onDateChangedListener, boolean isLunar, boolean isLeapMonth) {
        if (mYear != year || mMonth != monthOfYear || mDay != dayOfMonth || this.isLunar != isLunar) {

            updateDate(year, monthOfYear, dayOfMonth, false);

        }
        refreshTextPreference();
        mOnDateChangedListener = onDateChangedListener;
        sendAccessibilityEvent();
    }

    public int getYear() {
        return mYear;
    }

    public int getMonth() {
        return mMonth;
    }

    public int getDayOfMonth() {
        return mDay;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus == false) {
            return;
        }

        java.text.DateFormat format;
        String order;
        final String[] months = getShortMonths();
        if (months[0].startsWith("1")) {
            format = DateFormat.getDateFormat(getContext());
        } else {
            format = DateFormat.getMediumDateFormat(getContext());
        }

        if (format instanceof SimpleDateFormat) {
            order = ((SimpleDateFormat) format).toPattern();
        } else {
            order = new String(DateFormat.getDateFormatOrder(getContext()));
        }

        if (mOrder.equals(order)) {
            return;
        }

        mMonths = months;
        reorderPickers(mMonths);
    }

    private void updateYearPicker() {
        mYearPicker.setData(/* R.layout.date_time_item, */new DateAdapter(
                DateAdapter.SET_YEAR), -1, (mYear - mStartYear), (mEndYear
                - mStartYear + 1), mOneScreenCount, 0, (mEndYear
                - mStartYear), false);
    }

    /**
     * Sets the maximal date supported by this DatePicker in milliseconds
     *
     * @param maxDate
     */
    public void setMaxDate(long maxDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(maxDate);
        mEndCal = cal;
        int newYear = cal.get(Calendar.YEAR);
        mEndYear = newYear;
        refresh();
    }

    public void refresh() {
        updateYearPicker();
        mOldYear = -1;
        setMonthRange(mYear);
        mOldMonth = -1;
        setDayRange(mMonth);
    }

    /**
     * Sets the minimal date supported by this NumberPicker in milliseconds
     *
     * @param minDate
     */
    public void setMinDate(long minDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(minDate);
        mStartCal = cal;
        int newYear = cal.get(Calendar.YEAR);
        mStartYear = newYear;
        refresh();
    }



    public boolean isLunar() {
        return isLunar;
    }

    private int getMonthDays() {

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DATE, 1);
            cal.set(Calendar.YEAR, mYear);
            cal.set(Calendar.MONTH, mMonth);
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH);

    }

    private Calendar getCurrentCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.YEAR, mYear);
        cal.set(Calendar.MONTH, mMonth);
        return cal;
    }

    private int getYearMonths() {

            return 12;

    }

    // because the change of mFocusedPosition，change the highlight position of
    // the imageView and textView
    private void adjustLayout4FocusedPosition() {
        mMonthUnit = (TextView) findViewById(R.id.mc_scroll1_text);
        if (mMonthUnit != null) {
            mMonthUnit.setText(R.string.mc_date_time_month);
        }

//        mDayUnit = (TextView) findViewById(R.id.mc_scroll2_text);
//        if (mDayUnit != null) {
//            mDayUnit.setText(R.string.mc_date_time_day);
//        }

        mYearUnit = (TextView) findViewById(R.id.mc_scroll3_text);
        if (mYearUnit != null) {
            mYearUnit.setText(R.string.mc_date_time_year);
        }
    }

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    /**
     * 设置滚动的TextView的选中、非选中字体颜色
     * 注：若不使用setTextColor设置，则按照默认字体颜色；颜色必须是getColor之后的颜色值，而不是资源ID
     *
     * @param selectedColor 设置选中字体的color值
     * @param normalColor   设置非选中字体的color值
     **/
    public void setTextColor(int selectedColor, int normalColor, int unitColor) {
//        mDayPicker.setTextColor(selectedColor, normalColor);
        mMonthPicker.setTextColor(selectedColor, normalColor);
        mYearPicker.setTextColor(selectedColor, normalColor);

//        mDayUnit.setTextColor(unitColor);
        mMonthUnit.setTextColor(unitColor);
        mYearUnit.setTextColor(unitColor);
    }

    public void setTextColor(int selectedColor, List<Integer> normalColors, int unitColor) {
//        mDayPicker.setTextColor(selectedColor, normalColors);
        mMonthPicker.setTextColor(selectedColor, normalColors);
        mYearPicker.setTextColor(selectedColor, normalColors);

//        mDayUnit.setTextColor(unitColor);
        mMonthUnit.setTextColor(unitColor);
        mYearUnit.setTextColor(unitColor);
    }

    public void setItemHeight(int selectHeight, int normalHeight) {
//        mDayPicker.setItemHeight(selectHeight, normalHeight);
        mMonthPicker.setItemHeight(selectHeight, normalHeight);
        mYearPicker.setItemHeight(selectHeight, normalHeight);
    }

//    public TextView getDayUnit() {
//        return mDayUnit;
//    }

    public void setIsDrawFading(boolean isDrawFading) {
        mYearPicker.setIsDrawFading(isDrawFading);
        mMonthPicker.setIsDrawFading(isDrawFading);
//        mDayPicker.setIsDrawFading(isDrawFading);
    }

    private String getTimeText(int type) {
        int position = 0;
        switch (type) {
            case DateAdapter.SET_YEAR:
                return String.valueOf(mYear);

            case DateAdapter.SET_MONTH:
                position = mMonth;

                    if (mMonths == null) {
                        mMonths = getShortMonths();
                    }
                    if (position < mMonths.length) {
                        return mMonths[position];
                    }

                break;

            case DateAdapter.SET_DAY:
                position = mDay - 1;

                    return String.valueOf(position + 1);


            default:
                break;
        }
        return "";
    }

    private void sendAccessibilityEvent() {
        if (mIsAccessibilityEnable) {
//            String dateText = (getTimeText(DateAdapter.SET_YEAR) + mYearUnit.getText() + getTimeText(DateAdapter.SET_MONTH) + mMonthUnit.getText() +
//                    getTimeText(DateAdapter.SET_DAY) + mDayUnit.getText()).replace(" ", "").replace("廿十", "二十").replace("廿", "二十");
//            setContentDescription(dateText);
            sendAccessibilityEvent(AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (mIsAccessibilityEnable && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
//            String dateText = (getTimeText(DateAdapter.SET_YEAR) + mYearUnit.getText() + getTimeText(DateAdapter.SET_MONTH) + mMonthUnit.getText() +
//                    getTimeText(DateAdapter.SET_DAY) + mDayUnit.getText()).replace(" ", "").replace("廿十", "二十").replace("廿", "二十");
//            event.getText().add(dateText);
            return false;
        }

        return super.dispatchPopulateAccessibilityEvent(event);
    }

    private boolean isZh() {
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

    private void refreshTextPreference() {
        if (this.isLunar && isZh()) {
            mMonthPicker.setTextSize(mWordSelectTextSize, mWordNormalTextSizes);
//            mDayPicker.setTextSize(mWordSelectTextSize, mWordNormalTextSizes);
            mMonthPicker.setTypeface(mZhTypeface);
//            mDayPicker.setTypeface(mZhTypeface);
        } else {
            mMonthPicker.setTextSize(mNumberSelectTextSize, mNumberNormalTextSizes);
//            mDayPicker.setTextSize(mNumberSelectTextSize, mNumberNormalTextSizes);
            mMonthPicker.setTypeface(mNumTpyeface);
//            mDayPicker.setTypeface(mNumTpyeface);
        }
        mYearPicker.setTextSize(mNumberSelectTextSize, mNumberNormalTextSizes);
        mYearPicker.setTypeface(mNumTpyeface);
    }

    public String getTimePreviewText(boolean isLunar, int year, int month, int dayOfMonth, boolean isShowDay) {
        String previewText = "";
        month = month + 1;
        String yearText = getResources().getString(R.string.mc_date_time_year);
        String monthText = getResources().getString(R.string.mc_date_time_month);
        String dayText = getResources().getString(R.string.mc_date_time_day);
        if (!isLunar) {
            if (isZh()) { // 中文
                if (isShowDay) {
                    previewText = year + yearText + month + monthText + dayOfMonth + dayText + " " + Utils.getWeek(getContext(), year, month - 1, dayOfMonth);
                } else {
                    previewText = year + yearText + month + monthText;
                }
            } else { // 其他语言
                String[] mouths = getResources().getStringArray(R.array.mc_custom_time_picker_lunar_month);
                if (month > 0 && month <= mouths.length) {
                    if (isShowDay) {
                        previewText = mouths[month-1] + " " + year + "," + dayOfMonth + " " + Utils.getWeek(getContext(), year, month-1, dayOfMonth);
                    } else {
                        previewText = mouths[month-1] + " " + year;
                    }
                }
            }
        } else {
            String monthString;

            String[] mouths = getResources().getStringArray(R.array.mc_custom_time_picker_lunar_month);


            monthString = mouths[month-1];

            if (isZh()) { // 中文

                    previewText = year + yearText + monthString + monthText;

            }
        }
        return previewText;
    }

}
