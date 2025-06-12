import os

# 이미지 전처리 관련 전역 설정
RESIZE_LIMIT = 3000            # 너무 큰 이미지는 자동 축소
DEBUG_MODE = False              # 디버깅 시 얼굴 박스 표시
MEAN = [0.498, 0.498, 0.498]   # 모델 입력 정규화 mean
STD = [0.25, 0.25, 0.25]       # 모델 입력 정규화 std

# 배포 시 분기 설정
IS_LOCAL = os.getenv("IS_LOCAL", "true").lower() == "true"
BASE_URL = (
    "http://10.0.2.2:8000/static/cards"
    if IS_LOCAL else "https://api.animalfaceapp.com/static/cards"
)

# EC2 세팅 후 IP 확보되면
# PROD_IMAGE_URL = "http://3.39.xx.xx:8000/static/cards"  이런식으로 바꾸기
PROD_IMAGE_URL = BASE_URL

BASE_DIR = os.path.abspath(os.path.dirname(__file__))
IMAGE_SAVE_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "static", "cards"))
RESULT_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "..", "results"))
STATIC_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "static"))