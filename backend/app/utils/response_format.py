from typing import List
from PIL import Image, ImageDraw, ImageFont
import os
import uuid
from pathlib import Path
from app.config import IS_LOCAL, BASE_URL, PROD_IMAGE_URL, PROD_SHARE_PAGE_URL

BASE_DIR = os.path.abspath(os.path.dirname(__file__))
IMAGE_SAVE_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "static", "cards"))

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


# 최종 응답 포맷 함수
def format_response(prediction: List[dict], image_id: str) -> dict:
    main = prediction[0]
    animal = main["animal"]

    message = MESSAGES.get(animal, f"{animal}상! 단정하고 따뜻한 인상을 주는 스타일이에요 💫")

    return {
        "main_result": main,
        "top_k": prediction[:3],
        "message": message,
        "share_card_url": (
            f"{BASE_URL}/{image_id}_web.png" if IS_LOCAL else f"{PROD_IMAGE_URL}/{image_id}_web.png"
        ),
        "share_page_url": f"{PROD_SHARE_PAGE_URL}/{image_id}"  # 운영 기준
    }


def generate_share_card_for_app(animal: str, image_id: str, top_k: List[dict], save_dir: str) -> str:
    width, height = 600, 400
    img = Image.new("RGB", (width, height), color=(230, 250, 250))  # 앱 전용 연하늘 배경
    draw = ImageDraw.Draw(img)

    font_path = os.path.join(BASE_DIR, "..", "..", "assets", "fonts", "NanumGothic-Bold.ttf")
    try:
        title_font = ImageFont.truetype(font_path, 28)
        result_font = ImageFont.truetype(font_path, 36)
        bar_font = ImageFont.truetype(font_path, 18)
    except Exception:
        title_font = result_font = bar_font = ImageFont.load_default()

    draw.text((width/2, 40), "✨내 동물상 분석 결과✨", font=title_font, fill="black", anchor="mm")
    draw.text((width/2, 90), f"결과는 {animal}상!", font=result_font, fill="black", anchor="mm")

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
