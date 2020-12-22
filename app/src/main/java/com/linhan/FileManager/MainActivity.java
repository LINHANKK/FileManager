package com.linhan.FileManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.AlertDialog;
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
import com.linhan.FileManager.Utils.USBPath;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener,
        FileAdapter.OnclickInterfaceFile, AdapterView.OnItemClickListener {

    private final String ROOT = Environment.getExternalStorageDirectory().getAbsolutePath(); //根目录
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 99;
    private final String[] sorts = new String[]{"名称", "日期", "大小"};
    private int asc = 1;                                            // 可以帮助在正序和倒序之间进行切换
    private boolean isUDrive = false;                               //是否为U盘
    private boolean isCut = false;                                  //是否剪切
    public static final int T_DIR = 0;                              // 类型：文件夹
    public static final int T_FILE = 1;                             // 类型：文件
    public static final int SORT_NAME = 0;                          //按名称排序
    public static final int SORT_DATE = 1;                          //按日期排序
    public static final int SORT_SIZE = 2;                          //按大小排序
    private static Comparator<FileInfo> comparator;                 //当前所使用的比较器
    private static int currSort = SORT_DATE;                        //当前排序方式
    public String name = "";                                        //命名
    public List<FileInfo> list = new ArrayList<>();                 //存放文件数据
    public ArrayList<FileInfo> allList = new ArrayList<FileInfo>();        //存放临时数据
    HashMap<Integer, String> copyMap = new HashMap<Integer, String>();//复制文件结构

    public ListView lv;                             //显示文件的listView
    public LinearLayout layout;                     //底部栏的粘贴按钮
    public RelativeLayout relativeLayout;           //整个底部栏
    public EditText text;                           //创建文件或重命名时的命名文本
    public ImageView img;                           //底部栏的粘贴按钮的图片底色，当粘贴激活时，改变颜色
    public ImageView img1;                          //创建文件或重命名时的图标
    public ImageView iv_asc;                        //排序图标
    public ImageView iv_usb;                        //usb按钮的图标切换
    public TextView tv_path;                        //当前路径显示
    public TextView sort;                           //排序文本
    public TextView count;                          //显示当前路径下的文件数
    public TextView size;                           //显示当前路径下的所有文件数据大小
    public TextView tv_usb;                         //usb按钮的文字切换
    public View view;                               //绑定创建文件和重命名的layout
    public String currPath;                         //当前路径
    public String parentPath;                       //上级路径
    public MenuItem search;                         //搜素按钮
    public FileAdapter mAdapter;
    private FileUtils fileUtils = new FileUtils();

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
        mAdapter = new FileAdapter(this);
        checkPermission();
        initView();                                                         //初始化视图
        updateData(getStoragePath(this, false));       //获取数据
        FileUtils.KEY = "";
        mAdapter.setOnclickInterfaceFile(this);
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(this);
        layout.setEnabled(false);
        updateData();
    }

    private void initView(){
        tv_path = findViewById(R.id.path);
        lv = findViewById(R.id.list);
        sort = findViewById(R.id.sort);
        count = findViewById(R.id.count);
        size = findViewById(R.id.size);
        iv_asc = findViewById(R.id.iv_asc);
        layout = findViewById(R.id.pathclick);
        img = findViewById(R.id.imgpath);
        iv_usb = findViewById(R.id.usbImage);
        tv_usb = findViewById(R.id.usbText);
        relativeLayout = findViewById(R.id.bottom);
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

    @Override       //菜单选项
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
                    if (name.length() < 1) {
                        Toast.makeText(MainActivity.this, R.string.rename_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File destDir = new File(currPath + "/" + name);
                    if (!destDir.exists()) {
                        destDir.mkdirs();
                    }else {
                        Toast.makeText(MainActivity.this, R.string.exist_file, Toast.LENGTH_SHORT).show();
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
                    if (name.length() < 1) {
                        Toast.makeText(MainActivity.this, R.string.rename_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
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
                if (name.length() < 1) {
                    Toast.makeText(MainActivity.this, R.string.rename_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
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
                        if (name.length() < 1) {
                            Toast.makeText(MainActivity.this, R.string.rename_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }
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
                        if (name.length() < 1) {
                            Toast.makeText(MainActivity.this, R.string.rename_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }
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

    //获取内部存储路径
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

    //实时更新数据
    private void updateData(String path) {
        currPath = path; //记录当前的目录
        File file = new File(path);
        parentPath = file.getParent();  // 更新了上级目录
        mAdapter.getSelectMap().clear();
        list = fileUtils.getListData(path);//获取数据
        list = fileUtils.getGroupList(list); //二次排序
        mAdapter.setList(list);
        mAdapter.notifyDataSetChanged(); //刷新视图
        tv_path.setText(currPath);
        update_infobar();
    }

    //更新查询时的文件数据，新线程
    private void updateData1(String path) {
        new Thread(){
            @Override
            public void run() {
                list = fileUtils.getListData(path);
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
    }

    //启动新线程,处理耗时操作
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
    }

    // 选择不同的比较器
    private void update_sort() {
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

    //获取文件大小
    private String getListSize() {
        Long sum = 0L;
        FileInfo app;
        for (Iterator<FileInfo> iterator = list.iterator(); iterator.hasNext(); sum += app.bytesize) {
            app = iterator.next();
        }
        return fileUtils.getSize((float)sum);
    }

    @Override   //提交关键字
    public boolean onQueryTextSubmit(String query) {
        FileUtils.KEY = query.trim();
        list = fileUtils.getSearchResult(allList, query);//根据关键字生成结果
        update_sort();//重新排序
        return true;
    }

    //切换正负倒序
    public void clickImg(View v) {
        update_sort();
        asc *= 1;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        FileUtils.KEY = newText.trim();
        list = fileUtils.getSearchResult(allList, newText.trim());
        update_sort();
        return true;
    }

    //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
    private final void checkPermission() {
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

    @Override    //复选框的点击事件
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

    //退出时的对话框
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
            //当返回到根目录时，退出app
            getExit();
        }else if (currPath.equals("/storage/700C-92EB")){
            //当返回到U盘根目录时，退出app
            getExit();
        }else if (currPath.equals("/storage")){
            //当返回到内置SD卡根目录时，退出app
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
                String path = list.get(position).getPath();
                copyMap.put(position, path);
            }
            Toast.makeText(this, copyMap.size() + "个项目已保存", Toast.LENGTH_SHORT).show();
            //切换粘贴为激活状态,改变其图标图案
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
                                String path = list.get(position).getPath();
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
                    File hasFile = new File(currPath, file.getName());
                    if (hasFile.exists()) { //判断是否已经存在，存在则不进行粘贴操作
                        Toast.makeText(this, "该文件已存在", Toast.LENGTH_SHORT).show();
                    } else {        //不存在则粘贴并判断是否剪切
                        fileUtils.pasteFile(currPath, new File(path));
                        //如果是剪切那么这里必须要删除文件夹和文件
                        if (isCut) {
                            deletePath(view);
                            isCut = false;
                        }
                        Toast.makeText(this, path + "文件粘贴成功", Toast.LENGTH_SHORT).show();
                    }
                }
                if (file.isDirectory()) { //文件夹不做重复判断
                    fileUtils.pasteDir(currPath, new File(path));
                    if (isCut) {
                        deletePath(view);
                        isCut = false;
                    }
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

    //全选
    public void selectAll(View view) {
        mAdapter.getSelectMap().clear();
        for (int i = 0; i < list.size(); i++) {
            mAdapter.getSelectMap().put(i,i);
        }
        mAdapter.notifyDataSetChanged();
    }

    //取消
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
                    isCut = true;
                    Toast.makeText(this, path + "文件剪切成功", Toast.LENGTH_SHORT).show();
                    //切换粘贴为激活状态
                    layout.setEnabled(true);
                    img.setImageResource(R.drawable.ic_menu_paste_holo_light);
                }
                if (file.isDirectory()) {
                    isCut = true;
                    Toast.makeText(this, path + "文件夹剪切成功", Toast.LENGTH_SHORT).show();
                    //切换粘贴为激活状态
                    layout.setEnabled(true);
                    img.setImageResource(R.drawable.ic_menu_paste_holo_light);
                }
            }
        }else {
            Toast.makeText(this, "没有可剪切的项目", Toast.LENGTH_SHORT).show();
        }
    }

    //剪切删除
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

    //U盘和本地存储切换
    public void toggle(View view) {
        List<String> uPath = USBPath.getUPath(this);
        if (uPath.size() == 0) {
            Toast.makeText(this,"未读取到U盘",Toast.LENGTH_SHORT).show();
            return;
        }
        Log.e("Upath",uPath.toString());
        uPath.remove(0);
        String s1 = null;
        for (String s : uPath) {
            s1 = s;
        }
        Log.d("U盘", s1);
        if (isUDrive == false) {
            iv_usb.setImageResource(R.drawable.local);
            tv_usb.setText("本地");
            updateData(s1);
            isUDrive = true;
        }else {
            iv_usb.setImageResource(R.drawable.usb_drive);
            tv_usb.setText("U盘");
            updateData(getStoragePath(this, false));
            isUDrive = false;
        }
    }

    //重命名,后期需优化为单选
    public void rename(View v) {
        if (mAdapter.getSelectMap().size() == 0) {
            Toast.makeText(this, "您还没选中任何项目！", Toast.LENGTH_SHORT).show();
        }else {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.layout, null);
            new AlertDialog.Builder(this)
                    .setTitle("重命名")
                    .setView(view)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            text = view.findViewById(R.id.name1);
                            name = text.getText().toString();
                            if(name.length() < 1){
                                //如果用户输入为空
                                Toast.makeText(MainActivity.this, R.string.rename_empty, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Iterator<Integer> iterator = mAdapter.getSelectMap().keySet().iterator();
                            while(iterator.hasNext()) {
                                int position = iterator.next();
                                String path = list.get(position).getPath();
                                File file = new File(path);
                                File destDir = new File(currPath + "/" + name);
                                file.renameTo(destDir);
                            }
                            updateData(currPath);
                            mAdapter.notifyDataSetChanged();
                        }
                    }).setNegativeButton("取消", null).create().show();
        }
    }

    @Override   //文件item点击事件
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
}