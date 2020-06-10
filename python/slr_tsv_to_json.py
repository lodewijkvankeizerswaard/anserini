
fr = open("data/dummy_slr_file.txt", 'r')
fw = open("data/dummy_slr_file_0.tsv", 'a')

line_count = 0

for line in fr:
    line_id = line.split("\t")[0]
    line_vals = line[ line.index("\t") :]
    val_dict = {'id' : line_id, 'contents' : line_vals}

    print(val_dict)

fr.close()
fw.close()