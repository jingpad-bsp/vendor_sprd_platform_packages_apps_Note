package com.sprd.sprdnote.folder;

import android.content.Context;
import android.content.res.Resources;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;
import android.graphics.Typeface;
import com.sprd.sprdnote.R;
import android.view.Gravity;

/**
 * Created by qian.dai on 2017/3/20.
 */

public class MyPreferenceCategory extends PreferenceCategory{
    private Context mContext;

    public MyPreferenceCategory(Context context){
        super(context);
        mContext = context;
    }
    public MyPreferenceCategory(Context context, AttributeSet attrs){
        super(context,attrs);
        mContext = context;
    }
    /* SPRD: modified for bug1010715 @{
    @Override
    protected View onCreateView(ViewGroup parent) {
        Resources res = mContext.getResources();
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.preference_category_layout, null);
        view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,res.getDimensionPixelSize(R.dimen.categoryHeight)));
        view.setBackgroundColor(res.getColor(R.color.note_year_color));
        TextView tv= (TextView)view.findViewById(R.id.title);
        tv.setText(getTitle());
        return view;
    }
    */

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        /* SPRD: modified for bug1010715 @{*/
        final Resources res = mContext.getResources();
        view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,res.getDimensionPixelSize(R.dimen.categoryHeight)));
        if(view instanceof TextView){
            TextView tv = (TextView)view;
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(tv.getPaddingLeft(), 5, tv.getPaddingLeft(), 5);
            tv.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            tv.setTextColor(mContext.getResources().getColor(R.color.preference_category_color));
            tv.setBackgroundColor(res.getColor(R.color.note_year_color));
        }
        /* @} */
    }
}
