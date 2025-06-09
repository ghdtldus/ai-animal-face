#TTM을 사용하는 방식(Teachable Machine)
import numpy as np 
import tensorflow as tf
from sklearn.metrics.pairwise import cosine_similarity

# TTM은 임베딩이 아니라 softmax score 반환함
class_names = [
    "bear", "snake", "cat", "dog", "wolf", "dinosaur",
    "squirrel", "rabbit", "tiger", "turtle", "deer"
]

# 모델 경로 및 출력 인덱스도 수정
interpreter = tf.lite.Interpreter(model_path="app/models/efficientnet.tflite")
interpreter.allocate_tensors()
input_index = interpreter.get_input_details()[0]['index']
output_index = interpreter.get_output_details()[0]['index']

# 기존에 작성했던 inference.py의 필터 유지
forbidden_pairs = {
    ('cat', 'bear'), ('cat', 'dinosaur'), ('snake', 'bear'),
    ('rabbit', 'bear'), ('turtle', 'cat')
}
male_preference = {"bear", "tiger", "wolf"}
female_preference = {"rabbit", "cat", "deer"}

def filter_forbidden_pairs(top_k_list):
    result = []
    for animal in top_k_list:
        if all((animal, other) not in forbidden_pairs and (other, animal) not in forbidden_pairs
               for other in result):
            result.append(animal)
        if len(result) >= 2:
            break
    return result

def gender_filter(score_dict, gender):
    if gender == "male":
        return {k: v for k, v in score_dict.items() if k not in female_preference}
    elif gender == "female":
        return {k: v for k, v in score_dict.items() if k not in male_preference}
    return score_dict

# 메인 추론 함수 (출력 포맷은 기존과 동일한 형식으로 유지)
def predict_animal_face(img_data, gender: str = None):
    try:
        # 1. 입력 텐서 변환
        if isinstance(img_data, np.ndarray):
            input_tensor = img_data
        else:
            input_tensor = img_data.numpy()

        if input_tensor.shape[1] == 3:
            input_tensor = np.transpose(input_tensor, (0, 2, 3, 1))  # NCHW → NHWC

        input_tensor = input_tensor.astype(np.float32)
        if input_tensor.max() > 1.0:
            input_tensor /= 255.0  # ✅ TTM은 0~1 범위

        # 2. 추론
        interpreter.set_tensor(input_index, input_tensor)
        interpreter.invoke()
        output = interpreter.get_tensor(output_index).squeeze()  # (11,)

        # 3. 점수 딕셔너리 구성
        score_dict = {cls: float(score) for cls, score in zip(class_names, output)}

        # 4. 성별 필터링 적용
        score_dict = gender_filter(score_dict, gender)

        # 5. Top-5 중 금지조합 제거 후 최대 2개 선택
        top_k = sorted(score_dict.items(), key=lambda x: x[1], reverse=True)[:5]
        filtered_animals = filter_forbidden_pairs([x[0] for x in top_k])
        filtered_scores = [score_dict[a] for a in filtered_animals]

        # 6. Softmax 후 비율 계산 (안정적 확률 분포)
        e_x = np.exp(filtered_scores - np.max(filtered_scores))
        probs = e_x / e_x.sum()

        # 7. 결과 포맷 (기존과 동일)
        results = []
        for animal, p in zip(filtered_animals, probs):
            results.append({
                "animal": animal,
                "score": round(p * 100, 1)
            })

        return results

    except Exception as e:
        print(f"추론 실패: {e}")
        return [{"animal": "unknown", "score": 0.0}]