from typing import List
from PIL import Image, ImageDraw, ImageFont
import os
import uuid
from pathlib import Path

# 공 유 카드 이미지 저장 디렉토리 및 URL
IMAGE_SAVE_DIR = r"C:\Projects\ai-animal-face\firebase-hosting\public\static\cards"
BASE_URL = "https://animalfaceapp-e67a4.web.app/static/cards"
# 메시지 템플릿
MESSAGES = {
    "wolf": "늑대상! 강인하고 자유로운 영혼의 스타일이에요 🐺",
    "turtle": "거북이상! 느긋하고 차분한 매력을 가진 스타일이에요 🐢",
    "tiger": "호랑이상! 강인하고 자신감 넘치는 스타일이에요 🐯",
    "squirrel": "다람쥐상! 활발하고 귀여운 에너지를 가진 스타일이에요 🐿️",
    "dinosaur": "공룡상! 강력하고 존재감 넘치는 스타일이에요 🦖",
    "deer": "사슴상! 우아하고 섬세한 느낌의 스타일이에요 🦌",
    "rabbit": "토끼상! 귀엽고 사랑스러운 이미지를 가진 스타일이에요 🐰",
    "snake": "뱀상! 신비롭고 매혹적인 분위기를 가진 스타일이에요 🐍",
    "bear": "곰상! 든든하고 신뢰감을 주는 인상이에요 🐻",
    "cat": "고양이상! 부드럽고 세련된 매력을 가진 스타일이에요 😺",
    "dog": "강아지상! 충직하고 친근한 인상을 주는 스타일이에요 🐶",
}

# 공유 카드 이미지 생성 함수
def generate_share_card(animal: str) -> str:
    # 이미지 기본 설정
    width, height = 400, 200
    img = Image.new("RGB", (width, height), color=(255, 255, 255))
    draw = ImageDraw.Draw(img)

    # 폰트 설정
    try:
        font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        font = ImageFont.truetype(font_path, 24)
    except Exception:
        font = ImageFont.load_default()

    # 텍스트 작성
    text = f"당신의 동물상은 {animal}상입니다!"
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # 중앙 정렬 위치 계산
    x = (width - text_width) / 2
    y = (height - text_height) / 2
    draw.text((x, y), text, fill="black", font=font)

    # 파일 저장
    os.makedirs(IMAGE_SAVE_DIR, exist_ok=True)
    filename = f"{uuid.uuid4().hex}.png"
    filepath = os.path.join(IMAGE_SAVE_DIR, filename)
    img.save(filepath)

    return f"{BASE_URL}/{filename}"

# 최종 응답 포맷 함수
def format_response(prediction: List[dict], image_id: str) -> dict:
    main = prediction[0]
    animal = main["animal"]

    message = MESSAGES.get(animal, f"{animal}상! 단정하고 따뜻한 인상을 주는 스타일이에요 💫")

    return {
        "main_result": main,
        "top_k": prediction[:3],
        "message": message,
        "share_card_url": f"https://animalfaceapp-e67a4.web.app/static/cards/{image_id}.png",
        "share_page_url": f"https://animalfaceapp-e67a4.web.app/share/{image_id}.html"
    }