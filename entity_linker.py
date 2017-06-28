import os
import sys
import json
from pprint import pprint
import re

sys.path.append("corenlp-python/corenlp/")
from corenlp import StanfordCoreNLP
from word_data import WordData

class POSTracker:
    def __init__(self):
        corenlp_dir = "stanford-corenlp-full-2017-06-09/"
        self. corenlp = StanfordCoreNLP(corenlp_dir)  # wait a few minutes...

    def get_pos(self,string):
        json_data = json.loads(self.corenlp.parse(string))

        sentence = json_data['sentences'][0]
        pos_string = sentence['parsetree']
        pattern = r"\[(.*?)\]"
        texts = re.findall(pattern, pos_string)
        pprint(texts)

        result = []

        for item in texts:
            pprint(item)
            word_data = WordData.get_word_data(item)
            pprint(word_data.word+" "+word_data.named_entity+" "+word_data.pos)
            result.append(word_data)

        # print(result)

        return result