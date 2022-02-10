package com.sprd.notejar.view.data;

import android.content.Context;
import android.util.Log;

public class NoteItem extends Item {
    public static final int DEFAULT_PARENT_FOLDER_ID = -1;
    private String mTitle;
    private String mContent;
    private boolean mIsCollected;
    private boolean mIsContainPic;
    private boolean mIsPrivate;
    private int mBackgroundId;
    private int mParentFolderId;
    private boolean mSelected;
    private NoteSettingChangedListener mNoteSettingChangedListener;

    public NoteItem() {
        this(null, null, 0);
    }

    public NoteItem(String title, String content, long date) {
        this(title, content, date, false, false, false, false, false, -1, -1);
    }

    public NoteItem(String title, String content, long date,
                    boolean isCollected, boolean isContainPic, boolean isPrivate, boolean isDeleted,
                    boolean isSelected, int backgroundId, int parentFolderId) {
        super();
        mTitle = title;
        mContent = content;
        mDate = date;
        mIsCollected = isCollected;
        mIsContainPic = isContainPic;
        mIsPrivate = isPrivate;
        mIsDeleted = isDeleted;
        mBackgroundId = backgroundId;
        mParentFolderId = parentFolderId;
        mSelected = isSelected;
    }

    public NoteItem(NoteItem item) {
        mTitle = item.getTitle();
        mContent = item.getContent();
        mIsCollected = item.isCollected();
        mIsContainPic =item.isContainPic();
        mIsPrivate = item.isPrivate();
        mIsDeleted = item.isDeleted();
        mBackgroundId = item.getBackgroundId();
        mParentFolderId = item.getParentFolderId();
        mSelected = item.getIsSelected();
    }

    public void setNoteSettingChangedListener (NoteSettingChangedListener listener) {
        mNoteSettingChangedListener = listener;
    }

    public void setSelected (boolean isSelected) {
        mSelected = isSelected;
    }

    public boolean getIsSelected () {
        return mSelected;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public void setCollected(boolean collected) {
        mIsCollected = collected;
    }

    public boolean isCollected() {
        return mIsCollected;
    }

    public void setHasPictures(boolean containPic) {
        mIsContainPic = containPic;
    }

    public boolean isContainPic() {
        return mIsContainPic;
    }

    public void setPrivate(boolean priv) {
        mIsPrivate = priv;
    }

    public boolean isPrivate() {
        return mIsPrivate;
    }

    public void setBackgroundId(int backgroundId) {
        mBackgroundId = backgroundId;
        Log.d("NoteEditorActivity","backgroundId=============="+backgroundId+" mNoteListener =="+mNoteSettingChangedListener);
        if (mNoteSettingChangedListener != null) {
            mNoteSettingChangedListener.onBackgroundColorChanged();
        }
    }

    public int getBackgroundId() {
        return mBackgroundId;
    }

    public void setParentFolderId(int folderId) {
        mParentFolderId = folderId;
    }

    public int getParentFolderId() {
        return mParentFolderId;
    }

    public String toString() {
        //return "NoteItem [ID=" + mId + ", title=" + mTitle + ", content=" + mContent + "]";
        return "NoteItem [ID=" + mId + ", title=" + mTitle + ", content=" + mContent +
                ", isCollected=" + mIsCollected + ", isPrivate=" + mIsPrivate +
                ", isContainPic=" + mIsContainPic + ", isDeleted=" + mIsDeleted +
                ", backgroundID=" + mBackgroundId + ", folderID=" + mParentFolderId +
                "]";
    }

    public String toString(Context context) {
        return "NoteItem [ID=" + mId + ", title=" + mTitle + ", content=" + mContent +
                ", Date=" + getDate(context) + ", Time=" + getTime(context) +
                ", isCollected=" + mIsCollected + ", isPrivate=" + mIsPrivate +
                ", isContainPic=" + mIsContainPic + ", isDeleted=" + mIsDeleted +
                ", backgroundID=" + mBackgroundId + ", folderID=" + mParentFolderId +
                "]";
    }

    public interface NoteSettingChangedListener {
        /**
         * Called when the background color of current note has just changed
         */
        void onBackgroundColorChanged();

        /**
         * Called when user set clock
         */
        void onClockAlertChanged(long date, boolean set);

        /**
         * Call when user create note from widget
         */
        void onWidgetChanged();

        /**
         * Call when switch between check list mode and normal mode
         * @param oldMode is previous mode before change
         * @param newMode is new mode
         */
        void onCheckListModeChanged(int oldMode, int newMode);
    }
}