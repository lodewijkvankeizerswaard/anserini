#/bin/sh
sh target/appassembler/bin/IndexCollection -collection JsonCollection -input data/testcollection -generator SLRGenerator -slr -slr.index -slr.decimalPrecision 7 -threads 10 -index data/testcollection-neuralindex-7
