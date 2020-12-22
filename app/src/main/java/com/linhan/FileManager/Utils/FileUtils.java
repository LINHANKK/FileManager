package com.linhan.FileManager.Utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.linhan.FileManager.Adapter.FileAdapter;
import com.linhan.FileManager.Bean.FileInfo;
import com.linhan.FileManager.MainActivity;
import com.linhan.FileManager.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileUtils {
    public static String KEY = "";
    public static String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private FileAdapter adapter;
    private String pathFile = "";

    /**
     * 获取所有文件数据
     * @param path
     * @return
     */
    public List<FileInfo> getListData(String path) {
        ArrayList<FileInfo> list = new ArrayList<>();
        File pFile = new File(path);
        File[] files;           // 声明了一个文件对象数组
        if (pFile.exists()) {    // 判断路径是否存在
            files = pFile.listFiles();
        }else {
            pFile.mkdir();
            Log.e("name", pFile.getAbsolutePath());
            files = pFile.listFiles();
        }

        Log.e("fileDARTA", files.length+"");
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++){
                File file = files[i];
                FileInfo item = new FileInfo();
                if (!file.isHidden()) {
                    if (file.isDirectory() && file.canRead()) {
                        file.isHidden();
                        item.setIcon(R.drawable.folder);
                        item.setBytesize(file.length());
                        item.setSize(getSize((float)item.bytesize));
                        item.setType(MainActivity.T_DIR);
                    } else if (file.isFile()) {
                        Log.i("spl", file.getName());
                        String ext = getFileEXT(file.getName());
                        Log.i("spl", "ext=" + ext);
                        item.setIcon(getDrawableIcon(ext));
                        item.setSize(getSize((float)file.length()));
                        item.setType(MainActivity.T_FILE);
                    } else {
                        item.setIcon(R.drawable.mul_file);
                    }

                    item.setName(file.getName());
                    item.setLastModify(file.lastModified());
                    item.setPath(file.getPath());
                    Date date = new Date(file.lastModified());
                    item.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date));
                    list.add(item);
                    Log.e("listData:",list.toString());
                }

            }

        }
        files = null;
        return list;
    }


    /**
     * 格式转换应用大小 单位"B,KB,MB,GB"
     */
    public String getSize(float length) {
        long kb = 1024L;
        long mb = (long)1024 * kb;
        long gb = (long)1024 * mb;
        String fileSize;
        if (length < (float)kb) {
            fileSize = String.format("%dB",(int)length);
        } else if (length < (float)mb) {
            fileSize = String.format("%.2fKB", length / kb);
        } else if (length < (float)gb) {
            fileSize = String.format("%.2fMB", length / mb);
        } else {
            fileSize = String.format("%.2fGB", length / gb);
        }

        return fileSize;
    }

    /**
     * 获取查询结果
     * @param list
     * @param keyword
     * @return
     */
    public List<FileInfo> getSearchResult(List<FileInfo> list, String keyword) {
        ArrayList<FileInfo> searchResultList = new ArrayList<FileInfo>();
        for(int i = 0; i < list.size(); i++) {
            FileInfo app = (FileInfo)list.get(i);
            if (app.getName().toLowerCase().contains(keyword.toLowerCase())) {
                searchResultList.add(app);
            }
        }
        return searchResultList;
    }

    /**
     * 截取文件的扩展名
     *
     * @param filename 文件全名
     * @return 扩展名(mp3, txt)
     */
    public String getFileEXT(String filename) {
        if (filename.contains(".")) {
            int dot = filename.lastIndexOf(".");  //例如sss.ksd.txt
            return filename.substring(dot + 1);
        } else {
            return "";
        }
    }

    /**
     * 检查扩展名end 是否在ends数组中
     *
     * @param end
     * @param ends
     * @return
     */
    public boolean checkEndsInArray(String end, String[] ends) {
        for(int i = 0; i < ends.length; i++) {
            String aEnd = ends[i];
            if (end.equals(aEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获得与扩展名对应的图标资源idend
     *
     * @param end 扩展名
     * @return
     */
    public int getDrawableIcon(String end) {

        int id;
        if (end.equals("asf")) {
            id = R.drawable.asf;
        } else if (end.equals("avi")) {
            id = R.drawable.avi;
        } else if (end.equals("bmp")) {
            id = R.drawable.bmp;
        } else if (end.equals("doc")) {
            id = R.drawable.doc;
        } else if (end.equals("gif")) {
            id = R.drawable.gif;
        } else if (end.equals("html")) {
            id = R.drawable.html;
        } else if (end.equals("apk")) {
            id = R.drawable.iapk;
        } else if (end.equals("ico")) {
            id = R.drawable.ico;
        } else if (end.equals("jpg")) {
            id = R.drawable.jpg;
        } else if (end.equals("log")) {
            id = R.drawable.log;
        } else if (end.equals("mov")) {
            id = R.drawable.mov;
        } else if (end.equals("mp3")) {
            id = R.drawable.mp3;
        } else if (end.equals("mp4")) {
            id = R.drawable.mp4;
        } else if (end.equals("mpeg")) {
            id = R.drawable.mpeg;
        } else if (end.equals("pdf")) {
            id = R.drawable.pdf;
        } else if (end.equals("png")) {
            id = R.drawable.png;
        } else if (end.equals("ppt")) {
            id = R.drawable.ppt;
        } else if (end.equals("rar")) {
            id = R.drawable.rar;
        } else if (end.equals("txt") || end.equals("dat") || end.equals("ini")
                || end.equals("java")) {
            id = R.drawable.txt;
        } else if (end.equals("vob")) {
            id = R.drawable.vob;
        } else if (end.equals("wav")) {
            id = R.drawable.wav;
        } else if (end.equals("wma")) {
            id = R.drawable.wma;
        } else if (end.equals("wmv")) {
            id = R.drawable.wmv;
        } else if (end.equals("xls")) {
            id = R.drawable.xls;
        } else if (end.equals("xml")) {
            id = R.drawable.xml;
        } else if (end.equals("zip")) {
            id = R.drawable.zip;
        } else if (end.equals("3gp") || end.equals("flv")) {
            id = R.drawable.file_video;
        } else if (end.equals("amr")) {
            id = R.drawable.file_audio;
        } else {
            id = R.drawable.default_fileicon;
        }
        return id;
    }
    /**
     * 打开文件
     *
     * @param context
     * @param aFile
     */
    public void openFile(Context context, File aFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        String fileName = aFile.getName();
        String end = getFileEXT(fileName).toLowerCase();

        if (aFile.exists()) {
            if (checkEndsInArray(end, new String[]{"png", "gif", "jpg", "bmp"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "image/*");
            } else if (checkEndsInArray(end, new String[]{"apk"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "application/vnd.android.package-archive");
            } else if (checkEndsInArray(end, new String[]{"mp3", "amr", "ogg", "mid", "wav"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "audio/*");
            } else if (checkEndsInArray(end, new String[]{"mp4", "3gp", "mpeg", "mov", "flv"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "video/*");
            } else if (checkEndsInArray(end, new String[]{"txt", "ini", "log", "java", "xml", "html"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "text/*");
            } else if (checkEndsInArray(end, new String[]{"doc", "docx"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "application/msword");
            } else if (checkEndsInArray(end, new String[]{"xls", "xlsx"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "application/vnd.ms-excel");
            } else if (checkEndsInArray(end, new String[]{"ppt", "pptx"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "application/vnd.ms-powerpoint");
            } else if (checkEndsInArray(end, new String[]{"chm"})) {
                intent.setDataAndType(Uri.fromFile(aFile), "application/x-chm");
            } else {
                intent.setDataAndType(Uri.fromFile(aFile), "application/" + end);
            }

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "没有找到适合打开此文件的应用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 文件分组
     * @param list
     * @return
     */
    public List<FileInfo> getGroupList(List<FileInfo> list) {
        ArrayList<FileInfo> dirs = new ArrayList<FileInfo>();// 文件夹列表
        ArrayList<FileInfo> files = new ArrayList<FileInfo>();// 文件列表
        for(int i = 0; i < list.size(); i++) {
            FileInfo item = (FileInfo)list.get(i);
            if (item.type == 0) {
                dirs.add(item);
            } else {
                files.add(item);
            }
        }
        dirs.addAll(files);// 合并,文件夹+文件
        return dirs;
    }

    /**
     * 删除一个文件
     *
     * @param path
     */
    public void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 删除一个文件夹(递归调用)
     *
     * @param path
     */
    public void deleteDir(String path) {
        File dir = new File(path);
        File[] files = null;
        if (dir.exists()) {
            files = dir.listFiles();
        }

        if (files != null && files.length > 0) {
            for(int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                    deleteFile(file.getAbsolutePath());
                }
                if (file.isDirectory()) {
                   deleteDir(file.getAbsolutePath());
                }
            }
        }
        dir.delete();
    }

    /**
     * 对单个文件的内容的拷贝
     *
     * @param from 待拷贝的原文件对象
     * @param to   目的地文件对象
     */
    public void copyFile(File from, File to) {
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);
            in = fis.getChannel();
            out = fos.getChannel();
            in.transferTo(0L, in.size(), (WritableByteChannel)out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null && fos != null && in != null && out != null) {
                try {
                    fis.close();
                    fos.close();
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 在给定的文件夹目录下,拷入目标文件
     *
     * @param targetDir 目标文件夹的路径
     * @param file      待考的文件对象
     * @return
     */
    public int pasteFile(String targetDir, File file) {
        // 生成目的地文件对象  新的文件夹/abc.txt
        File newFile = new File(targetDir, file.getName());
        if (newFile.exists()) {
            return 1;   // 没有成功
        } else {
            // 没有重名文件
            try {
                if (file.isFile()) {
                    newFile.createNewFile();// 新创建文件(空)
                } else if (file.isDirectory()) {
                    newFile.mkdirs();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            copyFile(file, newFile);
            return 0;
        }
    }

    /**
     * 在给定的文件夹目录下,粘贴上之前标记的文件夹中所有的内容(递归)
     * @param targetDir 目标文件夹的路径
     * @param dir 待考的文件夹对象
     * @return
     */
    public int pasteDir(String targetDir, File dir) {
        // 生成目的地文件对象  targetDir=/temp, dir = abc
        //newDir = /temp/abc/....
        File newDir = new File(targetDir, dir.getName());
        // 生成这个newDir所对应的路径
        newDir.mkdirs();
        File[] files = null;// 声明了一个文件对象数组
        if (dir.exists()) {// 判断路径是否存在
            files = dir.listFiles();// 该文件对象下所属的所有文件和文件夹列表
        }

        if (files != null && files.length > 0) {
            for(int var6 = 0; var6 < files.length; ++var6) {
                File file = files[var6];
                // 2. 对每个子元素进行
                if (file.isFile()) {// 复制文件
                    pasteFile(newDir.getAbsolutePath(), file);
                }
                if (file.isDirectory()) {// 复制文件夹!!!
                   pasteDir(newDir.getAbsolutePath(), file);// 递归调用
                }
            }
        }
        return 0;
    }
}
