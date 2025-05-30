import mediapipe as mp
import cv2
from PIL import Image
import numpy as np
import io
import torch
import torchvision.transforms as transforms
from app.config import RESIZE_LIMIT, DEBUG_MODE, MEAN, STD

# 전역 얼굴 디텍터 (성능 향상 목적)
mp_face = mp.solutions.face_detection
face_detector = mp_face.FaceDetection(model_selection=1, min_detection_confidence=0.5)


# 얼굴 시각화 함수 (디버깅용)
def debug_show_face_box(image_np, x1, y1, x2, y2):
    import matplotlib.pyplot as plt
    debug_image = image_np.copy()
    debug_image = cv2.rectangle(debug_image, (x1, y1), (x2, y2), (255, 0, 0), 3)
    plt.imshow(cv2.cvtColor(debug_image, cv2.COLOR_BGR2RGB))
    plt.title("Detected Face")
    plt.axis("off")
    plt.show()


# 정사각형 패딩 함수 (중앙 정렬 + 검정 배경)
def pad_to_square(image: Image.Image, fill_color=(0, 0, 0)) -> Image.Image:
    w, h = image.size
    size = max(w, h)
    new_image = Image.new("RGB", (size, size), fill_color)
    paste_pos = ((size - w) // 2, (size - h) // 2)
    new_image.paste(image, paste_pos)
    return new_image


# 메인 전처리 함수 (이미지 → Tensor)
def preprocess_image(image_bytes: bytes) -> torch.Tensor:
    try:
        # 1. 이미지 바이트 → PIL 이미지
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        w, h = image.size

        # 2. 해상도 제한 → 비율 유지 자동 축소
        if max(w, h) > RESIZE_LIMIT:
            scale = RESIZE_LIMIT / max(w, h)
            image = image.resize((int(w * scale), int(h * scale)), Image.LANCZOS)

        # 3. PIL → NumPy → BGR (mediapipe용)
        image_np = np.array(image)
        image_bgr = cv2.cvtColor(image_np, cv2.COLOR_RGB2BGR)

        # 4. 얼굴 검출 수행
        results = face_detector.process(image_bgr)

        # 5. 얼굴 미검출 예외 처리
        if not results.detections:
            raise ValueError("얼굴이 감지되지 않았습니다. 정면 얼굴 사진을 다시 업로드해주세요.")

        # 6. 첫 얼굴의 박스 좌표 계산
        bbox = results.detections[0].location_data.relative_bounding_box
        ih, iw, _ = image_np.shape
        x1 = max(0, int(bbox.xmin * iw))
        y1 = max(0, int(bbox.ymin * ih))
        x2 = min(iw, int((bbox.xmin + bbox.width) * iw))
        y2 = min(ih, int((bbox.ymin + bbox.height) * ih))

        # 7. 얼굴 crop 및 정사각형 패딩
        face_crop = image_np[y1:y2, x1:x2]
        face_pil = Image.fromarray(face_crop)
        face_pil = pad_to_square(face_pil)

        # 8. 디버그 시각화 (선택)
        if DEBUG_MODE:
            debug_show_face_box(image_np, x1, y1, x2, y2)

        # 9. 모델 입력 전처리 (224×224, 정규화)
        transform = transforms.Compose([
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(mean=MEAN, std=STD)
        ])
        return transform(face_pil).unsqueeze(0)  # (1, 3, 224, 224)

    except Exception as e:
        # 최종 예외 처리
        raise ValueError(f"전처리 중 오류 발생: {str(e)}")
