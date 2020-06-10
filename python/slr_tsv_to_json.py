import json
from pathlib import Path

input_folder = "data/"
input_file = "dummy_slr_robust04.txt"
output_folder = "data/robust04_json/"
docs_per_file = 10

Path(output_folder).mkdir(parents=True, exist_ok=True)

fr = open(input_folder + input_file, 'r')

print("Reading from file: " + input_folder + input_file)

line_count = 0
file_count = 0

output_file_base = input_file[: input_file.rfind('.')] + "_"
output_file_path = output_file_base 
fw = open(output_file_path + "0.json", 'a')

print("Writing to file: " + output_file_path + "0.json")

doc_list = []

for line in fr:
    line_id = line.split("\t")[0]
    line_vals = line[ line.index("\t") + 1 :]
    val_dict = {'id' : line_id, 'contents' : line_vals[0:10]}
    doc_list.append(val_dict)

    line_count += 1
    if line_count >= docs_per_file:
        fw.write(json.dumps(doc_list))
        fw.close()
        file_suffix = str(file_count * docs_per_file) + ".json"
        print("Writing to file: " + output_file_path + file_suffix)
        fw = open(output_file_path + file_suffix, 'a')

        file_count += 1
        line_count = 0
    
    if file_count > 3:
        break

fr.close()
fw.close()