from fastapi import APIRouter, Request, HTTPException
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
import os
import json

router = APIRouter()

BASE_DIR = os.path.abspath(os.path.dirname(__file__))
RESULTS_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "..", "results"))
TEMPLATES_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "templates"))

templates = Jinja2Templates(directory=TEMPLATES_DIR)

@router.get("/share/{image_id}", response_class=HTMLResponse)
async def share_result(request: Request, image_id: str):
    json_path = os.path.join(RESULTS_DIR, f"{image_id}.json")
    if not os.path.exists(json_path):
        raise HTTPException(status_code=404, detail="결과를 찾을 수 없습니다.")

    with open(json_path, "r", encoding="utf-8") as f:
        result = json.load(f)

    return templates.TemplateResponse(
        "share_result.html",
        {
            "request": request,
            "image_id": image_id,
            "main_result": result["main_result"],
            "top_k": result["top_k"],
            "message": result["message"]
        }
    )
