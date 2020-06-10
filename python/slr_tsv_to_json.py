
fr = open("data/dummy_slr_file.txt", 'r')
fw = open("data/dummy_slr_file_0.tsv", 'a')

line_count = 0

for line in fr:
    vals = line.split("\t")
    val_dict = {}
    val_dict['id'] = vals[0]
    val_dict['contents'] = vals[1:]

    print(val_dict)

fr.close()
fw.close()