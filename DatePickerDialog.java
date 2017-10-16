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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhudai.R;

import com.zhudai.view.DatePicker.OnDateChangedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;



public class DatePickerDialog extends AlertDialog
        implements OnDateChangedListener, View.OnClickListener {

    private static final String YEAR = "年";
    private static final String MONTH = "月";
    private static final String DAY = "日";

    private final DatePicker mDatePicker;
    private final OnDateSetListener mCallBack;

    private TextView mTimerPreview;
    private boolean mIsShowDay = false;

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateSetListener {
        /**
         * @param view The view associated with this listener.
         * @param year The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *  with {@link java.util.Calendar}.
         * @param dayOfMonth The day of the month that was set.
         */
        void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth);
    }
    
    /**
     * @param context The context the dialog is to run in.
     * @param callBack How the parent is notified that the date is set.
     * @param year The initial year of the dialog.
     * @param monthOfYear The initial month of the dialog.
     * @param dayOfMonth The initial day of the dialog.
     */
    public DatePickerDialog(Context context,
            OnDateSetListener callBack,
            int year,
            int monthOfYear,
            int dayOfMonth) {
        this(context, 0, callBack, year, monthOfYear, dayOfMonth);
    }
    
    /**
     * @param context The context the dialog is to run in.Cust
     * @param theme the theme to apply to this dialog
     * @param callBack How the parent is notified that the date is set.
     * @param year The initial year of the dialog.
     * @param monthOfYear The initial month of the dialog.
     * @param dayOfMonth The initial day of the dialog.
     */
    public DatePickerDialog(Context context,
            int theme,
            OnDateSetListener callBack,
            int year,
            int monthOfYear,
            int dayOfMonth) {
        this(context, theme, callBack, year, monthOfYear, dayOfMonth, false, false);
    }

    /**
     * @param context The context the dialog is to run in.
     * @param theme the theme to apply to this dialog
     * @param callBack How the parent is notified that the date is set.
     * @param year The initial year of the dialog.
     * @param monthOfYear The initial month of the dialog.
     * @param dayOfMonth The initial day of the dialog.
     * @param isLunar whether is lunar calendar
     * @param isLeapMonth whether is leap month, only used when the calendar is lunar
     */
    public DatePickerDialog(Context context,
            int theme,
            OnDateSetListener callBack,
            int year,
            int monthOfYear,
            int dayOfMonth, boolean isLunar, boolean isLeapMonth) {
        super(context, theme);

        mCallBack = callBack;

//        setButton(BUTTON_POSITIVE, context.getText(R.string.mc_yes), this);
//        setButton(BUTTON_NEGATIVE, context.getText(android.R.string.cancel), (OnClickListener) null);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.mc_date_picker_dialog, null);
        Button positive = (Button) view.findViewById(R.id.positive);
        Button cancel = (Button)view.findViewById(R.id.negative);
        positive.setOnClickListener(this);
        cancel.setOnClickListener(this);
        setView(view);


        mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        mDatePicker.init(year, monthOfYear, dayOfMonth, this, isLunar, isLeapMonth);

        final int selectColor = context.getResources().getColor(
                R.color.mc_picker_selected_color);
        List<Integer> unSelectColors = new ArrayList<>();
        unSelectColors.add(context.getResources().getColor(
                R.color.mc_picker_unselected_color_one));
        unSelectColors.add(context.getResources().getColor(
                R.color.mc_picker_unselected_color_two));

        mDatePicker.setTextColor(selectColor, unSelectColors, selectColor);
        mDatePicker.setIsDrawLine(false);
        mDatePicker.setLineHeight(context.getResources().getDimensionPixelSize(R.dimen.mc_custom_time_picker_line_one_height),
                context.getResources().getDimensionPixelSize(R.dimen.mc_custom_time_picker_line_two_height));

        mTimerPreview = (TextView) view.findViewById(R.id.time_preview);
        mTimerPreview.setText(mDatePicker.getTimePreviewText(mDatePicker.isLunar(), mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth(), mIsShowDay));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.positive:
                if (mCallBack != null) {
                    mDatePicker.clearFocus();
                    mCallBack.onDateSet(mDatePicker, mDatePicker.getYear(),
                            mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
                }
                this.dismiss();
                break;
            case R.id.negative:
                this.dismiss();
                break;
        }
    }

    public void onDateChanged(DatePicker view, int year, int month, int day) {
        mTimerPreview.setText(mDatePicker.getTimePreviewText(mDatePicker.isLunar(), mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth(), mIsShowDay));
    }

    /**
     * Gets the {@link DatePicker} contained in this dialog.
     *
     * @return The calendar view.
     */
    public DatePicker getDatePicker() {
        return mDatePicker;
    }

    /**
     * Sets the current date.
     *
     * @param year The date year.
     * @param monthOfYear The date month.
     * @param dayOfMonth The date day of month.
     */
    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        updateDate(year, monthOfYear, dayOfMonth, true);
    }

    /**
     * Sets the current date.
     *
     * @param year The date year.
     * @param monthOfYear The date month.
     * @param dayOfMonth The date day of month.
     */
    public void updateDate(int year, int monthOfYear, int dayOfMonth, boolean doAnimate) {
        mDatePicker.updateDate(year, monthOfYear, dayOfMonth, doAnimate);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(YEAR, mDatePicker.getYear());
        state.putInt(MONTH, mDatePicker.getMonth());
        state.putInt(DAY, mDatePicker.getDayOfMonth());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int year = savedInstanceState.getInt(YEAR);
        int month = savedInstanceState.getInt(MONTH);
        int day = savedInstanceState.getInt(DAY);
        mDatePicker.init(year, month, day, this, false);
    }

    /**
     * 设置滚动的TextView的选中、非选中字体颜色
     * 注：若不使用setTextColor设置，则按照默认字体颜色；颜色必须是getColor之后的颜色值，而不是资源ID
     * 
     * @param selectedColor 设置选中字体的color值
     * @param normalColor 设置非选中字体的color值
     **/
    public void setTextColor(int selectedColor, int normalColor, int unitColor) {
        mDatePicker.setTextColor(selectedColor, normalColor, unitColor);
    }

    /**
     * @param year 设置可以选择的最小日期（不能小于1900）
     */
    public void setMinYear(int year) {

        Calendar cal = Calendar.getInstance();
        cal.set(year, 0, 1);
        mDatePicker.setMinDate(cal.getTimeInMillis());
    }

    /**
     * @param year 设置可以选择的最大日期（不能大于2099）
     */
    public void setMaxYear(int year) {

        Calendar cal = Calendar.getInstance();
        cal.set(year, 11, 31);
        mDatePicker.setMaxDate(cal.getTimeInMillis());
    }

    /**
     * @param isShowDay 是否显示日
     */
    public void setShowDayColumn(boolean isShowDay) {
        mIsShowDay = isShowDay;
//        mDatePicker.setShowDayColumn(isShowDay);
        mTimerPreview.setText(mDatePicker.getTimePreviewText(mDatePicker.isLunar(), mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth(), mIsShowDay));
    }
}
