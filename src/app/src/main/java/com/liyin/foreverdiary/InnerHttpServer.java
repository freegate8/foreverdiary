package com.liyin.foreverdiary;

/*
 * #%L
 * NanoHttpd-Samples
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

/**
 * 永恒日记 Forever Diary
 * @author liufei
 */


public class InnerHttpServer extends NanoHTTPD {

    /**
     * logger to log to.
     */
    static SimpleDateFormat sGmtFrmt;

    public InnerHttpServer(int port ) {
        super(port);
        sGmtFrmt = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        try {
            start();
        }catch (IOException e)
        {

        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String url = session.getUri();
            if (url == null) {
                com.liyin.foreverdiary.LOG.D("url is null");
                return super.serve(session);
            }
            String range = null;

            Map<String, String> headers = session.getHeaders();
            for (String key : headers.keySet()) {
                // Log.e(TAG, key + ":" + headers.get(key) + ", val:" + headers.get(key));
                if ("range".equals(key)) {
                    range = headers.get(key); // 读取range header，包含要返回的媒体流位置等信息。
                }
            }

            LOG.log(Level.INFO,"HttpServer URI:"+url);
            Map<String, List<String>> query = decodeParameters(session.getQueryParameterString());
            String filepath = url; // 根据url获取文件路径
            if(url.equals("/local") && query.get("action").contains("status"))
            {
                return replyStatus(session);
            }

            File file = new File(filepath);
            if (file != null && file.exists()) {

                String mimeType = getMimeType(filepath); //根据文件名返回mimeType： image/jpg, video/mp4, etc

                Response res = null;
                if (range == null) {
                    // 如果range为空，返回该文件的所有媒体流
                    InputStream is = new FileInputStream(file);
                    res = Response.newChunkedResponse(Status.OK, mimeType, is);
                    res.addHeader("Content-Length", String.valueOf(file.length()));
                } else {
                    // 根据range参数，返回制定位置的媒体流
                    res = getPartialResponse(file, range, mimeType);
                }

                Calendar cd = Calendar.getInstance();
                cd.setTimeZone(TimeZone.getTimeZone("GMT"));
                cd.setTimeInMillis(file.lastModified());
                res.addHeader("Last-Modified", sGmtFrmt.format(cd.getTime()));

                return res;
            } else {
                LOG.log(Level.WARNING, "file is null");
            }

            return super.serve(session);

        } catch (Exception e) {
            LOG.log(Level.WARNING,"HttpServer serve:", e);
        } finally {
            LOG.log(Level.WARNING,"before return in serve");
        }
        return super.serve(session);
    }


    private Response replyStatus(IHTTPSession session)
    {
        StringBuilder sb = new StringBuilder();
        try {
            JSONObject jsonObject = new JSONObject();
            String tplVerLocal = "vvv";
            jsonObject.put("tplver", tplVerLocal);
            sb.append(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newFixedLengthResponse(sb.toString());
    }

    // 根据range中定义的范围返回媒体流
    private Response getPartialResponse(File file, String rangeHeader, String mimeType) throws IOException {
        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        long fileLength = file.length();
        long start, end;
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - 1
                    - Long.parseLong(rangeValue.substring("-".length()));
        } else {
            String[] range = rangeValue.split("-");
            start = Long.parseLong(range[0]);
            end = range.length > 1 ? Long.parseLong(range[1])
                    : fileLength - 1;
        }
        if (end > fileLength - 1) {
            end = fileLength - 1;
        }
// 解析range中的start，end位置

        InputStream fileInputStream;
        if (start <= end) {
            long contentLength = end - start + 1;
            fileInputStream = new FileInputStream(file);
            fileInputStream.skip(start); //将媒体流跳转到start处，然后返回

            Response response = Response.newChunkedResponse(Status.PARTIAL_CONTENT, mimeType, fileInputStream);
            response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.addHeader("Content-Length", contentLength + "");
            return response;
        } else {
            return Response.newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, MIME_HTML, rangeHeader);
        }
    }

    String getFilePath(String url)
    {
        LOG.log(Level.WARNING, "url22:"+url);
        String fullpath = CustomApp.getFilesPath()+url;
        LOG.log(Level.WARNING, "url:"+fullpath);
        return fullpath;
    }

    String getMimeType(String filepath)
    {
        LOG.log(Level.WARNING, "filepath:"+filepath);
        String mime = "";
        String ext = "";
        int pos = filepath.lastIndexOf(".");
        if(pos != -1)
        {
            ext =filepath.substring(pos+1);
        }
        switch(ext)
        {
            case "jpg":
                mime = "image/jpeg";
                break;
            case "png":
                mime = "image/png";
                break;
            case "apk":
                mime = "application/vnd.android.package-archive";
                break;
            case "db":
                mime = "application/octet-stream";
                break;
            case "zip":
                mime = "application/zip";
                break;
            default:
                mime = MIME_HTML;
        }
        return mime;
    }
}
