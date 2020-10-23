<?php
header("Access-Control-Allow-Origin: *");
$re = array();

$verCode=10000;
$verName= "V1.0.0";
$desc= "请下载解压后安装，<br>升级说明：1.解决日志导出问题；<br>2.解决行高问题；<br>请问您是否现在下载最新版进行安装？";

if(intval($_REQUEST['ver']) < $verCode)
{
    $re['oldver'] = $_REQUEST['ver'];
    $re['ver']= $verCode;
    $re['verName'] = $verName;
    $re['title']= "新版提示：".$re['verName'];
    $re['apkurl']= "http://www.xingecloud.com/diary/V".+$re['ver'].".zip";
    $re['desc'] = $desc;
}
echo json_encode($re);
?>