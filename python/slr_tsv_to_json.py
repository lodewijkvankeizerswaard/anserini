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
file_nr = 0

output_file_base = output_folder + input_file[: input_file.rfind('.')] + "_"
fw = open(output_file_base + "0.json", 'a')

print("Writing to file: " + output_file_base + "0.json")

doc_list = []

for line in fr:
    line_id = line.split("\t")[0]
    line_vals = line[ line.index("\t") + 1 :]
    val_dict = {'id' : line_id, 'contents' : line_vals}
    doc_list.append(val_dict)

    line_count += 1
    if line_count >= docs_per_file:
        fw.write(json.dumps(doc_list))
        fw.close()
        file_nr += 1
        file_suffix = str(file_nr * docs_per_file) + ".json"
        print("Writing to file: " + output_file_base + file_suffix)
        doc_list = []
        fw = open(output_file_base + file_suffix, 'a')

        line_count = 0

fr.close()
fw.close()