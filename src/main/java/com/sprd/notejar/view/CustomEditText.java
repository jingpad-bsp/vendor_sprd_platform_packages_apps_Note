package com.sprd.notejar.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import com.sprd.sprdnote.R;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by danny.liu on 2017/3/24.
 */

@SuppressWarnings("DefaultFileTemplate")
public class CustomEditText extends EditText {

    private static final String TAG = "NoteEditText";
    private static final String SCHEME_TEL = "tel:";
    private static final String SCHEME_HTTP = "http:";
    private static final String SCHEME_HTTPS = "https:";
    private static final String SCHEME_EMAIL = "mailto:";
    private static final int ID_PASTE = android.R.id.paste;

    private boolean isUsedTodo = false;
    private boolean pasterAvail = true;

    private Paint mPaint;
    //Offset from the beginning of the EditText
    private int mOffset;

    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();

    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.tel);
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.web);
        sSchemaActionResMap.put(SCHEME_HTTPS, R.string.web);
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.email);
    }

    // we need this constructor for LayoutInflater
    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setLinkTextColor(0xFF0069FF);
        setLinksClickable(false);
        mPaint = new Paint();
        PathEffect effects = new DashPathEffect(new float[]{
                1, 2, 4, 8
        }, 1);
        mPaint.setPathEffect(effects);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1);
        mPaint.setColor(Color.parseColor("#dbdbdb"));
           /* Field cursorDrawable = null;
            try {
                cursorDrawable =TextView.class.getDeclaredField("mCursorDrawableRes");
                cursorDrawable.setAccessible(true);
                cursorDrawable.set(this, R.drawable.cursor_shape_normal);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }*/

    }

   //Don't remove, will use.
   /* @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == ID_PASTE && isUsedTodo) {
            pasterAvail = false;
            super.onTextContextMenuItem(id);
            pasterAvail = true;
            return true;
        }
        return super.onTextContextMenuItem(id);
    }*/
    public void setUsedTodo(boolean bool) {
        isUsedTodo = bool;
    }

    public void setText(String text, boolean addTodo) {
        if (addTodo) {
            setText(text);
        }  else {
            pasterAvail = false;
            setText(text);
            pasterAvail = true;
        }
    }
    public Boolean getUsedTodo() {
        return isUsedTodo;
    }

    public boolean getPasterAvail() {
        return pasterAvail;
    }

    public void setPasterAvail (boolean avail) {
        pasterAvail = avail;
    }
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        /*SPRD: add 20131206 Spreadtrum of 241544 Cannot click common text, mail address, and phone number in a note right @{ */
        Selection.setSelection(getEditableText(), Math.min(mOffset, getEditableText().length()));
        /* @} */
        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();
        Log.d(TAG, "LinedEditText onCreateContextMenu start " + selStart + " end " + selEnd);

        int min = Math.min(selStart, selEnd);
        int max = Math.max(selStart, selEnd);

        final URLSpan[] urls = getText().getSpans(min, max, URLSpan.class);
        if (urls.length == 1) {
            int defaultResId = 0;
            /*UNSOC: Modify for bug 1235573,1242933 @{*/
            for (Map.Entry<String, Integer> entry : sSchemaActionResMap.entrySet()) {
                if (urls[0].getURL().indexOf(entry.getKey()) >= 0) {
                    defaultResId = entry.getValue();
                    break;
                }
            }
            /*}@*/
            if (defaultResId == 0) {
                defaultResId = R.string.note_link_other;
            }

            menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            // goto a new intent
                            try {
                                urls[0].onClick(CustomEditText.this);
                            } catch (Exception e) {
                                Log.e(TAG, "The website is not normal");
                            }
                            return true;
                        }
                    });
        }
        super.onCreateContextMenu(menu);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction();
        Layout layout = getLayout();
        int line = 0;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                /*Modified for 742915 @{*/
                if (layout != null) {
                    line = layout.getLineForVertical(getScrollY() + (int) event.getY());
                    mOffset = layout.getOffsetForHorizontal(line, (int) event.getX());
                }
                /*}@*/
                break;
        }
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new CustomEditText.DeleteInputConnection(super.onCreateInputConnection(outAttrs),
                true);
    }

    //UNISOC: Modify for bug 1235447
    private static class DeleteInputConnection extends InputConnectionWrapper {

        public DeleteInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}
