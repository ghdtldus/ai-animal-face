import numpy as np 
import tensorflow as tf
from sklearn.metrics.pairwise import cosine_similarity

# 평균 임베딩 로드 (딱 1번만 로드해서 계속 재사용)
mean_embeddings = np.load("app/models/mean_embeddings.npy", allow_pickle=True).item()

# TFLite 모델 초기화
interpreter = tf.lite.Interpreter(model_path="app/models/efficientnet.tflite")
interpreter.allocate_tensors()
input_index = interpreter.get_input_details()[0]['index']
embedding_index = 173  # 확인된 임베딩 텐서 인덱스

# 추론 함수
def predict_animal_face(img_data, gender: str = None):
    try:
        # 1. 입력 텐서 형식 변환: (1, 3, 224, 224) → (1, 224, 224, 3)
        if isinstance(img_data, np.ndarray):
            input_tensor = img_data
        else:
            input_tensor = img_data.numpy()  # torch.Tensor인 경우 변환

        if input_tensor.shape[1] == 3:  # NCHW일 경우만 변환
            input_tensor = np.transpose(input_tensor, (0, 2, 3, 1))

        input_tensor = input_tensor.astype(np.float32)

        # 2. TFLite 모델 추론
        interpreter.set_tensor(input_index, input_tensor)
        interpreter.invoke()

        # 3. 임베딩 벡터 추출
        embedding = interpreter.get_tensor(embedding_index).reshape(-1)

        # 4. cosine similarity 계산
        sims = {
            cls: cosine_similarity(
                embedding.reshape(1, -1), mean_vec.reshape(1, -1)
            )[0][0]
            for cls, mean_vec in mean_embeddings.items()
        }

        # 5. 상위 K개 동물상 반환
        top_k = sorted(sims.items(), key=lambda x: x[1], reverse=True)[:3]

        results = [
            {"animal": cls, "score": round(sim * 100, 1)} for cls, sim in top_k
        ]

        return results

    except Exception as e:
        print(f"추론 실패: {e}")
        return [
            {"animal": "unknown", "score": 0.0}
        ]
