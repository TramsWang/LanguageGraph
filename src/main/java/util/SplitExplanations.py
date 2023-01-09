import jieba
import json

jieba.set_dictionary("/home/Ruoyu/jieba/extra_dict/dict.txt.big")
fp = open("/home/Ruoyu/LanguageGraph/phrases.json", "r")
phrases = json.load(fp)
fp.close()
for phrase in phrases:
    for i in range(len(phrase["explanations"])):
        phrase["explanations"][i] = list(jieba.cut(phrase["explanations"][i]))
fp = open("/home/Ruoyu/LanguageGraph/phrases_splitted.json", "w", encoding="utf8")
json.dump(phrases, fp, ensure_ascii=False)
fp.close()