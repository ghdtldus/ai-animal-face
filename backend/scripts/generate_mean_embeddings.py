import os
import numpy as np
import pandas as pd
from PIL import Image
from collections import defaultdict
import tensorflow as tf

# ==== 설정값 ====
TFLITE_MODEL_PATH = "app/models/efficientnet.tflite"
IMAGE_DIR = "dataset/dataset_MultiLabel_cropped"
LABEL_CSV_PATH = "dataset/labels.csv"
OUTPUT_PATH = "app/models/mean_embeddings.npy"

MEAN = 0.498
STD = 0.25
TARGET_SIZE = (224, 224)
EMBEDDING_TENSOR_INDEX = 173  # 반드시 모델에서 확인한 값 사용

# ==== 이미지 전처리 함수 ====
def preprocess_image(img_path):
    img = Image.open(img_path).convert("RGB").resize(TARGET_SIZE)
    img = np.array(img) / 255.0
    img = (img - MEAN) / STD
    return img[np.newaxis, ...].astype(np.float32)

# ==== 임베딩 추출 함수 ====
def extract_embedding(interpreter, img):
    input_idx = interpreter.get_input_details()[0]["index"]
    interpreter.set_tensor(input_idx, img)
    interpreter.invoke()
    return interpreter.get_tensor(EMBEDDING_TENSOR_INDEX).reshape(-1)

# ==== 메인 함수 ====
def main():
    print("평균 임베딩 생성 시작")

    # 모델 로드
    interpreter = tf.lite.Interpreter(model_path=TFLITE_MODEL_PATH)
    interpreter.allocate_tensors()

    # 라벨 CSV 로드
    df = pd.read_csv(LABEL_CSV_PATH)
    class_names = df.columns[1:]  # 첫 번째 열은 'image'

    class_embeddings = defaultdict(list)

    for i, row in df.iterrows():
        img_path = os.path.join(IMAGE_DIR, row['image'])
        if not os.path.exists(img_path):
            print(f"Not found: {img_path}")
            continue

        try:
            img = preprocess_image(img_path)
            emb = extract_embedding(interpreter, img)
        except Exception as e:
            print(f"Error processing {img_path}: {e}")
            continue

        for cls in class_names:
            if row[cls] == 1:
                class_embeddings[cls].append(emb)

        if i % 50 == 0:
            print(f"[{i}/{len(df)}]  {row['image']} 처리 완료")

    # 평균 벡터 계산
    mean_embeddings = {
        cls: np.mean(vectors, axis=0)
        for cls, vectors in class_embeddings.items()
        if len(vectors) > 0
    }

    # 저장
    np.save(OUTPUT_PATH, mean_embeddings)
    print(f"저장 완료: {OUTPUT_PATH}")
    print(f"총 클래스: {len(mean_embeddings)}개")

if __name__ == "__main__":
    main()
