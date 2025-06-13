from fastapi import FastAPI
from app.routers.upload import router as upload_router
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import os
from app.config import STATIC_DIR

app = FastAPI()

# static/cards 폴더 없으면 자동 생성
os.makedirs(os.path.join(os.path.dirname(__file__), "static/cards"), exist_ok=True)

# 정적 파일 서빙 (FastAPI가 직접 static/cards/*.png 제공)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

# CORS 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 실제 배포 땐 도메인 명시하는 게 좋아
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(upload_router, prefix="/upload")
@app.get("/")
def root():
    return {"message": "Animal Face Classifier API running."}
