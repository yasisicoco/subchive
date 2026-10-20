# 서비스 로고 출처

로고는 2026-10-20에 내려받은 PNG 원본을 변경하지 않고 사용합니다. 앱에서는 흰색 타일 안에 비율을 유지해 표시합니다. 색조 변경·재도색은 하지 않습니다. 원본 URL·출처 페이지·SHA-256은 `app/src/main/assets/logo_sources.json`에 보존했습니다.

| 서비스 | 출처 |
|---|---|
| Netflix, YouTube, Disney+, Spotify, Apple Music, ChatGPT, Notion, Microsoft 365, iCloud, Google One, Xbox Game Pass | [Dashboard Icons](https://github.com/homarr-labs/dashboard-icons/tree/b409eb0d1fa960277f8510d3ae3a19093b910ca4/png) |
| TVING | [개발자 Google Play 페이지](https://play.google.com/store/apps/details?id=net.cj.cjhv.gs.tving) |
| Wavve | [개발자 Google Play 페이지](https://play.google.com/store/apps/details?id=kr.co.captv.pooqV2) |
| 쿠팡플레이 | [개발자 Google Play 페이지](https://play.google.com/store/apps/details?id=com.coupang.mobile.play) |
| 멜론 | [개발자 Google Play 페이지](https://play.google.com/store/apps/details?id=com.iloen.melon) |
| Claude | [개발자 Google Play 페이지](https://play.google.com/store/apps/details?id=com.anthropic.claude) |
| 쿠팡 와우 | [쿠팡 앱 대표 아이콘](https://play.google.com/store/apps/details?id=com.coupang.mobile) |
| 네이버플러스 | [NAVER 앱 대표 아이콘](https://play.google.com/store/apps/details?id=com.nhn.android.search) |

Dashboard Icons 저장소의 Apache-2.0 라이선스는 `app/src/main/assets/licenses/dashboard-icons.txt`에 포함했습니다. 각 브랜드의 상표·로고 권리는 해당 권리자에게 있으며, 컬렉션 라이선스와 별개로 브랜드 사용 지침을 따릅니다. 원래 구독비서 앱의 로고나 이미지 자산은 사용하지 않았습니다.

`tools/fetch_service_logos.py`는 Dashboard Icons를 위의 커밋으로 고정해 내려받고, Google Play에서는 해당 시점의 게시된 아이콘 URL을 읽습니다. PNG 시그니처를 확인하고 파일별 해시를 기록합니다. 회원가입·API 키는 필요 없습니다.
