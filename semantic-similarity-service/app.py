from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer, util
import os

app = Flask(__name__)
model = SentenceTransformer('all-MiniLM-L6-v2')

@app.route('/similarity', methods=['POST'])
def similarity():
    data = request.json
    sentences = data['sentences']  # List of two sentences
    embeddings = model.encode(sentences)
    score = util.cos_sim(embeddings[0], embeddings[1]).item()
    return jsonify({'similarity': score})

if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
