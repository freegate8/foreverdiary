package com.liyin.foreverdiary;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.liyin.foreverdiary.zip.IZipCallback;
import com.liyin.foreverdiary.zip.ZipManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 永恒日记 Forever Diary
 * @author liufei
 */

public class ForeverDiaryApp extends CustomApp{

    private static String diaryHtmlFile = "index.htm";
    private static String dumpFileExtName = "zip";
    private static String todayWeather = "";
    private static WebView mWebView;
    private static int mHttpServerPort = 0;
    private MainActivity mainActivity;

    private static String sUsername = "";


    @Override
    public void onCreate() {
        super.onCreate();
        initDb();
        setParam("versionCode", String.valueOf(getVersionCode()));
        cleanCache();
    }
    //根据日记id获取其文件夹
    public static String getLogPath(String logid)
    {
        String path = CustomApp.getFilesPath();
        String date = logid.substring(0, 8);
        String time = logid.substring(8);
        return path+"/"+date+"/"+time;
    }
    public static ForeverDiaryApp getInstance()
    {
        return (ForeverDiaryApp)sApp;
    }

    public static void setWebView(WebView wv)
    {
        mWebView = wv;
    }

    public WebView getWebView()
    {
        return mWebView;
    }


    public int getHttpServerPort()
    {
        if(mHttpServerPort == 0)
        {
            mHttpServerPort = Utils.getFreePort(1000, 65535);
        }
        return mHttpServerPort;
    }

    public static String getLogContent(String logid)
    {
        String path = ForeverDiaryApp.getLogPath(logid);
        String content = FileOperator.ReadTxtFile(path+"/"+diaryHtmlFile);
        return content;
    }

    public void setMainActivity(MainActivity activity)
    {
        mainActivity = activity;
    }
    public void setCurrLogID(String id)
    {
        if(mainActivity != null)  mainActivity.setLogID(id);
    }

    public ArrayList<String> getDiaryClasses()
    {
        ArrayList<String> list = new ArrayList();
        try{
            Cursor c = getDB().rawQuery("select class  from diaryClasses", null);
            while (c.moveToNext()) {
                String cls = c.getString(c.getColumnIndex("class"));
                list.add(cls);
            }
            c.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return  list;
    }
    private boolean initDb()
    {
        String dbInited = getSavedParam("dbinited","0");
        if(dbInited.equals("1"))
        {
            updateDb();
            return false;
        }

        getDB().execSQL("DROP TABLE IF EXISTS diarys");
        getDB().execSQL("CREATE TABLE diarys(id INTEGER PRIMARY KEY, logid VARCHAR, class NVARCHAR, weather NVARCHAR, content NTEXT, bg VARCHAR, passwd VARCHAR, posttime TIMESTAMP DEFAULT (datetime('now','localtime')))");

        getDB().execSQL("DROP TABLE IF EXISTS diaryClasses");
        getDB().execSQL("CREATE TABLE diaryClasses(id INTEGER PRIMARY KEY, class NVARCHAR)");
        ContentValues value = new ContentValues();
        value.put("class","Work");
        mDB.insert("diaryClasses",null,value);
        value.put("class","Life");
        mDB.insert("diaryClasses",null,value);
        value.put("class","Study");
        mDB.insert("diaryClasses",null,value);
        value.put("class","Essay");
        mDB.insert("diaryClasses",null,value);

        mDB.execSQL("DROP TABLE IF EXISTS cfg");
        mDB.execSQL("CREATE TABLE cfg(id INTEGER PRIMARY KEY, name VARCHAR, value VARCHAR)");

        saveParam("dbinited", "1");
        return true;
    }

    //如果是之前已安装，执行数据库相关升级
    private boolean updateDb()
    {
        try{
            int bgCol = 0;
            int pwdCol = 0;
            Cursor c = getDB().rawQuery("select * from diarys where 1 ORDER BY id DESC LIMIT 1", null);
            if (c.moveToNext()) {
                bgCol =  c.getColumnIndex("bg");
                pwdCol = c.getColumnIndex("passwd");
            }
            c.close();

            if(bgCol == -1)
            {
                String sql = "alter table diarys add column bg varchar";
                mDB.execSQL(sql);
            }
            if(pwdCol == -1)
            {
                String sql = "alter table diarys add column passwd varchar";
                mDB.execSQL(sql);
            }

            //String sql = "update diarys set posttime='2020-04-01 12:55:55' where id=1";
            //mDB.execSQL(sql);

        }catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public boolean newDiaryClass(String cls)
    {
        ContentValues value = new ContentValues();
        value.put("class",cls);
        mDB.insert("diaryClasses",null,value);
        return true;
    }
    public boolean delDiaryClass(String cls)
    {
        mDB.execSQL("delete from diaryClasses WHERE class='"+cls+"'");
        return true;
    }

    public static void setWeather(String weather)
    {
        todayWeather = weather;
    }

    public static String getWeather()
    {
        return todayWeather;
    }

    public static String getTplName()
    {
        return "1";
    }

    //删除上次导出的临时文件
    private void cleanCache()
    {
        String cachePath = getCachePath();
        File cacheFolder = new File(cachePath);
        File[] caches = cacheFolder.listFiles();
        for (File file : caches) {
            if(file.getName().endsWith("."+dumpFileExtName))
            {
                FileOperator.deleteFile(file);
            }
        }

        String filesPath = getFilesPath();
        File filesFolder = new File(filesPath);
        File[] files = filesFolder.listFiles();
        for (File file : files) {
            if(file.getName().endsWith(".txt"))
            {
                FileOperator.deleteFile(file);
            }
        }

    }

    public static void setParam(String name, String value) {
        String sql = "DELETE FROM cfg WHERE name='"+name+"'";
        mDB.execSQL(sql);
        //LOG.D("name="+name+", value="+value);
        ContentValues r = new ContentValues();
        r.put("name",name);
        r.put("value",value);
        mDB.insert("cfg",null,r);
    }

    public static String getParam(String name)
    {
        String value = "";
        try {
            Cursor c = getDB().rawQuery("select * from cfg where name='"+name+"'", null);
            if (c.moveToNext()) {
                value =  c.getString(c.getColumnIndex("value"));
            }
            c.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return  value;
    }

    public boolean login(String password)
    {
        String pwd2 = getParam("enterPassword");
        if(pwd2.equals(password))
        {
            sUsername = "user";
            return true;
        }
        return false;
    }

    public void logout()
    {
        sUsername = "";
        System.exit(-1);
    }
    public boolean isLogout()
    {
        String pwd = getParam("enterPassword");
        if(pwd.length()>0 && sUsername.length()==0) return true;
        return false;
    }
    //清除所有密码
    public void clearPwd()
    {
        setParam("enterPassword","");
        String sql = "UPDATE diarys SET passwd='' WHERE 1";
        mDB.execSQL(sql);
    }

    //设置某篇日记的口令
    public void setDocPwd(String id, String pwd)
    {
        String sql = "UPDATE diarys SET passwd='"+pwd+"' WHERE id="+id;
        mDB.execSQL(sql);
    }
}
