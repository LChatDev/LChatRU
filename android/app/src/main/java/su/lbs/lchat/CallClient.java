package su.lbs.lchat;

import android.app.NotificationManager;
import android.content.*;
import android.os.*;
import org.json.*;
import java.util.concurrent.*;

/** Private Binder connection to the isolated audio process, never a public broadcast. */
final class CallClient {
    static volatile boolean ready;
    private static Context app;
    private static NativeApi api;
    private static boolean bound;
    private static Messenger engine;
    private static long generation;
    private static String legacyId="",legacyToken="",legacyBase="";
    private static final ExecutorService cleanup=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final Messenger replies=new Messenger(new Handler(Looper.getMainLooper()) {
        @Override public void handleMessage(Message message) {
            if(message.what!=CallService.STATE)return;
            Bundle state=message.getData();
            if(state.getLong("generation")!=generation)return;
            ready=!state.getBoolean("finished")&&engine!=null;
            // An idle state may have been queued before startForegroundService.
            // Do not let that stale reply cancel a launch that is already in progress.
            boolean starting=CallService.launching&&!state.getBoolean("active")&&!state.getBoolean("finished");
            CallService.active=state.getBoolean("active");
            CallService.launching=starting||state.getBoolean("launching");
            CallService.closing=state.getBoolean("closing");
            CallService.muted=state.getBoolean("muted");
            CallService.speaker=state.getBoolean("speaker");
            String status=state.getString("status","");
            if(CallService.active||!status.isEmpty())CallService.status=status;
            CallService.participantSummary=state.getString("participants","");CallService.displayName=state.getString("name","Голосовой звонок");
            CallService.incomingToken=state.getString("incoming_token","");
            CallService.incomingName=state.getString("incoming_name","");
            CallService.adminPresence=state.getString("admins","");
            CallService.adminArrival=state.getString("arrival","");
            CallService.adminArrivalUntil=state.getLong("arrival_until");
            CallService.callStartedMillis=state.getLong("started");
            String id=state.getString("id",""),base=state.getString("base",""),token=state.getString("token","");
            if(!id.isEmpty()&&CallService.active){
                SharedPreferences watch=app.getSharedPreferences("call-watch",0);
                boolean saved=id.equals(watch.getString("id",""))&&base.equals(watch.getString("base",""));
                if(!saved)saved=watch.edit().putString("id",id).putString("token",token).putString("base",base).commit();
                if(saved&&engine!=null)try{Message ack=Message.obtain(null,CallService.SAVED);Bundle data=new Bundle();data.putString("id",id);ack.setData(data);engine.send(ack);}catch(RemoteException ignored){}
            }
            if(!CallService.active&&!CallService.closing&&!CallService.launching)recover();
            if(state.getBoolean("finished")&&bound){app.unbindService(connection);bound=false;engine=null;ready=false;generation++;}
            notifyUi();
        }
    });
    private static final ServiceConnection connection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){
            engine=new Messenger(binder);
            try{register();}
            catch(RemoteException e){disconnected();}
        }
        @Override public void onServiceDisconnected(ComponentName name){rebind();}
        @Override public void onBindingDied(ComponentName name){rebind();}
        @Override public void onNullBinding(ComponentName name){rebind();}
        private void rebind(){
            disconnected();
            if(bound){try{app.unbindService(this);}catch(IllegalArgumentException ignored){}bound=false;}
            main.postDelayed(()->{if(app!=null&&api!=null&&!bound)connect(app,api);},1000);
        }
    };
    static void connect(Context context,NativeApi client){
        if(app==null){
            app=context.getApplicationContext();
            SharedPreferences old=app.getSharedPreferences("call-lease",0);
            legacyId=old.getString("id","");legacyToken=old.getString("token","");legacyBase=old.getString("base","");
        }
        api=client;
        if(bound){if(engine!=null)try{register();}catch(RemoteException e){disconnected();}return;}
        generation++;
        try{bound=app.bindService(new Intent(app,CallService.class),connection,Context.BIND_AUTO_CREATE);}
        catch(Exception e){CallDiagnostics.event("ENGINE_BIND_FAILED",e);}
    }
    private static void disconnected(){
        generation++;engine=null;ready=false;
        boolean wasActive=CallService.active||CallService.launching;
        CallService.active=false;CallService.launching=false;CallService.closing=false;
        CallService.incomingToken="";CallService.incomingName="";CallService.adminPresence="";
        CallService.adminArrival="";CallService.adminArrivalUntil=0;
        if(wasActive){CallService.status="Аудиодвижок остановился. Подключение освобождается…";CallDiagnostics.event("ENGINE_PROCESS_DISCONNECTED");}
        app.getSystemService(NotificationManager.class).cancel(7);
        recover();notifyUi();
    }
    private static boolean recovering;
    private static void register()throws RemoteException{Message message=Message.obtain(null,CallService.REGISTER);message.replyTo=replies;Bundle data=new Bundle();data.putLong("generation",generation);message.setData(data);engine.send(message);}
    private static int recoveryFailures;
    private static void recover(){
        if(recovering||api==null)return;
        NativeApi client=api;SharedPreferences prefs=app.getSharedPreferences("call-watch",0);
        String saved=prefs.getString("id","");
        String id=saved.isEmpty()?legacyId:saved,base=saved.isEmpty()?legacyBase:prefs.getString("base",""),token=saved.isEmpty()?legacyToken:prefs.getString("token","");
        recovering=true;
        cleanup.execute(()->{
            boolean retry=false;
            try{
                if(!id.isEmpty()&&base.equals(client.base)){
                    JSONObject me=client.get("me");
                    MobileApi.request(base,client.cookie(),"mobile.release",NativeApi.json("id",id,"token",token),me.getString("csrf"));
                    // An old participant can never clear the saved identifier of a newer call.
                    if(id.equals(prefs.getString("id","")))prefs.edit().clear().commit();
                    if(id.equals(legacyId)){CallLease.clear(app,id);legacyId="";}
                    CallDiagnostics.event("CRASH_CONNECTION_RELEASED");
                    recoveryFailures=0;
                    main.post(()->{if(!CallService.active&&!CallService.launching&&CallService.status.startsWith("Аудиодвижок"))CallService.status="Аудиодвижок остановился. Вы вышли из звонка.";});
                }
            }catch(MobileApi.Error e){
                if(e.code==403||e.code==404||e.code==410){
                    if(id.equals(prefs.getString("id","")))prefs.edit().clear().commit();
                    if(id.equals(legacyId)){CallLease.clear(app,id);legacyId="";}
                    CallDiagnostics.event("OLD_CONNECTION_NO_LONGER_OWNED");
                }else{retry=true;CallDiagnostics.event("CONNECTION_RELEASE_DEFERRED",e);}
            }catch(Exception e){retry=true;CallDiagnostics.event("CONNECTION_RELEASE_DEFERRED",e);}
            finally{boolean again=retry;main.post(()->{recovering=false;notifyUi();if(again){long delay=Math.min(60000L,3000L<<Math.min(4,recoveryFailures++));main.postDelayed(()->{if(!CallService.active&&!CallService.launching)recover();},delay);}});}
        });
    }
    private static void notifyUi(){Runnable r=CallService.listener;if(r!=null)main.post(r);}
}
