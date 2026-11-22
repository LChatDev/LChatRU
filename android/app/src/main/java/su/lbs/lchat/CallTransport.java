package su.lbs.lchat;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** HTTP has its own threads. It never executes on the RTC or Android UI thread. */
final class CallTransport {
    static final class Failure extends IOException {
        final int status;
        Failure(int status,String message){super(message);this.status=status;}
        boolean terminal(){return status==401||status==403||status==404||status==410;}
    }
    interface Reply {void done(JSONObject value,Exception error);}
    final String base,cookie,endpoint,token;
    volatile String participant="",csrf="";
    private final ScheduledExecutorService control=Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService signals=Executors.newSingleThreadExecutor();
    private final Set<HttpURLConnection> connections=ConcurrentHashMap.newKeySet();
    private HttpURLConnection admission;
    private final AtomicBoolean stopped=new AtomicBoolean();
    CallTransport(String base,String cookie,String endpoint,String token){
        URI u=URI.create(base);
        if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new IllegalArgumentException("HTTPS server required");
        if(!Arrays.asList("api.php","admin.php").contains(endpoint)||!token.matches("[a-f0-9]{48}"))throw new IllegalArgumentException("Invalid call parameters");
        this.base=base.endsWith("/")?base:base+"/";this.cookie=cookie==null?"":cookie;this.endpoint=endpoint;this.token=token;
    }
    static JSONObject json(Object...pairs){JSONObject j=new JSONObject();try{for(int i=0;i<pairs.length;i+=2)j.put((String)pairs[i],pairs[i+1]);}catch(JSONException e){throw new IllegalArgumentException(e);}return j;}
    private JSONObject request(String file,String action,JSONObject body,boolean leaving)throws Exception{
        if(stopped.get()&&!leaving)throw new IOException("Call cancelled");
        HttpURLConnection c=(HttpURLConnection)new URL(base+file+"?action="+URLEncoder.encode(action,"UTF-8")).openConnection();
        synchronized(connections){if(stopped.get()&&!leaving)throw new IOException("Call cancelled");if("room.join".equals(action))admission=c;connections.add(c);}
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(6000);c.setReadTimeout(7000);
        c.setRequestProperty("Cookie",cookie);c.setRequestProperty("Cache-Control","no-store");
        try{
            if(body!=null){c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("X-CSRF-Token",csrf);c.setDoOutput(true);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int code=c.getResponseCode();InputStream stream=code>=400?c.getErrorStream():c.getInputStream();if(stream==null)throw new Failure(code,"Нет ответа сервера");
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=stream){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Call response too large");out.write(b,0,n);}}
            JSONObject result;try{result=new JSONObject(out.toString("UTF-8"));}catch(JSONException e){throw new Failure(code,"Сервер вернул некорректный ответ");}
            if(code<200||code>=300)throw new Failure(code,result.optString("error","Ошибка сервера звонка"));return result;
        }finally{synchronized(connections){connections.remove(c);if(admission==c)admission=null;}c.disconnect();}
    }
    private JSONObject post(String action,JSONObject data)throws Exception{
        try{return request(endpoint,action,data,false);}
        catch(Failure e){if(e.status!=403||!"Обновите страницу и повторите действие.".equals(e.getMessage()))throw e;csrf=request("api.php","me",null,false).getString("csrf");return request(endpoint,action,data,false);}
    }
    void join(android.content.Context context,boolean listen,Reply reply){
        control.execute(()->{JSONObject result=null;Exception failure=null;try{
            JSONObject me=request("api.php","me",null,false);if(me.isNull("user"))throw new Failure(401,"Войдите в аккаунт");csrf=me.getString("csrf");
            CallLease.recover(context,base,cookie);
            if(stopped.get())throw new IOException("Call cancelled");
            result=post("room.join",json("token",token,"listen",listen));participant=result.getString("id");
            // Save before any microphone/SDP work. No Binder acknowledgement is needed.
            CallLease.remember(context,base,token,participant);
        }catch(Exception e){failure=e;}reply.done(result,failure);});
    }
    void poll(long after,boolean muted,boolean refresh,long delay,Reply reply){
        if(stopped.get())return;
        try{control.schedule(()->{if(stopped.get())return;JSONObject result=null;Exception error=null;try{result=post("room.poll",json("token",token,"id",participant,"after",after,"muted",muted,"camera",false,"refresh_ice",refresh));}catch(Exception e){error=e;}reply.done(result,error);},delay,TimeUnit.MILLISECONDS);}catch(RejectedExecutionException ignored){}
    }
    void send(JSONArray packets,Reply reply){
        if(stopped.get())return;
        try{signals.execute(()->{if(stopped.get())return;JSONObject result=null;Exception error=null;try{result=post("room.signals",json("token",token,"id",participant,"packets",packets));}catch(Exception e){error=e;}reply.done(result,error);});}catch(RejectedExecutionException ignored){}
    }
    void leave(android.content.Context context,Runnable completed){
        if(!stopped.compareAndSet(false,true))return;
        // Do not discard an admission response: the server may already have inserted
        // its participant. Read its ID, then release it in the queued control task.
        synchronized(connections){for(HttpURLConnection connection:connections)if(connection!=admission)connection.disconnect();}
        signals.shutdownNow();
        // Runs after an in-flight join. Even cancellation during admission releases its ID.
        try{control.execute(()->{try{
            if(!participant.isEmpty()){
                request("api.php","mobile.release",json("token",token,"id",participant),true);
                CallLease.clear(context,participant);CallDiagnostics.event("ROOM_RELEASED");
            }
        }catch(Exception e){CallDiagnostics.event("ROOM_RELEASE_PENDING",e);}
        finally{completed.run();control.shutdownNow();}});}catch(RejectedExecutionException e){completed.run();}
    }
}
