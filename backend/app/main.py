from fastapi import FastAPI
from app.routers.upload import router as upload_router
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import os
from app.routers.share import router as share_router

app = FastAPI()  
app.include_router(upload_router)
app.include_router(share_router)

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
static_dir = os.path.join(os.path.dirname(__file__), "../../firebase-hosting/public/static")
app.mount("/static", StaticFiles(directory=static_dir), name="static")