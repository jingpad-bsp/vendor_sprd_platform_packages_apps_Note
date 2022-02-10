package com.sprd.sprdnote.folder;

import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.GetChars;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TextKeyListener;
import android.text.method.TransformationMethod;
import android.text.style.UpdateLayout;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Created by qian.dai on 2017/5/18.
 */

public class MyPasswordTransformationMethod extends PasswordTransformationMethod {

        public MyPasswordTransformationMethod() {
             super();
        }
    public CharSequence getTransformation(CharSequence source, View view) {
        if (source instanceof Spannable) {
            Spannable sp = (Spannable) source;

            /*
             * Remove any references to other views that may still be
             * attached.  This will happen when you flip the screen
             * while a password field is showing; there will still
             * be references to the old EditText in the text.
             */
            ViewReference[] vr = sp.getSpans(0, sp.length(),
                    ViewReference.class);
            for (int i = 0; i < vr.length; i++) {
                sp.removeSpan(vr[i]);
            }

            removeVisibleSpans(sp);

            sp.setSpan(new ViewReference(view), 0, 0,
                    Spannable.SPAN_POINT_POINT);
        }

        return new PasswordCharSequence(source);
    }

        public void onTextChanged(CharSequence s, int start,
                                  int before, int count) {
            if (s instanceof Spannable) {
                Spannable sp = (Spannable) s;
                ViewReference[] vr = sp.getSpans(0, s.length(),
                        ViewReference.class);
                if (vr.length == 0) {
                    return;
                }

            /*
             * There should generally only be one ViewReference in the text,
             * but make sure to look through all of them if necessary in case
             * something strange is going on.  (We might still end up with
             * multiple ViewReferences if someone moves text from one password
             * field to another.)
             */
                View v = null;
                for (int i = 0; v == null && i < vr.length; i++) {
                    v = vr[i].get();
                }

                if (v == null) {
                    return;
                }

                    if (count > 0) {
                        removeVisibleSpans(sp);

                        if (count == 1) {
                            sp.setSpan(new Visible(sp, this), start, start + count,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
            }
        }

        private static void removeVisibleSpans(Spannable sp) {
            Visible[] old = sp.getSpans(0, sp.length(), Visible.class);
            for (int i = 0; i < old.length; i++) {
                sp.removeSpan(old[i]);
            }
        }

        private static class PasswordCharSequence
                implements CharSequence
        {
            public PasswordCharSequence(CharSequence source) {
                mSource = source;
            }

            public int length() {
                return mSource.length();
            }

            public char charAt(int i) {
                if (mSource instanceof Spanned) {
                    Spanned sp = (Spanned) mSource;

                    int st = sp.getSpanStart(new NoCopySpan.Concrete());
                    int en = sp.getSpanEnd(new NoCopySpan.Concrete());

                    if (i >= st && i < en) {
                        return mSource.charAt(i);
                    }

                    Visible[] visible = sp.getSpans(0, sp.length(), Visible.class);

                    for (int a = 0; a < visible.length; a++) {
                        if (sp.getSpanStart(visible[a].mTransformer) >= 0) {
                            st = sp.getSpanStart(visible[a]);
                            en = sp.getSpanEnd(visible[a]);

                            if (i >= st && i < en) {
                                return mSource.charAt(i);
                            }
                        }
                    }
                }

                return DOT;
            }

            public CharSequence subSequence(int start, int end) {

                return mSource.subSequence(start, end);
            }

            public String toString() {
                return subSequence(0, length()).toString();
            }

            private CharSequence mSource;
        }

        private static class Visible
                extends Handler
                implements UpdateLayout, Runnable
        {
            public Visible(Spannable sp, android.text.method.PasswordTransformationMethod ptm) {
                mText = sp;
                mTransformer = ptm;
                postAtTime(this, SystemClock.uptimeMillis() + 300);
            }

            public void run() {
                mText.removeSpan(this);
            }

            private Spannable mText;
            private android.text.method.PasswordTransformationMethod mTransformer;
        }

        /**
         * Used to stash a reference back to the View in the Editable so we
         * can use it to check the settings.
         */
        private static class ViewReference extends WeakReference<View>
                implements NoCopySpan {
            public ViewReference(View v) {
                super(v);
            }
        }

        private static char DOT = '\u2022';
    }
