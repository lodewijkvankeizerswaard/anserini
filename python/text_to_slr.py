import numpy as np
import torch
import sys


def get_sparse_representation(text="query of words", dim=1000, sparsity_ratio=0.9):
    rand = np.random.uniform(0, 1, size=dim)
    rand[rand <= sparsity_ratio] = 0
    return torch.Tensor(rand)

def condense_slr(slr):
    return [(i,float(v)) for i, v in enumerate(slr) if v > 0]
        

if __name__ == "__main__":
	# getting command line arguments
    # print('Argument List:', str(sys.argv))
    slr = get_sparse_representation(sys.argv[1]) if len(sys.argv) > 2 else get_sparse_representation()
    print(condense_slr(slr))
