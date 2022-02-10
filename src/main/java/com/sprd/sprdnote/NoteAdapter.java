package com.sprd.sprdnote;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.sprd.notejar.view.data.NoteItem;
import com.sprd.sprdnote.util.DLog;
import com.sprd.notejar.view.util.NoteCategory;
import com.sprd.sprdnote.data.ListItem;
import com.sprd.notejar.view.ListItemView;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by qian.dai on 2017/3/1.
 *
 * Modified by danny.liu on 2017/3/5
 */

public class NoteAdapter extends BaseAdapter{

    public static final int TYPE_ITEM = 0;
    public static final int TYPE_HEADER = 1;
    public static final int TYPE_COUNT = 2;
    public static final int TYPE_SHOW_NORMAL = 0;
    public static final int TYPE_SHOW_CHECK = 1;

    private Context mContext;
    private ArrayList<NoteCategory> mList = null;
    private ArrayList<NoteItem> mItems = null;
    private ArrayList<ListItem> mListItems = new ArrayList<>();
    private LayoutInflater mInflater = null;

    private String mSearchKey;
    private int showType = TYPE_SHOW_NORMAL;

    public NoteAdapter(final Context context, final ArrayList<NoteCategory> list) {
        mContext = context;
        mList = list;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mList != null) {
            parseData(list);
        }
    }

    private void parseData (ArrayList<NoteCategory> list) {
        mListItems.clear();
        int tmpCount = 0;
        DLog.d("NoteAdapter", "list.size() = "+list.size());
        for (int i = 0; i < list.size(); i++) {
            int year = Integer.parseInt(list.get(i).getHeaderTitle());
            DLog.d("NoteAdapter", "year === "+year+ " :i" +i + "current year=  "+Integer.parseInt(NoteCategory.getYear(new Date(System.currentTimeMillis()))));
            if (year == Integer.parseInt(NoteCategory.getYear(new Date(System.currentTimeMillis())))) {
                tmpCount = list.get(i).getItemCounts();
            /*UNISOC: Modify for bug 1235487 @{*/
            } else if (list.get(i).getItems() != null) {
                tmpCount  = list.get(i).getItems().size();
                ListItem mTemp = new ListItem();
                mTemp.setType(ListItem.HEAD);
                mTemp.setYear(year);
                mListItems.add(mTemp);
            }
            for (int m = 0; m < tmpCount; m++) {
                if (list.get(i).getItems() != null) {
                    ListItem noteItem = new ListItem();
                    noteItem.setType(ListItem.NOTE);
                    noteItem.setNoteItem((NoteItem)list.get(i).getItems().get(m));
                    mListItems.add(noteItem);
                }
            /*}@*/
            }
        }
    }

    public void setCategoryList ( ArrayList<NoteCategory> list){
        mList = list;
        if (mList != null) {
            parseData(mList);
        }
    }

    public void setItems(ArrayList<NoteItem> list){
        mItems = list;
    }

    /**
     * It be used to select all or cancel all
     * @param bool mItems are selected  while bool is true else cancel all selected.
     */
    public void setAllItemChecked (boolean bool) {
        if (null != mList && null != mItems && mItems.size() != 0) {
            int count = mItems.size();
            for (int i = 0; i < count; i++) {
                if (bool) {
                    mItems.get(i).setSelected(true);
                } else {
                    mItems.get(i).setSelected(false);
                }
            }
            setCategoryList(NoteCategory.categoryNote(mItems));
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        /* SPRD: modified for bug 898268 @{ */
        return ( null != mList ) ? mListItems.size() : 0;
        /* @} */
    }

    public void setSearchKey(String key) {
        mSearchKey = key;
    }
    /**
     *
     * @param type
     */
    public void setShowType (int type) {
        showType = type;
    }
    @Override
    public Object getItem(int position) {
        /* SPRD: modified for bug 878455 @{ */
        if (null == mList || position < 0 || position >= getCount()) {
        /* @} */
            return null;
        }

        if (mListItems.get(position).getType() == ListItem.HEAD) {
            return mListItems.get(position).getYear();
        } else {
            return mListItems.get(position).getNoteItem();
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType (int position) {
        /* SPRD: modified for bug 878455 @{ */
        if (null == mList || position < 0 || position >= getCount()) {
        /* @} */
            return TYPE_ITEM;
        }

        if (mListItems.get(position).getType() == ListItem.HEAD) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @Override
    public  int getViewTypeCount () {
        return TYPE_COUNT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        ViewHolder holder = null;
        ListItemView mListItem = new ListItemView();

        int itemType = getItemViewType(position);
        DLog.d("NoteAdapter", "position === "+position+ " type  ="+itemType);
        switch (itemType) {
            case TYPE_HEADER:
                HeadHolder headHolder = null;
                if (convertView == null || convertView.getTag() == null) {
                    convertView = mInflater.inflate(R.layout.listview_item_note_header, null);
                    headHolder = new HeadHolder();
                    headHolder.headText = (TextView)convertView.findViewById(R.id.list_header);
                    convertView.setTag(headHolder);
                } else {
                    headHolder = (HeadHolder)convertView.getTag();
                }
                /*UNISOC: Modify for bug 1235487 @{*/
                String header = "";
                if (getItem(position) != null) {
                    header = getItem(position).toString();
                }
                /*}@*/
                DLog.d("NoteAdapter", "Header ============"+header);
                headHolder.headText.setText(header);
                break;
            case TYPE_ITEM:
                if (convertView == null || convertView.getTag() == null) {
                    convertView = mInflater.inflate(R.layout.listview_item_note, null);
                    holder = new ViewHolder(convertView);
                    convertView.setTag(R.integer.listview_tag_1, holder);
                    mListItem.mCheck = (CheckBox)convertView.findViewById(R.id.check);
                    convertView.setTag(R.integer.listview_tag_2, mListItem);
                } else {
                   // view = convertView ;
                    holder = (ViewHolder) convertView.getTag(R.integer.listview_tag_1);//(ViewHolder) convertView.getTag() ;
                }
                NoteItem item = (NoteItem)getItem(position);
                if (item != null ) {
                    long createTime = item.getLongDate();
                    String title = item.getTitle();
                    holder.data.setText(item.isToday()? mContext.getString(R.string.today)+" "+item.getTime(mContext, new Date(createTime)):item.getDisplayTime(mContext, new Date(createTime)));
                    SpannableStringBuilder style;
                    if (title != null && title.length() > 0) {
                        /*Modified for 732233: start*/
                        String tempTitle = title.toLowerCase();
                        if (mSearchKey != null && mSearchKey.length() > 0 && tempTitle.contains(mSearchKey.toLowerCase())) {
                            int startKey = tempTitle.indexOf(mSearchKey.toLowerCase());
                            style = new SpannableStringBuilder(title);
                            /*UNISOC: Modify for 1193510 1240495@{*/
                            if (title.length() == title.toLowerCase().length()) {
                                style.setSpan(new ForegroundColorSpan(mContext.getColor(R.color.actionbar_color_text)), startKey,
                                    startKey + mSearchKey.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                            }
                            /*}@*/
                            /*Modified for 732233: end*/
                            holder.title.setText(style);
                        } else {
                            holder.title.setText(title);
                        }
                    } else {
                        holder.title.setText(mContext.getString(R.string.picture_title));
                    }
                    DLog.d("NoteAdapter", "isCollected = "+item.isCollected()+ "isContainPic = "+item.isContainPic()+ "showType == "+showType
                    +"item.isSelected === "+item.getIsSelected()+": title = "+title+" : content = "+item.getContent());
                    if (showType == TYPE_SHOW_CHECK) {
                        holder.check.setVisibility(View.VISIBLE);
                        if (item.getIsSelected()) {
                            holder.check.setChecked(true);
                        } else {
                            holder.check.setChecked(false);
                        }
                        holder.picture.setVisibility(View.GONE);
                        holder.favorite2.setVisibility(View.GONE);
                        holder.favorite.setVisibility(View.GONE);
                    } else {
                        holder.check.setVisibility(View.GONE);
                        item.setSelected(false);
                        if (item.isContainPic()) {
                            holder.picture.setVisibility(View.VISIBLE);

                            if (item.isCollected()) {
                                holder.favorite.setVisibility(View.VISIBLE);
                                holder.favorite2.setVisibility(View.GONE);
                            } else {
                                holder.favorite.setVisibility(View.GONE);
                                holder.favorite2.setVisibility(View.GONE);
                            }
                        } else if (item.isCollected()) {
                            holder.favorite2.setVisibility(View.VISIBLE);
                            holder.picture.setVisibility(View.GONE);
                            holder.favorite.setVisibility(View.GONE);
                        } else {
                            holder.picture.setVisibility(View.GONE);
                            holder.favorite2.setVisibility(View.GONE);
                            holder.favorite.setVisibility(View.GONE);
                        }
                    }
                }
                break;
        }
        return convertView;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == TYPE_ITEM;
    }

    /*UNISOC: Modify for bug 1235447 @{*/
    private static class ViewHolder {
        TextView data;
        TextView title;
        ImageView picture;
        ImageView favorite;
        ImageView favorite2;
        CheckBox check;

        public ViewHolder(View view) {
            data = (TextView) view.findViewById(R.id.note_time);
            title = (TextView) view.findViewById(R.id.note_title);
            picture = (ImageView) view.findViewById(R.id.picture);
            favorite = (ImageView)view.findViewById(R.id.favorite);
            favorite2 = (ImageView)view.findViewById(R.id.favorite2);
            check = (CheckBox)view.findViewById(R.id.check);
        }
    }

    private static class HeadHolder {
        TextView headText;
    }
    /*}@*/
}
