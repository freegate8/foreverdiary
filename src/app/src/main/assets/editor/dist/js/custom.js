var Toast;
var apkurl;
var updateRes;
var LANGUAGE_CODE = ""; //标识语言
if(typeof Diary != 'undefined')
{
    LANGUAGE_CODE = Diary.getParam("LANGUAGE_CODE");
}

function GetDate() {
     /**
     * format=1表示获取年月日
     * format=0表示获取年月日时分秒
     * **/
     var now = new Date();
     var year = now.getFullYear();
     var month = (now.getMonth()+1);
     var date = now.getDate();
     
     month = (month+"").padStart(2,0);
     date = (date+"").padStart(2,0);
    
     _time = year+"-"+month+"-"+date;
    return _time;
}

function GetTime() {
     /**
     * format=1表示获取年月日
     * format=0表示获取年月日时分秒
     * **/
     var now = new Date();
     var hour = now.getHours();//得到小时
     var minu = now.getMinutes();//得到分钟
     var sec = now.getSeconds();//得到秒
     
     hour = (hour+"").padStart(2,0);
     minu = (minu+"").padStart(2,0);
     sec = (sec+"").padStart(2,0);
     
     _time = hour+":"+minu;
    return _time;
}

function GetDateTime() {
     /**
     * format=1表示获取年月日
     * format=0表示获取年月日时分秒
     * **/
     var now = new Date();
     var year = now.getFullYear();
     var month = (now.getMonth()+1);
     var date = now.getDate();
     var day = now.getDay();//得到周几
     var hour = now.getHours();//得到小时
     var minu = now.getMinutes();//得到分钟
     var sec = now.getSeconds();//得到秒
     
     month = (month+"").padStart(2,0);
     date = (date+"").padStart(2,0);
     hour = (hour+"").padStart(2,0);
     minu = (minu+"").padStart(2,0);
     sec = (sec+"").padStart(2,0);
    
     _time = year+month+date+hour+minu+sec
    return _time;
}

function GetNumHan(num)
{
    var day;
    switch(num)
     {
        case 1:
            day = "一";
            break;
        case 2:
            day = "二";
            break;
         case 3:
            day = "三";
            break;
          case 4:
            day = "四";
            break;
         case 5:
            day = "五";
            break;
         case 6:
            day = "六";
            break;
          case 7:
            day = "七";
            break;
    }
    return day;
}

function GetHumanDateTime() {
    lang = jQuery.i18n.normaliseLanguageCode({}); //获取浏览器的语言
    
     /**
     * format=1表示获取年月日
     * format=0表示获取年月日时分秒
     * **/
     var now = new Date();
     var year = now.getFullYear();
     var month = (now.getMonth()+1);
     var date = now.getDate();
     var day = now.getDay();//得到周几
     var hour = now.getHours();//得到小时
     var minu = now.getMinutes();//得到分钟
     var sec = now.getSeconds();//得到秒
     
     if(lang == "zh_CN")
     {
        switch(day)
        {
              case 0:
                day = "日";
                break;
              default:
                day = GetNumHan(day);
                break;
        }
        _time = month+"月"+date+"日 周"+day;
     }else{
        var options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        _time = now.toLocaleDateString("en-US", options);  
     } 
     
    //hour = (hour+"").padStart(2,0);
    return _time;
}

function getUrlParam(variable)
{
       var query = window.location.search.substring(1);
       var vars = query.split("&");
       for (var i=0;i<vars.length;i++) {
               var pair = vars[i].split("=");
               if(pair[0] == variable){return decodeURIComponent(pair[1]);}
       }
       return(null);
}

function logout()
{
    Diary.logout();
}

function isEmpty(exp)
{
	if(typeof(exp)!="undefined")
	{
		if(exp === null) return true;
		if(exp instanceof Array && exp.length==0) return true;
		if(typeof(exp)=="string" && exp.length==0)  return true;
	}else{
		return true;
	}
	return false;
}

function searchDiary()
{
    var curr = window.location.href;
    var key = $("#searchKey").val();
    //console.log("searchDiary:"+key);
    if(curr.indexOf("UI/index.html") != -1)
    {
        var filter = new Object();
        filter.key=key;
        listStr = Diary.list(JSON.stringify(filter), 0, count);
        list = JSON.parse(listStr);
        refreshList();
    }else{
        window.location.href="index.html?key="+key;
    }
    return false;
}

function checkUpdate()
{
   var versionCode = Diary.getParam("versionCode");
   $.get("https://www.xingecloud.com/diary/update", { ver: versionCode },
   function(data){
        var res = JSON.parse(data);
        updateRes = res;
        apkurl = res.apkurl;
        if(!isEmpty(apkurl))
        {
            $("#updateTip").html(res.desc);
            $("#updateTitle").html(res.title);
            showUpdateDlg();
        }
   });
   
    
}

function showUpdateDlg()
{
    $('#modal-update').modal({
            	     backdrop:"static",
            	     keyboard:false,
            	     show: true
            	});
}

function updateConfirm()
{
    //console.log("update:"+apkurl);
    window.location.href=apkurl;
}


function loadLanguages() {
    LANGUAGE_CODE = jQuery.i18n.normaliseLanguageCode({}); //获取浏览器的语言
    if(typeof Diary != 'undefined')
    {
        Diary.setParam("LANGUAGE_CODE", LANGUAGE_CODE);
    }
    //LANGUAGE_CODE = "en_US";
    jQuery.i18n.properties({
        name:'diary', 
        path:'../../dist/i18n/', 
        mode:'both',
        language: LANGUAGE_CODE, // 对应的语言
        cache: false,
        encoding: 'UTF-8',
        callback: function () { // 回调方法    
            try {
                $('[data-i18n-placeholder]').each(function () {
                    $(this).attr('placeholder', $.i18n.prop($(this).data('i18n-placeholder')));
                });
                $('[data-i18n-text]').each(function () {
                    //如果text里面还有html需要过滤掉
                    var html = $(this).html();
                    var reg = /<(.*)>/;
                    if (reg.test(html)) {
                        var htmlValue = reg.exec(html)[0];
                        $(this).html(htmlValue + $.i18n.prop($(this).data('i18n-text')));
                    }
                    else {
                        $(this).text($.i18n.prop($(this).data('i18n-text')));
                    }
                });
                $('[data-i18n-value]').each(function () {
                    $(this).val($.i18n.prop($(this).data('i18n-value')));
                });
            }
            catch(ex){ }
            
        }
    });
}

function checkAbout()
{
   var versionCode = Diary.getParam("versionCode");
   $.get("https://www.xingecloud.com/diary/api/about", { ver: versionCode },
   function(data){
        var res = JSON.parse(data);
        updateRes = res;
        cnt = res.about;
        if(!isEmpty(cnt))
        {
            $("#content").html(cnt);
        }
   });
   
    
}
