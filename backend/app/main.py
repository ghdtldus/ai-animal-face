from fastapi import FastAPI
from app.routers.upload import router as upload_router
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()  
app.include_router(upload_router)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 또는 ["http://10.0.2.2"]
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"message": "Animal Face Classifier API running."}

app.mount("/static", StaticFiles(directory="static"), name="static")
@app.get("/share/{image_id}", response_class=HTMLResponse)
def show_share_preview(image_id: str):
    image_url = f"https://sandwich.app/static/cards/{image_id}.png"
    return f"""
    <!DOCTYPE html>
    <html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <!-- Open Graph (OG) meta tags for Facebook, Kakao, etc. -->
        <meta property="og:title" content="나의 동물상 결과는? 🐾" />
        <meta property="og:description" content="내 동물상은 어떤 모습일까? 당신도 바로 확인해보세요!" />
        <meta property="og:image" content="{image_url}" />
        <meta property="og:url" content="https://sandwich.app/share/{image_id}" />
        <meta name="twitter:card" content="summary_large_image">

        <title>나의 동물상 결과</title>
        <style>
            body {{ font-family: sans-serif; text-align: center; padding: 20px; }}
            img {{ max-width: 100%; height: auto; border-radius: 12px; }}
            a {{ display: inline-block; margin-top: 20px; font-size: 18px; color: white;
                background: #4CAF50; padding: 10px 24px; border-radius: 8px; text-decoration: none; }}
        </style>
    </head>
    <body>
        <h2>📸 당신의 동물상은?</h2>
        <img src="{image_url}" alt="결과 이미지">
        <p>앱을 설치하고 친구와 함께 당신의 동물상을 비교해보세요!</p>
        <a href="https://play.google.com/store/apps/details?id=com.example.android">앱 설치하러 가기</a>
    </body>
    </html>
    """