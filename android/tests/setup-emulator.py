"""Install a private, checksum-verified API 31 emulator for LChat call QA."""
from pathlib import Path
import urllib.request,hashlib,zipfile,concurrent.futures,argparse
parser=argparse.ArgumentParser();parser.add_argument('--dest',type=Path,default=Path(__file__).resolve().parent.parent/'sdk/call-emulator-test')
root=parser.parse_args().dest;root.mkdir(parents=True,exist_ok=True)
packages=[('tools','https://dl.google.com/android/repository/emulator-windows_x64-16916650.zip','2cd8beaf83472f12e99a0554a2db3626db81f4ba'),('image','https://dl.google.com/android/repository/sys-img/google_atd/x86_64-31_r01.zip','feb32019317f4b71396db03d22e0f6a833f4e6fb')]
def install(item):
 name,url,digest=item;p=root/(name+'.zip')
 if not p.exists():
  print('Downloading '+name,flush=True)
  with urllib.request.urlopen(url,timeout=120) as response,p.open('wb') as output:
   while data:=response.read(1024*1024):output.write(data)
 assert hashlib.sha1(p.read_bytes()).hexdigest()==digest,'Checksum mismatch: '+name
 target=root/name
 with zipfile.ZipFile(p) as z:
  for entry in z.infolist():assert (target/entry.filename).resolve().is_relative_to(target.resolve())
  z.extractall(target)
 print('Installed and verified '+name,flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(install,packages))
