import numpy as np
import json

mean_embeddings = np.load("E:/ai-animal-face/backend/app/models/mean_embeddings.npy", allow_pickle=True).item()
serializable = {k: v.tolist() for k, v in mean_embeddings.items()}
with open("mean_embeddings.json", "w") as f:
    json.dump(serializable, f)
