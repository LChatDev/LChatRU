package su.lbs.lchat;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.os.*;
import org.json.*;
import org.webrtc.*;
import org.webrtc.audio.JavaAudioDeviceModule;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One serial native-media thread, asynchronous HTTP transport, one session owner. */
final class CallEngine {
    interface Listener {void changed(CallEngine owner,Bundle state);void ended(CallEngine owner,String reason);}
    private static final HandlerThread RTC_THREAD=new HandlerThread("LChat-RTC");
    static {RTC_THREAD.start();}
    private static final Handler RTC=new Handler(RTC_THREAD.getLooper());
    private static boolean initialized;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Context context;
    private final CallTransport wire;
    private final Listener listener;
    private final CallLifecycle life=new CallLifecycle();
    private final Map<String,Peer> peers=new LinkedHashMap<>();
    private final CallOutbox<JSONObject> outbox=new CallOutbox<>();
    private final Set<String> ignored=new HashSet<>(),adminsSeen=new HashSet<>();
    private List<PeerConnection.IceServer> ice=new ArrayList<>();
    private PeerConnection.IceTransportsType transport=PeerConnection.IceTransportsType.ALL;
    private PeerConnectionFactory factory;
    private JavaAudioDeviceModule device;
    private AudioSource source;
    private org.webrtc.AudioTrack track;
    private boolean muted,focusPaused,mediaReady;
    private long after,lastIceRefresh,closedTx,closedRx;
    private int pollFailures,signalFailures;
    private String title="Голосовой звонок",notice="Подключаемся…",incoming="",incomingName="",adminArrival="";
    private long adminArrivalUntil;
    private static final class Peer {
        String id,name,lastRemote="",lastAnswer="";
        PeerConnection pc;
        boolean connected,busy,remoteReady,offered,closed,moderator,hidden,remoteMuted;
        int restarts;
        long packetsSent,packetsReceived;
        final List<IceCandidate> candidates=new ArrayList<>();
        final Deque<JSONObject> descriptions=new ArrayDeque<>();
        Runnable retry;
    }
    private static class Sdp implements SdpObserver {
        public void onCreateSuccess(SessionDescription s){}
        public void onSetSuccess(){}
        public void onCreateFailure(String e){}
        public void onSetFailure(String e){}
    }
    CallEngine(Context context,CallTransport wire,boolean muted,Listener listener){this.context=context.getApplicationContext();this.wire=wire;this.muted=muted;this.listener=listener;}
    String token(){return wire.token;}
    private void dispatch(Runnable operation){RTC.post(()->{if(!life.live())return;try{operation.run();}catch(Exception|LinkageError e){CallDiagnostics.event("ENGINE_OPERATION_FAILED",e);stopOnRtc("Не удалось продолжить звонок. Подключение освобождено.");}});}
    void start(){dispatch(()->{
        if(!life.join())return;CallDiagnostics.event("V3_ROOM_JOIN_BEGIN");
        wire.join(context,muted,(reply,error)->dispatch(()->{
            if(error!=null){fail(error,"Не удалось войти в звонок");return;}
            if(!life.admitted())return;CallDiagnostics.event("V3_ROOM_ADMITTED");
            updateIce(reply.optJSONArray("ice_servers"));transport="relay".equals(reply.optString("ice_transport_policy"))?PeerConnection.IceTransportsType.RELAY:PeerConnection.IceTransportsType.ALL;
            createMedia();poll(0);stats();
        }));emit();
    });}
    private void createMedia(){
        CallDiagnostics.event("V3_NATIVE_INIT_BEGIN");
        if(!initialized){PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions());initialized=true;}
        device=JavaAudioDeviceModule.builder(context).setUseHardwareAcousticEchoCanceler(false).setUseHardwareNoiseSuppressor(false).setUseLowLatency(false).setEnableVolumeLogger(false)
            .setAudioRecordErrorCallback(new JavaAudioDeviceModule.AudioRecordErrorCallback(){
                public void onWebRtcAudioRecordInitError(String e){audioError("MIC_INIT");}
                public void onWebRtcAudioRecordStartError(JavaAudioDeviceModule.AudioRecordStartErrorCode c,String e){audioError("MIC_START");}
                public void onWebRtcAudioRecordError(String e){audioError("MIC_RUNTIME");}
            }).setAudioTrackErrorCallback(new JavaAudioDeviceModule.AudioTrackErrorCallback(){
                public void onWebRtcAudioTrackInitError(String e){audioError("OUTPUT_INIT");}
                public void onWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode c,String e){audioError("OUTPUT_START");}
                public void onWebRtcAudioTrackError(String e){audioError("OUTPUT_RUNTIME");}
            }).setAudioRecordStateCallback(new JavaAudioDeviceModule.AudioRecordStateCallback(){
                public void onWebRtcAudioRecordStart(){CallDiagnostics.event("V3_MIC_RUNNING");}
                public void onWebRtcAudioRecordStop(){CallDiagnostics.event("V3_MIC_STOPPED");}
            }).createAudioDeviceModule();
        factory=PeerConnectionFactory.builder().setAudioDeviceModule(device).createPeerConnectionFactory();
        source=factory.createAudioSource(new MediaConstraints());track=factory.createAudioTrack("lchat_voice",source);track.setEnabled(!muted&&!focusPaused);mediaReady=true;
        notice="Ожидаем участника…";CallDiagnostics.event("V3_NATIVE_READY");emit();
    }
    private void audioError(String stage){CallDiagnostics.event("V3_AUDIO_"+stage);stop("Не удалось запустить микрофон или динамик");}
    void mute(boolean value){dispatch(()->{muted=value;applyMute();emit();});}
    void toggleMute(){dispatch(()->{muted=!muted;applyMute();emit();});}
    void focus(boolean paused){dispatch(()->{focusPaused=paused;applyMute();emit();});}
    void input(AudioDeviceInfo input){dispatch(()->{if(device!=null)device.setPreferredInputDevice(input);});}
    void decline(){dispatch(()->{if(!incoming.isEmpty())ignored.add(incoming);incoming="";incomingName="";emit();});}
    void retry(){dispatch(()->{lastIceRefresh=0;for(Peer p:peers.values()){p.restarts=0;if(wire.participant.compareTo(p.id)<0)offer(p,true);else send(p,CallTransport.json("type","restart"));}notice="Восстанавливаем соединение…";emit();});}
    private void applyMute(){if(track!=null)track.setEnabled(!muted&&!focusPaused);}
    private boolean live(Peer p){return life.live()&&!p.closed&&peers.get(p.id)==p;}
    private void updateIce(JSONArray servers){
        if(servers==null)return;List<PeerConnection.IceServer> fresh=new ArrayList<>();
        try{for(int i=0;i<servers.length();i++){JSONObject row=servers.getJSONObject(i);Object urls=row.get("urls");List<String> addresses=new ArrayList<>();if(urls instanceof JSONArray){JSONArray list=(JSONArray)urls;for(int k=0;k<list.length();k++)addresses.add(list.getString(k));}else addresses.add(String.valueOf(urls));fresh.add(PeerConnection.IceServer.builder(addresses).setUsername(row.optString("username")).setPassword(row.optString("credential")).createIceServer());}}
        catch(JSONException e){throw new IllegalArgumentException("Invalid ICE response",e);}
        ice=fresh;lastIceRefresh=SystemClock.elapsedRealtime();
        for(Peer p:peers.values())if(live(p)&&!p.pc.setConfiguration(configuration()))CallDiagnostics.event("V3_ICE_UPDATE_REJECTED");
    }
    private PeerConnection.RTCConfiguration configuration(){PeerConnection.RTCConfiguration c=new PeerConnection.RTCConfiguration(ice);c.sdpSemantics=PeerConnection.SdpSemantics.UNIFIED_PLAN;c.iceTransportsType=transport;return c;}
    private void poll(long delay){
        boolean refresh=SystemClock.elapsedRealtime()-lastIceRefresh>=15*60*1000L||lastIceRefresh==0;
        wire.poll(after,muted||focusPaused,refresh,delay,(data,error)->dispatch(()->{
            if(error!=null){CallDiagnostics.event("V3_POLL_FAILED",error);if(error instanceof CallTransport.Failure&&((CallTransport.Failure)error).terminal()){fail(error,"Сервер завершил звонок");return;}if(++pollFailures>15){stopOnRtc("Нет связи с сервером звонка");return;}life.reconnect();notice="Восстанавливаем связь с сервером…";emit();poll(Math.min(5000,1000L*pollFailures));return;}
            pollFailures=0;try{
                updateIce(data.optJSONArray("ice_servers"));
                JSONArray rows=data.getJSONArray("peers");Set<String> present=new HashSet<>();
                for(int i=0;i<rows.length();i++)present.add(rows.getJSONObject(i).getString("id"));
                for(String old:new ArrayList<>(peers.keySet()))if(!present.contains(old))removePeer(old);
                for(int i=0;i<rows.length();i++){
                    JSONObject row=rows.getJSONObject(i);String id=row.getString("id");Peer p=peers.get(id);
                    if(p==null){p=createPeer(id,row.getString("name"));}
                    p.name=row.getString("name");
                    p.moderator=row.optInt("is_admin")==1;p.hidden=row.optInt("tile_hidden")==1;p.remoteMuted=row.optInt("muted")==1;
                    if(row.optInt("is_admin")==1&&adminsSeen.add(id)){adminArrival="Админ зашёл в звонок: "+p.name;adminArrivalUntil=System.currentTimeMillis()+10000;}
                    if(wire.participant.compareTo(id)<0&&!p.offered&&!p.busy)offer(p,false);
                }
                JSONArray events=data.getJSONArray("signals");for(int i=0;i<events.length();i++){
                    JSONObject event=events.getJSONObject(i);Peer p=peers.get(event.getString("sender"));if(p!=null)signal(p,event.getJSONObject("payload"));after=Math.max(after,event.getLong("id"));
                }
                incoming="";incomingName="";JSONArray calls=data.optJSONArray("incoming_calls");
                if(calls!=null)for(int i=0;i<calls.length();i++){JSONObject call=calls.getJSONObject(i);if(!ignored.contains(call.getString("token"))){incoming=call.getString("token");incomingName=call.optString("caller");break;}}
                title=peers.size()==1?peers.values().iterator().next().name:peers.isEmpty()?"Голосовой звонок":"Групповой звонок · "+(peers.size()+1);
                updateNotice();emit();poll(1000);
            }catch(Exception e){CallDiagnostics.event("V3_SIGNAL_PROTOCOL_FAILED",e);stopOnRtc("Некорректный ответ сервера звонка");}
        }));
    }
    private Peer createPeer(String id,String name){
        Peer p=new Peer();p.id=id;p.name=name;peers.put(id,p);CallDiagnostics.event("V3_PEER_CREATE");
        p.pc=factory.createPeerConnection(configuration(),new PeerConnection.Observer(){
            public void onSignalingChange(PeerConnection.SignalingState s){}
            public void onIceConnectionChange(PeerConnection.IceConnectionState s){}
            public void onIceConnectionReceivingChange(boolean b){}
            public void onIceGatheringChange(PeerConnection.IceGatheringState s){}
            public void onIceCandidate(IceCandidate candidate){dispatch(()->{if(live(p))send(p,CallTransport.json("type","candidate","candidate",CallTransport.json("candidate",candidate.sdp,"sdpMid",candidate.sdpMid,"sdpMLineIndex",candidate.sdpMLineIndex)));});}
            public void onIceCandidatesRemoved(IceCandidate[] c){}
            public void onAddStream(MediaStream s){}
            public void onRemoveStream(MediaStream s){}
            public void onDataChannel(DataChannel d){}
            public void onRenegotiationNeeded(){}
            public void onTrack(RtpTransceiver ignored){CallDiagnostics.event("V3_REMOTE_AUDIO");}
            public void onConnectionChange(PeerConnection.PeerConnectionState state){dispatch(()->{
                if(!live(p))return;CallDiagnostics.event("V3_PEER_"+state.name());p.connected=state==PeerConnection.PeerConnectionState.CONNECTED;
                if(p.connected){life.connected(System.currentTimeMillis());p.restarts=0;if(p.retry!=null)RTC.removeCallbacks(p.retry);p.retry=null;}
                else if(state==PeerConnection.PeerConnectionState.FAILED||state==PeerConnection.PeerConnectionState.DISCONNECTED){life.reconnect();scheduleRecovery(p,3000);}
                updateNotice();emit();
            });}
        });
        if(p.pc==null)throw new IllegalStateException("PeerConnection unavailable");
        if(p.pc.addTrack(track,Collections.singletonList("lchat"))==null)throw new IllegalStateException("Audio sender unavailable");
        CallDiagnostics.event("V3_AUDIO_ATTACHED");scheduleRecovery(p,15000);return p;
    }
    private void offer(Peer p,boolean restart){
        if(!live(p)||p.busy)return;p.busy=true;MediaConstraints constraints=new MediaConstraints();if(restart)constraints.mandatory.add(new MediaConstraints.KeyValuePair("IceRestart","true"));
        Runnable create=()->{if(!live(p))return;CallDiagnostics.event("V3_CREATE_OFFER");p.pc.createOffer(new Sdp(){public void onCreateSuccess(SessionDescription s){dispatch(()->local(p,s));}public void onCreateFailure(String e){dispatch(()->sdpFailed(p));}},constraints);};
        if(restart&&p.pc.signalingState()==PeerConnection.SignalingState.HAVE_LOCAL_OFFER)p.pc.setLocalDescription(new Sdp(){public void onSetSuccess(){dispatch(create);}public void onSetFailure(String e){dispatch(()->sdpFailed(p));}},new SessionDescription(SessionDescription.Type.ROLLBACK,""));else create.run();
    }
    private void local(Peer p,SessionDescription description){
        if(!live(p))return;p.pc.setLocalDescription(new Sdp(){public void onSetSuccess(){dispatch(()->{
            if(!live(p))return;p.busy=false;p.offered=true;if(description.type==SessionDescription.Type.ANSWER)p.lastAnswer=description.description;
            CallDiagnostics.event("V3_LOCAL_"+description.type.name());send(p,CallTransport.json("type",description.type.canonicalForm(),"sdp",description.description));drain(p);
        });}public void onSetFailure(String error){dispatch(()->sdpFailed(p));}},description);
    }
    private void signal(Peer p,JSONObject message)throws JSONException{
        if(!live(p))return;String type=message.getString("type");
        if("restart".equals(type)){if(wire.participant.compareTo(p.id)<0)offer(p,true);return;}
        if("candidate".equals(type)){
            JSONObject c=message.getJSONObject("candidate");IceCandidate candidate=new IceCandidate(c.optString("sdpMid",null),c.optInt("sdpMLineIndex"),c.getString("candidate"));
            if(!p.remoteReady||p.busy){if(p.candidates.size()<256)p.candidates.add(candidate);}else p.pc.addIceCandidate(candidate);return;
        }
        if(!Arrays.asList("offer","answer").contains(type))return;
        if("offer".equals(type)&&wire.participant.compareTo(p.id)<0)return;
        if(p.busy){if(p.descriptions.size()<4)p.descriptions.addLast(message);return;}
        if("answer".equals(type)&&p.pc.signalingState()!=PeerConnection.SignalingState.HAVE_LOCAL_OFFER)return;
        String sdp=message.getString("sdp");
        if("offer".equals(type)&&sdp.equals(p.lastRemote)&&!p.lastAnswer.isEmpty()){send(p,CallTransport.json("type","answer","sdp",p.lastAnswer));return;}
        p.busy=true;CallDiagnostics.event("V3_REMOTE_"+type.toUpperCase(Locale.ROOT));
        p.pc.setRemoteDescription(new Sdp(){public void onSetSuccess(){dispatch(()->{
            if(!live(p))return;p.remoteReady=true;p.lastRemote=sdp;flushCandidates(p);
            if("offer".equals(type))p.pc.createAnswer(new Sdp(){public void onCreateSuccess(SessionDescription s){dispatch(()->local(p,s));}public void onCreateFailure(String e){dispatch(()->sdpFailed(p));}},new MediaConstraints());
            else{p.busy=false;drain(p);}
        });}public void onSetFailure(String e){dispatch(()->sdpFailed(p));}},new SessionDescription(SessionDescription.Type.fromCanonicalForm(type),sdp));
    }
    private void flushCandidates(Peer p){for(IceCandidate candidate:p.candidates)p.pc.addIceCandidate(candidate);p.candidates.clear();}
    private void drain(Peer p){if(!live(p)||p.busy)return;if(p.remoteReady)flushCandidates(p);JSONObject next=p.descriptions.pollFirst();if(next!=null)try{signal(p,next);}catch(JSONException e){sdpFailed(p);}}
    private void sdpFailed(Peer p){if(!live(p))return;p.busy=false;CallDiagnostics.event("V3_SDP_FAILED");scheduleRecovery(p,3000);}
    private void scheduleRecovery(Peer p,long delay){
        if(!live(p)||p.retry!=null)return;
        p.retry=()->{p.retry=null;if(!live(p)||p.connected)return;if(p.restarts++>=3){notice="Не удалось соединиться. Проверьте сеть и TURN.";emit();return;}if(wire.participant.compareTo(p.id)<0)offer(p,true);else send(p,CallTransport.json("type","restart"));scheduleRecovery(p,10000);};RTC.postDelayed(p.retry,delay);
    }
    private void send(Peer p,JSONObject payload){
        if(!live(p))return;JSONObject packet=CallTransport.json("to",p.id,"payload",payload);
        if(!outbox.add(packet,packet.toString().getBytes(StandardCharsets.UTF_8).length)){stopOnRtc("Слишком много неподтверждённых сигналов");return;}RTC.postDelayed(this::flush,30);
    }
    private void flush(){
        if(!life.live()||outbox.busy())return;List<JSONObject> items=outbox.begin();if(items.isEmpty())return;
        JSONArray batch=new JSONArray();for(JSONObject item:items)batch.put(item);
        wire.send(batch,(data,error)->dispatch(()->{
            if(error!=null){CallDiagnostics.event("V3_SIGNAL_SEND_FAILED",error);if(error instanceof CallTransport.Failure&&((CallTransport.Failure)error).terminal()){fail(error,"Сервер отклонил звонок");return;}if(++signalFailures<=3){outbox.retry();RTC.postDelayed(this::flush,1000L*signalFailures);return;}outbox.acknowledge();signalFailures=0;for(Peer p:peers.values())scheduleRecovery(p,1000);}
            else{
                JSONArray result=data.optJSONArray("results");if(result==null||result.length()!=items.size()){outbox.retry();RTC.postDelayed(this::flush,1000);return;}
                outbox.acknowledge();signalFailures=0;
                for(int i=0;i<result.length();i++)if(!result.optJSONObject(i).optBoolean("ok")){Peer p=peers.get(items.get(i).optString("to"));if(p!=null)scheduleRecovery(p,1000);}
            }
            flush();
        }));
    }
    private void stats(){
        if(!life.live())return;
        for(Peer p:peers.values())if(live(p)&&p.connected)p.pc.getStats(report->dispatch(()->{
            if(!live(p))return;long tx=0,rx=0;
            for(RTCStats stat:report.getStatsMap().values()){
                Map<String,Object> m=stat.getMembers();Object kind=m.containsKey("kind")?m.get("kind"):m.get("mediaType");if(!"audio".equals(kind))continue;
                if("outbound-rtp".equals(stat.getType()))tx+=number(m.get("packetsSent"));if("inbound-rtp".equals(stat.getType()))rx+=number(m.get("packetsReceived"));
            }p.packetsSent=tx;p.packetsReceived=rx;
        }));
        RTC.postDelayed(this::stats,3000);
    }
    private long number(Object value){try{return value==null?0:Long.parseLong(value.toString());}catch(NumberFormatException e){return 0;}}
    private void updateNotice(){int connected=0;for(Peer p:peers.values())if(p.connected)connected++;if(connected>0)life.connected(System.currentTimeMillis());else life.waiting();notice=peers.isEmpty()?"Ожидаем участника…":connected>0?"На связи · участников "+(peers.size()+1):"Соединяем участников…";}
    private void emit(){
        Bundle b=new Bundle();b.putBoolean("active",life.live());b.putBoolean("launching",life.phase()==CallLifecycle.Phase.STARTING||life.phase()==CallLifecycle.Phase.JOINING);b.putBoolean("closing",life.phase()==CallLifecycle.Phase.STOPPING);b.putBoolean("finished",life.phase()==CallLifecycle.Phase.ENDED);b.putString("phase",life.phase().name());
        b.putBoolean("muted",muted);b.putString("status",focusPaused?"Микрофон приостановлен другим приложением":notice);b.putString("name",title);b.putLong("started",life.connectedAt());
        StringBuilder participants=new StringBuilder(),admins=new StringBuilder();long tx=closedTx,rx=closedRx;for(Peer p:peers.values()){if(participants.length()>0)participants.append('\n');participants.append(p.name).append(p.connected?" · на связи":" · подключается");if(p.remoteMuted)participants.append(" · микрофон выключен");if(p.moderator){if(admins.length()>0)admins.append('\n');admins.append(p.name);if(p.hidden)admins.append(" · без видеоплитки");}tx+=p.packetsSent;rx+=p.packetsReceived;}
        b.putString("admins",admins.toString());
        b.putString("participants",participants.toString());b.putLong("tx_packets",tx);b.putLong("rx_packets",rx);
        b.putString("incoming_token",incoming);b.putString("incoming_name",incomingName);b.putString("arrival",System.currentTimeMillis()<adminArrivalUntil?adminArrival:"");b.putLong("arrival_until",adminArrivalUntil);
        b.putString("id",wire.participant);b.putString("base",wire.base);b.putString("token",wire.token);
        main.post(()->listener.changed(this,b));
    }
    private void fail(Exception error,String fallback){CallDiagnostics.event("V3_CALL_FAILED",error);stopOnRtc(error instanceof CallTransport.Failure?error.getMessage():fallback);}
    void stop(String reason){RTC.post(()->stopOnRtc(reason));}
    private void stopOnRtc(String reason){
        if(!life.stop())return;notice=reason;incoming="";incomingName="";adminArrival="";CallDiagnostics.event("V3_STOP_BEGIN");emit();
        for(String id:new ArrayList<>(peers.keySet()))removePeer(id);
        // JNI teardown is always serialized after earlier SDP callbacks on RTC_THREAD.
        release(()->{if(track!=null)track.dispose();});track=null;
        release(()->{if(source!=null)source.dispose();});source=null;
        release(()->{if(factory!=null)factory.dispose();});factory=null;
        release(()->{if(device!=null)device.release();});device=null;mediaReady=false;outbox.clear();
        main.post(()->listener.ended(this,reason));
        wire.leave(context,()->RTC.post(()->{life.ended();CallDiagnostics.event("V3_STOP_COMPLETE");emit();}));
    }
    private void removePeer(String id){Peer p=peers.remove(id);if(p==null)return;closedTx+=p.packetsSent;closedRx+=p.packetsReceived;p.closed=true;if(p.retry!=null)RTC.removeCallbacks(p.retry);if(p.pc!=null){release(p.pc::close);release(p.pc::dispose);p.pc=null;}p.candidates.clear();p.descriptions.clear();}
    private void release(Runnable operation){try{operation.run();}catch(Exception e){CallDiagnostics.event("V3_NATIVE_RELEASE_FAILED",e);}}
}
