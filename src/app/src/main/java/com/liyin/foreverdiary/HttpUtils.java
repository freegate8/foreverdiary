package com.liyin.foreverdiary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Random;

/**
 * 永恒日记 Forever Diary
 * @author liufei
 */

public class HttpUtils {

    private static final String TAG = HttpUtils.class.getSimpleName();

    public static String getUrl(String path)
    {
        return getUrl(path, 15000);
    }

    public static String getUrl(String path, int timeout) {
        LOG.D("#############url = "+path);

        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        Random rand = new Random(date.getTime());
        String r = String.valueOf(rand.nextLong());
        String cnt = "";
        try {
            // 1.声明访问的路径， url 网络资源 http ftp rtsp
            URL url = new URL(path);
            // 2.通过路径得到一个连接 http的连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestProperty("H-Time", r);
            //LOG.D("#############H-Time = "+r);
            //LOG.D("#############H-Token = "+md5);

            int code = conn.getResponseCode();
            if (code == 200) {
                // 4.利用链接成功的 conn 得到输入流
                InputStream is = conn.getInputStream();// png的图片
                cnt = Inputstr2Str_Reader(is, null);
                LOG.D("#############html = "+ StringUtil.left(cnt, 50));
            } else {
                cnt = "";
                LOG.D("#############html error code= "+code);
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return  cnt;
    }

    /**
     * 利用BufferedReader实现Inputstream转换成String <功能详细描述>
     *
     * @param in
     * @return String
     */
    public static String Inputstr2Str_Reader(InputStream in, String encode)
    {

        String str = "";
        try
        {
            if (encode == null || encode.equals(""))
            {
                // 默认以utf-8形式
                encode = "utf-8";
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, encode));
            StringBuffer sb = new StringBuffer();

            while ((str = reader.readLine()) != null)
            {
                sb.append(str).append("\n");
            }
            return sb.toString();
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return str;
    }

    public static String toURLEncoded(String paramString) {
        if (paramString == null || paramString.equals("")) {
            return "";
        }

        try
        {
            String str = new String(paramString.getBytes(), "UTF-8");
            str = URLEncoder.encode(str, "UTF-8");
            return str;
        }
        catch (Exception localException)
        {
            return "";
        }
    }



    public static Bitmap getRemoteImage(final String url) {
        try {
            URL picUrl = new URL(url);
            Bitmap pngBM = BitmapFactory.decodeStream(picUrl.openStream());
            return pngBM;
        } catch (IOException e) {}
        return null;
    }


    /**
     * 传统Post方式，Post数据
     * @param params 填写的url的参数
     * @param encode 字节编码
     * @return
     */
    public static String sendPostMessage(String postUrl, Map<String, String> params, String encode){
        StringBuffer buffer = new StringBuffer();
        try {//把请求的主体写入正文！！
            if(params != null&&!params.isEmpty()){
                //迭代器
                //Map.Entry 是Map中的一个接口，他的用途是表示一个映射项（里面有Key和Value）
                for(Map.Entry<String, String> entry : params.entrySet()){
                    buffer.append(entry.getKey()).append("=").
                            append(URLEncoder.encode(entry.getValue(),encode)).
                            append("&");
                }
            }
//            System.out.println(buffer.toString());
            //删除最后一个字符&，多了一个;主体设置完毕
            buffer.deleteCharAt(buffer.length()-1);
            byte[] mydata = buffer.toString().getBytes();
            URL url = new URL(postUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setDoInput(true);//表示从服务器获取数据
            connection.setDoOutput(true);//表示向服务器写数据

            connection.setRequestMethod("POST");
            //是否使用缓存
            connection.setUseCaches(false);
            //表示设置请求体的类型是文本类型
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length", String.valueOf(mydata.length));
            connection.connect();   //连接，不写也可以。。？？有待了解

            //获得输出流，向服务器输出数据
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(mydata,0,mydata.length);
            //获得服务器响应的结果和状态码
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                return changeInputeStream(connection.getInputStream(),encode);

            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return "";
    }


    /**
     * 将一个输入流转换成字符串
     * @param inputStream
     * @param encode
     * @return
     */
    private static String changeInputeStream(InputStream inputStream,String encode) {
        //通常叫做内存流，写在内存中的
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        String result = "";
        if(inputStream != null){
            try {
                while((len = inputStream.read(data))!=-1){
                    data.toString();

                    outputStream.write(data, 0, len);
                }
                //result是在服务器端设置的doPost函数中的
                result = new String(outputStream.toByteArray(),encode);
                outputStream.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return result;
    }


    /**
     * 把Json数据Post到服务器端
     * @param postUrl 填写的url的参数
     * @param json json格式的字符串
     * @return
     */
    public static String sendPostMessageJson(String postUrl, String json){
        StringBuffer buffer = new StringBuffer();
        try {//把请求的主体写入正文！！

           buffer.append(json);
            byte[] mydata = buffer.toString().getBytes();
            URL url = new URL(postUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setDoInput(true);//表示从服务器获取数据
            connection.setDoOutput(true);//表示向服务器写数据

            connection.setRequestMethod("POST");
            //是否使用缓存
            connection.setUseCaches(false);
            //表示设置请求体的类型是文本类型
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setRequestProperty("Content-Length", String.valueOf(mydata.length));
            connection.connect();   //连接，不写也可以。。？？有待了解

            //获得输出流，向服务器输出数据
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(mydata,0,mydata.length);
            //获得服务器响应的结果和状态码
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                return changeInputeStream(connection.getInputStream(),"utf-8");

            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return "";
    }
}
