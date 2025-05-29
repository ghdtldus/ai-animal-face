# 결과 JSON 포맷
from typing import List

def format_response(prediction: List[dict]) -> dict:
    main = prediction[0]
    animal = main['animal']

    # 동물별 맞춤 메시지 사전 (요청하신 동물 모두 포함)
    messages = {
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
        "dog": "개상! 충직하고 친근한 인상을 주는 스타일이에요 🐶",
    }

    message = messages.get(animal, f"{animal}상! 단정하고 따뜻한 인상을 주는 스타일이에요 💫")

    return {
        "main_result": main,
        "top_k": prediction[:3],
        "message": message,
        "share_card_url": None  # TODO: 공유 카드 이미지 URL 넣기
    }


# from PIL import Image, ImageDraw, ImageFont
# import os
# import uuid

# # 이미지 자동 생성 후 url 생성
# # 이미지 저장 디렉토리 (웹 서버에서 접근 가능해야 함)
# IMAGE_SAVE_DIR = "static/share_cards"
# BASE_URL = "https://yourdomain.com/static/share_cards"  # 실제 도메인으로 바꿀 것

# # 이미지 자동 생성 함수
# def generate_share_card(animal: str) -> str:
#     # 이미지 크기 및 배경색
#     img = Image.new("RGB", (400, 200), color=(255, 255, 255))
#     draw = ImageDraw.Draw(img)

#     # 폰트 설정 (시스템에 따라 경로 달라짐)
#     font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
#     font = ImageFont.truetype(font_path, 24)

#     # 텍스트 작성
#     text = f"당신의 동물상은 {animal}상입니다!"
#     w, h = draw.textsize(text, font=font)
#     draw.text(((400 - w) / 2, (200 - h) / 2), text, fill="black", font=font)

#     # 파일명 생성 (uuid로 고유값 생성)
#     filename = f"{uuid.uuid4().hex}.png"
#     filepath = os.path.join(IMAGE_SAVE_DIR, filename)

#     # 디렉토리 없으면 생성
#     os.makedirs(IMAGE_SAVE_DIR, exist_ok=True)

#     # 이미지 저장
#     img.save(filepath)

#     # URL 반환
#     return f"{BASE_URL}/{filename}"


# # 메세지 포멧
# def format_response(prediction: List[dict]) -> dict:
#     main = prediction[0]
#     animal = main['animal']

#     messages = {
#         "wolf": "늑대상! 강인하고 자유로운 영혼의 스타일이에요 🐺",
#         "turtle": "거북이상! 느긋하고 차분한 매력을 가진 스타일이에요 🐢",
#         "tiger": "호랑이상! 강인하고 자신감 넘치는 스타일이에요 🐯",
#         "squirrel": "다람쥐상! 활발하고 귀여운 에너지를 가진 스타일이에요 🐿️",
#         "dinosaur": "공룡상! 강력하고 존재감 넘치는 스타일이에요 🦖",
#         "deer": "사슴상! 우아하고 섬세한 느낌의 스타일이에요 🦌",
#         "rabbit": "토끼상! 귀엽고 사랑스러운 이미지를 가진 스타일이에요 🐰",
#         "snake": "뱀상! 신비롭고 매혹적인 분위기를 가진 스타일이에요 🐍",
#         "bear": "곰상! 든든하고 신뢰감을 주는 인상이에요 🐻",
#         "cat": "고양이상! 부드럽고 세련된 매력을 가진 스타일이에요 😺",
#         "dog": "개상! 충직하고 친근한 인상을 주는 스타일이에요 🐶",
#     }

#     message = messages.get(animal, f"{animal}상! 단정하고 따뜻한 인상을 주는 스타일이에요 💫")

#     share_card_url = generate_share_card(animal)  # 이미지 생성 후 URL 받아옴

#     return {
#         "main_result": main,
#         "top_k": prediction[:3],
#         "message": message,
#         "share_card_url": share_card_url
#     }
