const { chromium }=require('playwright');
const fs=require('node:fs');
const path=require('node:path');
(async()=>{
 const config=JSON.parse(fs.readFileSync(path.join(__dirname,'../lchat-call-scenario.json'),'utf8'));
 const browser=await chromium.launch({channel:'chrome',headless:true,args:['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
 const page=await browser.newPage();page.on('pageerror',e=>console.log('Browser page error:',e.message.slice(0,180)));
 await page.goto(config.base+'api.php?action=me',{waitUntil:'load'});
 const setup=await page.evaluate(async({token})=>{
  const api=async(action,data)=>{const r=await fetch('api.php?action='+action,{method:data?'POST':'GET',headers:data?{'Content-Type':'application/json','X-CSRF-Token':window.qa.csrf}:{},body:data?JSON.stringify(data):undefined});const d=await r.json();if(!r.ok)throw new Error(action+': '+d.error);return d;};
  window.qa={token,csrf:'',peers:new Map(),after:0,errors:[],polls:0};const q=window.qa;q.csrf=(await api('me')).csrf;
  const joined=await api('room.join',{token,name:'LChat browser QA'});q.id=joined.id;q.ice=joined.ice_servers;q.transport=joined.ice_transport_policy||'all';
  q.stream=await navigator.mediaDevices.getUserMedia({audio:true});
  const send=async(to,payload)=>api('room.signal',{token,id:q.id,to,payload});
  const offer=async(p,restart=false)=>{if(p.offering)return;p.offering=true;try{if(restart&&p.pc.signalingState==='have-local-offer')await p.pc.setLocalDescription({type:'rollback'});await p.pc.setLocalDescription(await p.pc.createOffer(restart?{iceRestart:true}:{}));await send(p.id,{type:'offer',sdp:p.pc.localDescription.sdp});p.offered=true;}finally{p.offering=false;}};
  const peer=id=>{if(q.peers.has(id))return q.peers.get(id);const pc=new RTCPeerConnection({iceServers:q.ice,iceTransportPolicy:q.transport});pc.addTrack(q.stream.getAudioTracks()[0],q.stream);const p={id,pc,candidates:[],offering:false,offered:false};q.peers.set(id,p);pc.onicecandidate=e=>{if(e.candidate)send(id,{type:'candidate',candidate:e.candidate.toJSON()}).catch(e=>q.errors.push(e.message));};pc.ontrack=e=>{const a=document.createElement('audio');a.autoplay=true;a.srcObject=e.streams[0]||new MediaStream([e.track]);document.body.append(a);a.play().catch(()=>{});};return p;};
  q.tick=async()=>{try{
    const d=await api('room.poll',{token,id:q.id,after:q.after,muted:false,camera:false});q.polls++;
    const present=new Set(d.peers.map(x=>x.id));for(const [id,p]of q.peers)if(!present.has(id)){p.pc.close();q.peers.delete(id);}
    for(const info of d.peers){const p=peer(info.id);if(q.id<info.id&&!p.offered&&!p.offering)await offer(p);}
    for(const event of d.signals){const p=q.peers.get(event.sender),s=event.payload;if(p){
      if(s.type==='candidate'){if(p.pc.remoteDescription)await p.pc.addIceCandidate(s.candidate);else p.candidates.push(s.candidate);}
      else if(s.type==='restart'){if(q.id<p.id)await offer(p,true);}
      else if(s.type==='offer'&&q.id>p.id){await p.pc.setRemoteDescription({type:'offer',sdp:s.sdp});for(const c of p.candidates.splice(0))await p.pc.addIceCandidate(c);await p.pc.setLocalDescription(await p.pc.createAnswer());await send(p.id,{type:'answer',sdp:p.pc.localDescription.sdp});}
      else if(s.type==='answer'&&p.pc.signalingState==='have-local-offer'){await p.pc.setRemoteDescription({type:'answer',sdp:s.sdp});for(const c of p.candidates.splice(0))await p.pc.addIceCandidate(c);}
    }q.after=Math.max(q.after,event.id);}
  }catch(e){q.errors.push(e.message);}q.timer=setTimeout(q.tick,600);};q.tick();
  return {joined:true,policy:q.transport,iceServers:q.ice.length};
 },{token:config.token});console.log(JSON.stringify(setup));
 for(let i=0;i<450;i++){
   if(fs.existsSync(path.join(__dirname,'../qa.stop')))break;
   await new Promise(r=>setTimeout(r,2000));
   const report=await page.evaluate(async()=>{let tx=0,rx=0,connected=0;for(const p of qa.peers.values()){if(p.pc.connectionState==='connected')connected++;for(const s of(await p.pc.getStats()).values()){if(s.type==='outbound-rtp'&&s.kind==='audio')tx+=s.packetsSent||0;if(s.type==='inbound-rtp'&&s.kind==='audio')rx+=s.packetsReceived||0;}}return {connected,tx,rx,polls:qa.polls,errors:qa.errors.slice(-3)};});
   fs.writeFileSync(path.join(__dirname,'../browser-stats.json'),JSON.stringify(report));
   if(i%5===0)console.log(JSON.stringify(report));
 }
 await page.evaluate(async()=>{clearTimeout(qa.timer);for(const p of qa.peers.values())p.pc.close();qa.stream.getTracks().forEach(t=>t.stop());await fetch('api.php?action=room.leave',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Token':qa.csrf},body:JSON.stringify({token:qa.token,id:qa.id})});});
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});
