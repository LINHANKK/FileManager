package com.linhan.FileManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.linhan.FileManager.Adapter.FileAdapter;
import com.linhan.FileManager.Bean.FileInfo;
import com.linhan.FileManager.Utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener,FileAdapter.OnclickInterfaceFile{

    private boolean isCut = false;
    public static final int T_DIR = 0;// 文件夹
    public static final int T_FILE = 1;// 文件
    public static final int SORT_NAME = 0;//按名称排序
    public static final int SORT_DATE = 1;//按日期排序
    public static final int SORT_SIZE = 2;//按大小排序
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 99;
    private static int currSort = SORT_DATE;//当前排序
    private int asc = 1;// 可以帮助在正序和倒序之间进行切换
    public String name = "";
    private final String ROOT = Environment.getExternalStorageDirectory().getAbsolutePath(); //根目录
    private final String[] sorts = new String[]{"名称", "日期", "大小"};
    public List<FileInfo> list;
    public ArrayList<FileInfo> allList = new ArrayList<FileInfo>();
    HashMap<Integer, String> copyMap = new HashMap<Integer, String>();

    public ListView lv;
    public ListView listView;
    public LinearLayout layout;
    public RelativeLayout relativeLayout;
    public EditText text;
    public ImageView img;
    public ImageView img1;
    public ImageView iv_asc;
    public TextView tv_path;
    public TextView sort;
    public TextView count;
    public TextView size;
    public View view;
    public String currPath; //当前目录
    public String parentPath; //上级目录

    public MenuItem search;
    private SearchView sv;
    private static Comparator<FileInfo> comparator;   //当前所使用的比较器

    public FileAdapter mAdapter;
    private FileUtils fileUtils = new FileUtils();
//    public ProgressDialog pd;

    // 日期比较器
    Comparator<FileInfo> dateComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo o1, FileInfo o2) {
            return o2.getLastModify() > o1.getLastModify() ? -1 * asc : (o2.getLastModify() == o1.getLastModify() ? 0 : 1 * asc);
        }
    };
    // 大小比较器
    Comparator<FileInfo> sizeComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo o1, FileInfo o2) {
            return o2.bytesize > o1.bytesize ? -1 * asc : (o2.bytesize == o1.bytesize ? 0 : 1 * asc);
        }
    };
    // 应用名比较器,为了适应汉字的比较
    Comparator<FileInfo> nameComparator = new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo o1, FileInfo o2) {
            Collator c = Collator.getInstance(Locale.CHINA);
            return asc == 1 ? c.compare(o1.getName(),o2.getName()) : c.compare(o2.getName(),o1.getName());
        }
    };

   Handler handler = new Handler() {
       @Override
       public void handleMessage(@NonNull Message msg) {
           if (msg.what == 1) {
               //pd.dismiss();
               mAdapter.notifyDataSetChanged();
               Toast.makeText(MainActivity.this,(CharSequence)("文件数:" + list.size()), Toast.LENGTH_LONG).show();
               update_sort();
           }
       }
   };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        initView();
        updateData(getStoragePath(this, false) + "/amap1");
        FileUtils.KEY = "";

        //初始化控件
        lv = findViewById(R.id.list);
        mAdapter = new FileAdapter(this);
        mAdapter.setList(list);
        mAdapter.setOnclickInterfaceFile(this);
        lv.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        sort = findViewById(R.id.sort);
        count = findViewById(R.id.count);
        size = findViewById(R.id.size);
        iv_asc = findViewById(R.id.iv_asc);
        layout = findViewById(R.id.pathclick);
        img = findViewById(R.id.imgpath);
        relativeLayout = findViewById(R.id.bottom);

        layout.setEnabled(false);
        updateData();
    }

    private void initView(){
        tv_path = findViewById(R.id.path);
        listView = findViewById(R.id.list);
        mAdapter = new FileAdapter(this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileInfo item = (FileInfo) parent.getItemAtPosition(position);
                File file = new File(item.getPath());
                // 判断文件/文件夹
                if (item.getType() == T_DIR) {
                    updateData(item.getPath());// 进入文件夹
                } else {    // 是文件: 打开
                    fileUtils.openFile(MainActivity.this, file);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_main, menu);
        search = menu.findItem(R.id.search);
        SearchView actionView = (SearchView) search.getActionView();
        if (actionView != null) {
            actionView.setIconifiedByDefault(false);//图标显示在外侧
            actionView.setSubmitButtonEnabled(true);//让提交按钮可用
            actionView.setQueryHint("请输入搜索的文件");//提示用户信息
            actionView.setOnQueryTextListener((SearchView.OnQueryTextListener)this);//关联提交事件
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort_name) {
            currSort = SORT_NAME;
        }else if (id == R.id.sort_date) {
            currSort = SORT_DATE;
        }else if (id == R.id.sort_size) {
            currSort = SORT_SIZE;
        }else if (id == R.id.c1) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            new AlertDialog.Builder(this)
            .setTitle("系统提示")
            .setView(view)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    text = view.findViewById(R.id.name1);
                    name = text.getText().toString();
                    File destDir = new File(currPath + "/" + name);
                    if (!destDir.exists()) {
                        destDir.mkdirs();
                    }
                    updateData(currPath);
                    mAdapter.notifyDataSetChanged();
                }
            }).setNegativeButton("取消", null).create().show();
        } else if (id == R.id.y1) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.txt);
            new AlertDialog.Builder(this)
            .setTitle("系统提示")
            .setView(view)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    text = view.findViewById(R.id.name1);
                    name = text.getText().toString();
                    File destDir = new File(currPath + "/" + name + ".txt");
                    if (!destDir.exists()) {
                        try {
                            destDir.createNewFile();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    updateData1(currPath);
                }
            }).setNegativeButton("取消", null).create().show();
        }else if (id == R.id.y2) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.xml);
            new AlertDialog.Builder(this)
                .setTitle("系统提示")
                .setView(view)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                text = view.findViewById(R.id.name1);
                name = text.getText().toString();
                File destDir = new File(currPath + "/" + name + ".xml");
                if (!destDir.exists()) {
                    try {
                        destDir.createNewFile();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                updateData1(currPath);
                }
            }).setNegativeButton("取消", null).create().show();
        }else if (id == R.id.y3) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.doc);
            new AlertDialog.Builder(this)
                .setTitle("系统提示")
                .setView(view)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        text = view.findViewById(R.id.name1);
                        name = text.getText().toString();
                        File destDir = new File(currPath + "/" + name + ".doc");
                        if (!destDir.exists()) {
                            try {
                                destDir.createNewFile();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                        updateData1(currPath);
                    }
                }).setNegativeButton("取消", null).create().show();
        }else if (id == R.id.y4) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            img1 = view.findViewById(R.id.icon1);
            img1.setImageResource(R.drawable.xls);
            new AlertDialog.Builder(this)
                .setTitle("系统提示")
                .setView(view)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        text = view.findViewById(R.id.name1);
                        name = text.getText().toString();
                        File destDir = new File(currPath + "/" + name + ".xls");
                        if (!destDir.exists()) {
                            try {
                                destDir.createNewFile();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                        updateData1(currPath);
                    }
                }).setNegativeButton("取消", null).create().show();
        }
        update_sort(); // 调用统一的排序方法
        asc *= -1;     //负数,正数
        return super.onOptionsItemSelected(item);
    }

    private String getStoragePath(Context mContext, boolean is_removale) {
        String targetPath = "";
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                Boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    targetPath = path;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return targetPath;
    }

    private void updateData(String path) {
        currPath = path; //记录当前的目录
        File file = new File(path);
        parentPath = file.getParent();  // 更新了上级目录
        mAdapter.getSelectMap().clear();
        list = fileUtils.getListData(path); //获取数据

        Log.e("listData",list.toString());
        list = fileUtils.getGroupList(list); //二次排序
        mAdapter.setList(list);
        mAdapter.notifyDataSetChanged(); //刷新视图
        tv_path.setText(submitPath(path));
    }

    private void updateData1(String path) {
        new Thread(){
            @Override
            public void run() {
                list = fileUtils.getListData(getStoragePath(MainActivity.this, false));
                allList.clear(); //清空
                allList.addAll(list);   //复制集合
                list = fileUtils.getGroupList(list); //二次排序
                mAdapter.setList(list);

                //给主线程发消息
                Message message = handler.obtainMessage();
                message.what = 1;
                handler.sendMessage(message);
            }
        }.start();
        //showProgressDialog();//显示进度框
    }

    private void updateData() {
        new Thread(){
            @Override
            public void run() {
                list = fileUtils.getListData(getStoragePath(MainActivity.this, false));
                allList.clear(); //清空
                allList.addAll(list);   //复制集合
                list = fileUtils.getGroupList(list); //二次排序
                mAdapter.setList(list);

                //给主线程发消息
                Message message = handler.obtainMessage();
                message.what = 1;
                handler.sendMessage(message);
            }
        }.start();
        //showProgressDialog();
    }

    private void update_sort() {
        // 选择不同的比较器
        if (currSort == SORT_NAME) {
            comparator = nameComparator;
        }
        if (currSort == SORT_DATE) {
            comparator = dateComparator;
        }
        if (currSort == SORT_SIZE) {
            comparator = sizeComparator;
        }
        Collections.sort(list, comparator); // 这里才是排序的操作
        list = fileUtils.getGroupList(list);//  2次排序
        mAdapter.setList(list);
        mAdapter.notifyDataSetChanged();    //刷新视图
        update_infobar();
    }

    // 更新顶部信息栏中内容
    private void update_infobar() {
        if (asc == 1) {
            iv_asc.setImageResource(R.drawable.top_icon);
        }else {
            iv_asc.setImageResource(R.drawable.down_icon);
        }
        sort.setText((CharSequence)("排序: " + sorts[currSort]));
        count.setText((CharSequence)("文件数: " + list.size()));
        size.setText((CharSequence)("大小: " + getListSize()));
    }

    // 显示一个环形进度框
//    public void showProgressDialog() {
//        pd = new ProgressDialog(this);
//        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        pd.setTitle("系统信息");
//        pd.setMessage("正在加载文件列表,请耐心等待...");
//        pd.show();
//    }

    public String submitPath(String path) {
        String str;
        if (path.equals(ROOT)) {
            str = "/";
        }else {
            str = currPath.substring(ROOT.length());
        }
        return str;
    }

    private String getListSize() {
        Long sum = 0L;
        FileInfo app;
        for (Iterator<FileInfo> iterator = list.iterator(); iterator.hasNext(); sum += app.bytesize) {
            app = iterator.next();
        }

        return fileUtils.getSize((float)sum);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        FileUtils.KEY = query.trim();
        list = fileUtils.getSearchResult(allList, query);//根据关键字生成结果
        update_sort();//重新排序
        return true;
    }

    public void clickImg(View v) {
        update_sort();
        asc *= 1;//切换正负倒序
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        FileUtils.KEY = newText.trim();
        list = fileUtils.getSearchResult(allList, newText.trim());
        update_sort();
        return true;
    }

    private final void checkPermission() {
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void itemClick(int position) {
        //点击多选的实现
        if(mAdapter.getSelectMap().containsKey(position)) {
            //删除key
            mAdapter.getSelectMap().remove(position);
            if (mAdapter.getSelectMap().size() == 0) {
                relativeLayout.setVisibility(View.GONE);
            }else {
                relativeLayout.setVisibility(View.VISIBLE);
            }
        }else {
            relativeLayout.setVisibility(View.VISIBLE);
            mAdapter.getSelectMap().put(position, position);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void getExit() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.stat_sys_warning)
                .setMessage("确定退出吗")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setNegativeButton("取消", null).show();
    }

    @Override
    public void onBackPressed() {
        // 点击"回退"键
        if (currPath.equals(ROOT)) {
            //退出app
            getExit();
        }else {
            updateData(parentPath);
        }
    }

    //复制
    public void cope(View view) {
        if (mAdapter.getSelectMap().size() == 0) {
            Toast.makeText(this, "您还没选中任何项目!", Toast.LENGTH_SHORT).show();
        }else {
            //把用户信息保存到一个合理的数据结构中
            copyMap.clear();
            Iterator<Integer> iterator = mAdapter.getSelectMap().keySet().iterator();
            while (iterator.hasNext()) {
                int position = iterator.next();
                String path = list.get(position).path;
                copyMap.put(position, path);
            }
            Toast.makeText(this, copyMap.size() + "个项目已保存", Toast.LENGTH_SHORT).show();
            //切换粘贴为激活状态
            layout.setEnabled(true);
            img.setImageResource(R.drawable.ic_menu_paste_holo_light);
        }
    }

    //删除
    public void delete(View view) {
        if (mAdapter.getSelectMap().size() == 0) {
            Toast.makeText(this, "您还没选中任何项目！",Toast.LENGTH_SHORT).show();
        }else {
            new AlertDialog.Builder(this)
                    .setTitle("系统提示")
                    .setMessage("您是否要删除这" + mAdapter.getSelectMap().size() + "个项目")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Iterator<Integer> iterator = mAdapter.getSelectMap().keySet().iterator();
                            while(iterator.hasNext()) {
                                int position = iterator.next();
                                String path = list.get(position).path;
                                File file = new File(path);
                                if (file.isFile()) {
                                    fileUtils.deleteFile(path);
                                }
                                if (file.isDirectory()) {
                                    fileUtils.deleteDir(path);
                                }
                            }
                            mAdapter.getSelectMap().clear();
                            updateData(currPath);
                        }
                    }).setNegativeButton("取消", null).create().show();
        }
    }

    //粘贴
    public void path(View view) {
        if (copyMap.size() > 0) {
            Iterator<String> iterator = copyMap.values().iterator();
            while (iterator.hasNext()) {
                String path = (String)iterator.next();
                File file = new File(path);
                if (file.isFile()) {
                    int res = fileUtils.pasteFile(currPath, new File(path));
                    if (res == 1) {
                        Toast.makeText(this, "该文件已存在", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(this, path + "文件复制成功", Toast.LENGTH_SHORT).show();
                    }
                }
                //如果是剪切那么这里必须要删除文件夹和文件，之前剪切的如果是复制那就不进行处理了
                if (isCut) {
                    deletePath(view);
                    isCut = false;
                }
                if (file.isDirectory()) {
                    fileUtils.pasteDir(currPath, new File(path));
                }
            }

            copyMap.clear();
            updateData(currPath);
            layout.setEnabled(false);
            img.setImageResource(R.drawable.ic_menu_paste_holo_dark);
        }else {
            Toast.makeText(this, "没有可粘帖的项目", Toast.LENGTH_SHORT).show();
        }
    }

    //全选操作
    public void selectAll(View view) {
        mAdapter.getSelectMap().clear();
        for (int i = 0; i < list.size(); i++) {
            mAdapter.getSelectMap().put(i,i);
        }
        mAdapter.notifyDataSetChanged();
    }

    //全不选操作
    public void selectNone(View view) {
        mAdapter.getSelectMap().clear();
        mAdapter.notifyDataSetChanged();
    }

    //剪切
    public void pathDelete(View view) {
        if (mAdapter.getSelectMap().size() > 0) {
            copyMap.clear();
            Iterator<Integer> iterator = mAdapter.getSelectMap().keySet().iterator();
            while (iterator.hasNext()) {
                int position = iterator.next();
                String path = list.get(position).path;
                copyMap.put(position, path);
            }
            Iterator<String> iterator1 = copyMap.values().iterator();
            while (iterator1.hasNext()) {
                String path = (String)iterator1.next();
                File file = new File(path);
                if (file.isFile()) {
                    int res = fileUtils.pasteFile(currPath, new File(path));
                    isCut = true;
                    Toast.makeText(this, path + "文件剪切成功", Toast.LENGTH_SHORT).show();
                    //切换粘贴为激活状态
                    layout.setEnabled(true);
                    img.setImageResource(R.drawable.ic_menu_paste_holo_light);
                    Toast.makeText(this, path + "文件剪切成功", Toast.LENGTH_SHORT).show();
                }
                if (file.isDirectory()) {
                    fileUtils.pasteDir(currPath, new File(path));
                }
            }
        }else {
            Toast.makeText(this, "没有可粘帖的项目", Toast.LENGTH_SHORT).show();
        }
    }

    public void deletePath(View view) {
        Iterator<String> iterator = copyMap.values().iterator();
        while (iterator.hasNext()) {
            String path = (String)iterator.next();
            File file = new File(path);
            if (file.isFile()) {
               fileUtils.deleteFile(path);
            }
            if (file.isDirectory()) {
                fileUtils.deleteDir(path);
            }
        }
        mAdapter.getSelectMap().clear();
        updateData(currPath);
    }
}