import numpy as np 
import tensorflow as tf
from sklearn.metrics.pairwise import cosine_similarity

# 평균 임베딩 로드 (딱 1번만 로드해서 계속 재사용)
mean_embeddings = np.load("app/models/mean_embeddings.npy", allow_pickle=True).item()

# TFLite 모델 초기화
interpreter = tf.lite.Interpreter(model_path="app/models/exp10b_float32.tflite")
interpreter.allocate_tensors()
input_index = interpreter.get_input_details()[0]['index']
embedding_index = 173  # 임베딩 텐서 인덱스 (모델에 맞게 확인 필요)

# === 후처리용 상수 및 함수들 ===

# 서로 같이 나오면 부자연스러운 동물 조합 (금지 조합)
forbidden_pairs = {
    ('cat', 'bear'),
    ('cat', 'dinosaur'),
    ('snake', 'bear'),
    ('rabbit', 'bear'),
    ('turtle', 'cat')
}

# 성별에 따라 우선순위에서 제외할 동물 집합
male_preference = {"bear", "tiger", "wolf"}
female_preference = {"rabbit", "cat", "deer"}

# 금지 조합 필터링 함수
def filter_forbidden_pairs(top_k_list):
    """
    top_k_list: 동물 이름 리스트 (유사도 높은 순)
    이미 선택된 동물들과 forbidden_pairs에 포함되는 동물은 건너뜀
    최대 2개까지만 선택
    """
    result = []
    for animal in top_k_list:
        # 이미 선택된 동물들과 금지 조합인지 체크
        if all((animal, other) not in forbidden_pairs and (other, animal) not in forbidden_pairs
               for other in result):
            result.append(animal)
        if len(result) >= 2:  # 최대 2개까지만 선택
            break
    return result

# 성별에 따라 특정 동물 제외하는 필터 함수
def gender_filter(sim_dict, gender):
    """
    sim_dict: {동물: 유사도} 딕셔너리
    gender: 'male' 또는 'female' 또는 None
    성별에 따라 해당 성별과 덜 어울리는 동물 제거
    """
    if gender == "male":
        sim_dict = {k: v for k, v in sim_dict.items() if k not in female_preference}
    elif gender == "female":
        sim_dict = {k: v for k, v in sim_dict.items() if k not in male_preference}
    return sim_dict

# 유사도 최대값을 max_percent로 제한하고 비율에 맞춰 전체 조정
def adjust_similarity(similarity_dict, max_percent=0.7):
    """
    similarity_dict: {동물: 유사도} 딕셔너리
    max_percent: 유사도 최대 허용값 (0~1)
    가장 높은 유사도가 max_percent 초과 시 전체 비율 조정
    """
    if not similarity_dict:
        return similarity_dict
    max_value = max(similarity_dict.values())
    if max_value > max_percent:
        scale = max_percent / max_value
        similarity_dict = {k: round(v * scale, 4) for k, v in similarity_dict.items()}
    return similarity_dict

# ===============================

# 메인 추론 함수
def predict_animal_face(img_data, gender: str = None):
    try:
        # 1. 입력 텐서 형식 변환 (NCHW -> NHWC)
        if isinstance(img_data, np.ndarray):
            input_tensor = img_data
        else:
            input_tensor = img_data.numpy()  # torch.Tensor인 경우 변환

        # 채널 위치가 두 번째인 경우 (N, C, H, W)
        if input_tensor.shape[1] == 3:
            # NHWC로 변환 (N, H, W, C)
            input_tensor = np.transpose(input_tensor, (0, 2, 3, 1))

        input_tensor = input_tensor.astype(np.float32)

        # 2. TFLite 모델에 입력 텐서 세팅 및 추론 실행
        interpreter.set_tensor(input_index, input_tensor)
        interpreter.invoke()

        # 3. 임베딩 벡터 추출
        embedding = interpreter.get_tensor(embedding_index).reshape(-1)

        # 4. 각 클래스 평균 임베딩과 cosine similarity 계산
        sims = {
            cls: cosine_similarity(
                embedding.reshape(1, -1), mean_vec.reshape(1, -1)
            )[0][0]
            for cls, mean_vec in mean_embeddings.items()
        }

        # 4-1. 성별 기반 필터링 적용 (성별이 지정된 경우)
        sims = gender_filter(sims, gender)

        # 4-2. 유사도 상한 조정 (최대 0.7)
        sims = adjust_similarity(sims, max_percent=0.7)

        # 5. 유사도 높은 상위 5개 추출 후 forbidden pairs 필터 및 최대 2개 제한
        top_k = sorted(sims.items(), key=lambda x: x[1], reverse=True)[:5]
        filtered_animals = filter_forbidden_pairs([x[0] for x in top_k])

        # 6. 최종 결과 리스트 생성 (유사도 * 100하여 점수화)
        results = []
        for animal in filtered_animals:
            score = round(sims.get(animal, 0) * 100, 1)
            results.append({"animal": animal, "score": score})

        return results

    except Exception as e:
        print(f"추론 실패: {e}")
        # 실패 시 unknown 반환
        return [
            {"animal": "unknown", "score": 0.0}
        ]
