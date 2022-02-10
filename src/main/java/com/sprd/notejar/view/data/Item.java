package com.sprd.notejar.view.data;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Item {
    public int mId;
    public long mDate;
    public boolean mIsDeleted;

    public Item() {
        mId = -1;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public void setLongDate(long date) {
        mDate = date;
    }

    public long getLongDate() {
        return mDate;
    }

    public String getDate(Context context) {
        java.text.DateFormat dateFormat = DateFormat.getDateFormat(context);
        String cDate = dateFormat.format(new Date(mDate));
        return cDate;
    }

    public String getTime(Context context) {
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
        String cTime = timeFormat.format(new Date(mDate));
        return cTime;
    }

    public String getDisplayTime(Context context, Date d){
        SimpleDateFormat format = DateFormat.is24HourFormat(context)? new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) : new SimpleDateFormat("MM-dd hh:mm a", Locale.getDefault());
        return format.format(d);
    }

    public String getTime(Context context, Date d){
        SimpleDateFormat format = DateFormat.is24HourFormat(context)? new SimpleDateFormat("HH:mm", Locale.getDefault()) : new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return format.format(d);
    }

    public String getDisplayDateTime(Context context, Long date) {
        SimpleDateFormat format = DateFormat.is24HourFormat(context)? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) : new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        return format.format(new Date(mDate));
    }

    public String getDisplayDate(Date d) {
        SimpleDateFormat format = new SimpleDateFormat("MMdd", Locale.ENGLISH);
        return format.format(d);
    }

    public String getYear(Date d){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy", Locale.getDefault());
        return formatter.format(d);
    }

    public boolean isToday() {
        boolean isToday = (getYear(new Date(getLongDate()))
                .equals(getYear(new Date(System.currentTimeMillis())))
                && getDisplayDate(new Date(getLongDate())).equals(getDisplayDate(new Date(System.currentTimeMillis()))));
        return isToday;
    }

    public void setDeleted(boolean deleted) {
        mIsDeleted = deleted;
    }

    public boolean isDeleted() {
        return mIsDeleted;
    }

    public String toString() {
        return "";
    }
}
