package su.lbs.lchat;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.AudioDeviceInfo;
import android.os.*;

/** Android lifecycle/notifications only. Native media and HTTP are separate owners. */
public final class CallService extends Service implements CallEngine.Listener {
    static final int REGISTER=1,STATE=2;
    static volatile boolean active,launching,closing,muted,speaker;
    static volatile String status="",participantSummary="",displayName="Голосовой звонок",incomingName="",incomingToken="",adminPresence="",adminArrival="",phase="IDLE";
    static volatile long callStartedMillis,adminArrivalUntil,txPackets,rxPackets;
    static volatile Runnable listener;
    private static final String CHANNEL="lchat_calls";
    private Messenger client;
    private long clientGeneration;
    private CallEngine engine;
    private CallAudioRouter router;
    private Bundle last=new Bundle();
    private boolean foreground,destroyed;
    private long lastNotification;
    private final Messenger binder=new Messenger(new Handler(Looper.getMainLooper()){
        @Override public void handleMessage(Message message){
            if(message.what==REGISTER){client=message.replyTo;clientGeneration=message.getData().getLong("generation");sendState();}
        }
    });
    @Override public void onCreate(){
        super.onCreate();CallDiagnostics.install(this);CallDiagnostics.event("V3_SERVICE_CREATED");
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Голосовые звонки",NotificationManager.IMPORTANCE_LOW));
        last.putString("phase","IDLE");last.putString("status","");
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){if(engine==null)stopSelf();return START_NOT_STICKY;}
        String command=intent.getStringExtra("command");
        if("start".equals(command)){
            if(engine!=null){sendState();return START_NOT_STICKY;}
            try{
                if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED||checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Required call permissions missing");
                CallTransport transport=new CallTransport(intent.getStringExtra("base"),intent.getStringExtra("cookie"),intent.getStringExtra("endpoint"),intent.getStringExtra("token"));
                muted=intent.getBooleanExtra("listen",false);displayName="Голосовой звонок";speaker=false;
                if(Build.VERSION.SDK_INT>=30)startForeground(7,notification("Подключаемся…"),ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(7,notification("Подключаемся…"));foreground=true;
                router=new CallAudioRouter(this);
                router.start((paused,lost)->{CallEngine owner=engine;if(owner!=null){if(lost)owner.stop("Аудио используется другим приложением");else owner.focus(paused);}});
                engine=new CallEngine(this,transport,muted,this);engine.start();CallDiagnostics.event("V3_SERVICE_STARTED");
            }catch(Exception|LinkageError e){
                CallDiagnostics.event("V3_SERVICE_START_FAILED",e);if(router!=null)router.close();router=null;
                last=new Bundle();last.putBoolean("finished",true);last.putString("phase","ENDED");last.putString("status","Не удалось запустить звонок. Проверьте разрешение микрофона.");sendState();finish();
            }return START_NOT_STICKY;
        }
        CallEngine owner=engine;if(owner==null){if(!foreground)stopSelf();return START_NOT_STICKY;}
        if("end".equals(command))owner.stop("Звонок завершён");
        else if("mic".equals(command))owner.toggleMute();
        else if("mute-state".equals(command))owner.mute(intent.getBooleanExtra("muted",false));
        else if("retry".equals(command))owner.retry();
        else if("decline".equals(command))owner.decline();
        else if("speaker".equals(command))try{speaker=router.toggleSpeaker();last.putBoolean("speaker",speaker);sendState();updateNotification();}catch(Exception e){android.widget.Toast.makeText(this,"Не удалось переключить динамик. Проверьте разрешения Bluetooth.",android.widget.Toast.LENGTH_LONG).show();}
        else if("input".equals(command))try{AudioDeviceInfo input=router.input(intent.getIntExtra("device",0));router.communicationInput(input);owner.input(input);}catch(Exception e){android.widget.Toast.makeText(this,"Аудиоустройство недоступно",android.widget.Toast.LENGTH_LONG).show();}
        return START_NOT_STICKY;
    }
    @Override public void changed(CallEngine owner,Bundle state){
        if(destroyed||engine!=owner)return;last=new Bundle(state);last.putBoolean("speaker",speaker);
        active=state.getBoolean("active");launching=state.getBoolean("launching");closing=state.getBoolean("closing");muted=state.getBoolean("muted");displayName=state.getString("name","Голосовой звонок");status=state.getString("status","");
        sendState();updateNotification();
        if(state.getBoolean("finished")){engine=null;finish();}
    }
    @Override public void ended(CallEngine owner,String reason){if(engine!=owner)return;if(router!=null)router.close();router=null;}
    private void updateNotification(){
        if(!foreground||SystemClock.elapsedRealtime()-lastNotification<750)return;
        lastNotification=SystemClock.elapsedRealtime();getSystemService(NotificationManager.class).notify(7,notification(status));
    }
    private void sendState(){
        if(client==null)return;Bundle data=new Bundle(last);data.putLong("generation",clientGeneration);
        try{Message reply=Message.obtain(null,STATE);reply.setData(data);client.send(reply);}catch(RemoteException e){client=null;}
    }
    private Notification notification(String text){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,NativeActivity.class).setAction("system-call"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent end=PendingIntent.getService(this,1,new Intent(this,CallService.class).putExtra("command","end"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_lchat).setContentTitle(displayName).setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_CALL).setVisibility(Notification.VISIBILITY_PRIVATE);
        if(Build.VERSION.SDK_INT>=31)b.setStyle(Notification.CallStyle.forOngoingCall(new Person.Builder().setName(displayName).setImportant(true).build(),end));else b.addAction(new Notification.Action.Builder(null,"Завершить",end).build());
        PendingIntent mic=PendingIntent.getService(this,2,new Intent(this,CallService.class).putExtra("command","mic"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent route=PendingIntent.getService(this,3,new Intent(this,CallService.class).putExtra("command","speaker"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return b.addAction(new Notification.Action.Builder(null,muted?"Включить микрофон":"Выключить микрофон",mic).build()).addAction(new Notification.Action.Builder(null,speaker?"Выключить динамик":"Включить динамик",route).build()).build();
    }
    private void finish(){if(foreground){stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;}stopSelf();}
    @Override public void onDestroy(){destroyed=true;if(engine!=null)engine.stop("Звонок завершён");if(router!=null)router.close();router=null;super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return binder.getBinder();}
}
