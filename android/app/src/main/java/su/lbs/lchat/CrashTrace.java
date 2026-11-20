package su.lbs.lchat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Extract only a short native abort message and crashing-thread symbols, never memory or logs. */
final class CrashTrace {
    private static final int LIMIT=8*1024*1024;
    private static final class Field {int number, wire; long value; byte[] bytes;}
    private static List<Field> fields(byte[] data)throws IOException{
        ArrayList<Field> result=new ArrayList<>();int[] pos={0};
        while(pos[0]<data.length){
            long tag=varint(data,pos);int wire=(int)(tag&7),number=(int)(tag>>>3);
            if(number==0)throw new IOException("Invalid field");Field f=new Field();f.number=number;f.wire=wire;
            if(wire==0)f.value=varint(data,pos);
            else if(wire==1)skip(data,pos,8);
            else if(wire==2){long n=varint(data,pos);if(n<0||n>data.length-pos[0])throw new IOException("Invalid length");f.bytes=Arrays.copyOfRange(data,pos[0],pos[0]+(int)n);pos[0]+=(int)n;}
            else if(wire==5)skip(data,pos,4);else throw new IOException("Unsupported wire type");
            result.add(f);
        }return result;
    }
    private static long varint(byte[] b,int[] p)throws IOException{long value=0;for(int shift=0;shift<64;shift+=7){if(p[0]>=b.length)throw new IOException("Truncated varint");int v=b[p[0]++]&255;value|=(long)(v&127)<<shift;if((v&128)==0)return value;}throw new IOException("Invalid varint");}
    private static void skip(byte[] b,int[] p,int n)throws IOException{if(n>b.length-p[0])throw new IOException("Truncated field");p[0]+=n;}
    private static String safe(byte[] bytes){String s=new String(bytes,StandardCharsets.UTF_8).replaceAll("[\\r\\n\\t]+"," ").replaceAll("https?://\\S+","[url]").replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+","[email]").replaceAll("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b","[ip]");return s.length()>500?s.substring(0,500):s;}
    static String extract(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
        while((n=in.read(buf))!=-1){if(out.size()+n>LIMIT)throw new IOException("Trace too large");out.write(buf,0,n);}
        List<Field> top=fields(out.toByteArray());long crashTid=-1;String message="";
        for(Field f:top){if(f.number==6&&f.wire==0)crashTid=f.value;if(f.number==14&&f.bytes!=null)message=safe(f.bytes);}
        StringBuilder report=new StringBuilder();if(!message.isEmpty())report.append("Причина аварии: ").append(message).append('\n');
        for(Field entry:top)if(entry.number==16&&entry.bytes!=null){
            long tid=-2;byte[] thread=null;for(Field item:fields(entry.bytes)){if(item.number==1&&item.wire==0)tid=item.value;if(item.number==2&&item.bytes!=null)thread=item.bytes;}
            if(tid!=crashTid||thread==null)continue;int count=0;
            for(Field frame:fields(thread))if(frame.number==4&&frame.bytes!=null){String function="",file="";long relative=0;for(Field part:fields(frame.bytes)){if(part.number==1&&part.wire==0)relative=part.value;if(part.number==4&&part.bytes!=null)function=safe(part.bytes);if(part.number==6&&part.bytes!=null){file=safe(part.bytes);int slash=file.lastIndexOf('/');if(slash>=0)file=file.substring(slash+1);}}if(!function.isEmpty()||!file.isEmpty()){report.append("  ").append(file).append(" +0x").append(Long.toHexString(relative)).append(" ").append(function).append('\n');if(++count==12)break;}}
            break;
        }
        return report.length()==0?"Текст аварии недоступен":report.toString().trim();
    }
}
