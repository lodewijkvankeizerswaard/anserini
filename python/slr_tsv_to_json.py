import json

fr = open("data/dummy_slr_file.txt", 'r')
fw = open("data/dummy_slr_file_0.tsv", 'a')

line_count = 0

doc_list = []

for line in fr:
    line_id = line.split("\t")[0]
    line_vals = line[ line.index("\t") :]
    val_dict = {'id' : line_id, 'contents' : line_vals[0:10]}
    doc_list.append(val_dict)

print(json.dumps(doc_list))

fr.close()
fw.close()