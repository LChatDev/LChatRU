package su.lbs.lchat;

import android.content.*;
import org.json.*;

/** Stores participant identifiers only, never the session cookie or TURN secret. */
final class CallLease {
    static void remember(Context c,String base,String token,String id){c.getSharedPreferences("call-lease",Context.MODE_PRIVATE).edit().putString("base",base).putString("token",token).putString("id",id).commit();}
    static void clear(Context c,String id){SharedPreferences p=c.getSharedPreferences("call-lease",Context.MODE_PRIVATE);if(id!=null&&id.equals(p.getString("id","")))p.edit().clear().commit();}
    static void recover(Context c,String base,String cookie)throws Exception{SharedPreferences p=c.getSharedPreferences("call-lease",Context.MODE_PRIVATE);String id=p.getString("id",""),token=p.getString("token","");if(id.isEmpty()||!base.equals(p.getString("base","")))return;
        JSONObject me=MobileApi.requestSessionless(base,cookie,"me",null,null);try{MobileApi.requestSessionless(base,cookie,"mobile.release",new JSONObject().put("id",id).put("token",token),me.getString("csrf"));clear(c,id);}catch(MobileApi.Error e){if(e.code==403||e.code==404||e.code==410)clear(c,id);else throw e;}}
}
