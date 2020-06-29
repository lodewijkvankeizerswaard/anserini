import numpy as np
import matplotlib.pyplot as plt
import sys
np.set_printoptions(threshold=sys.maxsize)

def get_posting_lengths(reprs, sparse_dims):
    # lengths = np.zeros(sparse_dims)
    return lengths

def plot_ordered_posting_lists_lengths(path=".",reprs = None, name = "", n=-1):
    sparse_dims = reprs.shape[1]
    frequencies = (reprs !=0).sum(0)
    n = n if n > 0 else len(frequencies)
    top_n = sorted(frequencies, reverse=True)[:n]
    # run matplotlib on background, not showing the plot
    plt.plot(top_n)
    plt.ylabel('Frequency')
    n_text = f' (top {n})' if n != len(frequencies) else ''
    plt.xlabel('Latent Dimension (Sorted)' + n_text)
    plt.xlim(0, 2300)
    plt.ylim(0, top_n[0])
    # plt.show()
    plt.savefig(path+ f'/num_{name}_per_latent_term.pdf', bbox_inches='tight')
    plt.close()

sparse_dimensions = 5000
num_docs = 250
zipf_param = 0.35 # should be in (0,1)

# define decimal precision in :
# "formatter={'float_kind':lambda x: "%.7f" % x}"
# replace 7 with whatever you want

file_dir = "topics/"
filename = "zipfian_robust04_slr_topics.tsv"

zipf_mask = np.asarray([1/np.power((i+1),zipf_param) for i in range(sparse_dimensions)])

results_file = open(file_dir + filename, 'a')

reprs = []

# start = sys.exec("tail -n 1 data/zipfian_robust04_sparse_doc_reprs.txt | sed 's/\\t.*//'")[4:]

start = 0
# start = 3194
# start = 25607
# start = 242116

for i in range(start + 1, num_docs):
    doc_id = "doc_" + str(i)
    doc_repr = np.random.uniform(size =sparse_dimensions)
    mask = (doc_repr < zipf_mask).astype(float)
    doc_repr = doc_repr * mask
    # print(doc_repr)
    reprs.append(doc_repr)
    doc_repr_str = np.array2string(doc_repr, separator=' ', formatter={'float_kind':lambda x: "%.7f" % x})[1:-1].replace('\n', '')
    results_file.write(f'{doc_id}\t{doc_repr_str}\n')

results_file.close()

reprs = np.stack(reprs, axis = 0)

# print(reprs.shape)
# exit()
plot_ordered_posting_lists_lengths(name="3", reprs =reprs)

