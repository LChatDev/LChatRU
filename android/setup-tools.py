"""Fetch pinned Android build inputs from Google and Maven Central."""
from pathlib import Path
import hashlib
import urllib.request
import zipfile

root = Path(__file__).resolve().parent / 'sdk'
root.mkdir(exist_ok=True)
packages = [
    ('platform', 'https://dl.google.com/android/repository/platform-35_r02.zip', '0bb560a90a7a2cbd0dd8348224d518b638fe7949'),
    ('build-tools', 'https://dl.google.com/android/repository/build-tools_r35_windows.zip', 'af059bb67cf7786f45ee0db85e2d24985df1b4b6'),
    ('webrtc-150.7871.08', 'https://repo.maven.apache.org/maven2/io/github/webrtc-sdk/android/150.7871.08/android-150.7871.08.aar', None),
]
for name, url, sha1 in packages:
    archive = root / (name + '.zip')
    expected = sha1 or urllib.request.urlopen(url + '.sha1', timeout=45).read().decode().strip()
    if not archive.exists():
        print('Downloading ' + name, flush=True)
        with urllib.request.urlopen(url, timeout=120) as response, archive.open('wb') as output:
            while chunk := response.read(1024 * 1024): output.write(chunk)
    if hashlib.sha1(archive.read_bytes()).hexdigest() != expected:
        raise RuntimeError('Checksum mismatch: ' + name)
    target = root / name
    with zipfile.ZipFile(archive) as z:
        for item in z.infolist():
            if not (target / item.filename).resolve().is_relative_to(target.resolve()):
                raise RuntimeError('Unsafe ZIP path')
        z.extractall(target)
    print('Verified and extracted ' + name, flush=True)
