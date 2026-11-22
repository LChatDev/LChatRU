param([string]$AppWork,[string]$JavaHome=$env:JAVA_HOME,[string]$QaOutput)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$qa=if($QaOutput){$QaOutput}else{Join-Path $root 'build/instrumentation'}
New-Item -ItemType Directory -Path $qa -Force | Out-Null
if(!$AppWork){$AppWork=(Get-ChildItem "$root/build" -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName}
if(!$JavaHome){throw 'Pass -JavaHome with the JDK directory.'}
$jdk=Join-Path $JavaHome 'bin'
$tools="$root/sdk/build-tools/android-15"
$android="$root/sdk/platform/android-35/android.jar"
$manifest=@'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="su.lbs.lchat.tests">
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35" />
<application android:label="LChat private call QA" />
<instrumentation android:name="su.lbs.lchat.CallScenarioRunner" android:targetPackage="su.lbs.lchat" android:functionalTest="true" />
</manifest>
'@
[IO.File]::WriteAllText("$qa/AndroidManifest.xml",$manifest)
New-Item -ItemType Directory -Path "$qa/classes","$qa/dex" -Force | Out-Null
& "$tools/aapt2.exe" link -o "$qa/base.apk" --manifest "$qa/AndroidManifest.xml" -I $android
if($LASTEXITCODE){throw 'Instrumentation aapt failed'}
& "$jdk/javac.exe" --release 8 -encoding UTF-8 -classpath "$android;$AppWork/app.jar" -d "$qa/classes" "$PSScriptRoot/CallScenarioRunner.java"
if($LASTEXITCODE){throw 'Instrumentation compile failed'}
& "$jdk/jar.exe" cf "$qa/tests.jar" -C "$qa/classes" .
& "$jdk/java.exe" -cp "$tools/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib $android --classpath "$AppWork/app.jar" --output "$qa/dex" "$qa/tests.jar"
if($LASTEXITCODE){throw 'Instrumentation dex failed'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$qa/base.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$qa/dex/classes.dex",'classes.dex')|Out-Null}finally{$zip.Dispose()}
& "$tools/zipalign.exe" -f 4 "$qa/base.apk" "$qa/aligned.apk"
$oldPassword=$env:LCHAT_SIGNING_PASSWORD
try{
$env:LCHAT_SIGNING_PASSWORD=[IO.File]::ReadAllText("$root/signing/password.txt").Trim()
& "$jdk/java.exe" -jar "$tools/lib/apksigner.jar" sign --ks "$root/signing/lchat-release.p12" --ks-key-alias lchat --ks-pass env:LCHAT_SIGNING_PASSWORD --out "$qa/tests.apk" "$qa/aligned.apk"
if($LASTEXITCODE){throw 'Instrumentation signing failed'}
}finally{$env:LCHAT_SIGNING_PASSWORD=$oldPassword}
Write-Output "Built private test APK: $qa/tests.apk"
