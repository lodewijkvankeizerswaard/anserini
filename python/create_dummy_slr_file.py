import numpy as np
import subprocess

def np_arr_to_tsv(arr):
    s = ""
    for val in arr:
        s += str(val) + "\t"
    return s

if __name__ == "__main__":
    filename = "data/dummy_slr_file.txt"
    f = open(filename, "a")

    line = subprocess.check_output(['tail', '-1', filename]).decode('ascii')
    start = int(line.split('\t')[0])
    nr_docs = 8000000

    print("Start:" + str(start))
    print("End:" + str(nr_docs))

    for i in range(start+1, nr_docs):
        slr = np.random.uniform(0, 1, size=1000)
        slr[slr <= 0.9] = 0
        f.write(str(i) + "\t" + np_arr_to_tsv(slr) + "\n")

        if i % 100000 == 0:
            print("Pogres: " + str(i/(nr_docs - start + 1)*100) + "%")
    
    f.close()
