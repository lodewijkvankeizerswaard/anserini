import numpy as np
import sys
np.set_printoptions(threshold=sys.maxsize)

sparse_dimensions = 5000

num_docs = 250
num_queries = 10

# define decimal precision in :
# "formatter={'float_kind':lambda x: "%.7f" % x}"
# replace 7 with whatever you want

decimal_precision = 10

file_dir = "topics/"
filename = "fake_robust04_slr_topics.tsv"



zipf_mask = np.asarray([1/(i+1) for i in range(sparse_dimensions)])


results_file = open(file_dir + filename, 'w')


for i in range(num_docs):
	doc_id = "doc_" + str(i)
	doc_repr = np.random.uniform(size =sparse_dimensions)
	mask = (doc_repr < zipf_mask).astype(float)
	doc_repr = doc_repr * mask
	# print(len(doc_repr))

	doc_repr_str = np.array2string(doc_repr, precision = decimal_precision, separator=' ', formatter={'float_kind':lambda x: "%.7f" % x})[1:-1].replace('\n', '')

	results_file.write(f'{doc_id}\t{doc_repr_str}\n')

results_file.close()


