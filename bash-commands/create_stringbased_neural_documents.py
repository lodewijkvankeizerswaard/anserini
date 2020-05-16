import argparse

# Reading and writing json files
import json
from os import listdir, mkdir
from os.path import isfile, join

import numpy as np

def compute_SLR(raw):
    # Change default values!!!
    rand = np.random.uniform(0, 1, 100)
    rand[rand <= 0.9] = 0
    return rand

def SLR_to_index_format(raw, decimal_precision):
    vec = compute_SLR(raw)
    vec_str_count = [(i, (int)(ind * 10**decimal_precision)) for i,ind in enumerate(vec) if ind != 0]
    string_vec = ""
    for ind in vec_str_count:
        string_vec += (str(ind[0]) + " ")*ind[1]
    return string_vec


def write_SLR_to_collection(read_dir, write_dir, precision):
    # Get the json files to add SLR
    onlyfiles = [f for f in listdir(read_dir) if isfile(join(read_dir, f))]
    print("Found " + str(len(onlyfiles)) + " file(s)")
    try:
        mkdir(write_dir)
    except OSError:
        print("Creation of write directory failed!")

    for file in onlyfiles:
        read_path = read_dir + "/" + file
        write_path = write_dir + "/" + file
        print("Transforming \"" + read_path + "\" to \"" + write_path + "\"")
        with open(read_path, 'r') as read_file, open(write_path, 'w') as write_file:
            json_data = [json.loads(line) for line in read_file]
            print("Found " + str(len(json_data)) + " json document(s)")
            for i, document in enumerate(json_data):
                if i % 1000 == 0:
                    print("Processes " + str(i) + " json document(s)")
                new_doc = {'id':document['id'],
                            'contents': SLR_to_index_format(document['contents'], precision),
                            'raw': document['contents']}
                json.dump(new_doc, write_file)

if __name__ == "__main__":
    # Parse arguments
    parser = argparse.ArgumentParser(description='FILL THIS IN')
    parser.add_argument('-input', metavar='input', type=str, help='the directory containing the json files')
    parser.add_argument('-output', metavar='output', type=str, help='the directory to write the new json files to')
    parser.add_argument('-precision', metavar='decprecision', type=int, default=5, help='amount of decimals to save the document vectors to')
    
    args = parser.parse_args()

    # Get the json files to add SLR
    write_SLR_to_collection(args.input, args.output, args.precision)
    