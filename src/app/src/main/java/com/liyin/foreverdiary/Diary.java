package com.liyin.foreverdiary;

import android.content.ContentValues;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 永恒日记 Forever Diary
 * @author liufei
 */

public class Diary {
    protected static SQLiteDatabase mdb;
    protected String id;
    public static int MODE_VIEW = 0;
    public static int MODE_EXPORT = 1;
    public static int MODE_EDIT = 2;
    public static int MODE_IMPORT = 3;
    protected  String logid;


    public Diary(String id)
    {
        mdb = CustomApp.getDB();
        this.id = id;
        if(this.id != null && this.id.length()>0) {
            Cursor c = mdb.rawQuery("select * from diarys where id=" + id, null);
            if (c.moveToNext()) {
                logid = c.getString(c.getColumnIndex("logid"));
            }
            c.close();
        }
    }

    public int getIdByLogID(String logid)
    {
        int id = 0;
        try{
            Cursor c = mdb.rawQuery("select id from diarys where logid='"+logid+"'", null);
            if (c.moveToNext()) {
                id = c.getInt(c.getColumnIndex("id"));
            }
            c.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return id;
    }

    public boolean isExist()
    {
        if(id.length()==0) return false;

        boolean ret = false;
        Cursor c = mdb.rawQuery("select * from diarys where id="+id, null);
        if (c.moveToNext()) {
            ret = true;
        }
        c.close();
        return ret;
    }

    public boolean save(JSONObject d)
    {
        return save(d, MODE_EDIT);
    }

    public boolean save(JSONObject d, int mode)
    {

        String content = d.optString("content");
        String id = d.optString("id");
        String logid = d.optString("logid");
        if(id.length()==14 && id.startsWith("2020"))
        {
            this.id = null;
            logid = id;
        }else {
            this.id = id;
        }

        boolean bModify = false;
        if(this.id != null && this.id.length()>0 && !this.id.equals("null")) bModify = true;

        if(logid==null || logid.length()==0)
        {
            String posttime = d.optString("posttime");
            LOG.D(posttime);
            logid = posttime.substring(0,4)+posttime.substring(5,7)
                    +posttime.substring(8,10)
                    +posttime.substring(11, 13)
                    +posttime.substring(14, 16)
                    +posttime.substring(17, 19);
            LOG.D(logid);
            bModify = false;
        }

        //无内容，则执行删除操作
        if(isEmptyContent(content))
        {
            del();
            return true;
        }
        String newCnt = trimSrcUrl(content);

        if(id == null || id.length()==0 || id.equals("null"))
        {
            id = String.valueOf(getIdByLogID(logid));
            this.id = id;
        }
        if(id.equals("0") || id.equals("null"))
        {
            bModify = false;
        }else {
            bModify = true;
        }


        if(mode == MODE_IMPORT) bModify = false;

        ContentValues value = new ContentValues();
        value.put("logid", logid);
        value.put("weather", d.optString("weather"));
        value.put("class", d.optString("class"));
        value.put("bg", d.optString("bg"));
        String posttime = d.optString("posttime");
        if(posttime.length()>0) {
            value.put("posttime", d.optString("posttime"));
        }

        value.put("content",newCnt);

        if(!bModify) {
            LOG.D(value.toString());
            mdb.insert("diarys",null,value);
        }else{
            String whereClause = "id=?";
            String[] whereArgs={this.id};
            mdb.update("diarys",value,whereClause,whereArgs);
        }
        return true;
    }
    public static boolean save(String json, int mode)
    {
        boolean ret = false;
        try {
            JSONObject logObj = new JSONObject(json);
            String id = logObj.optString("id");
            if(id.equals("null")) id = null;
            Diary d = new Diary(id);
            ret = d.save(logObj, mode);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 以json字符串方式返回日记
     * @return
     */
    public String toString()
    {
        return toString(MODE_VIEW);
    }

    /**
     *
     * @param mode 0:默认模式（在程序内展示），1：导出模式（在导出模板中展示）
     * @return
     */
    public String toString(int mode)
    {
        JSONObject log = new JSONObject();
        try {
            String postime = "";
            Cursor c = mdb.rawQuery("select * from diarys where id="+id, null);
            if (c.moveToNext()) {
                log.put("id", c.getString(c.getColumnIndex("id")));
                log.put("logid", c.getString(c.getColumnIndex("logid")));
                log.put("class", c.getString(c.getColumnIndex("class")));
                log.put("weather", c.getString(c.getColumnIndex("weather")));
                log.put("bg", c.getString(c.getColumnIndex("bg")));
                log.put("passwd", c.getString(c.getColumnIndex("passwd")));
                postime = c.getString(c.getColumnIndex("posttime"));
                log.put("posttime", postime);
                String content = c.getString(c.getColumnIndex("content"));
                String id = c.getString(c.getColumnIndex("id"));
                if(mode != MODE_EXPORT) {
                    content = appendSrcUrl(content);
                }
                if(mode == MODE_VIEW) {
                    content = wrapImgWithAnchor(id, content);
                }
                log.put("content", content);
            }
            c.close();

            //获取本篇日记的相领两篇日记的id
            c = mdb.rawQuery("select id from diarys where posttime < '"+postime+"' order by posttime desc LIMIT 1", null);
            if (c.moveToNext()) {
                log.put("preid", c.getString(c.getColumnIndex("id")));
            }
            c.close();

            c = mdb.rawQuery("select id from diarys where posttime > '"+postime+"' order by posttime LIMIT 1", null);
            if (c.moveToNext()) {
                log.put("nextid", c.getString(c.getColumnIndex("id")));
            }
            c.close();

        }catch (Exception e)
        {
            e.printStackTrace();
        }
        //LOG.D(log.toString());
        return log.toString();
    }

    public static String dumpDate(String begindate, String enddate)
    {
        initDB();
        JSONArray list = new JSONArray();
        try{
            Cursor c = mdb.rawQuery("select id from diarys where date(posttime)>='"+begindate+"' AND date(posttime) <='"+enddate+"' ORDER BY id", null);
            while (c.moveToNext()) {
                String id = c.getString(c.getColumnIndex("id"));
                Diary d = new Diary(id);
                JSONObject item = new JSONObject(d.toString(MODE_EXPORT));
                list.put(item);
            }
            c.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return list.toString();
    }

    //获取某段时间内的不同的背景图片（用于导出）
    public static void getDistinctBg(List<String> bglist, String begindate, String enddate)
    {
        initDB();
        try{
            Cursor c = mdb.rawQuery("select distinct(bg) from diarys where date(posttime)>='"+begindate+"' AND date(posttime) <='"+enddate+"'", null);
            while (c.moveToNext()) {
                String bg = c.getString(c.getColumnIndex("bg"));
                bglist.add(bg);
            }
            c.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public boolean del()
    {
        if(!isExist()) return false;
        String sql = "DELETE FROM diarys WHERE id="+id;
        mdb.execSQL(sql);
        //删除日记文件夹
        String path = ForeverDiaryApp.getLogPath(logid);
        try {
            FileOperator.delFolder(path);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean del(String id)
    {
        Diary d = new Diary(id);
        return d.del();
    }

    /**
     * 去<>标签及空格符后，检测有无内容
     * @param content
     * @return
     */
    public static boolean isEmptyContent(String content)
    {
        if(content.indexOf("<img") != -1
                || content.indexOf("<audio") != -1) return false;
        String noHtmlCnt = content.replaceAll("\\<.*?>","");
        noHtmlCnt = noHtmlCnt.replaceAll("\\s*",""); //空格
        noHtmlCnt = noHtmlCnt.replaceAll("\\d*.*℃",""); //天气
        return  noHtmlCnt.length()==0;
    }

    //去除图片、音频等本地文件的路径，只保留图片文件名，反向操作：appendSrcRootPath
    private static String trimSrcUrl(String content)
    {
        Pattern p = Pattern.compile("(<\\s*\\S+\\s*src\\s*=(\\s*)\")(.*?\"\\s*>)");
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String a = m.group(3);
            a = a.replaceAll("/data/\\S*/\\S*files/", "files/");
            m.appendReplacement(sb, "$1" + a+ "$2");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    //给本地资源（图、音视频）的相对路径加上绝对路径
    public static String appendSrcUrl(String content)
    {
        String packName = ForeverDiaryApp.getApkPackageName();
        Pattern p = Pattern.compile("(<\\s*\\S+\\s*src\\s*=(\\s*)\")(.*?\"\\s*>)");
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String a = m.group(3);
            a = a.replaceAll("files/", "/data/data/"+packName+"/files/");
            m.appendReplacement(sb, "$1" + a+ "$2");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static ArrayList<String> getImgList(String id)
    {
        initDB();
        String content = "";
        try {
            Cursor c = mdb.rawQuery("select * from diarys where id="+id, null);
            if (c.moveToNext()) {
                content = c.getString(c.getColumnIndex("content"));
            }
            c.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        ArrayList<String> list = new ArrayList();

        Pattern p = Pattern.compile("(<\\s*img\\s*src\\s*=\"(.*?)\")");
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        String packName = ForeverDiaryApp.getApkPackageName();
        while (m.find()) {
            String imgSrc = m.group(2);
            if(imgSrc.startsWith("files"))
            {
                imgSrc = "/data/data/"+packName+"/"+imgSrc;
            }
            m.appendReplacement(sb, imgSrc);
            list.add(imgSrc);
        }
        return list;
    }

    //给正文中所有<img>标签增加<a>锚点
    public static String wrapImgWithAnchor(String id, String content)
    {
        Pattern p = Pattern.compile("(<\\s*img\\s*src\\s*=\\s*(.*?)[^>]*?>)");
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "<a href=\"imgviewer.html?id="+id + "\">"+ "$1"+"</a>");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    //可用背景
    public static String getPapers()
    {
        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        AssetManager am = app.getAssets();
        String bgstr= "[";
        try{
            String tplFile = "editor/dist/bg";
            String list[] = am.list(tplFile);
            for (int i=0; i<list.length;i++ ) {
                bgstr += "\""+list[i]+"\",";
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        if(bgstr.length()>1) bgstr = bgstr.substring(0, bgstr.length()-1);
        bgstr += "]";
        return bgstr;
    }

    private static void initDB()
    {
        if(mdb == null) mdb = ForeverDiaryApp.getDB();
    }
    public static String getMonths()
    {
        initDB();
        String sql = "select DISTINCT(strftime( \"%Y-%m\", posttime)) from diarys order by posttime desc";
        String list = "[";
        try {
            Cursor c = mdb.rawQuery(sql, null);
            while (c.moveToNext()) {
                list += "\""+c.getString(0)+"\",";
            }
            c.close();
            if(list.length()>1) list = list.substring(0, list.length()-1);
            list += "]";
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return list;
    }


    /**
     *
     * @param start
     * @param count
     * @return
     */
    public static String getDiaryList(String filter, int start, int count)
    {
        initDB();

        JSONArray list = new JSONArray();
        try{
             String fsql = "";
            JSONObject f = new JSONObject(filter);
            String m = f.optString("month");
            if(m.length() >0 )
            {
                fsql += "strftime( \"%Y-%m\", posttime)='"+m+"'";
            }
            String k = f.optString("key");
            if(k.length()>0)
            {
                fsql += " content like'%"+k+"%'";
            }
            if(fsql.length()>0) fsql = "where "+fsql;
            String sql = "select id, date(posttime) as date,strftime('%H:%M:%S',posttime) as time, class, logid, content,passwd  from diarys "+fsql+" ORDER BY posttime DESC LIMIT "+String.valueOf(start)+","+String.valueOf(count);
            //LOG.D(sql);
            Cursor c = mdb.rawQuery(sql, null);
            while (c.moveToNext()) {
                String cls = c.getString(c.getColumnIndex("class"));
                String id = c.getString(c.getColumnIndex("id"));
                String logid = c.getString(c.getColumnIndex("logid"));
                String date = c.getString(c.getColumnIndex("date"));
                String time = c.getString(c.getColumnIndex("time"));
                String passwd = c.getString(c.getColumnIndex("passwd"));
                if(passwd == null)  passwd = "";

                JSONObject item = new JSONObject();
                item.put("id", id);
                item.put("logid", logid);
                item.put("class", cls);
                item.put("passwd", passwd);

                String content = c.getString(c.getColumnIndex("content"));

                if(passwd.length()>0)
                {
                    content = content.replaceAll("\\<.*?>","");
                    if(content.length()>10) content = content.substring(0, 10)+"...";
                }else {
                    String imgs = loadLogImages(id, content);
                    content = Diary.appendSrcUrl( content);
                    content = content.replaceAll("\\<.*?>","");
                    if(content.length()>50) content = content.substring(0, 50)+"...";
                    content += "<br>"+imgs;
                }
                item.put("content", content);
                item.put("date", date);
                item.put("time", time);
                list.put(item);
            }
            c.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return list.toString();
    }

    //输出所有图片组成的<img>标签（并根据图片数量调整大小），用于日记列表
    public static String loadLogImages(String id, String content)
    {
        ArrayList<String> imgs = getImgList(id);
        int width = 100;
        switch (imgs.size())
        {
            case 1:
                width = 180;
                break;
            case 2:
                width = 80;
                break;
            case 3:
                width = 70;
                break;
            default:
                width = 50;
        }
        StringBuffer sb = new StringBuffer();
        String packName = ForeverDiaryApp.getApkPackageName();
        for(int i=0; i<imgs.size(); i++)
        {
            sb.append("<img src=\""+imgs.get(i)+"\" width=\""+String.valueOf(width)+" px\">");
        }
        //LOG.D(sb.toString());
        return sb.toString();
        /*
        Pattern p = Pattern.compile("(<\\s*img\\s*src\\s*=\\s*\")(.*?\"\\s*>)");
        Matcher m = p.matcher(content);
        int width = 100;
        int count = 0;
        Matcher n = p.matcher(content);
        while (n.find()) {
            count++;
        }
        switch (count)
        {
            case 1:
                width = 180;
                break;
            case 2:
                width = 80;
                break;
            case 3:
                width = 60;
                break;
            default:
                width = 50;
        }
        StringBuffer sb = new StringBuffer();
        String packName = ForeverDiaryApp.getApkPackageName();
        String widthHtml = "width: "+String.valueOf(width)+"px;\"";
        while (m.find()) {
            String g0 = m.group(0);
            String g1 = m.group(1);
            String g2 = m.group(2);
            g2 = g2.replaceAll("width:\\s*\\S*\"", widthHtml);
            String img = g1+"/data/data/"+packName+"/"+g2;
            sb.append(img);
        }
        return sb.toString();

         */

    }
}
