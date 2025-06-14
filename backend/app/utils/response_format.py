from typing import List
from PIL import Image, ImageDraw, ImageFont
import os
from app.config import IMAGE_SAVE_DIR, PROD_IMAGE_URL, BASE_DIR

FONT_PATH = os.path.abspath(os.path.join(BASE_DIR, "..", "..", "android", "app", "src", "main", "res", "font", "hakgyoansim_dunggeunmiso_b.otf"))
ANIMAL_IMAGES_DIR = os.path.abspath(os.path.join(BASE_DIR, "static", "animal_icons"))

ANIMAL_NAME_KR = {
    "bear": "곰상", "cat": "고양이상", "dog": "강아지상", "deer": "사슴상",
    "rabbit": "토끼상", "wolf": "늑대상", "tiger": "호랑이상", "snake": "뱀상",
    "squirrel": "다람쥐상", "turtle": "거북이상", "dinosaur": "공룡상"
}

MESSAGES = {
    "dog": "강아지상은 귀엽고 친근한 인상이 매력이에요!",
    "cat": "고양이상은 도도하고 시크한 매력이 있어요!",
    "bear": "곰상은 듬직하고 포근한 느낌이 특징이에요!",
    "deer": "사슴상은 청순하고 순한 인상이 매력이에요!",
    "rabbit": "토끼상은 귀엽고 깜찍한 매력을 가지고 있어요!",
    "wolf": "늑대상은 강렬하고 카리스마 넘치는 인상이 돋보여요!",
    "tiger": "호랑이상은 당당하고 카리스마 있는 매력이 있어요!",
    "snake": "뱀상은 신비롭고 매혹적인 분위기를 풍겨요!",
    "squirrel": "다람쥐상은 발랄하고 귀여운 매력이 있어요!",
    "turtle": "거북이상은 차분하고 성실한 느낌이 인상적이에요!",
    "dinosaur": "공룡상은 독특하고 강렬한 존재감을 가지고 있어요!"
}


def format_response(prediction: List[dict], image_id: str) -> dict:
    main = prediction[0]
    animal = main["animal"]
    message = MESSAGES.get(animal, f"{animal}상! 단정하고 따뜻한 인상을 주는 스타일이에요 💫")

    return {
        "main_result": main,
        "top_k": prediction[:3],
        "message": message,
        "share_card_url": f"{PROD_IMAGE_URL}/{image_id}_app.png"
    }


def generate_share_card_for_app(animal: str, image_id: str, top_k: list, save_dir: str = IMAGE_SAVE_DIR) -> str:
    width, height = 600, 600
    img = Image.new("RGB", (width, height), color=(240, 220, 200))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([20, 20, width - 20, height - 20], radius=30, fill=(255, 255, 255), outline=(112, 84, 56), width=4)

    try:
        title_font = ImageFont.truetype(FONT_PATH, 28)
        result_font = ImageFont.truetype(FONT_PATH, 36)
        bar_font = ImageFont.truetype(FONT_PATH, 18)
        message_font = ImageFont.truetype(FONT_PATH, 22)
    except Exception:
        title_font = result_font = bar_font = message_font = ImageFont.load_default()

    message = MESSAGES.get(animal, f"{animal}상이에요!")

    draw.text((width // 2, 60), "내 동물상 분석 결과", font=title_font, fill=(112, 84, 56), anchor="mm")
    draw.text((width // 2, 110), f"{ANIMAL_NAME_KR.get(animal, animal + '상')}!!", font=result_font, fill="black", anchor="mm")

    try:
        animal_img_path = os.path.join(ANIMAL_IMAGES_DIR, f"ic_{animal}.png")
        animal_img = Image.open(animal_img_path).convert("RGBA").resize((200, 200))
        img.paste(animal_img, (width//2 - 100, 150), animal_img)
    except Exception as e:
        print(f"❌ 동물 이미지 로드 실패: {e}")

    draw.text((width // 2, 370), message, font=message_font, fill=(112, 84, 56), anchor="mm")

    color_map = ["#a5dff9", "#ffb5a7"]
    background_colors = ["#d8f1ff", "#ffe4d9"]

    bar_y = 425
    bar_width = 320
    bar_height = 20
    gap = 50
    bar_radius = 10

    for i, item in enumerate(top_k[:2]):
        label = ANIMAL_NAME_KR.get(item["animal"], f"{item['animal']}상")
        percent_text = f"{int(item['score'])}%"
        score = item['score'] / 100

        x0 = (width - bar_width) // 2
        y0 = bar_y + i * gap
        x1 = x0 + bar_width
        y1 = y0 + bar_height

        draw.text((x0, y0 - 25), f"{label}", font=bar_font, fill="black")
        draw.text((x1, y0 - 25), percent_text, font=bar_font, fill="black", anchor="ra")

        draw.rounded_rectangle([x0, y0, x1, y1], radius=bar_radius, fill=background_colors[i])
        draw.rounded_rectangle([x0, y0, x0 + int(bar_width * score), y1], radius=bar_radius, fill=color_map[i])

    os.makedirs(save_dir, exist_ok=True)
    filename = f"{image_id}_app.png"
    filepath = os.path.join(save_dir, filename)
    img.save(filepath, optimize=True)

    return f"{PROD_IMAGE_URL}/{filename}"
