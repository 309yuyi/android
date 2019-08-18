package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;

/**FileManerger - 文件授权和读写管理类
 * 构造后要调用requestFilePermission()才会申请权限
 * 1.请传入当前Activity
 * 2.请在当前Activity的onRequestPermissionsResult回调本类的函数，最后一个参数false
 * */
public class FileManerger {

    private String rootFilePath;
    private Activity activity;
    public static final int PERMISSION_REQUESTED = 905;
    public static final int PERMISSION_REQUESTING = 418;
    public static final int PERMISSION_FAILED = 286;
    public long timecounter = 0;
    private static final int REQUEST_CODE = 205;

    public FileManerger(Context context) {
        activity = (Activity) context;
    }

    public FileManerger(Context context, String rootpath) {
        activity = (Activity) context;
        rootFilePath = rootpath;
    }

    public static void setCameraPath(Context context,String path)
    {
        SharedPreferences.Editor editor = context.getSharedPreferences("path",MODE_PRIVATE).edit();
        editor.putString("camerapath",path);
        editor.commit();
    }

    /**
     * 获取相机的目录：
     * 如果第一次打开则从系统中读取，否则从应用数据中读取。
     * @param context 当前context
     * @return 相机路径字段
     */
    public static String getCameraPath(Context context)
    {
        SharedPreferences read = context.getSharedPreferences("path", MODE_PRIVATE);
        String defaultCameraPath = Environment.DIRECTORY_DCIM;
        return read.getString("camerapath","/sdcard/DCIM/Camera/");
    }
    public File [] getImageFileLists()
    {
        File file_read = new File(rootFilePath);
        //文件名

        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if(s.endsWith(".jpg")||s.endsWith(".png"))
                    return true;
                else
                    return false;
            }
        };
        File tmp[] =  file_read.listFiles(filenameFilter);
        return tmp;
    }
    public int getImagesnum()
    {
        File[] file=getImageFileLists();
        if(file!=null)
        {
            return file.length;
        }
        else
        {
            return 0;
        }
    }

    public void setRootPath(String rootpath)
    {
        rootFilePath=rootpath;
    }

    /**申请文件读写权限*/
    int countt=0;
    public int requestFilePermission()
    {
        //if(System.currentTimeMillis()-timecounter<1000)
        if(countt++==1)//防止小米云测FC
        {
            OnFinalPermissionResult(false);

            return PERMISSION_FAILED;
        }
        int checkSelfReadPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int checkSelfWritePermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (checkSelfReadPermission == PackageManager.PERMISSION_GRANTED&&checkSelfWritePermission == PackageManager.PERMISSION_GRANTED) {
            //如果都获取到了，则返回真
            OnFinalPermissionResult(true);
            return PERMISSION_REQUESTED;
        } else {

/*
            new DialogHelper(activity){
                @Override
                public boolean onDialogConfirmed(boolean result) {
                    if(result)
                    {
                            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);//动态申请打电话权限
                            timecounter = System.currentTimeMillis();
                    }
                    return super.onDialogConfirmed(result);
                }
            }.setAlertDialog("授权引导","本程序依赖于文件读写权限，特此向您说明，望确认授权以使用本程序功能。","同意授权");
*/
            //但凡有一个没获取到，则调用API获取两个权限
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);//动态申请打电话权限

            return PERMISSION_REQUESTING;
        }
    }


    static public String loadjson(Activity activity){



        File file = new File("/mnt/sdcard/eecso/filelist.json");

//Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        return text.toString();
    }
    public Intent getAppDetailSettingIntent() {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", activity.getPackageName());
        }
        return localIntent;
    }

    public static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    public static Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {

        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);

            return bm1;

        } catch (OutOfMemoryError ex) {
        }
        return null;

    }

    public static Bitmap getRotatedBitmap(String filepath,double targetsize)
    {
        if(new File(filepath).exists())
        {

            ExifInterface exif = null;
            try {
                exif = new ExifInterface(filepath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = FileManerger.exifToDegrees(rotation);

            Bitmap  tmpbit = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(filepath),(int)(targetsize), (int)(targetsize));
            tmpbit=adjustPhotoRotation(tmpbit,rotationInDegrees);
            return tmpbit;
        }
        else
        {
            return null;
        }
    }

    public static void writeFileData(Context context,String filename, String content){

        try {

            FileOutputStream fos = context.openFileOutput(filename, MODE_PRIVATE);//获得FileOutputStream

            //将要写入的字符串转换为byte数组

            byte[]  bytes = content.getBytes();

            fos.write(bytes);//将byte数组写入文件
            fos.close();//关闭文件输出流

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class Dialogdalay extends Thread {

        @Override
        public void run() {

        }
    }

    public boolean onPermissionRequestResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CODE) {


            //防止小米云测FC


            if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                if (grantResults[0] == 0) {
                    //Toast.makeText(activity, "申请成功", Toast.LENGTH_SHORT).show();
                    OnFinalPermissionResult(true);
                    return true;
                } else {


                    requestFilePermission();
                    /**用户拒绝授权，弹窗解释*/
                    // Toast.makeText(activity, "用户拒绝授权", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            return false;
        }
        return false;
    }

    /**当最终授权结果后，由内部调用，外部重写获得*/
    public boolean OnFinalPermissionResult(boolean result)
    {
        return result;
    }



}