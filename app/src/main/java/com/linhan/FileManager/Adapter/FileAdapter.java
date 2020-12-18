package com.linhan.FileManager.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.linhan.FileManager.Bean.FileInfo;
import com.linhan.FileManager.R;
import com.linhan.FileManager.Utils.FileUtils;

import java.util.HashMap;
import java.util.List;

public class FileAdapter extends BaseAdapter {

    List<FileInfo> list;
    LayoutInflater inflater;
    HashMap<Integer, Integer> selectMap;
    FileAdapter.OnclickInterfaceFile onclickInterfaceFile;

    public FileAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        this.selectMap = new HashMap<Integer, Integer>();
    }

    public void setOnclickInterfaceFile(OnclickInterfaceFile onclickInterfaceFile) {
        this.onclickInterfaceFile = onclickInterfaceFile;
    }

    public void setList(List<FileInfo> list) {
        this.list = list;
    }

    public HashMap<Integer, Integer> getSelectMap() {
        return selectMap;
    }

    @Override
    public int getCount() {
        if (list == null) {
            return 0;
        }else {
            return list.size();
        }
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.item, null);
            holder.icon = convertView.findViewById(R.id.icon);
            holder.imgOnClick = convertView.findViewById(R.id.imgOnClick);
            holder.lineOnClick = convertView.findViewById(R.id.lineOnClick);
            holder.name = convertView.findViewById(R.id.name);
            holder.size = convertView.findViewById(R.id.desc);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FileInfo fileInfo = list.get(position);
        String name1 = fileInfo.getName();
        String key = FileUtils.KEY;
        int i = name1.toLowerCase().indexOf(key.toLowerCase());
        if(i > -1) {
            int end = i + key.length();
            SpannableStringBuilder builder = new SpannableStringBuilder(fileInfo.getName());
            builder.setSpan(new ForegroundColorSpan(Color.BLUE), i, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            holder.name.setText(builder);// 将样式设置给TextView
        }else {
            holder.name.setText(name1);
        }

        holder.icon.setImageResource(fileInfo.getIcon());
        holder.size.setText(fileInfo.getSize() + "" +fileInfo.getTime());
        if (selectMap.containsKey(position)) {
            holder.imgOnClick.setImageResource(R.drawable.blue_selected);
        } else {
            holder.imgOnClick.setImageResource(R.drawable.blue_unselected);
        }
        holder.lineOnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onclickInterfaceFile.itemClick(position);
            }
        });
        return convertView;
    }

    public class ViewHolder {
        ImageView icon;
        TextView name;
        TextView size;
        RelativeLayout lineOnClick;
        ImageView imgOnClick;
    }

    public interface OnclickInterfaceFile {
        void itemClick(int position);
    }
}
