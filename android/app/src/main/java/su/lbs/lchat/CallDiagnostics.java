package su.lbs.lchat;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

/** Local, bounded technical events. No URLs, cookies, tokens, names or message bodies. */
final class CallDiagnostics {
    private static Context app;
    private static boolean engine;
    private static String version(Context context){try{return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;}catch(PackageManager.NameNotFoundException ignored){return "unknown";}}
    static synchronized void install(Context context){if(app!=null){event("APP_OPEN_"+version(context));return;}app=context.getApplicationContext();engine=context instanceof CallService;Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();Thread.setDefaultUncaughtExceptionHandler((thread,error)->{event("UNCAUGHT",error);if(previous!=null)previous.uncaughtException(thread,error);else{android.os.Process.killProcess(android.os.Process.myPid());System.exit(10);}});event(engine?"AUDIO_PROCESS_OPEN":"APP_OPEN_"+version(context));}
    static synchronized void event(String stage){event(stage,null);}
    static synchronized void event(String stage,Throwable error){if(app==null)return;StringBuilder line=new StringBuilder(new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date())).append(' ').append(stage);
        if(error!=null){line.append(' ').append(error.getClass().getSimpleName());int count=0;for(StackTraceElement frame:error.getStackTrace())if(frame.getClassName().startsWith("su.lbs.lchat.")){line.append("\n  ").append(frame.getClassName()).append('.').append(frame.getMethodName()).append(':').append(frame.getLineNumber());if(++count==4)break;}}
        if(engine){try{File file=new File(app.getFilesDir(),"audio-diagnostics.log");String log=read(file)+line+"\n";if(log.length()>10000)log=log.substring(log.indexOf('\n',log.length()-8000)+1);try(Writer out=new OutputStreamWriter(new FileOutputStream(file),"UTF-8")){out.write(log);}}catch(Exception ignored){}return;}
        SharedPreferences prefs=app.getSharedPreferences("call-diagnostics",Context.MODE_PRIVATE);String log=prefs.getString("events","")+line+"\n";if(log.length()>10000)log=log.substring(log.indexOf('\n',log.length()-8000)+1);prefs.edit().putString("events",log).commit();
    }
    private static String read(File file)throws IOException{if(!file.isFile())return "";StringBuilder s=new StringBuilder();try(Reader in=new InputStreamReader(new FileInputStream(file),"UTF-8")){char[] b=new char[2048];int n;while((n=in.read(b))!=-1&&s.length()<12000)s.append(b,0,n);}return s.toString();}
    static String report(Context c){StringBuilder s=new StringBuilder("LChat ").append(version(c)).append(" · диагностика звонка\n").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" · Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        for(String p:new String[]{Manifest.permission.RECORD_AUDIO})s.append(p.substring(p.lastIndexOf('.')+1)).append(": ").append(c.checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED?"да":"нет").append('\n');
        s.append("Режим: отдельный процесс аудио, без аккаунта Telecom\nСвязь с аудиопроцессом: ").append(CallClient.ready).append("\nАудиослужба: ").append(CallService.active).append("\nОчистка: ").append(CallService.closing).append("\nВходящие в фоне: ").append(InboxService.running).append("\nУведомления разрешены: ").append(c.getSystemService(android.app.NotificationManager.class).areNotificationsEnabled()).append("\n\n").append(c.getSharedPreferences("call-diagnostics",Context.MODE_PRIVATE).getString("events","Нет событий"));
        try{s.append("\nАудиопроцесс:\n").append(read(new File(c.getFilesDir(),"audio-diagnostics.log")));}catch(IOException ignored){}
        if(Build.VERSION.SDK_INT>=30)try{for(android.app.ApplicationExitInfo exit:c.getSystemService(android.app.ActivityManager.class).getHistoricalProcessExitReasons(c.getPackageName(),0,5))if(exit.getProcessName().endsWith(":calls")){s.append("\nВыход аудиопроцесса: reason=").append(exit.getReason()).append(" status=").append(exit.getStatus());if(Build.VERSION.SDK_INT>=31&&exit.getReason()==android.app.ApplicationExitInfo.REASON_CRASH_NATIVE)try(InputStream trace=exit.getTraceInputStream()){if(trace!=null)s.append("\n").append(CrashTrace.extract(trace));}catch(Exception ignored){s.append("\nДетали аварии недоступны");}}}catch(Exception ignored){}
        return s.toString();
    }
}
