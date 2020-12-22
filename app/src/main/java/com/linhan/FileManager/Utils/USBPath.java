package com.linhan.FileManager.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.storage.StorageManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射获取U盘路径和名称
 */
public class USBPath {

    /**
     * 必须是静态方法，否则系统报错
     * @param context
     * @return
     */
    public static List<String> getUPath(Context context){
        List<String> strs = new ArrayList<>();

        StorageManager mStorageManager = (StorageManager) context.getSystemService(Activity.STORAGE_SERVICE);
        Class<?> volumeInfoClazz = null;
        Method getVolumes = null;
        Method isMountedReadable = null;
        Method getType = null;
        Method getPath = null;
        List<?> volumes = null;
        try {
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
            getVolumes = StorageManager.class.getMethod("getVolumes");
            isMountedReadable = volumeInfoClazz.getMethod("isMountedReadable");
            getType = volumeInfoClazz.getMethod("getType");
            getPath = volumeInfoClazz.getMethod("getPath");
            volumes = (List<?>) getVolumes.invoke(mStorageManager);
            if (volumes.size()==0){
                //如果设备列表为空
                return strs;
            }
            for (Object vol : volumes) {
                if (vol != null && (boolean) isMountedReadable.invoke(vol) && (int) getType.invoke(vol) == 0) {
                    File path2 = (File) getPath.invoke(vol);
                    String name = path2.getName();    //U盘名称
                    String p2 = path2.getPath();    //U盘路径
                    strs.add(name);
                    strs.add(p2);
                    return strs;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return strs;
    }
}
