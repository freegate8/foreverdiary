package com.liyin.foreverdiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.view.KeyEvent.KEYCODE_BACK;

/**
 * 永恒日记 Forever Diary
 * @author liufei
 */
public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private WebView mWebView;


    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //RequestCode
    private static final int TAKE_PHOTO_REQUEST_CODE = 1;
    public final int CODE_SELECT_IMAGE = 2;
    public final int REQUEST_CODE_FILE_CHOOSER = 3;
    public final int REQUEST_CODE_TAKE_AUDIO = 4;
    public final int REQUEST_EXTERNAL_STORAGE = 5;

    private  String mLogID;
    private Context mCtx = this;
    private long lastBackTime = 0;
    private String mCameraFilePath;
    ValueCallback<Uri[]> mUploadCallBackAboveL;
    ValueCallback<Uri> mUploadCallBack;

    public static final String SD_APP_DIR_NAME = "ForeverDiary"; //存储程序在外部SD卡上的根目录的名字
    public static final String PHOTO_DIR_NAME = "photo";    //存储照片在根目录下的文件夹名字
    public static final String VOICE_DIR_NAME = "voice";    //存储音频在根目录下的文件夹名字
    public static final String VIDEO_DIR_NAME = "video";    //存储视频在根目录下的文件夹名字

    private String mVoicePath;             //用于存储录音的最终目录，即根目录 / 录音的文件夹 / 录音
    private String mVoiceName;             //保存的录音的名字
    private File mVoiceFile;               //录音文件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ForeverDiaryApp app = ForeverDiaryApp.getInstance();
        int port = app.getHttpServerPort();
        InnerHttpServer mHttpServer = new InnerHttpServer(port);

        mWebView = findViewById(R.id.webview);
        ForeverDiaryApp.setWebView(mWebView);
        app.setMainActivity(this);

        //自适应屏幕
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        //mWebView.getSettings().setLoadWithOverviewMode(true); //缩放至屏幕的大小

        mWebView.getSettings().setAllowFileAccess(true);   //可访问文件
        mWebView.getSettings().setJavaScriptEnabled(true); //可执行Javascript脚本
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.getSettings().setAppCacheEnabled(false);  //禁止缓存数据
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setDefaultTextEncodingName("UTF-8");
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setSupportZoom(true);      //设定支持缩放
        mWebView.getSettings().setUseWideViewPort(true);  //支持viewport属性
        mWebView.setBackgroundColor(0xffffffff);
        //mWebView.getBackground().setAlpha(0);


        mWebView.addJavascriptInterface(new JsInterface(this), "Diary");

//        mWebView.getSettings().supportMultipleWindows();
//        mWebView.getSettings().setAllowContentAccess(true);

//        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
//        mWebView.getSettings().setLoadsImagesAutomatically(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //LOG.D(url);
                if(ForeverDiaryApp.getInstance().isLogout())
                {
                    url = "./lock.html";
                }
                //view.loadUrl(url);
                return super.shouldOverrideUrlLoading(view, url);
            }

        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {

            }
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                mUploadCallBack = valueCallback;
                showFileChooser();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                mUploadCallBack = valueCallback;
                showFileChooser();
            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
                mUploadCallBack = valueCallback;
                showFileChooser();
            }

            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                mUploadCallBackAboveL = filePathCallback;
                showFileChooser();
                return true;
            }
            /**
             * 打开选择文件/相机
             */
            private void showFileChooser() {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                Intent chooser = new Intent(Intent.ACTION_CHOOSER);
                chooser.putExtra(Intent.EXTRA_TITLE, "File Chooser");
                chooser.putExtra(Intent.EXTRA_INTENT, intent);
                startActivityForResult(chooser, REQUEST_CODE_FILE_CHOOSER);
            }


            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                Log.e(TAG, "onJsAlert " + message);
                //Toast.makeText(mCtx, "onJsAlert "+message, Toast.LENGTH_SHORT).show();
                result.cancel();
                return true;
            }



            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e(TAG, "onConsoleMessage [" + consoleMessage.messageLevel() + "] " + consoleMessage.message() + "(" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + ")");
                return super.onConsoleMessage(consoleMessage);
            }
        });
        String indexUrl = "file:///android_asset/editor/pages/UI/";
        String pwd = ForeverDiaryApp.getParam("enterPassword");
        if(pwd.length()>0)
        {
            indexUrl += "lock.html";
        }else {
            indexUrl += "index.html";
        }
        LOG.D("index:"+indexUrl);
        mWebView.loadUrl(indexUrl);

        //handle downloading
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimeType,
                                        long contentLength) {
                //Toast.makeText(getApplicationContext(), "下载日记中……", Toast.LENGTH_SHORT).show();
                /*
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading File...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                                url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                 */
                try{
                    if(url.indexOf(".apk")!= -1)
                    {
                        LOG.D(url);
                        Uri myUrl = Uri.parse(url);
                        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(myUrl,"application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setAction(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT >= 24) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setDataAndType(myUrl, "application/vnd.android.package-archive");
                        } else {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setDataAndType(myUrl, "application/vnd.android.package-archive");
                        }

                        startActivity( intent );
                    }else {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        });

        preparePermission();
    }

    public boolean preparePermission()
    {
        int auth1 = ContextCompat.checkSelfPermission(mCtx, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int auth2 = ContextCompat.checkSelfPermission(mCtx, Manifest.permission.CAMERA);
        int auth3 = ContextCompat.checkSelfPermission(mCtx, Manifest.permission.RECORD_AUDIO);

        if(auth1  == auth2  && auth2 == auth3 && auth1 == PackageManager.PERMISSION_GRANTED )
        {
            return true;
        }else{
            ActivityCompat.requestPermissions((Activity) this,  new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, TAKE_PHOTO_REQUEST_CODE);
        }

        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KEYCODE_BACK) {

            if(mWebView.canGoBack()) {

                mWebView.goBack();
            } else {
                long curTime = SystemClock.uptimeMillis();
                if(lastBackTime == 0 || curTime - lastBackTime > 2000) {
                    Toast.makeText(mCtx, "再按一次[返回]键退出程序", Toast.LENGTH_SHORT).show();
                    lastBackTime = curTime;
                } else {
                    lastBackTime = 0;
                    finish();
                }
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void setLogID(String logid) {
        mLogID = logid;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CODE_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    selectPic(resultCode, data);
                }
                break;
        }

        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (result == null && !TextUtils.isEmpty(mCameraFilePath)) {
                // 看是否从相机返回
                File cameraFile = new File(mCameraFilePath);
                if (cameraFile.exists()) {
                    result = Uri.fromFile(cameraFile);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
                }
            }
            if (result != null) {
                LOG.D("result:"+result.toString());
                String uriString = result.toString();
                if(uriString.startsWith("content://"))
                {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        mWebView.evaluateJavascript("javascript:onSelectFile(\"" + uriString + "\");", null);
                    } else {
                        mWebView.loadUrl("javascript:onSelectFile(\"" + uriString + "\");");
                    }
                    return;
                }
                String path = FileUtils.getPath(this, result);
                if (!TextUtils.isEmpty(path)) {
                    File f = new File(path);
                    if (f.exists() && f.isFile()) {
                        Uri newUri = Uri.fromFile(f);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if (mUploadCallBackAboveL != null) {
                                if (newUri != null) {
                                    mUploadCallBackAboveL.onReceiveValue(new Uri[]{newUri});
                                    mUploadCallBackAboveL = null;

                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                        mWebView.evaluateJavascript("javascript:onSelectFile(\"" + newUri + "\");", null);
                                    } else {
                                        mWebView.loadUrl("javascript:onSelectFile(\"" + newUri + "\");");
                                    }
                                    return;
                                }
                            }
                        } else if (mUploadCallBack != null) {
                            if (newUri != null) {
                                mUploadCallBack.onReceiveValue(newUri);
                                mUploadCallBack = null;

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                    mWebView.evaluateJavascript("javascript:onSelectFile(\"" + newUri + "\");", null);
                                } else {
                                    mWebView.loadUrl("javascript:onSelectFile(\"" + newUri + "\");");
                                }
                                return;
                            }
                        }
                    }
                }
            }
            clearUploadMessage();
            return;
        }else if(REQUEST_CODE_TAKE_AUDIO == requestCode )
        {
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();

            if (result != null) {
                String uriString = result.toString();
                LOG.D("result:"+result.toString());

                String path = FileUtils.getPath(this, result);
                if (!TextUtils.isEmpty(path)) {
                    LOG.D("path:"+path);
                    String diaryPath = ForeverDiaryApp.getLogPath(mLogID);
                    String fileName = path.substring(path.lastIndexOf("/"));
                    fileName = diaryPath+fileName;
                    LOG.D(fileName);
                    Utils.copyFile(this, path, fileName);

                    final int version = Build.VERSION.SDK_INT;
                    if (version < 18) {
                        mWebView.loadUrl("javascript:onAudioSelected('\"+fileName+\"')");
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        mWebView.evaluateJavascript("javascript:onAudioSelected('"+fileName+"')", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                LOG.D("..............onReceiveValue:"+value);
                            }
                        });
                    }
                }
            }
        }


    }



    /**
     *生成一个选择本地图库或是相册的选择
     **/
    protected Intent createDefaultOpenableIntent() {

        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("image/*");

        Intent chooser = createChooserIntent(createCameraIntent());
        chooser.putExtra(Intent.EXTRA_INTENT, i);
        return chooser;
    }

    private Intent createChooserIntent(Intent... intents) {
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        chooser.putExtra(Intent.EXTRA_TITLE, "选择图片来源");
        return chooser;
    }

     private Intent createCameraIntent() {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //20161230  改变相机拍摄的图片的路径
            File externalDataDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File cameraDataDir = new File(externalDataDir.getAbsolutePath()
                    + File.separator + "ForeverDiary-Photo");
            cameraDataDir.mkdirs();
            mCameraFilePath = cameraDataDir.getAbsolutePath() + File.separator
                    + System.currentTimeMillis() + ".jpg";
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(mCameraFilePath)));

            return cameraIntent;
        }

    public void goPhotoAlbum(String logid) {
        mLogID = logid;
        if (ContextCompat.checkSelfPermission(mCtx, Manifest.permission.CAMERA)  != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions((Activity) this,  new String[]{Manifest.permission.CAMERA}, TAKE_PHOTO_REQUEST_CODE);
        }else {
            Intent intent = createDefaultOpenableIntent();
            startActivityForResult(intent, CODE_SELECT_IMAGE);
        }
    }

    public void selectAudio()
    {
        if (ContextCompat.checkSelfPermission(mCtx, Manifest.permission.RECORD_AUDIO)  != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions((Activity) this,  new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_TAKE_AUDIO);
        }else {
            Intent intent = createAudioIntent();
            startActivityForResult(intent, REQUEST_CODE_TAKE_AUDIO);
        }
    }



    /**
     * webview没有选择文件也要传null，防止下次无法执行
     */
    private void clearUploadMessage() {
        if (mUploadCallBackAboveL != null) {
            mUploadCallBackAboveL.onReceiveValue(null);
            mUploadCallBackAboveL = null;
        }
        if (mUploadCallBack != null) {
            mUploadCallBack.onReceiveValue(null);
            mUploadCallBack = null;
        }
    }
    private void selectPic(int resultCode, Intent intent) {
        String picturePath = "";
        Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
        if (result == null && !TextUtils.isEmpty(mCameraFilePath)) {
            // 看是否从相机返回
            File cameraFile = new File(mCameraFilePath);
            if (cameraFile.exists()) {
                result = Uri.fromFile(cameraFile);
                picturePath = FileUtils.getPath(this, result);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
            }
        }

        if(intent != null) {
            Uri selectImageUri = intent.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectImageUri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
        }
        LOG.D(picturePath);
        String diaryPath = ForeverDiaryApp.getLogPath(mLogID);
        String picName = picturePath.substring(picturePath.lastIndexOf("/"));
        picName = diaryPath+picName;
        LOG.D(picName);

        Utils.copyFile(this, picturePath, picName);

        final int version = Build.VERSION.SDK_INT;
        if (version < 18) {
            mWebView.loadUrl("javascript:onSelectImg('\"+picName+\"')");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript("javascript:onSelectImg('"+picName+"')", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    LOG.D("..............Copyed img:"+value);
                }
            });
        }

    }

    /**
     *
     * @param requestCode
     * @param permissions 请求的权限
     * @param grantResults 请求权限返回的结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


    }



    /**
     *生成一个选择本地图库或是相册的选择
     **/
    protected Intent createAudioIntent() {

        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType("audio/*");

        Intent chooser = createVoiceChooserIntent(createVoiceRecordIntent());
        chooser.putExtra(Intent.EXTRA_INTENT, i);
        return chooser;
    }

    private Intent createVoiceChooserIntent(Intent... intents) {
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        chooser.putExtra(Intent.EXTRA_TITLE, "选择音频来源");
        return chooser;
    }

    /**
     *生成一个选择本地图库或是相册的选择
     **/
    protected Intent createVoiceRecordIntent() {
        Intent intent = new Intent();
        intent.setAction(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        createVoiceFile();
        Log.d(TAG, "创建录音文件");
        //添加权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Log.d(TAG, "启动系统录音机，开始录音...");
        return intent;
    }

    /**
     * 获取日期并格式化
     * 如：2017_10_20 周三 上午 11：20：35
     *
     * @return 格式化好的日期字符串
     */
    private String getMyTime() {
        //存储格式化后的时间
        String time;
        //存储上午下午
        String ampTime = "";
        //判断上午下午，am上午，值为 0 ； pm下午，值为 1
        int apm = Calendar.getInstance().get(Calendar.AM_PM);
        if (apm == 0) {
            ampTime = "上午";
        } else {
            ampTime = "下午";
        }
        //设置格式化格式
        SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd E " + ampTime + " kk:mm:ss");
        time = format.format(new Date());

        return time;
    }

    /**
     * 创建音频目录
     */
    private void createVoiceFile() {
        mVoiceName = getMyTime() + ".mp3";
        Log.d(TAG, "录音文件名称：" + mVoiceName);
        mVoiceFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + SD_APP_DIR_NAME + "/" + VOICE_DIR_NAME + "/", mVoiceName);
        mVoicePath = mVoiceFile.getAbsolutePath();
        mVoiceFile.getParentFile().mkdirs();
        Log.d(TAG, "按设置的目录层级创建音频文件，路径：" + mVoicePath);
        mVoiceFile.setWritable(true);
    }
}
