package com.liyin.foreverdiary;

import android.content.ContentValues;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.liyin.foreverdiary.zip.IZipCallback;
import com.liyin.foreverdiary.zip.ZipManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DiaryDump implements IZipCallback {

    private String dumpBeginDate;
    private String dumpEndDate;
    private String dumpZipPath; //导出或导入的工作目录
    private String mTplZipFilePath; //导出用的模板
    private WebView mWebView;
    private String mExportZipFile;
    private String mDiaryPath;

    private ForeverDiaryApp app;
    private int mType = 0; //0导出  1：导入
    private int mStep = 0;

    private int mImportCount = 0; //导入的日记条数

    public DiaryDump()
    {
        app = ForeverDiaryApp.getInstance();
        mWebView = app. getWebView();
        mDiaryPath = app.getFilesPath();
        ZipManager.debug(true);
    }
    public  boolean importFrom(String file)
    {
        LOG.D(file);
        mType = 1;
        String filesPath = ForeverDiaryApp.getFilesPath();
        dumpZipPath = filesPath+"/dump";
        FileOperator.createFolder(dumpZipPath);

        String importZip = file;
        if(file.startsWith("content://"))
        {
            InputStream inputStream = null;
            try {
                Uri uri = Uri.parse(file);
                inputStream = ForeverDiaryApp.getContext().getContentResolver().openInputStream(uri);
                importZip =  dumpZipPath+"/temp.zip";
                FileOperator.dumpStreamToFile(inputStream, importZip);
                //请对读取到的内容content进行处理...


            } catch (Exception e) {

            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        LOG.D(">>>>>>>>>>>unzip imported file:"+importZip);
        //解压导入的文件
        String pwd = ForeverDiaryApp.getParam("dumpPassword");
        try{
            ZipManager.unzip(importZip, dumpZipPath, pwd, this);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        //SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        return true;
    }


    public String export(String begindate, String enddate)
    {
        mType = 0;
        dumpBeginDate = begindate;
        dumpEndDate = enddate;

        //创建导出工作目录
        String filesPath = app.getFilesPath();
        dumpZipPath = filesPath+"/"+begindate+"_"+enddate;
        FileOperator.createFolder(dumpZipPath);

        //复制模板到导出工作目录并解压，在解压后执行后续工作
        exportTplFiles();
        return dumpZipPath;
    }


    //将模板文件解压到目标目录
    private  void exportTplFiles()
    {
        AssetManager am = app.getAssets();
        try{
            String tplFile = "editor/tpl/"+app.getTplName()+".zip";
            InputStream t = am.open(tplFile);
            mTplZipFilePath = dumpZipPath+"/tpl.zip";
            FileOperator.dumpStreamToFile(t, mTplZipFilePath);
            LOG.D(mTplZipFilePath);
            LOG.D(dumpZipPath);
            ZipManager.unzip(mTplZipFilePath, dumpZipPath, this);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    //将要导出的目录转移到压缩目标目录内
    private void exportDiaryStep2()
    {
        mStep++;
        String begindate = dumpBeginDate;
        String enddate = dumpEndDate;
        String zipPath = dumpZipPath+"/files";
        String cachePath = app.getCachePath();
        String filesPath = app.getFilesPath();

        ArrayList<String> datelist = new ArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        try {
            Calendar d = Calendar.getInstance();
            Date date1 = sdf.parse(begindate);
            Date date2  = sdf.parse(enddate);
            d.setTime(date2);
            d.add(Calendar.DAY_OF_YEAR, 1);
            date2  = d.getTime();
            Date tmp = date1;
            while(!tmp.equals(date2))
            {
                d.setTime(tmp);
                tmp = d.getTime();
                String format = sdf2.format(tmp);
                datelist.add(format);
                d.add(Calendar.DAY_OF_YEAR, 1);
                tmp = d.getTime();
            }
            for(int i=0; i<datelist.size(); i++) {
                String dateDir = datelist.get(i);
                String strDatePath = filesPath+"/"+dateDir;
                LOG.D( zipPath+"/"+dateDir);
                boolean b = FileOperator.moveDirectory(strDatePath, zipPath+"/"+dateDir);
                LOG.D(strDatePath+"  ..........TOOoo  :"+String.valueOf(b));
            }

        }catch (Exception e)
        {

        }
        //要导出的日记内容
        String diaryData = Diary.dumpDate(begindate, enddate);
        //LOG.D("diaryData:"+diaryData);
        diaryData = "var data="+diaryData;
        String diaryDataFile = "data.js";
        try {
            File file = new File(dumpZipPath+"/dist", diaryDataFile);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(diaryData.getBytes());
            outputStream.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        //删除模板包
        FileOperator.deleteFile(mTplZipFilePath);

        //背景图片导出
        ArrayList<String > bglist = new ArrayList<String>();
        Diary.getDistinctBg(bglist, begindate, enddate);
        AssetManager am = app.getAssets();
        try{

            for(int i=0; i<bglist.size(); i++)
            {
                String bgFilename = bglist.get(i);
                if(bgFilename.length()>0) {
                    String tplFile = "editor/dist/bg/" + bgFilename;
                    InputStream t = am.open(tplFile);
                    if (t != null) {
                        String bgPath = dumpZipPath + "/dist/bg/" + bgFilename;
                        FileOperator.dumpStreamToFile(t, bgPath);
                    }
                }
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        String zipFilePath = cachePath+"/"+begindate+"_"+enddate+".zip";
        mExportZipFile = zipFilePath;
        String pwd = ForeverDiaryApp.getParam("dumpPassword");
        ZipManager.zip(dumpZipPath, zipFilePath , pwd, this);
    }
    public void onStart()
    {

    }

    public void onProgress(int percentDone){
        String eval= "onProgress(" + String.valueOf(percentDone) +")";
        exeJavaScript(eval);
    }

    private void exeJavaScript(String eval)
    {
        final int version = Build.VERSION.SDK_INT;
        if (version < 18) {
            mWebView.loadUrl("javascript:"+eval);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript("javascript:"+eval, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                }
            });
        }
    }

    public void onFinish(boolean success)
    {
        //onFinish后立即执行删除压缩包可能导致解压不完全
        try {
            Thread.sleep(3000);
        }catch (Exception e)
        {

        }

        if(mType == 0)
        {
            if(mStep == 1) {
                //将压缩目录内的日记重新移回原目录
                String ret = success ? "1": "0";
                String filesPath = app.getFilesPath();
                FileOperator.moveDirectory(dumpZipPath+"/files", filesPath);
                //删除压缩目录
                FileOperator.delFolder(dumpZipPath);
                String eval= "onZipFinish(" + ret + ",'" + mExportZipFile + "')";
                exeJavaScript(eval);

            }else if(mStep ==0)
            {
                exportDiaryStep2();
            }

        }else {
            LOG.D("模板解压完毕，执行后续导入工作");
            //延时删除导入工作目录，异步解压onFinish不准确
            new Handler().postDelayed(new Runnable(){
                     public void run(){
                          importStep2();
                     }
            },30000);

        }

    }

    //导入第二步：找到data.js导入数据库，并将附件移至标准目录
    private void importStep2()
    {
        //列目录 ,预期"yyyy-MM-dd_yyyy-MM-dd"
        File dumpFolder = new File(dumpZipPath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if ((!dumpFolder.exists()) || (!dumpFolder.isDirectory())) {
            LOG.D("压缩工作目录["+dumpZipPath+"]不存在");
            return;
        }

        // 找到files目录，将其移至标准目录
        File[] files = dumpFolder.listFiles();
        for (int i = 0; i < files.length; i++) {
            String subDir = files[i].getAbsolutePath();
            String fname = subDir+"/dist/data.js";
            File f = new File(fname);
            if(f.exists()) //是我们的日记数据文件
            {

                String cnt = FileOperator.ReadTxtFile(f.getAbsolutePath());
                String json = cnt.substring(cnt.indexOf("=")+1);
                importJson(json);
                //将附件移至标准目录
                String ss = subDir+"/files";
                LOG.D(ss);
                FileOperator.moveDirectory(ss, mDiaryPath);
                LOG.D(mDiaryPath);
            }else{
                //可能解压失败，或者不是本应用的压缩包，所以应提示失败
                LOG.D("未找到日记文件，可能解压失败，或者不是本应用的压缩包，所以应提示失败");
            }
        }
        String eval= "onImportFinish("+String.valueOf(mImportCount)+")";
        exeJavaScript(eval);
        FileOperator.delFolder(dumpZipPath);
    }

    //导入json中的日记内容
    private boolean importJson(String json)
    {
        LOG.D(json);
        int count = 0;
        try {
            JSONArray ds = new JSONArray(json);
            LOG.D(String.valueOf(ds.length()));
            for (int i=0; i<ds.length(); i++) {
                JSONObject d = (JSONObject) ds.get(i);
                Diary.save(d.toString(), Diary.MODE_IMPORT);
                count++;
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        mImportCount = count;
        return true;
    }
}
