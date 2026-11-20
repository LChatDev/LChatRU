package su.lbs.lchat;

import android.content.*;
import android.net.Uri;
import android.webkit.CookieManager;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class NativeApi {
    volatile String base,csrf="";
    NativeApi(String b){base=b;CookieManager.getInstance().setAcceptCookie(true);}
    String cookie(){String c=CookieManager.getInstance().getCookie(base);return c==null?"":c;}
    JSONObject get(String action)throws Exception{JSONObject d=MobileApi.request(base,cookie(),action,null,null);if(d.has("csrf"))csrf=d.getString("csrf");return d;}
    JSONObject post(String action,JSONObject data)throws Exception{if(csrf.isEmpty())get("me");try{return MobileApi.request(base,cookie(),action,data,csrf);}catch(MobileApi.Error e){if(e.code!=403||!"Обновите страницу и повторите действие.".equals(e.getMessage()))throw e;get("me");return MobileApi.request(base,cookie(),action,data,csrf);}}
    JSONObject admin(String action,JSONObject data)throws Exception{if(csrf.isEmpty())get("me");return MobileApi.requestAt(base,cookie(),"admin.php",action,data,csrf);}
    static String enc(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(Exception e){return "";}}
    static JSONObject json(Object...values){JSONObject d=new JSONObject();try{for(int i=0;i<values.length;i+=2)d.put((String)values[i],values[i+1]);}catch(JSONException e){throw new IllegalArgumentException(e);}return d;}
    JSONObject upload(Context context,Uri uri,String endpoint,Map<String,String> fields,String forcedName,String forcedMime)throws Exception{
        if(!"upload.php".equals(endpoint)&&!"profile.php".equals(endpoint))throw new IOException("Invalid upload endpoint");
        ByteArrayOutputStream file=new ByteArrayOutputStream();try(InputStream in=context.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Не удалось открыть файл");byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){if(file.size()+n>8*1024*1024)throw new IOException("Файл превышает 8 МБ");file.write(buf,0,n);}}
        String name=forcedName;if(name==null){try(android.database.Cursor c=context.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}catch(Exception ignored){}}if(name==null)name="file.bin";name=name.replaceAll("[\\\"\r\n]","_");String mime=forcedMime==null?context.getContentResolver().getType(uri):forcedMime;if(mime==null)mime="application/octet-stream";
        String boundary="LChat"+UUID.randomUUID().toString().replace("-","");ByteArrayOutputStream body=new ByteArrayOutputStream();for(Map.Entry<String,String> field:fields.entrySet())body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+field.getKey()+"\"\r\n\r\n"+field.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+(endpoint.equals("profile.php")?"photo":"file")+"\"; filename=\""+name+"\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));body.write(file.toByteArray());body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));HttpURLConnection c=(HttpURLConnection)new URL(base+endpoint).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestMethod("POST");c.setRequestProperty("Cookie",cookie());c.setRequestProperty("X-CSRF-Token",csrf);c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);c.setDoOutput(true);c.setFixedLengthStreamingMode(body.size());
        try{try(OutputStream out=c.getOutputStream()){body.writeTo(out);}int code=c.getResponseCode();InputStream stream=code>=400?c.getErrorStream():c.getInputStream();ByteArrayOutputStream response=new ByteArrayOutputStream();try(InputStream in=stream){byte[] b=new byte[4096];int n;while(in!=null&&(n=in.read(b))!=-1){if(response.size()+n>1048576)throw new IOException("Слишком большой ответ");response.write(b,0,n);}}JSONObject result=new JSONObject(response.toString("UTF-8"));if(code>=400)throw new MobileApi.Error(code,result.optString("error","Ошибка загрузки"));return result;}finally{c.disconnect();}
    }
}
