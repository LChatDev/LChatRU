package su.lbs.lchat;

import android.content.*;
import android.media.*;
import android.os.*;

/** Owns Android audio focus/routing separately from native WebRTC resources. UI thread only. */
final class CallAudioRouter {
    interface Focus {void changed(boolean paused,boolean lost);}
    private final Context context;
    private final AudioManager audio;
    private AudioFocusRequest request;
    private PowerManager.WakeLock wake;
    private int previousMode;
    private boolean started,speaker;
    CallAudioRouter(Context context){this.context=context;audio=context.getSystemService(AudioManager.class);}
    void start(Focus callback){
        previousMode=audio.getMode();
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->callback.changed(change!=AudioManager.AUDIOFOCUS_GAIN,change==AudioManager.AUDIOFOCUS_LOSS),new Handler(Looper.getMainLooper())).build();
        if(audio.requestAudioFocus(request)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)throw new IllegalStateException("Аудио занято другим приложением");
        started=true;audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
        wake=context.getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LChat:NativeCall");wake.acquire(7*60*60*1000L);
    }
    boolean toggleSpeaker(){
        if(Build.VERSION.SDK_INT>=31){
            if(speaker){audio.clearCommunicationDevice();speaker=false;}
            else{boolean selected=false;for(AudioDeviceInfo device:audio.getAvailableCommunicationDevices())if(device.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER){selected=audio.setCommunicationDevice(device);break;}if(!selected)throw new IllegalStateException("Динамик недоступен");speaker=true;}
        }else{speaker=!speaker;audio.setSpeakerphoneOn(speaker);}return speaker;
    }
    AudioDeviceInfo input(int id){for(AudioDeviceInfo device:audio.getDevices(AudioManager.GET_DEVICES_INPUTS))if(device.getId()==id)return device;return null;}
    void communicationInput(AudioDeviceInfo input){
        if(Build.VERSION.SDK_INT>=31){
            if(input==null){audio.clearCommunicationDevice();speaker=false;return;}
            for(AudioDeviceInfo output:audio.getAvailableCommunicationDevices())if(output.getType()==input.getType()){audio.setCommunicationDevice(output);speaker=output.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;return;}
        }else if(input!=null&&input.getType()==AudioDeviceInfo.TYPE_BLUETOOTH_SCO){audio.startBluetoothSco();audio.setBluetoothScoOn(true);}
    }
    void close(){
        if(!started)return;started=false;
        try{if(Build.VERSION.SDK_INT>=31)audio.clearCommunicationDevice();else{audio.stopBluetoothSco();audio.setBluetoothScoOn(false);}audio.setSpeakerphoneOn(false);audio.setMode(previousMode);}catch(Exception e){CallDiagnostics.event("AUDIO_ROUTE_RELEASE",e);}
        if(request!=null)audio.abandonAudioFocusRequest(request);
        if(wake!=null&&wake.isHeld())wake.release();
    }
}
