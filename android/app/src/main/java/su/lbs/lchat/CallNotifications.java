package su.lbs.lchat;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;

/** Android supplies the incoming call layout. Accept always opens a visible Activity first. */
final class CallNotifications {
    static Notification incoming(Context c,String channel,String token,String name){
        PendingIntent open=PendingIntent.getActivity(c,90,new Intent(c,NativeActivity.class).setAction("incoming-call").setData(Uri.parse("lchat-incoming:"+token)).putExtra("token",token),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent answer=PendingIntent.getActivity(c,91,new Intent(c,NativeActivity.class).setAction("accept-call").setData(Uri.parse("lchat-answer:"+token)).putExtra("token",token),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent decline=PendingIntent.getService(c,92,new Intent(c,InboxService.class).setAction("decline-"+token).putExtra("command","decline").putExtra("token",token),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_lchat).setContentTitle(name).setContentText("Входящий интернет-звонок · LChat").setContentIntent(open).setCategory(Notification.CATEGORY_CALL).setVisibility(Notification.VISIBILITY_PRIVATE).setOngoing(true).setOnlyAlertOnce(true).setTimeoutAfter(90000);
        if(Build.VERSION.SDK_INT>=31)b.setStyle(Notification.CallStyle.forIncomingCall(new Person.Builder().setName(name).setImportant(true).build(),decline,answer));else b.addAction(new Notification.Action.Builder(null,"Отклонить",decline).build()).addAction(new Notification.Action.Builder(null,"Принять",answer).build());
        Notification result=b.build();result.flags|=Notification.FLAG_INSISTENT;return result;
    }
}
