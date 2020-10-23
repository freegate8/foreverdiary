package com.liyin.foreverdiary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class FileOperator {
    private static final int BYTESIZE = 1024;
    /**
     * 复制文件目录
     * @param srcDir 要复制的源目录 eg:/mnt/sdcard/DB
     * @param destDir 复制到的目标目录 eg:/mnt/sdcard/db/
     * @return
     */
    public static boolean copyDir(String srcDir, String destDir){
        File sourceDir = new File(srcDir);
        //判断文件目录是否存在
        if(!sourceDir.exists()){
            return false;
        }
        //判断是否是目录
        if (sourceDir.isDirectory()) {
            File[] fileList = sourceDir.listFiles();
            File targetDir = new File(destDir);
            //创建目标目录
            if(!targetDir.exists()){
                targetDir.mkdirs();
            }
            //遍历要复制该目录下的全部文件
            for(int i= 0;i<fileList.length;i++){
                if(fileList[i].isDirectory()){//如果如果是子目录进行递归
                    copyDir(fileList[i].getPath()+ "/",
                            destDir + fileList[i].getName() + "/");
                }else{//如果是文件则进行文件拷贝
                    copyFile(fileList[i].getPath(), destDir +fileList[i].getName());
                }
            }
            return true;
        }else {
            copyFileToDir(srcDir,destDir);
            return true;
        }
    }


    /**
     * 复制文件（非目录）
     * @param srcFile 要复制的源文件
     * @param destFile 复制到的目标文件
     * @return
     */
    private static boolean copyFile(String srcFile, String destFile){
        try{
            InputStream streamFrom = new FileInputStream(srcFile);
            OutputStream streamTo = new FileOutputStream(destFile);
            byte buffer[]=new byte[1024];
            int len;
            while ((len= streamFrom.read(buffer)) > 0){
                streamTo.write(buffer, 0, len);
            }
            streamFrom.close();
            streamTo.close();
            return true;
        } catch(Exception ex){
            return false;
        }
    }


    /**
     * 把文件拷贝到某一目录下
     * @param srcFile
     * @param destDir
     * @return
     */
    public static boolean copyFileToDir(String srcFile, String destDir){
        File fileDir = new File(destDir);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        String destFile = destDir +"/" + new File(srcFile).getName();
        try{
            InputStream streamFrom = new FileInputStream(srcFile);
            OutputStream streamTo = new FileOutputStream(destFile);
            byte buffer[]=new byte[1024];
            int len;
            while ((len= streamFrom.read(buffer)) > 0){
                streamTo.write(buffer, 0, len);
            }
            streamFrom.close();
            streamTo.close();
            return true;
        } catch(Exception ex){
            return false;
        }
    }


    /**
     * 移动文件目录到某一路径下
     * @param srcFile
     * @param destDir
     * @return
     */
    public static boolean moveFile(String srcFile, String destDir) {
        //复制后删除原目录
        if (copyDir(srcFile, destDir)) {
            deleteFile(new File(srcFile));
            return true;
        }
        return false;
    }

    /**
     * 删除单个文件
     *
     * @param fileName
     *            要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                //LOG.D("删除单个文件" + fileName + "成功！");
                return true;
            } else {
                //LOG.D("删除单个文件" + fileName + "失败！");
                return false;
            }
        } else {
            //LOG.D("删除单个文件失败：" + fileName + "不存在！");
            return false;
        }
    }

    /**
     * 删除文件，可以是文件或文件夹
     *
     * @param fileName
     *            要删除的文件名
     * @return 删除成功返回true，否则返回false
     */
    public static boolean delFolder(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            LOG.E("删除文件失败:" + fileName + "不存在！");
            return false;
        } else {
            if (file.isFile())
                return FileOperator.deleteFile(fileName);
            else
                return deleteDirectory(fileName);
        }
    }


    /**
     * 删除目录及目录下的文件
     *
     * @param dir
     *            要删除的目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    private static boolean deleteDirectory(String dir) {
        // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        if (!dir.endsWith(File.separator))
            dir = dir + File.separator;
        File dirFile = new File(dir);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            System.out.println("删除目录失败：" + dir + "不存在！");
            return false;
        }
        boolean flag = true;
        // 删除文件夹中的所有文件包括子目录
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // 删除子文件
            if (files[i].isFile()) {
                flag = FileOperator.deleteFile(files[i].getAbsolutePath());
                if (!flag)
                    break;
            }
            // 删除子目录
            else if (files[i].isDirectory()) {
                flag = deleteDirectory(files[i]
                        .getAbsolutePath());
                if (!flag)
                    break;
            }
        }
        if (!flag) {
            System.out.println("删除目录失败！");
            return false;
        }
        // 删除当前目录
        if (dirFile.delete()) {
            //System.out.println("删除目录" + dir + "成功！");
            return true;
        } else {
            return false;
        }
    }


    /**
     * 删除文件（包括目录）
     * @param delFile
     */
    public static void deleteFile(File delFile) {
        //如果是目录递归删除
        if (delFile.isDirectory()) {
            File[] files = delFile.listFiles();
            for (File file : files) {
                deleteFile(file);
            }
        }else{
            delFile.delete();
        }
        //如果不执行下面这句，目录下所有文件都删除了，但是还剩下子目录空文件夹
        delFile.delete();
    }

    /**
     * 将文件流保存在项目WEB-INF/temp目录下，并且返回这个文件；
     * @param is              待转化的文件流
     * @param fileName        临时文件名
     * @return
     * @throws IOException
     */
    public static File dumpStreamToFile(InputStream is, String fileName){
        File temp = new File(fileName);

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try{
            bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(new FileOutputStream(temp));                            //把文件流转为文件，保存在临时目录
            int len = 0;
            byte[] buf = new byte[10*BYTESIZE];                                                    //缓冲区
            while((len=bis.read(buf)) != -1){
                bos.write(buf, 0, len);
            }
            bos.flush();
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            try {
                if(bos!=null) bos.close();
                if(bis!=null) bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return temp;
    }


    /*
    检测所给路径是否存在，不存在则路径依次创建相应目录
     */
    public static void createFolder(String path)
    {
        try {
            //判断文件夹是否存在，不存在，则创建之
            String[] paths = path.split("/");
            for(int i=0 ; i<=paths.length; i++)
            {
                String tmpPath = "";
                for(int j=0; j<i;j++)
                {
                    tmpPath += paths[j]+"/";
                }
                File ppath = new File(tmpPath);
                if(!ppath.exists())
                {
                    ppath.mkdir();
                }
            }
        }
        catch (Exception e) {
            LOG.E("createFolder Error!");
            e.printStackTrace();

        }
    }


    public static String ReadTxtFile(String strFilePath)
    {
        String path = strFilePath;
        String content = ""; //文件内容字符串
        //打开文件
        File file = new File(path);
        //如果path是传递过来的参数，可以做一个非目录的判断
        if (file.isDirectory())
        {
            LOG.D("TestFile", "The File doesn't not exist.");
        }
        else
        {
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null)
                {
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;
                    //分行读取
                    while (( line = buffreader.readLine()) != null) {
                        content += line + "\n";
                    }
                    instream.close();
                }
            }
            catch (java.io.FileNotFoundException e)
            {
                LOG.D("TestFile", "The File doesn't not exist.");
            }
            catch (IOException e)
            {
                LOG.D("TestFile", e.getMessage());
            }
        }
        return content;
    }


    /**
     * 移动目录
     * @param srcDirName     源目录完整路径
     * @param destDirName    目的目录完整路径
     * @return 目录移动成功返回true，否则返回false
     */
    public  static boolean moveDirectory(String srcDirName, String destDirName) {

        File srcDir = new File(srcDirName);
        if(!srcDir.exists() || !srcDir.isDirectory())
            return false;

        File destDir = new File(destDirName);
        if(!destDir.exists())
            destDir.mkdirs();

        /**
         * 如果是文件则移动，否则递归移动文件夹。删除最终的空源文件夹
         * 注意移动文件夹时保持文件夹的树状结构
         */
        File[] sourceFiles = srcDir.listFiles();
        for (File sourceFile : sourceFiles) {
            if (sourceFile.isFile())
                moveFile(sourceFile.getAbsolutePath(), destDir.getAbsolutePath());
            else if (sourceFile.isDirectory())
                moveDirectory(sourceFile.getAbsolutePath(),
                        destDir.getAbsolutePath() + File.separator + sourceFile.getName());
            else
                ;
        }
        return srcDir.delete();
    }
}
