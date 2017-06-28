

class WordData:
    def __init__(self,word,pos,named_entity):
        self.word = word
        self.pos = pos
        self.named_entity = named_entity

    @staticmethod
    def get_word_data(string):
        strings = string.split()
        word = None
        pos = None
        named_entity = None
        for str in strings:
            if(str.startswith("Text=")):
                word = str[5:]
            elif(str.startswith("PartOfSpeech=")):
                pos = str[13:]
            elif(str.startswith("NamedEntityTag=")):
                named_entity = str[15:]

        word_data = WordData(word,pos,named_entity)
        return word_data
