package su.lbs.lchat;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class MobileApi {
    static final class Error extends IOException {final int code;Error(int code,String message){super(message);this.code=code;}}
    static JSONObject request(String base,String cookie,String action,JSONObject data,String csrf)throws Exception{
        return requestAt(base,cookie,"api.php",action,data,csrf);
    }
    static JSONObject requestAt(String base,String cookie,String file,String action,JSONObject data,String csrf)throws Exception{
        return requestAt(base,cookie,file,action,data,csrf,true);
    }
    static JSONObject requestSessionless(String base,String cookie,String action,JSONObject data,String csrf)throws Exception{
        return requestAt(base,cookie,"api.php",action,data,csrf,false);
    }
    private static JSONObject requestAt(String base,String cookie,String file,String action,JSONObject data,String csrf,boolean updateCookies)throws Exception{
        if(!"api.php".equals(file)&&!"admin.php".equals(file))throw new IOException("Invalid endpoint");
        URI origin=URI.create(base);if(!"https".equals(origin.getScheme())||origin.getHost()==null||origin.getUserInfo()!=null)throw new IOException("Нужен HTTPS-адрес");
        HttpURLConnection c=(HttpURLConnection)new URL(base+file+"?action="+action).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(120000);c.setRequestProperty("Cookie",cookie==null?"":cookie);c.setRequestProperty("Cache-Control","no-store");
        try{if(data!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("X-CSRF-Token",csrf==null?"":csrf);byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int code=c.getResponseCode();if(updateCookies){java.util.List<String> sessionCookies=c.getHeaderFields().get("Set-Cookie");if(sessionCookies==null)sessionCookies=c.getHeaderFields().get("set-cookie");if(sessionCookies!=null){android.webkit.CookieManager jar=android.webkit.CookieManager.getInstance();for(String value:sessionCookies)jar.setCookie(base,value);jar.flush();}}InputStream stream=code>=400?c.getErrorStream():c.getInputStream();if(stream==null)throw new IOException("Нет ответа сервера");ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=stream){byte[] buf=new byte[4096];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>64*1024*1024)throw new IOException("Слишком большой ответ");out.write(buf,0,n);}}
            JSONObject result=new JSONObject(out.toString("UTF-8"));if(code<200||code>=300)throw new Error(code,result.optString("error","Ошибка сервера"));return result;
        }finally{c.disconnect();}
    }
}
