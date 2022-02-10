package com.sprd.notejar.view.util;


import android.util.Log;

import com.sprd.notejar.view.data.NoteItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Created by danny.liu on 2017/3/2.
 */

public class NoteCategory {

    private String headerTitle;
    ArrayList<NoteItem> list = new ArrayList<>();
    public NoteCategory(String header) {
        headerTitle = header;
    }

    public void addItem(NoteItem item){
        if (list != null ) {
            list.add(item);
        }
    }

    public String getHeaderTitle() {
        return headerTitle;
    }
    public ArrayList getItems() {
        if (list != null && list.size() != 0) {
            return list;
        }else {
            return null;
        }
    }

    public NoteItem getItem(int position) {
        if (list.size() != 0) {
            if (Integer.parseInt(getHeaderTitle())
                    == Integer.parseInt(getYear(new Date(System.currentTimeMillis())))) {
                return list.get(position);
            } else if (position == 0) {
                return null;
            } else {
                return list.get(position -1);
            }

        }
        return null;
    }

    public int getItemCounts ()  {
        if (list.size() != 0) {
            if (getHeaderTitle() != null
                    && Integer.parseInt(getHeaderTitle()) == Integer.parseInt(getYear(new Date(System.currentTimeMillis())))) {
                return list.size();
            }
            return list.size() + 1;
        } else {
            return 0;
        }
    }

    public static  ArrayList<NoteCategory> categoryNote(ArrayList<NoteItem> list) {
        ArrayList <NoteCategory> cats = new ArrayList<>();
        /*Modified for bug 741244 @{*/
        boolean isCurrentYear = false;
        String currentYear = NoteCategory.getYear(new Date(System.currentTimeMillis()));
        if (list != null && list.size() != 0 ) {
            String preYear = "";
            Locale l = Locale.getDefault();
            NoteCategory category = null;
            for (int i = 0; i < list.size(); i++) {
                NoteItem item = list.get(i);
                long date = item.getLongDate();
                Date d = new Date(date);
                String itemYear = item.getYear(d);
                Log.i("NoteAdapter", "itemYear ========="+itemYear+": preYear = "+preYear);
                if (currentYear.equals(itemYear)) {
                    isCurrentYear = true;
                }
                if (!preYear.equals(itemYear)) {
                    category = new NoteCategory(itemYear);
                    preYear = itemYear;
                    category.addItem(item);
                    cats.add(category);
                } else {
                    if (category != null) {
                        category.addItem(item);
                    }
                }
            }

        }
        if (isCurrentYear) {
            for (int i = 0; i < cats.size(); i++) {
                String tempYear = cats.get(i).getHeaderTitle();
                if (tempYear.equals(currentYear) && i != 0) {
                    NoteCategory nc = cats.get(i);
                    cats.remove(i);
                    cats.add(0, nc);
                    Log.d("NoteAdapter", "nc.size = === "+nc.getItemCounts());
                    for (int j = nc.getItemCounts() - 1 ; j >= 0; j--) {
                        NoteItem temp = nc.getItem(j);
                        list.remove(temp);
                        list.add(0, temp);
                    }
                }
            }
        }
        /*}@*/
        return cats;
    }

    public static String getYear(Date d){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy", Locale.getDefault());
        return formatter.format(d);
    }
}