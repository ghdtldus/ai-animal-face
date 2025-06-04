import sys
from pathlib import Path

TEMPLATE = """
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>나의 동물상 결과는?</title>

  <meta property="og:title" content="나의 동물상 결과는? 🐾" />
  <meta property="og:description" content="내 동물상은 어떤 모습일까? 당신도 바로 확인해보세요!" />
  <meta property="og:image" content="https://animalfaceapp-e67a4.web.app/static/cards/{id}.png" />
  <meta property="og:url" content="https://animalfaceapp-e67a4.web.app/share/{id}" />
  <meta name="twitter:card" content="summary_large_image" />
</head>
<body>
  <h2>📸 당신의 동물상은?</h2>
  <img src="https://animalfaceapp-e67a4.web.app/static/cards/{id}.png" alt="결과 이미지" />
  <p>앱을 설치하고 친구와 함께 당신의 동물상을 비교해보세요!</p>
  <a href="https://play.google.com/store/apps/details?id=com.example.android">앱 설치하러 가기</a>

  <!-- 앱 딥링크 실행용 스크립트 -->
  <script>
    window.location.href = "intent://share/{id}.html#Intent;scheme=https;package=com.example.android;end";
  </script>
  </body>
</html>
"""

def generate_html(image_id: str):
    html = TEMPLATE.replace("{id}", image_id)
    path = Path("public/share") / f"{image_id}.html"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(html, encoding="utf-8")
    print(f"Created: {path}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("사용법: python generate_share_html.py <image_id>")
        sys.exit(1)
    generate_html(sys.argv[1])
