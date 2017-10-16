package com.zhudai.tab;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.joker.annotation.PermissionsDenied;
import com.joker.annotation.PermissionsGranted;
import com.joker.annotation.PermissionsNonRationale;
import com.joker.annotation.PermissionsRationale;
import com.joker.api.Permissions4M;
import com.zhudai.R;
import com.zhudai.call.CallLogContract;
import com.zhudai.call.activity.DayCallCountActivity;
import com.zhudai.call.adapter.LocalCallLogAdapter;
import com.zhudai.call.entity.CallLogInfo;
import com.zhudai.call.presenter.LocalCallPresenter;
import com.zhudai.callrecord.event.NewCallLogEvent;
import com.zhudai.view.DatePicker;
import com.zhudai.view.DatePickerDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.OnItemClick;
import timber.log.Timber;

/**
 * Created by xm on 2016/8/2.
 */
public class CalllogFragment extends TabFragmentBase implements ITabFragment, CallLogContract.CallLogView {

    private static final int CALL_LOG_CODE = 201;

    @BindView(R.id.list)
    ListView mCallLogListView;
    @BindView(R.id.empty)
    View mEmptyView;
    @BindView(R.id.nopmview)
    View mNoPmView;
    @BindView(R.id.image)
    ImageView mImage;
    @BindView(R.id.tip)
    TextView mTip;
    @BindView(R.id.refresh_btn_empty)
    Button mBtnOpenPermission;

    private LinearLayout mDateContain;
    private TextView mMonthTv;
    private TextView mYearTv;
    private TextView mRecordTv;
    private TextView mDayTalkTime;
    private View mHeadView;

    private LocalCallLogAdapter mLocalCallLogAdapter;
    private LocalCallPresenter mLocalCallPresenter;
    private SQLiteDatabase mSQLiteDatabase;
    private int mYear;
    private int mMonth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSQLiteDatabase = LitePal.getDatabase();
        mLocalCallPresenter = new LocalCallPresenter(CalllogFragment.this.getActivity(), this);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.tab_calllog;
    }

    @Override
    protected void initContentView(View root, Bundle savedInstanceState) {
        initHeadView();
        //让HeaderView不可点击
        mCallLogListView.addHeaderView(mHeadView, null, false);
        mLocalCallLogAdapter = new LocalCallLogAdapter(CalllogFragment.this.getActivity());
        mCallLogListView.setAdapter(mLocalCallLogAdapter);
        Timber.v("CalllogFragment initContentView");
        refresh();
    }

    private void initHeadView() {
        mHeadView = LayoutInflater.from(CalllogFragment.this.getActivity()).inflate(R.layout.local_call_title_layout, null);
        mDateContain = (LinearLayout) mHeadView.findViewById(R.id.data);
        mDateContain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog().show();
            }
        });
        mMonthTv = (TextView) mHeadView.findViewById(R.id.month);
        mYearTv = (TextView) mHeadView.findViewById(R.id.year);
        mRecordTv = (TextView) mHeadView.findViewById(R.id.record_num);
        mDayTalkTime = (TextView) mHeadView.findViewById(R.id.talk_time_num);
        mRecordTv.setText("0");
        mDayTalkTime.setText("0");
    }

    private void checkPermission() {
        Permissions4M.get(CalllogFragment.this)
                .requestPermissions(Manifest.permission.READ_CALL_LOG)
                .requestCodes(CALL_LOG_CODE)
                .requestPageType(Permissions4M.PageType.ANDROID_SETTING_PAGE)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        Permissions4M.onRequestPermissionsResult(CalllogFragment.this, requestCode, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @PermissionsGranted(CALL_LOG_CODE)
    public void callLogGranted() {
        if (mNoPmView != null) mNoPmView.setVisibility(View.GONE);
        Timber.v("读取通话记录权限授权成功 in activity with annotation");
    }

    @PermissionsDenied(CALL_LOG_CODE)
    public void callLogDenied() {
        Timber.v("读取通话记录权限授权失败 in activity with annotation");
    }

    @PermissionsRationale(CALL_LOG_CODE)
    public void callLogNonRationale() {
        Timber.v("请开启读取通话记录权限授权 in activity with annotation");
    }

    @PermissionsNonRationale(CALL_LOG_CODE)
    public void nonRationale(int code, final Intent intent) {
        if (!isAdded() || mNoPmView == null) return;
        mNoPmView.setVisibility(View.VISIBLE);
        mNoPmView.findViewById(R.id.btngopm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (intent != null) {
                    startActivity(intent);
                }
            }
        });
    }

    public void refresh() {
        setCurrentDate();
        loadData(mYear, mMonth);
    }

    private void loadData(int year, int month) {
        mEmptyView.setVisibility(View.GONE);
        mLocalCallPresenter.loadCallLogs(year, month);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewCallLog(NewCallLogEvent event) {
        Timber.v("CalllogFragment NewCallLogEvent");
        refresh();
    }

    private void setCurrentDate() {
        Calendar c = Calendar.getInstance();//
        mYear = c.get(Calendar.YEAR); // 获取当前年份
        mMonth = c.get(Calendar.MONTH) + 1;// 获取当前月份
        fillYearAndMonth(mYear, mMonth);
    }

    @OnItemClick(R.id.list)
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        CallLogInfo callLogInfo = (CallLogInfo) parent.getItemAtPosition(position);
        Intent intent = new Intent();
        intent.putExtra("LocalCallBo", callLogInfo);
        intent.setClass(CalllogFragment.this.getActivity(), DayCallCountActivity.class);
        startActivity(intent);
    }

    @Override
    public Dialog createDialog() {
        DatePickerDialog d;
        final Calendar calendar = Calendar.getInstance();
        d = new DatePickerDialog(CalllogFragment.this.getActivity(), 0, new SolarDatePickerListener(),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), false, false);
        d.getDatePicker();
        return d;
    }

    @Override
    public void notifyCallLogListAdapter(List<CallLogInfo> list) {
        mLocalCallLogAdapter.notifyData(list);
    }

    @Override
    public void fillRecord(String size) {
        mRecordTv.setText(size);
    }

    @Override
    public void fillAverageCall(String average) {
        mDayTalkTime.setText(average);
    }

    @Override
    public void fillYearAndMonth(int year, int month) {
        if (year != 0 && month != 0) {
            mYearTv.setText(String.valueOf(year));
            mMonthTv.setText(String.valueOf(month) + "月");
        }
    }

    @Override
    public void showEmptyView(List<CallLogInfo> list) {
        if (list != null && list.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mBtnOpenPermission.setVisibility(View.GONE);
            mImage.setBackgroundResource(R.drawable.no_permission);
            mTip.setText("您这个月没有通话记录哦～");
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    protected TabEnum getTab() {
        return TabEnum.CALLLOG;
    }

    @Override
    public void onTabDisplay() {
    }

    @Override
    protected String getSimpleName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.v("CalllogFragment onResume");
        if (isAdded() && isViewPrepared) checkPermission();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSQLiteDatabase.close();
    }

    private class SolarDatePickerListener implements DatePickerDialog.OnDateSetListener {
        @Override
        public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
            mYear = arg1;
            mMonth = arg2 + 1;
            fillYearAndMonth(mYear, mMonth);
            loadData(mYear, mMonth);
        }
    }

}
