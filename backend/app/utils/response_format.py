from typing import List
from PIL import Image, ImageDraw, ImageFont
import os
import uuid
from app.config import IS_LOCAL, BASE_URL, PROD_IMAGE_URL

BASE_DIR = os.path.abspath(os.path.dirname(__file__))
FONT_PATH = os.path.abspath("C:/Projects/ai-animal-face/android/app/src/main/res/font/hakgyoansim_dunggeunmiso_b.otf")
IMAGE_SAVE_DIR = os.path.join(BASE_DIR, "..", "static", "share_cards")
BASE_URL = "http://10.0.2.2:8000/static/cards"
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

ANIMAL_IMAGES_DIR = "C:/Projects/ai-animal-face/android/app/src/main/res/drawable"

# ✅ 공유 버튼 제거된 최종 응답 포맷
def format_response(prediction: List[dict], image_id: str) -> dict:
    main = prediction[0]
    animal = main["animal"]

    message = MESSAGES.get(animal, f"{animal}상! 단정하고 따뜻한 인상을 주는 스타일이에요 💫")

    return {
        "main_result": main,
        "top_k": prediction[:3],
        "message": message,
        "share_card_url": (
            f"{BASE_URL}/{image_id}_app.png" if IS_LOCAL else f"{PROD_IMAGE_URL}/{image_id}_app.png"
        )
    }


def generate_share_card_for_app(animal: str, image_id: str, top_k: list, save_dir: str = IMAGE_SAVE_DIR) -> str:
    width, height = 600, 600
    img = Image.new("RGB", (width, height), color=(240, 220, 200))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([20, 20, width - 20, height - 20], radius=30, fill=(255, 255, 255), outline=(112, 84, 56), width=4)

    # 폰트 설정
    try:
        title_font = ImageFont.truetype(FONT_PATH, 28)
        result_font = ImageFont.truetype(FONT_PATH, 36)
        bar_font = ImageFont.truetype(FONT_PATH, 18)
    except Exception:
        title_font = result_font = bar_font = ImageFont.load_default()

    # 메시지 텍스트
    message = MESSAGES.get(animal, f"{animal}상이에요!")

    # 텍스트
    draw.text((width // 2, 60), "내 동물상 분석 결과", font=title_font, fill=(112, 84, 56), anchor="mm")
    draw.text((width // 2, 110), f"{animal}상!!", font=result_font, fill="black", anchor="mm")

    # 동물 이미지 삽입
    try:
        animal_img_path = os.path.join(ANIMAL_IMAGES_DIR, f"ic_{animal}.png")
        animal_img = Image.open(animal_img_path).convert("RGBA").resize((200, 200))
        img.paste(animal_img, (width//2 - 100, 150), animal_img)
    except Exception as e:
        print(f"❌ 동물 이미지 로드 실패: {e}")
        
    draw.text((width // 2, 370), message, font=bar_font, fill=(112, 84, 56), anchor="mm")

    # 막대 그래프 색상
    color_map = ["#a5dff9", "#ffb5a7"]
    background_colors = ["#d8f1ff", "#ffe4d9"]

    # 막대 그래프
    bar_y = 425
    bar_width = 320
    bar_height = 20
    gap = 50
    bar_radius = 10

    for i, item in enumerate(top_k[:2]):
        label = f"{item['animal']}상"
        percent_text = f"{item['score']:.0f}%"
        score = item['score'] / 100

        x0 = (width - bar_width) // 2
        y0 = bar_y + i * gap
        x1 = x0 + bar_width
        y1 = y0 + bar_height

        # 동물명
        draw.text((x0, y0 - 25), f"{label}", font=bar_font, fill="black")
        # 점수
        draw.text((x1, y0 - 25), percent_text, font=bar_font, fill="black", anchor="ra")

        # 바 그리기
        draw.rounded_rectangle([x0, y0, x1, y1], radius=bar_radius, fill=background_colors[i])
        draw.rounded_rectangle([x0, y0, x0 + int(bar_width * score), y1], radius=bar_radius, fill=color_map[i])

    # 저장
    os.makedirs(save_dir, exist_ok=True)
    filename = f"{image_id}_app.png"
    filepath = os.path.join(save_dir, filename)
    img.save(filepath, optimize=True)

    return f"{BASE_URL}/{filename}"


def generate_share_card_for_web(animal: str, image_id: str, top_k: List[dict], save_dir: str) -> str:
    width, height = 600, 400
    img = Image.new("RGB", (width, height), color=(255, 241, 224))  # 기존 연살구톤 배경
    draw = ImageDraw.Draw(img)

    font_path = os.path.join(BASE_DIR, "..", "..", "assets", "fonts", "NanumGothic-Bold.ttf")
    try:
        title_font = ImageFont.truetype(font_path, 28)
        result_font = ImageFont.truetype(font_path, 36)
        bar_font = ImageFont.truetype(font_path, 18)
    except Exception:
        title_font = result_font = bar_font = ImageFont.load_default()

    draw.text((width/2, 40), "당신의 결과는", font=title_font, fill="black", anchor="mm")
    draw.text((width/2, 90), f"{animal}상!!", font=result_font, fill="black", anchor="mm")

    # bar 시각화
    start_y = 200
    bar_width = 300
    bar_height = 20
    gap = 50

    for i, item in enumerate(top_k[:2]):
        label = f"{item['animal']} {item['score']:.1f}%"
        score = item['score']
        bar_x = (width - bar_width) // 2
        bar_y = start_y + i * gap

        draw.text((bar_x - 10, bar_y + bar_height // 2), label, font=bar_font, fill="black", anchor="rm")
        draw.rectangle([bar_x, bar_y, bar_x + bar_width, bar_y + bar_height], fill="#ddd")
        draw.rectangle([bar_x, bar_y, bar_x + int(bar_width * score), bar_y + bar_height], fill="#6c63ff")

    filename = f"{image_id}_web.png"
    filepath = os.path.join(save_dir, filename)
    img.save(filepath, optimize=True)

    return f"{PROD_IMAGE_URL}/{filename}"
