package su.lbs.lchat;

import android.app.NotificationManager;
import android.content.*;
import android.os.*;
import java.util.concurrent.*;

/** Read-only state subscription. No ACK waits, process restart loops or media ownership. */
final class CallClient {
    static volatile boolean ready;
    private static Context app;
    private static NativeApi api;
    private static Messenger remote;
    private static boolean bound;
    private static long generation;
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final ExecutorService recovery=Executors.newSingleThreadExecutor();
    private static final Messenger replies=new Messenger(new Handler(Looper.getMainLooper()){
        @Override public void handleMessage(Message message){
            if(message.what!=CallService.STATE||message.getData().getLong("generation")!=generation)return;
            Bundle b=message.getData();ready=remote!=null&&!b.getBoolean("finished");
            if("IDLE".equals(b.getString("phase"))&&CallService.launching)return;
            CallService.active=b.getBoolean("active");CallService.launching=b.getBoolean("launching");CallService.closing=b.getBoolean("closing");CallService.muted=b.getBoolean("muted");CallService.speaker=b.getBoolean("speaker");
            CallService.status=b.getString("status","");CallService.phase=b.getString("phase","IDLE");CallService.displayName=b.getString("name","Голосовой звонок");CallService.participantSummary=b.getString("participants","");
            CallService.callStartedMillis=b.getLong("started");CallService.incomingToken=b.getString("incoming_token","");CallService.incomingName=b.getString("incoming_name","");CallService.adminPresence=b.getString("admins","");CallService.adminArrival=b.getString("arrival","");CallService.adminArrivalUntil=b.getLong("arrival_until");
            CallService.txPackets=b.getLong("tx_packets");CallService.rxPackets=b.getLong("rx_packets");
            if(b.getBoolean("finished"))unbind();notifyUi();
        }
    });
    private static final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){remote=new Messenger(binder);try{register();}catch(RemoteException e){died();}}
        public void onServiceDisconnected(ComponentName name){died();}
        public void onBindingDied(ComponentName name){died();}
        public void onNullBinding(ComponentName name){died();}
    };
    static void connect(Context context,NativeApi client){
        app=context.getApplicationContext();api=client;
        if(bound){if(remote!=null)try{register();}catch(RemoteException e){died();}return;}
        generation++;try{bound=app.bindService(new Intent(app,CallService.class),connection,Context.BIND_AUTO_CREATE);}catch(Exception e){CallDiagnostics.event("V3_BIND_FAILED",e);}
    }
    private static void register()throws RemoteException{Message m=Message.obtain(null,CallService.REGISTER);m.replyTo=replies;Bundle b=new Bundle();b.putLong("generation",generation);m.setData(b);remote.send(m);}
    private static void unbind(){generation++;remote=null;ready=false;if(bound){bound=false;try{app.unbindService(connection);}catch(IllegalArgumentException ignored){}}}
    private static void died(){
        boolean running=CallService.active||CallService.launching||CallService.closing;unbind();CallService.active=false;CallService.launching=false;CallService.closing=false;CallService.phase="ENDED";
        if(running){CallDiagnostics.event("V3_ENGINE_PROCESS_EXIT");CallService.status="Аудиодвижок остановился. Вы вышли из звонка.";app.getSystemService(NotificationManager.class).cancel(7);NativeApi current=api;
            if(current!=null)recovery.execute(()->{try{CallLease.recover(app,current.base,current.cookie());CallDiagnostics.event("V3_CRASH_LEASE_RELEASED");}catch(Exception e){CallDiagnostics.event("V3_CRASH_LEASE_PENDING",e);}});
        }notifyUi();
    }
    private static void notifyUi(){Runnable r=CallService.listener;if(r!=null)main.post(r);}
}
