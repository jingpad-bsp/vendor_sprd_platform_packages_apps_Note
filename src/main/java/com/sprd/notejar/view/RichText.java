package com.sprd.notejar.view;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.notejar.view.data.NoteItem;
import com.sprd.notejar.view.util.NoteUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import com.sprd.sprdnote.R;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Main View container. Be used to display picture、edittext、todo list and so on .
 */
@SuppressLint({"NewApi", "InflateParams"})
public class RichText extends ScrollView {

    public static final int TODO_EDITTEXT = 1;
    public static final int NORMAL_EDITTEXT = 2;
    public static final String NOTE_TXT_TAG = "notetxt";
    public static final String NOTE_IMG_TAG = "noteimg";
    public static final String NOTE_TODO_TAG ="notetodo";
    private static final String TAG = "RichText";
    private static final Boolean IS_DEBUG = false;
    private static final int FIRST_VIEW = 0;
    private static final int SECOND_VIEW = 1;
    private static final int EDIT_PADDING = 10;
    private static final int EDIT_FIRST_PADDING_TOP = 10;
    private static final int HORIZONTAL_PADDING = 20;
    private static final int MAX_TEXT_NUM = 3000;

    private Context mContext;
    private ActivityManager mAm;
    private LinearLayout mAllLayout;             // It's a container that include all views.
    private LayoutInflater mInflater;
    private OnClickListener mDeleteBtnListener;
    private OnFocusChangeListener mFocusListener;       //Listener that Change EditText focus.
    private CustomEditText mLastFocusEdit;          //The EditText that last get the focus.
    private LinearLayout mFirstView;
    private TextView mDateTextView;                 //Display edited time.
    private ArrayList<File> mCacheImage = new ArrayList<>(); //Cache image that will be delete.;
    private ArrayList<DataImageView>  mImages= new ArrayList<>();

    private int textCounts;
    private int picCounts;
    private int richTextHeight;
    private int disappearingImageIndex = 0;
    private int backgroundId = -1;
    private boolean isEditType = true;

    public RichText(Context context) {
        this(context, null);
    }

    public RichText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RichText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mInflater = LayoutInflater.from(context);

        mAllLayout = new LinearLayout(context);
        mAllLayout.setOrientation(LinearLayout.VERTICAL);
        mAllLayout.setBackgroundColor(Color.TRANSPARENT);
        //setupLayoutTransitions();
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        addView(mAllLayout, layoutParams);
        mDeleteBtnListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                RelativeLayout parentView = (RelativeLayout) v.getParent();
                onImageCloseClick(parentView);
            }
        };

        mFocusListener = new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    /*UNISOC: Modify for bug 1235498 @{*/
                    if (v instanceof CustomEditText) {
                        mLastFocusEdit = (CustomEditText) v;
                    } else {
                        logd(TAG, "V is not CustomEditText!");
                    }
                    /*}@*/
                }
                logd(TAG, "hasFocus ==="+hasFocus+" last focus count ===========" + mAllLayout.indexOfChild((View) v.getParent()));
            }
        };
        LinearLayout.LayoutParams dateTextViewParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mDateTextView = new TextView(context);
        mDateTextView.setPadding(dip2px(HORIZONTAL_PADDING), dip2px(EDIT_FIRST_PADDING_TOP), dip2px(EDIT_PADDING), 0);
        mDateTextView.setTextSize(14);
        mAllLayout.addView(mDateTextView, dateTextViewParam);
    }

    public void setDateColor(int colorId){
        if (mDateTextView != null)
        mDateTextView.setTextColor(mContext.getColor(colorId));
    }

    public void addFirstEditor() {
        if (isEditType) {
            mFirstView = addTodoIndex(1, "", false, false);
            mLastFocusEdit = (CustomEditText) mFirstView.getChildAt(SECOND_VIEW);
        }
    }

    /**
     * delete image.
     */
    private void onImageCloseClick(View view) {
        disappearingImageIndex = mAllLayout.indexOfChild(view);
        mAllLayout.removeView(view);
        for (int i = 0; i < ((RelativeLayout) view).getChildCount(); i++) {
            if (((RelativeLayout) view).getChildAt(i) instanceof DataImageView) {
                String imagePath = ((DataImageView) ((RelativeLayout) view).getChildAt(i)).getAbsolutePath();
                if (imagePath != null && imagePath.length() != 0) {
                    File img = new File(imagePath);
                    mCacheImage.add(img);
                    mImages.remove(((RelativeLayout)view).getChildAt(FIRST_VIEW));
                    logi(TAG, "onImageClose....mPiccounts = = ="+picCounts);
                    if (picCounts > 0) {
                        --picCounts;
                    }
                    mergeEditText();
                }
            }
        }
    }

    /**
     * Product Image view with insert photo.
     */
    private RelativeLayout createImageLayout() {
        RelativeLayout layout = (RelativeLayout) mInflater.inflate(R.layout.edit_imageview, null);
        final DataImageView image = (DataImageView)layout.findViewById(R.id.edit_imageView);
        final View closeView = layout.findViewById(R.id.image_close);
        final View backgroundView = layout.findViewById(R.id.bg);
        backgroundView.setVisibility(GONE);
        closeView.setTag(layout.getTag());
        closeView.setOnClickListener(mDeleteBtnListener);
        image.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (closeView.getVisibility() != View.VISIBLE) {
                    closeView.setVisibility(View.VISIBLE);
                    backgroundView.setVisibility(VISIBLE);
                    image.setIsSelected(true);
                    int size = mImages.size();
                    for (int i = 0; i < size; i++) {
                        if (mImages.get(i).getTag() != null && mImages.get(i).getTag()
                                != image.getTag() && mImages.get(i).isSelected()) {
                            mImages.get(i).setIsSelected(false);
                            ((RelativeLayout)(mImages.get(i).getParent())).getChildAt(1).setVisibility(GONE);
                            ((RelativeLayout)(mImages.get(i).getParent())).getChildAt(2).setVisibility(GONE);
                        }
                    }
                } else {
                    closeView.setVisibility(View.GONE);
                    backgroundView.setVisibility(GONE);
                }
                return true;
            }
        });
        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (closeView.getVisibility() == View.VISIBLE) {
                    closeView.setVisibility(View.GONE);
                    backgroundView.setVisibility(GONE);
                }
            }
        });

        return layout;
    }

    /**
     * Add image by absolutely path.
     *
     * @param imagePath
     */
    public void insertImage(String imagePath, String failStr) {
        isEditType = true;
        logd(TAG, "imagePath = " + imagePath + "width = =" + getWidth());
        int width = 0;
        int height = 0;
        Resources r = mContext.getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        height = dm.heightPixels;
        if (getWidth() == 0) {
            width = dm.widthPixels;
        } else {
            width = getWidth();
        }
        Bitmap bmp = getScaledBitmap(imagePath, width);
        if (bmp == null) {
            logi(TAG, "bmp is null....");
            Toast.makeText(mContext, failStr, Toast.LENGTH_SHORT).show();
            return;
        } else if (bmp.getWidth() > width*2 || bmp.getHeight() > height*2){
            loge(TAG, "bmp.width = "+bmp.getWidth()+":: bmp.height = "+bmp.getHeight()
                    +" :: width = "+width+" :: height = "+height);
            Toast.makeText(mContext, R.string.pic_large, Toast.LENGTH_SHORT).show();
            return;
        }
        insertImage(bmp, imagePath);
        mLastFocusEdit.requestFocus();
        mLastFocusEdit.setSelection(mLastFocusEdit.getText().length(), mLastFocusEdit.getText().length());
    }

    /**
     * Add image by bitmap.
     */
    private void insertImage(Bitmap bitmap, String imagePath) {
        LinearLayout normalLayout = null;
        if (((View)mLastFocusEdit.getParent()).getTag() != null &&
                (int)((View)mLastFocusEdit.getParent()).getTag() == TODO_EDITTEXT) {
            int lastEditIndex = mAllLayout.indexOfChild((LinearLayout) mLastFocusEdit.getParent());
            if (mAllLayout.getChildCount() - 1 == lastEditIndex) {
                logi(TAG, "inertImage,childCount+++++++++++");
                normalLayout = addTodoIndex(lastEditIndex + 1, "", false, false);
            }
            addImageViewAtIndex(lastEditIndex + 1, imagePath, bitmap);
            if (isEditType) {
                if (normalLayout != null) {
                    CustomEditText editText = (CustomEditText) normalLayout.getChildAt(SECOND_VIEW);
                    mLastFocusEdit = editText;
                    editText.requestFocus();
                    editText.setSelection(editText.getText().length(), editText.getText().length());
                } else {
                    mLastFocusEdit.requestFocus();
                    mLastFocusEdit.setSelection(mLastFocusEdit.getText().length(), mLastFocusEdit.getText().length());
                }
            }
        } else {
            String lastEditStr = mLastFocusEdit.getText().toString();
            int cursorIndex = mLastFocusEdit.getSelectionStart();
            //UNISOC: Modify for bug 1205904
            String editStr1 = lastEditStr.substring(0, cursorIndex);
            int lastEditIndex = mAllLayout.indexOfChild((LinearLayout) mLastFocusEdit.getParent());
            logd(TAG, "inertImage,....lastEditIndex = " + lastEditIndex);
            if (lastEditStr.length() == 0 || editStr1.length() == 0) {
                /*Modified for bug712271: start*/
                if (mAllLayout.getChildCount() - 1 == lastEditIndex) {
                    mAllLayout.removeView((LinearLayout)getLastFocusEdit().getParent());
                    lastEditIndex = mAllLayout.getChildCount();
                    /*Modified for bug 733792: start*/
                    if (lastEditStr.length() != 0) {
                        textCounts = textCounts - lastEditStr.codePointCount(0,lastEditStr.length());
                        normalLayout = addTodoIndex(lastEditIndex, lastEditStr, false, false);
                    } else {
                        normalLayout = addTodoIndex(lastEditIndex, "", false, false);
                    }
                    /*Modified for bug 733792: end*/
                }
                addImageViewAtIndex(lastEditIndex, imagePath, bitmap);
                if (isEditType) {
                    if (normalLayout != null) {
                        CustomEditText editText = (CustomEditText) normalLayout.getChildAt(SECOND_VIEW);
                        mLastFocusEdit = editText;
                        editText.requestFocus();
                        editText.setSelection(editText.getText().length(), editText.getText().length());
                    }else {
                        mLastFocusEdit.requestFocus();
                        mLastFocusEdit.setSelection(mLastFocusEdit.getText().length(), mLastFocusEdit.getText().length());
                    }
                }
                /*Modified for bug712271: end*/
            } else {
                mLastFocusEdit.setText(editStr1);
                //UNISOC: Modify for bug 1205904
                String editStr2 = lastEditStr.substring(cursorIndex, lastEditStr.length());
                if (mAllLayout.getChildCount() - 1 == lastEditIndex
                        || editStr2.length() > 0) {
                    logi(TAG, "inertImage,childCount");
                    normalLayout = addTodoIndex(lastEditIndex + 1, editStr2, false, false);
                }
                addImageViewAtIndex(lastEditIndex + 1, imagePath, bitmap);
                if (isEditType) {
                    if (normalLayout != null) {
                        CustomEditText editText = (CustomEditText) normalLayout.getChildAt(SECOND_VIEW);
                        mLastFocusEdit = editText;
                        editText.requestFocus();
                        editText.setSelection(editText.getText().length(), editText.getText().length());
                    } else {
                        mLastFocusEdit.requestFocus();
                        mLastFocusEdit.setSelection(mLastFocusEdit.getText().length(), mLastFocusEdit.getText().length());
                    }
                }
            }
        }
        hideKeyBoard();
    }

    /**
     * Insert todo list.
     * 1)todo after todo
     * 2)todo after content
     * 3)todo after image
     *
     * @param hint
     */
    public void insertTodo(String hint, View v, String noTodoStr) {
        if (v != null && ((View) v.getParent()).getTag() != null && (int) ((View) v.getParent()).getTag() == TODO_EDITTEXT) {
            if (v instanceof CustomEditText) {
                removeTodo(v);
            }
            return;
        } else if (v instanceof CustomEditText) {
            CustomEditText focusEdit = (CustomEditText) v;
            int lineCount = focusEdit.getLineCount();
            int cursorStartIndex = focusEdit.getSelectionStart();
            int cursorEndIndex = focusEdit.getSelectionEnd();
            String editContent = focusEdit.getText().toString();
            String toDoText = "";
            /*UNISOC: Modify for bug 1235490 @{*/
            StringBuffer preText = new StringBuffer();
            StringBuffer lastText = new StringBuffer();
            boolean isGetCursor = false;
            int charCount = 0;
            logd(TAG, focusEdit.getText().toString().length() + "  lastFocusEdit line == " + lineCount + " cursorIndex=" + cursorStartIndex + ": " + focusEdit.getSelectionEnd() + " ::" + focusEdit.getSelectionStart());
            if (cursorStartIndex != cursorEndIndex) {
                Toast.makeText(mContext, noTodoStr, Toast.LENGTH_SHORT).show();
                return;
            }
            Layout l = focusEdit.getLayout();
            int start = 0;
            for (int i = 0; i < lineCount; i++) {
                int end = l.getLineEnd(i);
                String lineContent = editContent.substring(start, end);
                charCount += lineContent.length();
                logd(TAG, "lineContet = " + lineContent + ":  charcount = " + charCount);
                if ((charCount > cursorStartIndex
                        || ((i == lineCount - 1) && charCount == cursorStartIndex)) && !isGetCursor) {
                    toDoText = lineContent;
                    isGetCursor = true;
                    start = end;
                    continue;
                }
                logd(TAG, "line[" + i + "] = " + lineContent+" : : isGetCursor === "+isGetCursor);
                start = end;
                if (!isGetCursor) {
                    preText.append(lineContent);
                } else {
                    lastText.append(lineContent);
                }
            }
            logi(TAG, "pre Text len = " + preText.length() + "toDoText =" + toDoText.length());
            if (toDoText.length() != 0) {
                Pattern p = Pattern.compile("\r|\n");
                Matcher m = p.matcher(toDoText);
                toDoText = m.replaceAll("");
            }

            logd(TAG, "pre Text len == " + preText.length() + "toDoText = " + toDoText.length());
            if (preText.length() != 0) {
                int lastIndex = preText.lastIndexOf("\n");
                if (lastIndex != -1 && lastIndex == preText.length()-1){
                    focusEdit.setText(preText.substring(0,lastIndex));
                } else {
                    focusEdit.setText(preText.toString());
                }
                LinearLayout todo = addTodoIndex(mAllLayout.indexOfChild((View) focusEdit.getParent()) + 1, toDoText, false, true);
                if (lastText.length() != 0) {
                    addTodoIndex(mAllLayout.indexOfChild((View) focusEdit.getParent()) + 2, lastText.toString(), false, false);
                }
                todo.getChildAt(SECOND_VIEW).requestFocus();
                mLastFocusEdit = (CustomEditText) todo.getChildAt(SECOND_VIEW);
            } else {
                ((LinearLayout) focusEdit.getParent()).getChildAt(FIRST_VIEW).setVisibility(VISIBLE);
                ((LinearLayout) focusEdit.getParent()).setTag(TODO_EDITTEXT);
                focusEdit.setUsedTodo(true);
                if (toDoText.length() != 0) {
                    focusEdit.setText(toDoText);
                } else {
                    focusEdit.setText("");
                }
                if (lastText.length() != 0) {
                    addTodoIndex(mAllLayout.indexOfChild((View) focusEdit.getParent()) + 1, lastText.toString(), false, false);
                }
                /*}@*/
                focusEdit.requestFocus();
                focusEdit.setSelection(focusEdit.getText().length(), focusEdit.getText().length());
            }
        }
    }

    /* UNISOC: add for bug1144942 1213087{@ */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mLastFocusEdit != null) {
            int allViewCount = mAllLayout.getChildCount();
            View lastView = mAllLayout.getChildAt(allViewCount - 1);
            if (lastView instanceof LinearLayout && event.getY() > lastView.getBottom()
                                                 && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mLastFocusEdit = (CustomEditText) ((LinearLayout) lastView).getChildAt(SECOND_VIEW);
                mLastFocusEdit.requestFocus();
                mLastFocusEdit.setSelection(mLastFocusEdit.getText().toString().length(), mLastFocusEdit.getText().toString().length());
                openKeyBoard();    //Unisoc: update for Bug1168625
            }
        }
        return super.dispatchTouchEvent(event);
    }
    /* @} */

    /**
     * Change todo to a normal edittext when we click TODO Image in the bottom.
     * @param v Focused edittext.
     */
    private void removeTodo(View v) {
        CustomEditText edit = (CustomEditText) v;
        String toDoStr = "";
        if (edit.getText() != null) {
            toDoStr = edit.getText().toString();
        }
        int allViewCount = mAllLayout.getChildCount();
        LinearLayout toDoLayout = (LinearLayout) v.getParent();
        int toDoLayoutIndex = mAllLayout.indexOfChild(toDoLayout);
        int toDo_editIndex = toDoLayout.indexOfChild(edit);
        if (allViewCount != 1) {
            View toDoPreView = mAllLayout.getChildAt(toDoLayoutIndex - 1);
            if (toDoLayoutIndex != allViewCount - 1) {
                View toDoSufView = mAllLayout.getChildAt(toDoLayoutIndex + 1);
                logd(TAG, "toDoPreView tag =" + (toDoPreView instanceof LinearLayout?(int) toDoPreView.getTag():-1));
                if (toDoPreView instanceof LinearLayout && (int) toDoPreView.getTag() == NORMAL_EDITTEXT) {
                    CustomEditText preEdit = (CustomEditText) ((LinearLayout) toDoPreView).getChildAt(1);
                    String preStr = preEdit.getText().toString();
                    String text = "";
                    if (toDoSufView instanceof LinearLayout && (int) toDoSufView.getTag() == NORMAL_EDITTEXT) {
                        String sufStr = ((CustomEditText) ((LinearLayout) toDoSufView).getChildAt(1)).getText().toString();
                        if (toDoStr.length() != 0 && sufStr.length() != 0) {
                            text = preStr + "\n" + toDoStr + "\n" + sufStr;
                        } else if (toDoStr.length() == 0 && sufStr.length() != 0) {
                            text = preStr + "\n" + sufStr;
                        } else if (toDoStr.length() != 0 && sufStr.length() == 0) {
                            text = preStr + "\n" + toDoStr;
                        } else {
                            text = preStr;
                        }
                        mAllLayout.removeView(toDoSufView);
                        textCounts = textCounts - sufStr.codePointCount(0, sufStr.length());
                    } else {
                        if (toDoStr.length() != 0) {
                            text = preStr + "\n" + toDoStr;
                        } else {
                            text = preStr;
                        }
                        logi(TAG, " suf ==== null remove Todo text === " + text);
                    }
                    textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length());
                    mAllLayout.removeView(toDoLayout);
                    preEdit.setText(text);
                    preEdit.requestFocus();
                    preEdit.setSelection(preEdit.getText().toString().length(), preEdit.getText().toString().length());
                } else {
                    ((RelativeLayout) toDoLayout.getChildAt(FIRST_VIEW)).getChildAt(FIRST_VIEW).setVisibility(VISIBLE);
                    ((RelativeLayout) toDoLayout.getChildAt(FIRST_VIEW)).getChildAt(SECOND_VIEW).setVisibility(GONE);
                    toDoLayout.getChildAt(FIRST_VIEW).setVisibility(GONE);
                    toDoLayout.setTag(NORMAL_EDITTEXT);
                    ((CustomEditText) (toDoLayout.getChildAt(SECOND_VIEW))).setUsedTodo(false);
                    ((CustomEditText) (toDoLayout.getChildAt(SECOND_VIEW))).setPaintFlags(
                            (((CustomEditText) (toDoLayout.getChildAt(SECOND_VIEW))).getPaintFlags()) & ~Paint.STRIKE_THRU_TEXT_FLAG);//Paint.STRIKE_THRU_TEXT_FLAG
                    CustomEditText todoEdit = (CustomEditText) toDoLayout.getChildAt(SECOND_VIEW);
                    if (toDoSufView instanceof LinearLayout && (int) toDoSufView.getTag() == NORMAL_EDITTEXT) {
                        CustomEditText sufEdit = (CustomEditText) ((LinearLayout) toDoSufView).getChildAt(SECOND_VIEW);
                        String sufStr = sufEdit.getText().toString();

                        mAllLayout.removeView(toDoSufView);
                        textCounts = textCounts - sufStr.codePointCount(0, sufStr.length());
                        if (toDoStr.length() != 0) {
                            todoEdit.setText(toDoStr + "\n" + sufStr);
                        } else {
                            todoEdit.setText(sufStr);
                        }
                        todoEdit.requestFocus();
                        todoEdit.setSelection(todoEdit.getText().toString().length(),
                                todoEdit.getText().toString().length());

                    } else {
                        todoEdit.requestFocus();
                    }
                }

            } else {
                if (toDoPreView instanceof LinearLayout && (int) toDoPreView.getTag() == NORMAL_EDITTEXT) {
                    CustomEditText pre = (CustomEditText) ((LinearLayout) ((LinearLayout) toDoPreView)).getChildAt(1);
                    String preStr = pre.getText().toString();
                    mAllLayout.removeView(toDoLayout);
                    textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length());
                    pre.setText(preStr + "\n" + toDoStr);
                    pre.requestFocus();
                    pre.setSelection(pre.getText().toString().length(), pre.getText().toString().length());

                    logd(TAG, "text =" + preStr + toDoStr + " ::: textCounts = "+textCounts);
                } else {
                    ((RelativeLayout) toDoLayout.getChildAt(FIRST_VIEW)).getChildAt(FIRST_VIEW).setVisibility(VISIBLE);
                    ((RelativeLayout) toDoLayout.getChildAt(FIRST_VIEW)).getChildAt(SECOND_VIEW).setVisibility(GONE);
                    toDoLayout.getChildAt(FIRST_VIEW).setVisibility(GONE);
                    toDoLayout.setTag(NORMAL_EDITTEXT);
                    ((CustomEditText) (toDoLayout.getChildAt(SECOND_VIEW))).setUsedTodo(false);
                    toDoLayout.getChildAt(SECOND_VIEW).requestFocus();
                    ((CustomEditText) (toDoLayout.getChildAt(SECOND_VIEW))).setPaintFlags(
                            (((CustomEditText) (toDoLayout.getChildAt(SECOND_VIEW))).getPaintFlags()) & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    mLastFocusEdit = (CustomEditText) toDoLayout.getChildAt(SECOND_VIEW);
                }
            }
        }
    }

    public void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(RichText.this.getWindowToken(), 0);
    }

    /* UNISOC: add for bug1144942 {@ */
    public void openKeyBoard() {    //Unisoc: update for Bug1168625
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);    //Unisoc: update for Bug1168625
    }
    /* @} */

    /**
     *Display note content.
     * @param content The Note's content that is xml style.
     * @param resId The background color's resid.
     * @param time  Note's created time.
     */
    public void setContent(String content,int resId , String time, String failStr) {
        logd(TAG, "content = = " + content + ":: resId =  " + resId + "time = " + time+ ":: picnume= "+picCounts);
        isEditType = false;
        setDisplayTime(time);
        Document doc = NoteUtils.string2Doc(content);
        if (doc == null){
            return;
        }
        Element rootElement = doc.getDocumentElement();
        NodeList list = rootElement.getChildNodes();
        View preView = null;
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            int count = mAllLayout.getChildCount();
            View lastView = mAllLayout.getChildAt(count - 1);
            logd(TAG, "Node === " + n.getNodeName() + ": count = " + count + " : text = " + n.getTextContent());
            if (n.getNodeName().equals(NOTE_TXT_TAG)) {
                String nodeText = n.getTextContent();
                String text = "";
                if (lastView instanceof LinearLayout && (int) (lastView.getTag()) == 2) {
                    CustomEditText editText = (CustomEditText) ((LinearLayout) lastView).getChildAt(1);
                    String preStr = editText.getText().toString();
                    if (preStr.length() != 0) {
                        if (nodeText.length() != 0 && text.length() != 0) {
                            text = preStr + "\n" + text;
                           // Log.d(TAG, "text ======="+text+":..."+nodeText);
                        } else {
                            text = preStr;
                        }
                    } else {
                        text = nodeText;
                    }
                    editText.setText(text);
                    mLastFocusEdit = (editText);
                    mLastFocusEdit.requestFocus();
                    mLastFocusEdit.setSelection(editText.getText().toString().length(),
                            editText.getText().toString().length());
                } else if (lastView instanceof RelativeLayout) {
                    addTodoIndex(count, nodeText, false, false);
                } else if (lastView instanceof LinearLayout && (int) (lastView.getTag()) == TODO_EDITTEXT) {
                    addTodoIndex(count, nodeText, false, false);
                } else {
                    addTodoIndex(count, nodeText, false, false);
                }
            } else if (n.getNodeName().equals(NOTE_IMG_TAG)) {
                String imgPath = n.getTextContent();
                Bitmap bmp = getScaledBitmap(imgPath, getWidth());
                if (bmp == null) {
                    logi(TAG, "bmp is null....");
                    Toast.makeText(mContext, failStr, Toast.LENGTH_SHORT).show();
                    continue;
                }
                addImageViewAtIndex(count, imgPath, bmp);
            } else if (n.getNodeName().equals(NOTE_TODO_TAG)) {
                String todoStr = n.getTextContent();
                //UNISOC: Modify for bug 1235487
                boolean finished = n.getAttributes().getNamedItem("status") != null ?
                                   n.getAttributes().getNamedItem("status").getNodeValue().equals("true") : false;
                if (lastView instanceof LinearLayout && (int) (lastView.getTag()) == 2) {
                    CustomEditText editText = (CustomEditText) ((LinearLayout) lastView).getChildAt(1);
                    String preStr = editText.getText().toString();
                    if (preStr.length() != 0) {
                        addTodoIndex(count, todoStr, finished, true);
                    } else {
                        mAllLayout.removeView(lastView);
                        addTodoIndex(count - 1, todoStr, finished, true);
                    }
                } else {
                    addTodoIndex(count, todoStr, finished, true);
                }
            }

        }
        //Display top content when read note and note edit.
        mAllLayout.getChildAt(FIRST_VIEW).setFocusable(true);
        mAllLayout.getChildAt(FIRST_VIEW).setFocusableInTouchMode(true);
        mAllLayout.getChildAt(FIRST_VIEW).requestFocus();
        isEditType = true;
    }

    /**
     * Save note's content in xml style.
     * @param folderId ParentFolder Id.
     * @param item
     * @return
     */
    public NoteItem saveContent(int folderId, NoteItem item, boolean persist, String defaultTitle) {
        DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;
        try {
            builder = df.newDocumentBuilder();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Document doc = builder.newDocument();
        Element element = doc.createElement("note");

        doc.appendChild(element);
        String content = "";
        /*Add for bug 735476: start*/
        String tempContent = "";
        /*Add for bug 735476: end*/
        boolean hasTitle = false;
        boolean hasPicture = false;
        item.setParentFolderId(folderId);
        item.setContent(content);
        for (int i = 0; i < mAllLayout.getChildCount(); i++) {
            View v = mAllLayout.getChildAt(i);
            if (v instanceof LinearLayout && v.getTag() != null && ((int) (v.getTag())) == NORMAL_EDITTEXT) {
                content += ((CustomEditText) (((LinearLayout) v).getChildAt(SECOND_VIEW))).getText().toString();
                tempContent += ((CustomEditText) (((LinearLayout) v).getChildAt(SECOND_VIEW))).getText().toString();
               logd(TAG, "content === "+content);
                if (i == 1) {
                    CustomEditText currentEditText = ((CustomEditText) (((LinearLayout) v).getChildAt(SECOND_VIEW)));
                    String title = getTitle(content, currentEditText);
                    /*UNISOC: Modify for bug 1427277 @{*/
                    currentEditText.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            currentEditText.getViewTreeObserver().removeOnPreDrawListener(this);
                            if (item.getTitle() == null) {
                                Log.d(TAG, "update item title");
                                String title1 = getTitle(currentEditText.getText().toString(), currentEditText);
                                item.setTitle(title1);
                            }
                            return false;
                        }
                    });
                    /* @} */
                    if (mAllLayout.getChildCount() == 2 && (currentEditText.getText().toString()).trim().length() == 0 ) {
                        return null;
                    }
                    if (title != null && title.length() != 0) {
                        hasTitle = true;
                        item.setTitle(title);
                    }
                } else if (!hasTitle) {
                    CustomEditText currentEditText = ((CustomEditText) (((LinearLayout) v).getChildAt(SECOND_VIEW)));
                    String title = getTitle(currentEditText.getText().toString(), currentEditText);
                    if (title != null && title.length() != 0) {
                        hasTitle = true;
                        item.setTitle(title);
                    }
                }
                Element text = doc.createElement(NOTE_TXT_TAG);
                Text text1 = doc.createTextNode(((CustomEditText) (((LinearLayout) v).getChildAt(1))).getText().toString());
                element.appendChild(text);
                text.appendChild(text1);
            } else if (v instanceof RelativeLayout) {
               int count = ((RelativeLayout) v).getChildCount();
               if (!hasTitle) {
                    hasTitle = true;
                    item.setTitle(defaultTitle);
                }
                if (count > 0) {
                    for (int j = 0; j < count; j++) {
                        if (((RelativeLayout) v).getChildAt(j) instanceof DataImageView) {
                            content += "<img>" + ((DataImageView) ((RelativeLayout) v).getChildAt(j)).getAbsolutePath() + "</img>";
                            hasPicture = true;
                            logd(TAG, "content = " + content);
                            tempContent = content;
                            Element img = doc.createElement(NOTE_IMG_TAG);
                            //img.setTextContent(((DataImageView)((RelativeLayout) v).getChildAt(j)).getAbsolutePath());
                            Text text = doc.createTextNode(((DataImageView) ((RelativeLayout) v).getChildAt(j)).getAbsolutePath());
                            element.appendChild(img);
                            img.appendChild(text);
                        }
                    }
                }
            } else if (v instanceof LinearLayout) {
                String state = "";
                String toDoStr = "";
                Element todo = doc.createElement(NOTE_TODO_TAG);
                for (int m = 0; m < ((LinearLayout) v).getChildCount(); m++) {
                    View view = ((LinearLayout) v).getChildAt(m);
                    if (view instanceof RelativeLayout) {
                        for (int n = 0; n < ((RelativeLayout) view).getChildCount(); n++) {
                            if ((int) (((RelativeLayout) view).getChildAt(n).getTag()) == R.id.img_todo_on) {
                                Attr attr = doc.createAttribute("status");
                                if (((ImageView) ((RelativeLayout) view).getChildAt(n)).getVisibility() == View.VISIBLE) {
                                    state = "status:false";
                                    attr.setNodeValue("false");
                                } else {
                                    state = "status:true";
                                    attr.setNodeValue("true");
                                }
                                todo.setAttributeNode(attr);
                            }
                        }
                    } else if (view instanceof CustomEditText) {
                        CustomEditText currentEditText = ((CustomEditText) view);
                        toDoStr = currentEditText.getText().toString();
                        if (i == 1 && toDoStr != null && toDoStr.trim().length() != 0) {
                            String title = getTitle(toDoStr, currentEditText);
                            if (title != null && title.length() != 0) {
                                hasTitle = true;
                                item.setTitle(title);
                            }
                        } else if (i != 1 && !hasTitle && toDoStr != null && toDoStr.trim().length() != 0) {
                            String title = getTitle(toDoStr, currentEditText);
                            if (title != null && title.length() != 0) {
                                hasTitle = true;
                                item.setTitle(title);
                            }
                        }
                        /*if (i == 1 && toDoStr != null && toDoStr.trim().length() == 0
                                && (mAllLayout.getChildCount() == 2 || (mAllLayout.getChildCount() == 3
                                && (mAllLayout.getChildAt(2)) instanceof EditText)
                                && ((CustomEditText) mAllLayout.getChildAt(2)).getText().toString().trim().length() == 0)) {
                            return null;
                        }*/
                        Text txt = doc.createTextNode(toDoStr);
                        todo.appendChild(txt);
                        element.appendChild(todo);
                    }
                }

                if (toDoStr.length() != 0) {
                    content += "<Todo>" + state + " " + toDoStr + "</Todo>";
                }
                if (toDoStr.trim().length() != 0) {
                    tempContent = content;
                }
            }
        }

        if (content != null && content.trim().length() != 0 && tempContent.trim().length() != 0) {
            DOMSource source = new DOMSource(doc);
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer();
                StringWriter stringWriter = new StringWriter();
                StreamResult result = new StreamResult(stringWriter);//new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/NoteBook/tmp.xml"));
                transformer.transform(source, result);
                Log.d(TAG, "xml====================" + stringWriter.getBuffer().toString());
                content = stringWriter.getBuffer().toString();
                item.setContent(content);
                item.setHasPictures(hasPicture);
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException ex) {
                ex.printStackTrace();
            }

        } else {
            return null;
        }
        if (persist) {
            for (int i = 0; i < mCacheImage.size(); i++) {
                File img = mCacheImage.get(i);
                if (img.exists() && img.isFile()) {
                    //UNISOC: Modify for bug 1235479
                    if (!img.delete()) {
                        loge(TAG, img + "delete failed");
                    }
                }
            }
            mCacheImage.clear(); //Need?
        }
        logd(TAG, " All content =" + content);
        return item;
    }

    /*Add for bug 711375*/
    public void removeContent(){
        /*Modified for bug 732556: start*/
        logd(TAG, "removeCount ="+mAllLayout.getChildCount());
        if(mAllLayout.getChildCount() > 1) {//count > 1
            for (int count = mAllLayout.getChildCount() - 1; count > 0; count--) {
                mAllLayout.removeView(mAllLayout.getChildAt(count));
                logd(TAG, "removeContent......count = '"+mAllLayout.getChildCount());
            }
        }
        /*Modified for bug 732556: end*/
        logd(TAG, "removeContent......count = '"+mAllLayout.getChildCount());
        picCounts = 0;
        textCounts = 0;
    }

    private String getTitle(String content, CustomEditText currentEditText){
        if (content != null && content.length() != 0) {
            int lineCount = currentEditText.getLineCount();
            Layout l = currentEditText.getLayout();
            int start = 0;
            String title = "";
            for (int j = 0; j < lineCount; j++) {
                int end = l.getLineEnd(j);
                title = content.substring(start, end).trim();
                if (title.trim().length() != 0) {
                    break;
                }
            }
            if (title.trim().length() != 0) {
                return title;
            }
        }
        return null;
    }
    /**
     *Add todo or normal editText into note.
     * @param index Add position.
     * @param hint  Edittext's content.
     * @param isChecked It's used to display todo's status.on(true) or off(false) while this is a Todo EditText.
     * @param isTodo   Todo or not. It is a todo (true) or not(false).
     * @return
     */
    public LinearLayout addTodoIndex(final int index, String hint, boolean isChecked, boolean isTodo) {
        final LinearLayout toDoLayout = createTodoLayout();

        final ImageView checkbox_on = (ImageView) toDoLayout.findViewById(R.id.img_todo_on);
        final ImageView checkbox_off = (ImageView) toDoLayout.findViewById(R.id.img_todo_off);
        final CustomEditText edt = (CustomEditText) toDoLayout.findViewById(R.id.et_todo);
        //
        if (mAm !=  null && mAm.isUserAMonkey()) {
            logi(TAG, "Monkey running.");
            edt.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
        final int flags = edt.getPaintFlags();
        if (isTodo) {
            toDoLayout.setTag(TODO_EDITTEXT);
            edt.setUsedTodo(true);
        } else {
            toDoLayout.setTag(NORMAL_EDITTEXT);
            edt.setUsedTodo(false);
        }
        toDoLayout.setBackgroundColor(Color.TRANSPARENT);
        edt.setAutoLinkMask(Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS);
        checkbox_on.setTag(R.id.img_todo_on);
        checkbox_off.setTag(R.id.img_todo_off);

        edt.addTextChangedListener(new TextWatcher() {
            int beforeCounts = 0;
            int afterCounts = 0;
            String temp = null;
            int startCursor = -1;
            Toast toast = null;  //Add for bug1187946
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                logd(TAG, "length ="+charSequence.length()+ " : char = "+charSequence);
                // beforeCounts = charSequence.length();
                beforeCounts = charSequence.toString().codePointCount(0, charSequence.length());
                temp = charSequence.toString();
                startCursor = edt.getSelectionStart();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                logd(TAG, " char ="+charSequence);
                /*Method one from google :https://issuetracker.google.com/issues/37007899*/
                /*SPRD: 735611 @{*/
                float add = edt.getLineSpacingExtra();
                float mul = edt.getLineSpacingMultiplier();
                edt.setLineSpacing(0f, 1f);
                edt.setLineSpacing(add, mul);
                /*@}*//*

                /*Method two from http://blog.csdn.net/u011592300/article/details/65446589
                if (i2 != i1) {
                    int startSelection = edt.getSelectionStart();
                    edt.setText(charSequence);
                    edt.setSelection(startSelection);
                }*/
            }

            @Override
            public void afterTextChanged(Editable editable) {
                logd(TAG, "mTextCount ="+textCounts);
                logd(TAG, "afterTextChanged " + edt.getPasterAvail() + " : editabl  == " + editable.toString() + edt.getUsedTodo());
                //afterCounts = editable.length();
                afterCounts = editable.toString().codePointCount(0, editable.length());
                textCounts = textCounts+(afterCounts-beforeCounts);
                logd(TAG, "aftert == "+afterCounts+" beforce == "+beforeCounts);
                /* SPRD: modified for bug1017406 @{*/
                if (afterCounts > beforeCounts) {
                    if (textCounts >= MAX_TEXT_NUM) {    //Unisoc: modify for Bug1166184
                        int mStart = afterCounts - (textCounts - MAX_TEXT_NUM);    //Unisoc: modify for Bug1174608
                        /*UNISOC: Add for bug1187946 {@ */
                        if (null != toast) {
                            toast.cancel();
                        }
                        toast = Toast.makeText(mContext, R.string.input_limit, Toast.LENGTH_SHORT);
                        toast.show();
                        /* @} */
                        if (afterCounts - beforeCounts > 1 && mStart >= 0) {
                            editable.delete(mStart, afterCounts);
                            /* UNISOC: add for bug1147180 {@ */
                            if (afterCounts < beforeCounts) {
                                edt.setSelection(afterCounts, afterCounts);
                            } else {
                                edt.setSelection(mStart, mStart);
                            }
                            /* @} */
                        } else {
                            if (startCursor != -1) {
                                edt.setSelection(startCursor);
                            }
                            edt.setText(temp);
                            if (startCursor != -1) {
                                edt.setSelection(startCursor);
                            }
                        }
                        return;
                    }
                }
                /* @} */
                if (edt.getPasterAvail()) {
                    if (isEditType && edt.getUsedTodo()) {
                        int enterIndex = editable.toString().lastIndexOf('\n');
                        if (enterIndex != -1) {
                            String temp = "";
                            if (enterIndex < editable.length()) {
                                temp = editable.toString().substring(enterIndex + 1, editable.length());
                            } else {
                                temp = "";
                            }
                            edt.setText(editable.toString().substring(0, enterIndex), false);
                            int index = mAllLayout.indexOfChild(((LinearLayout) edt.getParent()));
                            logd(TAG, "index === " + index + " : str== " + editable.toString().substring(0, enterIndex));
                            addTodoIndex(index + 1, temp, false, true);
                        }
                    } else {
                        mLastFocusEdit = edt;
                    }
                } else {
                    edt.setPasterAvail(true);
                }
            }
        });

        edt.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    /*UNISOC: Modify for bug 1235498 @{*/
                    CustomEditText edit = null;
                    if (v instanceof CustomEditText) {
                        edit = (CustomEditText) v;
                    } else {
                        logd(TAG, "edt onkeylistener v is not CustomEditText!");
                        return false;
                    }
                    /*}@*/
                    String toDoStr = "";
                    if (edit.getText() != null) {
                        toDoStr = edit.getText().toString();
                    }
                    int startSelection = edit.getSelectionStart();
                    if (startSelection == 0) {
                        int allViewCount = mAllLayout.getChildCount();
                        int toDoLayoutIndex = mAllLayout.indexOfChild(toDoLayout);
                        if (toDoLayout.getChildCount() != 1) {
                            View toDoPreView = mAllLayout.getChildAt(toDoLayoutIndex - 1);
                            if (toDoLayoutIndex != allViewCount - 1) {
                                View toDoSufView = mAllLayout.getChildAt(toDoLayoutIndex + 1);
                                if (toDoPreView instanceof LinearLayout && (int) (toDoPreView.getTag()) == NORMAL_EDITTEXT) {
                                    CustomEditText preEdit = (CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW);
                                    String preStr = preEdit.getText().toString();
                                    String text = "";
                                    if (toDoSufView instanceof LinearLayout && (int) (toDoSufView.getTag()) == NORMAL_EDITTEXT) {
                                        String sufStr = ((CustomEditText) ((LinearLayout) toDoSufView).getChildAt(SECOND_VIEW)).getText().toString();
                                        if (toDoStr.length() != 0 && sufStr.length() != 0) {
                                            if (preStr.length() != 0) {
                                                text = preStr + "\n" + toDoStr + "\n" + sufStr;
                                            } else {
                                                text = toDoStr + "\n" + sufStr;
                                            }
                                            textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length()) - sufStr.codePointCount(0, sufStr.length());
                                        } else if (toDoStr.length() == 0 && sufStr.length() != 0) {
                                            if (preStr.length() != 0) {
                                                text = preStr + "\n" + sufStr;
                                            } else {
                                                text = sufStr;
                                            }
                                            textCounts = textCounts - sufStr.codePointCount(0, sufStr.length());
                                        } else if (toDoStr.length() != 0 && sufStr.length() == 0) {
                                            if (preStr.length() != 0) {
                                                text = preStr + "\n" + toDoStr;
                                            } else {
                                                text = toDoStr;
                                            }
                                            textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length());

                                        } else {
                                            text = preStr;
                                        }
                                        mAllLayout.removeView(toDoLayout);
                                        mAllLayout.removeView(toDoSufView);
                                    } else {
                                        if (toDoStr.length() != 0) {
                                            if (preStr.length() != 0) {
                                                text = preStr + "\n" + toDoStr;
                                            } else {
                                                text = toDoStr;
                                            }
                                            textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length());
                                        } else {
                                            text = preStr;
                                        }
                                        mAllLayout.removeView(toDoLayout);
                                    }
                                    preEdit.setText(text, false);
                                    preEdit.requestFocus();
                                    preEdit.setSelection(preEdit.getText().toString().length(), preEdit.getText().toString().length());
                                } else {
                                    if (toDoSufView instanceof LinearLayout && (int) (toDoSufView.getTag()) == NORMAL_EDITTEXT) {
                                        mAllLayout.removeView(toDoSufView);
                                        CustomEditText sufEdit = (CustomEditText) ((LinearLayout) toDoSufView).getChildAt(SECOND_VIEW);
                                        String sufStr = sufEdit.getText().toString();
                                        toDoLayout.getChildAt(FIRST_VIEW).setVisibility(GONE);
                                        toDoLayout.setTag(NORMAL_EDITTEXT);
                                        edit.setUsedTodo(false);
                                        edit.setPaintFlags(flags);
                                        logi(TAG, "Text  todoSufPrevie  === "+toDoStr + "\n" + sufStr +" ::: todostr = "+toDoStr+ ": sufStr == "+sufStr );
                                        textCounts = textCounts - sufStr.codePointCount(0, sufStr.length());
                                        edit.setText(toDoStr + "\n" + sufStr, false);
                                        edit.requestFocus();
                                    } else {
                                        if ((int) toDoLayout.getTag() == TODO_EDITTEXT) {
                                            toDoLayout.getChildAt(FIRST_VIEW).setVisibility(GONE);
                                            toDoLayout.setTag(NORMAL_EDITTEXT);
                                            edt.setPaintFlags(flags);
                                            edt.setUsedTodo(false);
                                            edt.requestFocus();
                                        } else {
                                            if (toDoPreView instanceof LinearLayout && (int) (toDoPreView.getTag()) == TODO_EDITTEXT) {
                                                ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).requestFocus();
                                                ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).setSelection(
                                                        ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).getText().toString().length(),
                                                        ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).getText().toString().length());
                                                if (edt.getText().length() == 0) {
                                                    mAllLayout.removeView(toDoLayout);
                                                }
                                            } else if (toDoPreView instanceof  RelativeLayout) {
                                                int index = mAllLayout.indexOfChild(toDoPreView);
                                                mAllLayout.removeView(toDoPreView);
                                                --picCounts;
                                                if (index > 1) {
                                                    View preView = mAllLayout.getChildAt(index - 1);
                                                    if (preView instanceof LinearLayout && (int) ((LinearLayout) preView).getTag() == NORMAL_EDITTEXT) {
                                                        String preStr = ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).getText().toString();
                                                        if (toDoStr.length() != 0) {
                                                            textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length());
                                                            ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).setText(preStr + "\n" + toDoStr, false);
                                                        }
                                                        mAllLayout.removeView(toDoLayout);
                                                        ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).requestFocus();
                                                        ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).setSelection(((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).getText().toString().length(),
                                                                ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).getText().toString().length());
                                                    }
                                                }
                                            } else {
                                                if (edt.getText().length() == 0) {
                                                    mAllLayout.removeView(toDoLayout);
                                                }
                                            }

                                        }
                                    }
                                }

                            } else {
                                if (toDoPreView instanceof LinearLayout && (int) (toDoPreView.getTag()) == NORMAL_EDITTEXT) {
                                    CustomEditText pre = (CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW);//((CustomEditText)(toDoPreView.getChildAt(1)));
                                    String preStr = pre.getText().toString();
                                    textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length());
                                    pre.setText(preStr + "\n" + toDoStr, false);
                                    pre.requestFocus();
                                    pre.setSelection(pre.getText().toString().length(), pre.getText().toString().length());
                                    logd(TAG, "text=" + preStr + toDoStr);
                                    mAllLayout.removeView(toDoLayout);
                                } else {
                                    if ((int) toDoLayout.getTag() == TODO_EDITTEXT) {
                                        toDoLayout.getChildAt(FIRST_VIEW).setVisibility(GONE);
                                        toDoLayout.setTag(NORMAL_EDITTEXT);
                                        edt.setUsedTodo(false);
                                        edt.setPaintFlags(flags);
                                        mLastFocusEdit = edt;
                                        mLastFocusEdit.requestFocus();
                                    } else {
                                        if (toDoPreView instanceof LinearLayout && (int) (toDoPreView.getTag()) == TODO_EDITTEXT) {
                                            if (edt.getText().length() == 0) {
                                                mAllLayout.removeView(toDoLayout);
                                            }
                                            ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).requestFocus();
                                            ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).setSelection(
                                                    ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).getText().toString().length(),
                                                    ((CustomEditText) ((LinearLayout) toDoPreView).getChildAt(SECOND_VIEW)).getText().toString().length());

                                            //If we want to no delete image use back, comment this code.
                                        } else if (toDoPreView instanceof  RelativeLayout){
                                            int index = mAllLayout.indexOfChild(toDoPreView);
                                            mAllLayout.removeView(toDoPreView);
                                            --picCounts;
                                            if (index > 1) {
                                                View preView = mAllLayout.getChildAt(index - 1);
                                                if (preView instanceof LinearLayout && (int) ((LinearLayout) preView).getTag() == NORMAL_EDITTEXT) {
                                                    String preStr = ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).getText().toString();
                                                    if (toDoStr.length() != 0) {
                                                        textCounts = textCounts - toDoStr.codePointCount(0, toDoStr.length());
                                                        ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).setText(preStr + "\n" + toDoStr, false);
                                                    }
                                                    mAllLayout.removeView(toDoLayout);
                                                    ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).requestFocus();
                                                    ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).setSelection(((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).getText().toString().length(),
                                                            ((CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW)).getText().toString().length());
                                                }
                                            }

                                        }

                                    }

                                }
                            }
                        }
                    } else {
                        edit.setPasterAvail(false);
                    }
                }
                return false;
            }
        });
        edt.setBackgroundColor(Color.TRANSPARENT);
        logi(TAG, "add todo ......hint = " + hint);
        edt.setText(hint);
        edt.setTextSize(16);
        edt.setOnFocusChangeListener(mFocusListener);
        if (isEditType) {
           // edt.requestFocus();
            mLastFocusEdit = edt;
            mLastFocusEdit.requestFocus();

            /*UNISOC: Modified for bug 1010507 @{ */
            edt.setSelection(0, 0);
            /* }@ */
        }
        //edt.setTextColor(Color.BLACK);
        if (isTodo) {
            toDoLayout.getChildAt(0).setVisibility(VISIBLE);
            if (!isChecked) {
                checkbox_on.setVisibility(View.VISIBLE);
                checkbox_off.setVisibility(View.GONE);
                edt.setPaintFlags(flags);
            } else {
                checkbox_on.setVisibility(View.GONE);
                checkbox_off.setVisibility(View.VISIBLE);
                edt.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | flags);
            }
        } else {
            toDoLayout.getChildAt(0).setVisibility(GONE);
        }

        checkbox_on.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                logi(TAG, "checkbox_on ...onCLick.");
                checkbox_on.setVisibility(View.GONE);
                checkbox_off.setVisibility(View.VISIBLE);
                edt.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | flags);
            }
        });
        checkbox_off.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                logi(TAG, "checkbox_off ...onCLick.");
                checkbox_on.setVisibility(View.VISIBLE);
                checkbox_off.setVisibility(View.GONE);
                edt.setPaintFlags(flags);
            }
        });


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        edt.setLayoutParams(lp);
        View tempView = mAllLayout.getChildAt(index - 1);
        if (tempView instanceof LinearLayout) {
            toDoLayout.setPadding(dip2px(16), dip2px(4), dip2px(HORIZONTAL_PADDING), 0);
        } else {
            toDoLayout.setPadding(dip2px(16), dip2px(8), dip2px(HORIZONTAL_PADDING), 0);
        }

        mAllLayout.addView(toDoLayout, index);
        return toDoLayout;
    }

    /**
     * Produce todo-list view.
     */
    private LinearLayout createTodoLayout() {
        LinearLayout layout = (LinearLayout) mInflater.inflate(R.layout.edit_todo, null);
        return layout;
    }

    /**
     *Used to display image that was inserted.
     * @param index
     * @param imagePath
     * @param bmp
     */
    private void addImageViewAtIndex(final int index,
                                     final String imagePath, final Bitmap bmp) {
        if (bmp == null) {
            logi(TAG, "addImageViewAtIndex... bmp is null");
        }
        logd(TAG, "addImageViewAtIndex ==== "+picCounts);
        ++picCounts;

        final RelativeLayout imageLayout = createImageLayout();
        final DataImageView imageView = (DataImageView) imageLayout
                .findViewById(R.id.edit_imageView);
        final ImageView closeImage = (ImageView) imageLayout.findViewById(R.id.bg);
        imageLayout.setBackgroundColor(Color.TRANSPARENT);

        imageView.setImageBitmap(bmp);
        imageView.setBitmap(bmp);
        imageView.setAbsolutePath(imagePath);
        final String imageName = imagePath.substring(imagePath.lastIndexOf("/") + 1, imagePath.length());
        logd(TAG, "setcontent--imageName == " + imageName+ "; data = "+getDataFolderPath(mContext)+"bmp height = "+bmp.getHeight()+"bmp = width ="+bmp.getWidth());
        if (!imagePath.contains(getDataFolderPath(mContext))) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    String time = dateFormat.format(new Date());
                    File dir = new File(getDataFolderPath(mContext));
                    if (!dir.exists() || !dir.isDirectory()) {
                        //UNISOC: Modify fro bug 1235479
                        if (!dir.mkdir()) {
                            loge(TAG, dir + " mkdir is failed");
                            return;
                        }
                    }
                    String path = getDataFolderPath(mContext) +time+".jpg";
                    File f = new File(path);
                    ByteArrayOutputStream baos = null;
                    FileOutputStream fOut = null;
                    try {
                        baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] bts = baos.toByteArray();
                        Bitmap res = createBitmap(bts, 640, (bmp.getHeight() / bmp.getWidth()) * 640);
                        fOut = new FileOutputStream(f);

                        res.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                        baos.flush();
                        fOut.flush();
                        imageView.setAbsolutePath(path);
                        logd(TAG, "image path=" + imageView.getAbsolutePath() + ": imagepath = " + imagePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {

                        try {
                            /*UNISOC: Modified for bug 1199503 @{*/
                            if (baos != null){
                                baos.close();
                            }
                            if (fOut != null) {
                                fOut.close();
                            }
                            /*}@*/
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }).start();
        }
        logi(TAG, "bmp.getHeight() = " + bmp.getHeight() + ": bmp.getWidth = " + bmp.getWidth());

        Resources r = mContext.getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        int width = dm.widthPixels;
        int imageHeight = width!= 0 ? (width * bmp.getHeight() / bmp.getWidth()): bmp.getHeight();
        logi(TAG, "imageHeight =" + imageHeight + ":: width = " + width);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (imageHeight * 0.9));
        // ViewGroup.LayoutParams.MATCH_PARENT, imageHeight);
        RelativeLayout.LayoutParams lpl = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (imageHeight * 0.9));// + HORIZONTAL_PADDING);
        lp.setMargins(0, HORIZONTAL_PADDING, 0, 6);
        lpl.setMargins(0, HORIZONTAL_PADDING, 0, 6);

        closeImage.setLayoutParams(lpl);
        closeImage.setPadding(dip2px(HORIZONTAL_PADDING), 0, dip2px(HORIZONTAL_PADDING), 0);
        imageView.setLayoutParams(lp);
        mImages.add(imageView);
        imageLayout.setPadding(dip2px(HORIZONTAL_PADDING), 0, dip2px(HORIZONTAL_PADDING), 0);
        mAllLayout.addView(imageLayout, index);
    }

    private void mergeEditText() {
        View preView = mAllLayout.getChildAt(disappearingImageIndex - 1);
        View nextView = mAllLayout.getChildAt(disappearingImageIndex);
        if (preView != null && preView instanceof LinearLayout && (int) preView.getTag() == NORMAL_EDITTEXT && null != nextView
                && nextView instanceof LinearLayout && (int) nextView.getTag() == NORMAL_EDITTEXT) {
            CustomEditText preEdit = (CustomEditText) ((LinearLayout) preView).getChildAt(SECOND_VIEW);
            CustomEditText nextEdit = (CustomEditText) ((LinearLayout) nextView).getChildAt(SECOND_VIEW);
            String str1 = preEdit.getText().toString();
            String str2 = nextEdit.getText().toString();
            String mergeText = "";
            if (str2.length() > 0) {
                mergeText = str1 + "\n" + str2;
                textCounts = textCounts - str2.codePointCount(0, str2.length());
            } else {
                mergeText = str1;
            }
            mAllLayout.setLayoutTransition(null);
            mAllLayout.removeView(nextView);
            preEdit.setText(mergeText);
            preEdit.requestFocus();
            preEdit.setSelection(preEdit.length(), preEdit.length());
          //  mAllLayout.setLayoutTransition(mTransitioner);
        }
    }

    /**
     * @param dipValue
     */
    public int dip2px(float dipValue) {
        float m = getContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * m + 0.5f);
    }

    /**
     *
     * @param resid
     */
    //Change backgroud for child view.
    @Override
    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
        backgroundId = resid;
        if (backgroundId != -1) {
            for (int i = 0; i < mAllLayout.getChildCount(); i++) {
                mAllLayout.getChildAt(i).setBackgroundResource(backgroundId);
            }
        }
    }

    public int getRichTextHeight() {
        return richTextHeight;
    }

    /**
     * It's Used to get tmp content while capture picture.
     *
     * @return
     */
    public String getContent(String defaultStr) {
        /* UNISOC: Modifyfor bug 1235490 @{ */
        StringBuffer content = new StringBuffer();
        richTextHeight = 0;
        for (int i = 0; i < mAllLayout.getChildCount(); i++) {
            View v = mAllLayout.getChildAt(i);
            richTextHeight += v.getHeight();
            if (v instanceof LinearLayout && (int) v.getTag() == NORMAL_EDITTEXT) {
                content.append(((CustomEditText) ((LinearLayout) v).getChildAt(SECOND_VIEW)).getText().toString());
            } else if (v instanceof RelativeLayout) {
                int count = ((RelativeLayout) v).getChildCount();
                if (count > 0) {
                    for (int j = 0; j < count; j++) {
                        if (((RelativeLayout) v).getChildAt(j) instanceof DataImageView) {
                            content.append("<img>" + ((DataImageView) ((RelativeLayout) v).getChildAt(j)).getAbsolutePath() + "</img>");
                        }
                    }
                }
            } else if (v instanceof LinearLayout && (int) v.getTag() == TODO_EDITTEXT) {
                String todoContent = "\n" + defaultStr + ((CustomEditText) ((LinearLayout) v).getChildAt(SECOND_VIEW)).getText().toString();
                content.append(todoContent);
            }
        }
        logd(TAG, "getContent: tmpContent=====" + content + ": height = " + richTextHeight);
        return content.toString();
        /* }@ */
    }

    public CustomEditText getLastFocusEdit() {
        return mLastFocusEdit;
    }

    public int getPicCount() {
        return picCounts;
    }

    public void setDisplayTime(String time) {
        if (time != null && time.length() != 0) {
            if (mDateTextView != null) {
                mDateTextView.setText(time);
            }
        }
    }

    private String getDataFolderPath(Context paramContext) {
       // return paramContext.getApplicationContext().getFilesDir().toString();
        return Environment.getDataDirectory() + "/user/"
                + paramContext.getUserId() + "/"
                + paramContext.getPackageName() + "/files/";
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int rqsW, int rqsH) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (rqsW == 0 || rqsH == 0) return 1;
        if (height > rqsH || width > rqsW) {
            final int heightRatio = Math.round((float) height/ (float) rqsH);
            final int widthRatio = Math.round((float) width / (float) rqsW);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public Bitmap createBitmap(byte[] bts, int requestWith, int requestHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bts, 0, bts.length, options);
        options.inSampleSize = calculateInSampleSize(options, requestWith, requestHeight);
        options.inJustDecodeBounds = false;
        logi(TAG, "options.inSampleSize == "+options.inSampleSize);
        return BitmapFactory.decodeByteArray(bts, 0, bts.length, options);
    }

    private Bitmap getScaledBitmap(String filePath, int width) {
        logd(TAG, "the filepath =  " + filePath + ": width ==" + width);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        /*UNISOC: Modified for bug 1161932 @{ */
        int sampleSize = 0;
        if (width == 0) {
            Resources r = mContext.getResources();
            DisplayMetrics dm = r.getDisplayMetrics();
            width = dm.widthPixels;
        }
        logi(TAG, "getScaleBitmap...width = "+width+" : options width = "+options.outWidth);

        sampleSize = options.outWidth > width && width > 0? options.outWidth / width + 1 : 1;
        /* @} */
        logi(TAG, "sampeSize =="+sampleSize);
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private void logd(String tag, String msg) {
        if (IS_DEBUG) {
            Log.d(tag, msg);
        }
    }

    private void loge(String tag, String msg) {
        Log.e(tag, msg);
    }

    private void logi(String tag, String msg) {
        Log.i(tag, msg);
    }
}
