package su.lbs.lchat;

import java.util.*;

/** Bounded FIFO that retains wire order across retry. Accessed only by the RTC thread. */
final class CallOutbox<T> {
    private static final class Entry<T>{final T value;final int bytes;Entry(T value,int bytes){this.value=value;this.bytes=bytes;}}
    private final Deque<Entry<T>> pending=new ArrayDeque<>();
    private List<Entry<T>> sending=Collections.emptyList();
    boolean add(T value,int bytes){if(bytes>48000||bytes<0||pending.size()+sending.size()>=512)return false;pending.addLast(new Entry<>(value,bytes));return true;}
    List<T> begin(){
        if(!sending.isEmpty()||pending.isEmpty())return Collections.emptyList();
        List<Entry<T>> batch=new ArrayList<>();int bytes=0;
        while(!pending.isEmpty()&&batch.size()<24){Entry<T> next=pending.peekFirst();if(!batch.isEmpty()&&bytes+next.bytes>50000)break;pending.removeFirst();batch.add(next);bytes+=next.bytes;}
        sending=batch;List<T> values=new ArrayList<>();for(Entry<T> item:batch)values.add(item.value);return values;
    }
    void acknowledge(){sending=Collections.emptyList();}
    void retry(){for(int i=sending.size()-1;i>=0;i--)pending.addFirst(sending.get(i));sending=Collections.emptyList();}
    boolean busy(){return !sending.isEmpty();}
    boolean empty(){return sending.isEmpty()&&pending.isEmpty();}
    void clear(){pending.clear();sending=Collections.emptyList();}
}
