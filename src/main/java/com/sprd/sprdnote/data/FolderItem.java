package com.sprd.sprdnote.data;

import android.content.Context;
import com.sprd.notejar.view.data.Item;
public class FolderItem extends Item {
    private String mName;

    public FolderItem() {
        this(null, 0, false);
    }

    public FolderItem(String name, long date) {
        this(name, date, false);
    }

    public FolderItem(String name, long date, boolean isDeleted) {
        super();
        mName = name;
        mDate = date;
        mIsDeleted = isDeleted;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public String toString() {
        return "FolderItem [id=" + mId + ", name=" + mName + "]";
    }

    public String toString(Context context) {
        return "FolderItem [ID=" + mId + ", name=" + mName +
                ", Date=" + getDate(context) + ", Time=" + getTime(context) +
                ", isDeleted=" + mIsDeleted + "]";
    }
}