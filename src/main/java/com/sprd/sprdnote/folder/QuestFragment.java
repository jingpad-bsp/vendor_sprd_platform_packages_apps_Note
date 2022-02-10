package com.sprd.sprdnote.folder;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.sprdnote.R;
import com.sprd.sprdnote.SecurityActivity;

/**
 * Created by qian.dai on 2017/3/6.
 */

public class QuestFragment extends Fragment implements TextWatcher, TextView.OnEditorActionListener, MenuItem.OnMenuItemClickListener{
    private Context mContext;
    private MenuItem mDone;
    private MenuItem mClear;
    private EditText mQuest;
    private EditText mAnswer;
    private TextView mErrorAnswer;
    private TextView mSubHeader;
    private TextView mHeader;
    private static final int MENU_DONE  = 0;
    private static final int MENU_CLEAR  = 1;
    private int mStartType;
    private String quest;
    private String answer;

    @SuppressLint({"NewApi", "ValidFragment"})
    public QuestFragment(final Context context) {
        mContext = context;
    }
    public QuestFragment() {
    }

    @SuppressLint({"NewApi", "ValidFragment"})
    public QuestFragment(final Context context, final int type) {
        mContext = context;
        mStartType = type;
    }

    public void saveState() {
        Bundle outState = new Bundle();
        // Save State Here
        outState.putString("quest", mQuest.getText().toString());
        outState.putString("answer", mAnswer.getText().toString());
        outState.putBoolean("done_enable", mDone.isEnabled());
        ((SecurityActivity)getActivity()).saveStateForQuest(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(mStartType == FolderUtil.INPUT_FORGET_PWD) {
            getActivity().setTitle(R.string.find_pwd);
        } else {
            getActivity().setTitle(R.string.set_password);
        }
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        View view = inflater.inflate(R.layout.security_quest, null);
        mQuest = (EditText) view.findViewById(R.id.question);
        //mQuest.requestFocus();
        mQuest.setInputType(InputType.TYPE_CLASS_TEXT);
        if (mStartType != FolderUtil.INPUT_FORGET_PWD) {
        mQuest.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        mQuest.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mDone != null) {
                    if(!mAnswer.getText().toString().trim().isEmpty() && !mQuest.getText().toString().trim().isEmpty()) {
                        mDone.setEnabled(true);
                    } else {
                        mDone.setEnabled(false);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int tmp = s.toString().length();
                if (tmp >= 50 ) {
                    //show toast
                    Toast.makeText(getActivity(), R.string.text_too_long_for_quest, Toast.LENGTH_SHORT).show();
                }

            }
        });
        }
        mAnswer = (EditText) view.findViewById(R.id.answer);
        mAnswer.addTextChangedListener(this);
        mAnswer.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
        mAnswer.setInputType(InputType.TYPE_CLASS_TEXT);
        mAnswer.setOnEditorActionListener(this);
        mErrorAnswer = (TextView)view.findViewById(R.id.error_answer);
        mHeader = (TextView)view.findViewById(R.id.headerText);
        mSubHeader = (TextView)view.findViewById(R.id.subText);
        updateUi();
        return view;
    }

    public void updateUi() {
        SharedPreferences preferences = getActivity().getSharedPreferences("security", Context.MODE_PRIVATE);
        quest = preferences.getString("question", "");
        answer = preferences.getString("answer", "");
        Bundle questState = ((SecurityActivity)getActivity()).getStateForQuest();
        if (mStartType == FolderUtil.INPUT_FORGET_PWD) {
            mHeader.setText(R.string.answer_quest_title);
            mSubHeader.setVisibility(View.GONE);
            mQuest.setText(quest);
            mQuest.setBackground(null);
            mQuest.clearFocus();
            mQuest.setFocusable(false);
            mQuest.setLongClickable(false);
            mAnswer.setHint(R.string.input_answer_hint);
            if ( questState != null) {
                mAnswer.setText(String.valueOf(questState.getString("answer")));
                if(mDone!= null) mDone.setEnabled(questState.getBoolean("done_enable"));
            }
            mAnswer.requestFocus();
        } else {
            if (!quest.isEmpty() && !answer.isEmpty()) {
                mQuest.setText(quest);
                mAnswer.setText(answer);
                if(mDone!= null) mDone.setEnabled(true);
            }

            if ( questState != null) {
                mQuest.setText(String.valueOf(questState.getString("quest")));
                mAnswer.setText(String.valueOf(questState.getString("answer")));
                if(mDone!= null) mDone.setEnabled(questState.getBoolean("done_enable"));
            }
            mQuest.requestFocus();
        }

        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0,MENU_DONE,0,R.string.done).setOnMenuItemClickListener(this).setVisible(true).setEnabled(false);
        menu.add(0,MENU_CLEAR,0,R.string.clear).setOnMenuItemClickListener(this).setVisible(false);
        mDone = menu.findItem(MENU_DONE);

        if(mStartType == FolderUtil.INPUT_FORGET_PWD) {
            mDone.setTitle(android.R.string.ok);
        }
        mClear = menu.findItem(MENU_CLEAR);
        mDone.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mClear.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (mAnswer != null && mQuest != null) {
            if (mAnswer.getText().length() > 0 && mQuest.getText().length() > 0) {
                mDone.setEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DONE:
                handleDone();
                break;
            case MENU_CLEAR:
                handleClear();
                break;
            default:
                break;
        }
        //UNISOC: Modify for bug 1235508
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String str = s.toString().trim();
        if (mDone != null) {
            if (str.length() <= 0) {
                mDone.setEnabled(false);
            } else {
                if(!mQuest.getText().toString().trim().isEmpty())
                mDone.setEnabled(true);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        int tmp = s.toString().length();
        if (tmp >= 14 ) {
            //show toast
            Toast.makeText(getActivity(), R.string.text_too_long, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_NULL) {
            handleDone();
            return true;
        }
        return false;
    }
    private void handleClear(){
        ((SecurityActivity) getActivity()).clear();
    }

    private void handleDone() {
        if (mAnswer.getText().toString().isEmpty() || mQuest.getText().toString().isEmpty()) {
            return;
        }

        if (mStartType == FolderUtil.INPUT_FORGET_PWD) {
            if (mAnswer.getText().toString().trim().equals(answer)) {
                //restart set password
                SharedPreferences preferences = getActivity().getSharedPreferences("security", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove("pwd");
                editor.commit();
                Toast.makeText(getActivity(), R.string.text_find_pwd_success, Toast.LENGTH_SHORT).show();
                ((SecurityActivity) getActivity()).changeFragment(FolderUtil.INPUT_PWD);
            } else {
                mErrorAnswer.setVisibility(View.VISIBLE);
                mAnswer.setText("");
                mClear.setVisible(true);
            }
        } else {
            SharedPreferences preferences = getActivity().getSharedPreferences("security", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("question", mQuest.getText().toString().trim());
            editor.putString("answer", mAnswer.getText().toString().trim());
            editor.commit();
            Toast.makeText(mContext, R.string.set_password_done, Toast.LENGTH_SHORT).show();

            //start private activity
            ((SecurityActivity) getActivity()).changeFragment(FolderUtil.INPUT_DONE);
        }
    }
}
