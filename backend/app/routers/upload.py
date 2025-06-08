from fastapi import APIRouter, File, UploadFile, Form
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import List, Optional
from app.utils.image_preprocess import preprocess_image
from app.utils.inference import predict_animal_face
from app.utils.response_format import format_response
import logging
from pprint import pprint
import uuid
import os
from app.utils.response_format import format_response, generate_share_card 
from pathlib import Path
router = APIRouter()
logger = logging.getLogger("uvicorn.error")

# 응답 데이터 구조 정의
class AnimalScore(BaseModel):
    animal: str
    score: float

class UploadResponse(BaseModel):
    main_result: AnimalScore
    top_k: List[AnimalScore]
    message: str
    share_card_url: Optional[str] = None
    share_page_url: Optional[str] = None

# /upload API
@router.post("/upload", response_model=UploadResponse)
async def upload_image(
    file: UploadFile = File(...),
    gender: str = Form(None)
):
    try:
        if not file.filename.endswith((".jpg", ".jpeg", ".png")):
            raise ValueError("지원하지 않는 파일 형식입니다.")

        # 1. image_id 한 번만 생성
        image_id = uuid.uuid4().hex
        print(f"🔥 image_id: {image_id}")

        # 2. 경로 설정
        BASE_DIR = os.path.abspath(os.path.dirname(__file__))  # /app 기준
        STATIC_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "..", "firebase-hosting", "public", "static", "cards"))
        IMAGE_SAVE_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "static", "cards"))
        RESULT_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "..", "results"))
        os.makedirs(STATIC_DIR, exist_ok=True)
        os.makedirs(RESULT_DIR, exist_ok=True)

        # 3. 원본 이미지 저장 (선택적. 현재는 필요 없음)
        # image_path = os.path.join(STATIC_DIR, f"{image_id}.uploaded.png")
        # with open(image_path, "wb") as f:
        #     f.write(await file.read())

        # 4. 전처리 및 예측
        img_bytes = await file.read()
        preprocessed = preprocess_image(img_bytes)
        prediction = predict_animal_face(preprocessed, gender)

        # 5. 공유 카드 이미지 생성
        main_animal = prediction[0]["animal"]
        share_card_url = generate_share_card(main_animal, image_id=image_id, top_k=prediction, save_dir=IMAGE_SAVE_DIR)

        # 6. 결과 JSON 저장
        result_data = {
            "main_result": prediction[0],
            "top_k": prediction[:3],
            "message": f"{main_animal}상! 당신은 {main_animal}상의 매력을 가지고 있어요!",
            "image_url": f"http://10.0.2.2:8000/static/cards/{image_id}.png"
        }

        with open(os.path.join(RESULT_DIR, f"{image_id}.json"), "w", encoding="utf-8") as f:
            import json
            json.dump(result_data, f, ensure_ascii=False, indent=2)

        # 7. 응답 리턴
        return UploadResponse(
            main_result=prediction[0],
            top_k=prediction[:3],
            message=result_data["message"],
            share_card_url=result_data["image_url"],
            share_page_url=f"http://10.0.2.2:8000/share/{image_id}"
            
        )
    except ValueError as ve:
        logger.warning(f"입력 오류: {ve}")
        return JSONResponse(
            status_code=422,
            content={
                "main_result": {"animal": "unknown", "score": 0.0},
                "top_k": [],
                "message": str(ve),
                "share_card_url": None
            }
        )

    except Exception as e:
        logger.exception("서버 내부 오류 발생")
        return JSONResponse(
            status_code=500,
            content={
                "main_result": {"animal": "unknown", "score": 0.0},
                "top_k": [],
                "message": f"서버 오류: {str(e)}",
                "share_card_url": None
            }
        )
