package su.lbs.lchat;

import android.app.*;
import android.content.*;
import android.os.*;
import android.webkit.CookieManager;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Private instrumentation APK only. Never included in the distributed client. */
public final class CallScenarioRunner extends Instrumentation {
    private NativeActivity activity;
    private Bundle args;
    interface Condition {boolean yes()throws Exception;}
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    void waitFor(String stage,long timeout,Condition condition)throws Exception{
        long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){if(condition.yes()){Bundle b=new Bundle();b.putString("stream",stage+"\n");sendStatus(0,b);return;}Thread.sleep(150);}throw new AssertionError(stage+" timed out; "+CallService.status);
    }
    long counter(String name)throws Exception{try{return CallService.class.getDeclaredField(name).getLong(null);}catch(NoSuchFieldException e){return 0;}}
    void command(String command){getTargetContext().startService(new Intent(getTargetContext(),CallService.class).putExtra("command",command));}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            File file=new File(args.getString("config","/data/local/tmp/lchat-call-scenario.json"));JSONObject config=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
            String base=config.getString("base"),cookie=config.getString("cookie"),token=config.getString("token");boolean legacy=args.getBoolean("legacy",false)||"true".equals(args.getString("legacy"));
            runOnMainSync(()->{CookieManager jar=CookieManager.getInstance();jar.setAcceptCookie(true);for(String pair:cookie.split(";"))jar.setCookie(base,pair.trim()+"; Path=/; Secure; HttpOnly");jar.flush();});
            activity=(NativeActivity)startActivitySync(new Intent(getTargetContext(),NativeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitFor("native authenticated screen",30000,()->activity.user!=null&&activity.resumed);
            for(int cycle=0;cycle<(legacy?1:2);cycle++){
                final NativeActivity a=activity;
                if(cycle==0)runOnMainSync(()->a.call(token,"api.php",false));
                else runOnMainSync(()->getTargetContext().startActivity(new Intent(getTargetContext(),NativeActivity.class).setAction("accept-call").putExtra("token",token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                waitFor("foreground call started",20000,()->CallService.active&&!CallService.launching);
                if(legacy){waitFor("legacy peer connected",45000,()->CallService.status.contains("На связи: 1"));result.putString("stream","Legacy call connected");break;}
                waitFor("bidirectional RTP",45000,()->counter("txPackets")>10&&counter("rxPackets")>10);
                if(cycle==0)Thread.sleep(Long.parseLong(args.getString("hold","0")));
                long tx=counter("txPackets"),rx=counter("rxPackets");
                String participant=activity.api.get("me").getJSONObject("call_state").getString("participant_id");
                runOnMainSync(()->a.call(token,"api.php",false));
                if(!participant.equals(activity.api.get("me").getJSONObject("call_state").getString("participant_id")))throw new AssertionError("Duplicate accept created a second admission");
                command("mic");waitFor("mute",3000,()->CallService.muted);command("mic");waitFor("unmute",3000,()->!CallService.muted);
                command("speaker");waitFor("speaker route",3000,()->CallService.speaker);command("speaker");waitFor("earpiece route",3000,()->!CallService.speaker);
                if(cycle==0){
                    getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME").close();waitFor("activity in background",5000,()->!activity.resumed);Thread.sleep(8000);
                    if(!CallService.active||counter("txPackets")<=tx||counter("rxPackets")<=rx)throw new AssertionError("RTP stopped in background");
                    Bundle b=new Bundle();b.putString("stream","background RTP continues\n");sendStatus(0,b);
                    long beforeSleep=counter("rxPackets");getUiAutomation().executeShellCommand("input keyevent KEYCODE_SLEEP").close();waitFor("screen actually off",5000,()->!getTargetContext().getSystemService(PowerManager.class).isInteractive());Thread.sleep(8000);
                    if(!CallService.active||counter("rxPackets")<=beforeSleep)throw new AssertionError("RTP stopped with screen off");
                    getUiAutomation().executeShellCommand("input keyevent KEYCODE_WAKEUP").close();
                    waitFor("screen awake",5000,()->getTargetContext().getSystemService(PowerManager.class).isInteractive());
                    b=new Bundle();b.putString("stream","screen-off RTP continues\n");sendStatus(0,b);
                }
                long beforeRestart=counter("rxPackets");command("retry");waitFor("ICE restart preserves RTP",20000,()->counter("rxPackets")>beforeRestart+30);
                result.putLong("tx_packets_"+cycle,counter("txPackets"));result.putLong("rx_packets_"+cycle,counter("rxPackets"));
                command("end");waitFor("call ended and lease released",20000,()->!CallService.active&&!CallService.closing&&!CallService.launching);
                if(new File(getTargetContext().getFilesDir(),"call-v3.json").exists())throw new AssertionError("Call lease remains after hangup");
                JSONObject me=activity.api.get("me");if(!"idle".equals(me.getJSONObject("call_state").getString("status")))throw new AssertionError("Server still considers account in call");
                if(cycle==0){runOnMainSync(()->getTargetContext().startActivity(new Intent(getTargetContext(),NativeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));waitFor("return to native UI",5000,()->activity.resumed);}
            }
            if(!legacy){
                runOnMainSync(()->activity.call(token,"api.php",false));Thread.sleep(150);command("end");
                waitFor("cancel during admission",30000,()->!CallService.active&&!CallService.closing&&!CallService.launching);
                JSONObject me=activity.api.get("me");if(!"idle".equals(me.getJSONObject("call_state").getString("status")))throw new AssertionError("Cancelled admission left a server participant");
            }
            result.putString("stream","PASS: real Android native call scenario\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){
            result.putString("stream","FAIL: "+error.getClass().getSimpleName()+": "+error.getMessage()+"\n"+CallDiagnostics.report(getTargetContext()));finish(Activity.RESULT_CANCELED,result);
        }finally{if(CallService.active)try{command("end");}catch(Exception ignored){}}
    }
}
