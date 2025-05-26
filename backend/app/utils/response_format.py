# 결과 JSON 포맷
from typing import List

def format_response(prediction: List[dict]) -> dict:
    main = prediction[0]
    return {
        "main_result": main,
        "top_k": prediction[:3],
        "message": f"{main['animal']}상! 단정하고 따뜻한 인상을 주는 스타일이에요 💫",
        "share_card_url": None  # TODO: 나중에 공유 카드 이미지 URL 넣기
    }
