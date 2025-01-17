import numpy as np
import subprocess

def dummy_slr_arr(val):
    slr = np.random.uniform(0, 1, size=1000)
    return slr

def slr_to_string(arr):
    s = ""
    for val in arr:
        s += str(val) + "\t"
    return s

def write_slr_line(file, docid, slr):
    f.write(str(docid) + "\t" + slr_to_string(slr) + "\n")


if __name__ == "__main__":
    doc_id_list = []
    doc_id_file = open("data/robust04/FBIS/FB396001", "r")

    for line in doc_id_file:
        # print(line)
        try:
            doc_id_tag = line.index("DOCNO")
            if(doc_id_tag):
                doc_id_list.append(line.split(" ")[1])
                # print()
        except:
            pass
    
    doc_id_file.close()

    filename = "data/dummy_slr_robust04.tsv"
    f = open(filename, "a")

    

    # last_doc_line = subprocess.check_output(['tail', '-1', filename]).decode('ascii')
    # start_docid = int(last_doc_line.split('\t')[0])
    # nr_docs = 8000000

    # print("Start:" + str(start))
    # print("End:" + str(nr_docs))

    for i, doc_id in enumerate(doc_id_list):
        val = (i / len(doc_id_list)) * 0.000001
        write_slr_line(f, doc_id, dummy_slr_arr(val))

        # if i % 100000 == 0:
        #     print("Pogres: " + str(i/(nr_docs - start + 1)*100) + "%")
    
    f.close()
