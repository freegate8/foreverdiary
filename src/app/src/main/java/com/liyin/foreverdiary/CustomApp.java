package com.liyin.foreverdiary;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 永恒日记 Forever Diary
 * @author liufei
 */

public class CustomApp extends Application {
    protected static CustomApp sApp;
    /**
     * 本地数据库
     */
    protected static SQLiteDatabase mDB;

    //启动app，传入的参数
    private static Map<String, String> mParams = new HashMap<String, String>();


    public static final String DATA_DIR = "data";
    public static final String DB_FILE = "main.db";

    @Override
    public void onCreate() {
        super.onCreate();
        LOG.initTag("diary", false); // 测试LOG输出.

        Context ctx = getApplicationContext();
        sApp = this;
        mDB = SQLiteDatabase.openOrCreateDatabase(getDbFile(),  null);

    }

    public static CustomApp getInstance()
    {
        return sApp;
    }


    /**
     * 获取本地数据库文件全路径
     * @return
     */
    private static String getDbFile()
    {
        return getDataPath()+"/"+DB_FILE;
    }

    /**
     * 获取本地数据库
     * @return SQLiteDatabase对象
     */
    public static SQLiteDatabase getDB()
    {
        return mDB;
    }

    /**
     *获取本地资源保存路径
     * @return
     */
    public static String getDataPath()
    {
        Context ctx = CustomApp.getContext();
        String savePath = ctx.getDir(DATA_DIR, Context.MODE_PRIVATE).getAbsolutePath();
        String c = ctx.getFilesDir().getPath();
        return savePath;
    }

    /**
     * 获取files目录
     * @return
     */
    public static String getFilesPath() {
        Context ctx = CustomApp.getContext();
        String filesPath = ctx.getFilesDir().getPath();
        return filesPath;
    }

    /**
     * 获取cache目录
     * @return
     */
    public static String getCachePath() {
        Context ctx = CustomApp.getContext();
        String filesPath = ctx.getCacheDir().getPath();
        return filesPath;
    }

    /**
     * 获取全局上下文*/
    public static Context getContext() {
        return sApp.getApplicationContext();
    }

    /**
     * 获取包名
     * @return
     */
    public static String getApkPackageName()
    {
        String packageName = "";
        try {
            packageName = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageName;
    }

    /**
     * 获取版本号
     * @return
     */
    public static int getVersionCode() {
        //获取包管理器
        PackageManager pm = getContext().getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
            //返回版本号
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;

    }


    protected static String getSavedParam(String name, String defaultVal) {
        String param;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sApp.getApplicationContext());
        param = prefs.getString(name, defaultVal);
        return param;
    }

    /**
     * 保存参数到本地
     * @param name
     * @param value
     */
    public static void saveParam(String name, String value) {
        //LOG.D("name="+name+", value="+value);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sApp.getApplicationContext());
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(name, value);
        edit.commit();
    }
}
