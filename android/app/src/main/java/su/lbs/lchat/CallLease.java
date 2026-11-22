package su.lbs.lchat;

import android.content.*;
import org.json.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Atomic cross-process journal. SharedPreferences caches must not own call admission. */
final class CallLease {
    private interface Locked<T>{T run(File file)throws Exception;}
    private static synchronized <T>T locked(Context c,Locked<T> task)throws Exception{
        File lock=new File(c.getFilesDir(),"call-v3.lock"),file=new File(c.getFilesDir(),"call-v3.json");
        try(RandomAccessFile access=new RandomAccessFile(lock,"rw");FileChannel channel=access.getChannel();FileLock ignored=channel.lock()){return task.run(file);}
    }
    private static JSONObject read(File file)throws Exception{if(!file.exists())return new JSONObject();if(file.length()>4096)throw new IOException("Invalid call journal");return new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));}
    static void remember(Context c,String base,String token,String id)throws Exception{
        locked(c,file->{byte[] data=new JSONObject().put("base",base).put("token",token).put("id",id).toString().getBytes(StandardCharsets.UTF_8);File temporary=new File(file.getPath()+".tmp");try(FileOutputStream out=new FileOutputStream(temporary)){out.write(data);out.getFD().sync();}Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);return null;});
    }
    static void clear(Context c,String id)throws Exception{locked(c,file->{if(id.equals(read(file).optString("id")))Files.deleteIfExists(file.toPath());return null;});}
    static void recover(Context c,String base,String cookie)throws Exception{
        JSONObject saved=locked(c,CallLease::read);
        if(saved.optString("id").isEmpty()){
            SharedPreferences old=c.getSharedPreferences("call-lease",Context.MODE_PRIVATE);
            saved=new JSONObject().put("id",old.getString("id","")).put("base",old.getString("base","")).put("token",old.getString("token",""));
        }
        String id=saved.optString("id");if(id.isEmpty()||!base.equals(saved.optString("base")))return;
        JSONObject me=MobileApi.requestSessionless(base,cookie,"me",null,null);
        try{MobileApi.requestSessionless(base,cookie,"mobile.release",new JSONObject().put("id",id).put("token",saved.getString("token")),me.getString("csrf"));}
        catch(MobileApi.Error e){if(e.code!=403&&e.code!=404&&e.code!=410)throw e;}
        clear(c,id);
        SharedPreferences old=c.getSharedPreferences("call-lease",Context.MODE_PRIVATE);if(id.equals(old.getString("id","")))old.edit().clear().commit();
    }
}
