#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input data/msmarco-pas -generator SLRGenerator -slr -slr.index -slr.decimalPrecision 7 -threads 10 -index data/msmarco-pas-neuralindex-7
