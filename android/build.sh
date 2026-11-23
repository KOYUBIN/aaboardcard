#!/bin/bash
# 사용: build.sh <웹버전번호>   (standalone html -> assets/index.html -> 서명된 APK)
set -e
WEBV=${1:?web version}
ROOT=/home/claude/android; APP=$ROOT/app; B=$APP/build
BT=$ROOT/sdk/build-tools/35.0.0; AJ=$ROOT/sdk/platforms/android-34/android.jar; K=$ROOT/key.jks
python3 - <<PY
import re
css=open('$ROOT/fonts/node_modules/@fontsource/jua/400.css').read()
css=re.sub(r"url\(\./files/([^)]+\.woff2)\) format\('woff2'\),\s*url\([^)]+\.woff\) format\('woff'\)",r"url(fonts/jua/\1) format('woff2')",css)
local="<style>@font-face{font-family:Pretendard;font-weight:100 900;font-display:swap;src:url(fonts/PretendardVariable.woff2) format('woff2')}\n"+css+"</style>"
s=open('/home/claude/out/insta_post_maker.html',encoding='utf-8').read()
s,n=re.subn(r'<link rel="stylesheet" href="https://[^"]+">\n?','',s); assert n==2
s=s.replace('</title>','</title>\n'+local,1)
open('$APP/assets/index.html','w',encoding='utf-8').write(s)
PY
rm -rf $B && mkdir -p $B/cls $B/src
sed "s/__WEBV__/$WEBV/" $APP/src/com/aaboardcard/postmaker/MainActivity.java > $B/src/MainActivity.java
$BT/aapt2 compile --dir $APP/res -o $B/res.zip
$BT/aapt2 link -o $B/base.apk -I $AJ --manifest $APP/AndroidManifest.xml -A $APP/assets --java $B/gen $B/res.zip
javac -nowarn -Xlint:-options --release 8 -cp $AJ -d $B/cls $B/gen/com/aaboardcard/postmaker/R.java $B/src/MainActivity.java 2>&1 | grep -vE "Picked up|^Note" || true
$BT/d8 --release --min-api 29 --lib $AJ --output $B $(find $B/cls -name '*.class') 2>&1 | grep -v "Picked up" | head -5 || true
cp $B/base.apk $B/un.apk && (cd $B && zip -q un.apk classes.dex)
$BT/zipalign -f 4 $B/un.apk $B/al.apk
$BT/apksigner sign --ks $K --ks-pass pass:${KS_PASS:?서명 키 비밀번호} --key-pass pass:${KS_PASS:?서명 키 비밀번호} --out $B/AA_post_maker.apk $B/al.apk 2>&1 | grep -v "Picked up" || true
$BT/apksigner verify --verbose $B/AA_post_maker.apk 2>&1 | grep -E "^Verifies|v3 scheme"
$BT/aapt2 dump badging $B/AA_post_maker.apk | grep -E "^package|uses-permission"
ls -la $B/AA_post_maker.apk
