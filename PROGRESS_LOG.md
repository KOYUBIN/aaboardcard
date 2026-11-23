# 진행 로그

## 2026-11-23 — Instagram 스킬 팩 설치

- 출처: https://github.com/Jakeschincariol/instagram-agent-skill (커밋 `d03c56b`, MIT 라이선스)
- 설치 위치: `.claude/skills/ig-*` (13개 스킬, 프로젝트 로컬 설치). 라이선스 사본: `.claude/skills/LICENSE-instagram-agent-skill`
- 설치 전 점검: Python 스크립트 5개는 표준 라이브러리만 사용하며 네트워크 호출 코드가 없음 (import 목록 grep으로 확인).
- 동작 확인:
  - `python3 .claude/skills/ig-reel/hookscore.py <파일>` 실행됨 (훅 순위 출력)
  - `python3 .claude/skills/ig-reel/beats.py <파일> --target 30` 실행됨 (비트 시트 출력)
- `/ig-reel` 확인: 설치 직후에는 `Unknown skill: ig-reel` 오류가 났으나, 약 50분 뒤 같은 세션에서 스킬 목록에 `ig-*` 13개가 나타났고 `ig-reel` 호출 시 스킬 본문이 로드됨 (2026-11-23 08:05 UTC). 실제 릴스 대본 생성까지는 실행하지 않음.
- 참고: README 예시는 "Proposals used to take me five hours. Twenty minutes now." 훅을 86점으로 표기하지만, 실제 `hookscore.py` 실행 결과는 51.2점(OK)이었음.
- 미완료: `templates/voice.md`를 `~/.claude/instagram/voice.md`로 복사해 작성하는 단계는 하지 않음 (클라우드 컨테이너의 홈 디렉터리는 세션 종료 시 사라짐).
