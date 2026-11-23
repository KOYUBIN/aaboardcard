# AA 게시글 메이커 (@aa_board_card)

보드게임·홀덤 인스타그램 게시글(캐러셀·릴스 커버)과 릴스 영상(MP4)을 만드는 툴.

MP4 생성에 [mp4-muxer](https://github.com/Vanilagy/mp4-muxer) (MIT)를 포함한다.

| 경로 | 내용 |
|---|---|
| `web/insta_post_maker.html` | PC용 단일 파일. 받아서 더블클릭 |
| `dist/AA_post_maker.apk` | 안드로이드 앱 (Android 10 이상) |
| `dist/index.html` | 앱이 내려받는 화면 파일 |
| `dist/version.json` | 앱이 확인하는 버전 정보 |
| `android/` | 앱 소스 (웹뷰 + 사진 선택 + 사진첩 저장 + 업데이트) |

## 업데이트 방식

앱은 실행할 때 `dist/version.json`을 읽는다.

- `web` 숫자가 앱보다 크면 `dist/index.html`을 내려받고, 세트 탭에서 "지금 적용"을 누르면 바뀐다. 재설치 불필요.
- `apk` 숫자가 앱보다 크면 "새 앱 받기" 버튼이 뜨고, 누르면 `apkUrl`의 APK를 브라우저로 받는다.

이 방식은 저장소가 **공개(public)** 일 때만 동작한다.

## 새 버전 올리는 순서

1. `web/insta_post_maker.html` 수정
2. `android/build.sh <새 web 번호>` 실행 → `dist/index.html`, APK 생성
3. `dist/version.json`의 `web`(화면만 바뀐 경우) 또는 `apk`(앱 자체가 바뀐 경우) 숫자를 올림
4. 커밋 후 push

APK 서명 키(`key.jks`)는 저장소에 올리지 않는다. 같은 키로 서명해야 덮어쓰기 설치가 된다.
