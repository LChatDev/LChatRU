param(
    [Parameter(Mandatory=$true)][string]$JavaHome,
    [Parameter(Mandatory=$true)][string]$BuildTools,
    [Parameter(Mandatory=$true)][string]$AndroidJar,
    [Parameter(Mandatory=$true)][string]$WebRtcDir
)
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
[xml]$manifest=Get-Content -LiteralPath (Join-Path $root 'app/src/main/AndroidManifest.xml') -Raw
$androidNs='http://schemas.android.com/apk/res/android'
$minSdk=[int]$manifest.manifest.'uses-sdk'.GetAttribute('minSdkVersion',$androidNs)
$targetSdk=[int]$manifest.manifest.'uses-sdk'.GetAttribute('targetSdkVersion',$androidNs)
if($minSdk -lt 26 -or $targetSdk -lt $minSdk){throw 'Invalid supported Android SDK range.'}
$abis=@('arm64-v8a','armeabi-v7a','x86','x86_64')
$work=Join-Path $root ('build/work-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
foreach($name in @('classes','generated','dex','assets')){New-Item -ItemType Directory -Path (Join-Path $work $name) -Force | Out-Null}
function Check-Exit {if($LASTEXITCODE -ne 0){throw "Build command failed with code $LASTEXITCODE"}}
$aapt=Join-Path $BuildTools 'aapt2.exe'
$classesJar=Join-Path $WebRtcDir 'classes.jar'
foreach($path in @($aapt,$AndroidJar,$classesJar,(Join-Path $JavaHome 'bin/javac.exe'))){if(!(Test-Path -LiteralPath $path)){throw "Missing build tool: $path"}}
Copy-Item -LiteralPath (Join-Path $root 'licenses') -Destination (Join-Path $work 'assets') -Recurse
& $aapt compile --dir (Join-Path $root 'app/src/main/res') -o (Join-Path $work 'resources.zip');Check-Exit
& $aapt link -o (Join-Path $work 'base.apk') --manifest (Join-Path $root 'app/src/main/AndroidManifest.xml') -I $AndroidJar --java (Join-Path $work 'generated') -A (Join-Path $work 'assets') --min-sdk-version $minSdk --target-sdk-version $targetSdk (Join-Path $work 'resources.zip');Check-Exit
$sources=@(Get-ChildItem (Join-Path $root 'app/src/main/java') -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})+@(Get-ChildItem (Join-Path $work 'generated') -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
& (Join-Path $JavaHome 'bin/javac.exe') --release 8 -encoding UTF-8 -classpath "$AndroidJar;$classesJar" -d (Join-Path $work 'classes') @sources;Check-Exit
& (Join-Path $JavaHome 'bin/jar.exe') cf (Join-Path $work 'app.jar') -C (Join-Path $work 'classes') .;Check-Exit
& (Join-Path $JavaHome 'bin/java.exe') -cp (Join-Path $BuildTools 'lib/d8.jar') com.android.tools.r8.D8 --release --min-api $minSdk --lib $AndroidJar --output (Join-Path $work 'dex') (Join-Path $work 'app.jar') $classesJar;Check-Exit
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::Open((Join-Path $work 'base.apk'),[IO.Compression.ZipArchiveMode]::Update)
try{
    foreach($dex in Get-ChildItem (Join-Path $work 'dex') -Filter '*.dex'){[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$dex.FullName,$dex.Name,[IO.Compression.CompressionLevel]::Optimal) | Out-Null}
    foreach($abi in $abis){
        $lib=Join-Path $WebRtcDir "jni/$abi/libjingle_peerconnection_so.so"
        if(!(Test-Path $lib)){throw "Missing native WebRTC library: $abi"}
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$lib,"lib/$abi/libjingle_peerconnection_so.so",[IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
}finally{$archive.Dispose()}
& (Join-Path $BuildTools 'zipalign.exe') -P 16 -f 4 (Join-Path $work 'base.apk') (Join-Path $work 'aligned.apk');Check-Exit
$signing=Join-Path $root 'signing';New-Item -ItemType Directory -Path $signing -Force | Out-Null
$keystore=Join-Path $signing 'lchat-release.p12';$passfile=Join-Path $signing 'password.txt'
$oldPassword=$env:LCHAT_SIGNING_PASSWORD
try{
    if(!(Test-Path $passfile)){
        if(Test-Path $keystore){throw 'Signing password is missing. Restore signing/password.txt.'}
        $bytes=New-Object byte[] 32;[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
        [IO.File]::WriteAllText($passfile,[Convert]::ToBase64String($bytes),[Text.UTF8Encoding]::new($false))
    }
    $env:LCHAT_SIGNING_PASSWORD=[IO.File]::ReadAllText($passfile).Trim()
    if(!(Test-Path $keystore)){
        & (Join-Path $JavaHome 'bin/keytool.exe') -genkeypair -keystore $keystore -storetype PKCS12 -storepass:env LCHAT_SIGNING_PASSWORD -keypass:env LCHAT_SIGNING_PASSWORD -alias lchat -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=LChat, OU=Selfhost, O=LChat';Check-Exit
    }
    & (Join-Path $JavaHome 'bin/java.exe') -jar (Join-Path $BuildTools 'lib/apksigner.jar') sign --ks $keystore --ks-key-alias lchat --ks-pass env:LCHAT_SIGNING_PASSWORD --key-pass env:LCHAT_SIGNING_PASSWORD --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out (Join-Path $work 'signed.apk') (Join-Path $work 'aligned.apk');Check-Exit
}finally{$env:LCHAT_SIGNING_PASSWORD=$oldPassword}
& (Join-Path $JavaHome 'bin/java.exe') -jar (Join-Path $BuildTools 'lib/apksigner.jar') verify --verbose (Join-Path $work 'signed.apk');Check-Exit
& (Join-Path $JavaHome 'bin/java.exe') -jar (Join-Path $BuildTools 'lib/apksigner.jar') verify --verbose --min-sdk-version 31 --max-sdk-version 32 (Join-Path $work 'signed.apk');Check-Exit
& (Join-Path $BuildTools 'zipalign.exe') -c -P 16 4 (Join-Path $work 'signed.apk');Check-Exit
Copy-Item -LiteralPath (Join-Path $work 'signed.apk') -Destination (Join-Path $root 'LChat.apk') -Force
$version=$manifest.manifest.GetAttribute('versionName',$androidNs)
Copy-Item -LiteralPath (Join-Path $work 'signed.apk') -Destination (Join-Path $root "LChat-$version-universal.apk") -Force
$digest=(Get-FileHash -LiteralPath (Join-Path $root "LChat-$version-universal.apk") -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $root "LChat-$version-universal.apk.sha256"),"$digest  LChat-$version-universal.apk`n",[Text.UTF8Encoding]::new($false))
Write-Output "Built and verified: Android API $minSdk+, universal ARM32/ARM64/x86/x86_64 APK."
