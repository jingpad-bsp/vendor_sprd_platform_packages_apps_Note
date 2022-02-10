package com.sprd.sprdnote.data;

import com.sprd.notejar.view.data.NoteItem;

/**
 * Created by danny.liu on 2017/9/4.
 */

public class ListItem {
    public static int NOTE = 0;
    public static int HEAD = 1;
    private int type;
    private NoteItem mNoteItem;
    private int year;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public NoteItem getNoteItem() {
        return mNoteItem;
    }

    public void setNoteItem(NoteItem noteItem) {
        this.mNoteItem = noteItem;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}