package com.liyin.foreverdiary;

public class StringUtil {

     public static String  left(String s, int len)
    {
        if(s.length()>=len) return s.substring(0, len);
        return s;
    }

    /**
     * 截取指定字符串中位于begin和end字符串之间的字符串
     * @param s 指定字符串
     * @param begin 开始标记
     * @param end 结束标记
     * @return 空字符串或
     */
    public static String mid(String s, String begin, String end)
    {
        String ret = "";
        int pos = s.indexOf(begin);
        if(pos == -1) return ret;

        int pos2 = s.indexOf(end);
        if(pos2 == -1)
        {
            ret = s.substring(pos+begin.length());
            return ret;
        }

        ret = s.substring(pos+begin.length(), pos2);
        return ret;
    }

}
