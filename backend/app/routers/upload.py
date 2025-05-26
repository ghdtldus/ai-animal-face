from fastapi import APIRouter, File, UploadFile, Form
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import List, Optional
from app.utils.image_preprocess import preprocess_image
from app.utils.inference import predict_animal_face
from app.utils.response_format import format_response
import logging

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

# /upload API
@router.post("/upload", response_model=UploadResponse)
async def upload_image(
    file: UploadFile = File(..., description="사용자 얼굴 이미지 (jpg/png)"),
    gender: str = Form(None, description="성별 정보 (optional): 'male' 또는 'female'")
):
    try:
        if not file.filename.endswith((".jpg", ".jpeg", ".png")):
            raise ValueError("지원하지 않는 파일 형식입니다. JPG 또는 PNG 이미지를 업로드하세요.")

        img_bytes = await file.read()
        preprocessed = preprocess_image(img_bytes)
        prediction = predict_animal_face(preprocessed, gender)
        return format_response(prediction)

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
