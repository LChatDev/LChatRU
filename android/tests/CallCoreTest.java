package su.lbs.lchat;

import java.util.*;

/** Host-JVM regression checks for the actual production lifetime and outbox. */
public final class CallCoreTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        CallLifecycle call=new CallLifecycle();check(call.join(),"first admission");check(!call.join(),"duplicate start must not join twice");check(call.admitted(),"admission");
        check(call.connected(100),"connection");check(call.reconnect(),"network interruption");check(call.connected(300),"reconnection");check(call.connectedAt()==100,"reconnect must not reset duration");
        call.waiting();check(call.phase()==CallLifecycle.Phase.CONNECTING,"waiting for replacement participant");call.connected(350);check(call.connectedAt()==100,"waiting must not reset duration");
        check(call.stop(),"first hangup");check(!call.stop(),"hangup must be idempotent");check(!call.connected(400),"late native callback cannot revive a stopped call");check(!call.waiting(),"late peer removal cannot revive a stopped call");check(!call.admitted(),"late HTTP admission cannot revive a stopped call");call.ended();check(!call.live(),"terminal state");
        CallLifecycle cancelled=new CallLifecycle();cancelled.join();cancelled.stop();check(!cancelled.admitted(),"cancel during server join");
        CallOutbox<String> outbox=new CallOutbox<>();outbox.add("offer",20000);outbox.add("candidate1",1000);List<String> first=outbox.begin();check(first.equals(Arrays.asList("offer","candidate1")),"wire order");
        check(outbox.begin().isEmpty(),"one in-flight batch");outbox.add("candidate2",1000);outbox.retry();check(outbox.begin().equals(Arrays.asList("offer","candidate1","candidate2")),"retry must precede newer signals");outbox.acknowledge();check(outbox.empty(),"ack removes in-flight items");
        outbox.add("large",48000);outbox.add("small",3000);check(outbox.begin().equals(Collections.singletonList("large")),"request byte budget");outbox.acknowledge();check(outbox.begin().equals(Collections.singletonList("small")),"next request");outbox.clear();
        check(!outbox.add("oversized",48001),"server packet limit");for(int i=0;i<512;i++)check(outbox.add("x",1),"bounded capacity");check(!outbox.add("overflow",1),"outbox cannot grow forever");check(outbox.begin().size()==24,"server batch count limit");outbox.clear();check(outbox.empty(),"hangup clears pending and in-flight signals");
        System.out.println("PASS: duplicate admission, cancellation, late callbacks, reconnect clock, idempotent hangup, signal ordering/retry/count/size/capacity");
    }
}
