package com.sprd.sprdnote.folder;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.sprd.sprdnote.R;

/**
 * Created by qian.dai on 2017/3/20.
 */

public class FolderPreference extends Preference{
    private Context mContext;
    private TextView mTitle;
    private ImageView mIcon;
    private TextView mSize;
    private String mTitleInfo="";
    private int mIconInfo;
    private int mSizeInfo;
    //UNISOC: Add for bug 1225706
    private int mTOrder;
    private int mAOrder;

    public FolderPreference(Context context){
        super(context);
        mContext = context;
    }
    public FolderPreference(Context context, String title){
        super(context);
        mContext = context;
        mTitleInfo = title;
    }
    public FolderPreference(Context context, AttributeSet attrs){
        super(context,attrs);
        mContext = context;
    }
    public FolderPreference(Context context, String title , int resId){
        super(context);
        mContext = context;
        mTitleInfo = title;
        mIconInfo = resId;
    }

    //UNISOC: Modify for bug 1235571
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FolderPreference)){
            return false;
        }
        if (this == obj || getOrder() == ((FolderPreference)obj).getOrder()) {
            return true;
        }
        return false;
    }

    /*UNISOC: Add for bug 1225706 @{*/
    @Override
    public int compareTo(Preference another) {
        mTOrder = getOrder();
        mAOrder = another.getOrder();
        if (mTOrder != mAOrder) {
            // Do order comparison
            return mAOrder - mTOrder;
        }
        return 0;
    }
    /*}*/

    @Override
    protected View onCreateView(ViewGroup parent) {
        Resources res = mContext.getResources();
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.preference_layout, null);
        view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,res.getDimensionPixelSize(R.dimen.rowHeight)));
        return view;
    }

    @Override
    protected void onBindView(View view) {
        mTitle = (TextView) view.findViewById(R.id.title);
        mIcon = (ImageView)view.findViewById(R.id.icon);
        mSize = (TextView)view.findViewById(R.id.size);
        mTitle.setText(mTitleInfo);
        mIcon.setImageResource(mIconInfo);
        mSize.setText(String.valueOf(mSizeInfo));
        super.onBindView(view);
    }

    public void setSize(int number){
        this.mSizeInfo = number;
        notifyChanged();
    }
    public void setTitle(int resId){
        this.mTitleInfo = String.valueOf(resId);
        notifyChanged();
    }
    public CharSequence getTitle(){
        return this.mTitleInfo;
    }
    public void setTitle(CharSequence title){
        this.mTitleInfo = title.toString();
        notifyChanged();
    }
    public void setIcon(int resId){
        this.mIconInfo = resId;
        notifyChanged();
    }
}
