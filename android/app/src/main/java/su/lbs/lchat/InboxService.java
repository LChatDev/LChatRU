package su.lbs.lchat;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.os.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** User-visible background inbox. No microphone is opened until the user accepts a call. */
public final class InboxService extends Service {
    static final String LIVE="lchat_inbox", CALLS="lchat_incoming", MESSAGES="lchat_messages";
    static volatile boolean running=false, foreground=false;
    static volatile String ringingToken="", ringingName="";
    private final ScheduledExecutorService loop=Executors.newSingleThreadScheduledExecutor();
    private final Set<String> declined=ConcurrentHashMap.newKeySet();
    private final Map<Integer,ArrayList<Notification.MessagingStyle.Message>> histories=new ConcurrentHashMap<>();
    private volatile String base,cookie,csrf="";private long cursor=0,userId=0;private boolean initialized=false;private volatile boolean stopping=false;private int failures=0;private ScheduledFuture<?> next;
    private NotificationManager manager;
    @Override public void onCreate(){super.onCreate();manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(LIVE,"Фоновая связь LChat",NotificationManager.IMPORTANCE_LOW));
        NotificationChannel calls=new NotificationChannel(CALLS,"Входящие звонки",NotificationManager.IMPORTANCE_HIGH);calls.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());calls.enableVibration(true);manager.createNotificationChannel(calls);
        NotificationChannel messages=new NotificationChannel(MESSAGES,"Сообщения",NotificationManager.IMPORTANCE_HIGH);messages.enableVibration(true);manager.createNotificationChannel(messages);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){if(intent==null){stopSelf();return START_NOT_STICKY;}String command=intent.getStringExtra("command");
        if("stop".equals(command)){getSharedPreferences("lchat",MODE_PRIVATE).edit().putBoolean("background",false).apply();stopSelf();return START_NOT_STICKY;}
        if("decline".equals(command)||"accepted".equals(command)){String token=intent.getStringExtra("token");if(token!=null)declined.add(token);if(Objects.equals(token,ringingToken))clearCall();return START_NOT_STICKY;}
        if("reply".equals(command)){Bundle results=RemoteInput.getResultsFromIntent(intent);CharSequence text=results==null?null:results.getCharSequence("reply");int chat=intent.getIntExtra("chat_id",0);if(text!=null&&chat>0){String body=text.toString().trim();if(!body.isEmpty()&&body.length()<=4000)submit(()->reply(chat,body));}return START_NOT_STICKY;}
        if(!"start".equals(command))return START_NOT_STICKY;
        String site=intent.getStringExtra("base"),session=intent.getStringExtra("cookie");if(site==null||!site.startsWith("https://")||session==null||session.isEmpty()){stopSelf();return START_NOT_STICKY;}
        if(running){submit(()->{if(!Objects.equals(base,site)||!Objects.equals(cookie,session)){clearAccount();base=site;cookie=session;}});return START_NOT_STICKY;}
        base=site;cookie=session;running=true;
        try{if(Build.VERSION.SDK_INT>=34)startForeground(8,connection("Сообщения и звонки в фоне"),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(8,connection("Сообщения и звонки в фоне"));}catch(Exception e){running=false;stopSelf();return START_NOT_STICKY;}
        submit(this::poll);return START_NOT_STICKY;
    }
    private Notification connection(String text){PendingIntent open=PendingIntent.getActivity(this,80,new Intent(this,NativeActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,81,new Intent(this,InboxService.class).putExtra("command","stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,LIVE).setSmallIcon(R.drawable.ic_lchat).setContentTitle("LChat на связи").setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"Выключить фон",stop).build()).build();}
    private void submit(Runnable r){try{loop.execute(()->{if(!stopping)r.run();});}catch(RejectedExecutionException ignored){}}
    private void poll(){if(stopping)return;try{
        JSONObject d=MobileApi.request(base,cookie,"mobile.events"+(initialized?"&after="+cursor:""),null,null);if(stopping)return;long uid=d.getLong("user_id");if(userId!=0&&uid!=userId){clearAccount();d=MobileApi.request(base,cookie,"mobile.events",null,null);uid=d.getLong("user_id");if(stopping)return;}userId=uid;csrf=d.getString("csrf");cursor=d.getLong("cursor");initialized=true;failures=0;
        JSONArray items=d.getJSONArray("messages");for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);if(!foreground||CallService.active)message(item);}
        String found="",name="";JSONArray calls=d.getJSONArray("incoming_calls");if(!CallService.active&&"idle".equals(d.getJSONObject("call_state").getString("status")))for(int i=0;i<calls.length();i++){JSONObject call=calls.getJSONObject(i);if(!declined.contains(call.getString("token"))){found=call.getString("token");name=call.getString("caller");break;}}
        if(found.isEmpty())clearCall();else if(!found.equals(ringingToken)){ringingToken=found;ringingName=name;incoming(found,name);}
        if(declined.size()>200)declined.clear();manager.notify(8,connection("Сообщения и звонки в фоне"));
    }catch(MobileApi.Error e){if(e.code==401||e.code==403){clearAccount();stopSelf();return;}failures++;clearCall();manager.notify(8,connection("Не удалось проверить входящие"));}
     catch(Exception e){failures++;clearCall();manager.notify(8,connection("Восстанавливаем связь…"));}
     if(!stopping)next=loop.schedule(this::poll,failures==0?4:Math.min(60,4L<<Math.min(4,failures)),TimeUnit.SECONDS);
    }
    private void incoming(String token,String name){CallDiagnostics.event("INCOMING_NOTIFICATION");manager.notify(9,CallNotifications.incoming(this,CALLS,token,name));}
    private void message(JSONObject item)throws JSONException{int chat=item.getInt("chat_id");String name=item.getString("name"),title="direct".equals(item.getString("kind"))?name:item.getString("title");ArrayList<Notification.MessagingStyle.Message> list=histories.computeIfAbsent(chat,k->new ArrayList<>());Notification.MessagingStyle.Message m=Build.VERSION.SDK_INT>=28?new Notification.MessagingStyle.Message(item.getString("body"),System.currentTimeMillis(),new Person.Builder().setName(name).build()):new Notification.MessagingStyle.Message(item.getString("body"),System.currentTimeMillis(),name);list.add(m);while(list.size()>8)list.remove(0);
        Notification.MessagingStyle style=new Notification.MessagingStyle("Вы").setConversationTitle(title);if(Build.VERSION.SDK_INT>=28)style.setGroupConversation(!"direct".equals(item.getString("kind")));for(Notification.MessagingStyle.Message row:list)style.addMessage(row);
        PendingIntent open=PendingIntent.getActivity(this,chat,new Intent(this,NativeActivity.class).setAction("open-chat").setData(android.net.Uri.parse("lchat-chat:"+chat)).putExtra("chat_id",chat),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent reply=PendingIntent.getService(this,chat,new Intent(this,InboxService.class).setAction("reply-"+chat).putExtra("command","reply").putExtra("chat_id",chat),PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0));
        Notification.Action action=new Notification.Action.Builder(null,"Ответить",reply).addRemoteInput(new RemoteInput.Builder("reply").setLabel("Сообщение").build()).setAllowGeneratedReplies(true).build();
        manager.notify("chat",chat,new Notification.Builder(this,MESSAGES).setSmallIcon(R.drawable.ic_lchat).setStyle(style).setContentTitle(title).setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_MESSAGE).setVisibility(Notification.VISIBILITY_PRIVATE).addAction(action).build());
    }
    private void reply(int chat,String body){try{JSONObject data=new JSONObject().put("chat_id",chat).put("body",body).put("nonce",UUID.randomUUID().toString().replace("-",""));MobileApi.request(base,cookie,"message.send",data,csrf);manager.cancel("chat",chat);histories.remove(chat);}catch(Exception e){manager.notify("chat",chat,new Notification.Builder(this,MESSAGES).setSmallIcon(R.drawable.ic_lchat).setContentTitle("Ответ не отправлен").setContentText("Откройте чат и повторите отправку").setContentIntent(PendingIntent.getActivity(this,chat,new Intent(this,NativeActivity.class).putExtra("chat_id",chat),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).build());}}
    private void clearCall(){ringingToken="";ringingName="";manager.cancel(9);}
    private void clearAccount(){clearCall();for(Integer chat:histories.keySet())manager.cancel("chat",chat);histories.clear();declined.clear();cursor=0;userId=0;initialized=false;csrf="";}
    @Override public void onDestroy(){stopping=true;running=false;if(next!=null)next.cancel(false);loop.shutdownNow();clearAccount();cookie=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
