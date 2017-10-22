from gensim.models.keyedvectors import KeyedVectors
import sys
import numpy as np

str1 = sys.argv[1]
str2 = sys.argv[2]

model = KeyedVectors.load_word2vec_format('/scratchd/home/shankar/word2vec/wiki.model.bin', binary=True)
vec1 = np.zeros(300)
for word in str1.split():
        try:
                vec1 = vec1 + model[word]
        except:
                pass
vec2 = np.zeros(300)
for word in str2.split():
        try:
                vec2 = vec2 + model[word]
        except:
                pass
value = np.dot(vec1,vec2)
n1 = np.linalg.norm(vec1)
n2 = np.linalg.norm(vec2)
value = value/(n1* n2)
print(value)