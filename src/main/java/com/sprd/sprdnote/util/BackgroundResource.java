package com.sprd.sprdnote.util;

import com.sprd.sprdnote.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by danny.liu on 2017/3/15.
 * Used to wallpaper
 */

public class BackgroundResource {

    private static final int BG_YELLOW = 0;
    private static final int BG_RED = 1;
    private static final int BG_BLUE = 2;
    private static final int BG_GREEN = 3;
    private static final int BG_WHITE = 4;

    public static final Map<Integer, Integer> sBgSelectorBtnsMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorBtnsMap.put(R.id.iv_bg_yellow, BG_YELLOW);
        sBgSelectorBtnsMap.put(R.id.iv_bg_red, BG_RED);
        sBgSelectorBtnsMap.put(R.id.iv_bg_blue, BG_BLUE);
        sBgSelectorBtnsMap.put(R.id.iv_bg_green, BG_GREEN);
        sBgSelectorBtnsMap.put(R.id.iv_bg_white, BG_WHITE);
    }

    public static final Map<Integer, Integer> sBgSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorSelectionMap.put(BG_YELLOW, R.id.iv_bg_yellow_select);
        sBgSelectorSelectionMap.put(BG_RED, R.id.iv_bg_red_select);
        sBgSelectorSelectionMap.put(BG_BLUE, R.id.iv_bg_blue_select);
        sBgSelectorSelectionMap.put(BG_GREEN, R.id.iv_bg_green_select);
        sBgSelectorSelectionMap.put(BG_WHITE, R.id.iv_bg_white_select);
    }

    public static final Map<Integer, Integer> sBgDrawableMap = new HashMap<Integer, Integer>();
    /*UNISOC: Modify for 1242655 @{*/
    static {
        sBgDrawableMap.put(BG_YELLOW, R.color.edit_yellow);
        sBgDrawableMap.put(BG_RED, R.color.edit_red);
        sBgDrawableMap.put(BG_BLUE, R.color.edit_blue);
        sBgDrawableMap.put(BG_GREEN, R.color.edit_green);
        sBgDrawableMap.put(BG_WHITE, R.color.edit_white);
    }
    /*}@*/
}