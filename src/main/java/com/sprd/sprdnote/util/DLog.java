package com.sprd.sprdnote.util;

import android.util.Log;

/**
 * Created by danny.liu on 2017/2/21.
 */

@SuppressWarnings("ALL")
public class DLog  {
    private static final boolean DEBUG = true;

    public static void d (String tag, String msg){
        if (DEBUG) {
            Log.d(tag, msg);
        } else {
            Log.i(tag, msg);
        }
    }

    public static void e (String tag, String msg){
            Log.e(tag, msg);
    }

    public static void v (String tag, String msg){
        if (DEBUG) {
            Log.v(tag, msg);
        }
    }

    public static void i (String tag, String msg){
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    public static void w (String tag, String msg){
        if (DEBUG) {
            Log.w(tag, msg);
        }
    }
}