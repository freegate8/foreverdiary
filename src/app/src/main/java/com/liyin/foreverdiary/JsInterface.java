package com.liyin.foreverdiary;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 永恒日记 Forever Diary
 * @author liufei
 */

public class JsInterface {
    public static final String APK_NAME = "ForeverDiary";
    public final int CODE_SELECT_IMAGE = 2;//相册RequestCode
    private MainActivity mainActivity;

    public JsInterface(Activity activity)
    {
        mainActivity = (MainActivity) activity;
    }

    @JavascriptInterface
    public boolean save(String log) {
        return Diary.save(log, Diary.MODE_EDIT);
    }

    @JavascriptInterface
    public void setCurrLogID(String logid) {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        app.setCurrLogID(logid);
    }

    @JavascriptInterface
    public String loadLog(String id, int mode) {
        Diary d = new Diary(id);
        return d.toString(mode);
    }

    @JavascriptInterface
    public String getWeather() {
        String content= ForeverDiaryApp.getWeather();
        return content;
    }

    @JavascriptInterface
    public String InsertImg(String logid) {
        LOG.D("InsertImg....."+logid);
        mainActivity.goPhotoAlbum(logid);
        return logid;
    }

    @JavascriptInterface
    public void selectAudio() {
        mainActivity.selectAudio();
    }


    @JavascriptInterface
    public String list(String filter, int start, int count) {
          String content= Diary.getDiaryList(filter, start, count);
        return content;
    }

    @JavascriptInterface
    public Boolean del(String id) {
        return Diary.del(id);
    }

    @JavascriptInterface
    public void exportDiary(String startdate, String enddate) {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        DiaryDump dump = new DiaryDump();
        dump.export(startdate, enddate);
    }

    /**
     * 导入日记
     * @param dumpFile
     */
    @JavascriptInterface
    public void importDiary(String dumpFile)
    {
        DiaryDump dump = new DiaryDump();
        dump.importFrom(dumpFile);
    }

    /**
     获取日记分类
     */
    @JavascriptInterface
    public String getDiaryClasses() {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        ArrayList<String> list = app.getDiaryClasses();
        JSONArray jlist = new JSONArray();
        try{
            for (String l: list) {
                JSONObject item = new JSONObject();
                item.put("name", l);
                jlist.put(item);
            }
        }catch (Exception e)
        {

        }
        return jlist.toString();
    }
    @JavascriptInterface
    public void newDiaryClass(String cls)
    {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        app.newDiaryClass(cls);
    }

    @JavascriptInterface
    public void delDiaryClass(String cls)
    {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        app.delDiaryClass(cls);
    }

    @JavascriptInterface
    public void setParam(String name, String value)
    {
        ForeverDiaryApp.setParam(name, value);
    }

    @JavascriptInterface
    public String getParam(String name)
    {
        return ForeverDiaryApp.getParam(name);
    }

    @JavascriptInterface
    public int login(String pwd)
    {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        boolean ret =  app.login(pwd);
        return ret ? 1 : 0;
    }

    @JavascriptInterface
    public void logout()
    {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        app.logout();
    }

    /**********************************日记参数************************/
    //获取稿纸（背景图片）
    @JavascriptInterface
    public String getPapers()
    {
        return Diary.getPapers();
    }

    /************************获取日记月份列表******************/
    @JavascriptInterface
    public String getMonths() {
        return Diary.getMonths();
    }

    @JavascriptInterface
    public void clearPwd() {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        app.clearPwd();
    }

    @JavascriptInterface
    public void setPwd(String id, String pwd) {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        app.setDocPwd(id, pwd);
    }

    @JavascriptInterface
    public int getHttpPort()
    {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        int port = app.getHttpServerPort();
        return port;
    }

    //获取某篇日记中图片列表
    @JavascriptInterface
    public String getDiaryImgList(String id)
    {
        ArrayList<String> list =Diary.getImgList(id);
        JSONArray jlist = new JSONArray();
        try{
            for (String l: list) {
                JSONObject item = new JSONObject();
                item.put("img", l);
                jlist.put(item);
            }
        }catch (Exception e)
        {

        }
        return jlist.toString();
    }

}
