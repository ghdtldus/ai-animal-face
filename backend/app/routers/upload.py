# upload API 라우터
from fastapi import APIRouter, File, UploadFile, Form
from app.utils.image_preprocess import preprocess_image
from app.utils.inference import predict_animal_face
from app.utils.response_format import format_response
from pydantic import BaseModel
from typing import List, Optional

router = APIRouter()

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
        img_bytes = await file.read()
        preprocessed = preprocess_image(img_bytes)
        prediction = predict_animal_face(preprocessed, gender)
        return format_response(prediction)
    except Exception as e:
        return {
            "main_result": {"animal": "unknown", "score": 0.0},
            "top_k": [],
            "message": f"에러 발생: {str(e)}",
            "share_card_url": None
        }
