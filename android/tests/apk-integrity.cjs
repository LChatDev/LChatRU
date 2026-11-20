// Validate the produced archive and DEX independently of the signing tool.
const fs=require('node:fs'),zlib=require('node:zlib'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const file=process.argv[2];if(!file)throw new Error('Usage: node apk-integrity.cjs app.apk');
const apk=fs.readFileSync(file),table=new Uint32Array(256),entries=new Map();
for(let n=0;n<256;n++){let c=n;for(let k=0;k<8;k++)c=c&1?0xedb88320^(c>>>1):c>>>1;table[n]=c>>>0;}
function crc32(b){let c=0xffffffff;for(const byte of b)c=table[(c^byte)&255]^(c>>>8);return(c^0xffffffff)>>>0;}
let eocd=-1;for(let i=apk.length-22;i>=Math.max(0,apk.length-65557);i--)if(apk.readUInt32LE(i)===0x06054b50){eocd=i;break;}
assert(eocd>=0,'Missing ZIP directory');assert.equal(eocd+22+apk.readUInt16LE(eocd+20),apk.length,'Truncated APK');
const count=apk.readUInt16LE(eocd+10);let pos=apk.readUInt32LE(eocd+16);
for(let i=0;i<count;i++){
 assert.equal(apk.readUInt32LE(pos),0x02014b50);const flags=apk.readUInt16LE(pos+8),method=apk.readUInt16LE(pos+10),crc=apk.readUInt32LE(pos+16),size=apk.readUInt32LE(pos+20),length=apk.readUInt32LE(pos+24),nl=apk.readUInt16LE(pos+28),el=apk.readUInt16LE(pos+30),cl=apk.readUInt16LE(pos+32),offset=apk.readUInt32LE(pos+42),name=apk.toString('utf8',pos+46,pos+46+nl);
 assert(!entries.has(name),'Duplicate ZIP entry: '+name);assert(!(flags&1),'Encrypted entry');assert(!name.includes('..'),'Unsafe path');assert.equal(apk.readUInt32LE(offset),0x04034b50);
 assert.equal(apk.toString('utf8',offset+30,offset+30+apk.readUInt16LE(offset+26)),name,'Local filename mismatch');
 assert.equal(apk.readUInt16LE(offset+8),method,'Local compression mismatch');
 const start=offset+30+apk.readUInt16LE(offset+26)+apk.readUInt16LE(offset+28),compressed=apk.subarray(start,start+size);
 assert.equal(compressed.length,size);const data=method===0?compressed:method===8?zlib.inflateRawSync(compressed):null;assert(data,'Unsupported compression: '+name);assert.equal(data.length,length,'Size mismatch: '+name);assert.equal(crc32(data),crc,'CRC mismatch: '+name);entries.set(name,{data,method});pos+=46+nl+el+cl;
}
assert(entries.has('AndroidManifest.xml'));assert(entries.has('resources.arsc'));
for(const [name,{data}]of entries)if(/^classes\d*\.dex$/.test(name)){
 assert.match(data.toString('ascii',0,8),/^dex\n0\d\d\x00$/);assert.equal(data.readUInt32LE(32),data.length);
 assert.deepEqual(data.subarray(12,32),crypto.createHash('sha1').update(data.subarray(32)).digest(),'DEX signature');
 let a=1,b=0;for(let i=12;i<data.length;i++){a=(a+data[i])%65521;b=(b+a)%65521;}assert.equal(data.readUInt32LE(8),((b<<16)|a)>>>0,'DEX checksum');
}
const expected={'arm64-v8a':183,'armeabi-v7a':40,x86:3,x86_64:62};
for(const [abi,machine]of Object.entries(expected)){
 const item=entries.get(`lib/${abi}/libjingle_peerconnection_so.so`);assert(item,'Missing architecture '+abi);const elf=item.data;assert.equal(elf.readUInt32BE(0),0x7f454c46);assert.equal(elf.readUInt16LE(18),machine);assert.equal(item.method,8,'Expected compressed libraries for extraction');
}
assert(entries.has('META-INF/LCHAT.SF'));assert(entries.has('META-INF/LCHAT.RSA'));
console.log(`PASS: ${count} ZIP entries, CRCs, DEX SHA-1/checksum, all 4 native architectures, compressed library packaging.`);
console.log(`SHA256 ${crypto.createHash('sha256').update(apk).digest('hex')}`);
