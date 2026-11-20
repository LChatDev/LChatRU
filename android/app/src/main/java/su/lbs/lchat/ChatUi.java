package su.lbs.lchat;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Native messenger layout. Views never own the call or its microphone. */
final class ChatUi {
    final NativeActivity a;
    static final int SURFACE=0xff17212b, BACKGROUND=0xff0e1621, TEXT=0xfff5f5f5, MUTED=0xff7f91a4, BLUE=0xff6ab3f3, OUT=0xff2b5278;
    LinearLayout toolbar, reply;
    TextView subtitle;
    int replyId;
    AlertDialog callDialog;
    TextView callName,callStatus,callClock,callPeople;
    Button mute,speaker;
    final ArrayList<Button> tabs=new ArrayList<>();
    String lastChats="",lastCall="";
    boolean forceScroll,historyScroll;
    ChatUi(NativeActivity activity){a=activity;}
    int dp(int x){return a.dp(x);}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    android.graphics.drawable.Drawable touch(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x207fffff),shape(color,radius),shape(-1,radius));}
    TextView text(String value,int size,int color){TextView v=new TextView(a);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(false);return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(a);l.setOrientation(1);return l;}
    LinearLayout.LayoutParams lp(int width,int height){return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));}
    void space(LinearLayout l,int h){l.addView(new View(a),lp(1,h));}
    View icon(String kind,String description,Runnable task){
        View v=new Glyph(a,kind,BLUE);v.setContentDescription(description);v.setBackground(touch(0x00000000,28));v.setOnClickListener(w->task.run());v.setLayoutParams(lp(48,48));return v;
    }
    void shell(String title){
        a.generation++;a.root=column();a.root.setBackgroundColor(BACKGROUND);a.root.setFitsSystemWindows(true);a.setContentView(a.root);
        toolbar=new LinearLayout(a);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(6),0,dp(6),0);toolbar.setBackgroundColor(SURFACE);
        boolean chat=a.screen.equals("chat");toolbar.addView(icon(chat?"back":"menu",chat?"Назад":"Меню",chat?a::home:a::settings));
        LinearLayout names=column();TextView heading=text(title,20,TEXT);heading.setTypeface(null,Typeface.BOLD);heading.setSingleLine(true);heading.setEllipsize(TextUtils.TruncateAt.END);names.addView(heading);
        subtitle=text(chat&&a.chat!=null?(a.chat.optString("kind").equals("direct")?"Личный чат":a.chat.optInt("member_count")+(a.chat.optString("kind").equals("channel")?" подписчиков":" участников")):"",13,MUTED);
        if(chat){space(names,3);names.addView(subtitle);names.setOnClickListener(v->info());}toolbar.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        if(chat&&a.chat!=null&&!a.adminChat&&!a.chat.optString("kind").equals("channel"))toolbar.addView(icon("phone","Позвонить",()->{int id=a.chatId;a.job(()->a.api.post("room.create",NativeApi.json("chat_id",id,"video",false)),d->a.call(d.optString("token"),"api.php",false));}));
        toolbar.addView(icon("more",chat?"Информация о чате":"Настройки",chat?this::info:a::settings));a.root.addView(toolbar,lp(-1,56));
        a.callCard=column();a.root.addView(a.callCard);a.error=a.label("",13);a.error.setVisibility(View.GONE);a.root.addView(a.error);a.body=column();a.root.addView(a.body,new LinearLayout.LayoutParams(-1,0,1));
        a.body.setAlpha(0);a.body.animate().alpha(1).setDuration(160).start();lastCall="";showCall();
    }
    void home(){
        a.screen="home";a.chatId=0;a.adminChat=false;a.messages.clear();a.composer=null;replyId=0;lastChats="";shell("LChat");
        EditText find=a.input("Поиск");find.setSingleLine(true);find.setText(a.search);find.setCompoundDrawablePadding(dp(10));a.add(a.body,find);
        find.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int x,int c,int f){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int x,int b,int c){a.search=s.toString();lastChats="";renderChats();}});
        LinearLayout strip=new LinearLayout(a);strip.setBackgroundColor(SURFACE);tabs.clear();String[] names={"Все чаты","Группы","Каналы"},kinds={"","group","channel"};
        for(int i=0;i<3;i++){String k=kinds[i];Button b=a.button(names[i],()->{a.filter=k;lastChats="";renderChats();});b.setTag(k);b.setTextSize(14);b.setBackground(touch(SURFACE,0));tabs.add(b);strip.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}a.body.addView(strip);
        FrameLayout frame=new FrameLayout(a);ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);a.list=column();scroll.addView(a.list);frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        View compose=icon("compose","Новый чат",a::create);compose.setBackground(touch(0xff5288c1,30));FrameLayout.LayoutParams fab=new FrameLayout.LayoutParams(dp(56),dp(56),Gravity.END|Gravity.BOTTOM);fab.setMargins(0,0,dp(20),dp(20));compose.setElevation(dp(6));frame.addView(compose,fab);a.body.addView(frame,new LinearLayout.LayoutParams(-1,0,1));renderChats();a.refresh(false);
    }
    void renderChats(){
        if(!a.screen.equals("home")||a.list==null)return;
        String signature=a.filter+"|"+a.search+"|"+a.chats.toString();if(signature.equals(lastChats))return;lastChats=signature;a.list.removeAllViews();
        for(Button b:tabs){boolean active=b.getTag().equals(a.filter);b.setTextColor(active?BLUE:MUTED);b.setTypeface(null,active?Typeface.BOLD:Typeface.NORMAL);}
        for(int i=0;i<a.chats.length();i++){
            JSONObject c=a.chats.optJSONObject(i);String title=c.optString("title","Чат"),kind=c.optString("kind");if((!a.filter.isEmpty()&&!a.filter.equals(kind))||!title.toLowerCase(Locale.ROOT).contains(a.search.toLowerCase(Locale.ROOT)))continue;
            LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(9),dp(12),dp(9));row.setBackground(touch(SURFACE,0));a.avatar(row,title,c.optJSONObject("peer")==null?0:c.optJSONObject("peer").optInt("id"));
            LinearLayout lines=column();lines.setPadding(dp(12),0,dp(8),0);TextView name=text((kind.equals("channel")?"▸ ":kind.equals("group")?"◈ ":"")+title,16,TEXT);name.setTypeface(null,Typeface.BOLD);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);lines.addView(name);space(lines,6);
            String preview=c.optString("last_message","");TextView snippet=text(preview.equals("null")||preview.isEmpty()?"Начните разговор":preview,15,MUTED);snippet.setSingleLine(true);snippet.setEllipsize(TextUtils.TruncateAt.END);lines.addView(snippet);row.addView(lines,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout right=column();right.setGravity(Gravity.END);right.addView(text(time(c.optString("last_message_at")),12,MUTED));int unread=c.optInt("unread");if(unread>0){space(right,7);TextView badge=text(unread>99?"99+":""+unread,12,TEXT);badge.setGravity(Gravity.CENTER);badge.setPadding(dp(7),dp(3),dp(7),dp(3));badge.setBackground(shape(0xff5288c1,12));right.addView(badge);}row.addView(right);row.setOnClickListener(v->a.open(c,false));a.list.addView(row,lp(-1,76));
            View divider=new View(a);divider.setBackgroundColor(0x10ffffff);LinearLayout.LayoutParams d=lp(-1,1);d.leftMargin=dp(78);a.list.addView(divider,d);
        }
        if(a.list.getChildCount()==0){TextView empty=text(a.search.isEmpty()?"Здесь появятся ваши разговоры":"Ничего не найдено",16,MUTED);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(24),dp(70),dp(24),dp(24));a.list.addView(empty);}
        space(a.list,80);
    }
    void open(JSONObject c,boolean admin){
        a.cancelRecording();a.stopPlayer();a.pendingBody="";a.pendingNonce="";a.screen="chat";a.chat=c;a.chatId=c.optInt("id");a.adminChat=admin;a.messages.clear();a.room=null;a.activeReplyKeyboard=null;a.replyKeyboardMessageId=0;a.replyKeyboardCollapsed=false;a.replyKeyboardPanel=null;a.replyKeyboardToggle=null;a.composer=null;replyId=0;forceScroll=true;historyScroll=false;
        shell(c.optString("title","Чат"));a.join=a.button("В группе идёт звонок · Присоединиться",()->{if(a.room!=null)a.call(a.room.optString("token"),"api.php",false);});a.join.setBackground(touch(0xff286c88,0));a.join.setTextColor(-1);a.join.setVisibility(View.GONE);a.body.addView(a.join,lp(-1,44));
        a.scroll=new ScrollView(a);a.scroll.setFillViewport(true);a.scroll.setBackground(new Wallpaper());a.bubbles=column();a.bubbles.setGravity(Gravity.BOTTOM);a.bubbles.setPadding(dp(8),dp(10),dp(8),dp(10));a.scroll.addView(a.bubbles);a.body.addView(a.scroll,new LinearLayout.LayoutParams(-1,0,1));
        boolean canWrite=admin||!c.optString("kind").equals("channel")||c.optInt("owner_id")==a.user.optInt("id");
        if(canWrite){
            reply=column();reply.setBackgroundColor(SURFACE);reply.setVisibility(View.GONE);a.body.addView(reply);
            a.replyKeyboardPanel=column();a.replyKeyboardPanel.setBackgroundColor(SURFACE);a.replyKeyboardPanel.setVisibility(View.GONE);a.body.addView(a.replyKeyboardPanel);
            LinearLayout compose=new LinearLayout(a);compose.setGravity(Gravity.BOTTOM);compose.setPadding(dp(3),dp(4),dp(3),dp(4));compose.setBackgroundColor(SURFACE);
            compose.addView(icon("attach","Прикрепить файл",()->a.pick(61)));a.replyKeyboardToggle=a.button("▦",()->{a.replyKeyboardCollapsed=!a.replyKeyboardCollapsed;a.renderBotKeyboard();});a.replyKeyboardToggle.setVisibility(View.GONE);compose.addView(a.replyKeyboardToggle,lp(40,48));
            a.composer=a.input(c.optString("kind").equals("channel")?"Публикация":"Сообщение");a.composer.setBackgroundColor(Color.TRANSPARENT);a.composer.setTextSize(16);a.composer.setMaxLines(5);a.composer.setInputType(1|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);compose.addView(a.composer,new LinearLayout.LayoutParams(0,-2,1));
            FrameLayout send=new FrameLayout(a);View voice=icon("mic","Записать голосовое",a::record),arrow=icon("send","Отправить сообщение",a::send);send.addView(voice,new FrameLayout.LayoutParams(-1,-1));send.addView(arrow,new FrameLayout.LayoutParams(-1,-1));arrow.setVisibility(View.GONE);compose.addView(send,lp(48,48));
            a.composer.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int x,int c,int f){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int x,int b,int c){boolean has=s.length()>0;voice.setVisibility(has?View.GONE:View.VISIBLE);arrow.setVisibility(has?View.VISIBLE:View.GONE);}});a.body.addView(compose);
        }else{TextView subscribed=text("Вы подписаны на канал",15,BLUE);subscribed.setGravity(Gravity.CENTER);subscribed.setBackgroundColor(SURFACE);a.body.addView(subscribed,lp(-1,52));}
        a.refresh(true);
    }
    static String time(String raw){return raw!=null&&raw.length()>=16?raw.substring(11,16):"";}
    void renderMessages(){
        boolean bottom=forceScroll||a.scroll.getChildAt(0).getHeight()-a.scroll.getHeight()-a.scroll.getScrollY()<dp(140);int oldY=a.scroll.getScrollY(),oldHeight=a.bubbles.getHeight();a.bubbles.removeAllViews();
        if(!a.messages.isEmpty()&&!a.adminChat)a.add(a.bubbles,a.button("Предыдущие сообщения",()->{int id=a.chatId,before=a.messages.firstKey();a.job(()->a.api.get("messages&chat_id="+id+"&before="+before),d->{JSONArray rows=d.optJSONArray("messages");if(rows!=null)for(int n=0;n<rows.length();n++){JSONObject m=rows.optJSONObject(n);a.messages.put(m.optInt("id"),m);}historyScroll=true;renderMessages();});}));
        String previousDay="";
        for(JSONObject m:a.messages.values()){
            String date=m.optString("created_at"),day=date.length()>=10?date.substring(0,10):"";if(!day.equals(previousDay)){TextView stamp=text(day,12,TEXT);stamp.setPadding(dp(10),dp(4),dp(10),dp(4));stamp.setBackground(shape(0x80445566,12));LinearLayout.LayoutParams p=lp(-2,-2);p.gravity=Gravity.CENTER;p.setMargins(0,dp(10),0,dp(8));a.bubbles.addView(stamp,p);previousDay=day;}
            boolean own=m.optInt("user_id")==a.user.optInt("id");LinearLayout bubble=column();bubble.setPadding(dp(10),dp(6),dp(10),dp(5));bubble.setBackground(shape(own?OUT:SURFACE,13));
            if(!own&&a.chat.optString("kind").equals("group")||m.optInt("moderator")>0){TextView name=text(m.optString("name")+(m.optInt("moderator")>0?" · администратор":""),13,BLUE);name.setTypeface(null,Typeface.BOLD);bubble.addView(name);space(bubble,4);}
            if(m.optInt("reply_to_id")>0){TextView quote=text(m.optString("reply_name")+"\n"+m.optString("reply_body"),13,BLUE);quote.setMaxLines(3);quote.setEllipsize(TextUtils.TruncateAt.END);quote.setPadding(dp(8),dp(5),dp(8),dp(5));quote.setBackground(shape(0x206ab3f3,4));bubble.addView(quote);space(bubble,5);}
            if(m.optInt("voice_duration")>0){Button voice=a.button("▶  Голосовое сообщение  ·  "+m.optInt("voice_duration")+" с",()->a.play(a.api.base+"download.php?play=1&id="+m.optInt("id")));bubble.addView(voice);}
            else{String full=m.optString("body");TextView message=text(full.length()>8192?full.substring(0,8192)+"…":full,16,TEXT);message.setMaxWidth(dp(480));message.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);message.setLinkTextColor(BLUE);bubble.addView(message);if(full.length()>8192)bubble.addView(a.button("Читать далее",()->a.showLongTextPage(full,0)));}
            if(!m.isNull("file_name")&&m.optInt("voice_duration")==0)bubble.addView(a.button("↓  "+m.optString("file_name"),()->a.download(m)));
            if(!m.isNull("invite_token"))bubble.addView(a.button("Открыть приглашение",()->a.joinInvite(m.optString("invite_kind"),m.optString("invite_token"))));
            if(!m.isNull("event_token")&&a.room!=null&&m.optString("event_token").equals(a.room.optString("token")))bubble.addView(a.button("Присоединиться к звонку",()->a.call(m.optString("event_token"),"api.php",false)));
            a.renderInlineButtons(bubble,m);JSONArray reactions=m.optJSONArray("reactions");if(reactions!=null&&reactions.length()>0){LinearLayout strip=new LinearLayout(a);for(int i=0;i<reactions.length();i++){JSONObject r=reactions.optJSONObject(i);Button chip=a.button(r.optString("emoji")+" "+r.optInt("count"),()->react(m,r.optString("emoji")));strip.addView(chip,lp(-2,36));}bubble.addView(strip);}
            TextView meta=text((m.isNull("edited_at")?"":"изменено · ")+time(date)+(own?"  ✓":""),11,own?0xffa5c6e5:MUTED);meta.setGravity(Gravity.END);bubble.addView(meta);
            bubble.setOnLongClickListener(v->{a.menu(m);return true;});LinearLayout.LayoutParams p=lp(-2,-2);p.gravity=own?Gravity.END:Gravity.START;p.setMargins(dp(own?42:0),dp(2),dp(own?0:42),dp(2));a.bubbles.addView(bubble,p);
        }
        final boolean history=historyScroll;historyScroll=false;forceScroll=false;a.scroll.post(()->{if(history)a.scroll.scrollTo(0,oldY+Math.max(0,a.bubbles.getHeight()-oldHeight));else if(bottom)a.scroll.fullScroll(View.FOCUS_DOWN);else a.scroll.scrollTo(0,oldY);});
    }
    void reply(JSONObject m){if(a.composer==null||a.adminChat)return;replyId=m.optInt("id");reply.removeAllViews();LinearLayout line=new LinearLayout(a);TextView preview=text("Ответ: "+m.optString("name")+"\n"+m.optString("body"),13,BLUE);preview.setPadding(dp(16),dp(8),0,dp(8));preview.setMaxLines(2);preview.setEllipsize(TextUtils.TruncateAt.END);line.addView(preview,new LinearLayout.LayoutParams(0,-2,1));line.addView(icon("close","Отменить ответ",this::clearReply));reply.addView(line);reply.setVisibility(View.VISIBLE);a.composer.requestFocus();}
    void clearReply(){replyId=0;if(reply!=null)reply.setVisibility(View.GONE);}
    void react(JSONObject m,String emoji){if(a.adminChat)return;int id=a.chatId;a.job(()->a.api.post("message.react",NativeApi.json("chat_id",id,"message_id",m.optInt("id"),"emoji",emoji)),d->a.refresh(false));}
    void menu(JSONObject m){
        ArrayList<String> options=new ArrayList<>();options.add("Копировать");if(!a.adminChat){options.add("Ответить");options.add("Реакция");}boolean own=m.optInt("user_id")==a.user.optInt("id");if(own&&!a.adminChat&&m.isNull("file_name")&&m.isNull("event_token")&&m.isNull("invite_token"))options.add("Изменить");boolean remove=a.adminChat||a.user.optBoolean("is_admin")||own||a.chat.optInt("owner_id")==a.user.optInt("id");if(remove)options.add("Удалить у всех");
        new AlertDialog.Builder(a).setItems(options.toArray(new String[0]),(d,i)->{String item=options.get(i);if(item.equals("Копировать")){String body=m.optString("body");if(body.length()>100000){a.toast("Текст слишком большой для буфера обмена");return;}((android.content.ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("LChat",body));}else if(item.equals("Ответить"))reply(m);else if(item.equals("Реакция")){String[] emoji={"👍","❤️","😂","😮","😢","👏"};new AlertDialog.Builder(a).setItems(emoji,(x,n)->react(m,emoji[n])).show();}else if(item.equals("Изменить")){EditText edit=a.input("Сообщение");edit.setText(m.optString("body"));new AlertDialog.Builder(a).setTitle("Редактировать сообщение").setView(edit).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",(x,n)->{int id=a.chatId;a.job(()->a.api.post("message.edit",NativeApi.json("chat_id",id,"message_id",m.optInt("id"),"body",edit.getText().toString())),z->a.refresh(false));}).show();}else new AlertDialog.Builder(a).setMessage("Удалить сообщение у всех?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(x,n)->{int id=a.chatId;boolean admin=a.adminChat;JSONObject data=NativeApi.json("chat_id",id,"message_id",m.optInt("id"));a.job(()->admin?a.api.admin("chat.delete",data):a.api.post("message.delete",data),z->{a.messages.remove(m.optInt("id"));renderMessages();});}).show();}).show();
    }
    void info(){
        if(a.chat==null)return;JSONObject c=a.chat;int id=c.optInt("id");String kind=c.optString("kind");boolean owner=c.optInt("owner_id")==a.user.optInt("id"),community=!kind.equals("direct");ArrayList<String> opts=new ArrayList<>();opts.add("Участники");opts.add("Файлы чата");if(community){opts.add("Поделиться ссылкой");opts.add("Пригласить контакт");if(owner){opts.add("Изменить название");opts.add(kind.equals("channel")?"Адрес канала":"Обновить ссылку");}else opts.add("Выйти");}
        new AlertDialog.Builder(a).setTitle(c.optString("title")).setItems(opts.toArray(new String[0]),(d,n)->{String option=opts.get(n);if(option.equals("Участники"))a.job(()->a.api.get("chat.info&chat_id="+id),r->a.choose(r.optJSONArray("members"),u->{if(owner&&community&&u.optInt("id")!=a.user.optInt("id"))new AlertDialog.Builder(a).setTitle(u.optString("name")).setMessage("@"+u.optString("username")).setNegativeButton("Закрыть",null).setPositiveButton("Исключить",(x,k)->new AlertDialog.Builder(a).setMessage("Исключить участника?").setNegativeButton("Отмена",null).setPositiveButton("Исключить",(y,j)->a.job(()->a.api.post(kind.equals("channel")?"channel.remove":"group.remove",NativeApi.json("chat_id",id,"user_id",u.optInt("id"))),z->a.toast("Участник удалён"))).show()).show();else a.toast("@"+u.optString("username"));}));
            else if(option.equals("Файлы чата")){ArrayList<JSONObject> files=new ArrayList<>();ArrayList<String> names=new ArrayList<>();for(JSONObject m:a.messages.values())if(!m.isNull("file_name")){files.add(m);names.add(m.optString("file_name"));}if(files.isEmpty())a.toast("Нет файлов в загруженных сообщениях");else new AlertDialog.Builder(a).setTitle("Файлы из загруженных сообщений").setItems(names.toArray(new String[0]),(x,k)->a.download(files.get(k))).show();}
            else if(option.equals("Поделиться ссылкой"))a.share(a.api.base+(kind.equals("channel")?android.net.Uri.encode(c.optString("channel_slug")):"index.php?invite="+c.optString("invite_token")));
            else if(option.equals("Пригласить контакт"))a.job(()->a.api.get("contacts"),r->a.choose(r.optJSONArray("contacts"),u->{String link=a.api.base+(kind.equals("channel")?android.net.Uri.encode(c.optString("channel_slug")):"index.php?invite="+c.optString("invite_token"));a.job(()->kind.equals("channel")?a.api.post("message.send",NativeApi.json("chat_id",u.optInt("chat_id"),"body","Приглашение в канал: "+link,"nonce",UUID.randomUUID().toString())):a.api.post("invite.send",NativeApi.json("kind","group","token",c.optString("invite_token"),"user_ids",new JSONArray().put(u.optInt("id")),"nonce",UUID.randomUUID().toString())),z->a.toast("Приглашение отправлено"));}));
            else if(option.equals("Изменить название"))a.prompt("Название",s->a.job(()->a.api.post("chat.update",NativeApi.json("chat_id",id,"title",s)),r->a.load(id)));
            else if(option.equals("Адрес канала"))a.prompt("Адрес канала",s->a.job(()->a.api.post("channel.update",NativeApi.json("chat_id",id,"slug",s)),r->a.load(id)));
            else if(option.equals("Обновить ссылку"))new AlertDialog.Builder(a).setMessage("Старая ссылка перестанет работать.").setNegativeButton("Отмена",null).setPositiveButton("Обновить",(x,k)->a.job(()->a.api.post("group.invite-reset",NativeApi.json("chat_id",id)),r->a.load(id))).show();
            else if(option.equals("Выйти"))new AlertDialog.Builder(a).setMessage("Выйти из чата?").setNegativeButton("Отмена",null).setPositiveButton("Выйти",(x,k)->a.job(()->a.api.post(kind.equals("channel")?"channel.leave":"group.leave",NativeApi.json("chat_id",id)),r->a.home())).show();
        }).show();
    }
    void showCall(){
        if(a.callCard==null)return;boolean active=CallService.active||CallService.launching;String signature=active+"|"+CallService.status+"|"+CallService.displayName+"|"+CallService.muted+"|"+CallService.speaker;
        if(!signature.equals(lastCall)){lastCall=signature;a.callCard.removeAllViews();if(active){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setBackgroundColor(0xff286c88);row.addView(icon("phone","Открыть звонок",this::openCall));TextView title=text(CallService.displayName+"\n"+CallService.status,13,-1);title.setMaxLines(2);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));row.addView(icon("close","Завершить звонок",()->a.command("end")));row.setOnClickListener(v->openCall());a.callCard.addView(row,lp(-1,56));}else if(!CallService.status.isEmpty()&&!CallService.status.equals("Звонок завершён")){TextView status=text(CallService.status,13,MUTED);status.setPadding(dp(16),dp(8),dp(16),dp(8));a.callCard.addView(status);}}
        if(callDialog!=null&&callDialog.isShowing()){callName.setText(CallService.displayName);callStatus.setText(CallService.status);long seconds=CallService.callStartedMillis>0?Math.max(0,(System.currentTimeMillis()-CallService.callStartedMillis)/1000):0;callClock.setText(active?String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60):"Завершён");mute.setText(CallService.muted?"Включить\nмикрофон":"Выключить\nмикрофон");speaker.setText(CallService.speaker?"Динамик\nвключён":"Динамик");callPeople.setText(CallService.participantSummary+(CallService.adminPresence.isEmpty()?"":"\nАдминистраторы: "+CallService.adminPresence));mute.setEnabled(active);speaker.setEnabled(active);}
    }
    void openCall(){
        if(callDialog!=null&&callDialog.isShowing())return;LinearLayout page=column();page.setGravity(Gravity.CENTER_HORIZONTAL);page.setPadding(dp(24),dp(24),dp(24),dp(32));page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff25485e,0xff17212b,0xff121d29}));
        TextView back=text("⌄  Свернуть",16,BLUE);back.setPadding(0,dp(8),0,dp(20));page.addView(back);space(page,32);TextView initial=text(CallService.displayName.isEmpty()?"L":CallService.displayName.substring(0,1),48,-1);initial.setGravity(Gravity.CENTER);initial.setBackground(shape(0xff5288c1,60));page.addView(initial,lp(112,112));space(page,24);callName=text(CallService.displayName,24,TEXT);callName.setGravity(Gravity.CENTER);page.addView(callName);space(page,12);callStatus=text("",14,0xffbbd6e9);callStatus.setGravity(Gravity.CENTER);page.addView(callStatus);callClock=text("",16,BLUE);callClock.setPadding(0,dp(12),0,dp(12));page.addView(callClock);
        ScrollView participants=new ScrollView(a);callPeople=text("",14,TEXT);callPeople.setGravity(Gravity.CENTER);participants.addView(callPeople);page.addView(participants,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls=new LinearLayout(a);mute=a.button("Микрофон",()->a.command("mic"));speaker=a.button("Динамик",()->a.command("speaker"));controls.addView(mute,new LinearLayout.LayoutParams(0,dp(64),1));controls.addView(speaker,new LinearLayout.LayoutParams(0,dp(64),1));page.addView(controls,lp(-1,-2));space(page,12);Button retry=a.button("Восстановить соединение",()->a.command("retry"));page.addView(retry,lp(-1,48));space(page,16);Button end=a.button("Завершить звонок",()->{a.command("end");callDialog.dismiss();});end.setBackground(touch(0xffe65161,28));end.setTextColor(-1);page.addView(end,lp(-1,56));
        callDialog=new AlertDialog.Builder(a).setView(page).create();back.setOnClickListener(v->callDialog.dismiss());callDialog.show();callDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);callDialog.getWindow().setLayout(-1,-1);showCall();
    }
    void destroy(){if(callDialog!=null)callDialog.dismiss();}
    static final class Wallpaper extends Drawable {
        final Paint p=new Paint(3);
        @Override public void draw(Canvas c){c.drawColor(BACKGROUND);p.setColor(0x0a98b9da);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);float unit=72*c.getDensity()/160f;if(unit<48)unit=72;for(float y=0;y<getBounds().height();y+=unit)for(float x=0;x<getBounds().width();x+=unit){c.save();c.translate(x+(y/unit%2)*unit/2,y);c.rotate(25);c.drawRoundRect(-9,-9,9,9,5,5,p);c.drawCircle(unit/2,unit/3,4,p);c.restore();}}
        @Override public void setAlpha(int alpha){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
    static final class Glyph extends View {
        final String kind;final Paint p=new Paint(3);
        Glyph(Context context,String icon,int color){super(context);kind=icon;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
        void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,p);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.translate(getWidth()/2f,getHeight()/2f);float scale=getResources().getDisplayMetrics().density;c.scale(scale,scale);switch(kind){
            case "menu":line(c,-9,-6,9,-6);line(c,-9,0,9,0);line(c,-9,6,9,6);break;
            case "back":line(c,8,0,-8,0);line(c,-8,0,-1,-7);line(c,-8,0,-1,7);break;
            case "close":line(c,-6,-6,6,6);line(c,-6,6,6,-6);break;
            case "more":c.drawCircle(0,-7,1,p);c.drawCircle(0,0,1,p);c.drawCircle(0,7,1,p);break;
            case "mic":c.drawRoundRect(-3,-10,3,3,3,3,p);c.drawArc(-7,-5,7,7,0,180,false,p);line(c,-7,-5,-7,0);line(c,7,-5,7,0);line(c,0,7,0,10);break;
            case "send":{Path path=new Path();path.moveTo(-9,-8);path.lineTo(10,0);path.lineTo(-9,8);path.lineTo(-5,0);path.close();c.drawPath(path,p);line(c,-5,0,5,0);break;}
            case "attach":c.save();c.rotate(35);c.drawRoundRect(-5,-9,5,9,5,5,p);c.drawRoundRect(-1,-8,2,6,2,2,p);c.restore();break;
            case "compose":line(c,-7,7,7,-7);line(c,-8,3,-8,8);line(c,-8,8,-3,8);line(c,3,-8,8,-3);break;
            case "phone":{Path path=new Path();path.moveTo(-7,-9);path.lineTo(-3,-9);path.lineTo(-1,-4);path.lineTo(-4,-2);path.quadTo(-1,4,3,4);path.lineTo(5,1);path.lineTo(10,3);path.lineTo(10,7);path.quadTo(-4,12,-10,-5);path.close();c.drawPath(path,p);break;}
        }c.restore();}
    }
}
